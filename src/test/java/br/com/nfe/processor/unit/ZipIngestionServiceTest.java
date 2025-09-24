package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import br.com.nfe.processor.adapter.out.ocr.OcrAdapter;
import br.com.nfe.processor.adapter.out.sefaz.SefazVerificationClient;
import br.com.nfe.processor.core.domain.model.Batch;
import br.com.nfe.processor.core.domain.model.ValidationResultType;
import br.com.nfe.processor.core.domain.repository.BatchRepository;
import br.com.nfe.processor.core.domain.service.AnomalyService;
import br.com.nfe.processor.core.domain.service.FiscalValidationService;
import br.com.nfe.processor.core.domain.service.XmlParserService;
import br.com.nfe.processor.core.domain.service.ZipIngestionService;
import br.com.nfe.processor.core.domain.service.dto.ParsedInvoice;
import br.com.nfe.processor.core.domain.valueobject.Cnpj;
import br.com.nfe.processor.core.domain.service.dto.ValidationResult;
import br.com.nfe.processor.exception.UnprocessableEntityException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ZipIngestionServiceTest {

    @Mock
    private BatchRepository batchRepository;
    @Mock
    private XmlParserService xmlParserService;
    @Mock
    private FiscalValidationService fiscalValidationService;
    @Mock
    private AnomalyService anomalyService;
    @Mock
    private OcrAdapter ocrAdapter;
    @Mock
    private SefazVerificationClient sefazVerificationClient;

    private ZipIngestionService service;

    @BeforeEach
    void setUp() {
        when(batchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sefazVerificationClient.isValidAccessKey(any())).thenReturn(true);
        when(fiscalValidationService.validate(any()))
                .thenReturn(List.of(new ValidationResult("TOTALS_RECONCILIATION", ValidationResultType.OK, "ok")));
        when(anomalyService.detect(any(), any(), any(), anyBoolean())).thenReturn(Collections.emptyList());
        when(xmlParserService.parse(any())).thenReturn(parsedInvoice());
        service = new ZipIngestionService(
                batchRepository,
                xmlParserService,
                fiscalValidationService,
                anomalyService,
                ocrAdapter,
                sefazVerificationClient,
                false);
    }

    @Test
    void shouldProcessZipWithXmlEntries() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "lote.zip", "application/zip", zipWith("nota.xml", "<xml/>"));
        Batch batch = service.ingest(file, false);
        assertThat(batch.getInvoicesTotal()).isEqualTo(1);
        assertThat(batch.getStatus()).isNotNull();
    }

    @Test
    void shouldFailWhenPdfProvidedAndOcrDisabled() {
        MockMultipartFile file = new MockMultipartFile("file", "lote.zip", "application/zip", zipWith("arquivo.pdf", "pdf"));
        assertThatThrownBy(() -> service.ingest(file, false))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("OCR desabilitado");
    }

    @Test
    void shouldRejectUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "lote.zip", "application/zip", zipWith("arquivo.txt", "conteudo"));
        assertThatThrownBy(() -> service.ingest(file, false))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Extensão não suportada");
    }

    private byte[] zipWith(String name, String content) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write(content.getBytes());
                zip.closeEntry();
            }
            return out.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ParsedInvoice parsedInvoice() {
        return new ParsedInvoice(
                "chave",
                "Emitente",
                new Cnpj("12345678000195"),
                "Destinatario",
                "456",
                1,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "5102");
    }
}
