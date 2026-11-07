package eu.kanade.tachiyomi.extension.en.eahentai

import java.text.Normalizer

/** Conservative denylist for child-coded, underage, and school-age gallery metadata. */
internal object ContentBlocklist {
    private val blockedTags = setOf(
        // Explicit child-coded categories.
        "lolicon", "shotacon", "loli", "shota", "child", "children", "childlike", "child like",
        "kid", "kids", "underage", "minor", "preteen", "pre teen", "tween", "juvenile",
        "toddler", "infant", "baby", "young", "youth", "adolescent", "adolescence",
        // School / school-age categories.
        "school", "schoolboy", "schoolgirl", "school student", "school students",
        "school uniform", "school gym uniform", "schoolgirl uniform", "school swimsuit", "school age",
        "elementary", "elementary school", "elementary student", "grade school", "primary school", "middle school", "middle school student",
        "junior high", "junior high school", "junior high student", "high school girl", "high school boy",
        "kindergarten", "preschool", "pre school", "randoseru",
        // Age-play / age-regression and related labels.
        "age play", "ageplay", "age regression", "under 18", "under 18s", "little girl", "little boy",
        "little sister", "little brother", "younger sister", "younger brother", "young girl", "young boy",
        // Romanized Japanese tags and character terms.
        "shougakusei", "shogakusei", "chugakusei", "chuugakusei", "youchienji", "youchien",
        "youji", "kodomo", "jidou", "imouto", "otouto", "shoujo", "shojo", "shounen", "shonen",
        // Japanese tag spellings.
        "å¹¼å¥³", "å°‘å¥³", "å°‘å¹´", "å°å­¦ç”Ÿ", "ä¸­å­¦ç”Ÿ", "å¹¼å…", "åœ’å…", "å¹¼ç¨šåœ’", "ãƒ­ãƒª", "ã‚·ãƒ§ã‚¿", "å¦¹", "å¼Ÿ",
        // Short labels used in titles / tags for school-year groups.
        "js", "jc", "jk",
    ).map(::normalize).toSet()

    private val blockedTitlePattern = Regex(
        listOf(
            """(?iu)(?<![\p{L}\p{N}])(?:lolicon|shotacon|loli|shota|schoolgirl|schoolboy|school|school\s+swimsuit|randoseru|""",
            """underage|minor|pre[ -]?teen|tween|juvenile|toddler|infant|kindergarten|preschool|pre[ -]?school|""",
            """elementary(?:\s+school)?|grade\s+school|primary\s+school|middle\s+school|junior\s+high|high\s+school\s+(?:girl|boy|student)|""",
            """child(?:ren|like)?|kid(?:s)?|baby|little\s+(?:girl|boy|sister|brother)|young\s+(?:girl|boy)|younger\s+(?:sister|brother)|""",
            """teen(?:age|ager)?|shougakusei|shogakusei|chugakusei|chuugakusei|youchienji|youchien|youji|kodomo|jidou|imouto|otouto|""",
            """shoujo|shojo|shounen|shonen|joshi\s+shougakusei|age[ -]*(?:play|regression)|under\s+18|å¹¼å¥³|å°‘å¥³|å°‘å¹´|å°å­¦ç”Ÿ|ä¸­å­¦ç”Ÿ|å¹¼å…|åœ’å…|å¹¼ç¨šåœ’|ãƒ­ãƒª|ã‚·ãƒ§ã‚¿|å¦¹|å¼Ÿ)(?![\p{L}\p{N}])""",
        ).joinToString(separator = ""),
    )

