package com.example.linda.giffychat.ChatRoom;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialcamera.MaterialCamera;
import com.example.linda.giffychat.Constants;
import com.example.linda.giffychat.Entity.ChatMessage;
import com.example.linda.giffychat.Entity.One2OneChat;
import com.example.linda.giffychat.Entity.Room;
import com.example.linda.giffychat.Entity.User;
import com.example.linda.giffychat.HelperMethods;
import com.example.linda.giffychat.Main.MainActivity;
import com.example.linda.giffychat.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A public or private chat room.
 */

public class ChatRoomActivity extends ChatActivity {

    private static final String TAG = ChatRoomActivity.class.getSimpleName();
    private static final String ref = "chatMessages";

    public String mRoomID;
    public Room mRoom;

    private String roomLogoBase64;

    @Override
    public void initActivity() {
        Bundle b = getIntent().getExtras();
        if(b != null) {
            mRoomID = b.getString("roomID");
            if(mRoomID != null) {
                roomLogoBase64 = b.getString("base64icon");
                getRoomData(mRoomID);
            } else {
                Log.d(TAG, "ChatRoomActivity created without RoomID! Can't initialize.");
            }

        } else {
            Log.d(TAG, "Activity started incorrectly, no extras in intent.");
        }
    }

    @Override
    public void sendNotification(String message, boolean isGif) {}

    @Override
    public void onGifSent() {}

    @Override
    protected void updateMessageAmount(int amount) {
        String path = "chatRooms/"+ mRoomID;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
        ref.child("messageCount").setValue(amount);
    }

    private void initActionBar() {
        if(roomLogoBase64 != null) {
            Bitmap icon = HelperMethods.getBitmapFromBase64(roomLogoBase64);
            RoundedBitmapDrawable dr = HelperMethods.giveBitmapRoundedCorners(icon, this);
            getSupportActionBar().setLogo(dr);
            getSupportActionBar().setTitle("   "+mRoom.getTitle());
        } else {
            getSupportActionBar().setTitle(mRoom.getTitle());
        }
    }

    /**
     * Fetches the room's data from the database and then starts the initialization of the chat.
     * @param roomID the chat room's id
     */

