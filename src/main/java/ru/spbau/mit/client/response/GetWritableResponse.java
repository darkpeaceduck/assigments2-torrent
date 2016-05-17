package ru.spbau.mit.client.response;

import ru.spbau.mit.client.FilesManager;

public class GetWritableResponse extends ClientResponse {
    private FilesManager.DistributedFilesEntry entry;

    public GetWritableResponse() {
        super();
        setType(Type.GET_WR);
    }

    public void setEntry(FilesManager.DistributedFilesEntry entry) {
        this.entry = entry;
    }

    public FilesManager.DistributedFilesEntry getEntry() {
        return entry;
    }
}
