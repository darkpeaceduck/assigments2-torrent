package ru.spbau.mit.torrent.client;

import ru.spbau.mit.torrent.client.request.GetRequest;
import ru.spbau.mit.torrent.client.request.Request;
import ru.spbau.mit.torrent.client.request.RequestReader;
import ru.spbau.mit.torrent.client.request.StatRequest;
import ru.spbau.mit.torrent.client.response.GetWritableResponse;
import ru.spbau.mit.torrent.client.response.Response;
import ru.spbau.mit.torrent.client.response.ResponseWriter;
import ru.spbau.mit.torrent.client.response.StatResponse;

import java.io.IOException;
import java.net.Socket;

public class SeedHandler implements Runnable {
    private Socket socket;
    private Client.SharedComponents sharedComponents;
    private RequestReader requestReader;
    private ResponseWriter responseWriter;

    SeedHandler(Socket socket, Client.SharedComponents sharedComponents) {
        this.socket = socket;
        this.sharedComponents = sharedComponents;
    }

    @Override
    public void run() {
        try {
            requestReader = new RequestReader(socket);
            responseWriter = new ResponseWriter(socket);
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
            return;
        }

        String hostAddr = socket.getInetAddress().getHostAddress();

        sharedComponents.getLog().println(String.format("New client %s have connected", hostAddr));

        for (int requestId = 0;; requestId++) {
            Request request = null;
            try {
                request = requestReader.nextRequest();
            } catch (IOException e) {
                e.printStackTrace(sharedComponents.getLog());
                break;
            } catch (RequestReader.ParseRequestException e) {
                e.printStackTrace(sharedComponents.getLog());
                continue;
            }

            sharedComponents.getLog().println(
                    String.format("Client %s send %d request %s", hostAddr, requestId, request.toString()));

            Response response;
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
                    String.format("Response to client %s to %d request have sended", hostAddr, requestId));
        }

        sharedComponents.getLog().println(String.format("Client %s have disconnected", hostAddr));
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

    private Response dispatchResponse(Request request) throws DispatchException {
        switch (request.getType()) {
            case STAT:
                return processStatRequest((StatRequest) request);
            case GET:
                return processGetRequest((GetRequest) request);
            default:
                throw new DispatchException();
        }
    }

    private StatResponse processStatRequest(StatRequest request) {
        StatResponse response = new StatResponse();
        response.setParts(sharedComponents.getFilesManager().getAvailableParts(request.getId()));
        return response;
    }

    private GetWritableResponse processGetRequest(GetRequest request) {
        GetWritableResponse response = new GetWritableResponse();
        response.setEntry(sharedComponents.getFilesManager().getDistributedFileEntry(request.getId(),
                request.getPart()));
        return response;
    }
}
