package ru.spbau.mit.torrent.tracker.request;

public class UploadRequest extends Request {
    private String name;
    private Long size;

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

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
