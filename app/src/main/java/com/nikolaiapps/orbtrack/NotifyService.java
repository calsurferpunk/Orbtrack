package com.nikolaiapps.orbtrack;


import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.MutableLiveData;
import java.util.Calendar;


public abstract class NotifyService extends IntentService
{
    //Param Types
    public static abstract class ParamTypes
    {
        static final String MessageType = "messageType";
        static final String ProgressType = "progressType";
        static final String RunForeground = "fromAlarm";
        static final String SubIndex = "subIndex";
        static final String SubCount = "subCount";
        static final String Id = "id";
        static final String Index = "index";
        static final String Count = "count";
        static final String NotifyTitle = "notifyTitle";
        static final String Section = "section";
    }

    //Message types
    public static abstract class MessageTypes
    {
        static final byte General = 0;
        static final byte Download = 1;
        static final byte Load = 2;
        static final byte LoadPercent = 3;
        static final byte Parse = 4;
        static final byte Save = 5;
    }

    //Notification settings
    protected static class NotifySettings
    {
        public String title;
        public String message;
        public int progress;
        public int maxProgress;
        public boolean indeterminate;
        public long timeMs;

        public NotifySettings()
        {
            title = "";
            message = "";
            progress = maxProgress = 0;
            indeterminate = false;
            timeMs = 0;
        }
        public NotifySettings(NotifySettings from)
        {
            title = from.title;
            message = from.message;
            progress = from.progress;
            maxProgress = from.maxProgress;
            indeterminate = from.indeterminate;
            timeMs = from.timeMs;
        }

        public boolean equals(NotifySettings other)
        {
            return(title.equals(other.title) && message.equals(other.message) && progress == other.progress && maxProgress == other.maxProgress && indeterminate == other.indeterminate);
        }
    }

    //Alarm receiver
    public static abstract class NotifyReceiver extends BroadcastReceiver
    {
        public static final String RetryAction = "RetryAction";
        public static final String CancelAction = "CancelAction";
        public static final String DismissAction = "DismissAction";
        public static final String SettingsAction = "SettingsAction";
        public static final long MAX_WAKE_LOCK_MS = 900000;             //15 minutes

        protected abstract void onBootCompleted(Context context, long timeNowMs, AlarmManager manager);
        protected abstract void onRunService(Context context, Intent intent);
        protected abstract void onCloseNotification(Context context, Intent intent, NotificationManagerCompat manager);
        protected void onSettings(Context context, Intent intent) {}
        protected void onCancel(Context context, Intent intent) {}

        @Override
        public void onReceive(Context context, Intent intent)
        {
            boolean isRetry;
            boolean isCancel;
            boolean isDismiss;
            boolean runService = false;
            boolean closeNotification = false;
            String action = intent.getAction();

            //if action is not set
            if(action == null)
            {
                //set to empty
                action = "";
            }

            //update status
            isRetry = action.equals(RetryAction);
            isCancel = action.equals(CancelAction);
            isDismiss = action.equals(DismissAction);

            //if from boot completed
            if(action.equals(Intent.ACTION_BOOT_COMPLETED))
            {
                //handle on boot completed
                onBootCompleted(context, Calendar.getInstance().getTimeInMillis(), (AlarmManager)context.getSystemService(Context.ALARM_SERVICE));
            }
            //else if settings
            else if(action.equals(SettingsAction))
            {
                //handle on settings and close notification
                onSettings(context, intent);
                closeNotification = true;
            }
            //else if retrying, canceling, or dismissing
            else if(isRetry || isCancel || isDismiss)
            {
                //close notification
                closeNotification = true;

                //run service if retrying
                runService = isRetry;

                //if canceling
                if(isCancel)
                {
                    //cancel
                    onCancel(context, intent);
                }
            }
            else
            {
                //run service
                runService = true;
            }

            //if closing notification
            if(closeNotification)
            {
                //close notification
                onCloseNotification(context, intent, NotificationManagerCompat.from(context));
            }

            //if running service
            if(runService)
            {
                //run service
                onRunService(context, intent);
            }
        }
    }

