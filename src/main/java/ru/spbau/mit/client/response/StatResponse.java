package ru.spbau.mit.client.response;

import java.util.List;

public class StatResponse extends ClientResponse {
    private List<Integer> parts;

    public StatResponse() {
        super();
        setType(Type.STAT);
    }

    public List<Integer> getParts() {
        return parts;
    }

    public void setParts(List<Integer> parts) {
        this.parts = parts;
    }
}
