package bitlap.sbt.analyzer.jbexternal.util

import org.apache.commons.lang3.StringUtils

/**
 * Copy from commons-lang3 [[org.apache.commons.lang.StringUtils]]
 * 
 * @author 梦境迷离
 * @version 1.0,2023/10/13
 */
fun abbreviate(str: String, maxWidth: Int): String {
    return abbreviate(str, "...", 0, maxWidth)
}

fun abbreviate(str: String, abbrevMarker: String, maxWidth: Int): String {
    return abbreviate(str, abbrevMarker, 0, maxWidth)
}

fun abbreviate(str: String, abbrevMarker: String, stringOffset: Int, maxWidth: Int): String {
    var offset = stringOffset
    return if (StringUtils.isNotEmpty(str) && "" == abbrevMarker && maxWidth > 0) {
        StringUtils.substring(str, 0, maxWidth)
    } else if (StringUtils.isAnyEmpty(str, abbrevMarker)) {
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
                            "Minimum abbreviation width with offset is %d",
                            minAbbrevWidthOffset
                        )
                    )
                } else {
                    if (offset + maxWidth - abbrevMarkerLength < strLen) abbrevMarker + abbreviate(
                        str.substring(offset),
                        abbrevMarker,
                        maxWidth - abbrevMarkerLength
                    ) else abbrevMarker + str.substring(strLen - (maxWidth - abbrevMarkerLength))
                }
            }
        }
    }
}