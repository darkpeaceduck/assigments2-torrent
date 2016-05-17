package ru.spbau.mit.client;

import ru.spbau.mit.client.request.ClientRequestWriter;
import ru.spbau.mit.client.response.ClientResponseReader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SeedConnection {
    private Socket seedSocket;
    private ClientRequestWriter requestWriter;
    private ClientResponseReader responseReader;

    public SeedConnection(InetSocketAddress socketAddress) throws IOException {
        seedSocket = new Socket(socketAddress.getAddress(), socketAddress.getPort());
        requestWriter = new ClientRequestWriter(seedSocket);
        responseReader = new ClientResponseReader(seedSocket);
    }

    public void closeAll() throws IOException {
        requestWriter.close();
        responseReader.close();
        seedSocket.close();
    }

    public ClientResponseReader getResponseReader() {
        return responseReader;
    }

    public ClientRequestWriter getRequestWriter() {
        return requestWriter;
    }
}
