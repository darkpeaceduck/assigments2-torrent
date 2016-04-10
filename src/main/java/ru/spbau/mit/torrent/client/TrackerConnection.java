package ru.spbau.mit.torrent.client;

import ru.spbau.mit.torrent.tracker.request.RequestWriter;
import ru.spbau.mit.torrent.tracker.response.ResponseReader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TrackerConnection {
    private Socket trackerSocket;
    private RequestWriter requestWriter;
    private ResponseReader responseReader;

    public TrackerConnection(InetSocketAddress socketAddress) throws IOException {
        trackerSocket = new Socket(socketAddress.getAddress(), socketAddress.getPort());
        requestWriter = new RequestWriter(trackerSocket);
        responseReader = new ResponseReader(trackerSocket);
    }

    public void closeAll() throws IOException {
        requestWriter.close();
        responseReader.close();
        trackerSocket.close();
    }

    public RequestWriter getRequestWriter() {
        return requestWriter;
    }

    public ResponseReader getResponseReader() {
        return responseReader;
    }
}
