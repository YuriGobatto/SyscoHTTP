package br.com.ygsoftware.sysco.model.get;

import br.com.ygsoftware.sysco.enums.RequestMethods;
import br.com.ygsoftware.sysco.model.Method;

/**
 * Created by adriana on 26/01/2017.
 */

abstract class Get extends Method<String> {

    Get(String key, String value) {
        super(key, value, RequestMethods.GET);
    }

}
