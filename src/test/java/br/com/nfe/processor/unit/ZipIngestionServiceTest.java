package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.nfe.processor.core.domain.model.Batch;
import br.com.nfe.processor.core.domain.repository.BatchRepository;
import br.com.nfe.processor.core.domain.service.ZipIngestionService;
import br.com.nfe.processor.core.domain.service.event.BatchProcessingRequestedEvent;
import br.com.nfe.processor.exception.UnprocessableEntityException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ZipIngestionServiceTest {

    @Mock
    private BatchRepository batchRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ZipIngestionService service;
    private ZipIngestionService serviceWithOcr;

    @BeforeEach
    void setUp() {
        when(batchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new ZipIngestionService(
                batchRepository,
                eventPublisher,
                false);
        serviceWithOcr = new ZipIngestionService(
                batchRepository,
                eventPublisher,
                true);
    }

    @Test
    void shouldPublishEventForXmlEntries() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "lote.zip", "application/zip", zipWith("nota.xml", "<xml/>"));
        Batch batch = service.ingest(file, false);

        ArgumentCaptor<BatchProcessingRequestedEvent> captor =
                ArgumentCaptor.forClass(BatchProcessingRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        BatchProcessingRequestedEvent event = captor.getValue();
        List<BatchProcessingRequestedEvent.InvoiceSource> sources = event.sources();
        List<Boolean> ocrFlags = sources.stream().map(BatchProcessingRequestedEvent.InvoiceSource::requiresOcr)
                .collect(Collectors.toList());

        org.assertj.core.api.Assertions.assertThat(event.batchId()).isEqualTo(batch.getId());
        org.assertj.core.api.Assertions.assertThat(ocrFlags).containsExactly(false);
    }

    @Test
    void shouldFailWhenPdfProvidedAndOcrDisabled() {
        MockMultipartFile file = new MockMultipartFile("file", "lote.zip", "application/zip", zipWith("arquivo.pdf", "pdf"));
        assertThatThrownBy(() -> service.ingest(file, false))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("OCR desabilitado");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldRejectUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "lote.zip", "application/zip", zipWith("arquivo.txt", "conteudo"));
        assertThatThrownBy(() -> service.ingest(file, false))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Extensão não suportada");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldPublishEventWithOcrEntryWhenEnabled() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lote.zip", "application/zip", zipWith("arquivo.pdf", "pdf"));

        Batch batch = serviceWithOcr.ingest(file, true);

        ArgumentCaptor<BatchProcessingRequestedEvent> captor =
                ArgumentCaptor.forClass(BatchProcessingRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        BatchProcessingRequestedEvent event = captor.getValue();

        org.assertj.core.api.Assertions.assertThat(event.batchId()).isEqualTo(batch.getId());
        org.assertj.core.api.Assertions.assertThat(event.sources())
                .singleElement()
                .extracting(BatchProcessingRequestedEvent.InvoiceSource::requiresOcr)
                .isEqualTo(true);
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
}
