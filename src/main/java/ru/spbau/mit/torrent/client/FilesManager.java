package ru.spbau.mit.torrent.client;

import org.apache.commons.lang3.StringUtils;
import ru.spbau.mit.torrent.etc.ConfigProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FilesManager {
    private Map<Integer, Map<Integer, DistributedFilesEntry>> distributedFiles = new HashMap<>();
    private Map<Integer, ReadyToDownloadFilesEntry> readyToDownloadFiles = new HashMap<>();
    private Long partSize;
    private Path propsPath;

    FilesManager(Path propsPath, Long partSize) throws IOException {
        this.partSize = partSize;
        this.propsPath = propsPath;
        try {
            Properties props = ConfigProcessor.parseConfig(propsPath);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String[] parsedKeyParts = ((String) entry.getKey()).split(",");
                String[] parsedValueParts = ((String) entry.getValue()).split(",");
                int id = Integer.valueOf(parsedKeyParts[0]);
                int partId = Integer.valueOf(parsedKeyParts[1]);

                int type = Integer.valueOf(parsedValueParts[0]);
                Path currentFilePath = Paths.get(parsedValueParts[1]);
                long size = Long.valueOf(parsedValueParts[2]);
                if (type == 0) {
                    long offset = Integer.valueOf(parsedValueParts[3]);
                    insertDistributedFileEntry(id, partId, new DistributedFilesEntry(currentFilePath, offset,
                            size));
                } else {
                    insertNewReadyToDownloadFile(id, new ReadyToDownloadFilesEntry(currentFilePath, size));
                }
            }
        } catch (IOException e) {
            distributedFiles = new HashMap<>();
            readyToDownloadFiles = new HashMap<>();
            throw e;
        }
    }

    private void insertDistributedFileEntry(Integer id, Integer partId, DistributedFilesEntry file) {
        if (!distributedFiles.containsKey(id)) {
            distributedFiles.put(id, new HashMap<>());
        }
        distributedFiles.get(id).put(partId, file);
    }

    public int getFilePartsNum(ReadyToDownloadFilesEntry entry) {
        int result = (int) (entry.getSize() / partSize);
        if (entry.getSize() % partSize > 0) {
            result++;
        }
        return result;
    }

    public Long getPartSize(ReadyToDownloadFilesEntry entry, int partId) {
        return Math.min(entry.getSize() - partId * partSize, partSize);
    }

    public synchronized  void saveToDrive() throws IOException {
        Properties properties = new Properties();
        distributedFiles.entrySet().forEach(entry -> {
            int fileId = entry.getKey();
            entry.getValue().entrySet().forEach(innerEntry -> {
                int partId = innerEntry.getKey();
                DistributedFilesEntry distributedFilesEntry = innerEntry.getValue();
                String key = StringUtils.join(new Object[]{fileId, partId}, ",");
                String value = StringUtils.join(new Object[]{0, distributedFilesEntry.getPath(),
                        distributedFilesEntry.getSize(), distributedFilesEntry.getOffset()},  ",");
                properties.setProperty(key, value);
            });
        });
        readyToDownloadFiles.entrySet().forEach(entry -> {
            int fileId = entry.getKey();
            ReadyToDownloadFilesEntry readyToDownloadFilesEntry = entry.getValue();
            String key = StringUtils.join(new Object[]{fileId, 0}, ",");
            String value = StringUtils.join(new Object[]{1, readyToDownloadFilesEntry.getDownloadPath(),
                    readyToDownloadFilesEntry.getSize()}, ",");
            properties.setProperty(key, value);
        });

        ConfigProcessor.storeConfig(propsPath, properties);
    }

    public synchronized void insertNewDistributedFile(Integer id, Path filePath) throws IOException {
        Long fileSize = Files.size(filePath);
        for (long offset = 0; offset < fileSize; offset += partSize) {
            insertDistributedFileEntry(id, (int) (offset / partSize), new
                    DistributedFilesEntry(filePath, offset, Math.min(partSize, fileSize - offset)));
        }
    }

    public synchronized void insertNewDistributedPart(Integer id, Integer partId, Path filePath)
            throws IOException {
        insertDistributedFileEntry(id, partId, new DistributedFilesEntry(filePath, 0L, Files.size(filePath)));
    }

    public synchronized List<Integer> getAvailableParts(Integer fileId) {
        if (!distributedFiles.containsKey(fileId)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(distributedFiles.get(fileId).keySet());
    }

    public synchronized List<Integer> getAvailableFileIds() {
        return new ArrayList<>(distributedFiles.keySet());
    }

    public synchronized DistributedFilesEntry getDistributedFileEntry(Integer fileId, Integer partId) {
        return distributedFiles.get(fileId).get(partId);
    }

    public synchronized void insertNewReadyToDownloadFile(Integer id, ReadyToDownloadFilesEntry entry) {
        readyToDownloadFiles.put(id, entry);
    }

    public synchronized Map<Integer, ReadyToDownloadFilesEntry> getReadyToDownloadFiles() {
        return new HashMap<>(readyToDownloadFiles);
    }

    public synchronized void cleanReadyToDownloadFiles() {
        readyToDownloadFiles.clear();
    }

    public synchronized void removeDistributedFileEntry(int fileId, int partId) {
        if (distributedFiles.containsKey(fileId) && distributedFiles.get(fileId).containsKey(partId)) {
            distributedFiles.get(fileId).remove(partId);
        }
    }

    public synchronized void removeDistributedFile(int fileId) {
        if (distributedFiles.containsKey(fileId)) {
            distributedFiles.get(fileId).clear();
        }
    }

    public static class DistributedFilesEntry {
        private Path path;
        private Long offset = 0L;
        private Long size = 0L;

        public DistributedFilesEntry(Path path, Long offset, Long size) {
            this.path = path;
            this.offset = offset;
            this.size = size;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public Path getPath() {
            return path;
        }

        public void setPath(Path path) {
            this.path = path;
        }

        public Long getOffset() {
            return offset;
        }

        public void setOffset(Long offset) {
            this.offset = offset;
        }
    }

    public static class ReadyToDownloadFilesEntry {
        private Path downloadPath;
        private Long size;

        public ReadyToDownloadFilesEntry(Path downloadPath, Long size) {
            this.downloadPath = downloadPath;
            this.size = size;
        }

        public Path getDownloadPath() {
            return downloadPath;
        }

        public void setDownloadPath(Path downloadPath) {
            this.downloadPath = downloadPath;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }
    }
}

