package com.example.linda.giffychat.Main;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.linda.giffychat.Entity.Room;
import com.example.linda.giffychat.HelperMethods;
import com.example.linda.giffychat.R;

import java.util.ArrayList;

import static com.example.linda.giffychat.HelperMethods.getBitmapFromBase64;

/**
 * A very simple list adapter for the items in "Favorites" tab.
 */

public class FavoriteListAdapter extends ArrayAdapter<Room> {

    private Resources res;
    private ProgressBar progressBar;

    public FavoriteListAdapter(Context context, ArrayList<Room> rooms, ProgressBar progressBar) {
        super(context, 0, rooms);
        this.progressBar = progressBar;
        res = context.getResources();
    }

    @Override @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        progressBar.setVisibility(View.INVISIBLE);
        Room room = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.room, parent, false);
        }
        TextView titleView = (TextView) convertView.findViewById(R.id.roomTitle);
        ImageView iconView = (ImageView) convertView.findViewById(R.id.roomIcon);
        if(room.getBase64RoomImage() != null) {

            Bitmap decoded = HelperMethods.getBitmapFromBase64(room.getBase64RoomImage());
            Bitmap ccBtm = HelperMethods.centerCropBitmap(decoded);
            RoundedBitmapDrawable dr = HelperMethods.giveBitmapRoundedCorners(ccBtm, getContext());

            iconView.setImageDrawable(dr);
        } else {
            iconView.setImageResource(R.drawable.ic_giffy);
        }
        iconView.setAdjustViewBounds(true);
        iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        titleView.setText(room.getTitle());

        return convertView;
    }

}
