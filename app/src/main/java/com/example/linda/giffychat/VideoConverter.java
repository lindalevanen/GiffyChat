package com.example.linda.giffychat;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.Image;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.gifencoder.AnimatedGifEncoder;
import com.example.linda.giffychat.ChatRoom.ChatRoomActivity;
import com.example.linda.giffychat.Entity.ChatMessage;
import com.example.linda.giffychat.Entity.Room;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import wseemann.media.FFmpegMediaMetadataRetriever;

import static android.R.attr.bitmap;
import static android.R.attr.data;
import static android.icu.util.MeasureUnit.BYTE;
import static com.example.linda.giffychat.R.layout.room;
import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * A class for converting a video in device's memory into a gif to be sent to server.
 */

public class VideoConverter extends AsyncTask {

    private String path;
    private Context context;
    private int duration;
    private String roomID;
    private ProgressDialog progDialogConvert;
    private ProgressDialog progDialogUpdate;

    private byte[] gif;
    private String timestamp;

    /**
     * Creates a new instance of the VideoConverter.
     * @param context The activity's context in which this class is used
     * @param path The path to the video to be converted to gif
     * @param progDialogUpdate A Progressdialog that is to be dismissed after the chat has updated.
     * @param roomID The room's id where the gif is to be displayed
     * @param duration The duration of the gif
     */

    public VideoConverter(Context context, String path, ProgressDialog progDialogUpdate,
                          String roomID, int duration) {
        this.context = context;
        this.path = path;
        this.roomID = roomID;
        this.duration = duration;
        this.progDialogUpdate = progDialogUpdate;

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
            FirebaseDatabase.getInstance()
                    .getReference()
                    .child("chatMessages")
                    .child(roomID)
                    .push()
                    .setValue(new ChatMessage(url.toString(),
                            FirebaseAuth.getInstance()
                                    .getCurrentUser()
                                    .getDisplayName(),
                                    true)
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

            ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();

            for (int i = 0; i < (duration * 10); i++) {
                int microsec = i * 100000;
                Bitmap bitmap = mmr.getFrameAtTime(microsec, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);

                // Some videos don't have the last milliseconds even though it should last the duration, that's
                // why there's the else if (i < 30).
                // The "Material Camera" library I use is quite limited so I'm using only portrait mode since
                // I don't get any information from the camera activity about the orientation the image was taken.
                // It's a crucial information especially because the portrait pictures somehow
                // get saved 90 degrees cw. So whenever the user takes the gif in portrait mode I should rotate
                // the result, but atm I don't know how to get the information how the gif was taken.
                if(bitmap != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 30, stream);
                    byte[] temp = stream.toByteArray();
                    Bitmap compressedB = BitmapFactory.decodeByteArray(temp, 0, temp.length);
                    Bitmap rotatedBitmap = HelperMethods.rotateBitmap(compressedB, 270f);
                    bitmaps.add(Bitmap.createScaledBitmap(rotatedBitmap, 160, 200, false));
                } else if (i < 30) {
                    return null;
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

    /**
     * Generates a gif (byte array) from Bitmap images. Very useful since we get the Bitmaps from
     * frames from the video taken and this converts the frames to
     * @param bitmapArray
     * @return
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
