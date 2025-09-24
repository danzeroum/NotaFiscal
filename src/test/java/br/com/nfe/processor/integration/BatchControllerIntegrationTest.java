package br.com.nfe.processor.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BatchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateBatchFromZip() throws Exception {
        byte[] zip = zipWith(Map.of(
                "ok-01.xml", TestSamples.okInvoice1(),
                "ok-02.xml", TestSamples.okInvoice2()));
        MockMultipartFile file = new MockMultipartFile("file", "ok-two-xml.zip", "application/zip", zip);
        mockMvc.perform(multipart("/batches").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void shouldReturnBatchSummary() throws Exception {
        byte[] zip = zipWith(Map.of(
                "ok-01.xml", TestSamples.okInvoice1(),
                "ok-02.xml", TestSamples.okInvoice2()));
        MockMultipartFile file = new MockMultipartFile("file", "ok-two-xml.zip", "application/zip", zip);
        String response = mockMvc.perform(multipart("/batches").file(file))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String batchId = JsonTestSupport.extractId(response);

        mockMvc.perform(get("/batches/" + batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(batchId))
                .andExpect(jsonPath("$.stats.invoicesTotal").value(2));
    }

    @Test
    void shouldFailWhenPdfWithoutOcr() throws Exception {
        byte[] zip = zipWith(Map.of("nota.pdf", TestSamples.placeholderPdf()));
        MockMultipartFile file = new MockMultipartFile("file", "pdf-no-xml.zip", "application/zip", zip);
        mockMvc.perform(multipart("/batches").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.instance").value("/batches"));
    }

    private byte[] zipWith(Map<String, String> files) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zip.putNextEntry(zipEntry);
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }
}
