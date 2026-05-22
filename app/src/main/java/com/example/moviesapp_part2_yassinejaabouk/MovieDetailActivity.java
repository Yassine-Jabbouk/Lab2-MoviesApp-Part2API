package com.example.moviesapp_part2_yassinejaabouk;

import static android.content.ContentValues.TAG;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.io.OutputStream;


public class MovieDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TMDB_API_KEY = BuildConfig.TMDB_API_KEY;
    private static final String GOOGLE_API_KEY = BuildConfig.GOOGLE_MAP_KEY;
    private static final String BASE_URL = "http://10.0.2.2:8000";

    private SupportMapFragment mapFragment;
    private TextView descriptionTextView;
    private TextView nameTextView;
    private ImageView img;
    private Button playButton, watchedButton;
    private String trailerKey;
    private String imdbId;
    private RequestQueue requestQueue;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    private FirebaseAuth mAuth;
    private int currentMovieId;
    private String currentMovieTitle, currentMoviePoster, currentMovieDate;
    private double currentMovieRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        Toolbar toolbar = findViewById(R.id.detailToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Movie Details");
        }

        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        descriptionTextView = findViewById(R.id.Details);
        nameTextView = findViewById(R.id.textName);
        img = findViewById(R.id.imageview);
        playButton = findViewById(R.id.playButton);
        watchedButton = findViewById(R.id.watchedButton);
        requestQueue = buildRequestQueue();



        currentMovieId = getIntent().getIntExtra("movieId", -1);
        if (currentMovieId != -1) {
            fetchMovieDetails(currentMovieId);
            checkIfWatched();
        }

        playButton.setOnClickListener(v -> playTrailer());
        watchedButton.setOnClickListener(v -> markAsWatched());

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void checkIfWatched() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String url = BASE_URL + "/watched/" + user.getUid();

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            if (response.getJSONObject(i).getInt("id") == currentMovieId) {
                                watchedButton.setText("Watched \u2713");
                                watchedButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
                                watchedButton.setEnabled(false);
                                break;
                            }
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }, error -> Log.e(TAG, "Error checking watched status"));

        requestQueue.add(request);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchMovieDetails(int movieId) {
        watchedButton.setEnabled(false);

        String movieDetailsUrl = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + TMDB_API_KEY;
        String movieVideosUrl = "https://api.themoviedb.org/3/movie/" + movieId + "/videos?api_key=" + TMDB_API_KEY;

        new Thread(() -> {
            try {
                // Fetch movie details
                String detailsResponse = fetchUrl(movieDetailsUrl);
                JSONObject details = new JSONObject(detailsResponse);

                currentMovieTitle = details.getString("title");
                currentMoviePoster = details.optString("poster_path", "");
                currentMovieDate = details.optString("release_date", "N/A");
                currentMovieRating = details.optDouble("vote_average", 0.0);
                imdbId = details.optString("imdb_id", null);

                // Fetch videos
                String videosResponse = fetchUrl(movieVideosUrl);
                JSONObject videosJson = new JSONObject(videosResponse);
                JSONArray results = videosJson.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject video = results.getJSONObject(i);
                    if (video.getString("type").equals("Trailer") && video.getString("site").equals("YouTube")) {
                        trailerKey = video.getString("key");
                        break;
                    }
                }

                runOnUiThread(() -> {
                    nameTextView.setText(currentMovieTitle);
                    descriptionTextView.setText(details.optString("overview", "No description available"));
                    Glide.with(this).load("https://image.tmdb.org/t/p/w500" + currentMoviePoster).into(img);
                    watchedButton.setEnabled(true);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error fetching movie details: " + e.toString());
                runOnUiThread(() ->
                        Toast.makeText(this, "Error loading movie details: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    // Helper method to fetch a URL and return response as String
    private String fetchUrl(String urlString) throws Exception {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    okhttp3.Request original = chain.request();
                    okhttp3.Request request = original.newBuilder()
                            .header("Accept-Encoding", "identity")
                            .build();
                    return chain.proceed(request);
                })
                .build();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(urlString)
                .build();

        okhttp3.Response response = client.newCall(request).execute();

        // Read raw bytes
        byte[] bytes = response.body().bytes();

        // Try to decompress as gzip
        try {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes);
            java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(bais);
            byte[] decompressed = new byte[65536];
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            int n;
            while ((n = gzip.read(decompressed)) != -1) {
                baos.write(decompressed, 0, n);
            }
            gzip.close();
            String result = baos.toString("UTF-8");
            Log.d("FETCH_URL", "Decompressed: " + result.substring(0, Math.min(200, result.length())));
            if (!response.isSuccessful()) throw new Exception("HTTP " + response.code());
            return result;
        } catch (java.util.zip.ZipException e) {
            // Not gzip, return as plain text
            String result = new String(bytes, "UTF-8");
            Log.d("FETCH_URL", "Plain: " + result.substring(0, Math.min(200, result.length())));
            if (!response.isSuccessful()) throw new Exception("HTTP " + response.code());
            return result;
        }
    }

    private void markAsWatched() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Safety check
        if (currentMovieTitle == null || currentMoviePoster == null || currentMovieDate == null) {
            Toast.makeText(this, "Movie details still loading, please wait", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject movieJson = new JSONObject();
        try {
            movieJson.put("id", currentMovieId);
            movieJson.put("title", currentMovieTitle);
            movieJson.put("poster", currentMoviePoster);
            movieJson.put("date", currentMovieDate);
            movieJson.put("rating", currentMovieRating);
            movieJson.put("isFavourite", false);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        String url = BASE_URL + "/watched/" + user.getUid();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, movieJson,
                response -> {
                    watchedButton.setText("Watched \u2713");
                    watchedButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
                    watchedButton.setEnabled(false);
                    Toast.makeText(this, "Added to Watched list", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    String errorMsg = "Make sure server is running";
                    if (error.networkResponse != null) {
                        errorMsg = "HTTP " + error.networkResponse.statusCode + ": "
                                + new String(error.networkResponse.data);
                    }
                    Log.e(TAG, "Watched error: " + errorMsg);
                    Toast.makeText(this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                }
        );

        requestQueue.add(request);
    }
    private RequestQueue buildRequestQueue() {
        com.android.volley.toolbox.HurlStack stack = new com.android.volley.toolbox.HurlStack() {
            @Override
            protected java.net.HttpURLConnection createConnection(java.net.URL url) throws java.io.IOException {
                java.net.HttpURLConnection conn = super.createConnection(url);
                conn.setRequestProperty("Accept-Encoding", "identity");
                return conn;
            }
        };
        return com.android.volley.toolbox.Volley.newRequestQueue(this, stack);
    }

    private void playTrailer() {
        if (trailerKey != null && !trailerKey.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + trailerKey));
            startActivity(intent);
        } else if (imdbId != null && !imdbId.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.imdb.com/title/" + imdbId));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Trailer not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        mMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13));
                fetchNearbyCinemas(location.getLatitude(), location.getLongitude());
            } else {
                LatLng defaultLoc = new LatLng(33.596460, -7.615480);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 13));
                fetchNearbyCinemas(defaultLoc.latitude, defaultLoc.longitude);
            }
        });
    }

    private void fetchNearbyCinemas(double lat, double lng) {
        new Thread(() -> {
            try {
                // Use Places API (New) - Text Search
                String urlString = "https://places.googleapis.com/v1/places:searchNearby";

                JSONObject requestBody = new JSONObject();
                requestBody.put("languageCode", "en");

                JSONArray includedTypes = new JSONArray();
                includedTypes.put("movie_theater");
                requestBody.put("includedTypes", includedTypes);

                requestBody.put("maxResultCount", 10);

                JSONObject locationRestriction = new JSONObject();
                JSONObject circle = new JSONObject();
                JSONObject center = new JSONObject();
                center.put("latitude", lat);
                center.put("longitude", lng);
                circle.put("center", center);
                circle.put("radius", 5000.0);
                locationRestriction.put("circle", circle);
                requestBody.put("locationRestriction", locationRestriction);

                java.net.URL url = new java.net.URL(urlString);
                javax.net.ssl.HttpsURLConnection conn = (javax.net.ssl.HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("X-Goog-Api-Key", GOOGLE_API_KEY);
                conn.setRequestProperty("X-Goog-FieldMask", "places.displayName,places.location,places.formattedAddress");
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                java.io.InputStream is = responseCode == 200 ? conn.getInputStream() : conn.getErrorStream();
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                Log.d("CINEMA_NEW", "Code: " + responseCode + " Body: " + sb.toString());

                if (responseCode == 200) {
                    JSONObject response = new JSONObject(sb.toString());
                    JSONArray places = response.optJSONArray("places");

                    if (places == null || places.length() == 0) {
                        Log.d("CINEMA_NEW", "No cinemas found");
                        return;
                    }

                    runOnUiThread(() -> {
                        try {
                            for (int i = 0; i < places.length(); i++) {
                                JSONObject place = places.getJSONObject(i);
                                JSONObject location = place.getJSONObject("location");
                                double placeLat = location.getDouble("latitude");
                                double placeLng = location.getDouble("longitude");
                                String name = place.getJSONObject("displayName").getString("text");
                                String address = place.optString("formattedAddress", "");

                                LatLng placeLatLng = new LatLng(placeLat, placeLng);
                                mMap.addMarker(new MarkerOptions()
                                        .position(placeLatLng)
                                        .title(name)
                                        .snippet(address)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            }
                        } catch (JSONException e) {
                            Log.e("CINEMA_NEW", "Parse error: " + e.toString());
                        }
                    });
                } else {
                    Log.e("CINEMA_NEW", "Error: " + sb.toString());
                    runOnUiThread(() ->
                            Toast.makeText(this, "Could not load cinemas", Toast.LENGTH_SHORT).show()
                    );
                }

            } catch (Exception e) {
                Log.e("CINEMA_NEW", "Exception: " + e.toString());
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
