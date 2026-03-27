package com.policymind.document.config;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Component
public class NetworkClientFactory {

    private final NetworkResilienceProperties properties;

    public NetworkClientFactory(NetworkResilienceProperties properties) {
        this.properties = properties;
    }

    public RestTemplate createRestTemplate(String serviceName) {
        return new RestTemplate(requestFactory(serviceName));
    }

    public RestClient createRestClient(String serviceName) {
        return RestClient.builder()
                .requestFactory(requestFactory(serviceName))
                .build();
    }

    private SimpleClientHttpRequestFactory requestFactory(String serviceName) {
        NetworkResilienceProperties.ServicePolicy policy = properties.policyFor(serviceName);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) policy.getConnectTimeout().toMillis());
        factory.setReadTimeout((int) policy.getReadTimeout().toMillis());
        return factory;
    }
}
