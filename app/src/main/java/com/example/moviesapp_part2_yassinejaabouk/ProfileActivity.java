package com.example.moviesapp_part2_yassinejaabouk;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText editName;
    private Button saveProfileButton;
    private TextView watchedCountText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.profileToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();

        editName = findViewById(R.id.editName);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        watchedCountText = findViewById(R.id.watchedCountText);

        if (user != null) {
            loadUserProfile();
            loadWatchedCount();
        }

        saveProfileButton.setOnClickListener(v -> saveProfile());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadUserProfile() {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        editName.setText(documentSnapshot.getString("name"));
                    }
                });
    }

    private void loadWatchedCount() {
        db.collection("users").document(user.getUid()).collection("watched")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    watchedCountText.setText("Movies Watched: " + queryDocumentSnapshots.size());
                });
    }

    private void saveProfile() {
        String name = editName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            editName.setError("Name is required");
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", user.getEmail());

        db.collection("users").document(user.getUid()).set(userData)
                .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show());
    }
}