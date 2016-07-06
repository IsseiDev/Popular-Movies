package com.example.vb.popularmovies;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by vb on 4/7/2016.
 */

// Has attributes of movie
public class Movie implements Parcelable{

    private int id;
    private String poster_path;
    private String title;
    private String overView;
    private String releaseDate;
    private String vote_average;
    private String thumbNail;

    public Movie(int id,String poster_path, String title, String overView, String releaseDate, String vote_average, String thumbNail){
        this.id = id;
        this.poster_path = poster_path;
        this.title = title;
        this.overView = overView;
        this.vote_average = vote_average;
        this.releaseDate = releaseDate;
        this.thumbNail = thumbNail;
    }

    public String getPosterPath(){
        return poster_path;
    }

    public String getThumbNail(){
        return thumbNail;
    }

    public String getTitle(){
        return title;
    }

    public String getOverView(){
        return overView;
    }

    public int getId(){
        return id;
    }

    public String getVoteAverage(){
        return vote_average;
    }

    public String getReleaseDate(){
        return releaseDate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{poster_path, title, overView, releaseDate, vote_average, thumbNail});
        dest.writeInt(id);
    }

    public static final Parcelable.Creator<Movie> CREATOR = new Parcelable.Creator<Movie>() {
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }

        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    private Movie(Parcel in) {
        String[] data = new String[6];
        in.readStringArray(data);

        id = in.readInt();
        poster_path = data[0];
        title = data[1];
        overView = data[2];
        releaseDate = data[3];
        vote_average = data[4];
        thumbNail = data[5];
    }
}
