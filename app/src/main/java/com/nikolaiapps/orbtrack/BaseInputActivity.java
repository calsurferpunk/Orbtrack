package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;


public abstract class BaseInputActivity extends AppCompatActivity
{
    //Activity result codes
    public static abstract class RequestCode
    {
        static final byte Setup = 1;
        static final byte OrbitalViewList = 2;
        static final byte MasterAddList = 3;
        static final byte MapLocationInput = 4;
        static final byte SDCardOpenItem = 5;
        static final byte ManualOrbitalInput = 6;
        static final byte EditWidget = 7;
        static final byte EditNotify = 8;
        static final byte GoogleDriveAddAccount = 9;
        static final byte GoogleDriveSave = 10;
        static final byte GoogleDriveSignIn = 11;
        static final byte GoogleDriveOpenFile = 12;
        static final byte GoogleDriveOpenFolder = 13;
        static final byte DropboxAddAccount = 14;
        static final byte DropboxSave = 15;
        static final byte DropboxOpenFile = 16;
        static final byte DropboxOpenFolder = 17;
        static final byte OthersOpenItem = 18;
        static final byte OthersSave = 19;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Settings.Options.Display.setTheme(this);
        setupActionBar(this, this.getSupportActionBar(), false);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
    }

    //Sets up the given action bar
    public static void setupActionBar(Context context, ActionBar actionBar, boolean useDrawer)
    {
        if(actionBar != null)
        {
            actionBar.setHomeButtonEnabled(useDrawer);
            actionBar.setDisplayHomeAsUpEnabled(useDrawer);
            actionBar.setBackgroundDrawable(ResourcesCompat.getDrawable(context.getResources(), Globals.resolveAttributeID(context, R.attr.actionBarBackground), null));
        }
    }

    //Locks screen orientation
    public void lockScreenOrientation(boolean lock)
    {
        Globals.lockScreenOrientation(this, lock);
    }
}
