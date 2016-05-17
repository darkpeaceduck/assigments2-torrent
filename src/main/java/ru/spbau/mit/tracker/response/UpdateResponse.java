package ru.spbau.mit.tracker.response;

public class UpdateResponse extends TrackerResponse {
    private boolean status;

    public UpdateResponse() {
        super();
        setType(Type.UPDATE);
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
