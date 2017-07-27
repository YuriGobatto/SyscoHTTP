package br.com.ygsoftware.sysco.model.get;

import android.os.Parcel;

import br.com.ygsoftware.sysco.utils.Check;

/**
 * Created by adriana on 26/07/2017.
 */

public class GetArray extends Get<String[]> {

    public GetArray(String key, String... value) {
        super(key, value);
    }

    private GetArray(Parcel in) {
        super(in);
        setValue(in.readString().split("::"));
    }

    private static String createString(String[] s) {
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (String value : s) {
            if (!isFirst) {
                builder.append("::");
            }
            builder.append(value);
        }
        return builder.toString();
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

    protected static final Creator<GetArray> CREATOR = new Creator<GetArray>() {
        @Override
        public GetArray createFromParcel(Parcel source) {
            return new GetArray(source);
        }

        @Override
        public GetArray[] newArray(int size) {
            return new GetArray[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(createString(getValue()));
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
