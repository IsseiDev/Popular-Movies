package com.example.vb.popularmovies;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by vb on 6/1/2016.
 */


public class Trailer implements Parcelable{
    private String name;
    private String key;

    public Trailer(String n,String k){
        name = n;
        key = k;
    }

    public String getName(){ return name;}

    public String getKey(){return key;}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{name,key});
    }

    public static final Parcelable.Creator<Trailer> CREATOR = new Parcelable.Creator<Trailer>() {
        public Trailer createFromParcel(Parcel in) {
            return new Trailer(in);
        }

        public Trailer[] newArray(int size) {
            return new Trailer[size];
        }
    };

    private Trailer(Parcel in){
        String[] data = new String[2];
        in.readStringArray(data);
        name = data[0];
        key = data[1];
    }
}
