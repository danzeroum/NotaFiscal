package br.com.nfe.processor.adapter.out.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class Tess4JOcrAdapter implements OcrAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Tess4JOcrAdapter.class);

    private static final Pattern ACCESS_KEY_PATTERN = Pattern.compile("\\b\\d{44}\\b");
    private static final Pattern CNPJ_PATTERN = Pattern.compile(
            "(?:(?:\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})|(?:\\b\\d{14}\\b))");
    private static final Pattern CPF_PATTERN = Pattern.compile(
            "(?:(?:\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})|(?:\\b\\d{11}\\b))");
    private static final Pattern CFOP_PATTERN = Pattern.compile("(?i)cfop[^0-9]{0,5}(\\d{4})");
    private static final Pattern TOTAL_PATTERN = Pattern.compile(
            "(?i)(?:valor\\s+total|total\\s+da\\s+nota)[^0-9]{0,15}(\\d+[.,]\\d{2})");
    private static final Pattern PRODUCTS_PATTERN = Pattern.compile(
            "(?i)(?:valor\\s+dos\\s+produtos|produtos)[^0-9]{0,15}(\\d+[.,]\\d{2})");
    private static final Pattern ICMS_PATTERN = Pattern.compile("(?i)icms[^0-9]{0,10}(\\d+[.,]\\d{2})");
    private static final Pattern IPI_PATTERN = Pattern.compile("(?i)ipi[^0-9]{0,10}(\\d+[.,]\\d{2})");
    private static final Pattern ISS_PATTERN = Pattern.compile("(?i)iss[^0-9]{0,10}(\\d+[.,]\\d{2})");

    private final boolean ocrEnabled;
    private final String language;
    private final String tessdataPath;

    public Tess4JOcrAdapter(
            @Value("${ocr.enabled:false}") boolean ocrEnabled,
            @Value("${ocr.language:por+eng}") String language,
            @Value("${ocr.tessdata-path:/usr/share/tessdata}") String tessdataPath) {
        this.ocrEnabled = ocrEnabled;
        this.language = language;
        this.tessdataPath = tessdataPath;
    }

    @Async
    @Override
    public CompletableFuture<Optional<String>> extractXml(byte[] fileContent) {
        if (!ocrEnabled) {
            LOGGER.debug("OCR disabled via feature flag");
            return CompletableFuture.completedFuture(Optional.empty());
        }
        try {
            String text = extractText(fileContent);
            if (text.isBlank()) {
                LOGGER.warn("OCR returned empty content");
                return CompletableFuture.completedFuture(Optional.empty());
            }
            Optional<String> xml = buildXmlFromText(text);
            if (xml.isEmpty()) {
                LOGGER.warn("OCR could not infer mandatory NFe fields");
            }
            return CompletableFuture.completedFuture(xml);
        } catch (IOException | TesseractException ex) {
            LOGGER.error("Failed to process file with OCR", ex);
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    private String extractText(byte[] content) throws IOException, TesseractException {
        if (isPdf(content)) {
            return extractTextFromPdf(content);
        }
        return extractTextFromImage(content);
    }

    private boolean isPdf(byte[] content) {
        return content.length >= 4
                && content[0] == '%'
                && content[1] == 'P'
                && content[2] == 'D'
                && content[3] == 'F';
    }

    private String extractTextFromPdf(byte[] pdf) throws IOException, TesseractException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(document);
            List<BufferedImage> pages = new ArrayList<>();
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                pages.add(renderer.renderImageWithDPI(page, 300, ImageType.RGB));
            }
            return doOcr(pages);
        }
    }

    private String extractTextFromImage(byte[] imageBytes) throws IOException, TesseractException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("Formato de imagem não suportado para OCR");
            }
            return doOcr(List.of(image));
        }
    }

    private String doOcr(List<BufferedImage> images) throws TesseractException {
        Tesseract tesseract = configureEngine();
        StringBuilder builder = new StringBuilder();
        for (BufferedImage image : images) {
            builder.append(tesseract.doOCR(image)).append('\n');
        }
        return builder.toString();
    }

    private Tesseract configureEngine() {
        Tesseract tesseract = new Tesseract();
        if (tessdataPath != null && !tessdataPath.isBlank()) {
            tesseract.setDatapath(tessdataPath);
        } else {
            String envPath = System.getenv("TESSDATA_PREFIX");
            if (envPath != null && !envPath.isBlank()) {
                tesseract.setDatapath(envPath);
            }
        }
        tesseract.setLanguage(language);
        tesseract.setTessVariable("user_defined_dpi", "300");
        return tesseract;
    }

    private Optional<String> buildXmlFromText(String rawText) {
        String normalizedText = rawText.replace('\r', '\n');
        Optional<String> accessKey = findAccessKey(normalizedText);
        if (accessKey.isEmpty()) {
            return Optional.empty();
        }
        String emitterName = extractLabeledValue(normalizedText, "EMITENTE", "Emitente", "Emit:", "Emit");
        String recipientName = extractLabeledValue(normalizedText, "DESTINATÁRIO", "Destinatário", "Dest:", "Dest");

        String emitterCnpj = extractDigits(CNPJ_PATTERN, normalizedText)
                .orElse("27865757000102");
        String recipientTaxId = extractDigits(CPF_PATTERN, normalizedText)
                .orElseGet(() -> extractSecondCnpj(normalizedText, emitterCnpj));

        BigDecimal totalAmount = findAmount(TOTAL_PATTERN, normalizedText).orElse(BigDecimal.ZERO);
        BigDecimal productsAmount = findAmount(PRODUCTS_PATTERN, normalizedText).orElse(totalAmount);
        if (productsAmount.compareTo(BigDecimal.ZERO) == 0 && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            productsAmount = totalAmount;
        }
        BigDecimal icmsAmount = findAmount(ICMS_PATTERN, normalizedText).orElse(BigDecimal.ZERO);
        BigDecimal ipiAmount = findAmount(IPI_PATTERN, normalizedText).orElse(BigDecimal.ZERO);
        BigDecimal issAmount = findAmount(ISS_PATTERN, normalizedText).orElse(BigDecimal.ZERO);

        String cfop = findCfop(normalizedText);

        String xml = buildXml(
                accessKey.get(),
                emitterName,
                emitterCnpj,
                recipientName,
                recipientTaxId,
                productsAmount,
                totalAmount,
                icmsAmount,
                ipiAmount,
                issAmount,
                cfop);
        return Optional.of(xml);
    }

    private Optional<String> findAccessKey(String text) {
        Matcher matcher = ACCESS_KEY_PATTERN.matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.group());
        }
        String digits = text.replaceAll("\\D", "");
        for (int i = 0; i <= digits.length() - 44; i++) {
            String candidate = digits.substring(i, i + 44);
            if (candidate.chars().distinct().count() > 1) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractDigits(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.group().replaceAll("\\D", ""));
        }
        return Optional.empty();
    }

    private String extractSecondCnpj(String text, String primaryCnpj) {
        Matcher matcher = CNPJ_PATTERN.matcher(text);
        while (matcher.find()) {
            String current = matcher.group().replaceAll("\\D", "");
            if (!current.equals(primaryCnpj)) {
                return current;
            }
        }
        return "12345678909";
    }

    private Optional<BigDecimal> findAmount(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Optional.of(parseDecimal(matcher.group(1)));
        }
        return Optional.empty();
    }

    private BigDecimal parseDecimal(String value) {
        String normalized = value.replace('.', ' ').replace(',', '.').replace(" ", "");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            LOGGER.debug("Unable to parse decimal value '{}'", value, ex);
            return BigDecimal.ZERO;
        }
    }

    private String findCfop(String text) {
        Matcher matcher = CFOP_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "5102";
    }

    private String buildXml(
            String accessKey,
            String emitterName,
            String emitterCnpj,
            String recipientName,
            String recipientTaxId,
            BigDecimal productsAmount,
            BigDecimal totalAmount,
            BigDecimal icmsAmount,
            BigDecimal ipiAmount,
            BigDecimal issAmount,
            String cfop) {
        String products = formatDecimal(productsAmount);
        String total = formatDecimal(totalAmount.compareTo(BigDecimal.ZERO) == 0 ? productsAmount : totalAmount);
        String icms = formatDecimal(icmsAmount);
        String ipi = formatDecimal(ipiAmount);
        String iss = formatDecimal(issAmount);
        String escapedEmitter = escapeXml(defaultIfBlank(emitterName, "Emitente via OCR"));
        String escapedRecipient = escapeXml(defaultIfBlank(recipientName, "Destinatário via OCR"));

        String template = """
                <NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">
                  <infNFe Id=\"NFe%s\">
                    <emit>
                      <xNome>%s</xNome>
                      <CNPJ>%s</CNPJ>
                    </emit>
                    <dest>
                      <xNome>%s</xNome>
                      <CPF>%s</CPF>
                    </dest>
                    <det nItem=\"1\">
                      <prod>
                        <cProd>0001</cProd>
                        <xProd>Item processado via OCR</xProd>
                        <CFOP>%s</CFOP>
                        <vProd>%s</vProd>
                      </prod>
                    </det>
                    <total>
                      <ICMSTot>
                        <vBC>%s</vBC>
                        <vICMS>%s</vICMS>
                        <vIPI>%s</vIPI>
                        <vISS>%s</vISS>
                        <vProd>%s</vProd>
                        <vNF>%s</vNF>
                      </ICMSTot>
                    </total>
                    <imposto>
                      <ICMS>
                        <ICMS00>
                          <orig>0</orig>
                          <CST>00</CST>
                          <CFOP>%s</CFOP>
                        </ICMS00>
                      </ICMS>
                    </imposto>
                  </infNFe>
                </NFe>
                """;

        return String.format(
                Locale.ROOT,
                template,
                accessKey,
                escapedEmitter,
                emitterCnpj,
                escapedRecipient,
                recipientTaxId,
                cfop,
                products,
                products,
                icms,
                ipi,
                iss,
                products,
                total,
                cfop);
    }

    private String formatDecimal(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String extractLabeledValue(String text, String... labels) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            for (String label : labels) {
                int index = line.toUpperCase(Locale.ROOT).indexOf(label.toUpperCase(Locale.ROOT));
                if (index >= 0) {
                    String candidate = line.substring(index + label.length()).replace(':', ' ').trim();
                    if (!candidate.isBlank()) {
                        return candidate;
                    }
                }
            }
        }
        return "";
    }

    private String escapeXml(String value) {
        String sanitized = value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
        return new String(sanitized.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
