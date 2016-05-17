package ru.spbau.mit.tracker.request;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TrackerRequestWriter {
    private DataOutputStream dataStream;

    public TrackerRequestWriter(Socket socket) throws IOException {
        dataStream = new DataOutputStream(socket.getOutputStream());
    }

    public void writeListRequest(ListRequest request) {
    }

    public void writeUploadRequest(UploadRequest request) throws IOException {
        dataStream.writeUTF(request.getName());
        dataStream.writeLong(request.getSize());
    }

    public void writeSourcesRequest(SourcesRequest request) throws IOException {
        dataStream.writeInt(request.getId());
    }

    public void writeUpdateRequest(UpdateRequest request) throws IOException {
        dataStream.writeShort(request.getSeedPort());
        dataStream.writeInt(request.getIds().size());
        for (int id : request.getIds()) {
            dataStream.writeInt(id);
        }
    }

    public void writeRequest(TrackerRequest request) throws IOException {
        dataStream.writeByte(request.getType().ordinal() + 1);
        switch (request.getType()) {
            case LIST:
                writeListRequest((ListRequest) request);
                break;
            case SOURCES:
                writeSourcesRequest((SourcesRequest) request);
                break;
            case UPDATE:
                writeUpdateRequest((UpdateRequest) request);
                break;
            case UPLOAD:
                writeUploadRequest((UploadRequest) request);
                break;
            default:
                break;
        }
        dataStream.flush();
    }

    public void close() throws IOException {
        dataStream.close();
    }
}
