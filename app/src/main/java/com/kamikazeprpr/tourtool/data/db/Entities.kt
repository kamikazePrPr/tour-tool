package com.kamikazeprpr.tourtool.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一个旅游团。
 */
@Entity(tableName = "team")
data class Team(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val teamCode: String? = null,
    val departDate: String? = null,
    /** 行程结束日期，yyyy-MM-dd */
    val endDate: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * 团里的一个人。
 *
 * sourceUri 存的是相册原图的 content:// URI —— 核对时直接读它，不复制副本。
 * 原图不删不改，URI 权限在选图那一刻持久化（uriPermissionHeld）。
 */
@Entity(
    tableName = "member",
    foreignKeys = [
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("teamId"), Index(value = ["teamId", "idNumber"])],
)
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamId: Long,
    val seq: Int = 0,
    val name: String = "",
    val gender: String = "",          // "M" / "F" / ""（未知）
    val idNumber: String = "",        // 18 位，末位可能是 X
    val birthDate: String? = null,    // yyyy-MM-dd
    val registeredAt: Long = System.currentTimeMillis(), // 登记时间：处理当时，仅 App 内显示，不导出
    val status: String = STATUS_NEED_REVIEW,
    val failReasons: String? = null,  // JSON 数组，记录哪一道校验没过
    val sourceUri: String? = null,
    val uriPermissionHeld: Boolean = false,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val STATUS_AUTO_OK = "AUTO_OK"
        const val STATUS_NEED_REVIEW = "NEED_REVIEW"
        const val STATUS_MANUAL_OK = "MANUAL_OK"
        const val STATUS_OCR_FAILED = "OCR_FAILED"
        const val STATUS_UNVERIFIED = "UNVERIFIED"
    }
}

/**
 * 标签：全局库，不按团区分。这一团建的「孕妇」，下一团直接能用。
 */
@Entity(tableName = "tag", indices = [Index(value = ["label"], unique = true)])
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val colorIndex: Int = 0,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "member_tag",
    primaryKeys = ["memberId", "tagId"],
    indices = [Index("tagId")],
    foreignKeys = [
        ForeignKey(
            entity = Member::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MemberTag(
    val memberId: Long,
    val tagId: Long,
)
