package com.nanoxai.marketplace.tests.container.remote.directories;

import com.aspose.imaging.Image;
import com.aspose.imaging.fileformats.dicom.DicomImage;
import com.aspose.imaging.imageoptions.JpegOptions;
import com.nanoxai.marketplace.tests.config.RemoteDirectoriesHandlerConfig;
import com.nanoxai.marketplace.tests.container.k8s.KubernetesPodService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class FileSystemClient {
    private final RestTemplate restTemplate;
    private RemoteDirectoriesHandlerConfig remoteDirectoriesHandlerConfig;

    @Autowired
    KubernetesPodService kubernetesPodService;

    public void uploadFileToContainerWorkspace(Path localFilePath, String remoteDestinationFilePath) {
        Resource resource = new FileSystemResource(localFilePath);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("upload_dir", remoteDestinationFilePath);
        body.add("file", resource);
        restTemplate.postForEntity(remoteDirectoriesHandlerConfig.getUploadFilesServiceUrl(), new HttpEntity<>(body), String.class);
    }

    @SneakyThrows
    public List<String> listFiles(String remoteDirectoryPath) {
        URI uri = UriComponentsBuilder.fromUriString(remoteDirectoriesHandlerConfig.getListFilesServiceUrl()).build(remoteDirectoryPath);
        log.info("Listing files from remote path: {}", uri);
        return restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>() {
        }).getBody();
    }

    public String downloadFile(String remoteFilePath, String remoteFileName) {
        URI uri = UriComponentsBuilder.fromUriString(remoteDirectoriesHandlerConfig.getDownloadFiledServiceUrl()).build(remoteFilePath, remoteFileName);
        return restTemplate.getForEntity(uri, String.class).getBody();
    }

    public void downloadOutputFolder(String runTestId) {
        kubernetesPodService.downloadOutputFolder(runTestId);
    }

    public void copyDirectoryContentToWorkspace(Path sourceDirectoryPath, String destinationDirectoryPath) throws IOException {
        String localRoot = sourceDirectoryPath.getParent().toString();
        log.info("Copying {} to remote path:{}", sourceDirectoryPath, destinationDirectoryPath);
        Files.walkFileTree(sourceDirectoryPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String remoteDestinationFilePath = file.getParent().toString().replace(localRoot, destinationDirectoryPath);
                log.debug("Copying file {} to remote path:{}", file, remoteDestinationFilePath);
                uploadFileToContainerWorkspace(file, remoteDestinationFilePath);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void deleteDirectoryContent(String remoteDirectoryPath) {
        log.info("Folder [{}] deleted", remoteDirectoryPath);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("delete_folder", remoteDirectoryPath);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(remoteDirectoriesHandlerConfig.getDeleteFilesServiceUrl(), formData, String.class);
        log.info("Response from delete directory content: {}", responseEntity.getBody());
    }

    public void iterateThroughSCdicomsAndConvertToJPEG(String runTestId) {
        File folder = new File(System.getProperty("user.dir") + "/src/main/resources/scOutputs/marketplace/" + runTestId + "/output");

        try{
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (!file.getName().equals("marketplace_data.json")) {
                    String newFileName = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".jpeg";
                    convertDicomToJpeg(file.getPath(), newFileName, runTestId);
                }
            }
        } catch (Exception e) {
            log.error("Error while converting file to JPEG. {}", e.getMessage());
            System.exit(1);
        }

    }

    public void convertDicomToJpeg(String source, String fileName, String runTestId) {
        Locale.setDefault(new Locale("en-us"));
        try{
            String resourcesPath = "src/main/resources/scOutputs/marketplace/" + runTestId + "/outputAsJpeg";
            File newFolder = new File(resourcesPath);

            if (!newFolder.exists()) {
                boolean created = newFolder.mkdirs();
                if (created) {
                    log.debug("Directory created successfully!");
                } else {
                    log.error("Failed to create directory.");
                }
            } else {
                log.debug("Directory already exists.");
            }

            DicomImage dicomImage = null;
            dicomImage = (DicomImage) Image.load(source);
            dicomImage.setActivePage(dicomImage.getDicomPages()[0]);
            JpegOptions jpegOptions = new JpegOptions();
            dicomImage.save(resourcesPath + "/" + fileName, jpegOptions);
        }
        catch (Exception e){
            log.error("Could not save {}, {}, {}", fileName, e.getMessage(), e.getCause());
        }
    }

    public boolean iterateThroughJPEGfilesAndCompareToExpected(String runTestId, String category, String algorithm) {
        boolean foundMatch = false;
        File actualJpegFolder = new File(System.getProperty("user.dir") + "/src/main/resources/scOutputs/marketplace/" + runTestId + "/outputAsJpeg");
        File expectedJpegFolder = new File(System.getProperty("user.dir") + "/src/main/resources/scOutputs/" + algorithm + "/Expected/" + category);
        File[] actualJpegList = actualJpegFolder.listFiles();
        File[] expectedJpegList = expectedJpegFolder.listFiles();

        assert actualJpegList != null;
        assert expectedJpegList != null;
        if (actualJpegList.length > 0 && expectedJpegList.length > 0) {
            for (File exp : expectedJpegList) {
                log.info("Finding match for {}", exp.getName());
                for (File act : actualJpegList) {
                    if (act.isFile() && exp.isFile() && !act.getName().equals(".DS_Store") && !act.getName().equals(".gitkeep")) {  //filter out folders
                        try{
                            if (compareExpectedAndActualSCdicoms(act.getPath(), exp.getPath())) {
                                foundMatch = true;
                                log.info("Match found");
                                break;
                            }
                        }
                        catch(Exception e){
                            log.error("Comparing actual: {} and expected: {}", act.getPath(), exp.getPath());
                            log.error(e.getMessage(), e.getCause());
                            foundMatch = false;
                        }
                    }
                    else{
                        log.debug("Cannot compare, invalid file");
                        foundMatch = false;
                    }
                }
                if (!foundMatch){
                    log.error("Could not find match for: {}", exp.getName());
                    return false;
                }
            }
        } else {
            log.error("No files found in folder");
            return false;
        }

        log.info("Found match for all dicoms");
        return true;
    }

    public boolean compareExpectedAndActualSCdicoms(String actualImage, String expectedImage) {
        BufferedImage img1 = null;
        BufferedImage img2 = null;

        try {
            img1 = ImageIO.read(new File(actualImage));
            img2 = ImageIO.read(new File(expectedImage));
        } catch (Exception e) {
            log.error("Can't open images");
        }

        int w1 = img1.getWidth();
        int w2 = img2.getWidth();
        int h1 = img1.getHeight();
        int h2 = img2.getHeight();

        if ((w1 != w2) || (h1 != h2)) {
            log.error("Both images should have same dimensions");
            return false;
        } else {
            long diff = 0;
            for (int j = 0; j < h1; j++) {
                for (int i = 0; i < w1; i++) {
                    //Getting the RGB values of a pixel
                    int pixel1 = img1.getRGB(i, j);
                    Color color1 = new Color(pixel1, true);
                    int r1 = color1.getRed();
                    int g1 = color1.getGreen();
                    int b1 = color1.getBlue();
                    int pixel2 = img2.getRGB(i, j);
                    Color color2 = new Color(pixel2, true);
                    int r2 = color2.getRed();
                    int g2 = color2.getGreen();
                    int b2 = color2.getBlue();
                    //sum of differences of RGB values of the two images
                    long data = Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
                    diff = diff + data;
                }
            }
            double avg = diff / (w1 * h1 * 3);
            double percentage = (avg / 255) * 100;
            log.debug("for {} difference is: {}", actualImage, percentage);
            return percentage == 0.0;
        }
    }
}