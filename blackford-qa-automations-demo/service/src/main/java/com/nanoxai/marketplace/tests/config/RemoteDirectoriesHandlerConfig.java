package com.nanoxai.marketplace.tests.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Data
@Configuration
@ConfigurationProperties("marketplace-e2e.container-directories-handler")
public class RemoteDirectoriesHandlerConfig {
    private String uploadFilesServiceUrl;
    private String downloadFiledServiceUrl;
    private String listFilesServiceUrl;
    private String deleteFilesServiceUrl;


}
