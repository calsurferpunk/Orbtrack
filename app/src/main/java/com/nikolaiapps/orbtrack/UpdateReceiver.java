package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import androidx.lifecycle.Observer;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import java.io.File;
import java.util.ArrayList;


public abstract class UpdateReceiver
{
    private Observer<Intent> observer = null;

    //Returns parent view
    protected View getParentView()
    {
        //needs to be overridden
        return(null);
    }

    //Returns database progress dialog
    protected MultiProgressDialog getDatabaseProgressDialog()
    {
        //needs to be overridden
        return(null);
    }

    //On pending satellites to load
    protected void onLoadPending(ArrayList<Database.DatabaseSatellite> pendingLoadSatellites)
    {
        //needs to be overridden
    }

    //On database update
    protected void onDatabaseUpdated()
    {
        //needs to be overridden
    }

    //On progress
    protected void onProgress(long updateValue, long updateCount, String section)
    {
        //needs to be overridden
    }

    //On load file update
    protected void onLoadFileUpdate(int progressType, String section, int percent, int overallPercent)
    {
        //needs to be overridden
    }

    //On got information
    protected void onGotInformation(Spanned infoText, int index)
    {
        //needs to be overridden
    }

    //On general update
    protected void onGeneralUpdate(int progressType, byte updateType, boolean ended, String section, int index, int count, File usedFile)
    {
        //needs to be overridden
    }

    //On download update
    protected void onDownloadUpdate(int progressType, byte updateType, long updateValue, long updateCount)
    {
        //needs to be overridden
    }

    //Register receiver
    public void register(Context context)
    {
        observer = new Observer<Intent>()
        {
            @Override
            public void onChanged(Intent intent)
            {
                if(intent != null)
                {
                    UpdateReceiver.this.onReceive(context, intent);
                }
            }
        };
        UpdateService.observe(context, observer);
    }

    //Unregister receiver
    public void unregister()
    {
        UpdateService.removeObserver(observer);
    }

    public void onReceive(Context context, Intent intent)
    {
        final byte updateType = intent.getByteExtra(UpdateService.ParamTypes.UpdateType, Byte.MAX_VALUE);
        final byte messageType = intent.getByteExtra(NotifyService.ParamTypes.MessageType, Byte.MAX_VALUE);
        final int progressType = intent.getIntExtra(NotifyService.ParamTypes.ProgressType, Byte.MAX_VALUE);
        final int index = (int)intent.getLongExtra(NotifyService.ParamTypes.Index, 0);
        final long count = intent.getLongExtra(NotifyService.ParamTypes.Count, 0);
        final int overall = (int)intent.getLongExtra(UpdateService.ParamTypes.SubIndex, -1);
        long countValue;
        final String section = intent.getStringExtra(NotifyService.ParamTypes.Section);
        Bundle extraData = intent.getExtras();
        boolean ended;
        boolean isGeneral = (messageType == NotifyService.MessageTypes.General);
        String infoString;
        Spanned infoText;
        Resources res = context.getResources();

        //if extra data not set
        if(extraData == null)
        {
            //create empty
            extraData = new Bundle();
        }

        //handle based on progress type
        switch(progressType)
        {
            case Globals.ProgressType.Started:
                //if updating satellites and have a valid index/count
                if(index >= 0 && index < count)
                {
                    //call on progress
                    onProgress(index + 1, count, (section == null ? "" : section));
                }
                //fall through

            case Globals.ProgressType.Finished:
            case Globals.ProgressType.Cancelled:
            case Globals.ProgressType.Denied:
            case Globals.ProgressType.Failed:
                //ending if not starting
                ended = (progressType != Globals.ProgressType.Started);

                //if MessageType.General
                if(isGeneral)
                {
                    //handle based on update type
                    switch(updateType)
                    {
                        case UpdateService.UpdateType.LoadFile:
                            //if ended
                            if(ended)
                            {
                                //check for any pending satellites to load and if error
                                ArrayList<Database.DatabaseSatellite> pendingLoadSatellites = extraData.getParcelableArrayList(UpdateService.ParamTypes.PendingSatellites);
                                int pendingCount = (pendingLoadSatellites != null ? pendingLoadSatellites.size() : 0);
                                boolean isError = ((progressType != Globals.ProgressType.Finished || count < 1) && (pendingCount < 1));

                                //if there are pending satellites
                                if(pendingCount > 0)
                                {
                                    //call on load pending
                                    onLoadPending(pendingLoadSatellites);
                                }

                                //if error or done with some
                                if(isError || count > 0)
                                {
                                    //show message
                                    Globals.showSnackBar(getParentView(), (!isError ? (res.getString(R.string.text_reading_success) + " " + count + " " + res.getString(R.string.text_items_from_file)) : res.getString(R.string.text_file_error_using)), isError);
                                }
                            }
                            //fall through

                        case UpdateService.UpdateType.BuildDatabase:
                            //if ended and really building database
                            if(ended && updateType == UpdateService.UpdateType.BuildDatabase)
                            {
                                //call on database updated
                                onDatabaseUpdated();
                            }
                            //fall through

                        case UpdateService.UpdateType.UpdateSatellites:
                        case UpdateService.UpdateType.GetMasterList:
                        case UpdateService.UpdateType.SaveFile:
                            //call on general update
                            onGeneralUpdate(progressType, updateType, ended, (section == null ? "" : section), index, (int)count, (File)extraData.getSerializable(UpdateService.ParamTypes.UsedFile));
                            break;
                    }
                }
                else if(updateType == UpdateService.UpdateType.GetInformation)
                {
                    //if ended
                    if(ended)
                    {
                        //get information
                        infoString = (String)extraData.getSerializable(UpdateService.ParamTypes.Information);
                        if(infoString != null)
                        {
                            infoText = (Build.VERSION.SDK_INT >= 24 ? Html.fromHtml(infoString, Html.FROM_HTML_MODE_COMPACT) : Html.fromHtml(infoString));
                        }
                        else
                        {
                            infoText = null;
                        }

                        //call on got information
                        onGotInformation(infoText, index);
                    }
                }
                break;

            case Globals.ProgressType.Running:
                //handle based on update type
                switch(updateType)
                {
                    case UpdateService.UpdateType.BuildDatabase:
                        //get dialog
                        MultiProgressDialog databaseProgress = getDatabaseProgressDialog();

                        //if progress display still exists and section is set
                        if(databaseProgress != null && section != null)
                        {
                            //update display
                            countValue = index + 1;
                            databaseProgress.setMessage(section + " (" + res.getQuantityString(R.plurals.title_space_of_space, (int)count, (index + 1), count) + ")");
                            databaseProgress.setProgress(countValue, count);
                        }
                        break;

                    case UpdateService.UpdateType.LoadFile:
                        //if loading
                        if(messageType == NotifyService.MessageTypes.Load)
                        {
                            //call on load file update
                            countValue = (count > 0 ? (int)(((index + 1) / (float)count) * 100) : 0);
                            onLoadFileUpdate(progressType, section, (int)countValue, overall);
                        }
                        break;
                }
                break;
        }

        //if a download message
        if(messageType == NotifyService.MessageTypes.Download)
        {
            //call on download update
            onDownloadUpdate(progressType, updateType, index + 1, count);
        }
    }
}
