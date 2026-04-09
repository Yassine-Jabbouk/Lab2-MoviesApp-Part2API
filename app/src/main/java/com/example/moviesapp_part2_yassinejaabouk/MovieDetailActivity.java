package com.example.moviesapp_part2_yassinejaabouk;

import static android.content.ContentValues.TAG;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MovieDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TMDB_API_KEY = "d54cde38155b4c4da726ac95ed215079";

    private SupportMapFragment mapFragment;
    private TextView descriptionTextView;
    private TextView nameTextView;
    private ImageView img;
    private Button playButton;
    private String trailerKey;
    private RequestQueue requestQueue;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        descriptionTextView = findViewById(R.id.Details);
        nameTextView = findViewById(R.id.textName);
        img = findViewById(R.id.imageview);
        playButton = findViewById(R.id.playButton);
        requestQueue = Volley.newRequestQueue(this);

        int movieId = getIntent().getIntExtra("movieId", -1);
        if (movieId != -1) {
            fetchMovieDetails(movieId);
        } else {
            descriptionTextView.setText("No movie ID provided");
        }

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTrailer();
            }
        });

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void fetchMovieDetails(int movieId) {
        String movieDetailsUrl = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + TMDB_API_KEY;
        String movieVideosUrl = "https://api.themoviedb.org/3/movie/" + movieId + "/videos?api_key=" + TMDB_API_KEY;

        JsonObjectRequest detailsRequest = new JsonObjectRequest(
                Request.Method.GET, movieDetailsUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String movieName = response.getString("title");
                            String movieDescription = response.getString("overview");
                            String imageUrl = "https://image.tmdb.org/t/p/w500" + response.getString("poster_path");

                            nameTextView.setText(movieName);
                            descriptionTextView.setText(movieDescription);

                            Glide.with(MovieDetailActivity.this)
                                    .load(imageUrl)
                                    .into(img);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching details: " + error.getMessage());
                        descriptionTextView.setText("Failed to fetch movie details");
                    }
                });

        JsonObjectRequest videosRequest = new JsonObjectRequest(
                Request.Method.GET, movieVideosUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("results")) {
                                JSONArray results = response.getJSONArray("results");
                                for (int i = 0; i < results.length(); i++) {
                                    JSONObject video = results.getJSONObject(i);
                                    if (video.getString("type").equals("Trailer")) {
                                        trailerKey = video.getString("key");
                                        break;
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching videos: " + error.getMessage());
                        Toast.makeText(MovieDetailActivity.this, "Trailer not available", Toast.LENGTH_SHORT).show();
                    }
                });

        requestQueue.add(detailsRequest);
        requestQueue.add(videosRequest);
    }

    private void playTrailer() {
        if (trailerKey != null && !trailerKey.isEmpty()) {
            String trailerUrl = "https://www.youtube.com/embed/" + trailerKey;
            Intent intent = new Intent(MovieDetailActivity.this, VideoPlayer.class);
            intent.putExtra("videoUrl", trailerUrl);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Trailer not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            LatLng cinemaLocation = new LatLng(33.596460, -7.615480);
            addCinemaMarker(cinemaLocation);
            moveToCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void addCinemaMarker(LatLng cinemaLocation) {
        mMap.addMarker(new MarkerOptions()
                .position(cinemaLocation)
                .title("Cinema")
                .snippet("Location of the cinema"));
    }

    private void moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = null;
            try {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } catch (SecurityException e) {
                e.printStackTrace();
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                return;
            }

            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
            } else {
                Toast.makeText(this, "Last known location not available", Toast.LENGTH_SHORT).show();
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                moveToCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}