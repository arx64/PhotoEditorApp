package com.arifin.photoeditorapp;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yalantis.ucrop.UCrop;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Bitmap originalBitmap;
    private Bitmap currentBitmap;
    private int currentRotation = 0;
    private boolean isFlipped = false;

    private ImageView imageView;
    private ProgressBar progressBar;
    private static final int PICK_IMAGE = 1;
    private static final int BRIGHTNESS = 1, GRAYSCALE = 2, SEPIA = 3;

    private long lastBrightnessUpdateTime = 0;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar);
        SeekBar seekBarBrightness = findViewById(R.id.seekBarBrightness);
        Button btnCrop = findViewById(R.id.btnCrop);
        Button btnRotate = findViewById(R.id.btnRotate);
        Button btnFlip = findViewById(R.id.btnFlip);
        Button btnSave = findViewById(R.id.btnSave);

        findViewById(R.id.btnPickImage).setOnClickListener(v -> pickImage());
        findViewById(R.id.btnGrayscale).setOnClickListener(v -> applyFilter(GRAYSCALE));
        findViewById(R.id.btnSepia).setOnClickListener(v -> applyFilter(SEPIA));
        findViewById(R.id.btnReset).setOnClickListener(v -> resetImage());
        btnCrop.setOnClickListener(v -> cropImage());
        btnRotate.setOnClickListener(v -> rotateImage());
        btnFlip.setOnClickListener(v -> flipImage());
        btnSave.setOnClickListener(v -> saveImage());

        seekBarBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && originalBitmap != null) {
                    applyBrightness(progress - 100); // Range -100 to 100
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
        if (System.currentTimeMillis() - lastBrightnessUpdateTime > 100) {
            lastBrightnessUpdateTime = System.currentTimeMillis();
            processImageInBackground(BRIGHTNESS, value);
        }
    }

    private void resetImage() {
        if (originalBitmap != null) {
            currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            imageView.setImageBitmap(currentBitmap);
            ((SeekBar) findViewById(R.id.seekBarBrightness)).setProgress(100);
        }
    }

    private void cropImage() {
        if (currentBitmap == null) {
            Toast.makeText(this, "Pilih gambar terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = bitmapToUri(currentBitmap);
        Uri destinationUri = Uri.fromFile(
                new java.io.File(getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg")
        );

        UCrop.of(uri, destinationUri)
                .withAspectRatio(1, 1)
                .start(this);
    }

    private void rotateImage() {
        if (currentBitmap == null) return;

        executorService.execute(() -> {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            try {
                Bitmap rotated = Bitmap.createBitmap(currentBitmap, 0, 0,
                        currentBitmap.getWidth(), currentBitmap.getHeight(), matrix, true);
                mainHandler.post(() -> {
                    currentBitmap = rotated;
                    imageView.setImageBitmap(currentBitmap);
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(MainActivity.this, "Rotation failed", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void flipImage() {
        if (currentBitmap == null) return;

        executorService.execute(() -> {
            Matrix matrix = new Matrix();
            matrix.preScale(isFlipped ? 1 : -1, 1);
            isFlipped = !isFlipped;

            try {
                Bitmap flipped = Bitmap.createBitmap(currentBitmap, 0, 0,
                        currentBitmap.getWidth(), currentBitmap.getHeight(), matrix, true);
                mainHandler.post(() -> {
                    currentBitmap = flipped;
                    imageView.setImageBitmap(currentBitmap);
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(MainActivity.this, "Flip failed", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void saveImage() {
        if (currentBitmap == null) {
            Toast.makeText(this, "Tidak ada gambar untuk disimpan", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            Toast.makeText(this, "Gambar disimpan di Gallery", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private Uri bitmapToUri(Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "temp", null);
        return Uri.parse(path);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                currentBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
                imageView.setImageBitmap(currentBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK && data != null) {
            Uri resultUri = UCrop.getOutput(data);
            try {
                currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                imageView.setImageBitmap(currentBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Gagal mengambil hasil crop", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processImageInBackground(int operation, int brightnessValue) {
        progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            Bitmap result = processImage(operation, brightnessValue);

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                currentBitmap = result;
                imageView.setImageBitmap(result);
            });
        });
    }

    private Bitmap processImage(int operation, int brightnessValue) {
        if (originalBitmap == null) return null;

        Bitmap base = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);

        switch (operation) {
            case BRIGHTNESS:
                return ImageProcessor.adjustBrightness(base, brightnessValue);
            case GRAYSCALE:
                return ImageProcessor.applyGrayscale(base);
            case SEPIA:
                return ImageProcessor.applySepia(base);
            default:
                return base;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
