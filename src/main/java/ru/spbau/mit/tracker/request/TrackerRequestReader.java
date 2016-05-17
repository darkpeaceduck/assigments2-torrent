package ru.spbau.mit.tracker.request;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class TrackerRequestReader {
    public static class ParseRequestException extends Exception {
        private static final long serialVersionUID = 4448519501858646038L;
    }

    private DataInputStream inputStream;

    public TrackerRequestReader(Socket socket) throws IOException {
        this.inputStream = new DataInputStream(socket.getInputStream());
    }

    public TrackerRequest nextRequest() throws IOException, ParseRequestException {
        Byte type = inputStream.readByte();
        switch (type) {
            case 1:
                return continueReadingListRequest();
            case 2:
                return continueReadingUploadRequest();
            case 3:
                return continueReadingSourcesRequest();
            case 4:
                return continueReadingUpdateRequest();
            default:
                throw new ParseRequestException();
        }
    }

    public void close() throws IOException {
        inputStream.close();
    }

    private ListRequest continueReadingListRequest() {
        return new ListRequest();
    }

    private UploadRequest continueReadingUploadRequest() throws IOException {
        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.setName(inputStream.readUTF());
        uploadRequest.setSize(inputStream.readLong());
        return uploadRequest;
    }

    private SourcesRequest continueReadingSourcesRequest() throws IOException {
        SourcesRequest sourcesRequest = new SourcesRequest();
        sourcesRequest.setId(inputStream.readInt());
        return sourcesRequest;
    }

    private UpdateRequest continueReadingUpdateRequest() throws IOException {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setSeedPort(inputStream.readShort());
        int count = inputStream.readInt();
        for (int i = 0; i < count; i++) {
            updateRequest.addId(inputStream.readInt());
        }
        return updateRequest;
    }
}
