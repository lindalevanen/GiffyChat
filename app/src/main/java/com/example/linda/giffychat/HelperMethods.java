package com.example.linda.giffychat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.DisplayMetrics;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;

import static com.example.linda.giffychat.R.layout.room;

/**
 * Contains some global handy helper methods.
 */

public class HelperMethods {

    public static String hash(String s) {
        int h = 0;
        for (int i = 0; i < s.length(); i++) {
            h = 31 * h + s.charAt(i);
        }
        if(h > 0) return Integer.toString(h);
        else return Integer.toString(h * (-1));
    }

    public static int dpToPx(Context cx, int dp) {
        DisplayMetrics displayMetrics = cx.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public static Bitmap centerCropBitmap(Bitmap btm) {
        Bitmap dstBmp;
        if (btm.getWidth() >= btm.getHeight()){

            dstBmp = Bitmap.createBitmap(
                    btm,
                    btm.getWidth()/2 - btm.getHeight()/2,
                    0,
                    btm.getHeight(),
                    btm.getHeight()
            );

        }else{

            dstBmp = Bitmap.createBitmap(
                    btm,
                    0,
                    btm.getHeight()/2 - btm.getWidth()/2,
                    btm.getWidth(),
                    btm.getWidth()
            );
        }
        return dstBmp;
    }

    public static String getBase64FromBitmap(Bitmap btm) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        btm.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();

        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public static Bitmap getBitmapFromBase64(String base64) {
        byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    public static Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

}
