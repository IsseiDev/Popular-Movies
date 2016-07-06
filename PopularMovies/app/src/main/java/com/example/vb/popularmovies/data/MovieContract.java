package com.example.vb.popularmovies.data;

import android.provider.BaseColumns;

/**
 * Created by vb on 6/3/2016.
 */
public class MovieContract {

    public static final String CONTENT_AUTHORITY = "com.example.vb.popularmovies.provider";

    public static class MovieEntry implements BaseColumns{
        public static final String TABLE_NAME = "movies";
        public static final String ID = "movie_id";
        public static final String POSTER_PATH = "poster_path";
        public static final String TITLE = "title";
        public static final String OVERVIEW = "overview";
        public static final String RELEASE_DATE = "releaseDate";
        public static final String VOTE_AVERAGE = "vote_average";
        public static final String THUMBNAIL = "thumbNail";

    }

    public static class ReviewEntry implements BaseColumns{
        public static final String TABLE_NAME = "reviews";
        public static final String ID = "movie_id";
        public static final String AUTHOR = "author";
        public static final String REVIEW = "review";

    }

    public static class TrailerEntry implements BaseColumns{
        public static final String TABLE_NAME = "trailers";
        public static final String ID = "movie_id";
        public static final String NAME = "name";
        public static final String KEY = "key";

    }

}
