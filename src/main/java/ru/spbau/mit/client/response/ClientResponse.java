package ru.spbau.mit.client.response;

public abstract class ClientResponse {
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