    private val japaneseUnderagePattern = Regex("å¹¼å¢–Íó–ÂG––Íó–ÂG–æÑó–Â?–¶›R}ó’â·–¶›R}ó–æó–Aó–rK–Aó–æó¢k–rIó·©ó
ßŸ
ıó–šåó–ò|ˆ¤((€€€ÁÉ¥Ù…Ñ”Ù…°Í¡½½±‰‰É•Ù¥…Ñ¥½¹A…ÑÑ•É¸€ôI••à (€€€€€€€€ˆ ı¥Ô¤ üğ…mqqÁí1õqqÁí9õt¤ üé)Mñ)ñ),¤ ü…mqqÁí1õqqÁí9õt¤ˆ°(€€€€¤((€€€ÁÉ¥Ù…Ñ”Ù…°Õ¹‘•É…••A…ÑÑ•É¸€ôI••à (€€€€€€€€ˆ ı¥Ô¤ üğ…mqqÁí1õqqÁí9õt¤ üélÀ´åuğÅlÀ´İt¥mqqÌµt¨ üéå½ñä½½ñå•…ÉÌımqqÌµt©½±‘ñå•…ÉmqqÌµt©½±¤ ü…mqqÁí1õqqÁí9õt¤ˆ°(€€€€¤((€€€™Õ¸‰±½­Ì¡Ñ¥Ñ±”èMÑÉ¥¹œ°Ñ…Ìè%Ñ•É…‰±”ñMÑÉ¥¹œø¤è	½½±•…¸ì(€€€€€€€¥˜€¡Ñ¥Ñ±”¹¥Í	±…¹¬ ¤¤É•ÑÕÉ¸ÑÉÕ”(€€€€€€€¥˜€¡‰±½­•‘Q¥Ñ±•A…ÑÑ•É¸¹½¹Ñ…¥¹Í5…Ñ¡%¸¡Ñ¥Ñ±”¤¤É•ÑÕÉ¸ÑÉÕ”(€€€€€€€¥˜€¡©…Á…¹•Í•U¹‘•É…•A…ÑÑ•É¸¹½¹Ñ…¥¹Í5…Ñ¡%¸¡Ñ¥Ñ±”¤¤É•ÑÕÉ¸ÑÉÕ”(€€€€€€€¥˜€¡Í¡½½±‰‰É•Ù¥…Ñ¥½¹A…ÑÑ•É¸¹½¹Ñ…¥¹Í5…Ñ¡%¸¡Ñ¥Ñ±”¤¤É•ÑÕÉ¸ÑÉÕ”(€€€€€€€¥˜€¡Õ¹‘•É…••A…ÑÑ•É¸¹½¹Ñ…¥¹Í5…Ñ¡%¸¡Ñ¥Ñ±”¤¤É•ÑÕÉ¸ÑÉÕ”((€€€€€€€É•ÑÕÉ¸Ñ…Ì¹…¹äìÑ…œ€´ø(€€€€€€€€€€€Ù…°¹½Éµ…±¥é•€ô¹½Éµ…±¥é”¡Ñ…œ¤(€€€€€€€€€€€¹½Éµ…±¥é•¥¸‰±½­•‘Q…Ìñğ(€€€€€€€€€€€€€€€‰±½­•‘Q¥Ñ±•A…ÑÑ•É¸¹½¹Ñ…¥¹Í5…Ñ¡%¸¡Ñ…œ¤ñğ(€€€€€€€€€€€€€€€©…Á…¹•Í•U¹‘•É…•A…ÑÑ•É¸¹½¹Ñ…¥¹Í5…Ñ¡%¸¡Ñ…œ¤ñğ(€€€€€€€€€€€€€€€Í¡½½±‰‰É•Ù¥…Ñ¥½¹A…ÑÑ•É¸¹½¹Ñ…¥¹Í5…Ñ¡%¸¡Ñ…œ¤ñğ(€€€€€€€€€€€€€€€Õ¹‘•É…••A…ÑÑ•É¸¹½¹Ñ…¥¹Í5…Ñ¡%¸¡Ñ…œ¤(€€€€€€€ô(€€€ô((€€€ÁÉ¥Ù…Ñ”™Õ¸¹½Éµ…±¥é”¡Ù…±Õ”èMÑÉ¥¹œ¤èMÑÉ¥¹œ€ô9½Éµ…±¥é•È(€€€€€€€€¹¹½Éµ…±¥é”¡Ù…±Õ”°9½Éµ…±¥é•È¹½É´¹9-¤(€€€€€€€€¹±½İ•É…Í” ¤(€€€€€€€€¹É•Á±…”¡I••à ‰myqqÁí1õqqÁí9õt¬ˆ¤°€ˆ€ˆ¤(€€€€€€€€¹ÑÉ¥´ ¤)ô(