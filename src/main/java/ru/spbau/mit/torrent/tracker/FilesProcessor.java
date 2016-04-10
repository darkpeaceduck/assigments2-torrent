package ru.spbau.mit.torrent.tracker;

import ru.spbau.mit.torrent.etc.ConfigProcessor;

import java.io.IOException;
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

    public synchronized List<FileInfo> getFileInfos() throws IOException {
        return new ArrayList<>(currentUpdatedFileInfos);
    }

    public synchronized FileInfo uploadProcess(String name, Long size) throws IOException {
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
        private Long size;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
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
