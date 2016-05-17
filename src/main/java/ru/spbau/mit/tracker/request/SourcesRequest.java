package ru.spbau.mit.tracker.request;

public class SourcesRequest extends TrackerRequest {
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
