package ru.spbau.mit.torrent.client.request;

public abstract class Request {
    public enum Type{
        STAT,
        GET
    }
    private Type type;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
