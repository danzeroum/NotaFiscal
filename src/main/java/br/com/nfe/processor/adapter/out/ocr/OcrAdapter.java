package br.com.nfe.processor.adapter.out.ocr;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface OcrAdapter {
    CompletableFuture<Optional<String>> extractXml(byte[] pdfContent);

    default boolean isAvailable() {
        return true;
    }
}
