package ru.spbau.mit.client.response;

import java.io.InputStream;

public class GetReadableResponse extends ClientResponse {
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
