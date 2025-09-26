package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import br.com.nfe.processor.adapter.out.ocr.Tess4JOcrAdapter;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class Tess4JOcrAdapterRegexTest {

    @Test
    void shouldHandleLargeInputWithoutRegexCatastrophicBacktracking() throws Exception {
        Tess4JOcrAdapter adapter = new Tess4JOcrAdapter(true, "por+eng", "/usr/share/tessdata", 75);
        Method method = Tess4JOcrAdapter.class.getDeclaredMethod("findAccessKey", String.class);
        method.setAccessible(true);
        String repeating = "1".repeat(43) + "2";
        String input = "X".repeat(10_000) + repeating;

        assertTimeoutPreemptively(Duration.ofMillis(200), () -> {
            @SuppressWarnings("unchecked")
            Optional<String> result = (Optional<String>) method.invoke(adapter, input);
            assertThat(result).contains(repeating);
        });
    }
}
