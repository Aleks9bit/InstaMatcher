package com.bargin.alexey.photomatcher;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

import com.bargin.alexey.photomatcher.fragments.GalleryFragment;
import com.bargin.alexey.photomatcher.fragments.LoginFragment;
import com.bargin.alexey.photomatcher.localUserData.User;

import java.io.File;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private final int LOGIN_FRAGMENT = 0;
    private final int GALLERY_FRAGMENT = 1;
    private User user;
    private static final int PERMISSION_REQUEST_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        user = new User(this);
        if (!checkForPermission())
            finish();
        if (user.getAccessToken() == null) {
            replaceFragment(LOGIN_FRAGMENT);
        } else replaceFragment(GALLERY_FRAGMENT);
    }

    private boolean checkForPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkPermission()) {
                return true;
            } else {
                requestPermission(); // Code for permission
                return true;
            }
        } else {
            return true;
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    , Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("value", "Permission Granted, Now you can use local drive .");
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sign_out) {
            CookieSyncManager.createInstance(this);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            user.resetAccessToken();
            replaceFragment(LOGIN_FRAGMENT);
            return true;
        }
        if (id == R.id.action_update) {
            startActivity(new Intent(this, MainActivity.class));
        }
        if (id == R.id.action_clear) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete data?")
                    .setMessage("Are you sure you want to delete cache and downloaded images?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            user.resetLocalImagePathList();
                            user.resetAlrearyCheckedPathList();
                            user.resetPairs();
                            File dir = new File(Environment.getExternalStorageDirectory() + "/"
                                    + getResources().getString(R.string.image_folder) + "/"
                                    + user.getUserName() + "/");
                            if (dir.exists()) {
                                String[] children = dir.list();
                                for (int i = 0; i < children.length; i++) {
                                    new File(dir, children[i]).delete();
                                }
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_menu_delete)
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            thread.interrupt();
        }
        super.onDestroy();
    }

    private void replaceFragment(int position) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        switch (position) {
            case LOGIN_FRAGMENT:
                fragmentTransaction.replace(R.id.home, LoginFragment.newInstance());
                break;
            case GALLERY_FRAGMENT:
                fragmentTransaction.replace(R.id.home, GalleryFragment.newInstance());
                break;
        }
        fragmentTransaction.commit();
    }

}
