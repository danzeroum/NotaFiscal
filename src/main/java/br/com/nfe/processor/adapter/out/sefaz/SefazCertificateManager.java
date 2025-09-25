package br.com.nfe.processor.adapter.out.sefaz;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Enumeration;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

@Component
public class SefazCertificateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SefazCertificateManager.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final String certificatePath;
    private final String certificatePassword;

    public SefazCertificateManager(
            @Value("${SEFAZ_CERT_PATH:}") String certificatePath,
            @Value("${SEFAZ_CERT_PASSWORD:}") String certificatePassword) {
        this.certificatePath = certificatePath;
        this.certificatePassword = certificatePassword;
    }

    public HttpComponentsMessageSender createMessageSender()
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        HttpComponentsMessageSender sender = new HttpComponentsMessageSender();
        sender.setConnectionTimeout(TIMEOUT);
        sender.setReadTimeout(TIMEOUT);
        sender.setHttpClient(createHttpClient());
        return sender;
    }

    public boolean isCertificateValid() {
        try {
            KeyStore keyStore = loadKeyStore();
            Enumeration<String> aliases = keyStore.aliases();
            if (!aliases.hasMoreElements()) {
                return false;
            }
            String alias = aliases.nextElement();
            Certificate certificate = keyStore.getCertificate(alias);
            if (certificate instanceof X509Certificate x509Certificate) {
                x509Certificate.checkValidity();
            }
            return true;
        } catch (CertificateException ex) {
            LOGGER.warn("Certificado SEFAZ inválido ou expirado", ex);
            return false;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException ex) {
            LOGGER.warn("Não foi possível validar o certificado SEFAZ", ex);
            return false;
        }
    }

    private HttpClient createHttpClient()
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        KeyStore keyStore = loadKeyStore();
        char[] password = certificatePassword().toCharArray();
        var sslContext = org.apache.hc.core5.ssl.SSLContexts.custom()
                .loadKeyMaterial(keyStore, password)
                .loadTrustMaterial(null, (chain, authType) -> true)
                .build();
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.of(TIMEOUT))
                .build();
        return HttpClients.custom()
                .setDefaultSocketConfig(socketConfig)
                .setSSLContext(sslContext)
                .build();
    }

    private KeyStore loadKeyStore()
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        String path = certificatePath();
        Path normalized = Paths.get(path).toAbsolutePath().normalize();
        if (!Files.exists(normalized)) {
            throw new IOException("Certificado SEFAZ não encontrado em " + normalized);
        }
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream inputStream = Files.newInputStream(normalized)) {
            keyStore.load(inputStream, certificatePassword().toCharArray());
        }
        return keyStore;
    }

    private String certificatePath() {
        if (!StringUtils.hasText(certificatePath)) {
            throw new IllegalStateException("Variável de ambiente SEFAZ_CERT_PATH não configurada");
        }
        return certificatePath;
    }

    private String certificatePassword() {
        if (!StringUtils.hasText(certificatePassword)) {
            throw new IllegalStateException("Variável de ambiente SEFAZ_CERT_PASSWORD não configurada");
        }
        return certificatePassword;
    }

}
