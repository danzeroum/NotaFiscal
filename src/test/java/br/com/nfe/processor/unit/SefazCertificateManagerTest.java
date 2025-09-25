package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.nfe.processor.adapter.out.sefaz.SefazCertificateManager;
import br.com.nfe.processor.adapter.out.sefaz.SefazCertificateManager.CertificateInfo;
import br.com.nfe.processor.integration.sefaz.TestCertificateFactory;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

class SefazCertificateManagerTest {

    @Test
    void shouldValidateCertificateSuccessfully() throws Exception {
        Path keystore = TestCertificateFactory.createTemporaryKeystore("strongpass");
        SefazCertificateManager manager = new SefazCertificateManager(keystore.toString(), "strongpass", 2, "");

        assertThat(manager.isCertificateValid()).isTrue();
        CertificateInfo info = manager.getCertificateInfo();
        assertThat(info.subject()).contains("CN=Test");
        assertThat(info.expirationDate()).isNotNull();
    }

    @Test
    void shouldReturnFalseWhenPasswordIsInvalid() throws Exception {
        Path keystore = TestCertificateFactory.createTemporaryKeystore("rightpass");
        SefazCertificateManager manager = new SefazCertificateManager(keystore.toString(), "wrongpass", 3, "");

        assertThat(manager.isCertificateValid()).isFalse();
    }

    @Test
    void shouldFailWhenPathNotConfigured() {
        SefazCertificateManager manager = new SefazCertificateManager("", "", 3, "");

        assertThatThrownBy(manager::isCertificateValid)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SEFAZ_CERT_PATH");
    }

    @Test
    void shouldRespectConfiguredTimeout() throws Exception {
        Path keystore = TestCertificateFactory.createTemporaryKeystore("pass");
        SefazCertificateManager manager = new SefazCertificateManager(keystore.toString(), "pass", 1, "");

        HttpComponentsMessageSender sender = manager.createMessageSender();

        assertThat(sender.getConnectionTimeout()).isEqualTo(java.time.Duration.ofSeconds(1));
        assertThat(sender.getReadTimeout()).isEqualTo(java.time.Duration.ofSeconds(1));
    }

    @Test
    void shouldRejectPathOutsideAllowedDirectories() {
        SefazCertificateManager manager = new SefazCertificateManager("/opt/../etc/passwd", "secret", 3, "");

        assertThat(manager.isCertificateValid()).isFalse();
        assertThatThrownBy(() -> manager.createMessageSender())
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("outside allowed");
    }
}
