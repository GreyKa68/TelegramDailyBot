package com.example.telegramdailybot.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Properties;

public class ExternalPropertiesInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        Path appPath = getJarLocation();
        String propertiesPath = "file:" + appPath + "/application.properties";

        try {
            URL propertiesUrl = new URL(propertiesPath);
            Properties externalProperties = new Properties();
            externalProperties.load(propertiesUrl.openStream());

            PropertiesPropertySource propertySource = new PropertiesPropertySource(
                    "externalProperties", externalProperties);
            environment.getPropertySources().addFirst(propertySource);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load external application.properties file", e);
        }
    }

    private Path getJarLocation() {
        try {
            URL jarUrl = ExternalPropertiesInitializer.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            Path jarPath;

            if (jarUrl.getProtocol().equalsIgnoreCase("jar")) {
                String jarUrlPath = jarUrl.getPath();
                int bangIndex = jarUrlPath.indexOf("!");
                String jarFileUrlPath = jarUrlPath.substring(0, bangIndex);
                jarPath = Paths.get(new URL(jarFileUrlPath).toURI());
            } else {
                jarPath = Paths.get(jarUrl.toURI());
            }

            Path normalizedJarPath = jarPath.toAbsolutePath().normalize();

            if (normalizedJarPath.startsWith(System.getProperty("user.home"))) {
                return normalizedJarPath.getParent();
            } else {
                String jarPathStr = jarPath.toString();
                int tildeIndex = jarPathStr.indexOf('~');
                if (tildeIndex != -1) {
                    String homeDir = System.getProperty("user.home");
                    String relativePath = jarPathStr.substring(tildeIndex + 1);
                    return Paths.get(homeDir + relativePath).getParent();
                } else {
                    return normalizedJarPath.getParent();
                }
            }
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException("Unable to find the JAR location", e);
        }
    }

}


