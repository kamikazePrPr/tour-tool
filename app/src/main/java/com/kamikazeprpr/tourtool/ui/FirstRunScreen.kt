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
import com.kamikazeprpr.tourtool.ui.about.LegalText
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
                text = LegalText.PRIVACY_TITLE,
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

            // 与「关于 → 用户隐私与协议」是同一份文本，改一处两边同步
            LegalText.PRIVACY_PARAGRAPHS.forEach { Paragraph(it) }
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
