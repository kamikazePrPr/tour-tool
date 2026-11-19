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
 * 安装后第一次打开：联网范围 + 信息安全说明。
 * 只在第一次出现；点「不使用，退出」不会创建任何数据。
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
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "使用前，请先看一下",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
        )
        Spacer(Modifier.height(24.dp))

        Section(
            title = "这个软件会联网做两件事",
            body = "看 GitHub 上有没有新版本，有就下载新版本。除了这两件事，它不连任何别的地方。",
        )
        Section(
            title = "客人的信息只在你这台手机里",
            body = "姓名、身份证号、照片，全部只存在这台手机上面。软件不上传、不备份到网上、不发给任何人，也没有任何统计和广告。",
        )
        Section(
            title = "相册里的照片不会被改动",
            body = "你选的身份证照片，软件只是看一眼，不修改、不删除、不移动。用完它还在原来的地方。",
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onAgree,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("我已知晓，继续使用", fontSize = 20.sp)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showExitConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("不使用，退出", fontSize = 18.sp)
        }

        Spacer(Modifier.height(20.dp))

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
private fun Section(title: String, body: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextMain,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = body,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        color = TextMuted,
    )
    Spacer(Modifier.height(22.dp))
}
