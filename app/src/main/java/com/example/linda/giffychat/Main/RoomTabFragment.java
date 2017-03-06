package com.example.linda.giffychat.Main;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.linda.giffychat.Entity.One2OneChat;
import com.example.linda.giffychat.Entity.Room;
import com.example.linda.giffychat.HelperMethods;
import com.example.linda.giffychat.R;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;

import static com.example.linda.giffychat.R.layout.list_room;

public class RoomTabFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = RoomTabFragment.class.getSimpleName();

    private View rootView;
    private RoomTabFragment.onRoomOpenListener mListener;
    private int tabNo;

    private ListView roomList;
    private ProgressBar progressBar;
    private TextView noContactsText;
    private FirebaseListAdapter<Room> globalListAdapter;
    private ArrayAdapter<Room> favoriteListAdapter;
    private FirebaseListAdapter<One2OneChat> contactListAdapter;
    private SharedPreferences favoritePrefs;
    private SharedPreferences booleanPrefs;

    public RoomTabFragment() {}

    public interface onRoomOpenListener {
        void openRoomIfMember(Room room);
        void openOne2OneChat(One2OneChat chat);
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static RoomTabFragment newInstance(int sectionNumber) {
        RoomTabFragment fragment = new RoomTabFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_tab, container, false);
        if(getArguments() != null) {
            tabNo = getArguments().getInt(ARG_SECTION_NUMBER);
            displayRooms();
        } else {
            Log.d(TAG, "RoomTabFragment's args not initialized! Idk how this would be possible but still");
        }
        return rootView;
    }

    private void displayRooms() {
        roomList = (ListView) rootView.findViewById(R.id.roomList);
        progressBar = (ProgressBar) rootView.findViewById(R.id.mainProgBar);
        progressBar.setVisibility(View.VISIBLE);

        switch (tabNo) {
            case 1:
                initGlobalRooms();
                break;
            case 2:
                initfavRooms();
                break;
            case 3:
                initContacts();
                break;
            default:
                Log.d(TAG, "There's more tabs than initialized!");
                break;
        }

        roomList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(tabNo == 1 || tabNo == 2) {
                    Room room = (Room) adapterView.getItemAtPosition(i);
                    openRoomIfMember(room);
                } else if(tabNo == 3) {
                    One2OneChat chat = (One2OneChat) adapterView.getItemAtPosition(i);
                    openOne2OneChat(chat);
                }

            }
        });
    }

    /**
     * Inits the global rooms tab.
     */

    private void initGlobalRooms() {

        globalListAdapter = new FirebaseListAdapter<Room>(getActivity(), Room.class,
                R.layout.list_room, FirebaseDatabase.getInstance().getReference().child("chatRooms")) {
            @Override
            protected void populateView(View v, final Room room, int position) {
                progressBar.setVisibility(View.INVISIBLE);
                TextView titleView = (TextView) v.findViewById(R.id.roomTitle);
                ImageView iconView = (ImageView) v.findViewById(R.id.roomIcon);

                iconView.setAdjustViewBounds(true);
                iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                if(room.getBase64RoomImage() != null) {

                    Bitmap decoded = HelperMethods.getBitmapFromBase64(room.getBase64RoomImage());
                    Bitmap ccBtm = HelperMethods.centerCropBitmap(decoded);
                    RoundedBitmapDrawable dr = HelperMethods.giveBitmapRoundedCorners(ccBtm, getContext());

                    iconView.setImageDrawable(dr);
                } else {
                    iconView.setImageResource(R.drawable.ic_giffy);
                }

                titleView.setText(room.getTitle());
            }
        };

        roomList.setAdapter(globalListAdapter);
    }

    /**
     * Inits the favorite rooms tab.
     */

    private void initfavRooms() {
        TextView noFavsText = (TextView) rootView.findViewById(R.id.noFavsText);
        noFavsText.setVisibility(View.GONE);

        favoritePrefs = getContext().getSharedPreferences(MainActivity.favoritePrefsName, Context.MODE_PRIVATE);
        Map<String, ?> favorites = favoritePrefs.getAll();
        ArrayList<String> favoriteRoomIds = new ArrayList<String>();

        for (Map.Entry<String, ?> entry : favorites.entrySet()) {
            favoriteRoomIds.add(entry.getKey());
        }
        if (favoriteRoomIds.isEmpty()) {
            progressBar.setVisibility(View.INVISIBLE);
            noFavsText.setVisibility(View.VISIBLE);
            favoriteListAdapter = new FavoriteListAdapter(getContext(), new ArrayList<Room>(), progressBar);
            roomList.setAdapter(favoriteListAdapter);
        } else {
            getFavRooms(favoriteRoomIds);
        }
    }

    /**
     * Gets favorite rooms from chatRooms reference by iterating them and checking if the id is of a favorite room.
     * Might change this solution, not efficient enough when there are a lot of rooms.
     * @param ids
     */

    public void getFavRooms(final ArrayList<String> ids) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("chatRooms");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArrayList<Room> favRooms = new ArrayList<Room>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if(ids.contains((String) ds.getKey())) {
                        Room favRoom = (Room) ds.getValue(Room.class);
                        favRooms.add(favRoom);
                    }
                }
                favoriteListAdapter = new FavoriteListAdapter(getContext(), favRooms, progressBar);
                roomList.setAdapter(favoriteListAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        });
    }

    /**
     * Initializes the contacts tab.
     */

    private void initContacts() {
        noContactsText = (TextView) rootView.findViewById(R.id.noContactsText);
        noContactsText.setVisibility(View.GONE);

        checkEmptyContacts();

        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final String path = "userData/" + user.getUid() + "/one2oneChats";

        contactListAdapter = new FirebaseListAdapter<One2OneChat>(getActivity(), One2OneChat.class,
                R.layout.list_contact, FirebaseDatabase.getInstance().getReference().child(path)) {
            @Override
            protected void populateView(View v, final One2OneChat chat, int position) {
                progressBar.setVisibility(View.INVISIBLE);
                noContactsText.setVisibility(View.INVISIBLE);
                TextView contactText = (TextView) v.findViewById(R.id.contactText);

                if(chat.getMember1().getUuid().equals(user.getUid())) {
                    contactText.setText(chat.getMember2().getUserName());
                } else {
                    contactText.setText(chat.getMember1().getUserName());
                }
            }
        };

        contactListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                if(contactListAdapter.getCount() == 0) {
                    noContactsText.setVisibility(View.VISIBLE);
                }
                super.onChanged();
            }
        });

        roomList.setAdapter(contactListAdapter);
    }

    /**
     * A little gum solution, there might be a better way to know whether or not the user has contacts.
     * This method hides the progressBar and shows a "no contacts"-textview if the user doesn't have contacts.
     */

    private void checkEmptyContacts() {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference rootRef = FirebaseDatabase.getInstance()
                .getReference("userData/" + user.getUid());
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(!snapshot.hasChild("one2oneChats")) {
                    progressBar.setVisibility(View.GONE);
                    noContactsText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        });
    }


    private void openRoomIfMember(Room room) {
        if(mListener != null) {
            mListener.openRoomIfMember(room);
        } else {
            Log.d(getTag(), "listener in RegisterFragment is null");
        }
    }

    private void openOne2OneChat(One2OneChat chat) {
        if(mListener != null) {
            mListener.openOne2OneChat(chat);
        } else {
            Log.d(getTag(), "listener in RegisterFragment is null");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof RoomTabFragment.onRoomOpenListener) {
            mListener = (RoomTabFragment.onRoomOpenListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement onRoomOpenListener");
        }
    }

    /**
     * Refresh the Favorite tab when resumed.
     */

    @Override
    public void onResume() {
        if(tabNo == 2) {
            displayRooms();
        }
        super.onResume();
    }

}
