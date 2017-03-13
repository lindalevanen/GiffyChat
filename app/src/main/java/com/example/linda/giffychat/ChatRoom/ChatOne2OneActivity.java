package com.example.linda.giffychat.ChatRoom;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialcamera.MaterialCamera;
import com.example.linda.giffychat.Constants;
import com.example.linda.giffychat.Entity.ChatMessage;
import com.example.linda.giffychat.Entity.One2OneChat;
import com.example.linda.giffychat.Entity.User;
import com.example.linda.giffychat.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A chat that has only 2 members and is created by the other one sending a private message to the other one.
 * (by longpressing a chat message in a chat list_room and pressing the message button)
 * (and later possibly also by searching for a user)
 */

public class ChatOne2OneActivity extends ChatActivity {

    private static final String TAG = ChatOne2OneActivity.class.getSimpleName();
    private static final String ref = "one2oneMessages";

    public String mChatID;
    public One2OneChat mChat;

    @Override
    public void initActivity() {
        Bundle b = getIntent().getExtras();
        if(b != null) {
            mChatID = b.getString("chatID");
            if(mChatID != null) {
                getRoomData(); // remember to change actionbar title after fetcing list_room data
            } else {
                Log.d(TAG, "ChatOne2OneAcivity created without chatID! Can't initialize.");
            }
        } else {
            Log.d(TAG, "Activity started incorrectly, no extras in intent.");
        }
    }

    /**
     * Update the message amuont for both users.
     * Not cool that there's duplicates of all of the one2oneChats' metadata, but gonna go with this,
     * since it's a lot easier to then set the reference to contacts-adapter.
     * TODO: change this if time, make a one2oneChats-reference where is all the metadata of the chats,
     * TODO: and put only chatID's to userData... Put the reference to userData and
     * TODO: when the contacts tab is being built, fetch the metadata from one2oneChats according to chatID.
     * @param amount the new amount of messages in this chat.
     */

    @Override
    protected void updateMessageAmount(int amount) {
        String path1 = "userData/"+mChat.getMember1().getUuid()+"/one2oneChats/" + mChatID;
        DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference(path1);
        ref1.child("messageCount").setValue(amount);

        String path2 = "userData/"+mChat.getMember2().getUuid()+"/one2oneChats/" + mChatID;
        DatabaseReference ref2 = FirebaseDatabase.getInstance().getReference(path2);
        ref2.child("messageCount").setValue(amount);
    }

    private void getRoomData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference roomRef =
                FirebaseDatabase.getInstance().getReference("userData/"+user.getUid()+"/one2oneChats/" + mChatID);
        ValueEventListener roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    mChat = dataSnapshot.getValue(One2OneChat.class);
                    initEditMessage(ref, mChatID);
                    initMessages(ref, mChatID);
                    hideProgbarIfNoMessages(ref, mChatID);
                    initChatUID();
                    initActionBar();
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

    private void initChatUID() {
        String ownUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if(mChat.getMember1().getUuid().equals(ownUid)) {
            getPartnerPlayerID(mChat.getMember2().getUuid());
        } else {
            getPartnerPlayerID(mChat.getMember1().getUuid());
        }
    }

    private void getPartnerPlayerID(final String FBUserID) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance()
                .getReference("userData/" + FBUserID + "/playerID");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    Constants.partnerPlayerID = (String) snapshot.getValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        });
    }

    private void initActionBar() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(mChat.getMember1().getUuid().equals(currentUser.getUid())) {
            getSupportActionBar().setTitle(mChat.getMember2().getUserName());
        } else {
            getSupportActionBar().setTitle(mChat.getMember1().getUserName());
        }
    }

    /**
     * Sends a notification to the user that current user is chatting with.
     * @param message the chat message to be shown as a notification
     * @param isGif boolean whether the notification should be a notification of a received gif
     */

    @Override
    public void sendNotification(String message, boolean isGif) {
        if(Constants.partnerPlayerID != null) {
            String sender = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            try {
                String messageContent;
                if(isGif) {
                    messageContent = sender + " has sent you a gif!";
                } else {
                    messageContent = sender + ": "+ message;
                }

                JSONObject notificationContent =
                        new JSONObject("{'contents': {'en': '"+ messageContent +"'}, " +
                                       "'data':  {'chatID': " + mChatID + "}, " +
                                       "'include_player_ids': ['" + Constants.partnerPlayerID + "']}");

                OneSignal.postNotification(
                        notificationContent,
                        new OneSignal.PostNotificationResponseHandler() {
                            /* for debug */
                            @Override
                            public void onSuccess(JSONObject response) {
                                Log.i("OneSignalExample", "postNotification Success: " + response.toString());
                            }
                            @Override
                            public void onFailure(JSONObject response) {
                                Log.e("OneSignalExample", "postNotification Failure: " + response.toString());
                            }
                        });

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Sending notification not possible to user that doesn't have a player id.");
        }
    }

    @Override
    public void onGifSent() {
        sendNotification(" ", true);
    }

    /* These two following methods are only used in chat rooms. */

    @Override
    public void onSendMessagePressed(User user) {}
    @Override
    public void onMessageLongClick(ChatMessage message) {}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_one2one_actionbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.recordGif:
                recordGif(4.0f);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        Constants.partnerPlayerID = null;
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Received recording or error from MaterialCamera
        if(resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAMERA_RQ:
                    startVideoConverter(data, ref, mChatID);
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
