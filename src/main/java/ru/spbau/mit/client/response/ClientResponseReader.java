package ru.spbau.mit.client.response;

import org.apache.commons.io.IOUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ClientResponseReader {
    private DataInputStream socketInputStream;

    public ClientResponseReader(Socket socket) throws IOException {
        socketInputStream = new DataInputStream(socket.getInputStream());
    }


    public StatResponse nextStatResponse() throws IOException {
        StatResponse response = new StatResponse();
        List<Integer> parts = new ArrayList<>();
        int size = socketInputStream.readInt();
        for (int index = 0; index < size; index++) {
            parts.add(socketInputStream.readInt());
        }
        response.setParts(parts);
        return response;
    }

    public GetReadableResponse nextGetReadableResponse() throws IOException {
        GetReadableResponse response = new GetReadableResponse();
        response.setInputStream(socketInputStream);
        return response;
    }

    public static void fullDownloadReadableResponse(GetReadableResponse response, Path downloadPath, long
            offset, long size)
            throws IOException {
        RandomAccessFile file = new RandomAccessFile(downloadPath.toString(), "rw");
        file.seek(offset);
        IOUtils.copyLarge(response.getInputStream(), Channels.newOutputStream(file.getChannel()), 0, size);
        file.close();
    }

    public void close() throws IOException {
        socketInputStream.close();
    }
}
