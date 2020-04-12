package com.sid.shaheenfalcon;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SFRequest implements Serializable {
    private String method = "";
    private String url = "";
    Map<String, String> headers = null;
    byte[] data = null;

    SFRequest(){
        this.headers = new HashMap<String, String>();
    }

    SFRequest(String method, String url, Map<String, String> headers){
        this.method = method;
        this.url = url;
        this.headers = headers;
    }

    SFRequest(String method, String url, Map<String, String> headers, byte[] data){
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.data = data;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getMethod(){
        return this.method;
    }

    public String getUrl() {
        return url;
    }

    public byte[] getData() { return  data; }

}
