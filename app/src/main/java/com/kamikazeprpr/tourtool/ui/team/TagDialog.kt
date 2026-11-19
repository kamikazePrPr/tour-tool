package com.kamikazeprpr.tourtool.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kamikazeprpr.tourtool.data.db.Member
import com.kamikazeprpr.tourtool.data.db.Tag
import com.kamikazeprpr.tourtool.ui.theme.TagColors
import com.kamikazeprpr.tourtool.ui.theme.TextMain
import com.kamikazeprpr.tourtool.ui.theme.TextMuted

/**
 * 长按成员弹出来的标签选择框。
 * 选中是实心色块，没选是浅底描边，一眼能分清；改完点保存一次性写库。
 */
@Composable
fun TagDialog(
    member: Member,
    allTags: List<Tag>,
    selectedIds: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (List<Long>) -> Unit,
) {
    var picked by remember { mutableStateOf(selectedIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("给这个人加标签", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(member.name.ifBlank { "没读到姓名" }, fontSize = 15.sp, color = TextMuted)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (allTags.isEmpty()) {
                    Text("还没有标签", fontSize = 16.sp, color = TextMuted)
                } else {
                    allTags.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { tag ->
                                val on = tag.id in picked
                                val style = TagColors.of(tag.colorIndex)
                                Column(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .height(48.dp)
                                        .background(
                                            color = if (on) style.fg else style.bg,
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                        .clickable {
                                            picked = if (on) picked - tag.id else picked + tag.id
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        text = tag.label,
                                        fontSize = 16.sp,
                                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (on) MaterialTheme.colorScheme.surface else style.fg,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("长按名单里的人就能改，加错了点一下取消再点保存。", fontSize = 13.sp, color = TextMuted)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(picked.toList()) }) {
                Text("保存", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", fontSize = 18.sp, color = TextMuted)
            }
        },
    )
}
