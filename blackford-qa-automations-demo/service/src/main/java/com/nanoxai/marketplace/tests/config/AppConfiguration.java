package com.nanoxai.marketplace.tests.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties("marketplace-e2e")
public class AppConfiguration {
    private String host;
    private String hostMock;
    private String jobConfigPath;
    private String image;
    private String namespace;
    private List<DirectoryConfig> containerDirectoriesConfig;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .build();
    }

    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }
}