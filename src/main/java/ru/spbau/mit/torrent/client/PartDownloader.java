package ru.spbau.mit.torrent.client;

import ru.spbau.mit.torrent.client.request.GetRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

public class PartDownloader implements Runnable {
    private Path downloadPartPath;
    private InetSocketAddress seed;
    private Client.SharedComponents sharedComponents;
    private GetRequest request;
    private Long partSize;


    PartDownloader(Path downloadPartPath, Long partSize, GetRequest request, InetSocketAddress seed, Client
            .SharedComponents
            sharedComponents) {
        super();
        this.request = request;
        this.partSize = partSize;
        this.downloadPartPath = downloadPartPath;
        this.seed = seed;
        this.sharedComponents = sharedComponents;
    }

    @Override
    public void run() {
        SeedConnection connection = null;
        try {
            connection = new SeedConnection(seed);
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
            return;
        }

        sharedComponents.getLog().println(String.format("Ready to download %s from %s:%d", downloadPartPath
                .toString(), seed.getAddress().getHostAddress(), seed.getPort()));

        try {
            connection.getRequestWriter().writeRequest(request);
            connection.getResponseReader().fullDownloadReadableResponse(
                    connection.getResponseReader().nextGetReadableResponse(), downloadPartPath,
                    partSize);
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
            return;
        }

        sharedComponents.getLog().println(String.format("Finished download %s from %s:%d", downloadPartPath
                .toString(), seed.getAddress().getHostAddress(), seed.getPort()));
        try {
            connection.closeAll();
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
        }
    }
}
