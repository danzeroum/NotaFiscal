package br.com.nfe.processor.integration.sefaz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.ws.test.server.RequestMatchers.payload;
import static org.springframework.ws.test.server.ResponseCreators.withPayload;

import br.com.nfe.processor.adapter.out.sefaz.SefazClient;
import br.com.nfe.processor.adapter.out.sefaz.SefazStatus;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.server.MockWebServiceServer;

@SpringBootTest(properties = {
        "sefaz.stub.enabled=false",
        "sefaz.endpoint=https://sefaz.test/ws"
})
@TestInstance(Lifecycle.PER_CLASS)
class SefazAdapterIntegrationTest {

    private static final String PASSWORD = "changeit";
    private static final String ACCESS_KEY = "12345678901234567890123456789012345678901234";
    private static Path keyStorePath;

    static {
        try {
            keyStorePath = TestCertificateFactory.createTemporaryKeystore(PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private SefazClient sefazClient;

    @Autowired
    private WebServiceTemplate webServiceTemplate;

    private MockWebServiceServer server;

    @DynamicPropertySource
    static void sefazProperties(DynamicPropertyRegistry registry) {
        registry.add("SEFAZ_CERT_PATH", () -> keyStorePath.toString());
        registry.add("SEFAZ_CERT_PASSWORD", () -> PASSWORD);
        registry.add("SEFAZ_ENV", () -> "homolog");
        registry.add("sefaz.allowed-cert-dirs", () -> keyStorePath.getParent().toString());
    }

    @BeforeEach
    void setUp() {
        server = MockWebServiceServer.createServer(webServiceTemplate);
    }

    @Test
    void shouldReturnAuthorizedStatus() {
        expectResponse("100");

        SefazStatus status = sefazClient.checkStatus(ACCESS_KEY);

        assertThat(status).isEqualTo(SefazStatus.AUTORIZADA);
        server.verify();
    }

    @Test
    void shouldReturnCanceledStatus() {
        expectResponse("101");

        SefazStatus status = sefazClient.checkStatus(ACCESS_KEY);

        assertThat(status).isEqualTo(SefazStatus.CANCELADA);
        server.verify();
    }

    @Test
    void shouldReturnDeniedStatus() {
        expectResponse("302");

        SefazStatus status = sefazClient.checkStatus(ACCESS_KEY);

        assertThat(status).isEqualTo(SefazStatus.DENEGADA);
        server.verify();
    }

    @Test
    void shouldReturnInexistentStatus() {
        expectResponse("217");

        SefazStatus status = sefazClient.checkStatus(ACCESS_KEY);

        assertThat(status).isEqualTo(SefazStatus.INEXISTENTE);
        server.verify();
    }

    @Test
    void shouldReturnUnavailableWhenTimeoutOccurs() {
        server.expect(payload(new StreamSource(testRequestXml()))).andRespond(message -> {
            throw new SocketTimeoutException("timeout");
        });

        SefazStatus status = sefazClient.checkStatus(ACCESS_KEY);

        assertThat(status).isEqualTo(SefazStatus.INDISPONIVEL);
        server.verify();
    }

    private void expectResponse(String cStat) {
        server.expect(payload(new StreamSource(testRequestXml()))).andRespond(withPayload(new StreamSource(testResponse(cStat))));
    }

    private java.io.StringReader testRequestXml() {
        return new java.io.StringReader("""
                <nfeDadosMsg xmlns=\"http://www.portalfiscal.inf.br/nfe/wsdl/NfeConsultaProtocolo4\">
                  <consSitNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">
                    <tpAmb>2</tpAmb>
                    <xServ>CONSULTAR</xServ>
                    <chNFe>""" + ACCESS_KEY + """</chNFe>
                  </consSitNFe>
                </nfeDadosMsg>
                """);
    }

    private java.io.StringReader testResponse(String code) {
        return new java.io.StringReader("""
                <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">
                  <soap:Body>
                    <nfeResultMsg xmlns=\"http://www.portalfiscal.inf.br/nfe/wsdl/NfeConsultaProtocolo4\">
                      <retConsSitNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">
                        <cStat>""" + code + """</cStat>
                        <xMotivo>status""" + code + """</xMotivo>
                      </retConsSitNFe>
                    </nfeResultMsg>
                  </soap:Body>
                </soap:Envelope>
                """);
    }
}
