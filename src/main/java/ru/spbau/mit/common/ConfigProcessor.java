package ru.spbau.mit.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public final class ConfigProcessor {
    private ConfigProcessor() {
    }

    public static Properties parseConfig(Path path) throws IOException {
        InputStream propStream = new FileInputStream(path.toString());
        Properties properties = new Properties();
        properties.load(propStream);
        return properties;
    }

    public static void storeConfig(Path path, Properties properties) throws IOException {
        OutputStream stream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption
                .WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        properties.store(stream, "");
        stream.close();
    }
}
