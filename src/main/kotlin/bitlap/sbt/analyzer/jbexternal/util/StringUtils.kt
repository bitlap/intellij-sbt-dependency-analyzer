package bitlap.sbt.analyzer.jbexternal.util

/**
 * Copy from commons-lang3
 */
fun abbreviate(str: String, maxWidth: Int): String {
    return abbreviate(str, "...", 0, maxWidth)
}

fun abbreviate(str: String, abbrevMarker: String, maxWidth: Int): String {
    return abbreviate(str, abbrevMarker, 0, maxWidth)
}

fun isAnyEmpty(vararg css: CharSequence): Boolean {
    if (css.isEmpty()) {
        return false
    } else {
        val length = css.size
        for (var3 in 0 until length) {
            val cs = css[var3]
            if (cs.isEmpty()) {
                return true
            }
        }

        return false
    }
}

fun abbreviate(str: String, abbrevMarker: String, stringOffset: Int, maxWidth: Int): String {
    var offset = stringOffset
    return if (str.isNotEmpty() && "" == abbrevMarker && maxWidth > 0) {
        str.substring(0, maxWidth)
    } else if (isAnyEmpty(str, abbrevMarker)) {
        str
    } else {
        val abbrevMarkerLength = abbrevMarker.length
        val minAbbrevWidth = abbrevMarkerLength + 1
        val minAbbrevWidthOffset = abbrevMarkerLength + abbrevMarkerLength + 1
        if (maxWidth < minAbbrevWidth) {
            throw IllegalArgumentException(String.format("Minimum abbreviation width is %d", minAbbrevWidth))
        } else {
            val strLen = str.length
            if (strLen <= maxWidth) {
                str
            } else {
                if (offset > strLen) {
                    offset = strLen
                }
                if (strLen - offset < maxWidth - abbrevMarkerLength) {
                    offset = strLen - (maxWidth - abbrevMarkerLength)
                }
                if (offset <= abbrevMarkerLength + 1) {
                    str.substring(0, maxWidth - abbrevMarkerLength) + abbrevMarker
                } else if (maxWidth < minAbbrevWidthOffset) {
                    throw IllegalArgumentException(
                        String.format(
                            "Minimum abbreviation width with offset is %d", minAbbrevWidthOffset
                        )
                    )
                } else {
                    if (offset + maxWidth - abbrevMarkerLength < strLen) abbrevMarker + abbreviate(
                        str.substring(offset), abbrevMarker, maxWidth - abbrevMarkerLength
                    ) else abbrevMarker + str.substring(strLen - (maxWidth - abbrevMarkerLength))
                }
            }
        }
    }
}