package ru.spbau.mit.torrent.client.request;


public class StatRequest extends Request {
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
