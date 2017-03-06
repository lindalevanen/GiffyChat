package com.example.linda.giffychat.ChatRoom;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialcamera.MaterialCamera;
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

    private void getRoomData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference roomRef =
                FirebaseDatabase.getInstance().getReference("userData/"+user.getUid()+"/one2oneChats/" + mChatID);
        ValueEventListener roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    mChat = dataSnapshot.getValue(One2OneChat.class);
                    initUI(ref, mChatID);
                    initMessages(ref, mChatID);
                    hideProgbarIfNoMessages(ref, mChatID);
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

    private void initActionBar() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(mChat.getMember1().getUuid().equals(currentUser.getUid())) {
            getSupportActionBar().setTitle(mChat.getMember2().getUserName());
        } else {
            getSupportActionBar().setTitle(mChat.getMember1().getUserName());
        }

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
