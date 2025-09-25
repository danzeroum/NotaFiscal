package br.com.nfe.processor.adapter.out.sefaz;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;

@Component
@ConditionalOnProperty(value = "sefaz.stub.enabled", havingValue = "false")
public class SefazAdapter implements SefazClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SefazAdapter.class);
    private static final Map<String, SefazStatus> STATUS_MAP = createStatusMap();

    private final WebServiceTemplate webServiceTemplate;
    private final String environment;

    public SefazAdapter(
            WebServiceTemplate webServiceTemplate,
            @Value("${SEFAZ_ENV:homolog}") String environment) {
        this.webServiceTemplate = webServiceTemplate;
        this.environment = environment;
    }

    @Override
    @CircuitBreaker(name = "sefazClient", fallbackMethod = "fallbackStatus")
    public SefazStatus checkStatus(String accessKey) {
        if (!StringUtils.hasText(accessKey)) {
            return SefazStatus.INEXISTENTE;
        }
        try {
            DOMResult result = new DOMResult();
            webServiceTemplate.sendSourceAndReceiveToResult(
                    new StreamSource(new ByteArrayInputStream(buildRequest(accessKey).getBytes(StandardCharsets.UTF_8))),
                    result);
            return parseStatus(result);
        } catch (WebServiceIOException ex) {
            LOGGER.error("Falha de comunicação com a SEFAZ", ex);
            throw ex;
        } catch (IOException | TransformerException ex) {
            LOGGER.error("Erro ao consultar status na SEFAZ", ex);
            throw new WebServiceIOException("Erro ao consultar SEFAZ", ex);
        }
    }

    @SuppressWarnings("unused")
    SefazStatus fallbackStatus(String accessKey, Throwable throwable) {
        LOGGER.warn("Retornando status INDISPONIVEL para chave {} devido a: {}", accessKey, throwable.getMessage());
        return SefazStatus.INDISPONIVEL;
    }

    private String buildRequest(String accessKey) {
        String amb = "prod".equalsIgnoreCase(environment) ? "1" : "2";
        return """
                <nfeDadosMsg xmlns=\"http://www.portalfiscal.inf.br/nfe/wsdl/NfeConsultaProtocolo4\">
                  <consSitNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">
                    <tpAmb>%s</tpAmb>
                    <xServ>CONSULTAR</xServ>
                    <chNFe>%s</chNFe>
                  </consSitNFe>
                </nfeDadosMsg>
                """.formatted(amb, accessKey);
    }

    private SefazStatus parseStatus(Result result) throws TransformerException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        TransformerFactory.newInstance().newTransformer().transform(result, new StreamResult(buffer));
        String payload = buffer.toString(StandardCharsets.UTF_8);
        String code = extractValue(payload, "cStat");
        if (code == null) {
            LOGGER.warn("Resposta da SEFAZ sem cStat: {}", payload);
            return SefazStatus.INDISPONIVEL;
        }
        return STATUS_MAP.getOrDefault(code, SefazStatus.DENEGADA);
    }

    private String extractValue(String xml, String tag) {
        int start = xml.indexOf("<" + tag + ">");
        int endTag = xml.indexOf("</" + tag + ">");
        if (start == -1 || endTag == -1) {
            return null;
        }
        int valueStart = start + tag.length() + 2;
        return xml.substring(valueStart, endTag).trim();
    }

    private static Map<String, SefazStatus> createStatusMap() {
        Map<String, SefazStatus> map = new HashMap<>();
        map.put("100", SefazStatus.AUTORIZADA);
        map.put("101", SefazStatus.CANCELADA);
        map.put("110", SefazStatus.CANCELADA);
        map.put("135", SefazStatus.CANCELADA);
        map.put("302", SefazStatus.DENEGADA);
        map.put("301", SefazStatus.DENEGADA);
        map.put("204", SefazStatus.DENEGADA);
        map.put("217", SefazStatus.INEXISTENTE);
        map.put("562", SefazStatus.INEXISTENTE);
        return map;
    }
}
