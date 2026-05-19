package com.example.moviesapp_part2_yassinejaabouk;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MyMovieAdapter extends RecyclerView.Adapter<MyMovieAdapter.ViewHolder> implements Filterable {

    private List<MyMovieData> originalMovieData;
    private List<MyMovieData> filteredMovieData;
    private Context context;

    public MyMovieAdapter(MyMovieData[] myMovieData, Context context) {
        this.originalMovieData = new ArrayList<>(Arrays.asList(myMovieData));
        this.filteredMovieData = new ArrayList<>(Arrays.asList(myMovieData));
        this.context = context;
    }

    public void addMovies(MyMovieData[] newMovies) {
        int startPos = filteredMovieData.size();
        List<MyMovieData> newList = Arrays.asList(newMovies);
        this.originalMovieData.addAll(newList);
        this.filteredMovieData.addAll(newList);
        notifyItemRangeInserted(startPos, newMovies.length);
    }

    public void clearMovies() {
        int size = filteredMovieData.size();
        this.originalMovieData.clear();
        this.filteredMovieData.clear();
        notifyItemRangeRemoved(0, size);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_movie_item_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final MyMovieData movieData = filteredMovieData.get(position);

        holder.textViewName.setText(movieData.getMovieName());
        holder.textViewDate.setText(movieData.getMovieDate());
        holder.textViewRating.setText(String.format(Locale.getDefault(), "%.1f", movieData.getRating()));

        Glide.with(context)
                .load("https://image.tmdb.org/t/p/w500" + movieData.getMovieImage())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.movieImage);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, MovieDetailActivity.class);
                intent.putExtra("movieId", movieData.getMovieId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredMovieData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView movieImage;
        TextView textViewName;
        TextView textViewDate;
        TextView textViewRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            movieImage = itemView.findViewById(R.id.imageview);
            textViewName = itemView.findViewById(R.id.textName);
            textViewDate = itemView.findViewById(R.id.textdate);
            textViewRating = itemView.findViewById(R.id.textRating);
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<MyMovieData> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(originalMovieData);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (MyMovieData movie : originalMovieData) {
                        if (movie.getMovieName().toLowerCase().contains(filterPattern)) {
                            filteredList.add(movie);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredMovieData.clear();
                filteredMovieData.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
    }
}