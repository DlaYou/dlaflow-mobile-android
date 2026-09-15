package pl.dlaflow.mobile.core.text

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.text.Normalizer

private val blockTags = setOf(
    "address",
    "article",
    "aside",
    "blockquote",
    "center",
    "dd",
    "div",
    "dl",
    "dt",
    "fieldset",
    "figcaption",
    "figure",
    "footer",
    "form",
    "h1",
    "h2",
    "h3",
    "h4",
    "h5",
    "h6",
    "header",
    "hr",
    "li",
    "main",
    "nav",
    "ol",
    "p",
    "pre",
    "section",
    "table",
    "tbody",
    "tfoot",
    "thead",
    "tr",
    "ul",
)

private val tableCellTags = setOf("td", "th")

private val knownTags = blockTags + tableCellTags + setOf(
    "a",
    "abbr",
    "b",
    "bdi",
    "bdo",
    "br",
    "cite",
    "code",
    "del",
    "em",
    "font",
    "i",
    "img",
    "ins",
    "kbd",
    "mark",
    "q",
    "s",
    "small",
    "span",
    "strike",
    "strong",
    "sub",
    "sup",
    "time",
    "tt",
    "u",
    "var",
    "wbr",
)

private val ignoredTags = setOf(
    "canvas",
    "head",
    "iframe",
    "noembed",
    "noframes",
    "noscript",
    "object",
    "plaintext",
    "script",
    "style",
    "svg",
    "textarea",
    "template",
    "title",
    "video",
    "xmp",
)

private val voidIgnoredTags = setOf(
    "base",
    "col",
    "colgroup",
    "embed",
    "link",
    "meta",
    "param",
    "source",
    "track",
)

private val windows1252: Charset = Charset.forName("windows-1252")

private val namedEntities = mapOf(
    "aacute" to "á",
    "acirc" to "â",
    "acute" to "´",
    "aelig" to "æ",
    "agrave" to "à",
    "aring" to "å",
    "atilde" to "ã",
    "auml" to "ä",
    "aogon" to "ą",
    "amp" to "&",
    "apos" to "'",
    "brvbar" to "¦",
    "bull" to "•",
    "ccedil" to "ç",
    "cacute" to "ć",
    "cent" to "¢",
    "copy" to "©",
    "curren" to "¤",
    "deg" to "°",
    "divide" to "÷",
    "eacute" to "é",
    "ecirc" to "ê",
    "egrave" to "è",
    "eogon" to "ę",
    "eth" to "ð",
    "euml" to "ë",
    "euro" to "€",
    "frac12" to "½",
    "frac14" to "¼",
    "frac34" to "¾",
    "gt" to ">",
    "hellip" to "…",
    "iacute" to "í",
    "icirc" to "î",
    "iexcl" to "¡",
    "igrave" to "ì",
    "iquest" to "¿",
    "iuml" to "ï",
    "laquo" to "«",
    "lstrok" to "ł",
    "lt" to "<",
    "ldquo" to "“",
    "lsquo" to "‘",
    "macr" to "¯",
    "mdash" to "—",
    "micro" to "µ",
    "middot" to "·",
    "nbsp" to " ",
    "ndash" to "–",
    "not" to "¬",
    "ntilde" to "ñ",
    "nacute" to "ń",
    "oacute" to "ó",
    "ocirc" to "ô",
    "ograve" to "ò",
    "ordf" to "ª",
    "ordm" to "º",
    "oslash" to "ø",
    "otilde" to "õ",
    "ouml" to "ö",
    "para" to "¶",
    "plusmn" to "±",
    "pound" to "£",
    "quot" to "\"",
    "raquo" to "»",
    "rdquo" to "”",
    "reg" to "®",
    "rsquo" to "’",
    "sect" to "§",
    "shy" to "\u00AD",
    "sacute" to "ś",
    "szlig" to "ß",
    "sup2" to "²",
    "sup3" to "³",
    "thorn" to "þ",
    "times" to "×",
    "trade" to "™",
    "uacute" to "ú",
    "ucirc" to "û",
    "ugrave" to "ù",
    "uuml" to "ü",
    "yacute" to "ý",
    "yen" to "¥",
    "yuml" to "ÿ",
    "zacute" to "ź",
    "zdot" to "ż",
    "zwj" to "\u200D",
    "zwnj" to "\u200C",
    "newline" to "\n",
    "tab" to "\t",
)

private val mojibakeWeights = mapOf(
    'Ã' to 3,
    'Â' to 3,
    'Ä' to 3,
    'Å' to 3,
    'Æ' to 2,
    'Ð' to 3,
    'Ñ' to 3,
    'Ø' to 2,
    'Ù' to 2,
    'Ý' to 2,
    'Þ' to 2,
    'â' to 3,
    'ð' to 3,
    '�' to 5,
    '‚' to 1,
    '„' to 1,
    '†' to 1,
    '‡' to 1,
    '™' to 1,
)

