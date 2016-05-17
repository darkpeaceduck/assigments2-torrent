package ru.spbau.mit.tracker.response;

public class UploadResponse extends TrackerResponse {
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
