package br.com.nfe.processor.core.domain.service.event;

import java.util.List;
import java.util.Objects;

public record BatchProcessingRequestedEvent(String batchId, List<InvoiceSource> sources) {

    public BatchProcessingRequestedEvent {
        Objects.requireNonNull(batchId, "batchId");
        Objects.requireNonNull(sources, "sources");
        sources = List.copyOf(sources);
    }

    public record InvoiceSource(String name, String xmlContent, byte[] binaryContent, boolean requiresOcr) {

        public static InvoiceSource xml(String name, String xmlContent) {
            return new InvoiceSource(name, xmlContent, null, false);
        }

        public static InvoiceSource ocr(String name, byte[] binaryContent) {
            return new InvoiceSource(name, null, binaryContent, true);
        }

        public InvoiceSource {
            Objects.requireNonNull(name, "name");
            if (requiresOcr) {
                Objects.requireNonNull(binaryContent, "binaryContent");
            } else {
                Objects.requireNonNull(xmlContent, "xmlContent");
            }
        }
    }
}
