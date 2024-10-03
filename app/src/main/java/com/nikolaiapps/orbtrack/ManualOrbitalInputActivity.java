package com.nikolaiapps.orbtrack;


import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;


public class ManualOrbitalInputActivity extends BaseInputActivity
{
    private EditTextSelect nameText;
    private EditTextSelect line1Text;
    private EditTextSelect line2Text;
    private TextView currentLbl;
    private TextView currentCharLbl;
    private TextView currentTotalLbl;
    private DateInputView launchDate;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manual_object_input_layout);

        int index;
        final Intent resultIntent = new Intent();
        final LinearLayout manualLayout = this.findViewById(R.id.Manual_Layout);
        TextInputLayout nameTextLayout = this.findViewById(R.id.Manual_Object_Name_Text_Layout);
        ArrayList<UpdateService.MasterOwner> owners = Database.getOwners(this);
        ArrayList<IconSpinner.Item> ownerItems = new ArrayList<>(0);
        ArrayList<UpdateService.MasterCategory> categories = Database.getCategories(this);
        ArrayList<IconSpinner.Item> categoryItems = new ArrayList<>(0);

        //setup edges
        setupViewEdges(manualLayout, EdgeDistance.TOP_AND_ACTION_AND_BOTTOM_BAR);

        //setup result intent
        BaseInputActivity.setRequestCode(resultIntent, BaseInputActivity.getRequestCode(this.getIntent()));

        //get displays
        nameText = this.findViewById(R.id.Manual_Object_Name);
        line1Text = this.findViewById(R.id.Manual_Object_Line1_Text);
        line2Text = this.findViewById(R.id.Manual_Object_Line2_Text);
        currentLbl = this.findViewById(R.id.Manual_Object_Current_Lbl);
        currentCharLbl = this.findViewById(R.id.Manual_Object_Current_Char_Lbl);
        currentTotalLbl = this.findViewById(R.id.Manual_Object_Current_Total_Lbl);
        launchDate = this.findViewById(R.id.Manual_Object_Launch_Date);
        final SelectListInterface ownerList = this.findViewById(R.id.Manual_Object_Owner_Text_List);
        final SelectListInterface groupList = this.findViewById(R.id.Manual_Object_Group_Text_List);
        MaterialButton cancelButton = this.findViewById(R.id.Manual_Object_Cancel_Button);
        MaterialButton addButton = this.findViewById(R.id.Manual_Object_Add_Button);

        //set icon
        if(nameTextLayout != null)
        {
            nameTextLayout.setStartIconDrawable(Globals.getDrawableText(this, " abc ", 16, Globals.resolveColorID(this, android.R.attr.textColor)));
        }

        //set owners list
        for(index = 0; index < owners.size(); index++)
        {
            UpdateService.MasterOwner currentOwner = owners.get(index);
            ownerItems.add(new IconSpinner.Item(Globals.getOwnerIconIDs(currentOwner.code), currentOwner.name, currentOwner.code));
        }
        Collections.sort(ownerItems, new IconSpinner.Item.Comparer());
        ownerItems.add(0, new IconSpinner.Item("", ""));
        ownerList.setAdapter(new IconSpinner.CustomAdapter(this, ownerItems.toArray(new IconSpinner.Item[0])));
        ownerList.setSelectedValue("");

        //set group list
        for(index = 0; index < categories.size(); index++)
        {
            UpdateService.MasterCategory currentCategory = categories.get(index);
            categoryItems.add(new IconSpinner.Item(currentCategory.name, currentCategory.index));
        }
        Collections.sort(categoryItems, new IconSpinner.Item.Comparer());
        categoryItems.add(0, new IconSpinner.Item("", ""));
        groupList.setAdapter(new IconSpinner.CustomAdapter(this, categoryItems.toArray(new IconSpinner.Item[0])));
        groupList.setSelectedValue("");

        //set events
        nameText.setOnFocusChangeListener(createOnFocusChangeListener(0));
        nameText.setOnSelectionChangedListener(createOnSelectionChangedListener(0));
        nameText.addTextChangedListener(createOnTextChangedListener(0));
        line1Text.setOnFocusChangeListener(createOnFocusChangeListener(1));
        line1Text.setOnSelectionChangedListener(createOnSelectionChangedListener(1));
        line1Text.addTextChangedListener(createOnTextChangedListener(1));
        line2Text.setOnFocusChangeListener(createOnFocusChangeListener(2));
        line2Text.setOnSelectionChangedListener(createOnSelectionChangedListener(2));
        line2Text.addTextChangedListener(createOnTextChangedListener(2));
        launchDate.setOnDateSetListener(createOnDateSetListener());

        //setup buttons
        addButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Editable nameString = nameText.getText();
                Editable line1String = line1Text.getText();
                Editable line2String = line2Text.getText();
                String name = (nameString != null ? nameString.toString().trim() : "");
                String line1 = (line1String != null ? line1String.toString() : "");
                String line2 = (line2String != null ? line2String.toString() : "");
                String firstError = "";
                String currentError;
                Calculations.SatelliteObjectType currentSat;
                Resources res = ManualOrbitalInputActivity.this.getResources();
                String invalidString = res.getString(R.string.title_invalid);

                //check for invalid values
                if(name.isEmpty())
                {
                    currentError = invalidString + " " + res.getString(R.string.title_name);
                    firstError = updateError(firstError, currentError);
                    nameText.setError(currentError);
                }
                if(line1.length() < 69)
                {
                    currentError = invalidString + " " + res.getString(R.string.title_length);
                    firstError = updateError(firstError, currentError);
                    line1Text.setError(currentError);
                }
                if(line2.length() < 69)
                {
                    currentError = invalidString + " " + res.getString(R.string.title_length);
                    firstError = updateError(firstError, currentError);
                    line2Text.setError(currentError);
                }

                //check for valid list selections
                if(ownerList.getSelectedValue("").equals(""))
                {
                    currentError = invalidString + " " + res.getString(R.string.title_owner);
                    firstError = updateError(firstError, currentError);
                }
                if(groupList.getSelectedValue("").equals(""))
                {
                    currentError = invalidString + " " + res.getString(R.string.title_group);
                    firstError = updateError(firstError, currentError);
                }

                //if there is no error
                if(firstError.isEmpty())
                {
                    currentSat = Calculations.loadSatellite(name, line1, line2);
                    if(currentSat.getSatelliteNum() != Universe.IDs.None && currentSat.tle.launchYear != Integer.MAX_VALUE)
                    {
                        Database.saveSatellite(ManualOrbitalInputActivity.this, name, currentSat.getSatelliteNum(), (String)ownerList.getSelectedValue(""), launchDate.getDate().getTimeInMillis(), line1, line2, Calculations.epochToGMTCalendar(currentSat.tle.epochYear, currentSat.tle.epochDay).getTimeInMillis(), null, Globals.getGMTTime().getTimeInMillis(), Database.OrbitalType.Satellite);

                        setResult(RESULT_OK, resultIntent);
                        ManualOrbitalInputActivity.this.finish();
                    }
                    else
                    {
                        Globals.showSnackBar(manualLayout, res.getString(R.string.text_read_inputs_error), null, true, true);
                    }
                }
                else
                {
                    Globals.showSnackBar(manualLayout, firstError, null, true, true);
                }
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setResult(RESULT_CANCELED, resultIntent);
                ManualOrbitalInputActivity.this.finish();
            }
        });
    }

    //Creates an on focus changed listener
    private View.OnFocusChangeListener createOnFocusChangeListener(final int lineNumber)
    {
        return(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if(hasFocus)
                {
                    updateDescription(lineNumber);
                }
            }
        });
    }

    //Creates an on selection changed listener
    private EditTextSelect.OnSelectionChangedListener createOnSelectionChangedListener(final int lineNumber)
    {
        return(new EditTextSelect.OnSelectionChangedListener()
        {
            @Override
            public void onSelectionChanged(int selStart, int selEnd)
            {
                updateDescription(lineNumber);
            }
        });
    }

    //Creates an on text changed listener
    private TextWatcher createOnTextChangedListener(final int lineNumber)
    {
        return(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s)
            {
                updateDescription(lineNumber);
            }
        });
    }

    //Creates an on date set listener
    private DateInputView.OnDateSetListener createOnDateSetListener()
    {
        return(new DateInputView.OnDateSetListener()
        {
            @Override
            public void onDateSet(DateInputView dateView, Calendar date)
            {
                updateLaunchDateYear();
            }
        });
    }

    //Updates launch date display
    private void updateLaunchDateYear()
    {
        int year;
        Calendar date;

        //if line 1 is set
        if(line1Text != null && line1Text.getText() != null && line1Text.getText().length() >= Calculations.TLE1Index.LaunchYear + 2)
        {
            //get current date
            date = launchDate.getDate();

            //if launch date is valid and changed
            year = Calculations.tryParseTLEYear(line1Text.getText().toString(), Calculations.TLE1Index.LaunchYear);
            if(year != Integer.MAX_VALUE && date.get(Calendar.YEAR) != year)
            {
                //update year
                date.set(Calendar.YEAR, year);
                launchDate.setDate(date);
            }
        }
    }

    //Updates current description and character placement
    private void updateDescription(int lineNumber)
    {
        int index;
        int position;
        int charPos = 0;
        int totalChars = 0;
        Resources res = this.getResources();
        String description = "(" + res.getString(R.string.title_space) + ")";
        String numberString = res.getString(R.string.title_number);
        String satelliteString = res.getQuantityString(R.plurals.title_satellites, 1);

        switch(lineNumber)
        {
            case 0:
                position = nameText.getSelectionStart();
                if(position < 20)
                {
                    description = satelliteString + " " + res.getString(R.string.title_name);
                    totalChars = Database.MAX_ORBITAL_NAME_LENGTH;
                    charPos = position + 1;
                }
                else
                {
                    description = "";
                }
                break;

            case 1:
                position = line1Text.getSelectionStart();
                switch(position)
                {
                    case 69:
                        description = "";
                        break;

                    case 63:
                    case 61:
                    case 52:
                    case 43:
                    case 32:
                    case 17:
                    case 8:
                    case 1:
                        //space
                        break;

                    default:
                        if(position >= 68)
                        {
                            index = 68;
                            description = res.getString(R.string.title_checksum);
                            totalChars = 1;
                        }
                        else if(position >= Calculations.TLE1Index.Enum)
                        {
                            index = Calculations.TLE1Index.Enum;
                            description = res.getString(R.string.title_element_set) + " " + numberString;
                            totalChars = 4;
                        }
                        else if(position == Calculations.TLE1Index.Ephem)
                        {
                            index = Calculations.TLE1Index.Ephem;
                            description = res.getString(R.string.text_0);
                            totalChars = 1;
                        }
                        else if(position >= Calculations.TLE1Index.DragDiv)
                        {
                            index = Calculations.TLE1Index.DragDiv;
                            description = res.getString(R.string.title_drag_term_divisor);
                            totalChars = 2;
                        }
                        else if(position >= Calculations.TLE1Index.Drag)
                        {
                            index = Calculations.TLE1Index.Drag;
                            description = res.getString(R.string.title_drag_term);
                            totalChars = 6;
                        }
                        else if(position >= Calculations.TLE1Index.MeanMotion2)
                        {
                            index = Calculations.TLE1Index.MeanMotion2;
                            description = res.getString(R.string.title_second_deriv_mean_motion_multi_6);
                            totalChars = 8;
                        }
                        else if(position >= Calculations.TLE1Index.MeanMotion1)
                        {
                            index = Calculations.TLE1Index.MeanMotion1;
                            description = res.getString(R.string.title_first_deriv_mean_motion_div_2);
                            totalChars = 10;
                        }
                        else if(position >= Calculations.TLE1Index.EpochDay)
                        {
                            index = Calculations.TLE1Index.EpochDay;
                            description = res.getString(R.string.title_epoch_day);
                            totalChars = 12;
                        }
                        else if(position >= Calculations.TLE1Index.EpochYear)
                        {
                            index = Calculations.TLE1Index.EpochYear;
                            description = res.getString(R.string.title_epoch_year);
                            totalChars = 2;
                        }
                        else if(position >= Calculations.TLE1Index.LaunchPiece)
                        {
                            index = Calculations.TLE1Index.LaunchPiece;
                            description = res.getString(R.string.title_piece_of_launch);
                            totalChars = 3;
                        }
                        else if(position >= Calculations.TLE1Index.LaunchNum)
                        {
                            index = Calculations.TLE1Index.LaunchNum;
                            description = res.getString(R.string.title_launch_number_of_year);
                            totalChars = 3;
                        }
                        else if(position >= Calculations.TLE1Index.LaunchYear)
                        {
                            index = Calculations.TLE1Index.LaunchYear;
                            description = res.getString(R.string.title_last_2_digits_of_year);
                            totalChars = 2;
                        }
                        else if(position == Calculations.TLE1Index.Class)
                        {
                            index = Calculations.TLE1Index.Class;
                            description = res.getString(R.string.title_classification);
                            totalChars = 1;
                        }
                        else if(position >= Calculations.TLE1Index.SatNum)
                        {
                            index = Calculations.TLE1Index.SatNum;
                            description = satelliteString + " " + numberString;
                            totalChars = 5;
                        }
                        else
                        {
                            index = 0;
                            description = res.getString(R.string.title_line_number) + " (" + res.getString(R.string.text_1) + ")";
                            totalChars = 1;
                        }

                        charPos = (position - index) + 1;
                        break;
                }

                //if year has been input
                if(position >= Calculations.TLE1Index.LaunchYear + 2)
                {
                    //update launch date year
                    updateLaunchDateYear();
                }
                break;

            case 2:
                position = line2Text.getSelectionStart();
                switch(position)
                {
                    case 69:
                        description = "";
                        break;

                    case 51:
                    case 42:
                    case 33:
                    case 25:
                    case 16:
                    case 7:
                    case 1:
                        //space
                        break;

                    default:
                        if(position >= 68)
                        {
                            index = 68;
                            description = res.getString(R.string.title_checksum);
                            totalChars = 1;
                        }
                        else if(position >= Calculations.TLE2Index.RevEp)
                        {
                            index = Calculations.TLE2Index.RevEp;
                            description = res.getString(R.string.title_rev_number_at_epoch);
                            totalChars = 5;
                        }
                        else if(position >= Calculations.TLE2Index.Revs)
                        {
                            index = Calculations.TLE2Index.Revs;
                            description = res.getString(R.string.title_mean_motion);
                            totalChars = 11;
                        }
                        else if(position >= Calculations.TLE2Index.MeanAnom)
                        {
                            index = Calculations.TLE2Index.MeanAnom;
                            description = res.getString(R.string.title_mean_anomaly);
                            totalChars = 8;
                        }
                        else if(position >= Calculations.TLE2Index.ArgPeri)
                        {
                            index = Calculations.TLE2Index.ArgPeri;
                            description = res.getString(R.string.title_arg_of_perigee);
                            totalChars = 8;
                        }
                        else if(position >= Calculations.TLE2Index.Eccen)
                        {
                            index = Calculations.TLE2Index.Eccen;
                            description = res.getString(R.string.title_eccentricity);
                            totalChars = 7;
                        }
                        else if(position >= Calculations.TLE2Index.RightAscn)
                        {
                            index = Calculations.TLE2Index.RightAscn;
                            description = res.getString(R.string.title_right_asc_ascd_node);
                            totalChars = 8;
                        }
                        else if(position >= Calculations.TLE2Index.Inclin)
                        {
                            index = Calculations.TLE2Index.Inclin;
                            description = res.getString(R.string.title_inclination);
                            totalChars = 8;
                        }
                        else if(position >= 2)
                        {
                            index = 2;
                            description = satelliteString + " " + numberString;
                            totalChars = 5;
                        }
                        else
                        {
                            index = 0;
                            description = res.getString(R.string.title_line_number) + " (" + res.getString(R.string.text_2) + ")";
                            totalChars = 1;
                        }

                        charPos = (position - index) + 1;
                        break;
                }
                break;
        }

        currentLbl.setText(description);
        currentCharLbl.setText(charPos > 0 ? String.valueOf(charPos) : "-");
        currentTotalLbl.setText(totalChars > 0 ? String.valueOf(totalChars) : "-");
    }

    //Updates error
    private String updateError(String currentError, String newError)
    {
        return(currentError.isEmpty() ? newError : currentError);
    }
}
