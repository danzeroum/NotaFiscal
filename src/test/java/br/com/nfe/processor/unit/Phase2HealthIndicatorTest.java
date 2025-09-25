package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.nfe.processor.adapter.out.ocr.OcrAdapter;
import br.com.nfe.processor.adapter.out.sefaz.SefazCertificateManager;
import br.com.nfe.processor.adapter.out.sefaz.SefazCertificateManager.CertificateInfo;
import br.com.nfe.processor.config.Phase2HealthIndicator;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Status;

class Phase2HealthIndicatorTest {

    @Test
    void shouldReportDownWhenOcrOrCertificateInvalid() {
        OcrAdapter ocrAdapter = Mockito.mock(OcrAdapter.class);
        SefazCertificateManager certificateManager = Mockito.mock(SefazCertificateManager.class);
        when(ocrAdapter.isAvailable()).thenReturn(false);
        when(certificateManager.getCertificateInfo()).thenReturn(CertificateInfo.valid(Instant.now().plusSeconds(3600), "CN=Test"));

        Phase2HealthIndicator indicator = new Phase2HealthIndicator(ocrAdapter, certificateManager);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void shouldReportUpWhenAllChecksPass() {
        OcrAdapter ocrAdapter = Mockito.mock(OcrAdapter.class);
        SefazCertificateManager certificateManager = Mockito.mock(SefazCertificateManager.class);
        when(ocrAdapter.isAvailable()).thenReturn(true);
        when(certificateManager.getCertificateInfo()).thenReturn(CertificateInfo.valid(Instant.now().plusSeconds(86400), "CN=Test"));

        Phase2HealthIndicator indicator = new Phase2HealthIndicator(ocrAdapter, certificateManager);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(indicator.health().getDetails())
                .containsEntry("sefazCertificateSubject", "CN=Test");
    }

    @Test
    void shouldWarnWhenCertificateExpiresSoon() {
        OcrAdapter ocrAdapter = Mockito.mock(OcrAdapter.class);
        SefazCertificateManager certificateManager = Mockito.mock(SefazCertificateManager.class);
        when(ocrAdapter.isAvailable()).thenReturn(true);
        when(certificateManager.getCertificateInfo())
                .thenReturn(CertificateInfo.valid(Instant.now().plusSeconds(10), "CN=Expiring"));

        Phase2HealthIndicator indicator = new Phase2HealthIndicator(ocrAdapter, certificateManager);

        assertThat(indicator.health().getDetails())
                .containsEntry("certificateWarning", "Certificate expires soon!");
    }
}
