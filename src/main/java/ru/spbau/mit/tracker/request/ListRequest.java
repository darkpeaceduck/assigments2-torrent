package ru.spbau.mit.tracker.request;

public class ListRequest extends TrackerRequest {
    public ListRequest() {
        super();
        setType(Type.LIST);
    }
}
