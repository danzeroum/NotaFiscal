package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.nfe.processor.adapter.out.sefaz.SefazCertificateManager;
import br.com.nfe.processor.integration.sefaz.TestCertificateFactory;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SefazCertificateManagerTest {

    @Test
    void shouldValidateCertificateSuccessfully() throws Exception {
        Path keystore = TestCertificateFactory.createTemporaryKeystore("strongpass");
        SefazCertificateManager manager = new SefazCertificateManager(keystore.toString(), "strongpass");

        assertThat(manager.isCertificateValid()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenPasswordIsInvalid() throws Exception {
        Path keystore = TestCertificateFactory.createTemporaryKeystore("rightpass");
        SefazCertificateManager manager = new SefazCertificateManager(keystore.toString(), "wrongpass");

        assertThat(manager.isCertificateValid()).isFalse();
    }

    @Test
    void shouldFailWhenPathNotConfigured() {
        SefazCertificateManager manager = new SefazCertificateManager("", "");

        assertThatThrownBy(manager::isCertificateValid)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SEFAZ_CERT_PATH");
    }
}
