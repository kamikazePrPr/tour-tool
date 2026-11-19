package com.kamikazeprpr.tourtool.core

/** 卡面上读到的字段（未经验证）。 */
data class CardFields(
    val name: String? = null,
    val gender: String? = null,   // "M" / "F"
    val birth: String? = null,    // yyyyMMdd
    val number: String? = null,   // 18 位
)

/** 三个通道合并后的结论。 */
data class MergeResult(
    val fields: CardFields,
    val autoOk: Boolean,
    val issues: List<String>,
    /** 不阻断、但值得让人知道的情况（例如"按校验位仲裁取了某一个"） */
    val notes: List<String>,
    val channelNumbers: List<String>,
    val channelCount: Int,
)

/**
 * 从 ML Kit 吐出来的整段文本里抠出姓名 / 性别 / 出生 / 号码，
 * 再把三个通道的结果合起来判定能不能自动通过。
 */
object CardParser {

    private val NUMBER_RE = Regex("""(\d{17}[0-9Xx])""")
    private val NAME_ANCHOR = Regex("""姓?\s*名""")
    private val GENDER_ANCHOR = Regex("""性\s*别""")
    private val BIRTH_RE = Regex("""(\d{4})\s*年\s*(\d{1,2})\s*月\s*(\d{1,2})\s*日?""")

    /** 姓名之后一旦碰到这些字，说明已经进到下一个字段了 */
    private val NAME_STOP_CHARS = setOf(
        '性', '民', '出', '住', '公', '签', '有', '年', '月', '日', '号', '族',
    )

    /**
     * 姓名不能靠「姓名 + 空格 + 单词」的正则：实测卡片左侧那个蓝色的「姓」经常读不出来，
     * 只剩一个「名」；而且名字可能被甩到下一行。所以这里从锚点往后逐字扫，
     * 遇到数字、下一个字段的首字、或者非空的换行就停。
     */
    private fun extractName(text: String): String? {
        extractNameByAnchor(text)?.let { return it }
        return extractNameFromTopLines(text)
    }

    private fun extractNameByAnchor(text: String): String? {
        val m = NAME_ANCHOR.find(text) ?: return null
        var i = m.range.last + 1
        val sb = StringBuilder()
        while (i < text.length && sb.length < 6) {
            val c = text[i]
            when {
                c in '\u4e00'..'\u9fa5' -> {
                    if (c in NAME_STOP_CHARS) break
                    sb.append(c)
                }
                c.isDigit() || c in 'A'..'Z' || c in 'a'..'z' -> break
                c == '\n' -> {
                    if (sb.isEmpty()) { i++; continue } else break
                }
                else -> Unit
            }
            i++
        }
        return sb.toString().takeIf { it.length in 2..4 }
    }

    /**
     * 兜底：卡片最上面那一行就是姓名行。
     * 实测「姓名」这两个蓝色小字经常被读成「姓*」「離的」之类，锚点完全靠不住；
     * 但名字本身是黑色大字，读得很准，所以直接到前几行里找汉字。
     */
    private fun extractNameFromTopLines(text: String): String? {
        for (line in text.lines().filter { it.isNotBlank() }.take(3)) {
            var han = line.filter { it in '\u4e00'..'\u9fa5' }
            han = han.removePrefix("姓名").removePrefix("姓").removePrefix("名")
            if (han.length in 2..4 && han.none { it in NAME_STOP_CHARS }) return han
        }
        return null
    }

    /** 出现在姓名/性别后面的标签，用来截断取值区域 */
    private val STOP_LABELS = listOf(
        "性别", "民族", "出生", "住址", "公民身份号码", "公民", "身份号码", "签发机关", "有效期限",
    )

    /**
     * 取锚点之后、下一个标签之前的那一段。
     * 卡片上的标签和值经常跨行，甚至被切成「姓」「名」两行，所以不能简单用「姓名 + 空格 + 单词」的正则。
     */
    private fun segmentAfter(text: String, anchor: Regex, window: Int = 40): String? {
        val m = anchor.find(text) ?: return null
        val start = m.range.last + 1
        if (start >= text.length) return null
        val hardEnd = minOf(text.length, start + window)
        val nextLabel = STOP_LABELS
            .mapNotNull { label -> text.indexOf(label, start).takeIf { it in (start + 1) until hardEnd } }
            .minOrNull()
        val end = nextLabel ?: hardEnd
        return text.substring(start, end)
    }

    /** 全角转半角，压掉零宽字符。 */
    fun normalizeText(raw: String): String {
        val sb = StringBuilder()
        for (ch in raw) {
            val c = when (ch) {
                in '０'..'９' -> '0' + (ch - '０')
                in 'Ａ'..'Ｚ' -> 'A' + (ch - 'Ａ')
                in 'ａ'..'ｚ' -> 'a' + (ch - 'ａ')
                '\uFF1A' -> ':'
                '\u3000' -> ' '
                '\u200B', '\uFEFF' -> ' '
                else -> ch
            }
            sb.append(c)
        }
        return sb.toString()
    }

