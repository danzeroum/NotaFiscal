package br.com.nfe.processor.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class TestSamples {

    private TestSamples() {
    }

    static String okInvoice1() throws IOException {
        return read("samples/xml/ok-01.xml");
    }

    static String okInvoice2() throws IOException {
        return read("samples/xml/ok-02.xml");
    }

    static String placeholderPdf() {
        return "%PDF-1.4\nplaceholder\n%%EOF";
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
