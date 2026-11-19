package com.kamikazeprpr.tourtool.export

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 最小 xlsx 生成器：一个工作表、全是文本单元格。
 *
 * 为什么不引 Apache POI：POI 在安卓上要开 desugar、方法数上万、包体多十几兆，
 * 而这里只需要「一张表 + 七列文字」。直接拼 OOXML 的 zip 包，零依赖，JVM 上也能单测。
 *
 * 关键点：身份证号必须走文本格式（numFmtId=49），否则 Excel 会把它当成数字
 * 显示成 1.10101E+17 这种科学计数法。
 */
object XlsxWriter {

    data class Sheet(
        val name: String,
        val header: List<String>,
        val rows: List<List<String>>,
        /** 需要强制文本格式的列下标（0 开始），一般就是身份证号那列 */
        val textColumns: Set<Int> = emptySet(),
        /** 各列宽度（字符数），缺省 12 */
        val columnWidths: List<Int> = emptyList(),
    )

    fun write(out: OutputStream, sheet: Sheet) {
        ZipOutputStream(out).use { zip ->
            entry(zip, "[Content_Types].xml", CONTENT_TYPES)
            entry(zip, "_rels/.rels", ROOT_RELS)
            entry(zip, "xl/workbook.xml", workbookXml(sheet.name))
            entry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            entry(zip, "xl/styles.xml", STYLES)
            entry(zip, "xl/worksheets/sheet1.xml", sheetXml(sheet))
        }
    }

    private fun entry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun workbookXml(sheetName: String): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="${escape(sheetName.take(31))}" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private fun sheetXml(sheet: Sheet): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        if (sheet.columnWidths.isNotEmpty()) {
            sb.append("<cols>")
            sheet.columnWidths.forEachIndexed { i, width ->
                sb.append("""<col min="${i + 1}" max="${i + 1}" width="$width" customWidth="1"/>""")
            }
            sb.append("</cols>")
        }

        sb.append("<sheetData>")
        var rowIndex = 1
        sb.append(rowXml(sheet.header, rowIndex++, emptySet()))
        sheet.rows.forEach { row ->
            sb.append(rowXml(row, rowIndex++, sheet.textColumns))
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun rowXml(cells: List<String>, rowNumber: Int, textColumns: Set<Int>): String {
        val sb = StringBuilder("""<row r="$rowNumber">""")
        cells.forEachIndexed { i, value ->
            val ref = columnName(i) + rowNumber
            val style = if (i in textColumns) """ s="1"""" else ""
            sb.append("""<c r="$ref"$style t="inlineStr"><is><t xml:space="preserve">""")
            sb.append(escape(value))
            sb.append("</t></is></c>")
        }
        sb.append("</row>")
        return sb.toString()
    }

    /** 0 → A, 25 → Z, 26 → AA */
    fun columnName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (true) {
            val ch: Char = 'A' + (n % 26)
            sb.insert(0, ch.toString())
            n = n / 26 - 1
            if (n < 0) break
        }
        return sb.toString()
    }

    private fun escape(raw: String): String {
        val sb = StringBuilder(raw.length + 8)
        for (ch in raw) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                // XML 1.0 不接受这些控制字符，直接丢掉
                in '\u0000'..'\u001F' -> if (ch == '\n' || ch == '\t') sb.append(ch)
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private const val ROOT_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val WORKBOOK_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    /** 两个样式：0=常规，1=文本（numFmtId 49 就是内置的 @ 文本格式） */
    private const val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="1"><font><sz val="11"/><name val="宋体"/></font></fonts>
<fills count="1"><fill><patternFill patternType="none"/></fill></fills>
<borders count="1"><border/></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="2">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="49" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""
}
