package com.nanoxai.marketplace.tests.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
@Configuration
public class KubernetesConfiguration {
    @Bean
    public KubernetesClient kubernetesClient(Optional<Config> config) {
        return config.map(DefaultKubernetesClient::new)
                .orElse(new DefaultKubernetesClient());
    }
}