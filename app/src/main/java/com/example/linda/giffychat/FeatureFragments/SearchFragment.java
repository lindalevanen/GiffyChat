package com.example.linda.giffychat.FeatureFragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.linda.giffychat.Entity.Room;
import com.example.linda.giffychat.HelperMethods;
import com.example.linda.giffychat.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static com.example.linda.giffychat.R.layout.room;

/**
 * Creates a view for room searching and searches a room by user input room id.
 */

public class SearchFragment extends Fragment {

    private static final String TAG = SearchFragment.class.getSimpleName();
    private onOpenRoomListener mListener;

    private View rootView;

    private EditText mRoomIdInput;
    private Room mFoundRoom;

    public SearchFragment() {}

    public interface onOpenRoomListener {
        void openRoomIfMember(Room room);
    }

    public static SearchFragment newInstance() {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_search, container, false);

        mRoomIdInput = (EditText) rootView.findViewById(R.id.searchInput);
        mRoomIdInput.requestFocus();

        initListeners();

        return rootView;
    }

    public void initListeners() {
        ((ImageView) rootView.findViewById(R.id.searchB)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = mRoomIdInput.getText().toString().trim();
                if(!id.isEmpty()) {
                    searchForRoom(id);
                }
            }
        });

        ((TextView) rootView.findViewById(R.id.roomTitle)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mFoundRoom != null) {
                    openRoom(mFoundRoom);
                } else {
                    Log.d(TAG, "The roomTitle-TextView shouldn't be visible when the room hasn't been found.");
                }

            }
        });
    }

    private void searchForRoom(final String id) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("chatRooms");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if(id.equals((String) ds.getKey())) {
                        mFoundRoom = (Room) ds.getValue(Room.class);
                        TextView roomView = (TextView) rootView.findViewById(R.id.roomTitle);
                        ImageView iconView = (ImageView) rootView.findViewById(R.id.roomIcon);

                        if(mFoundRoom.getBase64RoomImage() != null) {
                            Bitmap decoded = HelperMethods.getBitmapFromBase64(mFoundRoom.getBase64RoomImage());
                            Bitmap ccBtm = HelperMethods.centerCropBitmap(decoded);
                            RoundedBitmapDrawable dr = HelperMethods.giveBitmapRoundedCorners(ccBtm, getContext());
                            iconView.setImageDrawable(dr);
                        } else {
                            iconView.setImageResource(R.drawable.ic_giffy);
                        }

                        roomView.setText(mFoundRoom.getTitle());
                        return;
                    }
                }
                Toast.makeText(getContext(), "No room found with that ID.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        });
    }

    public void openRoom(Room room) {
        if (mListener != null) {
            mListener.openRoomIfMember(room);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //mRoomIdInput.requestFocus();
        if (context instanceof onOpenRoomListener) {
            mListener = (onOpenRoomListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement onOpenRoomListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