/**
 * Normalizes provider message text for a plain Compose Text surface.
 *
 * Provider APIs can return either plain text, HTML fragments, or text that was
 * accidentally decoded as Windows-1252. This function keeps real Unicode,
 * removes markup without rendering tags, decodes entities, and preserves
 * readable line breaks.
 */
internal fun normalizeMessageText(value: String, compact: Boolean = false): String {
    var normalized = value
        .replace("\uFEFF", "")
        .trim()
    if (normalized.isBlank()) return ""

    repeat(3) {
        val decoded = decodeHtmlEntities(normalized)
        normalized = repairMojibake(decoded)
        if (normalized == decoded) return@repeat
    }

    normalized = stripHtml(normalized)
    repeat(2) {
        val decoded = decodeHtmlEntities(normalized)
        normalized = repairMojibake(decoded)
        if (normalized == decoded) return@repeat
    }

    normalized = Normalizer.normalize(normalized, Normalizer.Form.NFC)
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace('\u00A0', ' ')
        .replace(Regex("[\\t\\u000B\\f ]+"), " ")
        .replace(Regex("[ \\t]*\\n[ \\t]*"), "\n")
        .replace(Regex("\\n{2,}"), "\n")
        .trim()

    return if (compact) {
        normalized.replace(Regex("\\s+"), " ").trim().take(2000)
    } else {
        normalized.take(2000)
    }
}

internal fun normalizeMessageBodyText(value: String): String = normalizeMessageText(value)

internal fun normalizeMessagePreviewText(value: String): String = normalizeMessageText(value, compact = true)

private fun decodeHtmlEntities(value: String): String {
    var current = value
    repeat(3) {
        val decoded = decodeHtmlEntitiesOnce(current)
        if (decoded == current) return current
        current = decoded
    }
    return current
}

private fun decodeHtmlEntitiesOnce(value: String): String {
    val output = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        if (value[index] != '&') {
            output.append(value[index++])
            continue
        }

        val semicolon = value.indexOf(';', index + 1)
        if (semicolon < 0 || semicolon - index > 32) {
            output.append('&')
            index++
            continue
        }

        val token = value.substring(index + 1, semicolon)
        val decoded = decodeEntityToken(token)
        if (decoded == null) {
            output.append('&')
            index++
        } else {
            output.append(decoded)
            index = semicolon + 1
        }
    }
    return output.toString()
}

private fun decodeEntityToken(token: String): String? {
    if (token.startsWith("#x", ignoreCase = true)) {
        return token.substring(2).toIntOrNull(16)?.let(::codePointToString)
    }
    if (token.startsWith('#')) {
        return token.substring(1).toIntOrNull()?.let(::codePointToString)
    }
    return namedEntities[token.lowercase()]
}

private fun codePointToString(codePoint: Int): String? {
    if (codePoint !in 0..0x10FFFF || codePoint in 0xD800..0xDFFF) return null
    return String(Character.toChars(codePoint))
}

private fun stripHtml(value: String): String {
    val output = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        if (value.startsWith("<!--", index)) {
            val endComment = value.indexOf("-->", index + 4)
            index = if (endComment >= 0) endComment + 3 else value.length
            continue
        }
        if (value[index] != '<') {
            output.append(value[index++])
            continue
        }

        val end = findTagEnd(value, index + 1)
        if (end < 0) {
            val malformedTag = Regex("^<\\s*/?\\s*([A-Za-z][A-Za-z0-9:-]*)").find(value.substring(index))?.groupValues?.getOrNull(1)?.lowercase()
            if (malformedTag in ignoredTags) {
                break
            }
            output.append(value[index++])
            continue
        }

        val rawTag = value.substring(index, end + 1)
        if (rawTag.trimStart().startsWith("<!") || rawTag.trimStart().startsWith("<?")) {
            index = end + 1
            continue
        }
        val tagName = htmlTagName(rawTag)
        if (tagName == null) {
            output.append(rawTag)
            index = end + 1
            continue
        }

        val closing = rawTag.trimStart().startsWith("</")
        // A closing tag cannot carry user-visible content. Drop it even when
        // the provider invents a non-standard wrapper name.
        if (closing) {
            index = end + 1
            continue
        }
        if (tagName in ignoredTags) {
            val closingStart = indexOfClosingTag(value, tagName, end + 1)
            index = if (closingStart >= 0) {
                val closingEnd = findTagEnd(value, closingStart + 2)
                if (closingEnd >= 0) closingEnd + 1 else value.length
            } else {
                value.length
            }
            continue
        }

        if (tagName in voidIgnoredTags) {
            index = end + 1
            continue
        }

        val selfClosing = rawTag.trimEnd().endsWith("/>")
        val shouldStrip = tagName in knownTags ||
            hasMatchingClosingTag(value, tagName, end + 1) ||
            selfClosing ||
            looksLikeHtmlTag(rawTag, tagName)
        if (!shouldStrip) {
            output.append(rawTag)
            index = end + 1
            continue
        }

        when {
            tagName == "br" || tagName == "hr" -> output.append('\n')
            tagName in tableCellTags -> output.append(' ')
            tagName in blockTags -> output.append('\n')
            tagName == "img" -> extractHtmlAttribute(rawTag, "alt")?.takeIf(String::isNotBlank)?.let(output::append)
        }
        index = end + 1
    }
    return output.toString()
}

