package ru.spbau.mit.tracker.request;

public class UploadRequest extends TrackerRequest {
    private String name;
    private long size;

    public UploadRequest() {
        super();
        setType(Type.UPLOAD);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
