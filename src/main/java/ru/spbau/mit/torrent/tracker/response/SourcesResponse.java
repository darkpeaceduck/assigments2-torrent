package ru.spbau.mit.torrent.tracker.response;

import java.net.InetSocketAddress;
import java.util.List;

public class SourcesResponse extends Response {
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
