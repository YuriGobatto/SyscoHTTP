package br.com.ygsoftware.sysco.array;

import java.util.ArrayList;

import br.com.ygsoftware.sysco.model.Method;


public class DataArray {

    private ArrayList<Method> methods;

    public DataArray() {
        methods = new ArrayList<>();
    }

    public DataArray(DataArray array) {
        this.methods = array.methods;
    }

    public <M extends Method> DataArray(ArrayList<M> array) {
        this.methods = (ArrayList<Method>) array;
    }

    public <M extends Method> DataArray add(M method) {
        methods.add(method);
        return this;
    }

    public boolean contains(String key){
        return get(key) != null;
    }

    public DataArray remove(String key) {
        if(contains(key)) {
            methods.remove(get(key));
        }
        return this;
    }

    public Method get(String key){
        for (Method data : methods) {
            if(key.equals(data.getKey())){
                return data;
            }
        }
        return null;
    }

    public int size() {
        return methods.size();
    }

    public <M extends Method> DataArray update(String key, M newMethod) {
        methods.set(methods.indexOf(get(key)), newMethod);
        return this;
    }

    public DataArray removeAll() {
        methods.clear();
        return this;
    }

    public ArrayList<Method> getAll() {
        return methods;
    }
}
