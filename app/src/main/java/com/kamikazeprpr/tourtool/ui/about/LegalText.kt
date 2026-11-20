package com.kamikazeprpr.tourtool.ui.about

/**
 * 「关于」页里所有长文本集中在这，改文案不用翻界面代码。
 *
 * 隐私声明与首次启动那一屏共用同一份，避免两处说法不一致——
 * 那种不一致在合规上就是漏洞。
 */
object LegalText {

    const val APP_NAME = "旅游证件登记"
    const val DEVELOPER = "无瑕"

    // ---------- 个人信息处理与使用声明 ----------

    const val PRIVACY_TITLE = "个人信息处理与使用声明"

    val PRIVACY_PARAGRAPHS = listOf(
        "本应用仅从用户经被采集人同意后拍摄或选取的证件照片中，收集姓名、性别、公民身份号码、" +
            "出生日期。处理目的仅限为游客办理出行登记、统一投保旅游意外险及出行时核验身份，不得另作他用。",
        "公民身份号码、出生日期依《个人信息保护法》第二十八条按敏感个人信息处理。" +
            "个人信息处理者应事先向被采集人告知处理的必要性及对个人权益的影响，并取得其单独同意。",
        "个人信息均在本设备本地离线处理，仅存于应用私有目录，不上传任何服务器、不进行云备份，" +
            "不作自动化决策或画像。不向任何第三方提供、转让或公开披露；" +
            "严禁出售、非法买卖、非法向他人提供个人信息。",
        "仅连接 GitHub 相关域名检查、下载应用更新，不经该等连接传输任何个人信息。" +
            "本地运行日志不含姓名及完整证件号码，保存 7 日后自动删除。" +
            "个人信息的保存期限截至用户主动删除之时；用户可删除单个团或全部数据，删除后不可恢复，" +
            "已导出的表格文件不因本机删除而失效。",
        "用户及被采集人有权查阅、复制、更正、删除其个人信息及撤回同意，" +
            "撤回不影响此前基于同意已进行的处理。" +
            "用户不得利用本应用从事违法违规活动，包括但不限于冒用他人身份、伪造变造证件、" +
            "未经同意收集他人个人信息、出售或非法提供他人个人信息。",
    )

    // ---------- 免责声明 ----------

    const val DISCLAIMER_TITLE = "免责声明"

    val DISCLAIMER_PARAGRAPHS = listOf(
        "本软件按「现状」（AS IS）及「现有」（AS AVAILABLE）基础提供。在适用法律允许的最大范围内，" +
            "开发者不就本软件的适用性、准确性、完整性、可靠性、无中断性或无错误作出任何明示或默示的保证，" +
            "并明确排除对适销性、特定用途适用性及不侵权的默示保证。",
        "本软件内置的文字识别（OCR）功能输出的识别结果，仅为技术辅助信息，可能存在识别偏差、遗漏或错误，" +
            "不构成对证件真实性、有效性或持证人身份的任何证明。" +
            "使用者应对识别结果逐项核验，并以证件原件记载为准。" +
            "因未履行核验义务而导致的登记错误、出行受阻、保险理赔争议或其他不利后果，由使用者自行承担。",
        "使用者应确保其收集、存储、使用及对外提供个人信息的行为，已取得被采集人充分、有效且明示的同意，" +
            "并符合其所在地法律法规及行业规范的要求。" +
            "使用者违反前述义务所引起的一切法律责任与损害后果，由使用者独立承担，" +
            "开发者不承担任何连带责任、补充责任或替代责任。",
        "在适用法律允许的最大范围内，开发者不对因使用或无法使用本软件而导致的任何直接、间接、附带、特殊、" +
            "惩罚性或后果性损害承担责任，包括但不限于利润损失、商誉损失、数据丢失、业务中断或其他经济损失，" +
            "即使开发者已被告知该等损害发生的可能性。",
        "本软件通过公开渠道分发。开发者不保证任何第三方下载来源的可信性与完整性，" +
            "使用者应自行验证安装包的数字签名与校验值。" +
            "因使用非官方渠道获取的安装包所导致的一切后果，开发者不承担责任。",
        "本声明的订立、效力、解释、履行及争议解决，均适用中华人民共和国法律（为本声明之目的，" +
            "不包括香港特别行政区、澳门特别行政区及台湾地区法律）。" +
            "因本声明引起的或与之相关的任何争议，双方应首先友好协商解决；" +
            "协商不成的，任何一方均可向开发者住所地有管辖权的人民法院提起诉讼。",
        "开发者保留随时修订本声明的权利。修订后的声明自本软件相应新版本发布之日起生效，" +
            "使用者继续使用即视为接受修订后的声明。",
    )

    // ---------- 开源许可 ----------

    const val LICENSE_TITLE = "开源许可"

    const val MIT_TITLE = "MIT License"

    val MIT_TEXT = """
        Copyright (c) 2026 无瑕

        Permission is hereby granted, free of charge, to any person obtaining a copy
        of this software and associated documentation files (the "Software"), to deal
        in the Software without restriction, including without limitation the rights
        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
        copies of the Software, and to permit persons to whom the Software is
        furnished to do so, subject to the following conditions:

        The above copyright notice and this permission notice shall be included in all
        copies or substantial portions of the Software.

        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
        IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
        FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
        AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
        LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
        OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
        SOFTWARE.
    """.trimIndent()

    /** 第三方组件：名称 / 许可 / 用途 */
    val THIRD_PARTY = listOf(
        Triple("AndroidX / Jetpack Compose", "Apache License 2.0", "界面框架"),
        Triple("AndroidX Room", "Apache License 2.0", "本地数据库"),
        Triple("ML Kit Text Recognition (Chinese)", "Apache License 2.0", "离线文字识别"),
        Triple("Kotlin & Coroutines", "Apache License 2.0", "编程语言与并发"),
        Triple("AndroidX Activity / Lifecycle", "Apache License 2.0", "页面与生命周期"),
    )

    // ---------- 更新日志 ----------

    const val CHANGELOG_TITLE = "更新日志"

    data class Release(val version: String, val date: String, val notes: List<String>)

    val CHANGELOG = listOf(
        Release(
            version = "0.1.0",
            date = "2026-11-20",
            notes = listOf(
                "第一个可用版本",
                "从相册批量选身份证照片，本机离线识别姓名、性别、号码、出生日期",
                "三通道识别 + 校验码、日期码、地区码、性别生日交叉四道校验",
                "每个人可补填手机号，11 位校验，号码显示在名单上",
                "长按成员打标签，每种标签独立底色，可现场新建",
                "长按团条目改名、加起止日期、写备注、删除整团",
                "导出 Excel / 纯文本：列可选可排序、实时预览、保存到下载文件夹或直发微信",
                "本机脱敏日志，姓名与完整号码不落盘，7 天自动清理",
                "从 GitHub 检查更新，下载后过 sha256、包名、签名三道校验",
                "首次启动的个人信息处理声明",
            ),
        ),
    )
}
