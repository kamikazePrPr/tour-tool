package com.kamikazeprpr.tourtool.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kamikazeprpr.tourtool.ui.theme.TextMain
import com.kamikazeprpr.tourtool.ui.theme.TextMuted

/**
 * 团内页面：选照片、看结果、做表格。
 * 目前是骨架，选照片与识别在下一个里程碑接上。
 */
@Composable
fun TeamScreen(teamId: Long, onBack: () -> Unit) {
    val vm: TeamViewModel = viewModel(key = "team-$teamId", factory = TeamViewModel.factory(teamId))
    val team by vm.team.collectAsStateWithLifecycle(initialValue = null)
    val members by vm.members.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ 返回", fontSize = 20.sp) }
            Spacer(Modifier.weight(1f))
        }
        Text(
            text = team?.name ?: "…",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (members.isEmpty()) "还没有登记人" else "已经登记 ${members.size} 人",
            fontSize = 17.sp,
            color = TextMuted,
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { /* TODO 选照片 */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Text("选客人的身份证照片", fontSize = 21.sp)
        }

        Spacer(Modifier.height(24.dp))

        if (members.isEmpty()) {
            Text(
                text = "点上面的按钮，从相册里选中客人的身份证照片（一次可以选很多张），软件会自动认字。",
                fontSize = 17.sp,
                lineHeight = 27.sp,
                color = TextMuted,
            )
        }
    }
}
