package br.com.ygsoftware.sysco.interfaces;

import br.com.ygsoftware.sysco.model.Response;

/**
 * Created by adriana on 21/12/2016.
 */
public interface SSEListener {

    void onReadLine(Response response, String line);

    void onError(Exception e);

}
