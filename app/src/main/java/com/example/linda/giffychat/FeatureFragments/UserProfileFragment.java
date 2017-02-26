package com.example.linda.giffychat.FeatureFragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.linda.giffychat.HelperMethods;
import com.example.linda.giffychat.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.example.linda.giffychat.R.layout.room;


public class UserProfileFragment extends Fragment {

    private onFinishListener mListener;
    private View rootView;

    private String currentColor;

    public UserProfileFragment() {}

    public interface onFinishListener {
        void onProfileClose();
    }

    public static UserProfileFragment newInstance() {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_user_profile, container, false);
        initUI();
        return rootView;
    }

    private void initUI() {
        TextView usernameView = (TextView) rootView.findViewById(R.id.userNameTitle);
        TextView userEmailView = (TextView) rootView.findViewById(R.id.userEmail);
        userEmailView.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        usernameView.setText(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());

        ((ImageView) rootView.findViewById(R.id.profileClose)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFinish();
            }
        });

        initColors();
    }

    private void initColors() {
        String[] colors = getResources().getStringArray(R.array.colors);
        LinearLayout colorLO = (LinearLayout) rootView.findViewById(R.id.colorOptions);
        for(String color : colors) {
            View newColor = new View(getContext());
            int widthHeight = HelperMethods.dpToPx(getContext(), 30);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(widthHeight, widthHeight);
            params.setMargins(HelperMethods.dpToPx(getContext(), 1), 0, 0, 0);
            newColor.setBackgroundColor(Color.parseColor(color));
            newColor.setTag(color);

            newColor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String color = (String) view.getTag();
                    ((RelativeLayout) rootView.findViewById(R.id.profileBase))
                            .setBackgroundColor(Color.parseColor(color));
                    currentColor = color;
                }
            });

            colorLO.addView(newColor, params);
        }
    }

    public void updateUserColor(String color) {
        String userid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        DatabaseReference chatRoomRef = ref.child("userData");
        chatRoomRef.child(userid).child("color").setValue(color.substring(1));
    }

    public void onFinish() {
        if (mListener != null) {
            if(currentColor != null) {
                updateUserColor(currentColor);
            }
            mListener.onProfileClose();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof onFinishListener) {
            mListener = (onFinishListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement onFinishListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
