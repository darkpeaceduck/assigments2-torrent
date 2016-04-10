package ru.spbau.mit.torrent.client.response;

import java.io.InputStream;

public class GetReadableResponse extends Response{
    private InputStream inputStream;

    GetReadableResponse() {
        super();
        setType(Type.GET_R);
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }
}
