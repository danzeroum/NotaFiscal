package br.com.nfe.processor.integration.sefaz;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

public final class TestCertificateFactory {

    private TestCertificateFactory() {}

    @SuppressWarnings("removal")
    public static Path createTemporaryKeystore(String password) throws Exception {
        CertAndKeyGen keyGen = new CertAndKeyGen("RSA", "SHA256withRSA");
        keyGen.generate(2048);
        X500Name x500Name = new X500Name("CN=Test, OU=NFe, O=BuildToFlip, L=Sao Paulo, ST=SP, C=BR");
        X509Certificate certificate = keyGen.getSelfCertificate(x500Name, new Date(), 365L * 24 * 60 * 60);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("sefaz", keyGen.getPrivateKey(), password.toCharArray(), new java.security.cert.Certificate[] {certificate});
        Path file = Files.createTempFile("sefaz", ".p12");
        try (var output = Files.newOutputStream(file)) {
            keyStore.store(output, password.toCharArray());
        }
        return file;
    }
}
