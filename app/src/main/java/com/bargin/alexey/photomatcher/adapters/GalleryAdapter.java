package com.bargin.alexey.photomatcher.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bargin.alexey.photomatcher.R;

import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    private OnClickListener listener;
    public static List<Bitmap> bitmaps = new ArrayList<>();
    public static List<String> pathList = new ArrayList<>();
    protected static Context context;
    private int width;
    public static ArrayList<Boolean> bools = new ArrayList<>();

    public void addImage(Bitmap bitmap, String path, boolean hasLabel) {
        bitmaps.add(0, bitmap);
        pathList.add(0, path);
        bools.add(0, hasLabel);
    }

    public void replaceBitmapWithLabel(Bitmap defaultBitmap, boolean hasLabel) {
        if (hasLabel)
            bools.set(bitmaps.indexOf(defaultBitmap), true);
    }

    public GalleryAdapter(OnClickListener listener, Context context, int width) {
        this.listener = listener;
        this.context = context;
        this.width = width;
    }

    @Override
    public GalleryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_gallery, parent, false);
        v.getLayoutParams().width = width / 3 - 50;
        v.getLayoutParams().height = width / 3 - 50;
        v.requestLayout();
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.bind(bitmaps.get(position));
        if (bools.get(position))
            holder.itemView.findViewById(R.id.check).setVisibility(View.VISIBLE);
        else holder.itemView.findViewById(R.id.check).setVisibility(View.GONE);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onClick(pathList.get(position));
            }
        });
    }

    public interface OnClickListener {
        void onClick(String imagePath);
    }

    public static List<String> getPathList() {
        return pathList;
    }

    @Override
    public int getItemCount() {
        return bitmaps.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView image;
        public ImageView check;
        BitmapFactory.Options options = new BitmapFactory.Options();

        public ViewHolder(View v) {
            super(v);
            image = (ImageView) v.findViewById(R.id.image);
            check = (ImageView) v.findViewById(R.id.check);
            options.inSampleSize = 8;
        }

        public void bind(Bitmap imagePath) {
            image.setImageBitmap(imagePath);
        }
    }
}