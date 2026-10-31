package com.wilfredchau.synapsepkb.tag.model;

import com.wilfredchau.synapsepkb.common.exception.BusinessException;
import java.text.Normalizer;

public final class TagName {
    private TagName() {}

    public static String normalize(String input) {
        if (input == null) {
            throw new BusinessException(TagErrorCode.TAG_INVALID_NAME);
        }
        String nfc = Normalizer.normalize(input, Normalizer.Form.NFC);
        StringBuilder result = new StringBuilder();
        boolean space = false;
        for (int offset = 0; offset < nfc.length();) {
            int cp = nfc.codePointAt(offset);
            offset += Character.charCount(cp);
            if (isWhitespace(cp)) {
                space = result.length() > 0;
                continue;
            }
            if (cp < 32 || cp >= 127 && cp <= 159 || cp == 0x200B || cp == 0xFEFF
                    || cp >= 0xD800 && cp <= 0xDFFF) {
                throw new BusinessException(TagErrorCode.TAG_INVALID_NAME);
            }
            if (space) {
                result.append(' ');
                space = false;
            }
            result.appendCodePoint(cp);
        }
        int length = result.codePointCount(0, result.length());
        if (length < 1 || length > 32) {
            throw new BusinessException(TagErrorCode.TAG_INVALID_NAME);
        }
        return result.toString();
    }

    // Keep this explicit set identical to pkb_tag_normalize_v1 in V3.
    private static boolean isWhitespace(int cp) {
        return cp >= 9 && cp <= 13 || cp == 32 || cp == 0x85 || cp == 0xA0
                || cp == 0x1680 || cp >= 0x2000 && cp <= 0x200A || cp == 0x2028
                || cp == 0x2029 || cp == 0x202F || cp == 0x205F || cp == 0x3000;
    }
}
