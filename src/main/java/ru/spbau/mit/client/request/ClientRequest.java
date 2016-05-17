package ru.spbau.mit.client.request;

public abstract class ClientRequest {
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
