package br.com.ygsoftware.sysco.model.get;

import br.com.ygsoftware.sysco.utils.Check;

/**
 * Created by adriana on 26/07/2017.
 */

public class GetArray extends Get<String[]> {

    public GetArray(String key, String... value) {
        super(key, value);
    }

    @Override
    public void setValue(String... value) {
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
        return getValue().length;
    }

}
