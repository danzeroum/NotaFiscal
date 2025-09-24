package br.com.nfe.processor.adapter.out.ocr;

import java.util.Optional;

public interface OcrAdapter {
    Optional<String> extractXml(byte[] pdfContent);
}
