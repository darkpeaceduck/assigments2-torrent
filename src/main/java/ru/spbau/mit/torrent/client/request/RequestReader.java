package ru.spbau.mit.torrent.client.request;


import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class RequestReader {
    public static class ParseRequestException extends Exception {
        private static final long serialVersionUID = 4448519501858646038L;
    }
    private DataInputStream inputStream;

    public RequestReader(Socket socket) throws IOException {
        this.inputStream = new DataInputStream(socket.getInputStream());
    }

    private GetRequest continueReadingGetRequest() throws IOException {
        GetRequest getRequest = new GetRequest();
        getRequest.setId(inputStream.readInt());
        getRequest.setPart(inputStream.readInt());
        return getRequest;
    }

    private StatRequest continueReadingStatRequest() throws IOException {
        StatRequest statRequest = new StatRequest();
        statRequest.setId(inputStream.readInt());
        return statRequest;
    }

    public Request nextRequest() throws IOException, ParseRequestException {
        Byte type = inputStream.readByte();
        switch (type) {
            case 1:
                return continueReadingStatRequest();
            case 2:
                return continueReadingGetRequest();
            default:
                throw new ParseRequestException();
        }
    }

    public void close() throws IOException {
        inputStream.close();
    }
}
