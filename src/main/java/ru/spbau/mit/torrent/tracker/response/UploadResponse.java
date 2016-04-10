package ru.spbau.mit.torrent.tracker.response;

public class UploadResponse extends Response {
    private int id;

    public UploadResponse() {
        super();
        setType(Type.UPLOAD);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