    private final String wakeTag;
    protected final String notifyChannelId;
    protected NotifySettings currentNotify;
    private NotifySettings lastNotify;
    private NotificationManagerCompat notifyManger;

    public NotifyService(String name, String channelId)
    {
        super(name);
        wakeTag = name;
        notifyChannelId = channelId;
    }

    protected abstract void onRunIntent(Intent intent);
    protected abstract void onClearIntent(byte index);

    @Override
    protected void onHandleIntent(Intent intent)
    {
        PowerManager powerService = (PowerManager)getSystemService(POWER_SERVICE);
        PowerManager.WakeLock keepAwake = (powerService != null ? powerService.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeTag) : null);

        //stay awake
        if(keepAwake != null)
        {
            //try to get wake lock
            try
            {
                keepAwake.acquire(NotifyReceiver.MAX_WAKE_LOCK_MS);
            }
            catch(Exception ex)
            {
                keepAwake = null;
            }
        }

        //create notification channel
        Globals.createNotifyChannel(this, notifyChannelId);

        //create notification items
        currentNotify = new NotifySettings();
        lastNotify = new NotifySettings();
        notifyManger = NotificationManagerCompat.from(this);
        currentNotify.progress = 0;
        currentNotify.maxProgress = 0;
        currentNotify.indeterminate = true;

        //run intent
        onRunIntent(intent);

        //don't keep awake
        if(keepAwake != null)
        {
            //try to release wake lock
            try
            {
                keepAwake.release();
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }
    }

    //Updates notification and it's visibility
    protected void updateNotification(NotificationCompat.Builder notifyBuilder, int updateID, long limitMs, boolean showNotification)
    {
        long delayMs;

        //if showing notification
        if(showNotification)
        {
            //if notification exists and is changing
            if(notifyBuilder != null && !currentNotify.equals(lastNotify))
            {
                //if there is any limit
                if(limitMs > 0)
                {
                    //handle any wait
                    delayMs = System.currentTimeMillis() - lastNotify.timeMs;
                    if(delayMs < limitMs)
                    {
                        //sleep until at least 1000 ms has passed
                        SystemClock.sleep(limitMs - delayMs);
                    }
                }

                //update notification
                notifyBuilder.setContentTitle(currentNotify.title).setContentText(currentNotify.message);
                notifyBuilder.setProgress(currentNotify.maxProgress, currentNotify.progress, currentNotify.indeterminate);
                currentNotify.timeMs = System.currentTimeMillis();
                lastNotify = new NotifySettings(currentNotify);

                //show notification
                Globals.showNotification(this, updateID, notifyBuilder.build());
            }
        }
        else
        {
            //close notification
            notifyManger.cancel(updateID);
        }
    }

