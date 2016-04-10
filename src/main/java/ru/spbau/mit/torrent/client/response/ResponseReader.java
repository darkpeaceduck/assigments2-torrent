package ru.spbau.mit.torrent.client.response;

import org.apache.commons.io.IOUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ResponseReader {
    private DataInputStream socketInputStream;

    public ResponseReader(Socket socket) throws IOException {
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

    public static void fullDownloadReadableResponse(GetReadableResponse response, Path downloadPath, Long size)
            throws IOException {
        OutputStream outputStream = Files.newOutputStream(downloadPath);
        IOUtils.copyLarge(response.getInputStream(), outputStream, 0, size);
        outputStream.close();
    }

    public void close() throws IOException {
        socketInputStream.close();
    }
}
