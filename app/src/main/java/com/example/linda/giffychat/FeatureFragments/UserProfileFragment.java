package com.example.linda.giffychat.FeatureFragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.linda.giffychat.R;


public class UserProfileFragment extends Fragment {

    private onFinishListener mListener;

    private View rootView;

    public UserProfileFragment() {
        // Required empty public constructor
    }

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
        View rootView = inflater.inflate(R.layout.fragment_user_profile, container, false);

        //TODO: add correct values to user profile

        return rootView;
    }

    public void onFinish() {
        if (mListener != null) {
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
