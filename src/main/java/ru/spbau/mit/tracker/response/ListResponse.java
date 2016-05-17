package ru.spbau.mit.tracker.response;

import ru.spbau.mit.tracker.FilesProcessor;

import java.util.List;

public class ListResponse extends TrackerResponse {
    private List<FilesProcessor.FileInfo> fileInfos;

    public ListResponse() {
        super();
        setType(Type.LIST);
    }

    public List<FilesProcessor.FileInfo> getFileInfos() {
        return fileInfos;
    }

    public void setFileInfos(List<FilesProcessor.FileInfo> fileInfos) {
        this.fileInfos = fileInfos;
    }
}
