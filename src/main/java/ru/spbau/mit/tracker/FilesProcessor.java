package ru.spbau.mit.tracker;

import ru.spbau.mit.common.ConfigProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class FilesProcessor {
    private Path filesInfoPath;
    private List<FileInfo> currentUpdatedFileInfos;

    public FilesProcessor(Path filesInfoPath) throws IOException {
        this.filesInfoPath = filesInfoPath;
        currentUpdatedFileInfos = new ArrayList<>();
        if (Files.exists(filesInfoPath)) {
            Properties props = ConfigProcessor.parseConfig(filesInfoPath);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String[] parsedParts = ((String) entry.getKey()).split("\\.");
                FileInfo fileInfo = new FileInfo();
                fileInfo.setName(parsedParts[0]);
                fileInfo.setId(Integer.valueOf(parsedParts[1]));
                fileInfo.setSize(Long.valueOf((String) entry.getValue()));
                currentUpdatedFileInfos.add(fileInfo);
            }
        }
    }

    public synchronized List<FileInfo> getFileInfos() throws IOException {
        return new ArrayList<>(currentUpdatedFileInfos);
    }

    public synchronized FileInfo uploadProcess(String name, long size) throws IOException {
        FileInfo newFileInfo = new FileInfo();
        newFileInfo.setName(name);
        newFileInfo.setSize(size);
        newFileInfo.setId(currentUpdatedFileInfos.size());
        currentUpdatedFileInfos.add(newFileInfo);
        return newFileInfo;
    }

    public synchronized void saveToDrive() throws IOException {
        Properties properties = new Properties();
        for (FileInfo fileInfo : currentUpdatedFileInfos) {
            properties.setProperty(fileInfo.getName() + "." + String.valueOf(fileInfo.getId()),
                    String.valueOf(fileInfo.getSize()));
        }
        ConfigProcessor.storeConfig(filesInfoPath, properties);
    }

    public static class FileInfo {
        private int id;
        private String name;
        private long size;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
