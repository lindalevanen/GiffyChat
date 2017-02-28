package com.example.linda.giffychat.ChatRoom;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.example.linda.giffychat.Entity.ChatMessage;
import com.example.linda.giffychat.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;

/**
 * A custom class of the FirebaseRecyclerAdapter. Creates and populates type-specific viewholders for messages.
 */

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
            case R.layout.message_gif_portrait:
                messageView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_gif_portrait, parent, false);
                return new MessageHolder(messageView, viewType, context);
            case R.layout.message_gif_landscape:
                messageView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_gif_landscape, parent, false); //TÄHÄN JÄIN, PITÄÄ LAITTAA LANDSCAPET KUNTOON
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
                if(message.getGifOrientation() != 0) {
                    if(message.getGifOrientation() == 1) {
                        return R.layout.message_gif_portrait;
                    } else {
                        return R.layout.message_gif_landscape;
                    }
                } else {
                    return R.layout.message_gif_landscape;
                }
        }
        return super.getItemViewType(position);
    }

    /**
     * A possible solution to outOfMemoryError (not really). Fixed memoryerror by loading gifs on touch.
     * @param holder
     */

    @Override
    public void onViewRecycled(MessageHolder holder) {
        super.onViewRecycled(holder);
        MessageHolder viewHolder = (MessageHolder) holder;
        if(holder.viewType == R.layout.message_gif_portrait ||
                holder.viewType  == R.layout.message_gif_landscape) {
            Glide.clear(viewHolder.messageGif);
        }
    }

}