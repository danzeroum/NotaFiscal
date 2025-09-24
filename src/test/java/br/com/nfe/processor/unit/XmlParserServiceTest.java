package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.nfe.processor.core.domain.service.XmlParserService;
import br.com.nfe.processor.core.domain.service.dto.ParsedInvoice;
import br.com.nfe.processor.exception.UnprocessableEntityException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class XmlParserServiceTest {

    private XmlParserService service;

    @BeforeEach
    void setUp() {
        service = new XmlParserService();
    }

    @Test
    void shouldParseValidXml() throws Exception {
        String xml = Files.readString(Path.of("samples/xml/ok-01.xml"));
        ParsedInvoice invoice = service.parse(xml);
        assertThat(invoice.getAccessKey()).isNotBlank();
        assertThat(invoice.getEmitterName()).isEqualTo("Empresa Emitente Ltda");
        assertThat(invoice.getItemCount()).isEqualTo(2);
        assertThat(invoice.getTotalAmount().doubleValue()).isEqualTo(500.00d);
    }

    @Test
    void shouldFailWhenXmlInvalid() {
        String xml = "<xml></xml>";
        assertThatThrownBy(() -> service.parse(xml))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("infNFe");
    }
}
