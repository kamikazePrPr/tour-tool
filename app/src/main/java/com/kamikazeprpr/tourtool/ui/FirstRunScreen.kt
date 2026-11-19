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
 * 安装后第一次打开：隐私与数据使用声明。
 *
 * 正文由 Codex 依据《个人信息保护法》《网络安全法》《数据安全法》
 * 和 GB/T 35273-2020 起草，覆盖处理目的、敏感个人信息单独说明、
 * 处理方式、对外提供情况、保存与删除、个人权利六项要素。
 *
 * 只在第一次出现，点「我已知晓，继续使用，之后不再提示」之后永不再显示。
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
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "使用前，请先看一下",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
        )
        Spacer(Modifier.height(18.dp))

        Paragraph(
            "为游客办理登记、统一购买旅游保险，并在出行时核对身份，需要收集姓名、身份证号、" +
                "出生日期等信息。身份证号和出生日期属于敏感个人信息，只在办理登记和投保时使用，不做其他用途。"
        )
        Paragraph(
            "相册中的照片只供读取，不修改、不删除、不上传，识别在本机离线完成。" +
                "资料只存放在本机私有目录，不上传、不做云备份、不共享，也没有广告和统计。"
        )
        Paragraph(
            "应用只联网访问 GitHub 查询新版本，不会通过联网发送游客资料。" +
                "日志不含姓名和完整号码，保存 7 天后自动删除。"
        )
        Paragraph(
            "不得用这个软件做违法的事，比如冒用别人的身份、改动证件上的信息、" +
                "没经过本人同意就把别人的证件登记进来。您仍然可以要求查阅、更正、删除资料，" +
                "也可以随时删除某个团或全部数据，删掉就找不回来；已经发出去的 Excel 表格不受影响，" +
                "需要另外联系收件人处理。"
        )

        Spacer(Modifier.height(26.dp))

        Button(
            onClick = onAgree,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("我已知晓，继续使用，之后不再提示", fontSize = 17.sp)
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = { showExitConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("不使用，退出", fontSize = 18.sp)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "这份说明随时可以在「设置 → 关于」里再看一遍。",
            fontSize = 14.sp,
            color = TextMuted,
        )
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
        fontSize = 17.sp,
        lineHeight = 27.sp,
        color = TextMain,
    )
    Spacer(Modifier.height(16.dp))
}
