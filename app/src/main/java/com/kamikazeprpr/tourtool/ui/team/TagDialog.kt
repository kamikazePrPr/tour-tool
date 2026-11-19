package com.kamikazeprpr.tourtool.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kamikazeprpr.tourtool.data.db.Member
import com.kamikazeprpr.tourtool.data.db.Tag
import com.kamikazeprpr.tourtool.ui.theme.TagColors
import com.kamikazeprpr.tourtool.ui.theme.TextMain
import com.kamikazeprpr.tourtool.ui.theme.TextMuted
import kotlinx.coroutines.launch

/**
 * 长按成员弹出来的标签选择框。
 *
 * 选中是实心色块，没选是浅底；右上角那个加号可以现场新建标签——
 * 颜色按现有标签数量自动往后排，新加的永远跟已有的不同色。
 */
@Composable
fun TagDialog(
    member: Member,
    allTags: List<Tag>,
    selectedIds: Set<Long>,
    onDismiss: () -> Unit,
    onAddTag: suspend (String) -> Long?,
    onTagCreated: () -> Unit,
    onSave: (List<Long>) -> Unit,
) {
    var picked by remember { mutableStateOf(selectedIds) }
    var adding by remember { mutableStateOf(false) }
    var newLabel by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun create() {
        val name = newLabel.trim()
        if (name.isEmpty()) {
            hint = "先写个名字"
            return
        }
        scope.launch {
            val id = onAddTag(name)
            if (id != null) {
                picked = picked + id
                newLabel = ""
                adding = false
                hint = "已经加上了，点保存生效"
                onTagCreated()
            } else {
                hint = "这个名字用不了"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("给这个人加标签", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(member.name.ifBlank { "没读到姓名" }, fontSize = 15.sp, color = TextMuted)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            RoundedCornerShape(50),
                        )
                        .clickable {
                            adding = !adding
                            hint = null
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新建标签",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (adding) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = newLabel,
                            onValueChange = { newLabel = it },
                            placeholder = { Text("新标签叫啥", fontSize = 16.sp) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { create() }) {
                            Text("加上", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                hint?.let {
                    Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }

                if (allTags.isEmpty()) {
                    Text("还没有标签，点右上角加号建一个", fontSize = 16.sp, color = TextMuted)
                } else {
                    allTags.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { tag ->
                                val on = tag.id in picked
                                val style = TagColors.of(tag.colorIndex)
                                Box(
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
                                    contentAlignment = Alignment.Center,
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
                Text(
                    "长按名单里的人就能改；标签是全局的，这个团建的，下个团也能用。",
                    fontSize = 13.sp,
                    color = TextMuted,
                )
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
