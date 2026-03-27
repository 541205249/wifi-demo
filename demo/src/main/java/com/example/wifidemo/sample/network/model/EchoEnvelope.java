package com.example.wifidemo.sample.network.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class EchoEnvelope {
    private String url;
    private String origin;
    private String method;
    private String data;
    private Map<String, String> args = new LinkedHashMap<>();
    private Map<String, String> form = new LinkedHashMap<>();
    private Map<String, String> files = new LinkedHashMap<>();
    private Map<String, String> headers = new LinkedHashMap<>();
    private Map<String, Object> json = new LinkedHashMap<>();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Map<String, String> getArgs() {
        return args;
    }

    public void setArgs(Map<String, String> args) {
        this.args = args;
    }

    public Map<String, String> getForm() {
        return form;
    }

    public void setForm(Map<String, String> form) {
        this.form = form;
    }

    public Map<String, String> getFiles() {
        return files;
    }

    public void setFiles(Map<String, String> files) {
        this.files = files;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, Object> getJson() {
        return json;
    }

    public void setJson(Map<String, Object> json) {
        this.json = json;
    }
}
