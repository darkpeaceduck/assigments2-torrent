package ru.spbau.mit.client;

import ru.spbau.mit.tracker.request.TrackerRequestWriter;
import ru.spbau.mit.tracker.response.TrackerResponseReader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TrackerConnection {
    private Socket trackerSocket;
    private TrackerRequestWriter requestWriter;
    private TrackerResponseReader responseReader;

    public TrackerConnection(InetSocketAddress socketAddress) throws IOException {
        trackerSocket = new Socket(socketAddress.getAddress(), socketAddress.getPort());
        requestWriter = new TrackerRequestWriter(trackerSocket);
        responseReader = new TrackerResponseReader(trackerSocket);
    }

    public void closeAll() throws IOException {
        requestWriter.close();
        responseReader.close();
        trackerSocket.close();
    }

    public TrackerRequestWriter getRequestWriter() {
        return requestWriter;
    }

    public TrackerResponseReader getResponseReader() {
        return responseReader;
    }
}
