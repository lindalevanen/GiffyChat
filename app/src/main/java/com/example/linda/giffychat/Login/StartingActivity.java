package com.example.linda.giffychat.Login;

import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.linda.giffychat.Main.MainActivity;
import com.example.linda.giffychat.R;
import com.google.firebase.auth.FirebaseAuth;

public class StartingActivity extends AppCompatActivity
        implements RegisterFragment.onRegisterListener, LoginFragment.onLoginListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_starting);

        if(FirebaseAuth.getInstance().getCurrentUser() != null) {
            logIn();
        }

        setUpButtonListeners();

    }

    private void setUpButtonListeners() {
        ((Button) findViewById(R.id.registerB)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager manager = getSupportFragmentManager();
                FragmentTransaction trans = manager.beginTransaction();

                RegisterFragment registerFrag =
                        RegisterFragment.newInstance();
                trans.add(R.id.registerFrag, registerFrag, "registerFrag");
                trans.addToBackStack(null);
                trans.commit();
            }
        });

        ((Button) findViewById(R.id.loginB)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager manager = getSupportFragmentManager();
                FragmentTransaction trans = manager.beginTransaction();

                LoginFragment loginFrag =
                        LoginFragment.newInstance();
                trans.add(R.id.loginFrag, loginFrag, "loginFrag");
                trans.addToBackStack(null);
                trans.commit();
            }
        });
    }

    public void logIn() {

        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }

}
