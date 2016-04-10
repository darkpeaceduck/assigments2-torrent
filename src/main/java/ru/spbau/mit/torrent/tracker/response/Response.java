package ru.spbau.mit.torrent.tracker.response;

public abstract class Response {
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
