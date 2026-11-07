package eu.kanade.tachiyomi.extension.en.eahentai

import java.text.Normalizer

internal object ContentBlocklist {
    private val blocked = setOf(
        "lolicon", "shotacon", "loli", "shota", "child", "children", "childlike",
        "kid", "kids", "underage", "minor", "preteen", "tween", "juvenile", "toddler",
        "infant", "baby", "young", "youth", "adolescent", "school", "schoolboy",
        "schoolgirl", "school uniform", "school swimsuit", "elementary", "grade school",
        "primary school", "middle school", "junior high", "kindergarten", "preschool",
        "randoseru", "age play", "ageplay", "age regression", "under 18", "little girl",
        "little boy", "little sister", "little brother", "younger sister", "younger brother",
        "young girl", "young boy", "shougakusei", "shogakusei", "chugakusei",
        "chuugakusei", "youchienji", "youchien", "youji", "kodomo", "jidou",
        "imouto", "otouto", "shoujo", "shojo", "shounen", "shonen", "js", "jc", "jk",
        "幼女", "少女", "少年", "小学生", "中学生", "幼児", "園児", "幼稚園",
        "ロリ", "ショタ", "妹", "弟",
    ).map(::normalize).toSet()

    private val titlePattern = Regex(
        """(?iu)(?<![\p{L}\p{N}])(?:lolicon|shotacon|loli|shota|school|underage|minor|pre[ -]?teen|tween|juvenile|toddler|infant|kindergarten|preschool|elementary|grade\s+school|primary\s+school|middle\s+school|junior\s+high|child(?:ren|like)?|kids?|baby|teen(?:age|ager)?|little\s+(?:girl|boy|sister|brother)|young\s+(?:girl|boy)|younger\s+(?:sister|brother)|shougakusei|shogakusei|chugakusei|chuugakusei|youchienji|youchien|youji|kodomo|jidou|imouto|otouto|shoujo|shojo|shounen|shonen|age[ -]*(?:play|regression)|under\s+18)(?![\p{L}\p{N}])""",
    )
    private val japanesePattern = Regex("幼女|少女|少年|小学生|中学生|幼児|園児|幼稚園|ロリ|ショタ|妹|弟")
    private val abbreviationPattern = Regex("(?iu)(?<![\p{L}\p{N}])(?:JS|JC|JK)(?![\p{L}\p{N}])")
    private val agePattern = Regex("(?iu)(?<![\p{L}\p{N}])(?:[0-9]|1[0-7])[\\s-]*(?:yo|y/o|years?[\\s-]*old|year[\\s-]*old)(?![\p{L}\p{N}])")

    fun blocks(title: String, tags: Iterable<String>): Boolean {
        if (title.isBlank()) return true
        if (titlePattern.containsMatchIn(title) || japanesePattern.containsMatchIn(title) ||
            abbreviationPattern.containsMatchIn(title) || agePattern.containsMatchIn(title)
        ) return true
        return tags.any { tag ->
            normalize(tag) in blocked || titlePattern.containsMatchIn(tag) ||
                japanesePattern.containsMatchIn(tag) || abbreviationPattern.containsMatchIn(tag) ||
                agePattern.containsMatchIn(tag)
        }
    }

    private fun normalize(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFKC)
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
}
