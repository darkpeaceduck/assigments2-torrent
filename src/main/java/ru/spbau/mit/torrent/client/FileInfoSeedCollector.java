package ru.spbau.mit.torrent.client;

import ru.spbau.mit.torrent.tracker.request.SourcesRequest;
import ru.spbau.mit.torrent.tracker.response.SourcesResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;

public class FileInfoSeedCollector implements Callable<SourcesResponse> {
    private Client.SharedComponents sharedComponents;
    private InetSocketAddress trackerAddress;
    private Integer fileId;

    public FileInfoSeedCollector(Client.SharedComponents sharedComponents, InetSocketAddress trackerAddress,
                                 Integer fileId) {
        this.sharedComponents = sharedComponents;
        this.trackerAddress = trackerAddress;
        this.fileId = fileId;
    }

    @Override
    public SourcesResponse call() {
        TrackerConnection connection = null;
        try {
            connection = new TrackerConnection(trackerAddress);
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
            return null;
        }

        sharedComponents.getLog().println(String.format("Ready to gather seed info about %d", fileId));
        SourcesResponse response = null;
        try {
            SourcesRequest sourcesRequest = new SourcesRequest();
            sourcesRequest.setId(fileId);
            connection.getRequestWriter().writeRequest(sourcesRequest);
            response = connection.getResponseReader().nextSourcesResponse();
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
            return null;
        }

        sharedComponents.getLog().println(String.format("Finished gathering seeds info about %d", fileId));
        try {
            connection.closeAll();
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
        }
        return response;
    }
}
