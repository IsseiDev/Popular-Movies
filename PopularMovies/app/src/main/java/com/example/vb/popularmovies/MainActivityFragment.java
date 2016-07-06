package com.example.vb.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

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
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    ImageAdapter imgAdapter ;
    List<Movie> movies;
    String sort_order = "popular";
    Movie selectedMovie;
    MovieDetailsFragment detailsFragment;

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Bundle bundle = getActivity().getIntent().getExtras();
        if(bundle!=null && bundle.containsKey("sort_order"))
            sort_order = bundle.getString("sort_order");

        //add this line in order for this fragment to handle menu events
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume(){
        super.onResume();
        detailsFragment = (MovieDetailsFragment)getActivity().getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        outState.putParcelableArrayList("movies",(ArrayList<Movie>)movies);
        outState.putString("sort_order",sort_order);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if (id == R.id.action_popular) {
            sort_order = "popular";
            getActivity().setTitle(R.string.app_name);
            updateMovies();
        }

        if (id == R.id.action_toprated) {
            sort_order = "top_rated";
            getActivity().setTitle(R.string.app_name2);
            updateMovies();
        }

        if(id == R.id.action_favourites){
            Intent intent = new Intent(getContext(),Favourite.class);
            startActivity(intent);
        }

        return true;
    }

    private boolean checkNetworkConnection() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(getContext().CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    // add a new updated fragment to the details fragment container
    private void updateDetailsFragment(Movie m){
        detailsFragment = (MovieDetailsFragment)getActivity().getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
        if(detailsFragment!=null) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(detailsFragment).commit();
        }

        MovieDetailsFragment f = MovieDetailsFragment.newInstance(m,false,null);
        getActivity().getSupportFragmentManager().beginTransaction().add(R.id.details_frame_container,f,DETAILFRAGMENT_TAG).disallowAddToBackStack().commit();
        detailsFragment = (MovieDetailsFragment) getActivity().getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);
        // Grid View
        GridView gridview = (GridView) view.findViewById(R.id.imageGrid);

        if(savedInstanceState==null || !savedInstanceState.containsKey("movies")){
            movies = new ArrayList<>();
            updateMovies();
        }
        else{
            movies = savedInstanceState.getParcelableArrayList("movies");
            sort_order = savedInstanceState.getString("sort_order");
            if(sort_order.equals("popular"))
                getActivity().setTitle(R.string.app_name);
            else if(sort_order.equals("top_rated"))
                getActivity().setTitle(R.string.app_name2);
        }

        imgAdapter = new ImageAdapter(getActivity(),movies);
        gridview.setAdapter(imgAdapter);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                if(MainActivity.mTwoPane) {
                    selectedMovie = movies.get(position);
                    updateDetailsFragment(selectedMovie); // update UI of the details fragment
                }
                else {
                    Intent intent = new Intent(getActivity(), MovieDetails.class).putExtra("movieobj", movies.get(position));
                    startActivity(intent);
                }
            }
        });

        return view;
    }

    private void updateMovies(){
        if(checkNetworkConnection()) {
            movies.clear();  // remove old movie objects stored
            FetchMovie movieTask = new FetchMovie();
            movieTask.execute(sort_order);
        }
        else{
            Toast.makeText(getContext(),"Network Problem",Toast.LENGTH_LONG).show();
            movies.clear();
            // clear all images if there is no internet connection when updating movie list
            if(imgAdapter!=null) {
                imgAdapter.clear();
                imgAdapter.notifyDataSetChanged();
            }
        }
    }

    public class FetchMovie extends AsyncTask<String,Void,String[]> {
        private final String LOG_TAG = FetchMovie.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String movieJsonStr = null;

            try {

                final String BASE_URL = "http://api.themoviedb.org/3/movie/"+params[0]+"?";
                final String API_KEY = "api_key";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
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
                    movieJsonStr = null;
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
                    movieJsonStr = null;
                }
                movieJsonStr = buffer.toString();
                Log.v(LOG_TAG, "movie JSON string: " + movieJsonStr);
            } catch (IOException e) {
                // If the code didn't successfully get the data, there's no point in attempting
                // to parse it.
                movieJsonStr = null;
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
                return getMovieData(movieJsonStr);
            }
            catch (Exception e){
                Log.e(LOG_TAG,e.getMessage(),e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] movieList) {
            if(movieList != null) {
                imgAdapter.setImageUrls(movieList);

                // for tablets - load layout and display details of top movie on the right side
                // this is done after populating movie array list with data
                if (MainActivity.mTwoPane) {
                    updateDetailsFragment(movies.get(0));
                }
            }
        }
    }

    // retrieves movie data from JSON string
    public String[] getMovieData(String JsonStr) throws JSONException {

        final String RESULT = "results";
        final String ID = "id";
        final String TITLE = "title";
        final String POSTERPATH = "poster_path";
        final String OVERVIEW = "overview";
        final String RELEASE = "release_date";
        final String VOTE_AVERAGE = "vote_average";
        final String THUMBNAIL = "backdrop_path";
        final String BASE_URL = "http://image.tmdb.org/t/p/w342/";

        String url;
        String overView;
        String title;
        String release;
        String rating;
        String thumbNail;

        int id;

        JSONObject movieJson = new JSONObject(JsonStr);
        JSONArray movieArray = movieJson.getJSONArray(RESULT);
        JSONObject movieObj;

        String[] urlList = new String[movieArray.length()];

        for(int i=0;i<movieArray.length();i++)
        {
            movieObj = movieArray.getJSONObject(i);
            url = BASE_URL + movieObj.getString(POSTERPATH);
            overView = movieObj.getString(OVERVIEW);
            title = movieObj.getString(TITLE);
            release = movieObj.getString(RELEASE);
            rating = movieObj.getString(VOTE_AVERAGE);
            thumbNail = BASE_URL + movieObj.getString(THUMBNAIL);
            id = movieObj.getInt(ID);

            movies.add(new Movie(id,url,title,overView,release,rating,thumbNail));
            urlList[i] = url;
        }
        return urlList;
    }

    // adapter for images
    public class ImageAdapter extends BaseAdapter {
        private Context context;
        private List<String> image_url = new ArrayList<String>();

        public ImageAdapter(Context c, List<Movie> movies) {
            context = c;
            for(int i=0;i<movies.size();i++)
                image_url.add(movies.get(i).getPosterPath());
        }

        public int getCount() {
            return image_url.size();
        }

        public void clear(){ image_url.clear();}

        public Object getItem(int position) {
            return image_url.get(position);
        }

        public long getItemId(int position) {
            return 0;
        }

        public void setImageUrls(String[] movieList){
            image_url.clear();
            for(int i=0;i<movieList.length;i++) {
                image_url.add(movieList[i]);
            }
            notifyDataSetChanged();
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(context);
            } else {
                imageView = (ImageView) convertView;
            }
            // to preserve aspect ratio of image
            imageView.setAdjustViewBounds(true);
            Picasso.with(context).load(image_url.get(position)).into(imageView);
            return imageView;
        }

    }

}
