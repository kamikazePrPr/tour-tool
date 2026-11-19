package com.kamikazeprpr.tourtool.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kamikazeprpr.tourtool.ui.theme.TextMain
import com.kamikazeprpr.tourtool.ui.theme.TextMuted

/**
 * 安装后第一次打开：个人信息处理与使用声明。
 *
 * 正文由 Codex 依据《个人信息保护法》《网络安全法》《数据安全法》
 * 和 GB/T 35273-2020 以法律语言起草，覆盖处理目的、敏感个人信息的单独同意、
 * 处理方式、对外提供禁令、保存期限、个人权利与使用限制。
 *
 * 只在第一次出现；点「我已知晓，继续使用，之后不再提示」后永不再显示。
 */
@Composable
fun FirstRunScreen(
    onAgree: () -> Unit,
    onExit: () -> Unit,
) {
    var showExitConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Text(
                text = "个人信息处理与使用声明",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "请在使用前完整阅读以下内容",
                fontSize = 14.sp,
                color = TextMuted,
            )
            Spacer(Modifier.height(18.dp))

            Paragraph(
                "本应用仅从用户经被采集人同意后拍摄或选取的证件照片中，收集姓名、性别、公民身份号码、" +
                    "出生日期。处理目的仅限为游客办理出行登记、统一投保旅游意外险及出行时核验身份，不得另作他用。"
            )
            Paragraph(
                "公民身份号码、出生日期依《个人信息保护法》第二十八条按敏感个人信息处理。" +
                    "个人信息处理者应事先向被采集人告知处理的必要性及对个人权益的影响，并取得其单独同意。"
            )
            Paragraph(
                "个人信息均在本设备本地离线处理，仅存于应用私有目录，不上传任何服务器、不进行云备份，" +
                    "不作自动化决策或画像。不向任何第三方提供、转让或公开披露；" +
                    "严禁出售、非法买卖、非法向他人提供个人信息。"
            )
            Paragraph(
                "仅连接 GitHub 相关域名检查、下载应用更新，不经该等连接传输任何个人信息。" +
                    "本地运行日志不含姓名及完整证件号码，保存 7 日后自动删除。" +
                    "个人信息的保存期限截至用户主动删除之时；用户可删除单个团或全部数据，删除后不可恢复，" +
                    "已导出的表格文件不因本机删除而失效。"
            )
            Paragraph(
                "用户及被采集人有权查阅、复制、更正、删除其个人信息及撤回同意，" +
                    "撤回不影响此前基于同意已进行的处理。" +
                    "用户不得利用本应用从事违法违规活动，包括但不限于冒用他人身份、伪造变造证件、" +
                    "未经同意收集他人个人信息、出售或非法提供他人个人信息。"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 22.dp, vertical = 12.dp),
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Button(
                onClick = onAgree,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("我已知晓，继续使用，之后不再提示", fontSize = 17.sp)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showExitConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("不使用，退出", fontSize = 18.sp)
            }
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("确定不使用了？", fontSize = 22.sp) },
            text = { Text("退出后不会创建任何数据。", fontSize = 18.sp) },
            confirmButton = {
                TextButton(onClick = onExit) { Text("退出", fontSize = 18.sp) }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("继续使用", fontSize = 18.sp) }
            },
        )
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        color = TextMain,
    )
    Spacer(Modifier.height(15.dp))
}
