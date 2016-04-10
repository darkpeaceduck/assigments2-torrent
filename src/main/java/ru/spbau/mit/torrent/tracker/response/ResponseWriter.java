package ru.spbau.mit.torrent.tracker.response;

import ru.spbau.mit.torrent.tracker.FilesProcessor;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ResponseWriter {
    private DataOutputStream dataStream;

    public ResponseWriter(Socket socket) throws IOException {
        dataStream = new DataOutputStream(socket.getOutputStream());
    }

    private void writeListResponse(ListResponse response) throws IOException {
        dataStream.writeInt(response.getFileInfos().size());
        for (FilesProcessor.FileInfo fileInfo : response.getFileInfos()) {
            dataStream.writeInt(fileInfo.getId());
            dataStream.writeUTF(fileInfo.getName());
            dataStream.writeLong(fileInfo.getSize());
        }
    }

    private void writeUploadResponse(UploadResponse response) throws IOException {
        dataStream.writeInt(response.getId());
    }

    private void writeSourcesResponse(SourcesResponse response) throws IOException {
        dataStream.writeInt(response.getSocketAddresses().size());
        for (InetSocketAddress socketAddress : response.getSocketAddresses()) {
            dataStream.write(socketAddress.getAddress().getAddress());
            dataStream.writeShort(socketAddress.getPort());
        }
    }

    private void writeUpdateResponse(UpdateResponse response) throws IOException {
        dataStream.writeBoolean(response.isStatus());
    }

    public void writeResponse(Response response) throws IOException {
        switch (response.getType()) {
            case LIST:
                writeListResponse((ListResponse) response);
                break;
            case UPLOAD:
                writeUploadResponse((UploadResponse) response);
                break;
            case SOURCES:
                writeSourcesResponse((SourcesResponse) response);
                break;
            case UPDATE:
                writeUpdateResponse((UpdateResponse) response);
                break;
            default:
        }
        dataStream.flush();
    }

    public void close() throws IOException {
        dataStream.close();
    }
}
