package ru.spbau.mit.torrent.client;

import ru.spbau.mit.torrent.client.request.RequestWriter;
import ru.spbau.mit.torrent.client.response.ResponseReader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SeedConnection {
    private Socket seedSocket;
    private RequestWriter requestWriter;
    private ResponseReader responseReader;

    public SeedConnection(InetSocketAddress socketAddress) throws IOException {
        seedSocket = new Socket(socketAddress.getAddress(), socketAddress.getPort());
        requestWriter = new RequestWriter(seedSocket);
        responseReader = new ResponseReader(seedSocket);
    }

    public void closeAll() throws IOException {
        requestWriter.close();
        responseReader.close();
        seedSocket.close();
    }

    public ResponseReader getResponseReader() {
        return responseReader;
    }

    public RequestWriter getRequestWriter() {
        return requestWriter;
    }
}
