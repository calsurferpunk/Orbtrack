package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;


public abstract class BaseInputActivity extends AppCompatActivity
{
    //Activity result codes
    public static abstract class RequestCode
    {
        static final byte None = 0;
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
        static final byte Settings = 20;
        static final byte ExactAlarm = 21;
        static final byte ExactAlarmRetry = 22;
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
            actionBar.setBackgroundDrawable(ResourcesCompat.getDrawable(context.getResources(), Globals.resolveAttributeID(context, R.attr.actionBarBackground), null));
        }
    }

    //Locks screen orientation
    public void lockScreenOrientation(boolean lock)
    {
        Globals.lockScreenOrientation(this, lock);
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
    public static void handleActivityOpenFileRequest(Activity activity, View parentView, Intent data, int requestCode)
    {
        int index;
        int count;
        ArrayList<String> fileNames;
        ArrayList<String> fileAscii = new ArrayList<>(0);
        ArrayList<byte[]> filesData = new ArrayList<>(0);

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
                    filesData.add(data.getByteArrayExtra(FileBrowserBaseActivity.ParamTypes.FilesData + index));
                }
                if(!UpdateService.isRunning(UpdateService.UpdateType.LoadFile))
                {
                    //go through each file
                    for(index = 0; index < fileNames.size(); index++)
                    {
                        //remember file name, extension, and data
                        String currentName = fileNames.get(index);
                        String currentExtension = Globals.getFileExtension(currentName);
                        byte[] currentData = filesData.get(index);

                        try
                        {
                            //if a .zip
                            if(currentExtension.equalsIgnoreCase(".zip"))
                            {
                                //get all usable files in the .zip file
                                InputStream[] zipFileStreams = Globals.readZipFile(activity, "files", new ByteArrayInputStream(currentData), Globals.fileDataExtensions);
                                for(InputStream currentFileStream : zipFileStreams)
                                {
                                    //add file data
                                    fileAscii.add(Globals.readTextFile(activity, currentFileStream));
                                    currentFileStream.close();
                                }
                            }
                            else
                            {
                                //try to get text file from data
                                fileAscii.add(Globals.readTextFile(activity, new ByteArrayInputStream(currentData)));
                            }
                        }
                        catch(Exception ex)
                        {
                            //do nothing
                        }
                    }

                    //load file data
                    UpdateService.loadFileData(activity, fileAscii);
                }
                break;

            case RequestCode.OthersOpenItem:
                try
                {
                    InputStream fileStream;
                    ClipData multiData = data.getClipData();
                    ContentResolver resolver = activity.getContentResolver();
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

                    //go through each file
                    for(index = 0; index < fileList.size(); index++)
                    {
                        //remember current file, path, and extension
                        Uri currentFile = fileList.get(index);
                        String currentPath = currentFile.getPath();
                        String currentExtension = Globals.getFileExtension(currentPath);

                        //if current scheme has content
                        if(currentFile.getScheme().equals("content"))
                        {
                            //if able to get cursor
                            Cursor currentCursor = resolver.query(currentFile, null, null, null, null);
                            if(currentCursor != null)
                            {
                                try
                                {
                                    //if able to get first row
                                    if(currentCursor.moveToFirst())
                                    {
                                        //if able to get display name
                                        int nameIndex = currentCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                                        String cursorResult = (nameIndex >= 0 ? currentCursor.getString(nameIndex) : null);
                                        if(cursorResult != null)
                                        {
                                            //update path and extension
                                            currentPath = cursorResult;
                                            currentExtension = Globals.getFileExtension(currentPath);
                                        }
                                    }
                                    currentCursor.close();
                                }
                                catch(Exception ex)
                                {
                                    //do nothing
                                }
                            }
                        }

                        //read file data
                        fileStream = resolver.openInputStream(fileList.get(index));
                        if(currentExtension.equals(".zip"))
                        {
                            //get all usable files in the .zip file
                            InputStream[] zipFileStreams = Globals.readZipFile(activity, "files", fileStream, Globals.fileDataExtensions);
                            for(InputStream currentFileStream : zipFileStreams)
                            {
                                //add file data
                                fileAscii.add(Globals.readTextFile(activity, currentFileStream));
                                currentFileStream.close();
                            }
                        }
                        else if(currentExtension.equals(".tle") || currentExtension.equals(".json") || currentExtension.equals(".txt"))
                        {
                            //add file data
                            fileAscii.add(Globals.readTextFile(activity, fileStream));
                        }
                        fileStream.close();
                    }

                    //load all files
                    UpdateService.loadFileData(activity, fileAscii);
                }
                catch(Exception ex)
                {
                    //show error
                    Globals.showSnackBar(parentView, activity.getString(R.string.text_file_error_using), ex.getMessage(), true, true);
                }
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
