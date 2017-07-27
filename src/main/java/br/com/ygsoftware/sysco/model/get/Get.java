package br.com.ygsoftware.sysco.model.get;

import android.os.Parcel;

import br.com.ygsoftware.sysco.enums.RequestMethods;
import br.com.ygsoftware.sysco.model.Method;

/**
 * Created by adriana on 26/01/2017.
 */

abstract class Get<S> extends Method<S> {

    Get(String key, S value) {
        super(key, value, RequestMethods.GET);
    }

    Get(Parcel in) {
        super(in);
    }

}
