package ru.spbau.mit.torrent.client;

import ru.spbau.mit.torrent.client.request.StatRequest;
import ru.spbau.mit.torrent.client.response.StatResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;

public class FileInfoPartsCollector implements Callable<StatResponse> {
    private InetSocketAddress seedAddress;
    private Client.SharedComponents sharedComponents;
    private int fileId;

    public FileInfoPartsCollector(InetSocketAddress seed, Client.SharedComponents sharedComponents, int fileId) {
        this.seedAddress = seed;
        this.sharedComponents = sharedComponents;
        this.fileId = fileId;
    }

    @Override
    public StatResponse call() {
        SeedConnection connection;
        try {
            connection = new SeedConnection(seedAddress);
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
            return null;
        }

        sharedComponents.getLog().println(String.format("Ready to gather part info %d from %s:%d", fileId,
                seedAddress.getAddress().getHostAddress(), seedAddress.getPort()));

        StatResponse response = null;
        try {
            StatRequest request = new StatRequest();
            request.setId(fileId);
            connection.getRequestWriter().writeRequest(request);
            response = connection.getResponseReader().nextStatResponse();
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
            return null;
        }

        sharedComponents.getLog().println(String.format("Finished gathering part info %d from %s:%d", fileId,
                seedAddress.getAddress().getHostAddress(), seedAddress.getPort()));
        try {
            connection.closeAll();
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
        }
        return response;
    }
}
