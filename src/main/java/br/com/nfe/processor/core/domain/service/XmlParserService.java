package br.com.nfe.processor.core.domain.service;

import br.com.nfe.processor.core.domain.service.dto.ParsedInvoice;
import br.com.nfe.processor.core.domain.valueobject.Cnpj;
import br.com.nfe.processor.exception.UnprocessableEntityException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Service
public class XmlParserService {

    public ParsedInvoice parse(String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setIgnoringComments(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            Element infNFe = (Element) document.getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", "infNFe")
                    .item(0);
            if (infNFe == null) {
                throw new UnprocessableEntityException("XML de NFe inválido: infNFe ausente");
            }
            String accessKey = Optional.ofNullable(infNFe.getAttribute("Id"))
                    .filter(id -> !id.isBlank())
                    .orElseThrow(() -> new UnprocessableEntityException("Chave de acesso ausente"));

            Element emit = (Element) infNFe.getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", "emit")
                    .item(0);
            Element dest = (Element) infNFe.getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", "dest")
                    .item(0);
            Element totals = (Element) infNFe.getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", "ICMSTot")
                    .item(0);

            if (emit == null || dest == null || totals == null) {
                throw new UnprocessableEntityException("XML incompleto para emissão de NFe");
            }

            String emitterName = textContent(emit, "xNome");
            Cnpj emitterTaxId = parseCnpj(emit);
            String recipientName = textContent(dest, "xNome");
            String recipientTaxId = firstNonEmpty(dest, "CNPJ", "CPF");

            NodeList detList = infNFe.getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", "det");
            int itemCount = detList.getLength();

            BigDecimal calculatedProducts = BigDecimal.ZERO;
            for (int i = 0; i < detList.getLength(); i++) {
                Element det = (Element) detList.item(i);
                Element prod = (Element) det.getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", "prod")
                        .item(0);
                if (prod != null) {
                    calculatedProducts = calculatedProducts.add(parseMoney(prod, "vProd"));
                }
            }

            BigDecimal vNF = parseMoney(totals, "vNF");
            BigDecimal vICMS = parseMoney(totals, "vICMS");
            BigDecimal vIPI = parseMoney(totals, "vIPI");
            BigDecimal vISS = hasChild(totals, "vISS") ? parseMoney(totals, "vISS") : BigDecimal.ZERO;

            Element imposto = (Element) infNFe.getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", "imposto")
                    .item(0);
            String cfop = null;
            if (imposto != null) {
                NodeList cfopNodes = imposto.getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", "CFOP");
                if (cfopNodes.getLength() > 0) {
                    cfop = cfopNodes.item(0).getTextContent();
                }
            }

            return new ParsedInvoice(
                    accessKey,
                    emitterName,
                    emitterTaxId,
                    recipientName,
                    recipientTaxId,
                    itemCount,
                    vNF.setScale(2, RoundingMode.HALF_UP),
                    calculatedProducts.setScale(2, RoundingMode.HALF_UP),
                    vICMS.setScale(2, RoundingMode.HALF_UP),
                    vIPI.setScale(2, RoundingMode.HALF_UP),
                    vISS.setScale(2, RoundingMode.HALF_UP),
                    cfop);
        } catch (UnprocessableEntityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnprocessableEntityException("Falha ao parsear XML de NFe: " + ex.getMessage());
        }
    }

    private BigDecimal parseMoney(Element element, String tag) {
        return new BigDecimal(textContent(element, tag));
    }

    private String textContent(Element element, String tagName) {
        NodeList list = element.getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", tagName);
        if (list.getLength() == 0) {
            throw new UnprocessableEntityException("Campo obrigatório ausente: " + tagName);
        }
        return list.item(0).getTextContent();
    }

    private boolean hasChild(Element element, String tagName) {
        return element.getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", tagName).getLength() > 0;
    }

    private String firstNonEmpty(Element element, String... tags) {
        for (String tag : tags) {
            NodeList list = element.getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", tag);
            if (list.getLength() > 0) {
                String value = list.item(0).getTextContent();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        throw new UnprocessableEntityException("Documento fiscal sem identificador válido");
    }

    private Cnpj parseCnpj(Element element) {
        String raw = textContent(element, "CNPJ");
        try {
            return new Cnpj(raw);
        } catch (IllegalArgumentException ex) {
            throw new UnprocessableEntityException("CNPJ do emitente inválido: " + ex.getMessage());
        }
    }
}
