package br.com.nfe.processor.adapter.out.sefaz;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sefaz.stub.enabled", havingValue = "true", matchIfMissing = true)
public class SefazStubClient implements SefazClient {

    @Override
    public SefazStatus checkStatus(String accessKey) {
        if (accessKey == null) {
            return SefazStatus.INEXISTENTE;
        }
        String normalized = accessKey.replaceAll("[^0-9]", "");
        if (normalized.length() != 44) {
            return SefazStatus.INEXISTENTE;
        }
        return SefazStatus.AUTORIZADA;
    }
}
