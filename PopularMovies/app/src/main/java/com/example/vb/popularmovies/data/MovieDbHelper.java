package com.example.vb.popularmovies.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.vb.popularmovies.data.MovieContract.MovieEntry;
import com.example.vb.popularmovies.data.MovieContract.ReviewEntry;
import com.example.vb.popularmovies.data.MovieContract.TrailerEntry;

/**
 * Created by vb on 6/2/2016.
 */
public class MovieDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 5;

    static final String MOVIE_DATABASE_NAME = "popularmovie.db";

    public MovieDbHelper(Context context) {
        super(context, MOVIE_DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_MOVIE_TABLE = "CREATE TABLE " + MovieEntry.TABLE_NAME +
                "(" + MovieEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "+
                MovieEntry.ID + " INEGER NOT NULL, " +
                MovieEntry.POSTER_PATH + " TEXT NOT NULL, " +
                MovieEntry.TITLE + " TEXT NOT NULL, " +
                MovieEntry.OVERVIEW + " TEXT NOT NULL, " +
                MovieEntry.RELEASE_DATE + " TEXT NOT NULL, " +
                MovieEntry.VOTE_AVERAGE + " TEXT NOT NULL, " +
                MovieEntry.THUMBNAIL + " TEXT NOT NULL); ";

        final String SQL_CREATE_TRAILER_TABLE = "CREATE TABLE " + TrailerEntry.TABLE_NAME +
                "(" + TrailerEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "+
                TrailerEntry.ID + " INEGER NOT NULL, " +
                TrailerEntry.NAME + " TEXT NOT NULL, " +
                TrailerEntry.KEY + " TEXT NOT NULL, " +
                "FOREIGN KEY(movie_id) REFERENCES " + MovieEntry.TABLE_NAME +"(movie_id) );" ;

        final String SQL_CREATE_REVIEW_TABLE = "CREATE TABLE " + ReviewEntry.TABLE_NAME +
                "(" + ReviewEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "+
                ReviewEntry.ID + " INEGER NOT NULL, " +
                ReviewEntry.AUTHOR + " TEXT NOT NULL, " +
                ReviewEntry.REVIEW + " TEXT NOT NULL, " +
                "FOREIGN KEY(movie_id) REFERENCES "+ MovieEntry.TABLE_NAME +"(movie_id) );" ;

        sqLiteDatabase.execSQL(SQL_CREATE_MOVIE_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_REVIEW_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_TRAILER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MovieEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ReviewEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TrailerEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
