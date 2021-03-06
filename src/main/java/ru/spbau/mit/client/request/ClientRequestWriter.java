package ru.spbau.mit.client.request;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientRequestWriter {
    private DataOutputStream dataStream;

    public ClientRequestWriter(Socket socket) throws IOException {
        dataStream = new DataOutputStream(socket.getOutputStream());
    }

    public void writeGetRequest(GetRequest request) throws IOException {
        dataStream.writeInt(request.getId());
        dataStream.writeInt(request.getPart());
    }

    public void writeStatRequest(StatRequest request) throws IOException {
        dataStream.writeInt(request.getId());
    }

    public void writeRequest(ClientRequest request) throws IOException {
        dataStream.writeByte(request.getType().ordinal() + 1);
        switch (request.getType()) {
            case GET:
                writeGetRequest((GetRequest) request);
                break;
            case STAT:
                writeStatRequest((StatRequest) request);
            default:
                break;
        }
        dataStream.flush();
    }

    public void close() throws IOException {
        dataStream.close();
    }
}
