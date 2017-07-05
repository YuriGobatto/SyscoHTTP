package br.com.ygsoftware.sysco.model.post;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import br.com.ygsoftware.sysco.utils.Check;

/**
 * Created by adriana on 26/01/2017.
 */

public final class PostString extends Post<String> {

    public PostString(String key, String value) {
        super(key, value);
    }

    public PostString(String key, String value, boolean encode) throws UnsupportedEncodingException {
        this(key, (encode ? URLEncoder.encode(value, "UTF-8") : value));
    }

    @Override
    public void setValue(String value) {
        if (!Check.isValidValue(value)) {
            try {
                throw new Exception("The value is not valid");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.setValue(value);
    }

    public int getLength() {
        return getValue().length();
    }
}
