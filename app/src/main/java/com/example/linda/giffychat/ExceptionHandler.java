package com.example.linda.giffychat;

import android.content.Context;
import android.content.Intent;

import java.io.PrintWriter;
import java.io.StringWriter;

import android.os.Process;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;

/**
 * Created by Linda on 22/02/17.
 */

public class ExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {
    private final Context myContext;
    private final Class<?> myActivityClass;
    private FirebaseAnalytics mFirebaseAnalytics;

    public ExceptionHandler(Context context, Class<?> c) {
        myContext = context;
        myActivityClass = c;
        this.mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    public void uncaughtException(Thread thread, Throwable exception) {

        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        System.err.println(stackTrace);// You can use LogCat too

        FirebaseCrash.log(exception.getMessage());
        FirebaseCrash.report(exception);

        //you can use this String to know what caused the exception and in which Activity
        /*intent.putExtra("uncaughtException",
                "Exception is: " + stackTrace.toString());
        intent.putExtra("stacktrace", s);
        myContext.startActivity(intent);*/
        //for restarting the Activity
        Process.killProcess(Process.myPid());
        System.exit(0);
    }
}
