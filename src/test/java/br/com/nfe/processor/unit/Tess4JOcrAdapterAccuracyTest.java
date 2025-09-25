package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.nfe.processor.adapter.out.ocr.Tess4JOcrAdapter;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class Tess4JOcrAdapterAccuracyTest {

    @Test
    void shouldKeepAccessKeyExtractionAccuracyAboveSeventyPercent() throws Exception {
        Tess4JOcrAdapter adapter = new Tess4JOcrAdapter(
                true, "por", "/usr/share/tessdata", "27865757000102", "12345678909");
        Method method = Tess4JOcrAdapter.class.getDeclaredMethod("buildXmlFromText", String.class);
        method.setAccessible(true);

        Map<String, String> dataset = new LinkedHashMap<>();
        dataset.put(sampleTextOne(), "35220812345678000123550010000000011000000011");
        dataset.put(sampleTextTwo(), "29230598765432000123550010000000022000000022");
        dataset.put(sampleTextThree(), "21191244556677000123550010000000033000000033");
        dataset.put(sampleTextFour(), "43191288990011000123550010000000044000000044");
        dataset.put(sampleTextFive(), "51190566778899000123550010000000055000000055");

        long successes = dataset.entrySet().stream()
                .filter(entry -> extractAccessKey(method, adapter, entry.getKey())
                        .map(xml -> xml.contains(entry.getValue()))
                        .orElse(false))
                .count();

        double accuracy = (successes * 100.0) / dataset.size();
        assertThat(accuracy).isGreaterThanOrEqualTo(70.0);
    }

    @SuppressWarnings("unchecked")
    private Optional<String> extractAccessKey(Method method, Tess4JOcrAdapter adapter, String sample)
            throws Exception {
        return (Optional<String>) method.invoke(adapter, sample);
    }

    private String sampleTextOne() {
        return """
                CHAVE DE ACESSO 35220812345678000123550010000000011000000011
                Emitente: SUPERMERCADOS IDEAL LTDA
                CNPJ: 12.345.678/0001-95
                Destinatário: CLIENTE TESTE
                CPF: 987.654.321-00
                Valor Total: 1.500,00
                Valor dos Produtos: 1.400,00
                ICMS: 100,00
                IPI: 0,00
                ISS: 0,00
                CFOP 5102
                """;
    }

    private String sampleTextTwo() {
        return """
                Chave de Acesso: 29230598765432000123550010000000022000000022
                EMITENTE ACME IMPORTACOES LTDA 00.111.222/0001-33
                Dest: CONSUMIDOR BETA CPF 123.456.789-01
                Valor dos Produtos 2.100,50
                Valor Total 2.200,50
                ICMS 80,00
                IPI 20,00
                ISS 0,00
                CFOP 5405
                """;
    }

    private String sampleTextThree() {
        return """
                CHAVE 21191244556677000123550010000000033000000033
                Emit: FARMACIA POPULAR SA CNPJ 55.666.777/0001-88
                Destinatário: HOSPITAL CENTRAL
                CNPJ: 22.333.444/0001-55
                Total da Nota 3.500,99
                Produtos 3.490,99
                ICMS 10,00
                IPI 0,00
                ISS 0,00
                CFOP 6108
                """;
    }

    private String sampleTextFour() {
        return """
                Chave NFe 43191288990011000123550010000000044000000044
                Emitente: INDUSTRIA METAL BRASIL LTDA
                CNPJ 43.198.765/0001-22
                Destinatário: CONSTRUCOES NOVA ERA
                CPF 102.938.475-66
                Valor Total da Nota 8.900,00
                Valor dos Produtos: 8.500,00
                ICMS: 300,00
                IPI: 50,00
                ISS: 50,00
                CFOP 5101
                """;
    }

    private String sampleTextFive() {
        return """
                acesso 51190566778899000123550010000000055000000055
                Emitente: PADARIA SABOR CASEIRO ME
                CNPJ 51.190.566/0001-77
                Dest: CLIENTE FINAL CPF 65432198700
                Valor Total: 250,00
                Valor dos Produtos: 200,00
                ICMS: 30,00
                IPI: 0,00
                ISS: 20,00
                CFOP 5102
                """;
    }
}
