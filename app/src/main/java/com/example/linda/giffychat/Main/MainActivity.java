package com.example.linda.giffychat.Main;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.linda.giffychat.ChatRoom.ChatRoomActivity;
import com.example.linda.giffychat.Constants;
import com.example.linda.giffychat.Entity.Room;
import com.example.linda.giffychat.ExceptionHandler;
import com.example.linda.giffychat.FeatureFragments.SearchFragment;
import com.example.linda.giffychat.FeatureFragments.UserProfileFragment;
import com.example.linda.giffychat.HelperMethods;
import com.example.linda.giffychat.Login.StartingActivity;
import com.example.linda.giffychat.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.bitmap;
import static android.R.attr.dial;
import static android.view.View.GONE;
import static com.example.linda.giffychat.HelperMethods.hash;
import static com.example.linda.giffychat.R.id.emailText;
import static java.lang.System.currentTimeMillis;
import static junit.runner.Version.id;

/**
 * The activity that's created when the user opens the app when logged in.
 * Contains a toolbar with search, profile and logout functionalities and a RoomTabFragments in a ViewPager.
 */

public class MainActivity extends AppCompatActivity
        implements RoomTabFragment.onRoomOpenListener, SearchFragment.onOpenRoomListener, UserProfileFragment.onFinishListener {

    private static final String TAG = "MainActivity";
    public static final int GALLERY_PHOTO_REQUEST_CODE = 1;
    public static final String favoritePrefsName = "favoritePrefs";
    private SharedPreferences favoritePrefs;

    private RoomTabAdapter mTabAdapter;
    private ViewPager mViewPager;

    private ImageView roomImage;
    private String base64roomImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        // usercolors get reset on app restart
        if(Constants.userColors == null) {
            Constants.initUserColors();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this,
                MainActivity.class));

        ((FloatingActionButton) findViewById(R.id.addRoomB)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeNewRoom();
            }
        });

        initTabStructure();
    }

    /**
     * Adds a new room to room db.
     * @param room the room to be added
     */

    private void addRoom(Room room) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        DatabaseReference chatRoomRef = ref.child("chatRooms");
        chatRoomRef.child(room.getId()).setValue(room);
    }

    private void signOut() {
        startActivity(new Intent(this, StartingActivity.class));
        finish();
    }

    public void openSearch() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction trans = manager.beginTransaction();

        SearchFragment searchFrag = SearchFragment.newInstance();
        trans.add(R.id.search_fragment, searchFrag, "searchFrag");
        trans.addToBackStack(null);
        trans.commit();
    }

    private void openUserProfile() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction trans = manager.beginTransaction();

        UserProfileFragment profileFrag = UserProfileFragment.newInstance();
        trans.add(R.id.profile_fragment, profileFrag, "profileFrag");
        trans.addToBackStack(null);
        trans.commit();
    }

    /**
     * Removes the UserProfileFragment if it's attached to the activity.
     */

    public void onProfileClose() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction trans = manager.beginTransaction();
        UserProfileFragment profileFrag = (UserProfileFragment) manager.findFragmentById(R.id.profile_fragment);
        if(profileFrag != null) {
            trans.remove(profileFrag).commit();
        }
    }

    /**
     * Removes the SearchFragment if it's attached to the activity.
     */

    private void onSearchClose() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction trans = manager.beginTransaction();
        SearchFragment searchFrag = (SearchFragment) manager.findFragmentById(R.id.search_fragment);
        if(searchFrag != null) {
            trans.remove(searchFrag).commit();
        }
    }

    /**
     * Shows a room creation dialog so the user can create a new room.
     */

    private void makeNewRoom() {
        LayoutInflater li = LayoutInflater.from(this);
        final View dialogLO = li.inflate(R.layout.dialog_make_room, null);
        final LinearLayout membersLO = (LinearLayout) dialogLO.findViewById(R.id.memberLO);

        final RelativeLayout emailsLO = (RelativeLayout) dialogLO.findViewById(R.id.emailsLO);
        final ImageView addMemberButton = (ImageView) dialogLO.findViewById(R.id.addMemberB);
        final ImageView removeMemberButton = (ImageView) dialogLO.findViewById(R.id.removeMemberB);
        final CheckBox box = ((CheckBox) dialogLO.findViewById(R.id.privateCheck));
        roomImage = (ImageView) dialogLO.findViewById(R.id.addPhotoB);
        final RelativeLayout privateLOCheck = ((RelativeLayout) dialogLO.findViewById(R.id.privateCheckLO));

        setRoomCreationListeners(membersLO, emailsLO, addMemberButton, removeMemberButton, box, roomImage, privateLOCheck);

        final AlertDialog d = new AlertDialog.Builder(this)
                .setView(dialogLO)
                .setPositiveButton("CREATE ROOM", null)
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
                        String roomName = ((EditText) dialogLO.findViewById(R.id.roomNameInput))
                                .getText().toString().trim();

                        if(!roomName.isEmpty()) {

                            ArrayList<String> emails = new ArrayList<String>();
                            for (int i = 0; i < membersLO.getChildCount(); i++) {
                                EditText emailInput = (EditText) membersLO.getChildAt(i);
                                String email = emailInput.getText().toString().trim();
                                if(!email.isEmpty()) {
                                    emails.add(email);
                                }
                            }
                            Room newRoom = createNewRoom(emails, roomName, box.isChecked());

                            favoriteRoom(newRoom);
                            addRoom(newRoom);
                            openRoomIfMember(newRoom);

                            d.dismiss();
                        } else {
                            Toast.makeText(getApplicationContext(), "Give your room a name!", Toast.LENGTH_SHORT).show();
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
     * A method for setting listeners for the views used in room creating dialog.
     * @param membersLO The LinearLayout where the email EditText-views are located
     * @param emailsLO the RelativeLayout which appears when privatebox is checked.
     * @param addMemberButton a Button for adding an EditText field for a new email address.
     * @param removeMemberButton a Button for removing an EditText field when there's 2 or more fields.
     * @param box The private checkbox just for visual effect.
     * @param roomImage An ImageView for a preview of the room image
     * @param privateLOCheck Both the checkbox and the TextView "Private". When pressed, toggles the checkbox.
     */

    private void setRoomCreationListeners(final LinearLayout membersLO, final RelativeLayout emailsLO,
                                          final ImageView addMemberButton, final ImageView removeMemberButton,
                                          final CheckBox box, final ImageView roomImage,
                                          final RelativeLayout privateLOCheck) {

        addMemberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                membersLO.addView(newMemberInput());
                removeMemberButton.setVisibility(View.VISIBLE);
            }
        });

        removeMemberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                membersLO.removeViewAt(membersLO.getChildCount()-1);
                if(membersLO.getChildCount() == 1) {
                    removeMemberButton.setVisibility(GONE);
                }
            }
        });

        roomImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestRoomLogo();
            }
        });

        privateLOCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(box.isChecked()) {
                    box.setChecked(false);
                    if(membersLO.getChildCount() > 0) {
                        membersLO.removeAllViews();
                    }
                    emailsLO.setVisibility(View.GONE);
                    removeMemberButton.setVisibility(View.GONE);
                } else {
                    box.setChecked(true);
                    membersLO.addView(newMemberInput());
                    emailsLO.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * Creates a new room with data given as parameters. If the room is set to private but no members added,
     * only the room creator can access it.
     * @param emails The peoples' emails that can access the room
     * @param roomName The name of the room
     * @param isPrivate tells if the room is private (private-box checked)
     * @return the newly created room
     */

    private Room createNewRoom(ArrayList<String> emails, String roomName, boolean isPrivate) {
        Room newRoom;
        String id = generateID();
        if(isPrivate) {
            String ownEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            if(!emails.contains(ownEmail)) {
                emails.add(ownEmail);
            }

            newRoom = new Room(id, roomName, emails, base64roomImage);
        } else {
            newRoom = new Room(id, roomName, null, base64roomImage);
        }

        return newRoom;
    }

    /**
     * Adds a room to user's favorites.
     * @param room the room to be added to favorites
     */

    private void favoriteRoom(Room room) {
        favoritePrefs = getApplication()
                .getSharedPreferences(MainActivity.favoritePrefsName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = favoritePrefs.edit();
        editor.putBoolean(room.getId(), true);
        editor.apply();
    }

    /**
     * Starts an intent that lets the user to choose an image from their gallery.
     */

    private void requestRoomLogo() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, GALLERY_PHOTO_REQUEST_CODE);
    }

    /**
     * Creates a new EditText for the room creation dialog to be used as a new member's email input field.
     * @return The new email EditText
     */

    private EditText newMemberInput() {
        EditText newMember = new EditText(getApplicationContext());

        newMember.setHint("example@example.com");
        newMember.setSingleLine();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        newMember.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        newMember.setLayoutParams(params);
        newMember.requestFocus();
        return newMember;
    }

    /**
     * Opens a chat room.
     * @param room The room to be opened.
     */

    public void openRoom(Room room) {
        Intent intent = new Intent(getApplicationContext(), ChatRoomActivity.class);
        Bundle b = new Bundle();
        b.putString("roomID", room.getId());
        if(room.getBase64RoomImage() != null) {
            b.putString("base64icon", room.getBase64RoomImage());
        }
        intent.putExtras(b);
        startActivity(intent);
    }

    /**
     * Opens a room if the user is a member of that room, or if the room is public.
     * @param room the room to be opened
     */

    public void openRoomIfMember(final Room room) {

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(room.getId());
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChild("members")) {
                    String ownEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    for (DataSnapshot ds : snapshot.child("members").getChildren()) {
                        if(ds.getValue().equals(ownEmail)) {
                            onSearchClose();
                            openRoom(room);
                            return;
                        }
                    }
                    Toast.makeText(getApplicationContext(),
                            "You are not a member of this private room!", Toast.LENGTH_SHORT).show();
                } else {
                    onSearchClose();
                    openRoom(room);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        });
    }

    private String generateID() {
        // We'll use the hash method so the id could be shorter.
        return hash(Long.toString(currentTimeMillis()));
    }

    /**
     * Initializes the tab structure.
     */

    private void initTabStructure() {
        mTabAdapter = new RoomTabAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mTabAdapter);

        setTabListeners(((RelativeLayout) findViewById(R.id.tab0base)));
        setTabListeners(((RelativeLayout) findViewById(R.id.tab1base)));

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                TextView tab0 = ((TextView) findViewById(R.id.tab0));
                TextView tab1 = ((TextView) findViewById(R.id.tab1));
                tab0.setBackgroundResource(0);
                tab1.setBackgroundResource(0);

                switch (position) {
                    case 0:
                        tab0.setBackgroundResource(R.drawable.text_border);
                        break;
                    case 1:
                        tab1.setBackgroundResource(R.drawable.text_border);
                        break;
                    default:
                        Log.d(TAG, "More tabs than initialized!!!");
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    private void setTabListeners(RelativeLayout tab) {
        tab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewPager.setCurrentItem(Integer.parseInt((String) view.getTag()));
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_actionbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                signOut();
                return true;
            case R.id.search:
                openSearch();
                return true;
            case R.id.ownProfile:
                openUserProfile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == GALLERY_PHOTO_REQUEST_CODE) {
            if(resultCode == RESULT_OK){
                Uri selectedImage = data.getData();

                //roomImage.setAdjustViewBounds(true);
                //roomImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

                try {
                    Bitmap bmp = HelperMethods.getBitmapFromUri(selectedImage, this);
                    Bitmap compressedBtm = HelperMethods.getResizedBitmap(bmp, 200);
                    Bitmap ccBtm = HelperMethods.centerCropBitmap(compressedBtm);
                    RoundedBitmapDrawable rbd = HelperMethods.giveBitmapRoundedCorners(ccBtm, this);
                    roomImage.setImageDrawable(rbd);
                    base64roomImage = HelperMethods.getBase64FromBitmap(ccBtm);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }
}
