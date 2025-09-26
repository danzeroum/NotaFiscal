package br.com.nfe.processor.infrastructure.health;

import br.com.nfe.processor.adapter.out.ocr.OcrAdapter;
import br.com.nfe.processor.adapter.out.sefaz.SefazClient;
import br.com.nfe.processor.core.domain.repository.BatchRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class NfeProcessorHealthIndicator implements HealthIndicator {

    private final BatchRepository batchRepository;
    private final OcrAdapter ocrAdapter;
    private final SefazClient sefazClient;

    @Value("${features.mock-data-enabled:false}")
    private boolean mockEnabled;

    @Value("${ocr.enabled:false}")
    private boolean ocrEnabled;

    @Value("${sefaz.stub.enabled:true}")
    private boolean sefazStubEnabled;

    public NfeProcessorHealthIndicator(
            BatchRepository batchRepository,
            OcrAdapter ocrAdapter,
            SefazClient sefazClient) {
        this.batchRepository = batchRepository;
        this.ocrAdapter = ocrAdapter;
        this.sefazClient = sefazClient;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        Health.Builder builder = Health.up();

        try {
            long batchCount = batchRepository.count();
            details.put("database", "UP");
            details.put("totalBatches", batchCount);
        } catch (Exception ex) {
            details.put("database", "DOWN");
            builder.down(ex);
        }

        if (ocrEnabled) {
            boolean available = ocrAdapter.isAvailable();
            details.put("ocr", available ? "UP" : "DOWN");
            if (!available) {
                builder.down();
            }
        } else {
            details.put("ocr", "DISABLED");
        }

        details.put("sefaz", sefazStubEnabled ? "STUB" : "REAL");
        details.put("buildToFlipVersion", "6.0");
        details.put("mockDataEnabled", mockEnabled);
        details.put("traceIdEnabled", true);
        details.put("rfc7807Enabled", true);
        details.put("p95Target", "800ms");
        details.put("timestamp", Instant.now());

        return builder.withDetails(details).build();
    }
}
