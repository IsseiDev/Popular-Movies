package com.example.vb.popularmovies.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.example.vb.popularmovies.data.MovieContract.MovieEntry;
import com.example.vb.popularmovies.data.MovieContract.ReviewEntry;
import com.example.vb.popularmovies.data.MovieContract.TrailerEntry;

import java.util.HashMap;

/**
 * Created by vb on 6/2/2016.
 */
public class MovieProvider extends ContentProvider {

    public static final String PROVIDER_NAME = "com.example.vb.popularmovies.provider";
    public static final String URL = "content://" + PROVIDER_NAME + "/movies";
    public static final String review_URL = "content://" + PROVIDER_NAME + "/reviews";
    public static final String trailer_URL = "content://" + PROVIDER_NAME + "/trailers";
    public static final Uri CONTENT_URI = Uri.parse(URL);
    public static final Uri REVIEW_CONTENT_URI = Uri.parse(review_URL);
    public static final Uri TRAILER_CONTENT_URI = Uri.parse(trailer_URL);
    public static final int MOVIE = 1;
    public static final int MOVIE_ID = 2;
    public static final int DBREVIEW = 3;
    public static final int DBREVIEW_ID = 4;
    public static final int DBTRAILER = 5;
    public static final int DBTRAILER_ID = 6;
    private SQLiteDatabase db;
    private static HashMap<String, String> MOVIE_PROJECTION_MAP;


    static final UriMatcher uriMatcher;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(PROVIDER_NAME, "movies", MOVIE);
        uriMatcher.addURI(PROVIDER_NAME, "movies/#", MOVIE_ID);

        uriMatcher.addURI(PROVIDER_NAME, "reviews", DBREVIEW);
        uriMatcher.addURI(PROVIDER_NAME, "reviews/#", DBREVIEW_ID);

        uriMatcher.addURI(PROVIDER_NAME, "trailers", DBTRAILER);
        uriMatcher.addURI(PROVIDER_NAME, "trailers/#", DBTRAILER_ID);
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        MovieDbHelper dbHelper = new MovieDbHelper(context);

        db = dbHelper.getWritableDatabase();
        return db!=null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        final int match = uriMatcher.match(uri);
        long rowID;
        Uri _uri;

        switch(match){
            case MOVIE:     rowID = db.insertOrThrow(MovieEntry.TABLE_NAME, "", values);
                            _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
                            getContext().getContentResolver().notifyChange(_uri, null);
                            return _uri;

            case DBREVIEW:  rowID = db.insertOrThrow(ReviewEntry.TABLE_NAME, "", values);
                            _uri = ContentUris.withAppendedId(REVIEW_CONTENT_URI, rowID);
                            getContext().getContentResolver().notifyChange(_uri, null);
                            return _uri;

            case DBTRAILER:  rowID = db.insertOrThrow(TrailerEntry.TABLE_NAME, "", values);
                            _uri = ContentUris.withAppendedId(TRAILER_CONTENT_URI, rowID);
                            getContext().getContentResolver().notifyChange(_uri, null);
                            return _uri;
        }
        throw new SQLException("Failed to add a record into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){
            case MOVIE:
                count = db.delete(MovieEntry.TABLE_NAME, selection, selectionArgs);
                break;

            case DBREVIEW:
                count = db.delete( ReviewEntry.TABLE_NAME, selection,selectionArgs);
                break;

            case DBTRAILER:
                count = db.delete( TrailerEntry.TABLE_NAME, selection,selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case MOVIE:
                qb.setTables(MovieEntry.TABLE_NAME);
                break;

            case DBREVIEW:
                qb.setTables(ReviewEntry.TABLE_NAME);
                break;

            case DBTRAILER:
                qb.setTables(TrailerEntry.TABLE_NAME);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Cursor c = qb.query(db,	projection,	selection, selectionArgs,null, null, null);

        /**
         * register to watch a content URI for changes
         */
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){
            case MOVIE:
                count = db.update(MovieEntry.TABLE_NAME, values, selection, selectionArgs);
                break;

            case DBREVIEW:
                count = db.update(ReviewEntry.TABLE_NAME, values, selection, selectionArgs);
                break;

            case DBTRAILER:
                count = db.update(TrailerEntry.TABLE_NAME, values, selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri );
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)){
            case MOVIE:
                return "vnd.android.cursor.dir/vnd.popularmovies.movies";

            case MOVIE_ID:
                return "vnd.android.cursor.item/vnd.popularmovies.movies";

            case DBREVIEW:
                return "vnd.android.cursor.dir/vnd.popularmovies.reviews";

            case DBREVIEW_ID:
                return "vnd.android.cursor.item/vnd.popularmovies.reviews";

            case DBTRAILER:
                return "vnd.android.cursor.dir/vnd.popularmovies.trailers";

            case DBTRAILER_ID:
                return "vnd.android.cursor.item/vnd.popularmovies.trailers";

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }
}
