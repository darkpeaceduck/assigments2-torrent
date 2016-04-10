package ru.spbau.mit.torrent.client.response;

public abstract class Response {
    public enum Type {
        GET_WR,
        GET_R,
        STAT
    }

    private Type type;

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
