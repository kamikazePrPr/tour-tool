package com.kamikazeprpr.tourtool.core

/**
 * 身份证号的结构校验与卡面文本解析。
 *
 * 这里不依赖任何 Android API，可以直接在 JVM 上跑单元测试。
 * 校验码算法见 GB 11643-1999 附录 B：加权因子 Wi = 2^(18-i) mod 11，
 * 加权和 mod 11 查表 "10X98765432" 得到第 18 位。
 */
object IdCard {

    private val WEIGHTS = intArrayOf(7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
    private const val CHECK_TABLE = "10X98765432"

    /** 省级行政区代码前两位 */
    private val PROVINCE_CODES = setOf(
        "11", "12", "13", "14", "15",
        "21", "22", "23",
        "31", "32", "33", "34", "35", "36", "37",
        "41", "42", "43", "44", "45", "46",
        "50", "51", "52", "53", "54",
        "61", "62", "63", "64", "65",
        "71", "81", "82", "91",
    )

    /** 计算第 18 位校验码；前 17 位不合法时返回 null。 */
    fun checkDigit(first17: String): Char? {
        if (first17.length != 17) return null
        if (first17.any { it !in '0'..'9' }) return null
        var sum = 0
        for (i in 0 until 17) sum += (first17[i] - '0') * WEIGHTS[i]
        return CHECK_TABLE[sum % 11]
    }

    /**
     * 校验一个 18 位号码。返回空列表表示全部通过；否则每一条都是一句能给人看的话。
     */
    fun validate(raw: String): List<String> {
        val id = normalizeNumber(raw)
        val issues = mutableListOf<String>()

        if (id.length != 18) {
            issues += "号码不是 18 位（读到 ${id.length} 位）"
            return issues
        }
        if (id.take(17).any { it !in '0'..'9' }) {
            issues += "号码前 17 位里有不是数字的字符"
            return issues
        }
        if (id[17] !in '0'..'9' && id[17] != 'X') {
            issues += "号码最后一位既不是数字也不是 X"
        }
        val expected = checkDigit(id.take(17))
        if (expected != null && expected != id[17]) {
            issues += "号码校验位对不上"
        }
        if (!isValidDate(id.substring(6, 14))) {
            issues += "号码里的出生日期不是真实日期"
        }
        if (id.take(2) !in PROVINCE_CODES) {
            issues += "号码开头的地区代码不认识"
        }
        return issues
    }

    /** 全角转半角，去掉空格与不可见字符，末位字母转大写。 */
    fun normalizeNumber(raw: String): String {
        val sb = StringBuilder()
        for (ch in raw) {
            val c = when (ch) {
                in '０'..'９' -> '0' + (ch - '０')
                in 'Ａ'..'Ｚ' -> 'A' + (ch - 'Ａ')
                in 'ａ'..'ｚ' -> 'a' + (ch - 'ａ')
                else -> ch
            }
            if (c == 'x' || c == 'X') sb.append('X')
            else if (c in '0'..'9') sb.append(c)
        }
        return sb.toString()
    }

    fun isValidDate(yyyymmdd: String): Boolean {
        if (yyyymmdd.length != 8 || yyyymmdd.any { it !in '0'..'9' }) return false
        val y = yyyymmdd.substring(0, 4).toIntOrNull() ?: return false
        val m = yyyymmdd.substring(4, 6).toIntOrNull() ?: return false
        val d = yyyymmdd.substring(6, 8).toIntOrNull() ?: return false
        if (y !in 1900..2100 || m !in 1..12 || d !in 1..31) return false
        val len = when (m) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            else -> if (isLeap(y)) 29 else 28
        }
        return d <= len
    }

    private fun isLeap(y: Int) = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0

    /** 从号码推出生日期 `yyyy-MM-dd`。 */
    fun birthFromNumber(id: String): String? {
        val n = normalizeNumber(id)
        if (n.length != 18) return null
        val d = n.substring(6, 14)
        if (!isValidDate(d)) return null
        return "${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}"
    }

    /** 从号码推性别：第 17 位奇数=男，偶数=女。 */
    fun genderFromNumber(id: String): String? {
        val n = normalizeNumber(id)
        if (n.length != 18) return null
        val c = n[16]
        if (c !in '0'..'9') return null
        return if ((c - '0') % 2 == 1) GENDER_MALE else GENDER_FEMALE
    }

    const val GENDER_MALE = "M"
    const val GENDER_FEMALE = "F"

    fun genderLabel(g: String?): String = when (g) {
        GENDER_MALE -> "男"
        GENDER_FEMALE -> "女"
        else -> ""
    }
}
