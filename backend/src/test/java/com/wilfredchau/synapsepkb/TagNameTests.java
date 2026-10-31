package com.wilfredchau.synapsepkb;

import static org.assertj.core.api.Assertions.*;

import com.wilfredchau.synapsepkb.common.exception.BusinessException;
import com.wilfredchau.synapsepkb.security.CurrentUser;
import com.wilfredchau.synapsepkb.tag.model.TagName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.context.SecurityContextHolder;

class TagNameTests {
    @Test
    void normalizesNfcAndAllWhitespaceWithoutFoldingCase() {
        for (int cp : new int[]{9, 10, 11, 12, 13, 32, 0x85, 0xA0, 0x1680, 0x2000, 0x2001,
                0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200A,
                0x2028, 0x2029, 0x202F, 0x205F, 0x3000}) {
            String ws = Character.toString(cp);
            assertThat(TagName.normalize(ws + "Cafe\u0301" + ws + ws + "Java" + ws)).isEqualTo("Caf\u00e9 Java");
        }
        assertThat(TagName.normalize("\u673a\u5668\u5b66\u4e60")).isEqualTo("\u673a\u5668\u5b66\u4e60");
        assertThat(TagName.normalize("\uFF2Aava")).isEqualTo("\uFF2Aava");
        assertThat(TagName.normalize("\uD83D\uDC69\u200D\uD83D\uDCBB")).isEqualTo("\uD83D\uDC69\u200D\uD83D\uDCBB");
    }

    @Test
    void countsUnicodeCodePoints() {
        String supplementary = new String(Character.toChars(0x20000));
        assertThat(TagName.normalize(supplementary.repeat(32))).isEqualTo(supplementary.repeat(32));
        assertThatThrownBy(() -> TagName.normalize(supplementary.repeat(33))).isInstanceOf(BusinessException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\u3000", "\u0000", "a\u0001", "a\u007f", "\u009f", "\u200b", "\ufeff", "\ud800"})
    void rejectsInvalidNames(String name) {
        assertThatThrownBy(() -> TagName.normalize(name)).isInstanceOf(BusinessException.class);
    }

    @Test
    void doesNotFallBackToDefaultUser() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(CurrentUser::id).isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).toApiError().code()).isEqualTo("AUTHENTICATION_REQUIRED");
    }
}
