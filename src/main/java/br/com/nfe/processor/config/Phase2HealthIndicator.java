package br.com.nfe.processor.config;

import br.com.nfe.processor.adapter.out.ocr.OcrAdapter;
import br.com.nfe.processor.adapter.out.sefaz.SefazCertificateManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class Phase2HealthIndicator implements HealthIndicator {

    private final OcrAdapter ocrAdapter;
    private final SefazCertificateManager certificateManager;

    public Phase2HealthIndicator(OcrAdapter ocrAdapter, SefazCertificateManager certificateManager) {
        this.ocrAdapter = ocrAdapter;
        this.certificateManager = certificateManager;
    }

    @Override
    public Health health() {
        boolean ocrAvailable = ocrAdapter.isAvailable();
        boolean certificateValid = certificateManager.isCertificateValid();
        Health.Builder builder = (ocrAvailable && certificateValid) ? Health.up() : Health.down();
        builder.withDetail("ocrAvailable", ocrAvailable);
        builder.withDetail("sefazCertificateValid", certificateValid);
        return builder.build();
    }
}
