package ru.spbau.mit.client;

import ru.spbau.mit.client.request.ClientRequest;
import ru.spbau.mit.client.response.ClientResponseWriter;
import ru.spbau.mit.client.request.GetRequest;
import ru.spbau.mit.client.request.ClientRequestReader;
import ru.spbau.mit.client.request.StatRequest;
import ru.spbau.mit.client.response.ClientResponse;
import ru.spbau.mit.client.response.GetWritableResponse;
import ru.spbau.mit.client.response.StatResponse;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class SeedHandler implements Runnable {
    private Socket socket;
    private Client.SharedComponents sharedComponents;
    private ClientRequestReader requestReader;
    private ClientResponseWriter responseWriter;

    SeedHandler(Socket socket, Client.SharedComponents sharedComponents) {
        this.socket = socket;
        this.sharedComponents = sharedComponents;
    }

    @Override
    public void run() {
        try {
            requestReader = new ClientRequestReader(socket);
            responseWriter = new ClientResponseWriter(socket);
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
            return;
        }

        String hostAddr = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();

        sharedComponents.getLog().println(String.format("New client %s have connected", hostAddr));

        for (int requestId = 0;; requestId++) {
            ClientRequest request = null;
            try {
                request = requestReader.nextRequest();
            } catch (EOFException e2) {
                sharedComponents.getLog().println(String.format("Client %s have disconnected", hostAddr));
                break;
            } catch (IOException e) {
                e.printStackTrace(sharedComponents.getLog());
                break;
            } catch (ClientRequestReader.ParseRequestException e) {
                e.printStackTrace(sharedComponents.getLog());
                continue;
            }

            sharedComponents.getLog().println(
                    String.format("Client %s send %d request %s", hostAddr, requestId, request.toString()));

            ClientResponse response;
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

    private ClientResponse dispatchResponse(ClientRequest request) throws DispatchException {
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
