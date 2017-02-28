package com.example.linda.giffychat.FeatureFragments;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.linda.giffychat.Constants;
import com.example.linda.giffychat.HelperMethods;
import com.example.linda.giffychat.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static com.example.linda.giffychat.R.id.changePasswordB;
import static com.example.linda.giffychat.R.id.messageBase;
import static com.example.linda.giffychat.R.layout.room;
import static com.facebook.login.widget.ProfilePictureView.TAG;
import static junit.runner.Version.id;


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

        initListeners();
        initColors();
    }

    private void initListeners() {
        ((ImageView) rootView.findViewById(R.id.profileClose)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFinish();
            }
        });

        ((TextView) rootView.findViewById(R.id.changePasswordB)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPasswordChangePrompt();
            }
        });
    }

    private void showPasswordChangePrompt() {
        new AlertDialog.Builder(getContext())
                .setTitle("Password reset")
                .setMessage("Do you want a password reset email to be sent to you?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        changePassword();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(R.drawable.ic_mail_outline)
                .show();
    }

    private void changePassword() {
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        FirebaseAuth.getInstance().sendPasswordResetEmail(userEmail)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Email has been sent!", Toast.LENGTH_SHORT).show();
                        } else {
                            try {
                                throw task.getException();
                            } catch (Exception e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void initColors() {
        String[] colors = getResources().getStringArray(R.array.colors);
        LinearLayout colorLO = (LinearLayout) rootView.findViewById(R.id.colorOptions);

        View resetColor = makeNewColor("#FAFAFA");
        GradientDrawable whiteBG = new GradientDrawable();
        whiteBG.setColor(0xFFFAFAFA); // material white
        whiteBG.setStroke(1, 0xFF000000); //black border with full opacity
        resetColor.setBackground(whiteBG);
        colorLO.addView(resetColor);

        for(String color : colors) {
            View newColor = makeNewColor(color);
            colorLO.addView(newColor);
        }
    }

    /**
     * Makes a new color to be option in
     * @param colorHex
     * @return
     */

    private View makeNewColor(String colorHex) {
        View newColor = new View(getContext());
        int widthHeight = HelperMethods.dpToPx(getContext(), 30);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(widthHeight, widthHeight);
        params.setMargins(HelperMethods.dpToPx(getContext(), 1), 0, 0, 0);
        newColor.setBackgroundColor(Color.parseColor(colorHex));
        newColor.setTag(colorHex);
        newColor.setLayoutParams(params);

        newColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String color = (String) view.getTag();
                ((RelativeLayout) rootView.findViewById(R.id.profileBase))
                        .setBackgroundColor(Color.parseColor(color));
                currentColor = color;
            }
        });
        return newColor;
    }

    private void updateUserColor(String color) {
        String userid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        DatabaseReference chatRoomRef = ref.child("userData");
        chatRoomRef.child(userid).child("color").setValue(color.substring(1));
        Constants.addUserColor(userid, color);
        Toast.makeText(getActivity(), "Color updated!", Toast.LENGTH_SHORT).show();
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
