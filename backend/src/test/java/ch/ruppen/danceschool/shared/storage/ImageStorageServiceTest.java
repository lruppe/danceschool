package ch.ruppen.danceschool.shared.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageStorageServiceTest {

    @Test
    void extractKey_returnsFilename_fromFullUrl() {
        String url = "http://localhost:8080/uploads/550e8400-e29b-41d4-a716-446655440000.jpg";
        assertThat(ImageStorageService.extractKey(url)).isEqualTo("550e8400-e29b-41d4-a716-446655440000.jpg");
    }

    @Test
    void extractKey_returnsFilename_fromR2Url() {
        String url = "https://pub-abc123.r2.dev/550e8400-e29b-41d4-a716-446655440000.png";
        assertThat(ImageStorageService.extractKey(url)).isEqualTo("550e8400-e29b-41d4-a716-446655440000.png");
    }

    @Test
    void extractKey_returnsNull_whenUrlIsNull() {
        assertThat(ImageStorageService.extractKey(null)).isNull();
    }

    @Test
    void extractKey_returnsNull_whenUrlIsBlank() {
        assertThat(ImageStorageService.extractKey("  ")).isNull();
    }

    @Test
    void extractKey_returnsInput_whenNoSlash() {
        assertThat(ImageStorageService.extractKey("some-key.jpg")).isEqualTo("some-key.jpg");
    }
}
