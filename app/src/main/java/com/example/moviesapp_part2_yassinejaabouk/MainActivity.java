package com.example.moviesapp_part2_yassinejaabouk;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TMDB_API_KEY = BuildConfig.TMDB_API_KEY;
    private static final String BASE_URL = "https://api.themoviedb.org/3/";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private MyMovieAdapter myMovieAdapter;
    private TextInputEditText searchEditText;
    private TextInputLayout searchInputLayout;
    private ImageButton logoutButton, menuButton, filterButton;
    private MaterialButton vibeSearchBtn;
    private ChipGroup chipGroup;
    private ProgressBar progressBar;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RequestQueue requestQueue;
    
    private int currentPage = 1;
    private boolean isLoading = false;
    private int selectedGenreId = -1; 
    private String currentSearchQuery = "";
    private String sortBy = "popularity.desc";

    private final List<MyMovieData> allFetchedMovies = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final ActivityResultLauncher<Intent> voiceSearchLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (results != null && !results.isEmpty()) {
                        searchEditText.setText(results.get(0));
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        requestQueue = Volley.newRequestQueue(this);
        
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        searchEditText = findViewById(R.id.editTextSearch);
        searchInputLayout = findViewById(R.id.searchInputLayout);
        logoutButton = findViewById(R.id.logoutButton);
        menuButton = findViewById(R.id.menuButton);
        filterButton = findViewById(R.id.filterButton);
        vibeSearchBtn = findViewById(R.id.vibeSearchBtn);
        chipGroup = findViewById(R.id.chipGroup);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        myMovieAdapter = new MyMovieAdapter(new MyMovieData[0], this);
        recyclerView.setAdapter(myMovieAdapter);

        updateNavHeader(user);
        setupNavigationDrawer();
        fetchGenres();
        fetchMovies(false);

        setupListeners(layoutManager);
    }

    private void updateNavHeader(FirebaseUser user) {
        View headerView = navigationView.getHeaderView(0);
        TextView nameTv = headerView.findViewById(R.id.userNameTextView);
        TextView emailTv = headerView.findViewById(R.id.userEmailTextView);
        
        emailTv.setText(user.getEmail());
        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    nameTv.setText(documentSnapshot.getString("name"));
                } else {
                    nameTv.setText("User Name");
                }
            });
    }

    private void setupNavigationDrawer() {
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            } else if (id == R.id.nav_watched) {
                startActivity(new Intent(MainActivity.this, WatchedMoviesActivity.class));
            } else if (id == R.id.nav_favorites) {
                startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
            } else if (id == R.id.nav_mood_recommend) {
            startActivity(new Intent(MainActivity.this, MoodScanActivity.class));
        }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupListeners(LinearLayoutManager layoutManager) {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    currentSearchQuery = s.toString().trim();
                    currentPage = 1;
                    allFetchedMovies.clear();
                    myMovieAdapter.clearMovies();
                    fetchMovies(false);
                };
                searchHandler.postDelayed(searchRunnable, 500);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchInputLayout.setEndIconOnClickListener(v -> startVoiceSearch());

        vibeSearchBtn.setOnClickListener(v -> {
            // Navigate to dedicated Vibe Search Activity
            Intent intent = new Intent(MainActivity.this, VibeSearchActivity.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        filterButton.setOnClickListener(this::showFilterMenu);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) { 
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 5) {
                            currentPage++;
                            fetchMovies(true);
                        }
                    }
                }
            }
        });

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedGenreId = -1;
            } else {
                int checkedId = checkedIds.get(0);
                Chip chip = findViewById(checkedId);
                if (chip != null && chip.getTag() != null) {
                    selectedGenreId = (int) chip.getTag();
                } else {
                    selectedGenreId = -1;
                    if (searchEditText.getText() != null && !searchEditText.getText().toString().isEmpty()) {
                        searchEditText.setText(""); 
                        return; 
                    }
                }
            }
            
            currentPage = 1;
            allFetchedMovies.clear();
            myMovieAdapter.clearMovies();
            fetchMovies(false);
        });
    }

    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak the movie title...");

        try {
            voiceSearchLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Voice search not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFilterMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add(1, 1, 1, "Popularity");
        popup.getMenu().add(1, 2, 2, "Rating (High to Low)");
        popup.getMenu().add(1, 3, 3, "Release Date (Newest)");
        
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: sortBy = "popularity.desc"; break;
                case 2: sortBy = "vote_average.desc"; break;
                case 3: sortBy = "release_date.desc"; break;
            }
            applyLocalSortAndFilter();
            return true;
        });
        popup.show();
    }

    private void applyLocalSortAndFilter() {
        List<MyMovieData> listToDisplay = new ArrayList<>(allFetchedMovies);

        if (sortBy.equals("vote_average.desc")) {
            listToDisplay.sort((m1, m2) -> Double.compare(m2.getRating(), m1.getRating()));
        } else if (sortBy.equals("release_date.desc")) {
            listToDisplay.sort((m1, m2) -> m2.getMovieDate().compareTo(m1.getMovieDate()));
        }

        myMovieAdapter.clearMovies();
        myMovieAdapter.addMovies(listToDisplay.toArray(new MyMovieData[0]));
    }

    private void fetchGenres() {
        String url = BASE_URL + "genre/movie/list?api_key=" + TMDB_API_KEY;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray genres = response.getJSONArray("genres");
                        for (int i = 0; i < genres.length(); i++) {
                            JSONObject g = genres.getJSONObject(i);
                            addGenreChip(g.getInt("id"), g.getString("name"));
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }, error -> Log.e("TMDB", "Genre error"));
        requestQueue.add(request);
    }

    private void addGenreChip(int id, String name) {
        Chip chip = new Chip(this);
        chip.setText(name);
        chip.setTag(id);
        chip.setCheckable(true);
        chip.setClickable(true);
        chipGroup.addView(chip);
    }

    private void fetchMovies(boolean isLoadMore) {
        if (isLoading) return;
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        String url;
        if (!currentSearchQuery.isEmpty()) {
            try {
                url = BASE_URL + "search/movie?api_key=" + TMDB_API_KEY + "&query=" + URLEncoder.encode(currentSearchQuery, "UTF-8") + "&page=" + currentPage;
            } catch (UnsupportedEncodingException e) {
                url = BASE_URL + "search/movie?api_key=" + TMDB_API_KEY + "&query=" + currentSearchQuery + "&page=" + currentPage;
            }
        } else {
            url = BASE_URL + "discover/movie?api_key=" + TMDB_API_KEY + "&sort_by=" + sortBy + "&page=" + currentPage;
            if (selectedGenreId != -1) url += "&with_genres=" + selectedGenreId;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONArray results = response.getJSONArray("results");
                        List<MyMovieData> newBatch = new ArrayList<>();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject m = results.getJSONObject(i);
                            MyMovieData movie = new MyMovieData(m.getInt("id"), m.getString("title"), m.optString("release_date", "N/A"), m.optString("poster_path", ""), m.optDouble("vote_average", 0.0));
                            
                            if (!currentSearchQuery.isEmpty() && selectedGenreId != -1) {
                                JSONArray genreIds = m.optJSONArray("genre_ids");
                                boolean matchesGenre = false;
                                if (genreIds != null) {
                                    for(int j=0; j<genreIds.length(); j++) {
                                        if(genreIds.getInt(j) == selectedGenreId) {
                                            matchesGenre = true;
                                            break;
                                        }
                                    }
                                }
                                if(matchesGenre) {
                                    newBatch.add(movie);
                                }
                            } else {
                                newBatch.add(movie);
                            }
                        }
                        
                        allFetchedMovies.addAll(newBatch);
                        applyLocalSortAndFilter();

                    } catch (JSONException e) { e.printStackTrace(); }
                }, error -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                });
        requestQueue.add(request);
    }
}
