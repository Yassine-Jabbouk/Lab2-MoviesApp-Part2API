package com.example.moviesapp_part2_yassinejaabouk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import java.util.Locale;

public class WatchedMoviesAdapter extends RecyclerView.Adapter<WatchedMoviesAdapter.ViewHolder> {

    private List<MyMovieData> movies;
    private List<Boolean> favorites;
    private Context context;
    private OnFavoriteClickListener listener;

    public interface OnFavoriteClickListener {
        void onFavoriteClick(int movieId, boolean isFavorite);
    }

    public WatchedMoviesAdapter(List<MyMovieData> movies, List<Boolean> favorites, Context context, OnFavoriteClickListener listener) {
        this.movies = movies;
        this.favorites = favorites;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_watched_movie, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyMovieData movie = movies.get(position);
        boolean isFav = favorites.get(position);

        holder.title.setText(movie.getMovieName());
        holder.date.setText(movie.getMovieDate());
        holder.rating.setText(String.format(Locale.getDefault(), "%.1f", movie.getRating()));

        Glide.with(context)
                .load("https://image.tmdb.org/t/p/w500" + movie.getMovieImage())
                .into(holder.poster);

        holder.favIcon.setImageResource(isFav ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        
        holder.favIcon.setOnClickListener(v -> {
            boolean newFavStatus = !favorites.get(position);
            favorites.set(position, newFavStatus);
            holder.favIcon.setImageResource(newFavStatus ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
            listener.onFavoriteClick(movie.getMovieId(), newFavStatus);
        });
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView poster, favIcon;
        TextView title, date, rating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.imageview);
            title = itemView.findViewById(R.id.textName);
            date = itemView.findViewById(R.id.textdate);
            rating = itemView.findViewById(R.id.textRating);
            favIcon = itemView.findViewById(R.id.favIcon);
        }
    }
}