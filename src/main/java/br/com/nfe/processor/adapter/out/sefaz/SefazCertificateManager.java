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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
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
    private static final List<Path> DEFAULT_ALLOWED_DIRECTORIES = List.of(
            Paths.get("/etc/sefaz/certificates"),
            Paths.get("/opt/nfe-processor/certs"),
            Paths.get(System.getProperty("java.io.tmpdir")));

    private final String certificatePath;
    private final String certificatePassword;
    private final Duration timeout;
    private final List<Path> allowedDirectories;

    public SefazCertificateManager(
            @Value("${SEFAZ_CERT_PATH:}") String certificatePath,
            @Value("${SEFAZ_CERT_PASSWORD:}") String certificatePassword,
            @Value("${sefaz.timeout-seconds:3}") int timeoutSeconds,
            @Value("${sefaz.allowed-cert-dirs:}") String allowedDirs) {
        this.certificatePath = certificatePath;
        this.certificatePassword = certificatePassword;
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.allowedDirectories = resolveAllowedDirectories(allowedDirs);
    }

    public HttpComponentsMessageSender createMessageSender()
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        HttpComponentsMessageSender sender = new HttpComponentsMessageSender();
        sender.setConnectionTimeout(timeout);
        sender.setReadTimeout(timeout);
        sender.setHttpClient(createHttpClient());
        return sender;
    }

    public boolean isCertificateValid() {
        return getCertificateInfo().isValid();
    }

    public CertificateInfo getCertificateInfo() {
        try {
            KeyStore keyStore = loadKeyStore();
            Enumeration<String> aliases = keyStore.aliases();
            if (!aliases.hasMoreElements()) {
                return CertificateInfo.invalid(null, null);
            }
            String alias = aliases.nextElement();
            Certificate certificate = keyStore.getCertificate(alias);
            if (certificate instanceof X509Certificate x509Certificate) {
                Instant expiration = x509Certificate.getNotAfter().toInstant();
                try {
                    x509Certificate.checkValidity();
                    return CertificateInfo.valid(expiration, x509Certificate.getSubjectX500Principal().getName());
                } catch (CertificateException ex) {
                    LOGGER.warn("Certificado SEFAZ inválido ou expirado", ex);
                    return CertificateInfo.invalid(expiration, x509Certificate.getSubjectX500Principal().getName());
                }
            }
            return CertificateInfo.invalid(null, alias);
        } catch (SecurityException ex) {
            LOGGER.warn("Caminho do certificado SEFAZ não permitido", ex);
            return CertificateInfo.invalid(null, null);
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
            LOGGER.warn("Não foi possível validar o certificado SEFAZ", ex);
            return CertificateInfo.invalid(null, null);
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
                .setSoTimeout(Timeout.of(timeout))
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
        ensureAllowedDirectory(normalized);
        if (!Files.exists(normalized)) {
            throw new IOException("Certificado SEFAZ não encontrado em " + normalized);
        }
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream inputStream = Files.newInputStream(normalized)) {
            keyStore.load(inputStream, certificatePassword().toCharArray());
        }
        return keyStore;
    }

    private void ensureAllowedDirectory(Path normalized) {
        boolean allowed = allowedDirectories.stream()
                .map(path -> path.toAbsolutePath().normalize())
                .anyMatch(normalized::startsWith);
        if (!allowed) {
            String allowedDirs = allowedDirectories.stream()
                    .map(path -> path.toAbsolutePath().normalize().toString())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("nenhum diretório configurado");
            throw new SecurityException(
                    "Certificate path outside allowed directories: "
                            + normalized
                            + " (permitidos: "
                            + allowedDirs
                            + ")");
        }
    }

    private List<Path> resolveAllowedDirectories(String allowedDirsProperty) {
        List<Path> allowed = new ArrayList<>(DEFAULT_ALLOWED_DIRECTORIES);
        if (StringUtils.hasText(allowedDirsProperty)) {
            Stream.of(allowedDirsProperty.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Paths::get)
                    .map(path -> path.toAbsolutePath().normalize())
                    .forEach(allowed::add);
        }
        return allowed;
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

    public record CertificateInfo(boolean valid, Instant expirationDate, String subject) {
        public static CertificateInfo valid(Instant expirationDate, String subject) {
            return new CertificateInfo(true, expirationDate, subject);
        }

        public static CertificateInfo invalid(Instant expirationDate, String subject) {
            return new CertificateInfo(false, expirationDate, subject);
        }
    }
}
