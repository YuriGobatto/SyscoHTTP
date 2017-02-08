package br.com.ygsoftware.sysco.array;

import java.util.ArrayList;

import br.com.ygsoftware.sysco.model.Method;

/**
 * Created by adriana on 26/01/2017.
 */

public class DataArray extends ArrayList<Method> {


    public boolean contains(String key){
        return get(key) != null;
    }

    public void remove(String key){
        if(contains(key)) {
            remove(get(key));
        }
    }

    public Method get(String key){
        for (Method data : this){
            if(key.equals(data.getKey())){
                return data;
            }
        }
        return null;
    }
}
