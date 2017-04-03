package com.bargin.alexey.photomatcher.localUserData;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.bargin.alexey.photomatcher.MainActivity;
import com.bargin.alexey.photomatcher.R;
import com.bargin.alexey.photomatcher.adapters.GalleryAdapter;
import com.bargin.alexey.photomatcher.fragments.GalleryFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class User {
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private static final String SHARED = "Instagram_Preferences";
    private static final String API_USERNAME = "username";
    private static final String API_ID = "id";
    private static final String API_NAME = "name";
    private static final String API_ACCESS_TOKEN = "access_token";
    private static final String USER_LIST_IMAGES = "list_images";
    private static final String PAIRS = "pairs";
    private static final String LOCAL_IMAGE_LIST_PATH = "local_image";
    private static final String CHECKED_LOCAL_IMAGE_LIST_PATH = "checked_local_image";
    private Context context;
    public static ArrayList<String> localInstaImages = new ArrayList<>();
    private ArrayList<String> localImagesList;
    private ArrayList<String> instaImagesList;
    private ProgressDialog matcherDialog;
    public static NotificationManager mNotifyManager;

    public User(Context context) {
        this.context = context;
        sharedPref = context.getSharedPreferences(SHARED, Context.MODE_PRIVATE);
        editor = sharedPref.edit();
    }

    public void saveImagePathThatWasChecked(String checkedPath) {
        String json = sharedPref.getString(CHECKED_LOCAL_IMAGE_LIST_PATH, null);
        JSONArray array = null;
        ArrayList<String> list = new ArrayList<>();
        try {
            if (json != null) {
                array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    list.add(array.getString(i));
                }
                if (!list.contains(checkedPath))
                    array.put(checkedPath);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (array != null) {
            editor.putString(CHECKED_LOCAL_IMAGE_LIST_PATH, array.toString());
            editor.commit();
        }
    }

    public boolean wasAlreadyCheck(String path) {
        JSONArray array = null;
        ArrayList<String> list = new ArrayList<>();
        String json = sharedPref.getString(CHECKED_LOCAL_IMAGE_LIST_PATH, null);
        try {
            if (json != null) {
                array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++)
                    list.add(array.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list.contains(path);
    }

    public void resetAlrearyCheckedPathList() {
        editor.putString(CHECKED_LOCAL_IMAGE_LIST_PATH, null);
        editor.commit();
    }

    public void saveLocalImagePathIfNotExist(String newPath) {
        JSONArray array = new JSONArray();
        String json = sharedPref.getString(LOCAL_IMAGE_LIST_PATH, null);
        ArrayList<String> list = new ArrayList<>();
        try {
            if (json != null) {
                array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++)
                    list.add(array.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (!list.contains(newPath))
            array.put(newPath);
        editor.putString(LOCAL_IMAGE_LIST_PATH, array.toString());
        editor.commit();
    }

    public ArrayList<String> getLocalImagePathList() {
        ArrayList<String> result = null;
        try {
            String savedJsonArrayString = sharedPref.getString(LOCAL_IMAGE_LIST_PATH, null);
            if (savedJsonArrayString != null) {
                result = new ArrayList<>();
                JSONArray jsonArray = new JSONArray(savedJsonArrayString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    result.add(jsonArray.getString(i));
                }
            }
        } catch (JSONException e) {
            return null;
        }
        return result;
    }

    public void removeLocalImagePath(String path) {
        JSONArray array;
        ArrayList<String> list = new ArrayList<>();
        String json = sharedPref.getString(LOCAL_IMAGE_LIST_PATH, null);
        if (json != null)
            try {
                array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++)
                    list.add(array.getString(i));
                if (list.contains(path)) list.remove(path);
                else return;
                array = new JSONArray(list);
                editor.putString(LOCAL_IMAGE_LIST_PATH, array.toString());
                editor.commit();
            } catch (JSONException e) {
                e.printStackTrace();
            }
    }

    public void resetLocalImagePathList() {
        editor.putString(LOCAL_IMAGE_LIST_PATH, null);
        editor.commit();
    }

    public void storeAccessToken(String accessToken, String id, String username, String name) {
        editor.putString(API_ID, id);
        editor.putString(API_NAME, name);
        editor.putString(API_ACCESS_TOKEN, accessToken);
        editor.putString(API_USERNAME, username);
        editor.commit();
    }

    public String getPairs() {
        return sharedPref.getString(PAIRS, null);
    }

    public String getPairs(String userName) {
        JSONObject userPairs;
        JSONObject savedPairs;
        try {
            if (getPairs() != null) {
                savedPairs = new JSONObject(getPairs());
            } else {
                return null;
            }
            if (savedPairs.has(userName)) {
                userPairs = savedPairs.getJSONObject(userName);
                return userPairs.toString();
            } else {
                return null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void storeUserImagesList(ArrayList<String> list) {
        String images = "";
        ArrayList<String> newList = new ArrayList<>();
        for (String imageUrl : list) {
            String[] arr = imageUrl.split("/");
            String fileName = arr[arr.length - 1];
            String PATH = Environment.getExternalStorageDirectory() + "/"
                    + context.getResources().getString(R.string.image_folder) + "/" + getUserName() + "/" + fileName;
            File file = new File(PATH);
            localInstaImages.add(PATH);
            if (!file.exists()) newList.add(imageUrl);
            if (!images.contains(imageUrl)) images += imageUrl + " ; ";
        }
        String[] arr = new String[newList.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = newList.get(i);
        new DownloadFile().execute(arr);// start download new photos
        editor.putString(USER_LIST_IMAGES, images);
        editor.commit();
    }

    public void storeAccessToken(String accessToken) {
        editor.putString(API_ACCESS_TOKEN, accessToken);
        editor.commit();
    }

    /**
     * Reset access token and user name
     */
    public void resetAccessToken() {
        editor.putString(API_ID, null);
        editor.putString(API_NAME, null);
        editor.putString(API_ACCESS_TOKEN, null);
        editor.putString(API_USERNAME, null);
        editor.putString(USER_LIST_IMAGES, null);
        editor.commit();
    }

    public void resetPairs() {
        editor.putString(PAIRS, null);
        editor.commit();
    }

    /**
     * Get user name
     *
     * @return User name
     */
    public String getUserName() {
        return sharedPref.getString(API_USERNAME, null);
    }

    /**
     * @return
     */
    public String getId() {
        return sharedPref.getString(API_ID, null);
    }

    /**
     * @return
     */
    public String getName() {
        return sharedPref.getString(API_NAME, null);
    }

    /**
     * Get access token
     *
     * @return Access token
     */
    public String getAccessToken() {
        return sharedPref.getString(API_ACCESS_TOKEN, null);
    }

    public String getUserListImages() {
        return sharedPref.getString(USER_LIST_IMAGES, null);
    }

    class DownloadFile extends AsyncTask<String, Integer, ArrayList<String>> {
        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setMessage("Downloading new photos from your account. Please wait...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.show();
        }

        protected void onProgressUpdate(Integer... progress) {
            mProgressDialog.setProgress(progress[0]);
            if (mProgressDialog.getProgress() == mProgressDialog.getMax()) {
                mProgressDialog.dismiss();
                Toast.makeText(context, "Photos were downloaded", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected ArrayList<String> doInBackground(String... aurl) {
            ArrayList<String> result = new ArrayList<>();
            mProgressDialog.setMax(aurl.length);
            try {
                for (int i = 0; i < aurl.length; i++) {
                    String[] arr = aurl[i].split("/");
                    String fileName = arr[arr.length - 1];

                    URL url = new URL((String) aurl[i]);
                    URLConnection connection = url.openConnection();
                    connection.connect();
                    String username = getUserName();
                    String PATH = Environment.getExternalStorageDirectory() + "/"
                            + context.getResources().getString(R.string.image_folder) + "/";
                    File folder = new File(PATH);
                    if (!folder.exists()) {
                        folder.mkdir();//If there is no folder it will be created.
                    }
                    PATH += username + "/";
                    folder = new File(PATH);
                    if (!folder.exists())
                        folder.mkdir();
                    InputStream input = new BufferedInputStream(url.openStream());
                    OutputStream output = new FileOutputStream(PATH + fileName);
                    result.add(PATH + fileName);
                    publishProgress(i + 1);
                    byte data[] = new byte[1024];
                    int count;
                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            if (mProgressDialog.isShowing())
                mProgressDialog.dismiss();
            if (!result.isEmpty())
                new MainMatcher().execute(result);
        }

    }

    class MainMatcher extends AsyncTask<ArrayList<String>, Integer, Object> {
        JSONObject pairs;
        JSONObject savedPairs;

        NotificationCompat.Builder mBuilder;
        int id = 1;
        PowerManager powerManager;
        PowerManager.WakeLock wakeLock;

        @Override
        protected void onCancelled() {
            mNotifyManager.cancel(id);
            super.onCancelled();
        }

        @Override
        protected void onPreExecute() {
            powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WAKE_LOCK");
            wakeLock.acquire();

            matcherDialog = new ProgressDialog(context);
            matcherDialog.setTitle("Matching photo\nPlease wait...");
            matcherDialog.setCancelable(false);
            matcherDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            matcherDialog.show();

            Intent emptyIntent = new Intent(context, MainActivity.class);
            emptyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            mNotifyManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(context);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, emptyIntent, 0);
            mBuilder.setContentTitle("PhotoMatcher")
                    .setContentText("Matching in progress...")
                    .setSmallIcon(R.drawable.icon)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            try {
                if (getPairs() == null) {
                    savedPairs = new JSONObject();
                } else {
                    savedPairs = new JSONObject(getPairs());
                }
                if (savedPairs.has(getUserName())) {
                    pairs = savedPairs.getJSONObject(getUserName());
                } else {
                    pairs = new JSONObject();
                    savedPairs.put(getUserName(), pairs);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Object doInBackground(ArrayList<String>... params) {
            localImagesList = getLocalImagePathList();
            instaImagesList = params[0];
            matcherDialog.setMax(localImagesList.size() * instaImagesList.size());
            int progress = 0;
            for (String localImage : localImagesList) {
                Bitmap localBitmap = BitmapFactory.decodeFile(localImage);
                float diff = 6f;
                String clone = null;
                float currentDiff;
                for (String instaImage : instaImagesList) {
                    publishProgress(++progress);
                    currentDiff = matchImages(BitmapFactory.decodeFile(instaImage)
                            , localBitmap);
                    if (currentDiff < diff) {
                        diff = currentDiff;
                        clone = instaImage;
                    }
                }
                if (diff <= 5f) {
                    try {
                        pairs.put(localImage, clone);
                        removeLocalImagePath(localImage);
                        saveImagePathThatWasChecked(localImage);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Bitmap bitmap = GalleryAdapter.bitmaps.get(GalleryAdapter.pathList.indexOf(localImage));
                    GalleryFragment.galleryAdapter.replaceBitmapWithLabel(bitmap, true);
                    if (clone != null)
                        instaImagesList.remove(clone);
                }
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
            GalleryFragment.galleryAdapter.notifyDataSetChanged();
            matcherDialog.setProgress(progress[0]);
            mBuilder.setProgress(matcherDialog.getMax(), progress[0], false);
            mNotifyManager.notify(id, mBuilder.build());
        }

        @Override
        protected void onPostExecute(Object result) {
            editor.putString(PAIRS, savedPairs.toString());
            editor.commit();
            GalleryFragment.galleryAdapter.notifyDataSetChanged();
            matcherDialog.dismiss();
            mBuilder.setContentText("Matching complete")
                    .setProgress(0, 0, false);
            mNotifyManager.notify(id, mBuilder.build());
            wakeLock.release();
        }
    }

    public Bitmap resizeBitmap(Bitmap bitmap, int wantedWidth, int wantedHeight) {
        bitmap = Bitmap.createScaledBitmap(bitmap, wantedWidth, wantedHeight, false);
        return bitmap;
    }

    public float matchImages(Bitmap im1, Bitmap im2) {
        if (im1 == null || im2 == null) {
            return 100f;
        }
        int maxH, maxW;
        int w1 = im1.getWidth();
        int w2 = im2.getWidth();
        int h1 = im1.getHeight();
        int h2 = im2.getHeight();
        if (w1 < w2 && h1 < h2) {
            im2 = resizeBitmap(im2, w1, h1);
            maxW = w1;
            maxH = h1;
        } else {
            im1 = resizeBitmap(im1, w2, h2);
            maxH = h2;
            maxW = w2;
        }
        long diff = 0;
        int step = 5;
        int r1, r2, b1, b2, g1, g2, a1, a2, br1, br2, c1, c2;
        for (int y = 0; y < maxH - 1; y += step) {
            for (int x = 0; x < maxW - 1; x += step) {
                c1 = im1.getPixel(x, y);
                c2 = im2.getPixel(x, y);

                r1 = Color.red(c1);
                g1 = Color.green(c1);
                b1 = Color.blue(c1);
                a1 = Color.alpha(c1);
                br1 = (int) (r1 * 0.3 + b1 * 0.11 + g1 * 0.59);

                r2 = Color.red(c2);
                g2 = Color.green(c2);
                b2 = Color.blue(c2);
                a2 = Color.alpha(c2);
                br2 = (int) (r2 * 0.3 + b2 * 0.11 + g2 * 0.59);

                diff += Math.abs(a1 - a2);
                diff += Math.abs(br1 - br2);
            }
        }
        double n = maxW * maxH * 3;
        double p = diff / n / 255.0 * step * step;
        return (float) (p * 100.0);
    }
}
