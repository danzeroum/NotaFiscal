package br.com.nfe.processor.adapter.out.sefaz;

public interface SefazClient {
    SefazStatus checkStatus(String accessKey);
}
