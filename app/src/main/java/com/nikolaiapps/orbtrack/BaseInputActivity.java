package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import java.io.File;
import java.util.ArrayList;


public abstract class BaseInputActivity extends AppCompatActivity
{
    static
    {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    //Activity result codes
    public static abstract class RequestCode
    {
        static final byte None = 0;
        static final byte Setup = 1;
        static final byte OrbitalViewList = 2;
        static final byte OrbitalSelectList = 3;
        static final byte MasterAddList = 4;
        static final byte MapLocationInput = 5;
        static final byte SDCardOpenItem = 6;
        static final byte ManualOrbitalInput = 7;
        static final byte EditWidget = 8;
        static final byte EditNotify = 9;
        static final byte GoogleDriveAddAccount = 10;
        static final byte GoogleDriveSave = 11;
        static final byte GoogleDriveSignIn = 12;
        static final byte GoogleDriveOpenFile = 13;
        static final byte GoogleDriveOpenFolder = 14;
        static final byte DropboxAddAccount = 15;
        static final byte DropboxSave = 16;
        static final byte DropboxOpenFile = 17;
        static final byte DropboxOpenFolder = 18;
        static final byte OthersOpenItem = 19;
        static final byte OthersSave = 20;
        static final byte Settings = 21;
    }

    public static final String EXTRA_REQUEST_CODE = "RequestCode";

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
            actionBar.setBackgroundDrawable(ResourcesCompat.getDrawable(context.getResources(), Globals.resolveAttributeID(context, R.attr.colorAccentDarkest), null));
        }
    }

    //Hides action bar
    public static void hideActionBar(ActionBar actionBar)
    {
        if(actionBar != null)
        {
            actionBar.hide();
        }
    }
    public void hideActionBar()
    {
        hideActionBar(this.getSupportActionBar());
    }

    //Gets request code from given intent
    public static byte getRequestCode(Intent intent)
    {
        //get request code, using RequestCode.None as default
        return(intent == null ? RequestCode.None : intent.getByteExtra(EXTRA_REQUEST_CODE, RequestCode.None));
    }

    //Sets request code to the given intent
    public static void setRequestCode(Intent intent, byte requestCode)
    {
        //if request code is set
        if(requestCode != RequestCode.None)
        {
            //if intent is not set
            if(intent == null)
            {
                //create it
                intent = new Intent();
            }

            //set request code
            intent.putExtra(EXTRA_REQUEST_CODE, requestCode);
        }
    }

    //Handles activity master add list result and returns progress type
    public static int handleActivityMasterAddListResult(Resources res, View parentView, Intent data)
    {
        boolean isError = true;
        int progressType = data.getIntExtra(MasterAddListActivity.ParamTypes.ProgressType, Globals.ProgressType.Unknown);
        long count;
        String message;

        //if a known progress type
        if(progressType != Globals.ProgressType.Unknown)
        {
            //handle based on progress type
            switch(progressType)
            {
                case Globals.ProgressType.Denied:
                    //show login failed
                    message = res.getString(R.string.text_login_failed);
                    break;

                case Globals.ProgressType.Cancelled:
                    //show cancelled
                    message = res.getString(R.string.text_update_cancelled);
                    break;

                case Globals.ProgressType.Failed:
                    //show failed
                    message = res.getString(R.string.text_download_failed);
                    break;

                default:
                    //get and show count
                    count = data.getLongExtra(MasterAddListActivity.ParamTypes.SuccessCount, 0);
                    message = res.getQuantityString(R.plurals.title_satellites_added, (int)count, (int)count);
                    isError = (count < 1);
                    break;
            }

            //show message
            Globals.showSnackBar(parentView, message, isError);
        }

        //return progress type
        return(progressType);
    }

    //Handles activity open file request
    public static void handleActivityOpenFileRequest(Activity activity, Intent data, int requestCode)
    {
        int index;
        int count;
        ArrayList<String> fileNames;
        ArrayList<File> files = new ArrayList<>(0);

        //handle based on request code
        switch(requestCode)
        {
            case RequestCode.GoogleDriveOpenFile:
            case RequestCode.DropboxOpenFile:
                //load from file if not already
                fileNames = data.getStringArrayListExtra(FileBrowserBaseActivity.ParamTypes.FileNames);
                count = data.getIntExtra(FileBrowserBaseActivity.ParamTypes.FilesDataCount, 0);
                for(index = 0; index < count; index++)
                {
                    //add file to list
                    files.add((File)data.getSerializableExtra(FileBrowserBaseActivity.ParamTypes.Files + index));
                }
                if(!UpdateService.isRunning(UpdateService.UpdateType.LoadFile))
                {
                    //load file data
                    UpdateService.loadFileData(activity, fileNames, files, null);
                }
                break;

            case RequestCode.OthersOpenItem:
                ClipData multiData = data.getClipData();
                ArrayList<Uri> fileList = new ArrayList<>(0);

                //if there are multiple files
                if(multiData != null)
                {
                    //go through each file
                    for(index = 0; index < multiData.getItemCount(); index++)
                    {
                        //add current file to list
                        fileList.add(multiData.getItemAt(index).getUri());
                    }
                }
                else
                {
                    //add file to list
                    fileList.add(data.getData());
                }

                //load all files
                UpdateService.loadFileData(activity, null, null, fileList);
                break;
        }
    }

    //Handles activity Google Drive file browser request
    public static void handleActivityGoogleDriveOpenFileBrowserRequest(Activity activity, ActivityResultLauncher<Intent> launcher, View parentView, Intent data, boolean isOkay)
    {
        boolean isError = true;

        //if signed in
        if(isOkay)
        {
            //try to get account
            Task<GoogleSignInAccount> getAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);

            //if got account
            if(getAccountTask.isSuccessful())
            {
                //note: don't confirm internet since would have done already to get past sign in

                //show google drive file browser
                Orbitals.showGoogleDriveFileBrowser(activity, launcher, false);

                //no error
                isError = false;
            }
        }

        //if there was an error
        if(isError)
        {
            //show error message
            Globals.showSnackBar(parentView, activity.getString(R.string.text_login_failed), true);
        }
    }

    //Handles activity SD card open files request
    public static void handleActivitySDCardOpenFilesRequest(Activity activity, Intent data)
    {
        //load from file if not already
        ArrayList<String> fileNames = data.getStringArrayListExtra(FileBrowserBaseActivity.ParamTypes.FileNames);
        if(!UpdateService.isRunning(UpdateService.UpdateType.LoadFile))
        {
            UpdateService.loadFile(activity, fileNames);
        }
    }
}
