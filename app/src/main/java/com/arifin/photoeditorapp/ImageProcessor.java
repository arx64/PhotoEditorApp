package com.arifin.photoeditorapp;

import android.graphics.Bitmap;
import android.graphics.Color;

public class ImageProcessor {

    // Optimasi: Gunakan array pixel untuk proses lebih cepat
    public static Bitmap adjustBrightness(Bitmap original, int value) {
        // Gunakan metode yang lebih cepat dengan getPixels()
        int width = original.getWidth();
        int height = original.getHeight();
        int[] pixels = new int[width * height];

        // Ambil semua pixel sekaligus (lebih cepat)
        original.getPixels(pixels, 0, width, 0, 0, width, height);

        // Proses brightness di array (optimasi kecepatan)
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int a = Color.alpha(pixel);
            int r = clamp(Color.red(pixel) + value);
            int g = clamp(Color.green(pixel) + value);
            int b = clamp(Color.blue(pixel) + value);
            pixels[i] = Color.argb(a, r, g, b);
        }

        // Buat bitmap baru dari array pixel
        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return newBitmap;
    }
    public static Bitmap applyGrayscale(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();
        int[] pixels = new int[width * height];
        original.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int gray = (int) (0.299 * Color.red(pixel) +
                    0.587 * Color.green(pixel) +
                    0.114 * Color.blue(pixel));
            pixels[i] = Color.rgb(gray, gray, gray);
        }

        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return newBitmap;
    }

    public static Bitmap applySepia(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();
        int[] pixels = new int[width * height];
        original.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);

            int newRed = (int) (0.393 * r + 0.769 * g + 0.189 * b);
            int newGreen = (int) (0.349 * r + 0.686 * g + 0.168 * b);
            int newBlue = (int) (0.272 * r + 0.534 * g + 0.131 * b);

            pixels[i] = Color.rgb(clamp(newRed), clamp(newGreen), clamp(newBlue));
        }

        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return newBitmap;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}