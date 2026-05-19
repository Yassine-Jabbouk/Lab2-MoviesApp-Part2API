package com.example.moviesapp_part2_yassinejaabouk;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;

public class VibeSearchActivity extends AppCompatActivity {

    private TextInputEditText editTextVibe;
    private MaterialButton btnSearchVibe;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private MyMovieAdapter adapter;
    private RequestQueue requestQueue;

    private static final String TMDB_API_KEY = BuildConfig.TMDB_API_KEY;
    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String BASE_URL = "https://api.themoviedb.org/3/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vibe_search);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI Vibe Explorer");
        }

        editTextVibe = findViewById(R.id.editTextVibe);
        btnSearchVibe = findViewById(R.id.btnSearchVibe);
        recyclerView = findViewById(R.id.recyclerViewVibe);
        progressBar = findViewById(R.id.progressBarVibe);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyMovieAdapter(new MyMovieData[0], this);
        recyclerView.setAdapter(adapter);

        requestQueue = Volley.newRequestQueue(this);

        btnSearchVibe.setOnClickListener(v -> {
            hideKeyboard();
            performVibeSearch();
        });
        String vibeQuery = getIntent().getStringExtra("vibe_query");
        if (vibeQuery != null && !vibeQuery.isEmpty()) {
            editTextVibe.setText(vibeQuery);
            performVibeSearch();
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void performVibeSearch() {
        String query = editTextVibe.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Please describe the vibe of the movie you want!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSearchVibe.setEnabled(false);
        tvEmptyState.setVisibility(View.GONE);
        adapter.clearMovies();

        String prompt = "You are an expert movie librarian. I will give you a mood or description. " +
                "Return ONLY one single English word that best describes this movie vibe for a database keyword search. " +
                "Examples: 'romance', 'adventure', 'thriller', 'comedy', 'heartbreak'. " +
                "No explanation, just one word. Query: " + query;

        // Use direct HTTPS call instead of Gemini SDK
        new Thread(() -> {
            try {
                JSONObject requestBody = new JSONObject()
                        .put("contents", new JSONArray()
                                .put(new JSONObject()
                                        .put("parts", new JSONArray()
                                                .put(new JSONObject()
                                                        .put("text", prompt)))));

                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                java.io.InputStream is = responseCode == 200 ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseStr = sb.toString();
                Log.d("GEMINI_RESPONSE", responseStr);

                if (responseCode == 200) {
                    JSONObject jsonResponse = new JSONObject(responseStr);
                    String keywords = jsonResponse
                            .getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim();

                    Log.d("VibeSearch", "AI Keywords: " + keywords);
                    runOnUiThread(() -> fetchMoviesFromTMDB(keywords));

                } else {
                    Log.e("GEMINI_ERROR", responseStr);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSearchVibe.setEnabled(true);
                        tvEmptyState.setText("AI Error: " + responseStr);
                        tvEmptyState.setVisibility(View.VISIBLE);
                    });
                }

            } catch (Exception e) {
                Log.e("GEMINI_ERROR", e.toString());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSearchVibe.setEnabled(true);
                    tvEmptyState.setText("Error: " + e.getMessage());
                    tvEmptyState.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void fetchMoviesFromTMDB(String keywords) {
        keywords = keywords.replaceAll("-", " ").replaceAll("\n", " ").trim();
        Log.d("TMDB_DEBUG", "Keywords: " + keywords);

        // Use discover with keywords API instead of search
        String keywordSearchUrl = "https://api.themoviedb.org/3/search/keyword?api_key=" + TMDB_API_KEY + "&query=" + keywords.split(" ")[0];

        final String finalKeywords = keywords;
        JsonObjectRequest keywordRequest = new JsonObjectRequest(Request.Method.GET, keywordSearchUrl, null,
                response -> {
                    try {
                        JSONArray keywordResults = response.getJSONArray("results");
                        if (keywordResults.length() > 0) {
                            // Build keyword IDs string
                            StringBuilder keywordIds = new StringBuilder();
                            for (int i = 0; i < Math.min(3, keywordResults.length()); i++) {
                                if (i > 0) keywordIds.append("|");
                                keywordIds.append(keywordResults.getJSONObject(i).getInt("id"));
                            }
                            // Discover movies with these keywords
                            String discoverUrl = "https://api.themoviedb.org/3/discover/movie?api_key=" + TMDB_API_KEY
                                    + "&with_keywords=" + keywordIds
                                    + "&sort_by=popularity.desc";
                            fetchDiscoverMovies(discoverUrl);
                        } else {
                            // Fallback: search each word individually
                            String searchUrl = "https://api.themoviedb.org/3/search/movie?api_key=" + TMDB_API_KEY
                                    + "&query=" + finalKeywords.split(" ")[0];
                            fetchDiscoverMovies(searchUrl);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Log.e("TMDB_ERROR", error.toString());
                    Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnSearchVibe.setEnabled(true);
                });

        requestQueue.add(keywordRequest);
    }

    private void fetchDiscoverMovies(String url) {
        Log.d("TMDB_DISCOVER", "URL: " + url);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    btnSearchVibe.setEnabled(true);
                    try {
                        JSONArray results = response.getJSONArray("results");
                        Log.d("TMDB_DISCOVER", "Results: " + results.length());
                        if (results.length() == 0) {
                            tvEmptyState.setText("No movies found for this vibe. Try describing it differently!");
                            tvEmptyState.setVisibility(View.VISIBLE);
                            return;
                        }
                        List<MyMovieData> movieList = new ArrayList<>();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject m = results.getJSONObject(i);
                            movieList.add(new MyMovieData(
                                    m.getInt("id"),
                                    m.getString("title"),
                                    m.optString("release_date", "N/A"),
                                    m.optString("poster_path", ""),
                                    m.optDouble("vote_average", 0.0)
                            ));
                        }
                        adapter.addMovies(movieList.toArray(new MyMovieData[0]));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        tvEmptyState.setText("Something went wrong loading movies.");
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    btnSearchVibe.setEnabled(true);
                    Log.e("TMDB_ERROR", error.toString());
                    Toast.makeText(this, "Network error searching TMDB", Toast.LENGTH_SHORT).show();
                });
        requestQueue.add(request);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}