package ru.spbau.mit.client;

import ru.spbau.mit.client.request.GetRequest;
import ru.spbau.mit.client.request.StatRequest;
import ru.spbau.mit.client.response.ClientResponseReader;
import ru.spbau.mit.client.response.StatResponse;
import ru.spbau.mit.tracker.request.SourcesRequest;
import ru.spbau.mit.tracker.response.SourcesResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.Set;

public final class ClientTasks {
    private ClientTasks() {
    }

    public static StatResponse collectFilePartInfos(Client.SharedComponents sharedComponents, InetSocketAddress
            seedAddress, int fileId) {
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

    public static SourcesResponse collectSeedInfos(Client.SharedComponents sharedComponents, InetSocketAddress
            trackerAddress, int fileId) {
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

    public static void downloadPart(Path downloadPartPath, long offset, long partSize, GetRequest request,
                                    InetSocketAddress seed, Client.SharedComponents sharedComponents) {
        SeedConnection connection = null;
        try {
            connection = new SeedConnection(seed);
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
            return;
        }

        sharedComponents.getLog().println(String.format("Ready to download %s ( part id %d) from %s:%d",
                downloadPartPath
                .toString(), request.getPart(), seed.getAddress().getHostAddress(), seed.getPort()));

        try {
            connection.getRequestWriter().writeRequest(request);
            ClientResponseReader.fullDownloadReadableResponse(
                    connection.getResponseReader().nextGetReadableResponse(),
                    downloadPartPath,
                    offset,
                    partSize);
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
            return;
        }

        sharedComponents.getLog().println(String.format("Finished download %s ( part id %d) from %s:%d",
                downloadPartPath
                .toString(), request.getPart(), seed.getAddress().getHostAddress(), seed.getPort()));
        try {
            connection.closeAll();
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
        }
    }

    public static void mergeParts(Client.SharedComponents components, Path fileDownloadPath, Set<Path>
            partsPath) {
        components.getLog().println(String.format("Ready to merge parts to %s", fileDownloadPath.toString()));
        try {
            if (partsPath.size() == 1) {
                Files.move(partsPath.iterator().next(), fileDownloadPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                OutputStream outputStream = Files.newOutputStream(fileDownloadPath);
                for (Path path : partsPath) {
                    components.getLog().println(String.format("Merging %s ", path.toString()));
                    Files.copy(path, outputStream);
                }
                outputStream.close();
            }
        } catch (IOException e) {
                e.printStackTrace(components.getLog());
                return;
        }
        components.getLog().println(String.format("Finished merging parts into %s", fileDownloadPath.toString()));
    }
}
