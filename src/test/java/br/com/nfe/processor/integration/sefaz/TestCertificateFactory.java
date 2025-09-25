package br.com.nfe.processor.integration.sefaz;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public final class TestCertificateFactory {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private TestCertificateFactory() {}

    public static Path createTemporaryKeystore(String password) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        Instant notBefore = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant notAfter = Instant.now().plus(365, ChronoUnit.DAYS);
        X500Name subject = new X500Name("CN=Test, OU=NFe, O=BuildToFlip, L=Sao Paulo, ST=SP, C=BR");
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject,
                java.math.BigInteger.valueOf(System.currentTimeMillis()),
                Date.from(notBefore),
                Date.from(notAfter),
                subject,
                keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(keyPair.getPrivate());
        X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(builder.build(signer));

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("sefaz", keyPair.getPrivate(), password.toCharArray(), new java.security.cert.Certificate[] {certificate});

        Path file = Files.createTempFile("sefaz", ".p12");
        try (var output = Files.newOutputStream(file)) {
            keyStore.store(output, password.toCharArray());
        }
        return file;
    }
}
