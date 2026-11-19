package com.kamikazeprpr.tourtool.export

import com.kamikazeprpr.tourtool.data.db.Member
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipInputStream

class ExportServiceTest {

    private fun sampleMembers() = listOf(
        Member(id = 1, teamId = 1, seq = 1, name = "徐建清", gender = "M", idNumber = "320219196208117033", birthDate = "1962-08-11"),
        Member(id = 2, teamId = 1, seq = 2, name = "吴建华", gender = "F", idNumber = "320219195803227021", birthDate = "1958-03-22"),
    )

    @Test
    fun xlsx_containsAllParts() {
        val file = File.createTempFile("export", ".xlsx")
        try {
            val table = ExportService.buildTable(
                members = sampleMembers(),
                tagsByMember = emptyMap(),
                columns = ExportService.Column.entries,
                sortBy = ExportService.SortBy.ENTRY,
            )
            ExportService.writeXlsx(file, table, "测试团")

            val entries = mutableMapOf<String, String>()
            ZipInputStream(file.inputStream()).use { zip ->
                var e = zip.nextEntry
                while (e != null) {
                    entries[e.name] = zip.readBytes().toString(Charsets.UTF_8)
                    zip.closeEntry()
                    e = zip.nextEntry
                }
            }

            assertTrue(entries.containsKey("[Content_Types].xml"))
            assertTrue(entries.containsKey("xl/workbook.xml"))
            assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
            assertTrue(entries.containsKey("xl/styles.xml"))

            val sheet = entries["xl/worksheets/sheet1.xml"]!!
            assertTrue(sheet.contains("徐建清"))
            assertTrue(sheet.contains("320219196208117033"))
            // 身份证号那一列必须挂文本样式 s="1"，否则 Excel 会显示成科学计数法
            assertTrue(sheet.contains("""s="1""""))
        } finally {
            file.delete()
        }
    }

    @Test
    fun txt_startsWithBomAndUsesTabs() {
        val table = ExportService.buildTable(
            members = sampleMembers(),
            tagsByMember = mapOf(1L to listOf("孕妇")),
            columns = listOf(ExportService.Column.NAME, ExportService.Column.ID_NUMBER, ExportService.Column.TAGS),
            sortBy = ExportService.SortBy.NAME,
        )
        val file = File.createTempFile("export", ".txt")
        try {
            ExportService.writeTxt(file, table)
            val text = file.readText(Charsets.UTF_8)
            assertEquals('\uFEFF', text.first())
            assertTrue(text.contains("姓名\t身份证号\t标签"))
            assertTrue(text.contains("吴建华\t320219195803227021"))
            assertTrue(text.contains("\r\n"))
            // 按姓名排序：吴（w）在徐（x）前面？中文 Collator 按拼音，吴 wu、徐 xu，吴在前
            val lines = text.trim().lines()
            assertTrue(lines[1].startsWith("吴建华"))
        } finally {
            file.delete()
        }
    }

    @Test
    fun columnsAreSelectedAndOrdered() {
        val table = ExportService.buildTable(
            members = sampleMembers(),
            tagsByMember = emptyMap(),
            columns = listOf(ExportService.Column.ID_NUMBER, ExportService.Column.NAME),
            sortBy = ExportService.SortBy.ENTRY,
        )
        assertEquals(listOf("身份证号", "姓名"), table.header)
        assertEquals(listOf("320219196208117033", "徐建清"), table.rows[0])
        assertEquals(setOf(0), table.textColumns)
    }

    @Test
    fun fileNameStripsIllegalChars() {
        val name = ExportService.fileName("11月/大阪:团", "xlsx")
        assertTrue(name.endsWith(".xlsx"))
        assertTrue(name.contains("11月_大阪_团"))
    }
}
