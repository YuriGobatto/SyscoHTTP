package br.com.ygsoftware.sysco.model;

import br.com.ygsoftware.sysco.enums.RequestMethods;
import br.com.ygsoftware.sysco.utils.Check;


public abstract class Method<T> {

    private String key;
    private T value;
    private RequestMethods method = RequestMethods.POST;

    private boolean requested;

    public Method(String key, T value, RequestMethods method) {
        if (!Check.isValidKey(key)) {
            try {
                throw new Exception("The key is not valid");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        key = Check.prepareKey(key);
        this.key = key;
        this.value = value;
        this.method = method;
    }

    public String getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public final RequestMethods getMethod() {
        return method;
    }

    final boolean isRequested() {
        return requested;
    }

    final void setRequested(boolean requested) {
        this.requested = requested;
    }

}
