package ru.spbau.mit.tracker.response;

import java.net.InetSocketAddress;
import java.util.List;

public class SourcesResponse extends TrackerResponse {
    private List<InetSocketAddress> socketAddresses;

    public SourcesResponse() {
        super();
        setType(Type.SOURCES);
    }

    public List<InetSocketAddress> getSocketAddresses() {
        return socketAddresses;
    }

    public void setSocketAddresses(List<InetSocketAddress> socketAddresses) {
        this.socketAddresses = socketAddresses;
    }
}
