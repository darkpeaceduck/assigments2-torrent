package ru.spbau.mit.tracker.request;

import java.util.ArrayList;
import java.util.List;

public class UpdateRequest extends TrackerRequest {
    private short seedPort;
    private List<Integer> ids = new ArrayList<>();

    public UpdateRequest() {
        super();
        setType(Type.UPDATE);
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }

    public short getSeedPort() {
        return seedPort;
    }

    public void setSeedPort(short seedPort) {
        this.seedPort = seedPort;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void addId(int id) {
        ids.add(id);
    }
}
