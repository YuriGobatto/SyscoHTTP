package br.com.ygsoftware.sysco.model.get;

import android.os.Parcel;

import br.com.ygsoftware.sysco.utils.Check;

/**
 * Created by adriana on 26/01/2017.
 */

public final class GetString extends Get<String> {

    public GetString(String key, String value) {
        super(key, value);
    }

    private GetString(Parcel in) {
        super(in);
        setValue(in.readString());
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

    public int getLength(){
        return getValue().length();
    }

    protected static final Creator<GetString> CREATOR = new Creator<GetString>() {
        @Override
        public GetString createFromParcel(Parcel source) {
            return new GetString(source);
        }

        @Override
        public GetString[] newArray(int size) {
            return new GetString[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(getValue());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
