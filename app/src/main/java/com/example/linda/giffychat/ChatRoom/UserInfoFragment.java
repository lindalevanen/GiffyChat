package com.example.linda.giffychat.ChatRoom;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.linda.giffychat.Entity.User;
import com.example.linda.giffychat.Main.MainActivity;
import com.example.linda.giffychat.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserInfoFragment extends Fragment {
    private static final String USERNAME = "username";
    private static final String USEREMAIL = "email";
    private static final String USERID = "uuid";

    private static final String TAG = UserInfoFragment.class.getSimpleName();
    private onButtonPressedListener mListener;
    private View rootView;

    private User user;

    public UserInfoFragment() {}

    public interface onButtonPressedListener {
        void onSendMessagePressed(User user);
    }

    public static UserInfoFragment newInstance(User user) {
        UserInfoFragment fragment = new UserInfoFragment();
        Bundle args = new Bundle();
        args.putString(USEREMAIL, user.getEmail());
        args.putString(USERNAME, user.getUserName());
        args.putString(USERID, user.getUuid());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if(args != null) {
            String email = args.getString(USEREMAIL);
            String userName = args.getString(USERNAME);
            String uuid = args.getString(USERID);
            user = new User(email, userName, uuid);
        } else {
            Log.d(TAG, "Fragment created without setting a new instance of it.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_user_info, container, false);

        ((TextView) rootView.findViewById(R.id.userName)).setText(user.getUserName());
        initMessageButton();

        return rootView;
    }

    private void initMessageButton() {
        ImageView messageButton = (ImageView) rootView.findViewById(R.id.messageB);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser.getUid().equals(user.getUuid())) {
            /* The user can't send a message to themselves, so we'll hide messagebutton */
            messageButton.setVisibility(View.GONE);
        } else {
            messageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onSendMessagePressed(user);
                }
            });
        }
    }

    public void onSendMessagePressed(User user) {
        if (mListener != null) {
            mListener.onSendMessagePressed(user);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof onButtonPressedListener) {
            mListener = (onButtonPressedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement onButtonPressedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
