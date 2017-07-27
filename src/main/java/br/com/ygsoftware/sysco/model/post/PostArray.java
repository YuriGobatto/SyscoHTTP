package br.com.ygsoftware.sysco.model.post;

import android.os.Parcel;

import br.com.ygsoftware.sysco.utils.Check;

/**
 * Created by adriana on 26/07/2017.
 */

public class PostArray extends Post<String[]> {

    public PostArray(String key, String... value) {
        super(key + "[]", value);
    }

    private PostArray(Parcel source) {
        super(source);
        setValue(source.readString().split("::"));
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

    protected static final Creator<PostArray> CREATOR = new Creator<PostArray>() {
        @Override
        public PostArray createFromParcel(Parcel source) {
            return new PostArray(source);
        }

        @Override
        public PostArray[] newArray(int size) {
            return new PostArray[size];
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
