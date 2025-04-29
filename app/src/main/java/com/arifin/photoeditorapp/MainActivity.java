package com.arifin.photoeditorapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private Bitmap originalBitmap;
    private ProgressBar progressBar;
    private static final int PICK_IMAGE = 1;
    private static final int BRIGHTNESS = 1, GRAYSCALE = 2, SEPIA = 3;

    private long lastBrightnessUpdateTime = 0;

    // Gunakan ExecutorService untuk background task
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Views
        imageView = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar);
        SeekBar seekBarBrightness = findViewById(R.id.seekBarBrightness);

        // Button Click Listeners
        findViewById(R.id.btnPickImage).setOnClickListener(v -> pickImage());
        findViewById(R.id.btnGrayscale).setOnClickListener(v -> applyFilter(GRAYSCALE));
        findViewById(R.id.btnSepia).setOnClickListener(v -> applyFilter(SEPIA));
        findViewById(R.id.btnReset).setOnClickListener(v -> resetImage());

        // SeekBar Listener
        seekBarBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && originalBitmap != null) {
                    applyBrightness(progress - 100); // Range -100 to +100
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void applyFilter(int filterType) {
        if (originalBitmap != null) {
            processImageInBackground(filterType, 0);
        } else {
            Toast.makeText(this, "Pilih gambar terlebih dahulu", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyBrightness(int value) {
        // Batasi update maksimal 10x per detik saat seekbar digerakkan
        if (System.currentTimeMillis() - lastBrightnessUpdateTime > 100) {
            lastBrightnessUpdateTime = System.currentTimeMillis();
            processImageInBackground(BRIGHTNESS, value);
        }
    }

    private void resetImage() {
        if (originalBitmap != null) {
            imageView.setImageBitmap(originalBitmap);
            ((SeekBar) findViewById(R.id.seekBarBrightness)).setProgress(100);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                // Kompres gambar untuk menghindari OOM
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2; // Reduce resolution by half
                originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                imageView.setImageBitmap(originalBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Menggantikan AsyncTask dengan ExecutorService + Handler
    private void processImageInBackground(int operation, int brightnessValue) {
        progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            Bitmap result = processImage(operation, brightnessValue);

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                imageView.setImageBitmap(result);
            });
        });
    }

    private Bitmap processImage(int operation, int brightnessValue) {
        if (originalBitmap == null) return null;

        switch (operation) {
            case BRIGHTNESS:
                return ImageProcessor.adjustBrightness(originalBitmap, brightnessValue);
            case GRAYSCALE:
                return ImageProcessor.applyGrayscale(originalBitmap);
            case SEPIA:
                return ImageProcessor.applySepia(originalBitmap);
            default:
                return originalBitmap;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Bersihkan ExecutorService saat Activity dihancurkan
        executorService.shutdown();
    }
}