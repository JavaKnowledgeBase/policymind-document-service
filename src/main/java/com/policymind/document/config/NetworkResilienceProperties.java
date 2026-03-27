package com.policymind.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.network")
public class NetworkResilienceProperties {

    private ServicePolicy defaults = ServicePolicy.defaultPolicy();
    private Map<String, ServicePolicy> services = new HashMap<>();

    public ServicePolicy getDefaults() {
        return defaults;
    }

    public void setDefaults(ServicePolicy defaults) {
        this.defaults = defaults;
    }

    public Map<String, ServicePolicy> getServices() {
        return services;
    }

    public void setServices(Map<String, ServicePolicy> services) {
        this.services = services;
    }

    public ServicePolicy policyFor(String serviceName) {
        ServicePolicy resolved = ServicePolicy.defaultPolicy();
        if (defaults != null) {
            resolved.applyOverrides(defaults);
        }
        ServicePolicy override = services.get(serviceName);
        if (override != null) {
            resolved.applyOverrides(override);
        }
        return resolved;
    }

    public static class ServicePolicy {
        private Duration connectTimeout;
        private Duration readTimeout;
        private Duration callTimeout;
        private Integer retryMaxAttempts;
        private Duration retryWaitDuration;
        private Float circuitBreakerFailureRateThreshold;
        private Integer circuitBreakerSlidingWindowSize;
        private Integer circuitBreakerMinimumNumberOfCalls;
        private Duration circuitBreakerWaitDurationInOpenState;
        private Integer circuitBreakerPermittedCallsInHalfOpenState;
        private Integer maxConcurrentCalls;
        private Duration maxWaitDuration;

        static ServicePolicy defaultPolicy() {
            ServicePolicy policy = new ServicePolicy();
            policy.setConnectTimeout(Duration.ofSeconds(2));
            policy.setReadTimeout(Duration.ofSeconds(10));
            policy.setCallTimeout(Duration.ofSeconds(12));
            policy.setRetryMaxAttempts(3);
            policy.setRetryWaitDuration(Duration.ofMillis(300));
            policy.setCircuitBreakerFailureRateThreshold(50.0f);
            policy.setCircuitBreakerSlidingWindowSize(10);
            policy.setCircuitBreakerMinimumNumberOfCalls(5);
            policy.setCircuitBreakerWaitDurationInOpenState(Duration.ofSeconds(30));
            policy.setCircuitBreakerPermittedCallsInHalfOpenState(3);
            policy.setMaxConcurrentCalls(8);
            policy.setMaxWaitDuration(Duration.ofMillis(250));
            return policy;
        }

        public void applyOverrides(ServicePolicy override) {
            if (override.connectTimeout != null) {
                connectTimeout = override.connectTimeout;
            }
            if (override.readTimeout != null) {
                readTimeout = override.readTimeout;
            }
            if (override.callTimeout != null) {
                callTimeout = override.callTimeout;
            }
            if (override.retryMaxAttempts != null) {
                retryMaxAttempts = override.retryMaxAttempts;
            }
            if (override.retryWaitDuration != null) {
                retryWaitDuration = override.retryWaitDuration;
            }
            if (override.circuitBreakerFailureRateThreshold != null) {
                circuitBreakerFailureRateThreshold = override.circuitBreakerFailureRateThreshold;
            }
            if (override.circuitBreakerSlidingWindowSize != null) {
                circuitBreakerSlidingWindowSize = override.circuitBreakerSlidingWindowSize;
            }
            if (override.circuitBreakerMinimumNumberOfCalls != null) {
                circuitBreakerMinimumNumberOfCalls = override.circuitBreakerMinimumNumberOfCalls;
            }
            if (override.circuitBreakerWaitDurationInOpenState != null) {
                circuitBreakerWaitDurationInOpenState = override.circuitBreakerWaitDurationInOpenState;
            }
            if (override.circuitBreakerPermittedCallsInHalfOpenState != null) {
                circuitBreakerPermittedCallsInHalfOpenState = override.circuitBreakerPermittedCallsInHalfOpenState;
            }
            if (override.maxConcurrentCalls != null) {
                maxConcurrentCalls = override.maxConcurrentCalls;
            }
            if (override.maxWaitDuration != null) {
                maxWaitDuration = override.maxWaitDuration;
            }
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public Duration getCallTimeout() {
            return callTimeout;
        }

        public void setCallTimeout(Duration callTimeout) {
            this.callTimeout = callTimeout;
        }

        public Integer getRetryMaxAttempts() {
            return retryMaxAttempts;
        }

        public void setRetryMaxAttempts(Integer retryMaxAttempts) {
            this.retryMaxAttempts = retryMaxAttempts;
        }

        public Duration getRetryWaitDuration() {
            return retryWaitDuration;
        }

        public void setRetryWaitDuration(Duration retryWaitDuration) {
            this.retryWaitDuration = retryWaitDuration;
        }

        public Float getCircuitBreakerFailureRateThreshold() {
            return circuitBreakerFailureRateThreshold;
        }

        public void setCircuitBreakerFailureRateThreshold(Float circuitBreakerFailureRateThreshold) {
            this.circuitBreakerFailureRateThreshold = circuitBreakerFailureRateThreshold;
        }

        public Integer getCircuitBreakerSlidingWindowSize() {
            return circuitBreakerSlidingWindowSize;
        }

        public void setCircuitBreakerSlidingWindowSize(Integer circuitBreakerSlidingWindowSize) {
            this.circuitBreakerSlidingWindowSize = circuitBreakerSlidingWindowSize;
        }

        public Integer getCircuitBreakerMinimumNumberOfCalls() {
            return circuitBreakerMinimumNumberOfCalls;
        }

        public void setCircuitBreakerMinimumNumberOfCalls(Integer circuitBreakerMinimumNumberOfCalls) {
            this.circuitBreakerMinimumNumberOfCalls = circuitBreakerMinimumNumberOfCalls;
        }

        public Duration getCircuitBreakerWaitDurationInOpenState() {
            return circuitBreakerWaitDurationInOpenState;
        }

        public void setCircuitBreakerWaitDurationInOpenState(Duration circuitBreakerWaitDurationInOpenState) {
            this.circuitBreakerWaitDurationInOpenState = circuitBreakerWaitDurationInOpenState;
        }

        public Integer getCircuitBreakerPermittedCallsInHalfOpenState() {
            return circuitBreakerPermittedCallsInHalfOpenState;
        }

        public void setCircuitBreakerPermittedCallsInHalfOpenState(Integer circuitBreakerPermittedCallsInHalfOpenState) {
            this.circuitBreakerPermittedCallsInHalfOpenState = circuitBreakerPermittedCallsInHalfOpenState;
        }

        public Integer getMaxConcurrentCalls() {
            return maxConcurrentCalls;
        }

        public void setMaxConcurrentCalls(Integer maxConcurrentCalls) {
            this.maxConcurrentCalls = maxConcurrentCalls;
        }

        public Duration getMaxWaitDuration() {
            return maxWaitDuration;
        }

        public void setMaxWaitDuration(Duration maxWaitDuration) {
            this.maxWaitDuration = maxWaitDuration;
        }
    }
}
