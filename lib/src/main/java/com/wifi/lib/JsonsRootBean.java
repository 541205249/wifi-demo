package com.wifi.lib;

import com.fasterxml.jackson.annotation.JsonProperty;


public class JsonsRootBean {

    @JsonProperty("errCode")
    private int errcode;
    private String host;
    private int port;

    public void setErrCode(int errcode) {
        this.errcode = errcode;
    }

    public int getErrCode() {
        return errcode;
    }

    public void setAddress(String address) {
        this.host = address;
    }

    public String getAddress() {
        return host == null ? "120.25.163.9" : host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPort() {
        String ports = Integer.toString(port);
        return ports;
    }

    public int getIntPort() {
        return port;
    }

}