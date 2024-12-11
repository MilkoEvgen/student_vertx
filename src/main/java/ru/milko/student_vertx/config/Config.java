package ru.milko.student_vertx.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private final Properties properties = new Properties();

    public Config(String configFilePath) {
        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            properties.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration file: " + configFilePath, e);
        }
    }

    public String get(String key) {
        return properties.getProperty(key);
    }
}
