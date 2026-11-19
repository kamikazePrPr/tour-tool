package com.kamikazeprpr.tourtool.ui.member

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kamikazeprpr.tourtool.core.IdCard
import com.kamikazeprpr.tourtool.data.db.Member
import com.kamikazeprpr.tourtool.ocr.ImageLoader
import com.kamikazeprpr.tourtool.ui.theme.DeepIndigo
import com.kamikazeprpr.tourtool.ui.theme.StateBad
import com.kamikazeprpr.tourtool.ui.theme.TextMain
import com.kamikazeprpr.tourtool.ui.theme.TextMuted
import com.kamikazeprpr.tourtool.ui.theme.WarmWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 点开某一个人：上面是相册原图（能放大），下面是可以改的输入框。
 * 号码边输入边校验，校验不过就红字「身份证号错误」，按钮点不动。
 */
@Composable
fun MemberEditScreen(
    member: Member,
    onSave: (name: String, gender: String, idNumber: String) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(member.name) }
    var gender by remember { mutableStateOf(member.gender) }
    var idNumber by remember { mutableStateOf(member.idNumber) }
    // 打开就放大一点，让卡面上的字看得清；不会捏合的人用右下角的正负按钮
    var scale by remember { mutableFloatStateOf(1.7f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var confirmDelete by remember { mutableStateOf(false) }

    // 原图：按需采样解码，绝不整张读进内存
    val bitmap by produceState<Bitmap?>(initialValue = null, member.sourceUri) {
        value = withContext(Dispatchers.IO) {
            val uriStr = member.sourceUri ?: return@withContext null
            runCatching {
                ImageLoader.decodeSampled(ctx.contentResolver, Uri.parse(uriStr), 2400)
            }.getOrNull()
        }
    }

    val cleanNumber = IdCard.normalizeNumber(idNumber)
    val numberIssues = if (cleanNumber.length >= 15) IdCard.validate(cleanNumber) else emptyList()
    val canSave = name.isNotBlank() && cleanNumber.length == 18 && numberIssues.isEmpty()

    val transformState = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 6f)
        offset += pan
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("‹ 返回", fontSize = 20.sp) }
            Spacer(Modifier.weight(1f))
            Text(
                text = "第 ${member.seq} 个",
                fontSize = 16.sp,
                color = TextMuted,
            )
        }

        // ---- 原图 ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(androidx.compose.ui.graphics.Color(0xFF222222)),
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "身份证原图",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                        )
                        .transformable(transformState),
                )
            } else {
                Text(
                    text = "原图打不开了\n可能已经不在相册里",
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 16.sp,
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }

            // 右上角两个大按钮：不会捏合的人在手机上也能放大看清。
            // 放右上是因为证件号码印在卡片右下，按钮放右下会正好压住最后几位数字。
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ZoomButton("＋") {
                    scale = (scale * 1.5f).coerceIn(1f, 6f)
                }
                ZoomButton("－") {
                    scale = (scale / 1.5f).coerceIn(1f, 6f)
                    if (scale <= 1f) offset = Offset.Zero
                }
            }
        }

        // ---- 输入区 ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.58f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("姓名", fontSize = 16.sp) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            Text("性别", fontSize = 15.sp, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GenderButton("男", selected = gender == IdCard.GENDER_MALE) {
                    gender = IdCard.GENDER_MALE
                }
                GenderButton("女", selected = gender == IdCard.GENDER_FEMALE) {
                    gender = IdCard.GENDER_FEMALE
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = idNumber,
                onValueChange = { input ->
                    // 只收数字和 X，其他字符直接吞掉
                    idNumber = input.filter { it.isDigit() || it == 'x' || it == 'X' }.take(18)
                },
                label = { Text("身份证号", fontSize = 16.sp) },
                singleLine = true,
                isError = numberIssues.isNotEmpty(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            if (numberIssues.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "身份证号错误",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StateBad,
                )
                numberIssues.forEach {
                    Text(text = it, fontSize = 14.sp, color = StateBad)
                }
            } else if (cleanNumber.length == 18) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "号码核对通过",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = { onSave(name, gender, cleanNumber) },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("好了，就这样", fontSize = 19.sp)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("这个人不登记了", fontSize = 17.sp)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("把这个人去掉？", fontSize = 22.sp) },
            text = { Text("去掉之后，这个人不会出现在表格里。", fontSize = 17.sp) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("去掉", fontSize = 18.sp) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("留着", fontSize = 18.sp) }
            },
        )
    }
}

@Composable
private fun ZoomButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(WarmWhite)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 24.sp, color = DeepIndigo)
    }
}

@Composable
private fun GenderButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .height(52.dp)
                .fillMaxWidth(0.32f),
            shape = RoundedCornerShape(10.dp),
        ) { Text(label, fontSize = 19.sp) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .height(52.dp)
                .fillMaxWidth(0.32f),
            shape = RoundedCornerShape(10.dp),
        ) { Text(label, fontSize = 19.sp, color = TextMain) }
    }
}
