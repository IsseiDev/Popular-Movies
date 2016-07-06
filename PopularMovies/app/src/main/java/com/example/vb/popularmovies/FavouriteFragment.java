package com.example.vb.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.vb.popularmovies.data.MovieContract.MovieEntry;
import com.example.vb.popularmovies.data.MovieProvider;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class FavouriteFragment extends Fragment {

    ArrayList<Movie> movieList;
    ListAdapter adapter;
    Movie selectedMovie;
    MovieDetailsFragment detailsFragment;
    private static final String DETAILFRAGMENT_TAG = "DFTAG";

    public FavouriteFragment() {
        movieList = new ArrayList<>();
    }

    @Override
    public void onResume(){
        super.onResume();
        movieList.clear();
        getFavouriteMovies();
        adapter.notifyDataSetChanged();
        if(Favourite.mTwoPane)
            updateDetailsFragment(movieList.get(0));

    }

    // add a new fragment to the details fragment container
    private void updateDetailsFragment(Movie m){
        detailsFragment = (MovieDetailsFragment)getActivity().getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
        if(detailsFragment!=null) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(detailsFragment).commit();
        }

        MovieDetailsFragment f = MovieDetailsFragment.newInstance(m,true,this);
        getActivity().getSupportFragmentManager().beginTransaction().add(R.id.details_frame_container,f,DETAILFRAGMENT_TAG).disallowAddToBackStack().commit();
        detailsFragment = (MovieDetailsFragment) getActivity().getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);

    }

    // addMovie is false when movie has to be deleted
    public void updateFavouriteMovieList(Movie m,boolean addMovie){
        if(addMovie)
            movieList.add(m);
        else
            movieList.remove(m);
        adapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favourite, container, false);
        ListView listView = (ListView) view.findViewById(R.id.favourite_list_view);
        adapter = new ListAdapter(getContext(),movieList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Movie m = movieList.get(position);
                if(Favourite.mTwoPane){
                    updateDetailsFragment(m);
                }
                else {
                    Intent intent = new Intent(getActivity(), MovieDetails.class).putExtra("movieobj", m);
                    intent.putExtra("favourites", true);
                    startActivity(intent);
                }
            }
        });

        return view;
    }

    public void getFavouriteMovies(){
        String[] projection = {MovieEntry.ID,MovieEntry.POSTER_PATH,MovieEntry.TITLE,MovieEntry.OVERVIEW,MovieEntry.RELEASE_DATE,MovieEntry.VOTE_AVERAGE,MovieEntry.THUMBNAIL};
        Cursor cursor = getContext().getContentResolver().query(MovieProvider.CONTENT_URI,projection,null,null,null);

        int id;
        String poster_path,title,overview,release_date,vote_average,thumbnail;

        if(cursor!=null && cursor.moveToFirst()){
            do {
                id = cursor.getInt(cursor.getColumnIndex(MovieEntry.ID));
                poster_path = cursor.getString(cursor.getColumnIndex(MovieEntry.POSTER_PATH));
                title = cursor.getString(cursor.getColumnIndex(MovieEntry.TITLE));
                overview = cursor.getString(cursor.getColumnIndex(MovieEntry.OVERVIEW));
                release_date = cursor.getString(cursor.getColumnIndex(MovieEntry.RELEASE_DATE));
                vote_average = cursor.getString(cursor.getColumnIndex(MovieEntry.VOTE_AVERAGE));
                thumbnail = cursor.getString(cursor.getColumnIndex(MovieEntry.THUMBNAIL));
                movieList.add(new Movie(id,poster_path,title,overview,release_date,vote_average,thumbnail));

            } while(cursor.moveToNext());
        }
        cursor.close();
    }

    public class ListAdapter extends ArrayAdapter<Movie> {
        public ListAdapter(Context context, ArrayList<Movie> movies) {
            super(context, 0, movies);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            Movie movie = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.favourite_movie, parent, false);
            }

            ImageView image = (ImageView) convertView.findViewById(R.id.favourite_image);
            TextView movieName = (TextView) convertView.findViewById(R.id.favourite_movie_name);
            String text = "<B><H1>"+movie.getTitle()+"</H1></B><BR><BR>Average Vote:  "+movie.getVoteAverage()+"/10<BR>"+
                          "Release Date:  "+movie.getReleaseDate();

            Picasso.with(getContext()).load(movie.getPosterPath()).into(image);
            movieName.setText(Html.fromHtml(text));

            return convertView;
        }
    }


}

