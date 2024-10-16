package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.SwitchCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;


public class NotifySettingsActivity extends BaseInputActivity
{
    public static abstract class ParamTypes
    {
        static final String NoradID = "noradId";
        static final String Location = "location";
    }

    private int noradId;
    private boolean useList;
    private Intent resultData;
    private Calculations.ObserverType location;
    private View fullMoonStartDivider;
    private View fullMoonEndDivider;
    private SelectListInterface orbitalList;
    private LinearLayout fullMoonStartLayout;
    private LinearLayout fullMoonEndLayout;
    private ActivityResultLauncher<Intent> resultLauncher;
    private RadioGroup[] notifyGroup;
    private SwitchCompat[] notifySwitch;
    private AppCompatRadioButton[] notifyNext;
    private AppCompatRadioButton[] notifyAll;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.notify_settings_layout);

        byte index;
        Intent intent = this.getIntent();
        String titleString = this.getString(R.string.title_notifications);
        Database.DatabaseSatellite currentOrbital;
        final LinearLayout notifySettingsLayout = this.findViewById(R.id.Notify_Settings_Layout);
        final View listDivider = this.findViewById(R.id.Notify_Settings_List_Divider);
        final View orbitalGroup = this.findViewById(R.id.Notify_Settings_Orbital_Group);
        final TextView notificationsTitle = this.findViewById(R.id.Notify_Settings_Notifications_Title);
        final MaterialButton cancelButton = this.findViewById(R.id.Notify_Settings_Cancel_Button);
        final MaterialButton okayButton = this.findViewById(R.id.Notify_Settings_Ok_Button);

        //hide action bar
        hideActionBar();

        //get displays
        fullMoonStartDivider = this.findViewById(R.id.Notify_Settings_Full_Moon_Start_Divider);
        fullMoonEndDivider = this.findViewById(R.id.Notify_Settings_Full_Moon_End_Divider);
        orbitalList = this.findViewById(R.id.Notify_Settings_Orbital_Text_List);
        notifySwitch = new SwitchCompat[Globals.NotifyType.NotifyCount];
        notifySwitch[Globals.NotifyType.PassStart] = this.findViewById(R.id.Notify_Settings_Pass_Start_Switch);
        notifySwitch[Globals.NotifyType.PassEnd] = this.findViewById(R.id.Notify_Settings_Pass_End_Switch);
        notifySwitch[Globals.NotifyType.FullMoonStart] = this.findViewById(R.id.Notify_Settings_Full_Moon_Start_Switch);
        notifySwitch[Globals.NotifyType.FullMoonEnd] = this.findViewById(R.id.Notify_Settings_Full_Moon_End_Switch);
        notifyNext = new AppCompatRadioButton[Globals.NotifyType.NotifyCount];
        notifyAll = new AppCompatRadioButton[Globals.NotifyType.NotifyCount];
        notifyNext[Globals.NotifyType.PassStart] = this.findViewById(R.id.Notify_Settings_Pass_Start_Next_Radio);
        notifyAll[Globals.NotifyType.PassStart] = this.findViewById(R.id.Notify_Settings_Pass_Start_All_Radio);
        notifyNext[Globals.NotifyType.PassEnd] = this.findViewById(R.id.Notify_Settings_Pass_End_Next_Radio);
        notifyAll[Globals.NotifyType.PassEnd] = this.findViewById(R.id.Notify_Settings_Pass_End_All_Radio);
        notifyNext[Globals.NotifyType.FullMoonStart] = this.findViewById(R.id.Notify_Settings_Full_Moon_Start_Next_Radio);
        notifyAll[Globals.NotifyType.FullMoonStart] = this.findViewById(R.id.Notify_Settings_Full_Moon_Start_All_Radio);
        notifyNext[Globals.NotifyType.FullMoonEnd] = this.findViewById(R.id.Notify_Settings_Full_Moon_End_Next_Radio);
        notifyAll[Globals.NotifyType.FullMoonEnd] = this.findViewById(R.id.Notify_Settings_Full_Moon_End_All_Radio);
        fullMoonStartLayout = this.findViewById(R.id.Notify_Settings_Full_Moon_Start_Layout);
        fullMoonEndLayout = this.findViewById(R.id.Notify_Settings_Full_Moon_End_Layout);
        notifyGroup = new RadioGroup[Globals.NotifyType.NotifyCount];
        notifyGroup[Globals.NotifyType.PassStart] = this.findViewById(R.id.Notify_Settings_Pass_Start_Group);
        notifyGroup[Globals.NotifyType.PassEnd] = this.findViewById(R.id.Notify_Settings_Pass_End_Group);
        notifyGroup[Globals.NotifyType.FullMoonStart] = this.findViewById(R.id.Notify_Settings_Full_Moon_Start_Group);
        notifyGroup[Globals.NotifyType.FullMoonEnd] = this.findViewById(R.id.Notify_Settings_Full_Moon_End_Group);

        //setup edges
        setupViewEdges(notifySettingsLayout, EdgeDistance.TOP_AND_BOTTOM_BAR);

        //if intent not set
        if(intent == null)
        {
            //create empty
            intent = new Intent();
        }

        //get params
        noradId = intent.getIntExtra(ParamTypes.NoradID, Universe.IDs.Invalid);
        location = intent.getParcelableExtra(ParamTypes.Location);
        useList = (noradId == Universe.IDs.Invalid);
        currentOrbital = (useList ? null : Database.getOrbital(this, noradId));

        //set defaults
        resultData = new Intent();
        BaseInputActivity.setRequestCode(resultData, BaseInputActivity.getRequestCode(intent));

        //if -found orbital or using list- and -valid location-
        if((useList || currentOrbital != null) && location != null)
        {
            //set events
            for(index = 0; index < Globals.NotifyType.NotifyCount; index++)
            {
                notifySwitch[index].setOnCheckedChangeListener(createOnCheckedChangedListener(notifyGroup[index]));
            }
            okayButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    //handle setting notifications
                    handleSettingNotifications(noradId, location, useList, false, resultData);
                }
            });
            cancelButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    //stop
                    setResult(RESULT_CANCELED, resultData);
                    NotifySettingsActivity.this.finish();
                }
            });
            orbitalList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    updateDisplays((int)orbitalList.getSelectedValue(Universe.IDs.Invalid));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
            resultLauncher = Globals.createActivityLauncher(this, new ActivityResultCallback<>()
            {
                @Override
                public void onActivityResult(ActivityResult result)
                {
                    //handle setting notifications
                    handleSettingNotifications(noradId, location, useList, true, resultData);
                }
            });

            //update displays
            orbitalGroup.setVisibility(useList ? View.VISIBLE : View.GONE);
            listDivider.setVisibility(useList ? View.VISIBLE : View.GONE);
            if(!useList)
            {
                //update displays
                if(currentOrbital != null)
                {
                    titleString = currentOrbital.getName() + " " + titleString;
                }
                updateDisplays(noradId);
            }
            else
            {
                //update displays
                orbitalList.setAdapter(new IconSpinner.CustomAdapter(this, orbitalList, Database.getOrbitals(this)));
                orbitalList.setSelectedValue(noradId);
            }
            notificationsTitle.setText(titleString);
        }
        else
        {
            //stop
            this.finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        boolean granted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        boolean retrying = (requestCode == Globals.PermissionType.PostNotificationsRetry);

        //handle response
        switch(requestCode)
        {
            case Globals.PermissionType.PostNotifications:
            case Globals.PermissionType.PostNotificationsRetry:
                //if granted
                if(granted)
                {
                    //set notification
                    setNotifications(noradId, location, useList, resultData);
                }
                //else if not retrying
                else if(!retrying)
                {
                    //ask permission again
                    Globals.askPostNotificationsPermission(this, true);
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //Sets notifications
    private void setNotifications(int noradId, Calculations.ObserverType location, boolean useList, Intent resultData)
    {
        byte index;
        int id = (useList ? (int)orbitalList.getSelectedValue(Universe.IDs.Invalid) : noradId);
        boolean[] notifyUsing = new boolean[Globals.NotifyType.NotifyCount];
        boolean[] notifyNextChecked = new boolean[Globals.NotifyType.NotifyCount];

        //if have permission to use notifications
        if(Globals.havePostNotificationsPermission(this))
        {
            //go through each notify type
            for(index = 0; index < Globals.NotifyType.NotifyCount; index++)
            {
                //update status
                notifyUsing[index] = notifySwitch[index].isChecked();
                notifyNextChecked[index] = notifyNext[index].isChecked();
            }

            //set notifications
            Settings.setNotify(this, id, location, notifyUsing, notifyNextChecked);

            //set result
            setResult(RESULT_OK, resultData);
            this.finish();
        }
        //else if can ask for permission
        else if(Globals.canAskPostNotificationsPermission)
        {
            //ask permission
            Globals.askPostNotificationsPermission(this, false);
        }
    }

    //Handles settings notifications
    private void handleSettingNotifications(int noradId, Calculations.ObserverType location, boolean useList, boolean retrying, Intent resultData)
    {
        //done if -have permission to set exact timer- or -can't ask-
        if(Globals.haveExactAlarmPermission(NotifySettingsActivity.this) || !Globals.canAskExactAlarmPermission)
        {
            //set notifications
            setNotifications(noradId, location, useList, resultData);
        }
        else
        {
            //ask permission
            Globals.askExactAlarmPermission(NotifySettingsActivity.this, resultLauncher, retrying, new Globals.OnDenyListener()
            {
                @Override
                public void OnDeny(byte resultCode)
                {
                    //set notifications with denial
                    setNotifications(noradId, location, useList, resultData);
                }
            });
        }
    }

    //Updates displays with the given settings
    private void updateDisplays(SwitchCompat passSwitch, AppCompatRadioButton passNext, AppCompatRadioButton passAll, CalculateService.AlarmNotifySettings settings)
    {
        passSwitch.setChecked(settings.isEnabled());
        passNext.setChecked(settings.nextOnly);
        passAll.setChecked(!settings.nextOnly);
    }
    private void updateDisplays(int noradId)
    {
        byte index;
        int moonVisibility = (noradId == Universe.IDs.Moon ? View.VISIBLE : View.GONE);

        //update displays
        for(index = 0; index < Globals.NotifyType.NotifyCount; index++)
        {
            updateDisplays(notifySwitch[index], notifyNext[index], notifyAll[index], Settings.getNotifyPassSettings(NotifySettingsActivity.this, noradId, index));
        }
        if(fullMoonStartDivider != null)
        {
            fullMoonStartDivider.setVisibility(moonVisibility);
        }
        if(fullMoonEndDivider != null)
        {
            fullMoonEndDivider.setVisibility(moonVisibility);
        }
        fullMoonStartLayout.setVisibility(moonVisibility);
        fullMoonEndLayout.setVisibility(moonVisibility);
        if(moonVisibility == View.GONE)
        {
            notifyGroup[Globals.NotifyType.FullMoonStart].setVisibility(View.GONE);
            notifyGroup[Globals.NotifyType.FullMoonEnd].setVisibility(View.GONE);
        }
    }

    //Creates an on checked change listener
    private CompoundButton.OnCheckedChangeListener createOnCheckedChangedListener(final RadioGroup group)
    {
        return(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                //update visibility
                group.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
    }

    //Shows the dialog
    public static void show(Context context, ActivityResultLauncher<Intent> launcher, int noradId, Calculations.ObserverType location)
    {
        Intent notifyIntent = new Intent(context, NotifySettingsActivity.class);
        if(noradId != Integer.MIN_VALUE)
        {
            notifyIntent.putExtra(NotifySettingsActivity.ParamTypes.NoradID, noradId);
        }
        notifyIntent.putExtra(NotifySettingsActivity.ParamTypes.Location, location);
        if(context instanceof Activity && launcher != null)
        {
            Globals.startActivityForResult(launcher, notifyIntent, RequestCode.EditNotify);
        }
        else
        {
            notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(notifyIntent);
        }
    }
    public static void show(Activity context, ActivityResultLauncher<Intent> launcher, Calculations.ObserverType location)
    {
        show(context, launcher, Integer.MIN_VALUE, location);
    }
}
