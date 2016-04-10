package ru.spbau.mit.torrent.client.response;

import java.util.List;

public class StatResponse extends Response {
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
