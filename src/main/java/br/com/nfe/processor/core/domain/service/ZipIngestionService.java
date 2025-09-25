package br.com.nfe.processor.core.domain.service;

import br.com.nfe.processor.core.domain.model.Batch;
import br.com.nfe.processor.core.domain.model.BatchStatus;
import br.com.nfe.processor.core.domain.repository.BatchRepository;
import br.com.nfe.processor.core.domain.service.event.BatchProcessingRequestedEvent;
import br.com.nfe.processor.exception.BadRequestException;
import br.com.nfe.processor.exception.UnprocessableEntityException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ZipIngestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipIngestionService.class);

    private final BatchRepository batchRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final boolean ocrEnabled;

    public ZipIngestionService(
            BatchRepository batchRepository,
            ApplicationEventPublisher eventPublisher,
            @Value("${ocr.enabled:false}") boolean ocrEnabled) {
        this.batchRepository = batchRepository;
        this.eventPublisher = eventPublisher;
        this.ocrEnabled = ocrEnabled;
    }

    @Transactional
    public Batch ingest(MultipartFile file, boolean ocrRequested) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo ZIP é obrigatório");
        }
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".zip")) {
            throw new BadRequestException("Apenas arquivos ZIP são suportados");
        }

        Batch batch = createBatch();
        List<BatchProcessingRequestedEvent.InvoiceSource> sources = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (name.endsWith(".xml")) {
                    sources.add(BatchProcessingRequestedEvent.InvoiceSource.xml(name, readEntry(zipInputStream)));
                } else if (name.endsWith(".pdf") || name.endsWith(".png") || name.endsWith(".jpg")) {
                    if (!ocrEnabled || !ocrRequested) {
                        throw new UnprocessableEntityException("OCR desabilitado: PDF " + name + " não pode ser processado");
                    }
                    sources.add(BatchProcessingRequestedEvent.InvoiceSource.ocr(name, readBinaryEntry(zipInputStream)));
                } else {
                    throw new UnprocessableEntityException("Extensão não suportada: " + name);
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException ex) {
            throw new UnprocessableEntityException("Falha ao ler arquivo ZIP");
        }

        if (sources.isEmpty()) {
            throw new UnprocessableEntityException("Nenhum XML de NFe encontrado no lote");
        }

        eventPublisher.publishEvent(new BatchProcessingRequestedEvent(batch.getId(), sources));
        LOGGER.info("Lote {} recebido com {} arquivos para processamento", batch.getId(), sources.size());
        return batch;
    }

    private Batch createBatch() {
        Batch batch = new Batch();
        batch.setId("b_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        batch.setStatus(BatchStatus.RECEIVED);
        batch.setReceivedAt(Instant.now());
        return batchRepository.save(batch);
    }

    private String readEntry(ZipInputStream inputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private byte[] readBinaryEntry(ZipInputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int read;
        while ((read = inputStream.read(data)) != -1) {
            buffer.write(data, 0, read);
        }
        return buffer.toByteArray();
    }
}
