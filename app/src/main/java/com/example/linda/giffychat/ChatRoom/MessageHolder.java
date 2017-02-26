package com.example.linda.giffychat.ChatRoom;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.linda.giffychat.Entity.ChatMessage;
import com.example.linda.giffychat.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;

public class MessageHolder extends RecyclerView.ViewHolder {

    private static final String TAG = MessageHolder.class.getSimpleName();

    private TextView messageUser;
    private TextView messageTime;
    private TextView messageText;
    private ImageView messageGif;

    private int viewType;
    private Context context;

    public MessageHolder(View itemView, int viewType, Context context) {
        super(itemView);
        this.viewType = viewType;
        this.context = context;
        switch (viewType) {
            case R.layout.message_text:
                this.messageText = (TextView) itemView.findViewById(R.id.message_data);
                break;
            case R.layout.message_gif_portrait:
                this.messageGif = (ImageView) itemView.findViewById(R.id.gifView);
                break;
            case R.layout.message_gif_landscape:
                this.messageGif = (ImageView) itemView.findViewById(R.id.gifView);
                break;
        }

        this.messageTime = (TextView) itemView.findViewById(R.id.message_time);
        this.messageUser = (TextView) itemView.findViewById(R.id.message_user);
    }

    public void populateView(ChatMessage message, ProgressDialog progDialogUpdate) {
        switch (viewType) {
            case R.layout.message_text:
                this.messageText.setText(message.getMessageData());
                break;
            case R.layout.message_gif_portrait:
                loadGifImage(message, progDialogUpdate);
                break;
            case R.layout.message_gif_landscape:
                loadGifImage(message, progDialogUpdate);
                break;

        }

        this.messageUser.setText(message.getMessageUser());
        this.messageTime.setText(DateFormat.format("dd/MM (HH:mm)",
                message.getMessageTime()));
    }

    public void loadGifImage(final ChatMessage message, ProgressDialog progDialogUpdate) {
        //The message is a gif message
        File gifFile = getGifFromCache(message.getMessageTime());

        if(gifFile != null && gifFile.exists()) {
            /* When a gif is shown later, it can be fetched from phone's cache */
            Glide.with(context)
                    .load(gifFile)
                    .into(messageGif);
        } else {
            /* Fetch the gif from Firebase Storage when it's shown for the first time */
            /* Then store it to app's cache on the device */
            progDialogUpdate.dismiss();

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference gifRef = storage.getReferenceFromUrl(message.getMessageData());

            gifRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {

                    writeGifToCache(bytes, message.getMessageTime());
                    Glide.with(context)
                            .load(bytes)
                            .into(messageGif);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.d(TAG, exception.getMessage());
                }
            });
        }
    }

    private File getGifFromCache(long timestamp) {
        File file = null;
        try {
            String cache = context.getExternalCacheDir().getAbsolutePath();
            file = new File(cache + File.separator + Long.toString(timestamp) + ".jpg");
        } catch (NullPointerException e) {
            Toast.makeText(context, "Can't find app's cache on this device.", Toast.LENGTH_SHORT).show();
        }
        return file;
    }

    private void writeGifToCache(byte[] bytes, long timestamp) {
        try {
            String cache = context.getExternalCacheDir().getAbsolutePath();
            File file = new File(cache + File.separator + Long.toString(timestamp) + ".jpg");
            FileOutputStream fis = new FileOutputStream(file);
            fis.write(bytes);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}