package br.com.nfe.processor.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

final class JsonTestSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonTestSupport() {
    }

    static String extractId(String responseBody) {
        try {
            JsonNode node = MAPPER.readTree(responseBody);
            return node.get("id").asText();
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao parsear resposta JSON", ex);
        }
    }
}
