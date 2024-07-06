package com.nanoxai.marketplace.tests.container.k8s;

import com.nanoxai.marketplace.tests.config.AppConfiguration;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class KubernetesDeploymentService {

    private static final String MARKETPLACE_CONTAINER_CHART = "marketplace-container-marketplace-container-chart";
    private KubernetesClient client;
    private AppConfiguration testConfiguration;

    public void updateDeploymentWithEnvVars(String testID, Duration timeout) {
        log.info("update deployment with env vars for namespace: {}", testConfiguration.getNamespace());
        Deployment existDeployment = client.apps().deployments()
                .inNamespace(testConfiguration.getNamespace())
                .withName(MARKETPLACE_CONTAINER_CHART)
                .get();

        List<EnvVar> envVarList = testConfiguration.getContainerDirectoriesConfig().stream()
                .map(remoteDirectoryHandlerConfig ->
                        new EnvVar(remoteDirectoryHandlerConfig.getName(), String.format(remoteDirectoryHandlerConfig.getPath(), testID), null)).toList();
        existDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).setEnv(envVarList);

        client.apps()
                .deployments()
                .inNamespace(testConfiguration.getNamespace())
                .withName(MARKETPLACE_CONTAINER_CHART)
                .patch(existDeployment);

        Awaitility.await().ignoreExceptions().atMost(timeout).until(() ->
                client.pods().inNamespace(testConfiguration.getNamespace()).list().getItems().stream().filter(pod -> pod.getStatus().getPhase().equalsIgnoreCase("Running")).count() ==
                client.pods().inNamespace(testConfiguration.getNamespace()).list().getItems().size());
    }
}