package br.com.ygsoftware.sysco.model.post;

import android.os.Parcel;

import br.com.ygsoftware.sysco.enums.RequestMethods;
import br.com.ygsoftware.sysco.model.Method;

/**
 * Created by adriana on 26/01/2017.
 */

abstract class Post<T> extends Method<T> {

    Post(String key, T value) {
        super(key, value, RequestMethods.POST);
    }

    Post(Parcel in) {
        super(in);
    }

}
