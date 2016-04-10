package ru.spbau.mit.torrent.tracker.request;

public class ListRequest extends Request {
    public ListRequest() {
        super();
        setType(Type.LIST);
    }
}
