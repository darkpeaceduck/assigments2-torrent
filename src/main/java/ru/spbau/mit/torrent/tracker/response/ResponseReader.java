package ru.spbau.mit.torrent.tracker.response;

import ru.spbau.mit.torrent.tracker.FilesProcessor;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ResponseReader {
    private DataInputStream socketInputStream;

    public ResponseReader(Socket socket) throws IOException {
        socketInputStream = new DataInputStream(socket.getInputStream());
    }


    public ListResponse nextListResponse() throws IOException {
        ListResponse response = new ListResponse();
        List<FilesProcessor.FileInfo> fileInfos = new ArrayList<>();
        int size = socketInputStream.readInt();
        for (int index = 0; index < size; index++) {
            FilesProcessor.FileInfo fileInfo = new FilesProcessor.FileInfo();
            fileInfo.setId(socketInputStream.readInt());
            fileInfo.setName(socketInputStream.readUTF());
            fileInfo.setSize(socketInputStream.readLong());
            fileInfos.add(fileInfo);
        }
        response.setFileInfos(fileInfos);
        return response;
    }

    public SourcesResponse nextSourcesResponse() throws IOException {
        SourcesResponse response = new SourcesResponse();
        List<InetSocketAddress> peers = new ArrayList<>();
        int size = socketInputStream.readInt();
        for (int i = 0; i < size; i++) {
            final int ipv4BytesNum = 4;
            byte[] bytes = new byte[ipv4BytesNum];
            socketInputStream.read(bytes);
            peers.add(new InetSocketAddress(InetAddress.getByAddress(bytes),
                    socketInputStream
                            .readShort()));
        }
        response.setSocketAddresses(peers);
        return response;
    }

    public UploadResponse nextUploadResponse() throws IOException {
        UploadResponse uploadResponse = new UploadResponse();
        uploadResponse.setId(socketInputStream.readInt());
        return uploadResponse;
    }

    public UpdateResponse nextUpdateResponse() throws IOException {
        UpdateResponse response = new UpdateResponse();
        response.setStatus(socketInputStream.readBoolean());
        return response;
    }

    public void close() throws IOException {
        socketInputStream.close();
    }
}
