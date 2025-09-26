package br.com.nfe.processor.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class NfeMetricsService {

    private final Counter processedCounter;
    private final Counter errorCounter;
    private final Counter validationFailureCounter;
    private final Timer processingTimer;

    public NfeMetricsService(MeterRegistry meterRegistry) {
        this.processedCounter = Counter.builder("nfe.processed")
                .description("Total de NFes processadas")
                .tag("type", "success")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("nfe.processed")
                .description("Total de NFes processadas")
                .tag("type", "error")
                .register(meterRegistry);

        this.validationFailureCounter = Counter.builder("nfe.validation.failures")
                .description("Total de falhas de validação")
                .register(meterRegistry);

        this.processingTimer = Timer.builder("nfe.processing.time")
                .description("Tempo de processamento de NFe")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void recordSuccess(Timer.Sample sample) {
        stopTimer(sample);
        processedCounter.increment();
    }

    public void recordError(Timer.Sample sample) {
        stopTimer(sample);
        errorCounter.increment();
    }

    public void recordValidationFailure() {
        validationFailureCounter.increment();
    }

    private void stopTimer(Timer.Sample sample) {
        if (sample != null) {
            sample.stop(processingTimer);
        }
    }
}
