package br.com.ygsoftware.sysco.enums;

public enum RequestMethods {

    GET("GET"), POST("POST"), FILES("POST");

    private String method;

    RequestMethods(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }
}
