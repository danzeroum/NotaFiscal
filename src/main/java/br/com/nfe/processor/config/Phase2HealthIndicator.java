package br.com.nfe.processor.config;

import br.com.nfe.processor.adapter.out.ocr.OcrAdapter;
import br.com.nfe.processor.adapter.out.sefaz.SefazCertificateManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
        SefazCertificateManager.CertificateInfo certInfo = certificateManager.getCertificateInfo();
        boolean certificateValid = certInfo.isValid();
        Health.Builder builder = (ocrAvailable && certificateValid) ? Health.up() : Health.down();
        builder.withDetail("ocrAvailable", ocrAvailable);
        builder.withDetail("sefazCertificateValid", certificateValid);
        if (certInfo.subject() != null) {
            builder.withDetail("sefazCertificateSubject", certInfo.subject());
        }
        if (certInfo.expirationDate() != null) {
            long daysUntilExpiry = ChronoUnit.DAYS.between(Instant.now(), certInfo.expirationDate());
            builder.withDetail("certificateExpiresInDays", daysUntilExpiry);
            if (daysUntilExpiry < 30) {
                builder.withDetail("certificateWarning", "Certificate expires soon!");
            }
        }
        return builder.build();
    }
}
