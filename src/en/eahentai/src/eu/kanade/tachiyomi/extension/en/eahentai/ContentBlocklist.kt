package eu.kanade.tachiyomi.extension.en.eahentai

import java.text.Normalizer

/** Conservative denylist for child-coded, underage, and school-age gallery metadata. */
internal object ContentBlocklist {
    private val blockedTags = setOf(
        "lolicon", "shotacon", "loli", "shota", "child", "children", "childlike",
        "kid", "kids", "underage", "minor", "preteen", "pre teen", "tween",
        "juvenile", "toddler", "infant", "baby", "young", "youth", "adolescent",
        "school", "schoolboy", "schoolgirl", "school student", "school uniform",
        "school swimsuit", "elementary", "grade school", "primary school",
        "middle school", "junior high", "kindergarten", "preschool", "randoseru",
        "age play", "ageplay", "age regression", "under 18", "little girl",
        "little boy", "little sister", "little brother", "younger sister",
        "younger brother", "young girl", "young boy",
        "shougakusei", "shogakusei", "chugakusei", "chuugakusei",
        "youchienji", "youchien", "youji", "kodomo", "jidou", "imouto",
        "otouto", "shoujo", "shojo", "shounen", "shonen",
        "\u5e7c\u5973", "\u5c11\u5973", "\u5c11\u5e74", "\u5c0f\u5b66\u751f",
        "\u4e2d\u5b66\u751f", "\u5e7c\u5150", "\u5712\u5150",
        "\u5e7c\u7a1a\u5712", "\u30ed\u30ea", "\u30b7\u30e7\u30bf",
        "\u59b9", "\u5f1f",
        "js", "jc", "jk",
    ).map(::normalize).toSet()

    private val blockedTitlePattern = Regex(
        """(?iu)(?<![\p{L}\p{N}])(?:lolicon|shotacon|loli|shota|schoolgirl|schoolboy|school|underage|minor|pre[ -]?teen|tween|juvenile|toddler|infant|kindergarten|preschool|elementary(?: school)?|grade school|primary school|middle school|junior high|child(?:ren|like)?|kids?|baby|little (?:girl|boy|sister|brother)|young (?:girl|boy)|younger (?:sister|brother)|teen(?:age|ager)?|shougakusei|shogakusei|chugakusei|chuugakusei|youchienji|youchien|youji|kodomo|jidou|imouto|otouto|shoujo|shojo|shounen|shonen|age[ -]*(?:play|regression)|under\s*18|js|jc|jk)(?![\p{L}\p{N}])""",
    )

    private val japaneseUnderagePattern = Regex(
        """(?:\u5e7c\u5973|\u5c11\u5973|\u5c11\u5e74|\u5c0f\u5b66\u751f|\u4e2d\u5b66\u751f|\u5e7c\u5150|\u5712\u5150|\u5e7c\u7a1a\u5712|\u30ed\u30ea|\u30b7\u30e7\u30bf|\u59b9|\u5f1f)""",
    )

    fun blocks(title: String, tags: List<String>): Boolean {
        val normalizedTitle = normalize(title)
        if (blockedTitlePattern.containsMatchIn(normalizedTitle) ||
            japaneseUnderagePattern.containsMatchIn(title)
        ) return true

        return tags.any { tag ->
            val normalized = normalize(tag)
            normalized in blockedTags ||
                blockedTags.any { blocked ->
                    blocked.length > 2 && normalized.contains(blocked)
                }
        }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
}
