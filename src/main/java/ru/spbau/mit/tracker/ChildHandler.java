package ru.spbau.mit.tracker;

import ru.spbau.mit.tracker.request.*;
import ru.spbau.mit.tracker.response.*;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ChildHandler implements Runnable {
    private Socket socket;
    private Tracker.SharedComponents sharedComponents;
    private TrackerRequestReader requestReader;
    private TrackerResponseWriter responseWriter;

    ChildHandler(Socket socket, Tracker.SharedComponents sharedComponents) {
        this.socket = socket;
        this.sharedComponents = sharedComponents;
    }

    @Override
    public void run() {
        try {
            requestReader = new TrackerRequestReader(socket);
            responseWriter = new TrackerResponseWriter(socket);
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
            return;
        }

        String hostAddr = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();

        sharedComponents.getLog().println(String.format("New client %s have connected", hostAddr));

        for (int requestId = 0;; requestId++) {
            TrackerRequest request = null;
            try {
                request = requestReader.nextRequest();
            } catch (EOFException e2) {
                sharedComponents.getLog().println(String.format("Client %s have disconnected", hostAddr));
                break;
            } catch (IOException e) {
                e.printStackTrace(sharedComponents.getLog());
                break;
            } catch (TrackerRequestReader.ParseRequestException e) {
                e.printStackTrace(sharedComponents.getLog());
                continue;
            }

            sharedComponents.getLog().println(
                    String.format("Client %s send %d request %s", hostAddr, requestId, request.toString()));

            TrackerResponse response;
            try {
                response = dispatchResponse(request);
            } catch (DispatchException e1) {
                e1.printStackTrace(sharedComponents.getLog());
                continue;
            }

            try {
                responseWriter.writeResponse(response);
            } catch (IOException e) {
                e.printStackTrace(sharedComponents.getLog());
            }
            sharedComponents.getLog().println(
                    String.format("ClientResponse to client %s to %d request have sended", hostAddr, requestId));
        }

        closeSocket();
    }

    private void closeSocket() {
        try {
            requestReader.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace(sharedComponents.getLog());
        }

        try {
            responseWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace(sharedComponents.getLog());
        }

        try {
            socket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace(sharedComponents.getLog());
        }
    }

    private class DispatchException extends Exception {
        private static final long serialVersionUID = 8964566548132270723L;
    }

    private TrackerResponse dispatchResponse(TrackerRequest request) throws DispatchException {
        switch (request.getType()) {
            case LIST:
                return processListRequest();
            case UPLOAD:
                return processUploadRequest((UploadRequest) request);
            case SOURCES:
                return processSourcesRequest((SourcesRequest) request);
            case UPDATE:
                return processUpdateRequest((UpdateRequest) request);
            default:
                throw new DispatchException();
        }
    }

    private ListResponse processListRequest() {
        ListResponse response = new ListResponse();
        try {
            response.setFileInfos(sharedComponents.getFilesProcessor().getFileInfos());
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
            response.setFileInfos(new ArrayList<>());
        }
        return response;
    }

    private UploadResponse processUploadRequest(UploadRequest request) {
        UploadResponse response = new UploadResponse();
        try {
            response.setId(sharedComponents.getFilesProcessor().uploadProcess(request.getName(),
                    request.getSize()).getId());
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
            response.setId(-1);
        }
        return response;
    }

    private void updateAllSeeds() {
        sharedComponents.getSeedsProcessor().updateOnTick(System.currentTimeMillis());
    }

    private SourcesResponse processSourcesRequest(SourcesRequest request) {
        SourcesResponse response = new SourcesResponse();
        updateAllSeeds();
        response.setSocketAddresses(sharedComponents.getSeedsProcessor().getSeedsFromFileId(request.getId())
                .stream().map(SeedsProcessor.Seed::getAddress).collect(Collectors.toList()));
        return response;
    }

    private UpdateResponse processUpdateRequest(UpdateRequest request) {
        UpdateResponse response = new UpdateResponse();
        updateAllSeeds();
        sharedComponents.getSeedsProcessor().updateSeed(new InetSocketAddress(socket.getInetAddress(), request
                        .getSeedPort()),
                request
                        .getIds(), System
                        .currentTimeMillis());
        response.setStatus(true);
        return response;
    }

}
