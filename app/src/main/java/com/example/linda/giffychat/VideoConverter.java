package com.example.linda.giffychat;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.gifencoder.AnimatedGifEncoder;
import com.example.linda.giffychat.Entity.ChatMessage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

import wseemann.media.FFmpegMediaMetadataRetriever;

import static android.R.attr.bitmap;
import static android.R.attr.rotation;
import static android.R.attr.thumb;
import static android.R.attr.thumbnail;

/**
 * A class for converting a video in device's memory into a gif to be sent to server.
 */

public class VideoConverter extends AsyncTask {

    private String path;
    private Context context;
    private int duration;
    private int cameraPosition;
    private int cameraOrientation;
    private int cameraRotation;
    private String roomID;
    private ProgressDialog progDialogConvert;
    private ProgressDialog progDialogUpdate;

    private byte[] gif;
    private String timestamp;
    private String thumbnailbase64;

    /**
     * Creates a new instance of the VideoConverter.
     * @param context The activity's context in which this class is used
     * @param path The path to the video to be converted to gif
     * @param progDialogUpdate A Progressdialog that is to be dismissed after the chat has updated.
     * @param roomID The room's id where the gif is to be displayed
     * @param duration The duration of the gif
     * @param cameraPosition 0 = unknown, 1 = front camera, 2 = back camera
     * @param cameraOrientation 0 = unknown, 1 = portrait, 2 = landscape
     * @param cameraRotation 0 = unknown, 1 = 90 degrees cw, 2 = 180 degrees cw, 3 = 90 degrees ccw
     */

    public VideoConverter(Context context, String path, ProgressDialog progDialogUpdate, String roomID,
                          int duration, int cameraPosition, int cameraOrientation, int cameraRotation) {
        this.context = context;
        this.path = path;
        this.roomID = roomID;
        this.duration = duration;
        this.progDialogUpdate = progDialogUpdate;
        this.cameraPosition = cameraPosition;
        this.cameraOrientation = cameraOrientation;
        this.cameraRotation = cameraRotation;

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(context,
                VideoConverter.class));

        progDialogConvert = new ProgressDialog(context);
        progDialogConvert.setMessage("Converting video to gif...");
        progDialogConvert.setCancelable(false);
        progDialogUpdate.setMessage("Updating chat...");
        progDialogUpdate.setCancelable(false);
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        ArrayList<Bitmap> bitmaps = convertmp4toBitmaps(path);
        if(bitmaps != null) {
            gif = generateGIF(bitmaps);
        } else {
            Toast.makeText(context, "Converting failed, sorry!", Toast.LENGTH_SHORT).show();
            progDialogUpdate.dismiss();
            progDialogConvert.dismiss();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        sendGifToServer();
        deleteVideoFromCache();
        progDialogConvert.dismiss();
        progDialogUpdate.show();
        super.onPostExecute(o);
    }

    @Override
    protected void onPreExecute() {
        progDialogConvert.show();
        super.onPreExecute();
    }

    private void deleteVideoFromCache() {
        try {
            String cache = context.getExternalCacheDir().getAbsolutePath();
            File file2 = new File(path); //gum but works unlike only deleting this file
            File file = new File(cache + File.separator + file2.getName());
            file.delete();
        } catch (Exception e) {
            FirebaseCrash.report(e);
            e.printStackTrace();
        }
    }

    private void sendGifToServer() {
        if(gif != null) {
            try {
                timestamp = Long.toString(System.currentTimeMillis()) + ".jpg";
                StorageReference storageRef = FirebaseStorage.getInstance().getReference(timestamp);
                UploadTask uploadTask = storageRef.putBytes(gif);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        FirebaseCrash.log(exception.getMessage());
                        FirebaseCrash.report(exception);
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                        Uri url = taskSnapshot.getDownloadUrl();
                        sendPathToDB(url);
                    }
                });
            } catch (Exception e) {
                Toast.makeText(context, "Converting failed, sorry!", Toast.LENGTH_SHORT).show();
                FirebaseCrash.report(e);
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends the path of the location of the gif in Firebase storage to Firebase db.
     * @param url the url where the gif is located
     */

    private void sendPathToDB(Uri url) {
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            FirebaseDatabase.getInstance()
                    .getReference()
                    .child("chatMessages")
                    .child(roomID)
                    .push()
                    .setValue(new ChatMessage(url.toString(),
                            currentUser.getDisplayName(),
                            currentUser.getUid(),
                            true, cameraOrientation,
                            thumbnailbase64)
                    );
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrash.report(e);
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private ArrayList<Bitmap> convertmp4toBitmaps(String path) {
        try {
            FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();
            mmr.setDataSource(path);
            mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM);
            mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST);

            float rotationDegrees = 0f;
            int width = 160;
            int height = 200;

            // hopefully the way device saves pictures rotated weirdly isn't very device specific.....
            // works on samsung galaxy note 5, oneplus 3t
            // nexus 5x turns back cam videos upside down
            // The library doesn't support case 2 (reverse-portrait)
            switch (cameraRotation) {
                case 0: //portrait
                    switch (cameraPosition) {
                        case 1: // from camera
                            rotationDegrees = 270f;
                            break;
                        case 2: // back camera
                            rotationDegrees = 90f;
                            break;
                    }
                    break;
                case 1: //landscape
                    width = 200;
                    height = 160;
                    break;
                case 3: //reverse-landscape
                    rotationDegrees = 180f;
                    width = 200;
                    height = 160;
                    break;
                default:
                    break;
            }

            ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();

            Bitmap thumbnail = mmr.getFrameAtTime(0, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
            if(thumbnail == null) return null;
            Bitmap processedThumbnail = processBitmap(thumbnail, 7, width, height, rotationDegrees);
            thumbnailbase64 = HelperMethods.getBase64FromBitmap(processedThumbnail);

            for (int i = 0; i < (duration * 10); i++) {
                int microsec = i * 100000;
                Bitmap bitmap = mmr.getFrameAtTime(microsec, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
                if(bitmap != null) {
                    Bitmap processedBitmap = processBitmap(bitmap, 50, width, height, rotationDegrees);
                    bitmaps.add(processedBitmap);
                }
            }
            mmr.release();

            return bitmaps;
        } catch (Exception e) {
            FirebaseCrash.report(e);
            e.printStackTrace();
        }
        return null;
    }

    private Bitmap processBitmap(Bitmap btm, int compressionPercent, int width, int height, float rotationDegrees) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        btm.compress(Bitmap.CompressFormat.JPEG, compressionPercent, stream);
        byte[] temp = stream.toByteArray();
        Bitmap compressedB = BitmapFactory.decodeByteArray(temp, 0, temp.length);
        Bitmap rotatedBitmap = HelperMethods.rotateBitmap(compressedB, rotationDegrees);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, width, height, false);
        return scaledBitmap;
    }

    /**
     * Generates a gif (byte array) from Bitmap images. Very useful since we get the Bitmaps from
     * frames from the video taken and this converts the frames to useful byte array.
     * @param bitmapArray the bitmaps of the gif
     * @return a byte array gif
     */

    private byte[] generateGIF(ArrayList<Bitmap> bitmapArray) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            AnimatedGifEncoder encoder = new AnimatedGifEncoder();
            encoder.start(bos);
            for (Bitmap bitmap : bitmapArray) {
                encoder.addFrame(bitmap);
            }
            encoder.finish();
            return bos.toByteArray();
        } catch (Exception e) {
            FirebaseCrash.report(e);
            e.printStackTrace();
        }
        return null;
    }
}
