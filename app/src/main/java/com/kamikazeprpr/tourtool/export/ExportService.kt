package com.kamikazeprpr.tourtool.export

import com.kamikazeprpr.tourtool.core.IdCard
import com.kamikazeprpr.tourtool.data.db.Member
import java.io.File
import java.text.Collator
import java.util.Locale

/**
 * 把团里的成员整理成表格：选列、排序、生成 xlsx 或 txt。
 * 预览和最终文件都走同一份 [buildTable] 结果，不存在「预览一套、导出另一套」。
 */
object ExportService {

    enum class Column(val key: String, val title: String, val defaultOn: Boolean = true) {
        SEQ("seq", "序号"),
        NAME("name", "姓名"),
        GENDER("gender", "性别"),
        ID_TYPE("idType", "证件类型"),
        ID_NUMBER("idNumber", "证件号码"),
        BIRTH("birth", "出生日期"),
        PHONE("phone", "手机"),
        NOTE("note", "备注", defaultOn = false),
        TAGS("tags", "标签", defaultOn = false),
    }

    enum class SortBy(val label: String) {
        ENTRY("按录入顺序"),
        NAME("按姓名"),
        BIRTH("按出生日期"),
        TAGS("按标签"),
    }

    data class Table(
        val header: List<String>,
        val rows: List<List<String>>,
        val textColumns: Set<Int>,
    )

    /**
     * @param columns 勾选并排好序的列
     * @param tagsByMember 成员 id → 标签名列表
     */
    fun buildTable(
        members: List<Member>,
        tagsByMember: Map<Long, List<String>>,
        columns: List<Column>,
        sortBy: SortBy,
        descending: Boolean = false,
    ): Table {
        val sorted = when (sortBy) {
            SortBy.ENTRY -> members.sortedBy { it.seq }
            SortBy.NAME -> {
                val collator = Collator.getInstance(Locale.CHINA)
                members.sortedWith { a, b -> collator.compare(a.name, b.name) }
            }
            SortBy.BIRTH -> members.sortedWith(
                compareBy(nullsLast()) { it.birthDate }
            )
            SortBy.TAGS -> members.sortedBy { tagsByMember[it.id].orEmpty().joinToString(",") }
        }.let { if (descending) it.reversed() else it }

        val header = columns.map { it.title }
        val rows = sorted.mapIndexed { index, member ->
            columns.map { column ->
                when (column) {
                    Column.SEQ -> (index + 1).toString()
                    Column.NAME -> member.name
                    Column.GENDER -> IdCard.genderLabel(member.gender)
                    // 目前只登记身份证；留着这一列是为了以后加护照、港澳通行证时表格结构不用改
                    Column.ID_TYPE -> "身份证"
                    Column.ID_NUMBER -> member.idNumber
                    Column.BIRTH -> member.birthDate.orEmpty()
                    Column.PHONE -> member.phone
                    Column.NOTE -> member.note.orEmpty()
                    Column.TAGS -> tagsByMember[member.id].orEmpty().joinToString("、")
                }
            }
        }

        // 身份证号列强制文本，序号列保持常规（数字）
        val textColumns = columns.mapIndexedNotNull { i, c ->
            if (c == Column.ID_NUMBER) i else null
        }.toSet()

        return Table(header = header, rows = rows, textColumns = textColumns)
    }

    fun writeXlsx(file: File, table: Table, sheetName: String) {
        val widths = table.header.mapIndexed { i, title ->
            when (title) {
                "证件号码" -> 22
                "手机" -> 15
                "备注" -> 20
                "标签" -> 16
                "序号" -> 6
                else -> 12
            }
        }
        file.outputStream().use { out ->
            XlsxWriter.write(
                out,
                XlsxWriter.Sheet(
                    name = sheetName,
                    header = table.header,
                    rows = table.rows,
                    textColumns = table.textColumns,
                    columnWidths = widths,
                ),
            )
        }
    }

    /** 纯文本：UTF-8 带 BOM、Tab 分隔、CRLF 换行。不带 BOM 的话微信里打开是乱码。 */
    fun writeTxt(file: File, table: Table) {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append(table.header.joinToString("\t")).append("\r\n")
        table.rows.forEach { row ->
            sb.append(row.joinToString("\t") { it.replace("\t", " ").replace("\r", " ").replace("\n", " ") })
            sb.append("\r\n")
        }
        file.writeText(sb.toString(), Charsets.UTF_8)
    }

    fun fileName(teamName: String, ext: String): String {
        val clean = teamName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "名单" }
        val stamp = java.text.SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(java.util.Date())
        return "${clean}_$stamp.$ext"
    }
}
