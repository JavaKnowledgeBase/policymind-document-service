package com.policymind.document.service;

import com.policymind.document.config.NetworkResilienceProperties;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Component
public class OutboundCallExecutor {

    private final NetworkResilienceProperties properties;
    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final ExecutorService executorService;

    public OutboundCallExecutor(NetworkResilienceProperties properties,
                                RetryRegistry retryRegistry,
                                CircuitBreakerRegistry circuitBreakerRegistry,
                                BulkheadRegistry bulkheadRegistry,
                                TimeLimiterRegistry timeLimiterRegistry) {
        this.properties = properties;
        this.retryRegistry = retryRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
        this.executorService = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setName("outbound-call-" + thread.threadId());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public <T> T execute(String serviceName, Supplier<T> supplier) {
        NetworkResilienceProperties.ServicePolicy policy = properties.policyFor(serviceName);

        Retry retry = retryRegistry.retry(serviceName, buildRetryConfig(policy));
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName, buildCircuitBreakerConfig(policy));
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(serviceName, buildBulkheadConfig(policy));
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(serviceName, buildTimeLimiterConfig(policy));

        java.util.concurrent.Callable<T> timedCallable = TimeLimiter.decorateFutureSupplier(
                timeLimiter,
                () -> CompletableFuture.supplyAsync(supplier, executorService)
        );
        java.util.concurrent.Callable<T> circuitProtected = CircuitBreaker.decorateCallable(circuitBreaker, timedCallable);
        java.util.concurrent.Callable<T> isolated = Bulkhead.decorateCallable(bulkhead, circuitProtected);
        java.util.concurrent.Callable<T> retried = Retry.decorateCallable(retry, isolated);

        try {
            return retried.call();
        } catch (Exception ex) {
            throw rethrow(serviceName, unwrap(ex));
        }
    }

    private RetryConfig buildRetryConfig(NetworkResilienceProperties.ServicePolicy policy) {
        return RetryConfig.custom()
                .maxAttempts(policy.getRetryMaxAttempts())
                .waitDuration(policy.getRetryWaitDuration())
                .retryOnException(this::isRetryable)
                .build();
    }

    private CircuitBreakerConfig buildCircuitBreakerConfig(NetworkResilienceProperties.ServicePolicy policy) {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(policy.getCircuitBreakerFailureRateThreshold())
                .slidingWindowSize(policy.getCircuitBreakerSlidingWindowSize())
                .minimumNumberOfCalls(policy.getCircuitBreakerMinimumNumberOfCalls())
                .waitDurationInOpenState(policy.getCircuitBreakerWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(policy.getCircuitBreakerPermittedCallsInHalfOpenState())
                .recordException(this::isRecordable)
                .build();
    }

    private BulkheadConfig buildBulkheadConfig(NetworkResilienceProperties.ServicePolicy policy) {
        return BulkheadConfig.custom()
                .maxConcurrentCalls(policy.getMaxConcurrentCalls())
                .maxWaitDuration(policy.getMaxWaitDuration())
                .build();
    }

    private TimeLimiterConfig buildTimeLimiterConfig(NetworkResilienceProperties.ServicePolicy policy) {
        return TimeLimiterConfig.custom()
                .timeoutDuration(policy.getCallTimeout())
                .cancelRunningFuture(true)
                .build();
    }

    private boolean isRetryable(Throwable throwable) {
        Throwable root = unwrap(throwable);
        if (root instanceof IllegalArgumentException) {
            return false;
        }
        if (root instanceof HttpClientErrorException clientError) {
            return clientError.getStatusCode().value() == 429;
        }
        return root instanceof HttpServerErrorException
                || root instanceof ResourceAccessException
                || root instanceof SocketTimeoutException
                || root instanceof ConnectException
                || root instanceof TimeoutException
                || root instanceof IOException
                || root instanceof CallNotPermittedException
                || root instanceof BulkheadFullException;
    }

    private boolean isRecordable(Throwable throwable) {
        Throwable root = unwrap(throwable);
        if (root instanceof IllegalArgumentException) {
            return false;
        }
        if (root instanceof HttpClientErrorException clientError) {
            return clientError.getStatusCode().value() == 429;
        }
        return true;
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private RuntimeException rethrow(String serviceName, Throwable throwable) {
        if (throwable instanceof TimeoutException) {
            return new OutboundCallTimeoutException(serviceName, throwable);
        }
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException("Outbound network call failed", throwable);
    }
}
