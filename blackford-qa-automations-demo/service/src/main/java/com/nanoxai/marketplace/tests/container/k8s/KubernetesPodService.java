package com.nanoxai.marketplace.tests.container.k8s;

import com.nanoxai.marketplace.tests.config.AppConfiguration;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import io.fabric8.kubernetes.client.dsl.internal.core.v1.PodOperationsImpl;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

@Service
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class KubernetesPodService {

    private AppConfiguration testConfiguration;

    public void downloadOutputFolder(String runTestId){
        Config config = new ConfigBuilder()
                .withRequestTimeout(60000)  // 60 seconds request timeout
                .withWebsocketTimeout(60000L)  // 60 seconds websocket timeout
                .withWebsocketPingInterval(60000L)  // 60 seconds ping interval
                .build();
        KubernetesClient client = new DefaultKubernetesClient(config);
        File path = new File(System.getProperty("user.dir") + "/src/main/resources/scOutputs");
        PodOperationsImpl podOperations = (PodOperationsImpl) client.pods().inNamespace(testConfiguration.getNamespace()).withName(client.pods().inNamespace(testConfiguration.getNamespace()).list().getItems().get(0).getMetadata().getName());
        podOperations.dir("/marketplace/" + runTestId + "/output").copy(path.toPath());
    }
}