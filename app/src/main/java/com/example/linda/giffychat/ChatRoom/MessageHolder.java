package com.example.linda.giffychat.ChatRoom;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.linda.giffychat.Constants;
import com.example.linda.giffychat.Entity.ChatMessage;
import com.example.linda.giffychat.Entity.User;
import com.example.linda.giffychat.HelperMethods;
import com.example.linda.giffychat.Main.MainActivity;
import com.example.linda.giffychat.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.R.attr.cacheColorHint;
import static android.R.attr.path;
import static android.R.id.message;
import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * The MessageHolder handles the view of a message. There are 3 different types of messages:
 * 1. text message
 * 2. portrait gif message
 * 3. landscape gif message
 */

public class MessageHolder extends RecyclerView.ViewHolder {

    private static final String TAG = MessageHolder.class.getSimpleName();

    private View itemView;
    private TextView messageUser;
    private TextView messageTime;
    private TextView messageText;
    public ImageView messageGif;
    private ProgressBar progressBarGif;
    private ImageView playButtonGif;
    private Drawable placeHolderThumbnail;

    private RelativeLayout messageBase;

    private onMessageLongClickListener mListener;
    public int viewType;
    private Context context;

    public MessageHolder(View itemView, int viewType, Context context) {
        super(itemView);
        this.itemView = itemView;
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

        this.mListener = (onMessageLongClickListener) context;
        this.messageTime = (TextView) itemView.findViewById(R.id.message_time);
        this.messageUser = (TextView) itemView.findViewById(R.id.message_user);
        this.progressBarGif = (ProgressBar) itemView.findViewById(R.id.progBarGif);
        this.playButtonGif = (ImageView) itemView.findViewById(R.id.playButtonGif);
        this.messageBase = (RelativeLayout) itemView.findViewById(R.id.messageBase);
    }

    public interface onMessageLongClickListener {
        void onMessageLongClick(ChatMessage message);
    }

    public void populateView(final ChatMessage message, final ProgressDialog progDialogUpdate) {
        if(viewType == R.layout.message_text) {
            this.messageText.setText(message.getMessageData());
        } else if (viewType == R.layout.message_gif_landscape ||
                   viewType == R.layout.message_gif_portrait) {
            progDialogUpdate.dismiss();
            populateGifView(message);
        }

        if(message.getUser() != null) {
            changeMessageBG(message.getUser().getUuid());
            this.messageUser.setText(message.getUser().getUserName());
        } else {
            changeMessageBG(message.getMessageUserID());
            this.messageUser.setText(message.getMessageUser());
        }

        messageBase.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    mListener.onMessageLongClick(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        });

        this.messageTime.setText(DateFormat.format("dd/MM (HH:mm)", message.getMessageTime()));
    }

    private void populateGifView(final ChatMessage message) {
        playButtonGif.setVisibility(View.VISIBLE);
        if(message.getThumbnailBase64() != null) {
            Bitmap thumbnail = HelperMethods.getBitmapFromBase64(message.getThumbnailBase64());
            messageGif.setImageBitmap(thumbnail);
            placeHolderThumbnail = new GlideBitmapDrawable(context.getResources(), thumbnail);
        } else {
            messageGif.setImageDrawable(null);
            placeHolderThumbnail = null;
        }
        messageGif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playButtonGif.setVisibility(View.GONE);
                progressBarGif.setVisibility(View.VISIBLE);
                loadGifImage(message);
            }
        });
    }

    private void changeMessageBG(String uuid) {
        if(uuid != null) {
            String possibleSavedColor = Constants.getUserColor(uuid);
            if(possibleSavedColor != null) {
                messageBase.setBackgroundColor(Color.parseColor(possibleSavedColor));
            } else {
                loadMessageColor(uuid);
            }
        } else {
            messageBase.setBackgroundColor(Color.parseColor("#FFFAFAFA"));
        }
    }

    private void loadMessageColor(final String messageUserID) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("userData")
                .child(messageUserID).child("color");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot.getValue() != null) {
                    String color = "#" + snapshot.getValue();
                    messageBase.setBackgroundColor(Color.parseColor(color));
                    Constants.addUserColor(messageUserID, color);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        });
    }

    /**
     * Gets gif from cache if it's there. If not, fetches it from Firebase storage and saves it to cache.
     * @param message the message that contains the gif
     */

    private void loadGifImage(final ChatMessage message) {
        File gifFile = getGifFromCache(message.getMessageTime());

        if(gifFile != null && gifFile.exists()) {
            /* When a gif is shown later, it can be fetched from phone's cache */
            int size = (int) gifFile.length();
            byte[] bytes = new byte[size];
            try {
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(gifFile));
                buf.read(bytes, 0, bytes.length);
                buf.close();
                loadGiftToView(bytes);
            } catch (FileNotFoundException e) {
                Toast.makeText(context, R.string.gif_not_found, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } catch (OutOfMemoryError e) {
                Toast.makeText(context, R.string.cant_load_gif, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } catch (IOException e) {
                Toast.makeText(context, R.string.problem_reading_file, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            /* Fetch the gif from Firebase Storage when it's shown for the first time */
            /* Then store it to app's cache on the device */

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference gifRef = storage.getReferenceFromUrl(message.getMessageData());

            gifRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    writeGifToCache(bytes, message.getMessageTime());
                    try {
                        loadGiftToView(bytes);
                    } catch (OutOfMemoryError e) {
                        Toast.makeText(context, R.string.out_of_memory, Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.d(TAG, exception.getMessage());
                }
            });
        }
    }

    /**
     * Loads the gif to messageGif ImageView with Glide. Shows a thumbnail of the gif during loading
     * and hides the visible progressbar after the gif has loaded or failed loading.
     * @param gifBytes the bytes of the gif to be displayed
     */

    private void loadGiftToView(byte[] gifBytes) {
        Glide.with(context)
                .load(gifBytes)
                .placeholder(placeHolderThumbnail)
                .listener(new RequestListener<byte[], GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, byte[] model, Target<GlideDrawable> target,
                                               boolean isFirstResource) {
                        Toast.makeText(context, R.string.failed_to_get_gif, Toast.LENGTH_SHORT).show();
                        progressBarGif.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, byte[] model,
                                                   Target<GlideDrawable> target, boolean isFromMemoryCache,
                                                   boolean isFirstResource) {
                        progressBarGif.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(messageGif);
    }

    private File getGifFromCache(long timestamp) {
        File file = null;
        try {
            String cache = context.getExternalCacheDir().getAbsolutePath();
            file = new File(cache + File.separator + Long.toString(timestamp) + ".jpg");
        } catch (NullPointerException e) {
            Toast.makeText(context, R.string.cant_find_cache, Toast.LENGTH_SHORT).show();
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

