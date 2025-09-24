package br.com.nfe.processor.adapter.out.ocr;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Tess4JOcrAdapter implements OcrAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Tess4JOcrAdapter.class);

    private final boolean ocrEnabled;

    public Tess4JOcrAdapter(@Value("${ocr.enabled:false}") boolean ocrEnabled) {
        this.ocrEnabled = ocrEnabled;
    }

    @Override
    public Optional<String> extractXml(byte[] pdfContent) {
        if (!ocrEnabled) {
            LOGGER.debug("OCR disabled via feature flag");
            return Optional.empty();
        }
        LOGGER.info("OCR feature enabled - returning empty result for MVP stub");
        return Optional.empty();
    }
}
