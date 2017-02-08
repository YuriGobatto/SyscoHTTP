package br.com.ygsoftware.sysco.interfaces;

import br.com.ygsoftware.sysco.model.Response;

/**
 * Created by adriana on 16/12/2016.
 */

public interface RequestListener {

    void onSuccess(String url, Response response);

    void onError(String url, Exception e);

}
