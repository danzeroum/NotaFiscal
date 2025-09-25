package br.com.nfe.processor.config;

import br.com.nfe.processor.adapter.out.sefaz.SefazCertificateManager;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;

@Configuration
public class SefazClientConfiguration {

    @Bean
    @ConditionalOnProperty(value = "sefaz.stub.enabled", havingValue = "false")
    public WebServiceTemplate sefazWebServiceTemplate(
            SaajSoapMessageFactory messageFactory,
            SefazCertificateManager certificateManager,
            @Value("${sefaz.endpoint:https://sefaz.virtual/consulta}") String endpointUrl)
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException,
                    UnrecoverableKeyException {
        WebServiceTemplate template = new WebServiceTemplate(messageFactory);
        template.setMessageSender(certificateManager.createMessageSender());
        template.setDefaultUri(endpointUrl);
        return template;
    }
}
