package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.nfe.processor.config.TraceIdFilter;
import br.com.nfe.processor.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldDecorateProblemDetailWithTraceAndInstance() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/batches");

        MDC.put(TraceIdFilter.TRACE_ID_KEY, "trace-test");
        try {
            ProblemDetail problem = handler.handleUnexpected(new RuntimeException("boom"), request);

            assertThat(problem.getInstance()).isEqualTo("/batches");
            assertThat(problem.getType()).contains("internal-error");
            assertThat(problem.getTitle()).isEqualTo("Erro interno");
            assertThat(problem.getProperties()).containsEntry(TraceIdFilter.TRACE_ID_KEY, "trace-test");
            assertThat(problem.getProperties()).containsKey("timestamp");
        } finally {
            MDC.remove(TraceIdFilter.TRACE_ID_KEY);
        }
    }
}

