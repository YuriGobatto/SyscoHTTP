package br.com.ygsoftware.sysco.model.post;

import android.os.Parcel;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by adriana on 26/01/2017.
 */

public final class PostFile extends Post<File> {

    public PostFile(String key, File file) {
        super(key, file);
        if (!file.exists()) {
            try {
                throw new FileNotFoundException("The file " + file.getName() + " not found in " + file.getPath());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private PostFile(Parcel in) {
        super(in);
        setValue(in.readString());
    }

    @Override
    public void setValue(File file) {
        if (!file.exists()) {
            try {
                throw new FileNotFoundException("The file " + file.getName() + " not found in " + file.getPath());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        super.setValue(file);
    }

    public void setValue(String path){
        setValue(new File(path));
    }

    public String getPath(){
        return getValue().getPath();
    }

    public File[] listFiles(){
        return getValue().listFiles();
    }

    public String getName(){
        return getValue().getName();
    }

    protected static final Creator<PostFile> CREATOR = new Creator<PostFile>() {
        @Override
        public PostFile createFromParcel(Parcel source) {
            return new PostFile(source);
        }

        @Override
        public PostFile[] newArray(int size) {
            return new PostFile[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(getPath());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}