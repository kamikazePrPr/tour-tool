package com.kamikazeprpr.tourtool.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kamikazeprpr.tourtool.ui.theme.TextMain

/**
 * 首页：一个大按钮「新建一个旅游团」，下面是已有团的列表。
 * 目前只有骨架，团列表与录入流程在后续版本补上。
 */
@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            text = "旅游证件登记",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "把客人的身份证登记成表格",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = { /* TODO 新建团 */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Text("新建一个旅游团", fontSize = 22.sp)
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = "还没有旅游团。先建一个吧。",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
