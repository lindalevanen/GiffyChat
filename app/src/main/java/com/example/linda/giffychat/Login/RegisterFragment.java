package com.example.linda.giffychat.Login;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.linda.giffychat.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Creates a view for user registering.
 */

public class RegisterFragment extends Fragment {

    private onRegisterListener mListener;

    private String mUserName;
    private String mEmail;
    private String mPassword;

    private View view;

    public RegisterFragment() {}

    public interface onRegisterListener {
        void logIn();
    }

    public static RegisterFragment newInstance() {
        RegisterFragment fragment = new RegisterFragment();
        //Bundle args = new Bundle();
        //fragment.setArguments(args);
        return fragment;
    }

    /*@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_register, container, false);
        // Inflate the layout for this fragment

        ((Button) view.findViewById(R.id.submitButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                collectUserData();
            }
        });


        return view;
    }

    private void collectUserData() {
        // TODO: laita passu uudestaan kohta myös!!!
        mEmail = ((EditText) view.findViewById(R.id.emailInput)).getText().toString();
        mPassword = ((EditText) view.findViewById(R.id.passwordInput)).getText().toString();
        mUserName = ((EditText) view.findViewById(R.id.userNameInput)).getText().toString();

        if(!mEmail.isEmpty() && !mPassword.isEmpty() && !mUserName.isEmpty()) {
            registerUser(mEmail, mPassword);
        } else {
            Toast.makeText(getContext(), "Fill all fields!", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUser(String email, String password) {

        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        EditText email = ((EditText) view.findViewById(R.id.emailInput));
                        EditText password = ((EditText) view.findViewById(R.id.passwordInput));

                        if(!task.isSuccessful()) {
                            try {
                                throw task.getException();
                            } catch(FirebaseAuthWeakPasswordException e) {
                                password.setError(getString(R.string.error_weak_password));
                                password.requestFocus();
                            } catch(FirebaseAuthInvalidCredentialsException e) {
                                email.setError(getString(R.string.error_invalid_email));
                                email.requestFocus();
                            } catch(FirebaseAuthUserCollisionException e) {
                                email.setError(getString(R.string.error_user_exists));
                                email.requestFocus();
                            } catch (FirebaseException e) {
                                // somehow when the password is weak, FirebaseException is thrown,
                                // not FirebaseAuthWeakPasswordException...
                                if(e.getMessage().equals("An internal error has occurred. [ WEAK_PASSWORD  ]")) {
                                    password.setError(getString(R.string.error_weak_password));
                                    password.requestFocus();
                                }
                            } catch(Exception e) {
                                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), getString(R.string.register_successful), Toast.LENGTH_SHORT).show();
                            updateUserProfile(mUserName);
                            onSuccessfulRegistration();
                        }
                    }
                });
    }

    /**
     * Add here any other information of the user (in addition to email and password)
     * Displayname in this app needs not to be unique (email is the identifier)
     */

    private void updateUserProfile(String displayName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                //.setPhotoUri(Uri.parse("https://example.com/jane-q-user/profile.jpg")) //TODO: add a nice icon if time
                .build();

        // Check if user null just in case this method gets used wrong.
        if(user != null) {
            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(getTag(), "User profile updated.");
                            } else {
                                try {
                                    throw task.getException();
                                } catch (Exception e) {
                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
        } else {
            Toast.makeText(getContext(), "You are not logged in!", Toast.LENGTH_SHORT).show();
        }

    }

    private void onSuccessfulRegistration() {
        if(mListener != null) {
            mListener.logIn();
        } else {
            Log.d(getTag(), "listener in RegisterFragment is null");
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof onRegisterListener) {
            mListener = (onRegisterListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement onRegisterListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
