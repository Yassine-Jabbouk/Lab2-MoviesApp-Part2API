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

public class MovieDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TMDB_API_KEY = BuildConfig.TMDB_API_KEY;
    private static final String GOOGLE_API_KEY = BuildConfig.GEMINI_API_KEY;
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
        requestQueue = Volley.newRequestQueue(this);

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
        String movieDetailsUrl = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + TMDB_API_KEY;
        String movieVideosUrl = "https://api.themoviedb.org/3/movie/" + movieId + "/videos?api_key=" + TMDB_API_KEY;

        JsonObjectRequest detailsRequest = new JsonObjectRequest(Request.Method.GET, movieDetailsUrl, null,
                response -> {
                    try {
                        currentMovieTitle = response.getString("title");
                        currentMoviePoster = response.getString("poster_path");
                        currentMovieDate = response.optString("release_date", "N/A");
                        currentMovieRating = response.optDouble("vote_average", 0.0);
                        imdbId = response.optString("imdb_id", null);

                        nameTextView.setText(currentMovieTitle);
                        descriptionTextView.setText(response.getString("overview"));
                        Glide.with(this).load("https://image.tmdb.org/t/p/w500" + currentMoviePoster).into(img);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Log.e(TAG, "Error fetching details"));

        JsonObjectRequest videosRequest = new JsonObjectRequest(Request.Method.GET, movieVideosUrl, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject video = results.getJSONObject(i);
                            if (video.getString("type").equals("Trailer") && video.getString("site").equals("YouTube")) {
                                trailerKey = video.getString("key");
                                break;
                            }
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }, error -> Log.e(TAG, "Error fetching videos"));

        requestQueue.add(detailsRequest);
        requestQueue.add(videosRequest);
    }

    private void markAsWatched() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        JSONObject movieJson = new JSONObject();
        try {
            movieJson.put("id", currentMovieId);
            movieJson.put("title", currentMovieTitle);
            movieJson.put("poster", currentMoviePoster);
            movieJson.put("date", currentMovieDate);
            movieJson.put("rating", currentMovieRating);
            movieJson.put("isFavourite", false);
        } catch (JSONException e) { e.printStackTrace(); }

        String url = BASE_URL + "/watched/" + user.getUid();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, movieJson,
                response -> {
                    watchedButton.setText("Watched \u2713");
                    watchedButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
                    watchedButton.setEnabled(false);
                    Toast.makeText(this, "Added to Watched list", Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(this, "Error: Make sure server is running", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
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
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + lat + "," + lng +
                "&radius=5000" +
                "&type=movie_theater" +
                "&key=" + GOOGLE_API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);
                            JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                            LatLng placeLatLng = new LatLng(location.getDouble("lat"), location.getDouble("lng"));
                            String name = place.getString("name");

                            mMap.addMarker(new MarkerOptions()
                                    .position(placeLatLng)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }, error -> Log.e(TAG, "Places API Error: " + error.getMessage()));

        requestQueue.add(request);
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
