package com.example.vb.popularmovies;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vb.popularmovies.data.MovieContract.MovieEntry;
import com.example.vb.popularmovies.data.MovieContract.ReviewEntry;
import com.example.vb.popularmovies.data.MovieContract.TrailerEntry;
import com.example.vb.popularmovies.data.MovieProvider;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class MovieDetailsFragment extends Fragment {

    private String[] month = {"January","February","March","April","May","June","July","August","September","October","November","December"};
    private String BASE_URL = "http://api.themoviedb.org/3/movie/";
    private String API_KEY = "api_key";
    private Movie movie;
    private ArrayList<Review> reviewsList;
    private ArrayList<Trailer> trailerList;
    private View view;
    private boolean detailsFilled = false; // review and trailer details
    private ImageView star;
    private boolean favourite = false; // true if movie is added to favourite
    private boolean in_favourite = false;
    private boolean dbOperationComplete = true; // false if operation is taking place
    private static FavouriteFragment favouriteFragment;

    public MovieDetailsFragment(){
        reviewsList = new ArrayList<>();
        trailerList = new ArrayList<>();
    }

    public static MovieDetailsFragment newInstance(Movie m,boolean favourite,FavouriteFragment fav){
        MovieDetailsFragment f = new MovieDetailsFragment();
        favouriteFragment = fav;
        Bundle args = new Bundle();
        args.putParcelable("selectedMovie",m);
        if(favourite)
            args.putBoolean("favourites",true);
        else
            args.putBoolean("favourites",false);
        f.setArguments(args);
        return f;
    }

    public Movie getSelectedMovie(){
        return getArguments().getParcelable("selectedMovie");
    }

    // return true when displaying favourite movies
    public boolean inFavourites(){
        return getArguments().getBoolean("favourites");
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        outState.putParcelable("movieObject",movie);
        outState.putParcelableArrayList("reviews",(ArrayList<Review>)reviewsList);
        outState.putParcelableArrayList("trailers",(ArrayList<Trailer>)trailerList);
        outState.putBoolean("favourite",favourite);
        detailsFilled = false;
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_movie_details, container, false);
        reviewsList = new ArrayList<>();
        trailerList = new ArrayList<>();

        if(savedInstanceState!=null && savedInstanceState.containsKey("movieObject")){
            movie = savedInstanceState.getParcelable("movieObject");
            reviewsList = savedInstanceState.getParcelableArrayList("reviews");
            trailerList = savedInstanceState.getParcelableArrayList("trailers");
            createReviewTextView(reviewsList);
            createTrailerTextView(trailerList);
            detailsFilled = true;
            favourite = savedInstanceState.getBoolean("favourite");
        }
        else if(!MainActivity.mTwoPane){
            Bundle data = getActivity().getIntent().getExtras();
            if(data!=null) {
                if (data.containsKey("favourites") && data.getBoolean("favourites")) {
                    in_favourite = true;
                }
                if (data.containsKey("movieobj"))
                    movie = (Movie) data.getParcelable("movieobj");
            }
        }
        else{
            movie = getSelectedMovie();
            in_favourite = inFavourites();
        }

        if(checkDb(movie.getId())){
           favourite = true;
        }

        ImageView headerImage = (ImageView) view.findViewById(R.id.headerimage);
        Picasso.with(getActivity()).load(movie.getThumbNail()).into(headerImage);

        TextView title = (TextView) view.findViewById(R.id.movie_title);
        title.setText(movie.getTitle());

        ImageView thumbnail = (ImageView) view.findViewById(R.id.movie_thumbnail);
        Picasso.with(getActivity()).load(movie.getPosterPath()).into(thumbnail);

        String date = movie.getReleaseDate();
        String[] mdate = date.split("-");
        date = month[Integer.parseInt(mdate[1])-1] + " " + mdate[2] + " " + mdate[0];

        TextView details = (TextView) view.findViewById(R.id.movie_details);
        String movieDetails = "Release Date:\n"+date+"\n\nAverage Vote:\n"+movie.getVoteAverage()+"/10";
        details.setText(movieDetails);

        star = (ImageView) view.findViewById(R.id.star);
        if(favourite)
            star.setImageResource(R.drawable.star2);
        else
            star.setImageResource(R.drawable.star1);
        star.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dbOperationComplete) {
                    favourite = !favourite;
                    // add to favourites only when network connection is available when in popular/top rated screen
                    // always add to favourite when in favourites screen and star is tapped
                    if (favourite && (checkNetworkConnection() || in_favourite) ) {
                        star.setImageResource(R.drawable.star2);
                        if(Favourite.mTwoPane){
                            favouriteFragment.updateFavouriteMovieList(movie,true); // update list view if any movie is added/removed to favourites
                        }
                        writeToDb();
                        Toast.makeText(getContext(), "Added To Favourites", Toast.LENGTH_SHORT).show();
                    } else {
                        star.setImageResource(R.drawable.star1);
                        if(Favourite.mTwoPane){
                            favouriteFragment.updateFavouriteMovieList(movie,false); // update list view if any movie is added/removed to favourites
                        }
                        removeFromDb();
                    }
                }
            }
        });

        TextView synopsis = (TextView) view.findViewById(R.id.synopsis);
        synopsis.setText(movie.getOverView());

        if(!detailsFilled ) {
            if (checkNetworkConnection()) {
                getReview();
                getTrailers();
                detailsFilled = true;

                // update contents of db with latest information if movie is in favourites
                if(favourite)
                    updateDb();

            } else {
                if(in_favourite) {
                    detailsFilled = true;
                    readFromDb();
                }
                else
                    Toast.makeText(getContext(), "Network Problem", Toast.LENGTH_LONG).show();
            }
        }


        return view;
    }

    // retrieve reviews and trailers from DB
    private void readFromDb(){
        DataBaseTask dbtask = new DataBaseTask();
        dbtask.execute("read");
    }

    private void updateDb(){
        DataBaseTask dbtask = new DataBaseTask();
        dbtask.execute("update");
    }

    // returns true if movie is in DB
    private boolean checkDb(int id){
        String selection = MovieEntry.ID + "="+ id;
        String[] projection = {MovieEntry.ID};
        Cursor cursor = getContext().getContentResolver().query(MovieProvider.CONTENT_URI,projection,selection,null,null);
        if (!(cursor.moveToFirst()) || cursor.getCount() ==0) {
            cursor.close();
            return false;
        }
        cursor.close();
        return true;
    }

    private void writeToDb(){
        DataBaseTask dbtask = new DataBaseTask();
        dbtask.execute("write");
    }

    private void removeFromDb(){
        DataBaseTask dbtask = new DataBaseTask();
        dbtask.execute("remove");
    }

    private boolean checkNetworkConnection() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(getContext().CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void getReview(){
        FetchReview reviewTask=new FetchReview();
        reviewTask.execute();
    }

    private void getTrailers(){
        FetchTrailer trailerTask=new FetchTrailer();
        trailerTask.execute();
    }

    public CardView createCardView(){
        CardView card = new CardView(getContext());
        card.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        card.setRadius(4);
        return card;
    }

    public void createTrailerTextView(ArrayList<Trailer> trailers){
        LinearLayout layout = (LinearLayout) view.findViewById(R.id.trailerView);

        if(trailers.size() == 0){
            CardView card = createCardView();
            TextView text = new TextView(getContext());
            text.setPadding(30, 0, 35, 35);
            text.setText("No Trailer Available");
            card.addView(text);
            layout.addView(card);
        }

        for(Trailer t:trailers){
            if(t!=null){
                CardView card = createCardView();
                final String key = t.getKey();
                card.setCardBackgroundColor(Color.parseColor("#FFF3F5F6"));

                TextView text = new TextView(getContext());
                text.setGravity(Gravity.CENTER_VERTICAL);
                text.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.playbutton, 0, 0, 0);
                text.setText("\t\t"+t.getName());
                text.setTextSize(15);

                text.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v="+key)));
                    }
                });

                card.addView(text);
                layout.addView(card);
            }
        }
    }

    public void createReviewTextView(ArrayList<Review> reviews){
        LinearLayout layout = (LinearLayout) view.findViewById(R.id.reviewView);

        if(reviews.size()==0){
            CardView card = createCardView();
            TextView text = new TextView(getContext());
            text.setPadding(30, 0, 35, 35);
            text.setText("No Review Available");
            card.addView(text);
            layout.addView(card);
        }
        else {
            for (Review r : reviews) {
                if (r != null) {
                    CardView card = createCardView();
                    TextView text = new TextView(getContext());
                    text.setText(Html.fromHtml("<B>" + r.getAuthor() + ":</B><br>" + r.getReview()));
                    text.setPadding(0, 0, 35, 35);
                    card.addView(text);
                    layout.addView(card);
                }
            }
        }
    }


    private class DataBaseTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            if (params[0].equals("write"))
            {
                dbOperationComplete = false;
                ContentValues values = new ContentValues();

                values.put(MovieEntry.ID,movie.getId());
                values.put(MovieEntry.POSTER_PATH,movie.getPosterPath());
                values.put(MovieEntry.TITLE,movie.getTitle());
                values.put(MovieEntry.OVERVIEW,movie.getOverView());
                values.put(MovieEntry.RELEASE_DATE,movie.getReleaseDate());
                values.put(MovieEntry.VOTE_AVERAGE,movie.getVoteAverage());
                values.put(MovieEntry.THUMBNAIL,movie.getThumbNail());
                getContext().getContentResolver().insert(MovieProvider.CONTENT_URI, values);

                for(Review r:reviewsList){
                    values.clear();
                    values.put(ReviewEntry.ID,movie.getId());
                    values.put(ReviewEntry.AUTHOR,r.getAuthor());
                    values.put(ReviewEntry.REVIEW,r.getReview());
                    getContext().getContentResolver().insert(MovieProvider.REVIEW_CONTENT_URI, values);
                }

                for(Trailer t:trailerList){
                    values.clear();
                    values.put(TrailerEntry.ID,movie.getId());
                    values.put(TrailerEntry.NAME,t.getName());
                    values.put(TrailerEntry.KEY,t.getKey());
                    getContext().getContentResolver().insert(MovieProvider.TRAILER_CONTENT_URI, values);
                }
                return "write";

            } else if (params[0].equals("remove"))
            {
                dbOperationComplete = false;
                getContext().getContentResolver().delete(MovieProvider.REVIEW_CONTENT_URI,ReviewEntry.ID+"="+movie.getId(),null);
                getContext().getContentResolver().delete(MovieProvider.TRAILER_CONTENT_URI,ReviewEntry.ID+"="+movie.getId(),null);
                getContext().getContentResolver().delete(MovieProvider.CONTENT_URI,ReviewEntry.ID+"="+movie.getId(),null);
                return "remove";
            }
            else if (params[0].equals("update"))
            {
                dbOperationComplete = false;
                String s1,s2;

                // update reviews
                String selection = ReviewEntry.ID + "="+ movie.getId();
                ContentValues values = new ContentValues();
                for(Review r:reviewsList){
                    values.put(ReviewEntry.ID,movie.getId());
                    values.put(ReviewEntry.AUTHOR,r.getAuthor());
                    values.put(ReviewEntry.REVIEW,r.getReview());
                    getContext().getContentResolver().update(MovieProvider.REVIEW_CONTENT_URI,values,selection,null);
                }

                // update trailers
                selection = TrailerEntry.ID + "="+ movie.getId();
                values = new ContentValues();
                for(Trailer t:trailerList){
                    values.put(TrailerEntry.ID,movie.getId());
                    values.put(TrailerEntry.NAME,t.getName());
                    values.put(TrailerEntry.KEY,t.getKey());
                    getContext().getContentResolver().update(MovieProvider.TRAILER_CONTENT_URI,values,selection,null);
                }
                return "update";
            }
            else if (params[0].equals("read"))
            {
                dbOperationComplete = false;
                String s1,s2;

                // read reviews
                String selection = ReviewEntry.ID + "="+ movie.getId();
                String[] projection = {ReviewEntry.AUTHOR,ReviewEntry.REVIEW};
                Cursor cursor = getContext().getContentResolver().query(MovieProvider.REVIEW_CONTENT_URI,projection,selection,null,null);
                if (cursor.moveToFirst()) {
                    do{
                        s1 = cursor.getString(cursor.getColumnIndex(ReviewEntry.AUTHOR));
                        s2 = cursor.getString(cursor.getColumnIndex(ReviewEntry.REVIEW));
                        reviewsList.add(new Review(s1,s2));
                    } while (cursor.moveToNext());
                }

                // read trailers
                selection = TrailerEntry.ID + "="+ movie.getId();
                projection = new String[]{TrailerEntry.NAME,TrailerEntry.KEY};
                cursor = getContext().getContentResolver().query(MovieProvider.TRAILER_CONTENT_URI,projection,selection,null,null);
                if (cursor.moveToFirst()) {
                    do{
                        s1 = cursor.getString(cursor.getColumnIndex(TrailerEntry.NAME));
                        s2 = cursor.getString(cursor.getColumnIndex(TrailerEntry.KEY));
                        trailerList.add(new Trailer(s1,s2));
                    } while (cursor.moveToNext());
                }
                cursor.close();
                return "read";
            }

            return null;
        }

        @Override
        protected void onPostExecute(String res) {
            dbOperationComplete = true;
            if(res.equals("read")) {
                createReviewTextView(reviewsList);
                createTrailerTextView(trailerList);
            }
        }
    }

    public class FetchReview extends AsyncTask<Void,Void,ArrayList<Review>> {
        private final String LOG_TAG = FetchReview.class.getSimpleName();

        @Override
        protected ArrayList<Review> doInBackground(Void... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String reviewJsonStr = null;

            try {
                Uri builtUri = Uri.parse(BASE_URL+movie.getId()+"/reviews").buildUpon()
                        .appendQueryParameter(API_KEY, Keys.API_KEY).build();

                URL url = new URL(builtUri.toString());

                // Create the request to themoviedb, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    reviewJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    reviewJsonStr = null;
                }
                reviewJsonStr = buffer.toString();
                Log.v(LOG_TAG, "movie JSON string: " + reviewJsonStr);
            } catch (IOException e) {
                // If the code didn't successfully get the data, there's no point in attempting
                // to parse it.
                reviewJsonStr = null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try{
                // returns list of Movie objects
                return getReviewData(reviewJsonStr);
            }
            catch (Exception e){
                Log.e(LOG_TAG,e.getMessage(),e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Review> reviewList) {
            createReviewTextView(reviewList);
        }

        private ArrayList<Review> getReviewData(String str) throws JSONException {
            final String RESULT = "results";
            final String CONTENT = "content";
            final String AUTHOR = "author";
            JSONObject jsonObject = new JSONObject(str);

            JSONArray reviewArray = jsonObject.getJSONArray(RESULT);
            ArrayList<Review> result = new ArrayList<>();
            reviewsList.clear();

            for (int i = 0; i < reviewArray.length(); i++) {
                JSONObject reviewObj = reviewArray.getJSONObject(i);
                Review review = new Review(reviewObj.getString(AUTHOR),reviewObj.getString(CONTENT));
                result.add(review);
                reviewsList.add(review);
            }
            return result;
        }

    }

    public class FetchTrailer extends AsyncTask<Void,Void,ArrayList<Trailer>> {
        private final String LOG_TAG = FetchReview.class.getSimpleName();

        @Override
        protected ArrayList<Trailer> doInBackground(Void... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String trailerJsonStr = null;

            try {
                Uri builtUri = Uri.parse(BASE_URL+movie.getId()+"/videos").buildUpon()
                        .appendQueryParameter(API_KEY, Keys.API_KEY).build();

                URL url = new URL(builtUri.toString());

                // Create the request to themoviedb, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    trailerJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    trailerJsonStr = null;
                }
                trailerJsonStr = buffer.toString();
                Log.v(LOG_TAG, "movie JSON string: " + trailerJsonStr);
            } catch (IOException e) {
                // If the code didn't successfully get the data, there's no point in attempting
                // to parse it.
                trailerJsonStr = null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try{
                // returns list of Movie objects
                return getTrailerData(trailerJsonStr);
            }
            catch (Exception e){
                Log.e(LOG_TAG,e.getMessage(),e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Trailer> trailerList) {
            createTrailerTextView(trailerList);
        }

        private ArrayList<Trailer> getTrailerData(String str) throws JSONException {
            final String RESULT = "results";
            final String NAME = "name";
            final String KEY = "key";
            final String TYPE = "type";
            JSONObject jsonObject = new JSONObject(str);

            JSONArray trailerArray = jsonObject.getJSONArray(RESULT);
            ArrayList<Trailer> result = new ArrayList<>();
            trailerList.clear();

            for (int i = 0; i < trailerArray.length(); i++) {
                JSONObject trailerObj = trailerArray.getJSONObject(i);
                if(trailerObj.get(TYPE).equals("Trailer")) {
                    Trailer trailer = new Trailer(trailerObj.getString(NAME), trailerObj.getString(KEY));
                    result.add(trailer);
                    trailerList.add(trailer);
                }
            }
            return result;
        }

    }

}
