package com.kamikazeprpr.tourtool.ui.team

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kamikazeprpr.tourtool.core.CardParser
import com.kamikazeprpr.tourtool.core.IdCard
import com.kamikazeprpr.tourtool.data.AppLog
import com.kamikazeprpr.tourtool.data.TourRepository
import com.kamikazeprpr.tourtool.data.db.Member
import com.kamikazeprpr.tourtool.data.db.Team
import com.kamikazeprpr.tourtool.data.db.TourDatabase
import com.kamikazeprpr.tourtool.ocr.ImageLoader
import com.kamikazeprpr.tourtool.ocr.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TeamViewModel(app: Application, private val teamId: Long) : AndroidViewModel(app) {

    private val repo = TourRepository(TourDatabase.get(app))
    private val ocr = OcrEngine()

    val team: StateFlow<Team?> = repo.team(teamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val members: StateFlow<List<Member>> = repo.members(teamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    data class Progress(
        val current: Int,
        val total: Int,
        val autoOk: Int,
        val needReview: Int,
    )

    private val _progress = MutableStateFlow<Progress?>(null)
    val progress: StateFlow<Progress?> = _progress

    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage

    fun consumeMessage() { _lastMessage.value = null }

    /**
     * 逐张处理：持久化 URI 权限 → 采样解码 → 三通道识别 → 合并判定 → 落库。
     * 工作 Bitmap 用完立刻回收；相册原图全程只读。
     */
    fun importImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val app = getApplication<Application>()
            var autoOk = 0
            var review = 0
            var failed = 0

            for ((index, uri) in uris.withIndex()) {
                _progress.value = Progress(index + 1, uris.size, autoOk, review)

                var permissionHeld = false
                try {
                    app.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                    permissionHeld = true
                } catch (_: SecurityException) {
                    // 个别 ROM 不给持久授权，降级：本次仍能读，重启后核对页可能看不到原图
                }

                val bitmap = withContext(Dispatchers.IO) {
                    // 解码到 2000：比识别用的 1600 略大，给透视校正留余量
                    ImageLoader.decodeSampled(app.contentResolver, uri, DECODE_MAX_SIDE)
                }
                if (bitmap == null) {
                    failed++
                    continue
                }

                val texts = withContext(Dispatchers.Default) {
                    try {
                        ocr.readThreeChannels(bitmap)
                    } finally {
                        bitmap.recycle()
                    }
                }

                val merged = CardParser.merge(texts)
                // 诊断日志：记通道文本但先脱敏，号码只留前 3 后 2
                texts.forEachIndexed { i, t ->
                    AppLog.d("OCR", "ch$i => " + AppLog.maskAll(t.replace("\n", " ⏎ ").take(400)))
                }
                AppLog.i(
                    "OCR",
                    "结果 ok=${merged.autoOk} name=${if (merged.fields.name.isNullOrBlank()) "空" else "有"} " +
                        "num=${merged.fields.number?.let { AppLog.maskId(it) } ?: "空"} issues=${merged.issues.size}"
                )
                val birth = merged.fields.birth?.takeIf { it.length == 8 }?.let {
                    "${it.substring(0, 4)}-${it.substring(4, 6)}-${it.substring(6, 8)}"
                }

                repo.addMember(
                    teamId = teamId,
                    name = merged.fields.name.orEmpty(),
                    gender = merged.fields.gender.orEmpty(),
                    idNumber = merged.fields.number.orEmpty(),
                    birthDate = birth,
                    status = if (merged.autoOk) Member.STATUS_AUTO_OK else Member.STATUS_NEED_REVIEW,
                    failReasons = (merged.issues + merged.notes)
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString("；"),
                    sourceUri = uri.toString(),
                    uriPermissionHeld = permissionHeld,
                )

                if (merged.autoOk) autoOk++ else review++
            }

            _progress.value = null
            _lastMessage.value = buildString {
                append("读完 ${uris.size - failed} 张：")
                append("$autoOk 张直接对上了")
                if (review > 0) append("，$review 张要你看一眼")
                if (failed > 0) append("，$failed 张读不出来")
            }
        }
    }

    /** 人工改完保存：号码校验通过就是「人工确认过了」，否则仍然挂着「要你看一眼」 */
    fun saveManual(
        member: Member,
        name: String,
        gender: String,
        idNumber: String,
    ) {
        viewModelScope.launch {
            val cleanNumber = IdCard.normalizeNumber(idNumber)
            val birth = IdCard.birthFromNumber(cleanNumber)
            val issues = IdCard.validate(cleanNumber)
            repo.updateMember(
                member.copy(
                    name = name.trim(),
                    gender = gender,
                    idNumber = cleanNumber,
                    birthDate = birth,
                    status = if (issues.isEmpty()) Member.STATUS_MANUAL_OK else Member.STATUS_NEED_REVIEW,
                    failReasons = issues.takeIf { it.isNotEmpty() }?.joinToString("；"),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch { repo.deleteMember(member) }
    }

    /** 导出页要用：一次拿全团标签 */
    suspend fun tagsForExport(): Map<Long, List<String>> = repo.tagsOfTeam(teamId)

    companion object {
        // 解码上限要高于识别尺寸，否则等于先毁掉细节再放大
        private const val DECODE_MAX_SIDE = 3200

        fun factory(teamId: Long) = viewModelFactory {
            initializer {
                TeamViewModel(this[APPLICATION_KEY] as Application, teamId)
            }
        }
    }
}
