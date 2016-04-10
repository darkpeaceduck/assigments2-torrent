package ru.spbau.mit.torrent.client.response;

import ru.spbau.mit.torrent.client.FilesManager;

public class GetWritableResponse extends Response{
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
