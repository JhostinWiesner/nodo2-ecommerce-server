package com.apexstore.processor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

final class ProcessorConfig {
    private final Properties properties = new Properties();

    private ProcessorConfig() {
        try (InputStream input = ProcessorConfig.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IllegalStateException("No se encontró config.properties en el classpath");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer config.properties", e);
        }
    }

    static ProcessorConfig load() {
        return new ProcessorConfig();
    }

    String value(String property, String environmentVariable) {
        String environmentValue = System.getenv(environmentVariable);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        String propertyValue = properties.getProperty(property);
        if (propertyValue == null || propertyValue.isBlank()) {
            throw new IllegalStateException(
                "Falta la configuración '" + property + "' "
                    + "(o la variable de entorno " + environmentVariable + ")"
            );
        }
        return propertyValue.trim();
    }
}
