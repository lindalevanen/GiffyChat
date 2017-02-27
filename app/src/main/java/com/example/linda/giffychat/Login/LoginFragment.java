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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

/**
 * Creates a view for user login.
 */

public class LoginFragment extends Fragment {

    private onLoginListener mListener;

    private String mEmail;
    private String mPassword;

    private View view;

    public LoginFragment() {}

    public interface onLoginListener {
        void logIn();
    }

    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_login, container, false);

        ((Button) view.findViewById(R.id.submitButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                collectUserData();
            }
        });

        return view;
    }

    private void collectUserData() {
        mEmail = ((EditText) view.findViewById(R.id.emailInput)).getText().toString();
        mPassword = ((EditText) view.findViewById(R.id.passwordInput)).getText().toString();

        if(!mEmail.isEmpty() && !mPassword.isEmpty()) {
            loginUser(mEmail, mPassword);
        } else {
            Toast.makeText(getContext(), "Fill both fields!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginUser(String email, String password) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                try {
                                    throw task.getException();
                                } catch (FirebaseAuthInvalidCredentialsException e) {
                                    Toast.makeText(getContext(), getString(R.string.error_invalid_credentials),
                                            Toast.LENGTH_SHORT).show();
                                } catch (FirebaseAuthInvalidUserException e) {
                                    Toast.makeText(getContext(), getString(R.string.error_invalid_credentials),
                                            Toast.LENGTH_SHORT).show();
                                } catch(Exception e) {
                                    System.out.println(e.getClass().toString());
                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                onSuccessfulLogin();
                            }
                        }
                    });
        } else {
            Log.d(getTag(), "Can't login when a user has already logged in.");
        }
    }

    private void onSuccessfulLogin() {
        if(mListener != null) {
            mListener.logIn();
        } else {
            Log.d(getTag(), "listener in RegisterFragment is null");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof onLoginListener) {
            mListener = (onLoginListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement onLoginListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


}
