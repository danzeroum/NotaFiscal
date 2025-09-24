package br.com.nfe.processor.adapter.out.sefaz;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SefazVerificationClient {

    private final boolean stubEnabled;

    public SefazVerificationClient(@Value("${sefaz.stub.enabled:true}") boolean stubEnabled) {
        this.stubEnabled = stubEnabled;
    }

    public boolean isValidAccessKey(String accessKey) {
        if (!stubEnabled) {
            return true;
        }
        String normalized = accessKey.replaceAll("[^0-9]", "");
        return normalized.length() == 44;
    }
}
