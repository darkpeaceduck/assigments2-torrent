package ru.spbau.mit.torrent.tracker.request;

public class SourcesRequest extends Request {
    private int id;

    public SourcesRequest() {
        super();
        setType(Type.SOURCES);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
