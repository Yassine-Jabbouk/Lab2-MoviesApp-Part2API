package com.example.moviesapp_part2_yassinejaabouk;

// REMOVED: import static android.os.Build.VERSION_CODES.R;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WatchedMoviesAdapter adapter;
    private FirebaseAuth mAuth;
    private List<MyMovieData> favoriteList = new ArrayList<>();
    private List<Boolean> favoritesStatusList = new ArrayList<>();

    private static final String BASE_URL = "http://10.0.2.2:8000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Favorites");
        }

        mAuth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.recyclerViewFavorites);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadFavoriteMovies();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadFavoriteMovies() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String url = BASE_URL + "/watched/" + user.getUid();

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    favoriteList.clear();
                    favoritesStatusList.clear();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            if (obj.getBoolean("isFavourite")) {
                                MyMovieData movie = new MyMovieData(
                                        obj.getInt("id"),
                                        obj.getString("title"),
                                        obj.getString("date"),
                                        obj.getString("poster"),
                                        obj.getDouble("rating")
                                );
                                favoriteList.add(movie);
                                favoritesStatusList.add(true);
                            }
                        }
                        adapter = new WatchedMoviesAdapter(favoriteList, favoritesStatusList, this, (movieId, isFav) -> {
                            updateFavoriteStatus(movieId, isFav);
                        });
                        recyclerView.setAdapter(adapter);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Error connecting to backend", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void updateFavoriteStatus(int movieId, boolean isFav) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String url = BASE_URL + "/watched/" + user.getUid() + "/" + movieId + "?is_fav=" + isFav;

        StringRequest request = new StringRequest(Request.Method.PATCH, url,
                response -> {
                    if (!isFav) {
                        loadFavoriteMovies();
                    }
                },
                error -> Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }
}