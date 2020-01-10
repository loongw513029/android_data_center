package com.sztvis.datacenter.socket.resp;


public class StreamResult {
    //control stream result
    private boolean status;
    //the message of success or faild
    private String message;
    //output rtmp url when control success
    private String streamURL;

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStreamURL() {
        return streamURL;
    }

    public void setStreamURL(String streamURL) {
        this.streamURL = streamURL;
    }
}
