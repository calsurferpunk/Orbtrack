package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.SwitchCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;


public class NotifySettingsActivity extends BaseInputActivity
{
    public static abstract class ParamTypes
    {
        static final String NoradID = "noradId";
        static final String Location = "location";
    }

    private View fullMoonStartDivider;
    private View fullMoonEndDivider;
    private IconSpinner orbitalList;
    private LinearLayout fullMoonStartLayout;
    private LinearLayout fullMoonEndLayout;
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
        final int noradId;
        final boolean useList;
        String text;
        Intent intent = this.getIntent();
        Database.DatabaseSatellite currentOrbital;
        final Calculations.ObserverType location;
        final View divider = this.findViewById(R.id.Notify_Settings_Divider);
        final View orbitalGroup = this.findViewById(R.id.Notify_Settings_Orbital_Group);
        final TextView notificationsTitle = this.findViewById(R.id.Notify_Settings_Notifications_Title);
        final AppCompatButton cancelButton = this.findViewById(R.id.Notify_Settings_Cancel_Button);
        final AppCompatButton okayButton = this.findViewById(R.id.Notify_Settings_Ok_Button);

        //get displays
        fullMoonStartDivider = this.findViewById(R.id.Notify_Settings_Full_Moon_Start_Divider);
        fullMoonEndDivider = this.findViewById(R.id.Notify_Settings_Full_Moon_End_Divider);
        orbitalList = this.findViewById(R.id.Notify_Settings_Orbital_List);
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
                    byte index;
                    int id = (useList ? (int)orbitalList.getSelectedValue(Universe.IDs.Invalid) : noradId);
                    boolean[] notifyUsing = new boolean[Globals.NotifyType.NotifyCount];
                    boolean[] notifyNextChecked = new boolean[Globals.NotifyType.NotifyCount];

                    //go through each notify type
                    for(index = 0; index < Globals.NotifyType.NotifyCount; index++)
                    {
                        //update status
                        notifyUsing[index] = notifySwitch[index].isChecked();
                        notifyNextChecked[index] = notifyNext[index].isChecked();
                    }

                    //update settings
                    Settings.setNotify(NotifySettingsActivity.this, id, location, notifyUsing, notifyNextChecked);
                    setResult(RESULT_OK);
                    NotifySettingsActivity.this.finish();
                }
            });
            cancelButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    //stop
                    setResult(RESULT_CANCELED);
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

            //update displays
            orbitalGroup.setVisibility(useList ? View.VISIBLE : View.GONE);
            divider.setVisibility(useList ? View.VISIBLE : View.GONE);
            if(!useList)
            {
                //update displays
                text = currentOrbital.getName() + " " + notificationsTitle.getText();
                notificationsTitle.setText(text);
                updateDisplays(noradId);
            }
            else
            {
                //update list
                orbitalList.setAdapter(new IconSpinner.CustomAdapter(this, Database.getOrbitals(this)));
                orbitalList.setSelectedValue(noradId);
            }
        }
        else
        {
            //stop
            this.finish();
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
        fullMoonStartDivider.setVisibility(moonVisibility);
        fullMoonEndDivider.setVisibility(moonVisibility);
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
    public static void show(Context context, int noradId, Calculations.ObserverType location)
    {
        Intent notifyIntent = new Intent(context, NotifySettingsActivity.class);
        if(noradId != Integer.MIN_VALUE)
        {
            notifyIntent.putExtra(NotifySettingsActivity.ParamTypes.NoradID, noradId);
        }
        notifyIntent.putExtra(NotifySettingsActivity.ParamTypes.Location, location);
        if(context instanceof Activity)
        {
            ((Activity)context).startActivityForResult(notifyIntent, RequestCode.EditNotify);
        }
        else
        {
            notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(notifyIntent);
        }
    }
    public static void show(Activity context, Calculations.ObserverType location)
    {
        show(context, Integer.MIN_VALUE, location);
    }
}
