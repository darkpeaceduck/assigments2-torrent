package ru.spbau.mit.tracker.request;

public abstract class TrackerRequest {
    public enum Type {
        LIST,
        UPLOAD,
        SOURCES,
        UPDATE
    }

    private Type type;

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
