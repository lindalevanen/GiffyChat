package com.example.linda.giffychat.ChatRoom;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialcamera.MaterialCamera;
import com.example.linda.giffychat.Entity.ChatMessage;
import com.example.linda.giffychat.Entity.Room;
import com.example.linda.giffychat.ExceptionHandler;
import com.example.linda.giffychat.HelperMethods;
import com.example.linda.giffychat.Main.MainActivity;
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
import java.io.IOException;
import java.util.ArrayList;

import static android.R.attr.bitmap;
import static android.R.attr.data;
import static android.R.attr.rotation;

/**
 * The base activity for a chat room. Contains a toolbar, a RecyclerView for messages and a message-sending field.
 */

public class ChatRoomActivity extends AppCompatActivity {

    private static final String TAG = ChatRoomActivity.class.getSimpleName();;
    private final static int CAMERA_RQ = 6969;
    private FirebaseAnalytics mFirebaseAnalytics;

    private String mRoomID;
    private Room mRoom;

    private FirebaseRecyclerAdapter adapter;
    private ProgressBar progBar;
    private ProgressDialog progDialogUpdate;
    private SharedPreferences favoritePrefs;
    private RecyclerView listOfMessages;

    private Toolbar toolbar;
    private boolean roomHasLogo = false;

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
                ChatRoomActivity.class));

        Bundle b = getIntent().getExtras();
        if(b != null) {
            mRoomID = b.getString("roomID");
            if(b.getString("base64icon") != null) {
                roomHasLogo = true;
                Bitmap icon = HelperMethods.getBitmapFromBase64(b.getString("base64icon"));
                RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(getResources(), icon);
                dr.setCornerRadius(10f);
                getSupportActionBar().setLogo(dr);
            }
            getRoomData(mRoomID);
        } else {
            Log.d(TAG, "Activity started incorrectly, no extras in intent.");
        }
    }

    private void getRoomData(String roomID) {

        DatabaseReference roomRef =
                FirebaseDatabase.getInstance().getReference().child("chatRooms").child(roomID);

        ValueEventListener roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mRoom = dataSnapshot.getValue(Room.class);
                initUI();
                initMessages();
                hideProgbarIfNoMessages();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadRoom:onCancelled", databaseError.toException());
                Toast.makeText(getApplicationContext(), "Room loading cancelled.", Toast.LENGTH_SHORT).show();
            }
        };
        roomRef.addListenerForSingleValueEvent(roomListener);
    }

    /**
     * A bit gum solution but I couldn't figure out which method is triggered when no data is received
     * from messages database and I have to hide the progressbar when this happens.
     */

    private void hideProgbarIfNoMessages() {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("chatMessages");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.hasChild(mRoomID)) {
                    progBar.setVisibility(View.INVISIBLE);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        });
    }

    private void initMessages() {
        listOfMessages = (RecyclerView) findViewById(R.id.chatMessageList);

        adapter = new MessageRecyclerAdapter(ChatMessage.class, R.layout.md_listitem, MessageHolder.class,
                FirebaseDatabase.getInstance().getReference().child("chatMessages").child(mRoomID),
                this, progDialogUpdate);

        final LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        listOfMessages.setLayoutManager(manager);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                if(progBar.getVisibility() == View.VISIBLE) {
                    progBar.setVisibility(View.INVISIBLE);
                }

                int friendlyMessageCount = adapter.getItemCount();
                int lastVisiblePosition =
                        manager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    listOfMessages.scrollToPosition(positionStart);
                }
            }
        });

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(listOfMessages.getContext(),
                manager.getOrientation());
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.divider_drawable, null));
        listOfMessages.addItemDecoration(dividerItemDecoration);

        listOfMessages.setAdapter(adapter);
    }

    private void initUI() {
        if(roomHasLogo) {
            toolbar.setTitle("   "+mRoom.getTitle());
        } else {
            toolbar.setTitle(mRoom.getTitle());
        }


        ((RelativeLayout) findViewById(R.id.sendMessageLO)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                EditText chatMessageInput = (EditText) findViewById(R.id.editMessage);
                String message = chatMessageInput.getText().toString();

                if(!message.trim().isEmpty()) {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                    FirebaseDatabase.getInstance()
                            .getReference()
                            .child("chatMessages")
                            .child(mRoomID)
                            .push()
                            .setValue(new ChatMessage(message,
                                    currentUser.getDisplayName(),
                                    currentUser.getUid(),
                                    false, 0, null)
                            );

                    chatMessageInput.setText("");
                }


            }
        });
    }

    private void recordGif() {
        listOfMessages.scrollToPosition(listOfMessages.getAdapter().getItemCount() - 1);
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            try{
                new MaterialCamera(this)
                        .countdownSeconds(4.0f)
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

    private void askNewMemberInfo() {
        LayoutInflater li = LayoutInflater.from(this);
        final View dialogLO = li.inflate(R.layout.dialog_add_room_member, null);

        // Creates the dialog
        final AlertDialog d = new AlertDialog.Builder(this)
                .setView(dialogLO)
                .setPositiveButton("ADD MEMBER", null)
                .setNegativeButton("CANCEL", null)
                .create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button p = d.getButton(AlertDialog.BUTTON_POSITIVE);
                Button n = d.getButton(AlertDialog.BUTTON_NEGATIVE);

                p.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String email = ((EditText) dialogLO.findViewById(R.id.newMemberInput))
                                .getText().toString().trim();

                        if(!email.isEmpty()) {

                            addMemberToRoom(email);
                            d.dismiss();
                        } else {
                            Toast.makeText(getApplicationContext(), "Please type the new members email address.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                n.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        d.dismiss();
                    }
                });
            }
        });
        d.show();
    }

    private void addMemberToRoom(final String membersEmail) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(mRoomID);
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChild("members")) {
                    DatabaseReference db = FirebaseDatabase.getInstance().getReference("chatRooms")
                        .child(mRoom.getId())
                        .child("members");
                    String count = Long.toString(snapshot.child("members").getChildrenCount());
                    db.child(count).setValue(membersEmail);
                    Toast.makeText(getApplicationContext(),
                            membersEmail + " added successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "getRoomMembers shouldn't be called if the room is not private.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        });
    }

    private void showMembers(ArrayList<String> memberEmails) {
        LayoutInflater li = LayoutInflater.from(this);
        final View dialogLO = li.inflate(R.layout.dialog_room_members, null);

        TextView membersText = (TextView) dialogLO.findViewById(R.id.membersText);
        membersText.setText("Room "+ mRoom.getTitle() +"'s members:");

        LinearLayout membersLO = (LinearLayout) dialogLO.findViewById(R.id.memberList);

        for(String member : memberEmails) {
            TextView emailView = new TextView(this);
            emailView.setText(member);
            emailView.setTextColor(Color.parseColor("#000000"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            int emailMargin = HelperMethods.dpToPx(this, 10);
            params.setMargins(0, 0, 0, emailMargin);
            membersLO.addView(emailView, params);
        }

        // Creates the dialog
        final AlertDialog d = new AlertDialog.Builder(this)
                .setView(dialogLO)
                .setPositiveButton("OK", null)
                .setNegativeButton("ADD A NEW MEMBER", null)
                .create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button p = d.getButton(AlertDialog.BUTTON_POSITIVE);
                Button n = d.getButton(AlertDialog.BUTTON_NEGATIVE);

                p.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        d.dismiss();
                    }
                });

                n.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        askNewMemberInfo();
                        d.dismiss();
                    }
                });
            }
        });
        d.show();
    }

    private void showRoomID() {
        LayoutInflater li = LayoutInflater.from(this);
        final View dialogLO = li.inflate(R.layout.dialog_show_id, null);

        ((TextView) dialogLO.findViewById(R.id.roomID)).setText("Room "+ mRoom.getTitle() +"'s id:");
        ((TextView) dialogLO.findViewById(R.id.roomID)).setText(mRoomID);

        ((ImageView) dialogLO.findViewById(R.id.copyB)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(mRoom.getTitle() + "'s ID", mRoomID);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(ChatRoomActivity.this, "Room ID saved to clipboard!", Toast.LENGTH_SHORT).show();
            }
        });

        // Creates the dialog
        final AlertDialog d = new AlertDialog.Builder(this)
                .setView(dialogLO)
                .setPositiveButton("OK", null)
                .create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button p = d.getButton(AlertDialog.BUTTON_POSITIVE);
                p.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        d.dismiss();
                    }
                });
            }
        });
        d.show();
    }

    public void getRoomMembers() {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(mRoomID);
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChild("members")) {
                    ArrayList<String> memberEmails = new ArrayList<String>();
                    for (DataSnapshot ds : snapshot.child("members").getChildren()) {
                        memberEmails.add(ds.getValue().toString());
                    }
                    showMembers(memberEmails);
                } else {
                    Log.d(TAG, "getRoomMembers shouldn't be called if the room is not private.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        });
    }

    private void toggleFavorite() {
        favoritePrefs = this.getSharedPreferences(MainActivity.favoritePrefsName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = favoritePrefs.edit();
        if(!favoritePrefs.contains(mRoomID)) {
            // Set as favorite
            editor.putBoolean(mRoomID, true);
            Toast.makeText(this, "Added to favorites.", Toast.LENGTH_SHORT).show();
        } else {
            // Delete from favorites
            editor.remove(mRoomID);
            Toast.makeText(this, "Removed from favorites.", Toast.LENGTH_SHORT).show();
        }
        editor.apply();
    }

    private void changeRoomLogo() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, MainActivity.GALLERY_PHOTO_REQUEST_CODE);
    }

    private void startVideoConverter(Intent data) {
        Bundle extras = data.getExtras();
        int cameraPosition = extras.getInt("position");
        int cameraOrientation = extras.getInt("orientation");
        int cameraRotation = extras.getInt("rotation");
        VideoConverter vc = new VideoConverter(this, data.getDataString(), progDialogUpdate,
                mRoomID, 4, cameraPosition, cameraOrientation, cameraRotation);
        vc.execute();
    }

    private Bitmap changeCurrentRoomLogo(Uri imageData) {

        //roomImage.setAdjustViewBounds(true);
        //roomImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        try {
            Bitmap bmp = HelperMethods.getBitmapFromUri(imageData, this);
            Bitmap compressedBtm = HelperMethods.getResizedBitmap(bmp, 200);
            Bitmap ccBtm = HelperMethods.centerCropBitmap(compressedBtm);
            RoundedBitmapDrawable rbd = HelperMethods.giveBitmapRoundedCorners(ccBtm, this);
            getSupportActionBar().setLogo(rbd);
            getSupportActionBar().setTitle("   " + mRoom.getTitle());
            return ccBtm;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendNewLogoToServer(String base64) {
        FirebaseDatabase.getInstance().getReference()
                .child("chatRooms")
                .child(mRoomID)
                .child("base64RoomImage")
                .setValue(base64);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if(mRoom.getMembers() == null) {
            inflater.inflate(R.menu.chat_public_actionbar_menu, menu);
        } else {
            inflater.inflate(R.menu.chat_private_actionbar_menu, menu);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        favoritePrefs = this.getSharedPreferences(MainActivity.favoritePrefsName, Context.MODE_PRIVATE);
        if(favoritePrefs.contains(mRoomID)) {
            menu.findItem(R.id.add_favorite).setVisible(false);
            menu.findItem(R.id.remove_favorite).setVisible(true);
        } else {
            menu.findItem(R.id.add_favorite).setVisible(true);
            menu.findItem(R.id.remove_favorite).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.recordGif:
                recordGif();
                return true;
            case R.id.add_favorite:
                toggleFavorite();
                invalidateOptionsMenu();
                return true;
            case R.id.remove_favorite:
                toggleFavorite();
                invalidateOptionsMenu();
                return true;
            case R.id.addMember:
                askNewMemberInfo();
                return true;
            case R.id.showMembers:
                getRoomMembers();
                return true;
            case R.id.showID:
                showRoomID();
                return true;
            case R.id.changeLogo:
                changeRoomLogo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Received recording or error from MaterialCamera
        if(resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAMERA_RQ:
                    startVideoConverter(data);
                    break;
                case MainActivity.GALLERY_PHOTO_REQUEST_CODE:
                    Bitmap newLogo = changeCurrentRoomLogo(data.getData());
                    if(newLogo != null) {
                        String base64 = HelperMethods.getBase64FromBitmap(newLogo);
                        sendNewLogoToServer(base64);
                    }
                    break;
                default:
                    Log.d(TAG, "This requestcode is not handled: "+ requestCode);
                    break;
            }
        } else if(data != null) {
            Exception e = (Exception) data.getSerializableExtra(MaterialCamera.ERROR_EXTRA);
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Runtime.getRuntime().gc();
        deleteCache(this);
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