package com.example.moviesapp_part2_yassinejaabouk;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WatchedMoviesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WatchedMoviesAdapter adapter;
    private FirebaseAuth mAuth;
    private List<MyMovieData> watchedList = new ArrayList<>();
    private List<Boolean> favoritesList = new ArrayList<>();
    private ProgressBar progressBar;
    private FloatingActionButton aiRecommendBtn;

    private static final String BASE_URL = "http://10.0.2.2:8000";
    private static final String GEMINI_API_KEY = "AIzaSyCwexL310B1d4UECYYfC2dpeLQOEMzkN28"; // ✅ updated key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watched_movies);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Watched Movies");
        }

        mAuth = FirebaseAuth.getInstance();
        recyclerView = findViewById(R.id.recyclerViewWatched);
        progressBar = findViewById(R.id.progressBarWatched);
        aiRecommendBtn = findViewById(R.id.aiRecommendBtn);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadWatchedMovies();

        if (aiRecommendBtn != null) {
            aiRecommendBtn.setOnClickListener(v -> getAIRecommendations());
        }
    }

    private void getAIRecommendations() {
        if (watchedList.isEmpty()) {
            Toast.makeText(this, "Watch some movies first!", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder movieTitles = new StringBuilder();
        for (MyMovieData movie : watchedList) {
            movieTitles.append("- ")
                    .append(movie.getMovieName())
                    .append(" (Rating: ")
                    .append(movie.getRating())
                    .append(")\n");
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (aiRecommendBtn != null) aiRecommendBtn.setEnabled(false);

        String prompt = "I have watched these movies:\n" + movieTitles.toString() +
                "\nBased on my taste, recommend 5 similar movies I would enjoy. " +
                "For each movie include: title, year, and one sentence on why I'd like it.";

        // Run network call on background thread
        new Thread(() -> {
            try {
                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY;

                JSONObject requestBody = new JSONObject()
                        .put("contents", new org.json.JSONArray()
                                .put(new JSONObject()
                                        .put("parts", new org.json.JSONArray()
                                                .put(new JSONObject()
                                                        .put("text", prompt)))));

                java.net.URL url = new java.net.URL(apiUrl);
                javax.net.ssl.HttpsURLConnection conn = (javax.net.ssl.HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                // Write request
                java.io.OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();

                // Read response
                java.io.InputStream is = responseCode == 200 ? conn.getInputStream() : conn.getErrorStream();
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseStr = sb.toString();
                Log.d("GEMINI_RESPONSE", responseStr);

                if (responseCode == 200) {
                    JSONObject jsonResponse = new JSONObject(responseStr);
                    String text = jsonResponse
                            .getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (aiRecommendBtn != null) aiRecommendBtn.setEnabled(true);
                        new AlertDialog.Builder(WatchedMoviesActivity.this)
                                .setTitle("🎬 AI Movie Recommendations")
                                .setMessage(text)
                                .setPositiveButton("Awesome!", null)
                                .show();
                    });
                } else {
                    String finalError = responseStr;
                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (aiRecommendBtn != null) aiRecommendBtn.setEnabled(true);
                        new AlertDialog.Builder(WatchedMoviesActivity.this)
                                .setTitle("AI Error " + responseCode)
                                .setMessage(finalError)
                                .setPositiveButton("OK", null)
                                .show();
                    });
                }

            } catch (Exception e) {
                Log.e("GEMINI_ERROR", e.toString());
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (aiRecommendBtn != null) aiRecommendBtn.setEnabled(true);
                    new AlertDialog.Builder(WatchedMoviesActivity.this)
                            .setTitle("AI Error")
                            .setMessage(e.toString())
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadWatchedMovies() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String url = BASE_URL + "/watched/" + user.getUid();

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    watchedList.clear();
                    favoritesList.clear();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            MyMovieData movie = new MyMovieData(
                                    obj.getInt("id"),
                                    obj.getString("title"),
                                    obj.getString("date"),
                                    obj.getString("poster"),
                                    obj.getDouble("rating")
                            );
                            watchedList.add(movie);
                            favoritesList.add(obj.getBoolean("isFavourite"));
                        }
                        adapter = new WatchedMoviesAdapter(watchedList, favoritesList, this, this::updateFavoriteStatus);
                        recyclerView.setAdapter(adapter);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing server data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Connect your FastAPI server first!", Toast.LENGTH_LONG).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void updateFavoriteStatus(int movieId, boolean isFav) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String url = BASE_URL + "/watched/" + user.getUid() + "/" + movieId + "?is_fav=" + isFav;

        StringRequest request = new StringRequest(Request.Method.PATCH, url,
                response -> {
                    String msg = isFav ? "Added to Favorites" : "Removed from Favorites";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }
}