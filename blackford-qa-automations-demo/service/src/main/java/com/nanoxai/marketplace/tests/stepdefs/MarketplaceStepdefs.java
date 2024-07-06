package com.nanoxai.marketplace.tests.stepdefs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.nanoxai.marketplace.tests.config.AppConfiguration;
import com.nanoxai.marketplace.tests.container.k8s.KubernetesDeploymentService;
import com.nanoxai.marketplace.tests.container.remote.directories.FileSystemClient;
import com.nanoxai.marketplace.tests.models.AnalyzeRequest;
import com.nanoxai.marketplace.tests.models.MarketplaceErrorObject;
import com.nanoxai.marketplace.tests.models.MarketplaceReportOutput;
import com.nanoxai.marketplace.tests.utils.Utils;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.messages.internal.com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.awaitility.Durations.TEN_MINUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class MarketplaceStepdefs {
    private static final String MARKETPLACE_OUTPUT_DIRECTORY = "CONTAINER_OUTPUT_DIRECTORY";
    private static final String MARKETPLACE_INPUT_DIRECTORY = "CONTAINER_INPUT_DIRECTORY";
    private final FileSystemClient fileSystemClient;
    private final AppConfiguration appConfiguration;
    private final String MARKETPLACE_REPORT_FILE_NAME = "marketplace_data.json";
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final KubernetesDeploymentService kubernetesDeploymentService;
    private static String RUN_TEST_ID;
    private static boolean isSetupRan = false;
    private static final int WIREMOCK_PORT = 443;
    private WireMock wiremock;

    @Before
    public void setup() {
        if (!isSetupRan) {
            log.info("Running beforeAll setup");
            isSetupRan = true;
            iGenerateNewRunTestId();
            configuringEnvVars();
        }
    }

    private void configuringEnvVars() {
        log.info("Getting health on {} ",buildMarketPlaceUrl("/health"));
        kubernetesDeploymentService.updateDeploymentWithEnvVars(RUN_TEST_ID, Duration.ofMinutes(10));
        try{
            Awaitility.await().atMost(TEN_MINUTES)
                    .until(() -> restTemplate.exchange(
                            buildMarketPlaceUrl("/health"),
                            HttpMethod.GET, null, Void.class).getStatusCode() == HttpStatus.OK
                    );
        }
        catch (HttpServerErrorException e) {
            log.error("Error on healthcheck: {}, {}", e.getMessage(), e.getResponseBodyAsString());
        }
    }

    private String buildMarketPlaceUrl(String endPointPath) {
        return "https://" + appConfiguration.getHost() + endPointPath;
    }

    @Given("I configure mock server")
    public void setUpWireMock() {
        wiremock = new WireMock("https", appConfiguration.getHostMock(), WIREMOCK_PORT);
        resetTheMockServer();
    }

    private void resetTheMockServer() {
        log.info("Resetting the wiremock server.");
        wiremock.resetMappings();
        wiremock.resetRequests();
    }

    @SneakyThrows
    private String getFileContent(String fileName) {
        Path filePath = Paths.get(fileName);
        String fileString = Files.readString(new ClassPathResource("/output/" + filePath).getFile().toPath());
        return replaceTags(fileString);
    }

    private String replaceTags(String fileString) {
        String outputFolder = getConfig(MARKETPLACE_OUTPUT_DIRECTORY, RUN_TEST_ID);
        return fileString.replace("##OUTPUT_FOLDER##", outputFolder);
    }

    @Given("I generate new run-test-id")
    public void iGenerateNewRunTestId() {
        RUN_TEST_ID = Utils.generateRunTestId();
        log.info("Generated new job-test-id: {}", RUN_TEST_ID);
    }

    @Given("I upload the input directory: {} to the marketplace container workspace")
    public void iUploadTheInputDirectoryInputScenarioToTheMarketplaceContainerWorkspace(String inputDirectory) {
        try {
            Path directoryToUpload = new ClassPathResource(inputDirectory).getFile().toPath();
            fileSystemClient.copyDirectoryContentToWorkspace(directoryToUpload, String.format("/marketplace/%s/input", RUN_TEST_ID));

        } catch (RuntimeException | IOException e) {
            log.error("Failed to upload input directory {} to the marketplace container workspace", inputDirectory, e);
            Assert.fail("Failed to upload input directory to the marketplace container workspace");
        }
    }

    @Given("I call Marketplace on path {} to analyze studies")
    public void iSendAnalyzeWithStudies(String endPointPath, DataTable dataTable) {
        HttpHeaders headers = new HttpHeaders();
        AnalyzeRequest analyzeRequest = AnalyzeRequest.builder().studies(dataTable.asList()).build();
        HttpEntity<AnalyzeRequest> requestEntity = new HttpEntity<>(analyzeRequest, headers);
        log.info("calling analyze 1 study on path {}",buildMarketPlaceUrl(endPointPath));
        ResponseEntity<Void> response = restTemplate.exchange(
                buildMarketPlaceUrl(endPointPath),
                HttpMethod.POST, requestEntity, Void.class);
        assertNotNull(response.getStatusCode());
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Given("I call Marketplace on path {} to analyze study id :{}")
    public void iSendAnalyzeWithStudy(String endPointPath, String study) {
        iSendAnalyzeWithStudies(endPointPath, DataTable.create(List.of(Collections.singletonList(study))));
    }

    @Given("I call Marketplace on path {} to analyze bad study id :{}")
    public void iSendAnalyzeWithBadStudy(String endPointPath, String study) {
        AnalyzeRequest analyzeRequest = AnalyzeRequest.builder().studies(Collections.singletonList(study)).build();
        HttpEntity<AnalyzeRequest> requestEntity = new HttpEntity<>(analyzeRequest, null);
        Assertions.assertThrows(RestClientException.class, () -> {
            ResponseEntity<Void> response = restTemplate.exchange(
                    buildMarketPlaceUrl(endPointPath),
                    HttpMethod.POST, requestEntity, Void.class);
        });
    }

    @Given("I call Marketplace on path {} to analyze bad structure for study id :{}")
    public void iSendAnalyzeWithBadStructure(String endPointPath, String study) {
        AnalyzeRequest analyzeRequest = AnalyzeRequest.builder().studies(Collections.singletonList(study)).build();
        HttpEntity<AnalyzeRequest> requestEntity = new HttpEntity<>(analyzeRequest, null);
        log.info(requestEntity.getBody().toString());
        Assertions.assertThrows(HttpClientErrorException.class, () -> {
            ResponseEntity<Void> response = restTemplate.exchange(
                    buildMarketPlaceUrl(endPointPath),
                    HttpMethod.POST, requestEntity, Void.class);
        });
    }

    @Then("I download the report {} from the remote output path, check that second capture dicom file was created and compare it to:{}")
    public void iDownloadTheFie(String actualReportFileName, String expectedReportFile) throws JsonProcessingException {
        String remoteOutputPath = getConfig(MARKETPLACE_OUTPUT_DIRECTORY, RUN_TEST_ID);
        log.info("Remote Output Path: {}", remoteOutputPath);
        String remoterReportText = fileSystemClient.downloadFile(remoteOutputPath, actualReportFileName);
        String editedRemoterReportText = removeBmdFromActualReport(remoterReportText);
        compareExpectedReportToActualReport(editedRemoterReportText, expectedReportFile);
        checkIfFilesCreatedOnOutputDirectory(extractCreatedFilesPathsFromReport(editedRemoterReportText));
    }

    private String removeBmdFromActualReport(String report) {
        if(!report.contains("bmd")){
            log.warn("Report does not contains bmd, skipping");
            return report;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(report);

            JsonNode dataNode = rootNode.path("data");
            if (dataNode.isArray()) {
                for (JsonNode node : dataNode) {
                    JsonNode metricsNode = node.path("metrics");
                    if (metricsNode instanceof ObjectNode) {
                        ((ObjectNode) metricsNode).remove("bmd");
                    }
                }
            }

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (Exception e) {
            log.error("Cannot remove bmd from structure: {}", e.getMessage());
            return null;
        }
    }

    @And("I download the SC dicom files from remote output path")
    public void iDownloadTheSCDicomFilesFromRemoteOutputPath() {
        log.info("Downloading remote output folder...");
        fileSystemClient.downloadOutputFolder(RUN_TEST_ID);
    }

    @And("I convert all the SC dicoms to JPEG")
    public void iConvertAllTheSCDicomsToJPEG() {
        log.info("Converting dicom to jpeg files");
        fileSystemClient.iterateThroughSCdicomsAndConvertToJPEG(RUN_TEST_ID);
    }

    @Then("I compare the SC dicoms to the expected SC dicoms for category {} and algorithm {}")
    public void iCompareTheSCDicomsToTheExpectedSCDicomsAndAlgorithm(String category, String algorithm) {
        Assert.assertTrue(fileSystemClient.iterateThroughJPEGfilesAndCompareToExpected(RUN_TEST_ID, category, algorithm));
    }

    @And("I verify wiremock was called on path {} with payload {}")
    public void verifyMock(String endPointPath, String blackPortReportFileName) throws IOException {
        List<LoggedRequest> requests = wiremock.find(RequestPatternBuilder
                .newRequestPattern(RequestMethod.POST, urlEqualTo("/" + RUN_TEST_ID + endPointPath)));
        MarketplaceReportOutput expectedBody = mapper.readValue(getOutputFileResource(blackPortReportFileName).getFile(), MarketplaceReportOutput.class);
        MarketplaceReportOutput actualBody = mapper.readValue(requests.get(0).getBodyAsString(), MarketplaceReportOutput.class);
        Assertions.assertEquals(expectedBody.getVersion(), actualBody.getVersion());
        Assertions.assertEquals(expectedBody.getType(), actualBody.getType());
        Assertions.assertEquals(expectedBody.getData().get(0).getStudyInstanceUid(), actualBody.getData().get(0).getStudyInstanceUid());
        Assertions.assertEquals(expectedBody.getData().size(), actualBody.getData().size());
    }

    @And("I verify wiremock was called on path {} with error payload {}")
    public void verifyErrorMock(String endPointPath, String blackPortReportFileName) throws IOException {
        List<LoggedRequest> requests = wiremock.find(RequestPatternBuilder
                .newRequestPattern(RequestMethod.POST, urlEqualTo("/" + RUN_TEST_ID + endPointPath)));
        MarketplaceErrorObject expectedBody = mapper.readValue(getOutputFileResource(blackPortReportFileName).getFile(), MarketplaceErrorObject.class);
        MarketplaceErrorObject actualBody = mapper.readValue(requests.get(0).getBodyAsString(), MarketplaceErrorObject.class);
        Assertions.assertEquals(expectedBody.getMessage(), actualBody.getMessage());
    }

    private ClassPathResource getOutputFileResource(String filePath) {
        return new ClassPathResource("inputs/configs/" + filePath);
    }

    private String getConfig(String configName, String jobId) {
        return appConfiguration.getContainerDirectoriesConfig().stream()
                .filter(config -> config.getName().equals(configName))
                .findFirst().map(config -> String.format(config.getPath(), jobId))
                .orElseThrow(() -> new RuntimeException(String.format("Failed to find %s in the config file", configName)));
    }

    public List<String> extractCreatedFilesPathsFromReport(String marketplaceReportText) throws JsonProcessingException {
        List<String> reportCreatedFiles = new ArrayList<>();
        MarketplaceReportOutput marketplaceReportOutput = mapper.readValue(marketplaceReportText, MarketplaceReportOutput.class);
        marketplaceReportOutput.getData().forEach(data -> Optional.ofNullable(data.getFiles()).ifPresent(files -> files.forEach(file -> {
                    reportCreatedFiles.add(file.getPath());
                    file.setPath("");
                }
        )));
        return reportCreatedFiles;
    }

    @SneakyThrows
    public void compareExpectedReportToActualReport(String actualReportText, String expectedReportFile) {
        MarketplaceReportOutput expectedMarketplaceReport = mapper.readValue(getFileContent(expectedReportFile), MarketplaceReportOutput.class);
        MarketplaceReportOutput actualMarketplaceReport = mapper.readValue(actualReportText, MarketplaceReportOutput.class);

        setReportPathsToEmptyString(actualMarketplaceReport);
        setReportPathsToEmptyString(expectedMarketplaceReport);
        JSONAssert.assertEquals(
                new Gson().toJson(expectedMarketplaceReport),
                new Gson().toJson(actualMarketplaceReport),
                new CustomComparator(JSONCompareMode.STRICT));
    }

    private void setReportPathsToEmptyString(MarketplaceReportOutput marketplaceReportOutput) {
        marketplaceReportOutput.getData().forEach(data -> Optional.ofNullable(data.getFiles()).ifPresent(files -> files.forEach(file -> file.setPath("")
        )));
    }

    private void checkIfFilesCreatedOnOutputDirectory(List<String> filesPathsList) {
        String outputPath = getConfig(MARKETPLACE_OUTPUT_DIRECTORY,RUN_TEST_ID);
        List<String> actualOutputFiles = fileSystemClient.listFiles(outputPath).stream().filter(file -> !file.contains(MARKETPLACE_REPORT_FILE_NAME)).toList();
        List<String> expectedOutputFilesNames = new ArrayList<>(filesPathsList.stream().map(file -> file.replace(outputPath + "/", "")).toList());
        Assert.assertTrue(new HashSet<>(actualOutputFiles).containsAll(expectedOutputFilesNames));
    }

    @Given("I list the files from the path: {}")
    public void iListTheFilesFromThePath(String path) {
        fileSystemClient.listFiles(path);
    }

    @Given("I call Marketplace on path {} to check healthiness")
    public void iCallMarketplaceOnPathHealth(String path) throws InterruptedException {
        try{
            ResponseEntity<Void> response = restTemplate.exchange(
                    buildMarketPlaceUrl(path),
                    HttpMethod.GET, null, Void.class);
            assertNotNull(response.getStatusCode());
            assertEquals(HttpStatus.OK, response.getStatusCode());
        } catch (HttpServerErrorException e) {
            log.warn("Marketplace health check failed: {}, retrying in 1 minute", e.getMessage());
            Thread.sleep(60000);
            ResponseEntity<Void> response = restTemplate.exchange(
                    buildMarketPlaceUrl(path),
                    HttpMethod.GET, null, Void.class);
            assertNotNull(response.getStatusCode());
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Given("I configure wiremock with endpoint {} with status_code {}")
    public void iConfigureWiremockWithEndpointResult(String path, int statusCode) {
        String dynamicPath = "/" + RUN_TEST_ID + path;
        log.info("Setting path: {} on mock server", dynamicPath);
        StubMapping stubMapping = wiremock.register(post(urlEqualTo(dynamicPath))
                .willReturn(WireMock.aResponse().withStatus(statusCode)));
        Awaitility.await().atMost(Duration.of(30, SECONDS)).until(() -> {
            return Objects.nonNull(wiremock.getStubMapping(stubMapping.getUuid()));
        });
    }

    @And("I wait till {} called")
    public void waitTillResultCalled(String endPointPath) {
        String dynamicEndpointPath = "/" + RUN_TEST_ID + endPointPath;
        log.info("Waiting for {}", dynamicEndpointPath);
        Awaitility.await()
                .atMost(Duration.of(25, MINUTES))
                .until(() -> {
                    List<LoggedRequest> requests = wiremock.find(RequestPatternBuilder
                            .newRequestPattern(RequestMethod.POST, urlEqualTo(dynamicEndpointPath)));
                    return !requests.isEmpty();
                });
    }

    @Then("I Validate NON-compliant {} for study id :{} and error code:{}")
    public void iValidateNONCompliantResultForStudyIdAndErrorCode(String endPointPath, String studyId, String errorCode) {
        List<LoggedRequest> requests = wiremock.find(RequestPatternBuilder
                .newRequestPattern(RequestMethod.POST, urlEqualTo("/" + RUN_TEST_ID + endPointPath)));
        String bodyAsString = requests.get(0).getBodyAsString();
        log.info("Non complaint response is {}", bodyAsString);
        Assertions.assertTrue(bodyAsString.contains("Non compliant study"));
        Assertions.assertTrue(bodyAsString.contains(errorCode));
        Assertions.assertTrue(bodyAsString.contains(studyId));
    }

    @Then("Validate dicom file {}")
    public void validateDicomFileFromReference(String dicom) {
        String remoteOutputPath = getConfig(MARKETPLACE_OUTPUT_DIRECTORY, RUN_TEST_ID);
        Assertions.assertNotNull(fileSystemClient.downloadFile(remoteOutputPath, dicom));
    }

    @Then("I delete test files")
    public void tearDown() {
        log.info("Tear Down started");
        String remoteOutputPath = getConfig(MARKETPLACE_OUTPUT_DIRECTORY, RUN_TEST_ID);
        String remoteInputPath = getConfig(MARKETPLACE_INPUT_DIRECTORY, RUN_TEST_ID);
        fileSystemClient.deleteDirectoryContent(remoteOutputPath);
        fileSystemClient.deleteDirectoryContent(remoteInputPath);
    }

    @Given("I call Marketplace on path {} for all studies in input folder")
    public void iCallMarketplaceOnPathVAnalyzeAll(String path) {
        ResponseEntity<Void> response = restTemplate.exchange(
                buildMarketPlaceUrl(path),
                HttpMethod.POST, null, Void.class);
        assertNotNull(response.getStatusCode());
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @After
    public void theCleanupConditionIsMet(Scenario scenario) {
        log.info("Conditional teardown");
        if (!scenario.isFailed()) {
            try{
                tearDown();
            }
            catch (ResourceAccessException e){
                log.error("Failed to tear down", e);
            }
        }
    }

    @Given("I call Marketplace on path {} to analyze study:{} and expect bad request")
    public void iSendAnalyzeWithStudiesAndExpectBadRequest(String endPointPath, String study) {
        HttpHeaders headers = new HttpHeaders();
        AnalyzeRequest analyzeRequest = AnalyzeRequest.builder().studies(DataTable.create(List.of(Collections.singletonList(study))).asList()).build();
        HttpEntity<AnalyzeRequest> requestEntity = new HttpEntity<>(analyzeRequest, headers);
        try{
            ResponseEntity<Void> response = restTemplate.exchange(
                    buildMarketPlaceUrl(endPointPath),
                    HttpMethod.POST, requestEntity, Void.class);
        }
        catch (HttpClientErrorException e){
            Assert.assertTrue(e.getMessage().contains("[no body]"));
        }
    }
}