    //Creates an action
    private NotificationCompat.Action createAction(String actionString, int actionId, Class<?> receiverClass, String serviceParam, byte serviceIndex)
    {
        int stringId;
        Intent actionIntent = new Intent(this, receiverClass);

        //setup action
        actionIntent.setAction(actionString);
        actionIntent.putExtra(serviceParam, serviceIndex);
        switch(actionString)
        {
            case NotifyReceiver.RetryAction:
                stringId = R.string.title_retry;
                break;

            case NotifyReceiver.CancelAction:
                stringId = R.string.title_cancel;
                break;

            case NotifyReceiver.DismissAction:
            default:
                stringId = R.string.title_dismiss;
                break;
        }

        //return action
        return(new NotificationCompat.Action(0, this.getString(stringId), Globals.getPendingBroadcastIntent(this, actionId, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
    }

    //Sends a message
    protected void sendMessage(MutableLiveData<Intent> localBroadcast, byte messageType, byte serviceIndex, String serviceParam, int id, String titleDesc, String section, String filter, Class<?> receiverClass, long subIndex, long subCount, long index, long count, int progressType, int updateID, int dismissID, int cancelID, int retryID, boolean showNotification, Bundle extraData)
    {
        boolean isGeneral = (messageType == MessageTypes.General);
        boolean isStarted = (progressType == Globals.ProgressType.Started);
        boolean isFinished = (progressType == Globals.ProgressType.Finished);
        boolean isCanceled = (progressType == Globals.ProgressType.Cancelled);
        boolean showCancel = (cancelID >= 0);
        boolean haveSection = (section != null && !section.isEmpty());
        boolean haveSubIndex = (subIndex >= 0);
        boolean enoughProgress = (System.currentTimeMillis() - lastNotify.timeMs >= 250);
        long limitMs = 0;
        NotificationCompat.Builder notifyBuilder = (showNotification ? Globals.createNotifyBuilder(this, notifyChannelId) : null);

        //update notification
        switch(progressType)
        {
            case Globals.ProgressType.Started:
            case Globals.ProgressType.Failed:
            case Globals.ProgressType.Finished:
            case Globals.ProgressType.Cancelled:
            case Globals.ProgressType.Denied:
                //always update
                enoughProgress = true;

                //if the main update
                if(isGeneral)
                {
                    //if didn't start
                    if(!isStarted)
                    {
                        //set as done
                        onClearIntent(serviceIndex);
                    }

                    //if showing notification
                    if(showNotification)
                    {
                        Resources res = this.getResources();

                        //update message
                        currentNotify.title = res.getString(isStarted ? R.string.title_start : isFinished ? R.string.title_finished : isCanceled ? R.string.title_canceled : R.string.title_failed) + " " + titleDesc;
                        currentNotify.message = "";
                        currentNotify.progress = 0;
                        currentNotify.maxProgress = 0;
                        currentNotify.indeterminate = false;
                        limitMs = 1000;     //make sure at least 1 second passed from last

                        //if didn't start
                        if(!isStarted)
                        {
                            //if didn't finish
                            if(!isFinished)
                            {
                                //add retry button
                                notifyBuilder.addAction(createAction(NotifyReceiver.RetryAction, retryID, receiverClass, serviceParam, serviceIndex));
                            }

                            //add dismiss button
                            notifyBuilder.addAction(createAction(NotifyReceiver.DismissAction, dismissID, receiverClass, serviceParam, serviceIndex));

                            //don't show cancel
                            showCancel = false;
                        }
                        break;
                    }
                }
                //fall through

            default:
                //if enough progress to send
                if(enoughProgress)
                {
                    //update status
                    currentNotify.message = ((haveSection ? section : "") + (count > 0 ? (" (" + (int)((index / (float)count) * 100) + "%)") : ""));
                    currentNotify.progress = (int)(haveSubIndex ? subIndex : index);
                    currentNotify.maxProgress = (int)(haveSubIndex ? 100 : count);
                    currentNotify.indeterminate = false;
                }
                break;
        }

        //if enough progress
        if(enoughProgress)
        {
            //update broadcast
            Intent intent = new Intent(filter);
            intent.putExtra(ParamTypes.MessageType, messageType);
            intent.putExtra(serviceParam, serviceIndex);
            if(haveSection)
            {
                intent.putExtra(ParamTypes.Section, section);
            }
            intent.putExtra(ParamTypes.Id, id);
            intent.putExtra(ParamTypes.SubIndex, subIndex);
            intent.putExtra(ParamTypes.SubCount, subCount);
            intent.putExtra(ParamTypes.Index, index);
            intent.putExtra(ParamTypes.Count, count);
            intent.putExtra(ParamTypes.ProgressType, progressType);
            if(extraData != null)
            {
                intent.putExtras(extraData);
            }

            //send intent and update notification
            if(localBroadcast != null)
            {
                Globals.setBroadcastValue(this, localBroadcast, intent);
            }
            if(notifyBuilder != null && showCancel)
            {
                //add cancel button
                notifyBuilder.addAction(createAction(NotifyReceiver.CancelAction, cancelID, receiverClass, serviceParam, serviceIndex));
            }
            updateNotification(notifyBuilder, updateID, limitMs, showNotification);
        }
    }
}
