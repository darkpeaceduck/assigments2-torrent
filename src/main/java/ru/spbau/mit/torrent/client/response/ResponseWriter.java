package ru.spbau.mit.torrent.client.response;


import org.apache.commons.io.IOUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;


public class ResponseWriter {
    private DataOutputStream dataStream;

    public ResponseWriter(Socket socket) throws IOException {
        dataStream = new DataOutputStream(socket.getOutputStream());
    }

    private void writeStatResponse(StatResponse response) throws IOException {
        dataStream.writeInt(response.getParts().size());
        for (Integer id : response.getParts()) {
            dataStream.writeInt(id);
        }
    }

    private void writeGetWritableResponse(GetWritableResponse response) throws IOException {
        IOUtils.copyLarge(Files.newInputStream(response.getEntry().getPath()), dataStream, response.getEntry()
                        .getOffset(),
                response.getEntry().getSize());
    }

    public void writeResponse(Response response) throws IOException {
        switch (response.getType()) {
            case STAT:
                writeStatResponse((StatResponse) response);
                break;
            case GET_WR:
                writeGetWritableResponse((GetWritableResponse) response);
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
