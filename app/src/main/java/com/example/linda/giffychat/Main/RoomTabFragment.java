package com.example.linda.giffychat.Main;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.util.Base64;
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

import com.example.linda.giffychat.Entity.Room;
import com.example.linda.giffychat.HelperMethods;
import com.example.linda.giffychat.R;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;

import static com.example.linda.giffychat.R.layout.room;

public class RoomTabFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "RoomTabFragment";

    private View rootView;
    private RoomTabFragment.onRoomOpenListener mListener;

    private ListView roomList;
    private ProgressBar progressBar;
    private FirebaseListAdapter<Room> globalListAdapter;
    private ArrayAdapter<Room> favoriteListAdapter;
    private SharedPreferences favoritePrefs;

    public RoomTabFragment() {}

    public interface onRoomOpenListener {
        void openRoomIfMember(Room room);
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
        displayRooms();
        return rootView;
    }

    private void displayRooms() {
        roomList = (ListView) rootView.findViewById(R.id.roomList);
        progressBar = (ProgressBar) rootView.findViewById(R.id.mainProgBar);
        if(getArguments().getInt(ARG_SECTION_NUMBER) == 1) {

            globalListAdapter = new FirebaseListAdapter<Room>(getActivity(), Room.class,
                    room, FirebaseDatabase.getInstance().getReference().child("chatRooms")) {
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
                        RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(getResources(), ccBtm);
                        dr.setCornerRadius(10f);

                        iconView.setImageDrawable(dr);
                    } else {
                        iconView.setImageResource(R.drawable.ic_giffy);
                    }

                    titleView.setText(room.getTitle());
                }
            };

            roomList.setAdapter(globalListAdapter);

        } else if(getArguments().getInt(ARG_SECTION_NUMBER) == 2) {

            favoritePrefs = getContext().getSharedPreferences(MainActivity.favoritePrefsName, Context.MODE_PRIVATE);
            Map<String, ?> favorites = favoritePrefs.getAll();
            ArrayList<String> favoriteRoomIds = new ArrayList<String>();

            for(Map.Entry<String,?> entry : favorites.entrySet()){
                favoriteRoomIds.add(entry.getKey());
            }

            getFavRooms(favoriteRoomIds);
        } else {
            Log.d(TAG, "There's more tabs than initialized!");
        }

        roomList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Room room = (Room) adapterView.getItemAtPosition(i);
                openRoomIfMember(room);
            }
        });
    }

    public void getFavRooms(final ArrayList<String> ids) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("chatRooms");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String ownEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
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

    private void openRoomIfMember(Room room) {
        if(mListener != null) {
            mListener.openRoomIfMember(room);
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
        if(this.getArguments().getInt(ARG_SECTION_NUMBER) == 2) {
            displayRooms();
        }
        super.onResume();
    }

}
