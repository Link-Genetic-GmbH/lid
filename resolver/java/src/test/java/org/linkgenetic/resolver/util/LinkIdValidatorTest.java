package org.linkgenetic.resolver.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LinkIdValidatorTest {

    private final LinkIdValidator validator = new LinkIdValidator();

    @Test
    void isValid_accepts32To64Alphanumeric() {
        String id32 = "a".repeat(32);
        String id64 = "b".repeat(64);
        assertThat(validator.isValid(id32)).isTrue();
        assertThat(validator.isValid(id64)).isTrue();
    }

    @Test
    void isValid_rejectsOutsideBoundsAndNonAlnum() {
        assertThat(validator.isValid(null)).isFalse();
        assertThat(validator.isValid("")).isFalse();
        assertThat(validator.isValid("abc")).isFalse();
        assertThat(validator.isValid("c".repeat(65))).isFalse();
        assertThat(validator.isValid("abc-" + "d".repeat(30))).isFalse();
    }

    @Test
    void normalize_trimsAndLowercases() {
        assertThat(validator.normalize("  ABCDEF  ")).isEqualTo("abcdef");
        assertThat(validator.normalize(null)).isNull();
    }

    @Test
    void isUUID_trueFor32Hex() {
        String uuidLike = "0123456789abcdef0123456789abcdef"; // 32 hex
        assertThat(validator.isUUID(uuidLike)).isTrue();
        assertThat(validator.isUUID(uuidLike.toUpperCase())).isTrue();
    }

    @Test
    void isHash_trueFor32or64Hex() {
        String h32 = "0".repeat(32);
        String h64 = "a".repeat(64);
        assertThat(validator.isHash(h32)).isTrue();
        assertThat(validator.isHash(h64)).isTrue();
        assertThat(validator.isHash("z".repeat(32))).isFalse();
    }

    @Test
    void validate_throwsOnInvalid() {
        assertThrows(RuntimeException.class, () -> validator.validate("bad"));
    }
}
