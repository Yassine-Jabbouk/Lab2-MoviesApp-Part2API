package com.example.moviesapp_part2_yassinejaabouk;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MoodScanActivity extends AppCompatActivity {

    private ImageView imageViewSelfie;
    private MaterialButton btnTakePhoto, btnScanMood;
    private ProgressBar progressBar;
    private TextView tvMoodResult, tvInstructions;

    private Bitmap capturedBitmap;

    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    capturedBitmap = (Bitmap) result.getData().getExtras().get("data");
                    imageViewSelfie.setImageBitmap(capturedBitmap);
                    imageViewSelfie.setVisibility(View.VISIBLE);
                    btnScanMood.setVisibility(View.VISIBLE);
                    tvInstructions.setVisibility(View.GONE);
                }
            }
    );

    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) openCamera();
                else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_scan);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Scan My Mood");
        }

        imageViewSelfie = findViewById(R.id.imageViewSelfie);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnScanMood = findViewById(R.id.btnScanMood);
        progressBar = findViewById(R.id.progressBar);
        tvMoodResult = findViewById(R.id.tvMoodResult);
        tvInstructions = findViewById(R.id.tvInstructions);

        btnTakePhoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnScanMood.setOnClickListener(v -> analyzeMood());
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void analyzeMood() {
        if (capturedBitmap == null) {
            Toast.makeText(this, "Please take a photo first!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnScanMood.setEnabled(false);
        btnTakePhoto.setEnabled(false);
        tvMoodResult.setText("Analyzing your mood...");
        tvMoodResult.setVisibility(View.VISIBLE);

        // Convert bitmap to base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        capturedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        new Thread(() -> {
            try {
                JSONObject requestBody = new JSONObject()
                        .put("contents", new JSONArray()
                                .put(new JSONObject()
                                        .put("parts", new JSONArray()
                                                .put(new JSONObject()
                                                        .put("inline_data", new JSONObject()
                                                                .put("mime_type", "image/jpeg")
                                                                .put("data", base64Image)))
                                                .put(new JSONObject()
                                                        .put("text",
                                                                "Look at this person's face and detect their emotion. " +
                                                                        "Based on their mood, return ONLY one single English word " +
                                                                        "that describes what kind of movie they would enjoy right now. " +
                                                                        "Examples: if they look sad return 'comedy', if happy return 'adventure', " +
                                                                        "if scared return 'thriller', if relaxed return 'romance', " +
                                                                        "if angry return 'action', if tired return 'animation'. " +
                                                                        "Return ONLY one word, nothing else.")))));

                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
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
                Log.d("MOOD_RESPONSE", responseStr);

                if (responseCode == 200) {
                    JSONObject jsonResponse = new JSONObject(responseStr);
                    String keyword = jsonResponse
                            .getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim()
                            .toLowerCase()
                            .replaceAll("[^a-z]", "");

                    Log.d("MOOD_KEYWORD", keyword);

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnScanMood.setEnabled(true);
                        btnTakePhoto.setEnabled(true);
                        tvMoodResult.setText("Your mood says: " + keyword + " 🎬\nFinding movies for you...");

                        // Navigate to VibeSearchActivity with the mood keyword
                        new android.os.Handler().postDelayed(() -> {
                            Intent intent = new Intent(MoodScanActivity.this, VibeSearchActivity.class);
                            intent.putExtra("vibe_query", keyword);
                            startActivity(intent);
                        }, 1500);
                    });

                } else {
                    String errorStr = responseStr;
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnScanMood.setEnabled(true);
                        btnTakePhoto.setEnabled(true);
                        tvMoodResult.setText("Error: " + errorStr);
                    });
                }

            } catch (Exception e) {
                Log.e("MOOD_ERROR", e.toString());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnScanMood.setEnabled(true);
                    btnTakePhoto.setEnabled(true);
                    tvMoodResult.setText("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}