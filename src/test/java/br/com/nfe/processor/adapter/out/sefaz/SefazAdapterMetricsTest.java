package br.com.nfe.processor.adapter.out.sefaz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;

class SefazAdapterMetricsTest {

    private SimpleMeterRegistry registry;
    private WebServiceTemplate webServiceTemplate;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        webServiceTemplate = mock(WebServiceTemplate.class);
    }

    @Test
    void shouldRecordSuccessfulCheck() throws Exception {
        doAnswer(invocation -> {
            Result result = invocation.getArgument(1);
            TransformerFactory.newInstance()
                    .newTransformer()
                    .transform(new StreamSource(new java.io.StringReader(successResponse("100"))), result);
            return null;
        }).when(webServiceTemplate).sendSourceAndReceiveToResult(any(), any(Result.class));

        SefazAdapter adapter = new SefazAdapter(webServiceTemplate, "homolog", registry);

        SefazStatus status = adapter.checkStatus("12345678901234567890123456789012345678901234");

        assertThat(status).isEqualTo(SefazStatus.AUTORIZADA);
        assertThat(registry.counter("sefaz.check.total", "result", "success").count()).isEqualTo(1);
        assertThat(registry.find("sefaz.check.duration").timer()).isPresent();
    }

    @Test
    void shouldRecordFailureOnFallback() {
        SefazAdapter adapter = new SefazAdapter(webServiceTemplate, "homolog", registry);

        adapter.fallbackStatus("123", new WebServiceIOException("fail"));

        assertThat(registry.counter("sefaz.check.total", "result", "failure").count()).isEqualTo(1);
    }

    private String successResponse(String code) {
        return """
                <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">
                  <soap:Body>
                    <nfeResultMsg xmlns=\"http://www.portalfiscal.inf.br/nfe/wsdl/NfeConsultaProtocolo4\">
                      <retConsSitNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">
                        <cStat>""" + code + """</cStat>
                        <xMotivo>OK</xMotivo>
                      </retConsSitNFe>
                    </nfeResultMsg>
                  </soap:Body>
                </soap:Envelope>
                """;
    }
}
