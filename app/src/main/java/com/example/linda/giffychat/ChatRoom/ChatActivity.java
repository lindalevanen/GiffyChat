package com.example.linda.giffychat.ChatRoom;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.afollestad.materialcamera.MaterialCamera;
import com.example.linda.giffychat.Constants;
import com.example.linda.giffychat.Entity.ChatMessage;
import com.example.linda.giffychat.Entity.User;
import com.example.linda.giffychat.ExceptionHandler;
import com.example.linda.giffychat.R;
import com.example.linda.giffychat.VideoConverter;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;

/**
 * The base activity for a chat. Contains a toolbar, a RecyclerView for messages and a message-sending field.
 */

public abstract class ChatActivity extends AppCompatActivity
        implements MessageHolder.onMessageLongClickListener, UserInfoFragment.onButtonPressedListener, VideoConverter.OnGifSent {

    public final static int CAMERA_RQ = 6969;
    private static final String TAG = ChatActivity.class.getSimpleName();
    private FirebaseAnalytics mFirebaseAnalytics;

    private FirebaseRecyclerAdapter adapter;
    private ProgressBar progBar;
    private ProgressDialog progDialogUpdate;
    private RecyclerView listOfMessages;
    private RecyclerView.AdapterDataObserver dataObserver;

    private String chatID;

    private Toolbar toolbar;

    protected SharedPreferences favoritePrefs;
    private SharedPreferences messageAmountPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_chat_room);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progBar = (ProgressBar) findViewById(R.id.chatProgressBar);
        progDialogUpdate = new ProgressDialog(this);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this,
                ChatActivity.class));

        initActivity();
    }

    //public abstract int getView();

    public abstract void initActivity();

    public abstract void sendNotification(String message, boolean isGif);

    @Override
    public abstract void onGifSent();

    /**
     * A bit gum solution but I couldn't figure out which method is triggered when no data is received
     * from messages database and I have to hide the progressbar when this happens.
     */

    public void hideProgbarIfNoMessages(String reference, final String id) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference(reference);
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.hasChild(id)) {
                    progBar.setVisibility(View.INVISIBLE);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        });
    }

    /**
     * Inits the RecyclerView messagelist.
     * @param reference The reference where to get the messages
     * @param id the id of the chat
     */

    public void initMessages(String reference, String id) {
        chatID = id;
        listOfMessages = (RecyclerView) findViewById(R.id.chatMessageList);

        adapter = new MessageRecyclerAdapter(ChatMessage.class, R.layout.md_listitem, MessageHolder.class,
                FirebaseDatabase.getInstance().getReference().child(reference).child(id),
                this, progDialogUpdate);

        final LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        listOfMessages.setLayoutManager(manager);

        dataObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                if(progBar.getVisibility() == View.VISIBLE) {
                    progBar.setVisibility(View.INVISIBLE);
                }

                updateMessageAmount(adapter.getItemCount());
                updateMessageCountSP();

                int friendlyMessageCount = adapter.getItemCount();
                int lastVisiblePosition =
                        manager.findLastCompletelyVisibleItemPosition();
                // If the recyclerview is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    listOfMessages.scrollToPosition(positionStart);
                }
            }
        };

        adapter.registerAdapterDataObserver(dataObserver);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(listOfMessages.getContext(),
                manager.getOrientation());
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.divider_drawable, null));
        listOfMessages.addItemDecoration(dividerItemDecoration);

        listOfMessages.setAdapter(adapter);
    }

    protected abstract void updateMessageAmount(int amount);

    public void initEditMessage(final String reference, final String id) {
        ((ImageView) findViewById(R.id.sendMessageB)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText chatMessageInput = (EditText) findViewById(R.id.editMessage);
                String message = chatMessageInput.getText().toString();

                if(!message.trim().isEmpty()) {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    User user = new User(currentUser.getEmail(), currentUser.getDisplayName(), currentUser.getUid());
                    FirebaseDatabase.getInstance()
                            .getReference()
                            .child(reference)
                            .child(id)
                            .push()
                            .setValue(new ChatMessage(message,
                                    user.getUserName(), user.getUuid(), user,
                                    false, 0, null)
                            );
                    chatMessageInput.setText("");
                    sendNotification(message, false);
                }
            }
        });
    }



    /**
     * Triggers a video/gif-recording event with the MaterialCamera-library.
     * @param duration the max duration of the gif to be taken
     */

    public void recordGif(Float duration) {
        listOfMessages.scrollToPosition(listOfMessages.getAdapter().getItemCount() - 1);
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            try{
                new MaterialCamera(this)
                        .countdownSeconds(duration)
                        .showPortraitWarning(false)
                        .qualityProfile(MaterialCamera.QUALITY_480P)
                        .start(CAMERA_RQ);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            Toast.makeText(getApplicationContext(), "This device doesn't have a camera! Bummer.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Creates a new instance of the VideoConverter-class with the data from a video taken
     * @param data the result intent from CameraActivity
     */

    public void startVideoConverter(Intent data, String ref, String id) {
        Bundle extras = data.getExtras();
        int cameraPosition = extras.getInt("position");
        int cameraOrientation = extras.getInt("orientation");
        int cameraRotation = extras.getInt("rotation");
        VideoConverter vc = new VideoConverter(this, data.getDataString(), progDialogUpdate,
                ref, id, 4, cameraPosition, cameraOrientation, cameraRotation);
        vc.execute();
    }

    @Override
    protected void onDestroy() {
        adapter.unregisterAdapterDataObserver(dataObserver);
        super.onDestroy();
        Runtime.getRuntime().gc();
        deleteCache(this);
    }

    private void updateMessageCountSP() {
        messageAmountPrefs = getSharedPreferences(Constants.messagePrefsName, Context.MODE_PRIVATE);
        if(chatID != null) {
            int messageAmount = adapter.getItemCount();
            messageAmountPrefs.edit().putInt(chatID, messageAmount).apply();
        }
    }

    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}