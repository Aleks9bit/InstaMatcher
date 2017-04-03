package com.bargin.alexey.photomatcher.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.bargin.alexey.photomatcher.MainActivity;
import com.bargin.alexey.photomatcher.R;
import com.bargin.alexey.photomatcher.localUserData.User;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import static com.bargin.alexey.photomatcher.instagramSecretKey.InstagramSecretKey.CLIENT_ID;
import static com.bargin.alexey.photomatcher.instagramSecretKey.InstagramSecretKey.CLIENT_SECRET;
import static com.bargin.alexey.photomatcher.instagramSecretKey.InstagramSecretKey.REDIRECT_URI;

/**
 * A placeholder fragment containing a simple view.
 */
public class LoginFragment extends Fragment {
    String authURLString, tokenURLString, request_token;
    WebView webView;
    User user;

    private static final String AUTH_URL = "https://api.instagram.com/oauth/authorize/";
    private static final String TOKEN_URL = "https://api.instagram.com/oauth/access_token";
    public static final String API_URL = "https://api.instagram.com/v1";
    public static String CALLBACK_URL = REDIRECT_URI;

    static Context context;
    ProgressDialog dialog;

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        webView = (WebView) view.findViewById(R.id.web);
        user = new User(getContext());
        context = getContext();
        authorize();
        return view;
    }

    private void authorize() {
        authURLString = AUTH_URL + "?client_id=" + CLIENT_ID + "&amp;amp;redirect_uri=" + CALLBACK_URL +
                "&amp;amp;response_type=code&amp;amp;display=touch&amp;amp;scope=basic+public_content+comments";
        tokenURLString = TOKEN_URL + "?client_id=" + CLIENT_ID + "&amp;amp;client_secret=" + CLIENT_SECRET +
                "&amp;amp;redirect_uri=" + CALLBACK_URL + "&amp;amp;grant_type=authorization_code";
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setWebViewClient(new AuthWebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(authURLString);
    }

    private class AuthWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
            super.onPageFinished(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (dialog == null)
                dialog = ProgressDialog.show(getContext(), null, "Loading login page...");
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith(CALLBACK_URL)) {
                System.out.println(url);
                String parts[] = url.split("=");
                request_token = parts[1];//Это наш маркер запроса.
                Task task = new Task();
                task.execute();
                return true;
            }
            return false;
        }
    }

    private class Task extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                URL url = new URL(tokenURLString);
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
                httpsURLConnection.setRequestMethod("POST");
                httpsURLConnection.setDoInput(true);
                httpsURLConnection.setDoOutput(true);
                OutputStreamWriter outputStreamWriter = new
                        OutputStreamWriter(httpsURLConnection.getOutputStream());
                outputStreamWriter.write("client_id=" + CLIENT_ID +
                        "&amp;amp;client_secret=" + CLIENT_SECRET +
                        "&amp;amp;grant_type=authorization_code" +
                        "&amp;amp;redirect_uri=" + CALLBACK_URL +
                        "&amp;amp;code=" + request_token);

                outputStreamWriter.flush();
                String response = streamToString(httpsURLConnection.getInputStream());
                JSONObject jsonObject = (JSONObject) new JSONTokener(response).nextValue();
                String accessTokenString = jsonObject.getString("access_token");//Это наш маркер доступа
                String id = jsonObject.getJSONObject("user").getString("id");

                //Получаем данные о пользователе
                String username = jsonObject.getJSONObject("user").getString("username");
                String full_name = jsonObject.getJSONObject("user").getString("full_name");
                user.storeAccessToken(accessTokenString, id, username, full_name);
                if (!accessTokenString.isEmpty()) {
                    startActivity(new Intent(getContext(), MainActivity.class));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static ArrayList<String> getInstagramImagesUrl(String accessTokenString, User user) {
        ArrayList<String> userListImages = new ArrayList<>();
        try {
            String urlString = API_URL + "/users/" + "self" +
                    "/media/recent/?access_token=" + accessTokenString;
            URL url1 = new URL(urlString);
            String nextUrl = null;
            InputStream inputStream = url1.openConnection().getInputStream();
            String response1 = streamToString(inputStream);
            JSONObject jsonObject1 = (JSONObject) new JSONTokener(response1).nextValue();
            JSONArray jsonArray = jsonObject1.getJSONArray("data");
            JSONObject mainImageJsonObject;
            String imageUrlString;
            int arrayLength = jsonArray.length();
            for (int j = 0; j < arrayLength; j++) {
                if (jsonObject1.getJSONObject("pagination").has("next_url"))
                    nextUrl = jsonObject1.getJSONObject("pagination").getString("next_url");
                else nextUrl = null;

                mainImageJsonObject =
                        jsonArray.getJSONObject(j).getJSONObject("images").getJSONObject("standard_resolution");
                imageUrlString = mainImageJsonObject.getString("url");
                if (jsonArray.getJSONObject(j).getString("type").equals("image"))
                    userListImages.add(imageUrlString);
            }
            int i = 1;
            if (nextUrl != null)
                do {
                    url1 = new URL(nextUrl);
                    inputStream = url1.openConnection().getInputStream();
                    response1 = streamToString(inputStream);
                    jsonObject1 = (JSONObject) new JSONTokener(response1).nextValue();
                    jsonArray = jsonObject1.getJSONArray("data");
                    arrayLength = jsonArray.length();
                    for (int j = 0; j < arrayLength; j++) {
                        if (jsonObject1.getJSONObject("pagination").has("next_url"))
                            nextUrl = jsonObject1.getJSONObject("pagination").getString("next_url");
                        else nextUrl = null;

                        mainImageJsonObject =
                                jsonArray.getJSONObject(j).getJSONObject("images").getJSONObject("standard_resolution");
                        imageUrlString = mainImageJsonObject.getString("url");
                        if (jsonArray.getJSONObject(j).getString("type").equals("image"))
                            userListImages.add(imageUrlString);
                    }
                } while (nextUrl != null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userListImages;
    }

    private static String streamToString(InputStream is) throws IOException {
        String string = "";

        if (is != null) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is));

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                reader.close();
            } finally {
                is.close();
            }

            string = stringBuilder.toString();
        }

        return string;
    }
}