    fun parse(raw: String): CardFields {
        val text = normalizeText(raw)

        // 号码：ML Kit 经常把长数字从中间拆开（"3202191962081 17033"），
        // 所以先在去掉所有空白的副本上找，再去掉空白重试。
        val compact = text.replace(Regex("""\s+"""), "")
        val number = NUMBER_RE.find(compact)?.value?.let { IdCard.normalizeNumber(it) }

        // 姓名：锚点之后逐字扫，直到撞上下一个字段
        val name = extractName(text)

        // 性别：锚点之后到下一个标签之间，看有没有「男」或「女」
        val gender = segmentAfter(text, GENDER_ANCHOR, window = 12)?.let { seg ->
            when {
                seg.contains('男') -> IdCard.GENDER_MALE
                seg.contains('女') -> IdCard.GENDER_FEMALE
                else -> null
            }
        }

        // 出生：卡片上只有这一处「xxxx年x月x日」，直接全局找
        val birth = BIRTH_RE.find(text)?.let { m ->
            val y = m.groupValues[1]
            val mo = m.groupValues[2].padStart(2, '0')
            val d = m.groupValues[3].padStart(2, '0')
            val s = "$y$mo$d"
            s.takeIf { IdCard.isValidDate(it) }
        }

        return CardFields(name = name, gender = gender, birth = birth, number = number)
    }

    /** 多数票；并列或全空时返回 null。 */
    private fun vote(values: List<String?>): String? {
        val nonNull = values.filterNotNull()
        if (nonNull.isEmpty()) return null
        val counts = nonNull.groupingBy { it }.eachCount()
        val best = counts.maxByOrNull { it.value } ?: return null
        val tie = counts.values.count { it == best.value } > 1
        return if (tie) null else best.key
    }

    /**
     * 合并三个通道。
     *
     * 判定优先级：
     * 1. 号码先过校验码——校验码是数学硬证据，比"三次一致"更硬；
     * 2. 三个通道读数不一致时，若只有一个候选能过校验码，就采信它，并留一句说明；
     * 3. 姓名允许 2 比 1 的多数票，但必须是严格多数（并列不行）；
     * 4. 卡面读到的出生/性别若存在，就必须与号码一致（漏检不算冲突）。
     */
    fun merge(channelTexts: List<String>): MergeResult {
        val parsed = channelTexts.map { parse(it) }
        val issues = mutableListOf<String>()
        val notes = mutableListOf<String>()
        var autoOk = true

        // ---------- 号码 ----------
        val rawNumbers = parsed.map { it.number }.filterNotNull()
        val distinctNumbers = rawNumbers.distinct()
        var number: String? = null

        when {
            distinctNumbers.isEmpty() -> {
                issues += "没读出身份证号"
                autoOk = false
            }
            distinctNumbers.size == 1 -> {
                number = distinctNumbers[0]
                if (rawNumbers.size == 1 && channelTexts.size > 1) {
                    notes += "只有一次读到了号码，已按校验位核对"
                }
            }
            else -> {
                val validOnes = distinctNumbers.filter { IdCard.validate(it).isEmpty() }
                if (validOnes.size == 1) {
                    number = validOnes[0]
                    notes += "三次读得不完全一样，按校验位取了唯一对得上的那个"
                } else {
                    number = distinctNumbers.maxByOrNull { n -> rawNumbers.count { it == n } }
                    issues += "三次读出来的号码不一样，而且都没通过校验"
                    autoOk = false
                }
            }
        }

        if (number != null) {
            val v = IdCard.validate(number)
            if (v.isNotEmpty()) {
                issues += v
                autoOk = false
            }
        }

        // ---------- 姓名 ----------
        val rawNames = parsed.map { it.name }.filterNotNull()
        val nameCounts = rawNames.groupingBy { it }.eachCount()
        val name: String?
        when {
            nameCounts.isEmpty() -> {
                name = null
                issues += "没读出姓名"
                autoOk = false
            }
            nameCounts.size == 1 -> {
                name = nameCounts.keys.first()
            }
            else -> {
                val sorted = nameCounts.entries.sortedByDescending { it.value }
                val top = sorted[0]
                val tie = sorted.size > 1 && sorted[1].value == top.value
                if (!tie && top.value >= 2) {
                    name = top.key
                    notes += "三次读出来的姓名不完全一样，按多数取了「${top.key}」"
                } else {
                    name = null
                    issues += "三次读出来的姓名不一样"
                    autoOk = false
                }
            }
        }

        // ---------- 出生 / 性别 交叉核对 ----------
        var birth: String?
        var gender: String?
        val cardBirth = vote(parsed.map { it.birth })
        val cardGender = vote(parsed.map { it.gender })

        if (number != null) {
            // 内部统一用 yyyyMMdd，避免 "1962-08-11" 与 "19620811" 比不上的坑
            birth = IdCard.birthFromNumber(number)?.replace("-", "")
            gender = IdCard.genderFromNumber(number)

            if (cardBirth != null && birth != null && cardBirth != birth) {
                issues += "号码里的生日和卡片上印的生日对不上"
                autoOk = false
            }
            if (cardGender != null && gender != null && cardGender != gender) {
                issues += "号码算出来是${IdCard.genderLabel(gender)}，卡片上印的是${IdCard.genderLabel(cardGender)}"
                autoOk = false
            }
        } else {
            birth = cardBirth
            gender = cardGender
        }

        return MergeResult(
            fields = CardFields(name = name, gender = gender, birth = birth, number = number),
            autoOk = autoOk,
            issues = issues,
            notes = notes,
            channelNumbers = rawNumbers,
            channelCount = channelTexts.size,
        )
    }
}
