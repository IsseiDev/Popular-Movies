package com.example.vb.popularmovies;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by vb on 5/31/2016.
 */
public class Review implements Parcelable {
    private String author;
    private String review;

    public Review(String author, String review){
        this.author = author;
        this.review = review;
    }

    public String getAuthor(){
        return author;
    }

    public String getReview(){
        return review;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{author,review});
    }

    public static final Parcelable.Creator<Review> CREATOR = new Parcelable.Creator<Review>() {
        public Review createFromParcel(Parcel in) {
            return new Review(in);
        }

        public Review[] newArray(int size) {
            return new Review[size];
        }
    };

    private Review(Parcel in){
        String[] data = new String[2];
        in.readStringArray(data);
        author = data[0];
        review = data[1];
    }
}
