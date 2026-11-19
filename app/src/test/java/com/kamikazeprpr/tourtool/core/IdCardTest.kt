package com.kamikazeprpr.tourtool.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdCardTest {

    // 真实样本：320219196208117033（徐建清，1962-08-11，男）
    private val sample = "320219196208117033"

    @Test
    fun checkDigit_matchesKnownSample() {
        assertEquals('3', IdCard.checkDigit("32021919620811703"))
        assertEquals('X', IdCard.checkDigit("11010519491231002"))
    }

    @Test
    fun validate_acceptsRealNumber() {
        assertEquals(emptyList<String>(), IdCard.validate(sample))
    }

    @Test
    fun validate_rejectsWrongCheckDigit() {
        val issues = IdCard.validate("320219196208117034")
        assertTrue(issues.any { it.contains("校验位") })
    }

    @Test
    fun validate_rejectsWrongLength() {
        val issues = IdCard.validate("32021919620811703")
        assertTrue(issues.any { it.contains("不是 18 位") })
    }

    @Test
    fun validate_rejectsLetterInsteadOfDigit() {
        val issues = IdCard.validate("3202191962O8117033")
        assertTrue(issues.isNotEmpty())
    }

    @Test
    fun derivedFields_matchCard() {
        assertEquals("1962-08-11", IdCard.birthFromNumber(sample))
        assertEquals(IdCard.GENDER_MALE, IdCard.genderFromNumber(sample))
    }

    private val cardText = """
        姓名 徐建清
        性别 男   民族 汉
        出生 1962 年 8 月 11 日
        住址 江苏省江阴市澄江镇璜塘上村末甲里36号
        公民身份号码 320219196208117033
    """.trimIndent()

    @Test
    fun parse_extractsAllFields() {
        val f = CardParser.parse(cardText)
        assertEquals("徐建清", f.name)
        assertEquals(IdCard.GENDER_MALE, f.gender)
        assertEquals("19620811", f.birth)
        assertEquals(sample, f.number)
    }

    @Test
    fun merge_threeIdenticalChannels_isAutoOk() {
        val r = CardParser.merge(listOf(cardText, cardText, cardText))
        assertTrue(r.issues.toString(), r.autoOk)
        assertEquals("徐建清", r.fields.name)
        assertEquals(sample, r.fields.number)
    }

    @Test
    fun merge_numberDisagree_resolvedByCheckDigit() {
        // 一个通道把末位读错（校验位不成立），另外两个是对的 → 按校验位采信正确的那个
        val wrong = cardText.replace(sample, "320219196208117034")
        val r = CardParser.merge(listOf(cardText, cardText, wrong))
        assertTrue(r.issues.toString(), r.autoOk)
        assertEquals(sample, r.fields.number)
        assertTrue(r.notes.any { it.contains("校验位") })
    }

    @Test
    fun merge_numberDisagree_allInvalid_needsReview() {
        // 三个通道各读错一位，没有一个能通过校验位 → 仲裁不了，必须人工
        val a = cardText.replace(sample, "320219196208117034")
        val b = cardText.replace(sample, "320219196208117035")
        val c = cardText.replace(sample, "320219196208117036")
        val r = CardParser.merge(listOf(a, b, c))
        assertFalse(r.autoOk)
        assertTrue(r.issues.any { it.contains("号码不一样") })
    }

    @Test
    fun merge_nameMajority_accepted() {
        // 三通道里有两份姓名一致、第三份把「清」读成「青」→ 多数票采信
        val typo = cardText.replace("徐建清", "徐建青")
        val r = CardParser.merge(listOf(cardText, cardText, typo))
        assertTrue(r.issues.toString(), r.autoOk)
        assertEquals("徐建清", r.fields.name)
        assertTrue(r.notes.any { it.contains("多数") })
    }

    @Test
    fun merge_nameTie_needsReview() {
        val other = cardText.replace("徐建清", "徐建青")
        val third = cardText.replace("徐建清", "徐建明")
        val r = CardParser.merge(listOf(cardText, other, third))
        assertFalse(r.autoOk)
        assertTrue(r.issues.any { it.contains("姓名不一样") })
    }

    @Test
    fun merge_genderConflict_needsReview() {
        val wrongGender = cardText.replace("性别 男", "性别 女")
        val r = CardParser.merge(listOf(wrongGender, wrongGender, wrongGender))
        assertFalse(r.autoOk)
        assertTrue(r.issues.any { it.contains("男") && it.contains("女") })
    }

    @Test
    fun merge_birthConflict_needsReview() {
        val wrongBirth = cardText.replace("1962 年 8 月 11 日", "1962 年 9 月 11 日")
        val r = CardParser.merge(listOf(wrongBirth, wrongBirth, wrongBirth))
        assertFalse(r.autoOk)
        assertTrue(r.issues.any { it.contains("生日") })
    }

    @Test
    fun parse_numberSplitBySpace() {
        // 真实 OCR 输出：长号码中间被塞了一个空格
        val t = "公民身份号码 3202191962081 17033"
        assertEquals(sample, CardParser.parse(t).number)
    }

    @Test
    fun parse_numberSplitIntoThreeParts() {
        val t = "公民身份号码32021919501 1 168019"
        assertEquals("320219195011168019", CardParser.parse(t).number)
    }

    @Test
    fun parse_nameWhenLabelLosesLeftChar() {
        // 真实 OCR 输出：「姓」被读丢，只剩「名」
        val t = "名吴建华 \n性别女 |天族汉 \n|出生1958 年3月22日 \n公民身份号码 320219195803227021"
        assertEquals("吴建华", CardParser.parse(t).name)
    }

    @Test
    fun parse_nameWhenValueFallsToNextLine() {
        // 真实 OCR 输出：标签与值分两行
        val t = "姓名 \n毕洪兴 \n性别男 \n民族汉 \n出生1954年4月16日"
        assertEquals("毕洪兴", CardParser.parse(t).name)
    }

    @Test
    fun parse_nameWhenGenderLabelIsBroken() {
        // 「性别」被读成「性男」，名字后面立刻跟破碎标签
        val t = "|名戴洪兴 \n性男 (民族汉 \n出生1958 年12月6日"
        assertEquals("戴洪兴", CardParser.parse(t).name)
    }

    @Test
    fun merge_missingGenderOnCard_fallsBackToNumber() {
        val noGender = cardText.replace("性别 男", "")
        val r = CardParser.merge(listOf(noGender, noGender, noGender))
        assertTrue(r.issues.toString(), r.autoOk)
        assertEquals(IdCard.GENDER_MALE, r.fields.gender)
    }
}
