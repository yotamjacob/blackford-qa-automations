package com.nanoxai.marketplace.tests.utils;

import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

public class Utils {

    public static <T> T loadYaml(File file) throws IOException {
        FileInputStream fileInputStream = FileUtils.openInputStream(file);
        return Serialization.unmarshal(fileInputStream, Collections.emptyMap());
    }

    public static String generateRunTestId() {
        return DateTimeFormatter.ofPattern("yyyyMMddHHmmssSS").format(LocalDateTime.now());
    }



}
