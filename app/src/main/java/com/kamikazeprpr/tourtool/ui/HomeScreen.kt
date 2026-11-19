package com.kamikazeprpr.tourtool.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kamikazeprpr.tourtool.data.db.Team
import com.kamikazeprpr.tourtool.ui.home.HomeViewModel
import com.kamikazeprpr.tourtool.ui.team.TeamScreen
import com.kamikazeprpr.tourtool.ui.theme.TextMain
import com.kamikazeprpr.tourtool.ui.theme.TextMuted

/**
 * 首页：一个大按钮「新建一个旅游团」，下面是已有团。
 */
@Composable
fun HomeScreen() {
    val vm: HomeViewModel = viewModel()
    val teams by vm.teams.collectAsStateWithLifecycle()
    var openTeamId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }

    val currentTeamId = openTeamId
    if (currentTeamId != null) {
        TeamScreen(teamId = currentTeamId, onBack = { openTeamId = null })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "旅游证件登记",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "把客人的身份证登记成表格",
            fontSize = 17.sp,
            color = TextMuted,
        )
        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { showNewDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("新建一个旅游团", fontSize = 22.sp)
        }

        Spacer(Modifier.height(24.dp))

        if (teams.isEmpty()) {
            Text(
                text = "还没有旅游团。\n先点上面的按钮建一个。",
                fontSize = 18.sp,
                lineHeight = 28.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            teams.forEach { team ->
                val count by vm.memberCount(team.id).collectAsStateWithLifecycle(initialValue = 0)
                TeamCard(
                    team = team,
                    memberCount = count,
                    onClick = { openTeamId = team.id },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showNewDialog) {
        NewTeamDialog(
            onDismiss = { showNewDialog = false },
            onConfirm = { name ->
                vm.createTeam(name)
                showNewDialog = false
            },
        )
    }
}

@Composable
private fun TeamCard(team: Team, memberCount: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(team.name, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (memberCount == 0) "还没有登记人" else "已经登记 $memberCount 人",
                    fontSize = 16.sp,
                    color = TextMuted,
                )
            }
            Text("›", fontSize = 26.sp, color = TextMuted)
        }
    }
}

@Composable
private fun NewTeamDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("这个团叫什么名字？", fontSize = 22.sp) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                placeholder = { Text("例如：11月大阪三日团", fontSize = 18.sp) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("好了", fontSize = 20.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("算了", fontSize = 18.sp) }
        },
    )
}
