package no.ssb.forbruk.nets.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MetricsManager  {
    private static final Logger logger = LoggerFactory.getLogger(MetricsManager.class);

    @Autowired
    private MeterRegistry meterRegistry;

    public void trackCounterMetrics(String metricName, double increment, String... tags) {
        logger.info("Called trackCounterMetrics - " + LocalDateTime.now());
        meterRegistry.counter(metricName, tags).increment(increment);
    }

    public void trackGaugeMetrics(String metricName, double value, String... tags) {
        meterRegistry.gauge(metricName, (Iterable) Tags.of(tags), value);
    }

    public void registerGauge(String metricName, Double value) {
        Gauge.builder(metricName, value, Double::valueOf).register(meterRegistry);
    }



}
