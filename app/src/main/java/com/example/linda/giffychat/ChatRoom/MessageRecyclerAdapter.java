package com.example.linda.giffychat.ChatRoom;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.linda.giffychat.Entity.ChatMessage;
import com.example.linda.giffychat.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;

public class MessageRecyclerAdapter extends FirebaseRecyclerAdapter<ChatMessage, MessageHolder> {

    private Context context;
    ProgressDialog progDialogUpdate;

    public MessageRecyclerAdapter(Class<ChatMessage> modelClass, int modelLayout,
                                  Class<MessageHolder> viewHolderClass, DatabaseReference ref,
                                  Context context, ProgressDialog progDialogUpdate) {
        super(modelClass, modelLayout, viewHolderClass, ref);
        this.progDialogUpdate = progDialogUpdate;
        this.context = context;
    }

    @Override
    protected void populateViewHolder(final MessageHolder messageHolder,
                                      final ChatMessage message, final int position) {
        messageHolder.populateView(message, progDialogUpdate);
    }

    @Override
    public MessageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View messageView;
        switch (viewType) {
            case R.layout.message_text:
                messageView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_text, parent, false);
                return new MessageHolder(messageView, viewType, context);
            case R.layout.message_gif:
                messageView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_gif, parent, false);
                return new MessageHolder(messageView, viewType, context);
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        switch (message.getType()) {
            case TEXTMESSAGE:
                return R.layout.message_text;
            case GIFMESSAGE:
                return R.layout.message_gif;
        }
        return super.getItemViewType(position);
    }

}