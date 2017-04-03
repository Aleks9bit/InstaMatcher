package com.bargin.alexey.photomatcher.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bargin.alexey.photomatcher.R;
import com.bargin.alexey.photomatcher.adapters.GalleryAdapter;
import com.bargin.alexey.photomatcher.localUserData.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class GalleryFragment extends Fragment implements GalleryAdapter.OnClickListener {
    public static RecyclerView recyclerView;
    public static GalleryAdapter galleryAdapter;
    private ArrayList<String> images = new ArrayList<>();
    private GridLayoutManager verticalGridLayoutManager;
    public ProgressDialog dialog;
    public static Activity activity;
    static Context context;
    ArrayList<String> urls;
    User user;
    public static int width;
    ArrayList<Bitmap> bitmapsWithLabel = new ArrayList<>();

    public static GalleryFragment newInstance() {
        return new GalleryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = getActivity();
        context = getContext();
        user = new User(context);
        new AttachImages().execute();
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.gallery);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        width = display.getWidth();

        recyclerView.setHasFixedSize(true);
        verticalGridLayoutManager = new GridLayoutManager(getContext(), 3);
        recyclerView.setLayoutManager(verticalGridLayoutManager);

        galleryAdapter = new GalleryAdapter(this, getContext(), width);
        recyclerView.setAdapter(galleryAdapter);
        return view;
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        BitmapFactory.Options options;

        BitmapWorkerTask(BitmapFactory.Options options) {
            this.options = options;
        }

        JSONObject jsonObject;
        String json = user.getPairs(user.getUserName());

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... path) {
            Bitmap bitmap = BitmapFactory.decodeFile(path[0], options);
            if (json != null) {
                try {
                    jsonObject = new JSONObject(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject != null && jsonObject.has(path[0]))
                bitmapsWithLabel.add(bitmap);
            galleryAdapter.addImage(bitmap, path[0], false);

            images.remove(path[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            if (dialog.isShowing() && images.isEmpty()) {
                for (Bitmap bitm : bitmapsWithLabel) {
                    galleryAdapter.replaceBitmapWithLabel(bitm, true);
                }
                dialog.dismiss();
                new GetInstaImagesTask().execute();
            }
            galleryAdapter.notifyDataSetChanged();
            super.onPostExecute(bitmap);
        }
    }

    class GetInstaImagesTask extends AsyncTask {
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(getContext(), "", "Getting instagram images\nPlease wait...");
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            user = new User(getContext());
            urls = LoginFragment.getInstagramImagesUrl(user.getAccessToken(), user);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            dialog.dismiss();
            user.storeUserImagesList(urls);
        }
    }

    private class AttachImages extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            setListImagesPath();
            return null;
        }

        @Override
        protected void onPreExecute() {
            if (dialog == null)
                dialog = ProgressDialog.show(getContext(), "", "Searching for images\nPlease wait...");
        }
    }

    private void setListImagesPath() {
        GalleryAdapter.bitmaps.clear();
        GalleryAdapter.pathList.clear();
        Uri uri;
        Cursor cursor1, cursor2, cursor3, cursor4;
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
        String DCIM_IMAGE_BUCKET_NAME =
                Environment.getExternalStorageDirectory().toString()
                        + "/DCIM";
        String CAMERA_IMAGE_BUCKET_NAME = DCIM_IMAGE_BUCKET_NAME + "/Camera";
        String SELFIE_IMAGE_BUCKET_NAME =
                Environment.getExternalStorageDirectory().toString()
                        + "/DCIM/Selfie";
        String VIBER_IMAGE_BUCKET_NAME =
                Environment.getExternalStorageDirectory().toString()
                        + "/Viber/media/Viber Images";
        String DCIM_IMAGE_BUCKET_ID = String.valueOf(DCIM_IMAGE_BUCKET_NAME.toLowerCase().hashCode());
        String SELFIE_IMAGE_BUCKET_ID = String.valueOf(SELFIE_IMAGE_BUCKET_NAME.toLowerCase().hashCode());
        String VIBER_IMAGE_BUCKET_ID = String.valueOf(VIBER_IMAGE_BUCKET_NAME.toLowerCase().hashCode());
        String CAMERA_IMAGE_BUCKET_ID = String.valueOf(CAMERA_IMAGE_BUCKET_NAME.toLowerCase().hashCode());
        String[] selectionArgs = {DCIM_IMAGE_BUCKET_ID};
        final String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";

        cursor1 = activity.getContentResolver().query(uri, projection, selection,
                selectionArgs, null); // use only URI, PROJECNTION to display all images from device
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 10;

        findLocalImagesPath(cursor1, options);

        selectionArgs = new String[]{VIBER_IMAGE_BUCKET_ID};
        cursor2 = activity.getContentResolver().query(uri, projection, selection,
                selectionArgs, null);
        findLocalImagesPath(cursor2, options);

        selectionArgs = new String[]{SELFIE_IMAGE_BUCKET_ID};
        cursor3 = activity.getContentResolver().query(uri, projection, selection,
                selectionArgs, null);
        findLocalImagesPath(cursor3, options);

        selectionArgs = new String[]{CAMERA_IMAGE_BUCKET_ID};
        cursor4 = activity.getContentResolver().query(uri, projection, selection,
                selectionArgs, null);
        findLocalImagesPath(cursor4, options);
    }

    private void findLocalImagesPath(Cursor cursor, BitmapFactory.Options options) {
        String absolutePathOfImage;
        int columnIndexData = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(columnIndexData);
            if (!absolutePathOfImage.contains("mp4") && !absolutePathOfImage.contains("gif") && !absolutePathOfImage.contains("GIF")) {
                if (!user.wasAlreadyCheck(absolutePathOfImage))
                    user.saveLocalImagePathIfNotExist(absolutePathOfImage);
                BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(options);
                bitmapWorkerTask.execute(absolutePathOfImage);
                images.add(absolutePathOfImage);
            }
        }
    }

    @Override
    public void onClick(String imagePath) {
        new FoundImage().execute(imagePath);
    }

    class FoundImage extends AsyncTask<String, Integer, String> {
        ProgressDialog foundDialog;

        @Override
        protected void onPreExecute() {
            foundDialog = new ProgressDialog(context);
            foundDialog.setTitle("Getting image...");
            foundDialog.setCancelable(true);
            foundDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            foundDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String userName = user.getUserName();
            JSONObject userPairs;
            if (user.getPairs(userName) != null) {
                try {
                    userPairs = new JSONObject(user.getPairs(userName));
                    return userPairs.has(params[0]) ? userPairs.getString(params[0]) : null;
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            } else return null;
        }

        @Override
        protected void onPostExecute(String result) {
            foundDialog.dismiss();
            if (result == null)
                Toast.makeText(getContext(), "No matches", Toast.LENGTH_SHORT).show();
            else {
                AlertDialog.Builder alertdialog = new AlertDialog.Builder(getActivity());
                LayoutInflater inflaterr = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View viewtemplelayout = inflaterr.inflate(R.layout.imagefile, null);
                ImageView i = (ImageView) viewtemplelayout.findViewById(R.id.imageView_temp);//and set image to image view
                i.setImageBitmap(BitmapFactory.decodeFile(result));
                alertdialog.setView(viewtemplelayout);//add your view to alert dilaog
                alertdialog.show();
            }
        }
    }
}