    private void getRoomData(String roomID) {
        DatabaseReference roomRef =
                FirebaseDatabase.getInstance().getReference().child("chatRooms").child(roomID);
        ValueEventListener roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mRoom = dataSnapshot.getValue(Room.class);
                initEditMessage(ref, mRoomID);
                initMessages(ref, mRoomID);
                hideProgbarIfNoMessages(ref, mRoomID);
                initActionBar();
                invalidateOptionsMenu();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadRoom:onCancelled", databaseError.toException());
                Toast.makeText(getApplicationContext(), "Room loading cancelled.", Toast.LENGTH_SHORT).show();
            }
        };
        roomRef.addListenerForSingleValueEvent(roomListener);
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

    /**
     * Adds a new member to this room (if this room is private and it should be if this method is called!)
     * @param membersEmail the email of the member to be added
     */

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

    /**
     * Shows a dialog of the members in this private(!) room.
     * @param memberEmails the emails of the members in this room
     */

    private void showMembers(ArrayList<String> memberEmails) {
        LayoutInflater li = LayoutInflater.from(this);
        final View dialogLO = li.inflate(R.layout.dialog_room_members, null);

        TextView membersText = (TextView) dialogLO.findViewById(R.id.membersText);
        membersText.setText("Room "+ mRoom.getTitle() +"'s members:");

        LinearLayout membersLO = (LinearLayout) dialogLO.findViewById(R.id.memberList);

        for(String member : memberEmails) {
            membersLO.addView(newMemberEmail(member));
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

    /**
     * Creates a new TextView to be added in the room's members dialog.
     * @param member the member (email) to be displayed.
     * @return a new TextView that contains the member's email
     */

    private TextView newMemberEmail(String member) {
        TextView emailView = new TextView(this);
        emailView.setText(member);
        emailView.setTextColor(Color.parseColor("#000000"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int emailMargin = HelperMethods.dpToPx(this, 10);
        params.setMargins(0, 0, 0, emailMargin);
        emailView.setLayoutParams(params);
        return emailView;
    }

    /**
     * Shows the room's id in a dialog.
     * The id can be copied to clipboard by pressing the copy-button next to the id.
     */

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

    /**
     * Gets this room's members from Firebase db.
     * (if this room is private and it should be if this method is called!)
     */

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

    /**
     * Toggles the room's favorite state.
     */

    private void toggleFavorite() {
        favoritePrefs = this.getSharedPreferences(Constants.favoritePrefsName, Context.MODE_PRIVATE);
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

    private Bitmap changeCurrentRoomLogo(Uri imageData) {
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

    /**
     * Makes a request for the user to pick a new room logo from user's gallery.
     */

    private void changeRoomLogo() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, MainActivity.GALLERY_PHOTO_REQUEST_CODE);
    }

    @Override
    public void onSendMessagePressed(User user) {
        openChatIfItExists(user);
    }

    /**
     * Checks whether there's a chat for current user and the user in param and either opens the chat
     * with getChatDataAndOpenRoom-method or creates a new one with createOne2OneChat-method.
     * @param user the user that the chat is to be created with
     */

    private void openChatIfItExists(final User user) {
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final String chatID1 = currentUser.getUid() + user.getUuid();
        final String chatID2 = user.getUuid() + currentUser.getUid();
        String path = "userData/"+ currentUser.getUid() + "/one2oneChats";
        DatabaseReference roomRef =
                FirebaseDatabase.getInstance().getReference(path);
        ValueEventListener roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.hasChild(chatID1)) {
                        getChatDataAndOpenRoom(chatID1);
                    } else if (dataSnapshot.hasChild(chatID2)) {
                        getChatDataAndOpenRoom(chatID2);
                    } else {
                        // create a new room
                        User cUser = new User(currentUser.getEmail(), currentUser.getDisplayName(), currentUser.getUid());
                        One2OneChat newChat = createOne2OneChat(chatID1, cUser, user);
                        openOne2OneChat(newChat.getId());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

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
     * When we know there is a chat with id chatid in database, this method fetches the chat data
     * and opens the room by calling the method openOne2OneChat(One2OneChat)
     * @param chatid the id of the existing chat room
     */

    private void getChatDataAndOpenRoom(final String chatid) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String path = "userData/" + currentUser.getUid() + "/one2oneChats/"+ chatid;
        DatabaseReference roomRef =
                FirebaseDatabase.getInstance().getReference(path);
        ValueEventListener roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    One2OneChat newChat = dataSnapshot.getValue(One2OneChat.class);
                    openOne2OneChat(newChat.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadRoom:onCancelled", databaseError.toException());
                Toast.makeText(getApplicationContext(), "Room loading cancelled.", Toast.LENGTH_SHORT).show();
            }
        };
        roomRef.addListenerForSingleValueEvent(roomListener);
    }


    private One2OneChat createOne2OneChat(String chatID, User currentUser, User user2) {
        try {
            One2OneChat newChat = new One2OneChat(chatID, currentUser, user2);
            // Sets the new chat id for the users to be easily later fetched
            sendNewChatToUser(newChat, currentUser.getUuid());
            sendNewChatToUser(newChat, user2.getUuid());

            return newChat;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendNewChatToUser(final One2OneChat newChat, final String userID) {
        DatabaseReference userDataRef =
                FirebaseDatabase.getInstance().getReference().child("userData").child(userID);

        /* Get the data inside userDataRef, if there's no "one2oneChats" child, set new value there with the new chat */

        ValueEventListener roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    DatabaseReference db = FirebaseDatabase.getInstance().getReference("userData")
                            .child(userID)
                            .child("one2oneChats");
                    db.child(newChat.getId()).setValue(newChat);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadRoom:onCancelled", databaseError.toException());
                Toast.makeText(getApplicationContext(), "Room loading cancelled.", Toast.LENGTH_SHORT).show();
            }
        };
        userDataRef.addListenerForSingleValueEvent(roomListener);
    }

    private void openOne2OneChat(String chatID) {
        try {
            Intent intent = new Intent(getApplicationContext(), ChatOne2OneActivity.class);
            Bundle b = new Bundle();
            b.putString("chatID", chatID);
            intent.putExtras(b);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageLongClick(ChatMessage message) {
        try {
            if(message.getUser() != null) {
                showUserInfo(message.getUser());
            } else {
                Toast.makeText(this, "Sorry this old message doesn't support user info.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void showUserInfo(User user) {
        try {
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction trans = manager.beginTransaction();
            // pop_exit doesn't seem to work here since the animation doesn't appear on back button pressed...
            trans.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);

            // Remove previous userInfoFragment if there is one
            Fragment possibleFrag = manager.findFragmentByTag("userInfoFrag");
            if(possibleFrag != null) {
                trans.remove(possibleFrag);
                manager.popBackStack();
            }

            UserInfoFragment userInfoFrag = UserInfoFragment.newInstance(user);
            trans.replace(R.id.userInfoPanel, userInfoFrag, "userInfoFrag");
            trans.addToBackStack(null);
            trans.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        favoritePrefs = this.getSharedPreferences(Constants.favoritePrefsName, Context.MODE_PRIVATE);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        if(mRoom != null) {
            MenuInflater inflater = getMenuInflater();
            if(mRoom.getMembers() == null) {
                inflater.inflate(R.menu.chat_public_actionbar_menu, menu);
            } else {
                inflater.inflate(R.menu.chat_private_actionbar_menu, menu);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.recordGif:
                recordGif(4.0f);
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
                    startVideoConverter(data, ref, mRoomID);
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
}
