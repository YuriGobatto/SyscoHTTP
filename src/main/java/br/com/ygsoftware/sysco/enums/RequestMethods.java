package br.com.ygsoftware.sysco.enums;

public enum RequestMethods {

    GET("GET", 0), POST("POST", 1), FILES("POST", 2);

    private String method;
    private int type;

    RequestMethods(String method, int type) {
        this.method = method;
        this.type = type;
    }

    public String getMethod() {
        return method;
    }
}

