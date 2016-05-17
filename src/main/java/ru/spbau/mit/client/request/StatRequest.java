package ru.spbau.mit.client.request;


public class StatRequest extends ClientRequest {
    private Integer id;

    public StatRequest() {
        super();
        setType(Type.STAT);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
