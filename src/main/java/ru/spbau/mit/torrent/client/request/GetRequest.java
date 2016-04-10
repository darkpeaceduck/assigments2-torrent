package ru.spbau.mit.torrent.client.request;

public class GetRequest extends Request {
    private Integer id;
    private Integer part;

    public GetRequest() {
        super();
        setType(Type.GET);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPart() {
        return part;
    }

    public void setPart(Integer part) {
        this.part = part;
    }
}