private fun looksLikeHtmlTag(rawTag: String, tagName: String): Boolean {
    val content = rawTag.removePrefix("<").removeSuffix(">").trim()
    val openingContent = content.removePrefix("/").trim()
    val body = openingContent.drop(tagName.length).trim()
    val singleBooleanAttribute = body.matches(Regex("[A-Za-z_:][A-Za-z0-9:_.-]*"))
    // Preserve prose such as "<nie jest HTML>". A whitespace-only suffix is
    // not an HTML attribute list; an equals sign, quoted value, or a single
    // boolean attribute is.
    return body.isEmpty() || singleBooleanAttribute || body.contains('=') || body.contains('"') || body.contains('\'')
}

private fun findTagEnd(value: String, start: Int): Int {
    var quote: Char? = null
    for (index in start until value.length) {
        val character = value[index]
        if (quote != null) {
            if (character == quote) quote = null
        } else if (character == '\'' || character == '"') {
            quote = character
        } else if (character == '>') {
            return index
        }
    }
    return -1
}

private fun htmlTagName(rawTag: String): String? {
    val match = Regex("^<\\s*/?\\s*([A-Za-z][A-Za-z0-9:-]*)").find(rawTag) ?: return null
    return match.groupValues[1].lowercase()
}

private fun hasMatchingClosingTag(value: String, tagName: String, start: Int): Boolean =
    indexOfClosingTag(value, tagName, start) >= 0

private fun indexOfClosingTag(value: String, tagName: String, start: Int): Int {
    val pattern = Regex("</\\s*${Regex.escape(tagName)}\\s*>", RegexOption.IGNORE_CASE)
    return pattern.find(value, start)?.range?.first ?: -1
}

private fun extractHtmlAttribute(rawTag: String, attribute: String): String? {
    val match = Regex("\\b${Regex.escape(attribute)}\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", RegexOption.IGNORE_CASE).find(rawTag)
    return match?.groupValues?.drop(1)?.firstOrNull(String::isNotEmpty)
}

private fun repairMojibake(value: String): String {
    var current = value
    repeat(3) {
        val repaired = repairMojibakePass(current)
        if (repaired == current) return@repeat
        current = repaired
    }
    return current
}

private fun repairMojibakePass(value: String): String {
    if (mojibakeScore(value) == 0) return value

    val output = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        val character = value[index]
        if (character !in mojibakeWeights) {
            output.append(character)
            index++
            continue
        }

        var runEnd = index + 1
        while (runEnd < value.length && isWindows1252NonAscii(value[runEnd]) && runEnd - index < 16) {
            runEnd++
        }

        var replacement: String? = null
        var replacementEnd = index + 1
        for (end in runEnd downTo index + 2) {
            val source = value.substring(index, end)
            val candidate = decodeWindows1252AsUtf8(source) ?: continue
            if (candidate.any { character ->
                character.code in 0..8 || character.code in 11..12 || character.code in 14..31
            }) {
                continue
            }
            val sourceScore = mojibakeScore(source)
            val candidateScore = mojibakeScore(candidate)
            if (candidateScore < sourceScore ||
                candidateScore == sourceScore && polishCharacterCount(candidate) > polishCharacterCount(source)
            ) {
                replacement = candidate
                replacementEnd = end
                break
            }
        }

        if (replacement == null) {
            output.append(character)
            index++
        } else {
            output.append(replacement)
            index = replacementEnd
        }
    }
    return output.toString()
}

private fun isWindows1252NonAscii(character: Char): Boolean =
    character.code in 0x80..0xFF || character in "€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”•–—˜š›œžŸ"

private fun decodeWindows1252AsUtf8(value: String): String? {
    val encoder = windows1252.newEncoder()
    if (!encoder.canEncode(value)) return null
    val bytes = value.toByteArray(windows1252)
    return try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }
}

private fun mojibakeScore(value: String): Int = value.sumOf { character -> mojibakeWeights[character] ?: 0 }

private fun polishCharacterCount(value: String): Int = value.count { it in "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ" }
