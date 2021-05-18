package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;
import java.util.Calendar;


public class EditValuesDialog
{
    private static abstract class EditType
    {
        static final byte Orbital = 0;
        static final byte Location = 1;
        static final byte Folder = 2;
        static final byte Login = 3;
        static final byte SortBy = 4;
    }

    public interface OnSaveListener
    {
        void onSave(EditValuesDialog dialog, int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue);
    }

    public interface OnDismissListener
    {
        void onDismiss(EditValuesDialog dialog, int saveCount);
    }

    public interface OnCancelListener
    {
        void onCancel(EditValuesDialog dialog);
    }

    private int itemCount;
    private int savedCount;
    private int currentIndex;
    private boolean isLogin;
    private boolean isSortBy;
    private String title;
    private String itemTextValueTitle;
    private String itemTextValue2Title;
    private String itemList2Title;
    private String itemDateTitle;
    private final Activity currentContext;
    private TextView editValueTitle;
    private TextView editValue2Title;
    private TextView editText;
    private TextView editText2;
    private TextView editNumberTitle;
    private TextView editNumber2Title;
    private TextView editNumber3Title;
    private TextView editDateTitle;
    private EditText editValueText;
    private EditText editValue2Text;
    private EditText editNumberText;
    private EditText editNumber2Text;
    private EditText editNumber3Text;
    private IconSpinner editValueList;
    private TextView editValueList2Title;
    private IconSpinner editValueList2;
    private DateInputView editDate;
    private Button positiveButton;
    private AlertDialog editDialog;
    private final OnSaveListener saveListener;
    private OnDismissListener dismissListener;
    private final OnCancelListener cancelListener;
    private int[] itemIDs;
    private double[] itemNumberValues;
    private double[] itemNumber2Values;
    private double[] itemNumber3Values;
    private String[] itemNumberTitles;
    private String[] itemDefaultListValues;
    private String[] itemDefaultList2Values;
    private String[] itemTextValues;
    private String[] itemText2Values;
    private String[] itemTextRowValues;
    private String[] itemTextRow2Values;
    private String[] itemListValues;
    private String[] itemList2Values;
    private long[] itemDateValues;

    public EditValuesDialog(Activity context, OnSaveListener sListener, OnDismissListener dListener, OnCancelListener cListener)
    {
        currentIndex = savedCount = 0;
        currentContext = context;
        title = null;
        itemTextValueTitle = null;
        itemTextValue2Title = null;
        itemList2Title = null;
        saveListener = sListener;
        dismissListener = dListener;
        cancelListener = cListener;
    }
    public EditValuesDialog(Activity context, OnSaveListener sListener, OnDismissListener dListener)
    {
        this(context, sListener, dListener, null);
    }
    public EditValuesDialog(Activity context, OnSaveListener sListener)
    {
        this(context, sListener, null, null);
    }

    public void setOnDismissListener(OnDismissListener listener)
    {
        dismissListener = listener;
    }

    //Creates an on click listener
    private View.OnClickListener createOnClickListener(final int which)
    {
        return new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String textValue;
                String text2Value;
                String list2Value = (isLogin ? (String)editValueList2.getSelectedValue("") : "");
                boolean ignoreClick = false;
                boolean isSpaceTrack = (isLogin && (list2Value == null || list2Value.equals(Settings.Options.Sources.SpaceTrack)));
                boolean usingNumberValues = (itemNumberValues != null);
                double number = (usingNumberValues ? Globals.tryParseDouble(editNumberText.getText().toString()) : Double.MAX_VALUE);
                double number2 = (usingNumberValues ? Globals.tryParseDouble(editNumber2Text.getText().toString()) : Double.MAX_VALUE);
                double number3 = (usingNumberValues ? Globals.tryParseDouble(editNumber3Text.getText().toString()) : Double.MAX_VALUE);

                //handle based on button
                switch(which)
                {
                    case DialogInterface.BUTTON_POSITIVE:
                        //get values
                        textValue = editValueText.getText().toString().trim();
                        text2Value = editValue2Text.getText().toString().trim();

                        //if -sort by- or -text exists and -not login or not space-track or text 2 exists--
                        if(isSortBy || ((!textValue.equals("") || (isLogin && !isSpaceTrack)) && (!isLogin || !isSpaceTrack || !text2Value.equals(""))))
                        {
                            //save changes
                            savedCount++;
                            if(saveListener != null)
                            {
                                saveListener.onSave(EditValuesDialog.this, currentIndex, (itemIDs != null && currentIndex < itemIDs.length ? itemIDs[currentIndex] : -1), textValue, text2Value, number, number2, number3, (itemListValues != null ? editValueList.getSelectedValue("").toString() : null), (itemList2Values != null ? editValueList2.getSelectedValue("").toString() : null), (itemDateValues != null ? editDate.getDate().getTimeInMillis() : -1));
                            }
                        }
                        else
                        {
                            //ignore
                            ignoreClick = true;
                        }
                        break;

                    case DialogInterface.BUTTON_NEUTRAL:
                        //do nothing
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //close
                        if(cancelListener != null)
                        {
                            editDialog.cancel();
                        }
                        else
                        {
                            editDialog.dismiss();
                        }
                        break;
                }

                //if not ignoring
                if(!ignoreClick)
                {
                    //update index
                    currentIndex++;
                    if(currentIndex >= itemCount)
                    {
                        //close
                        editDialog.dismiss();
                    }
                    else
                    {
                        //update title and text
                        updateDisplays();
                    }
                }
            }
        };
    }

    //Updates displays
    private void updateDisplays()
    {
        String titleText = title;
        String itemTitle;
        String numberText;
        Calendar date;
        Resources res = currentContext.getResources();

        //if more than 1 item
        if(itemCount > 1)
        {
            //add current position
            titleText +=  (" " + (currentIndex + 1) + res.getString(R.string.text_space_of_space) + itemCount);
        }

        //set titles
        editDialog.setTitle(titleText);
        itemTitle = (itemTextValueTitle != null ? itemTextValueTitle : "");
        editValueTitle.setText(itemTitle);
        itemTitle = (itemTextValue2Title != null ? itemTextValue2Title : "");
        editValue2Title.setText(itemTitle);

        //set texts
        if(itemTextValues != null && currentIndex < itemTextValues.length)
        {
            editValueText.setText(itemTextValues[currentIndex]);
            editValueText.selectAll();
        }
        if(itemText2Values != null && currentIndex < itemText2Values.length)
        {
            editValue2Text.setText(itemText2Values[currentIndex]);
        }
        if(itemTextRowValues != null && currentIndex < itemTextRowValues.length)
        {
            editText.setText(itemTextRowValues[currentIndex]);
        }
        if(itemTextRow2Values != null && currentIndex < itemTextRow2Values.length)
        {
            editText2.setText(itemTextRow2Values[currentIndex]);
        }

        //if number titles are set
        if(itemNumberTitles != null)
        {
            //set number texts
            if(itemNumberValues != null && currentIndex < itemNumberValues.length)
            {
                numberText = Globals.getNumberString(itemNumberValues[currentIndex], 4);
                itemTitle = (itemNumberTitles.length > 0 && itemNumberTitles[0] != null ? itemNumberTitles[0] : "");
                editNumberTitle.setText(itemTitle);
                editNumberText.setText(numberText);
            }
            if(itemNumber2Values != null && currentIndex < itemNumber2Values.length)
            {
                numberText = Globals.getNumberString(itemNumber2Values[currentIndex], 4);
                itemTitle = (itemNumberTitles.length > 1 && itemNumberTitles[1] != null ? itemNumberTitles[1] : "");
                editNumber2Title.setText(itemTitle);
                editNumber2Text.setText(numberText);
            }
            if(itemNumber3Values != null && currentIndex < itemNumber3Values.length)
            {
                numberText = Globals.getNumberString(itemNumber3Values[currentIndex], 2);
                itemTitle = (itemNumberTitles.length > 2 && itemNumberTitles[2] != null ? itemNumberTitles[2] : "");
                editNumber3Title.setText(itemTitle);
                editNumber3Text.setText(numberText);
            }
        }

        //if using list
        if(itemListValues != null && itemDefaultListValues != null && currentIndex < itemDefaultListValues.length)
        {
            //set list
            editValueList.setSelectedValue(itemDefaultListValues[currentIndex]);
        }

        //if using list 2
        if(itemList2Values != null && itemDefaultList2Values != null && currentIndex < itemDefaultList2Values.length)
        {
            //set title and list
            itemTitle = (itemList2Title != null ? itemList2Title : "");
            editValueList2Title.setText(itemTitle);
            editValueList2.setSelectedValue(itemDefaultList2Values[currentIndex]);
        }

        //if using date
        if(itemDateValues != null && currentIndex < itemDateValues.length)
        {
            //set date
            date = Globals.getGMTTime();
            date.setTimeInMillis(itemDateValues[currentIndex]);
            itemTitle = (itemDateTitle != null ? itemDateTitle : "");
            editDateTitle.setText(itemTitle);
            editDate.setTimeZone(Globals.gmtTimeZone);
            editDate.setDate(date);
        }
    }

    //Show the dialog
    private void show(byte editType, String titleText, int[] ids, @Nullable String textValueTitle, String[] textValues, @Nullable String textValue2Title, @Nullable String[] text2Values, @Nullable String[] textRowValues, @Nullable String[] textRow2Values, @Nullable String[] numberTitles, @Nullable double[] numberValues, @Nullable double[] number2Values, @Nullable double[] number3Values, String[] listValues, String[] defaultListValue, String list2Title, int[] list2IconIds, String[] list2Values, String[] list2SubValues, String[] defaultList2Value, String dateTitleText, long[] dateValues)
    {
        boolean isEditFolder = (editType == EditType.Folder);
        boolean usingText = (textValues != null);
        boolean usingText2 = (text2Values != null);
        boolean usingList = (listValues != null && defaultListValue != null);
        boolean usingList2 = (list2Values != null && defaultList2Value != null);
        boolean usingList2Icons = (usingList2 && list2IconIds != null);
        boolean usingDate = (dateValues != null);
        boolean darkTheme = Settings.getDarkTheme(currentContext);
        int index;
        int currentId;
        int showText2 = (usingText2 ? View.VISIBLE : View.GONE);
        int showRowText = (textRowValues != null ? View.VISIBLE : View.GONE);
        int showRowText2 = (textRow2Values != null ? View.VISIBLE : View.GONE);
        int showNumber = (numberValues != null ? View.VISIBLE : View.GONE);
        int showNumber2 = (number2Values != null ? View.VISIBLE : View.GONE);
        int showNumber3 = (number3Values != null ? View.VISIBLE : View.GONE);
        final TableRow valueRow;
        final TableRow value2Row;
        final TextView editValueList2Title2;
        AlertDialog.Builder editDialogBuilder = new AlertDialog.Builder(currentContext, Globals.getDialogThemeID(currentContext));
        LayoutInflater viewInflater = (LayoutInflater)currentContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View editDialogView = (viewInflater != null ? viewInflater.inflate(R.layout.edit_dialog, currentContext.findViewById(android.R.id.content), false) : null);

        //remember if a specific edit type
        isLogin = (editType == EditType.Login);
        isSortBy = (editType == EditType.SortBy);

        //get values
        title = titleText;
        itemIDs = ids;
        itemTextValueTitle = textValueTitle;
        itemTextValues = textValues;
        itemTextValue2Title = textValue2Title;
        itemText2Values = text2Values;
        itemTextRowValues = textRowValues;
        itemTextRow2Values = textRow2Values;
        itemCount = (usingText ? itemTextValues.length : 1);
        itemNumberTitles = numberTitles;
        itemNumberValues = numberValues;
        itemNumber2Values = number2Values;
        itemNumber3Values = number3Values;
        itemListValues = listValues;
        itemDefaultListValues = defaultListValue;
        itemList2Title = list2Title;
        itemList2Values = list2Values;
        itemDefaultList2Values = defaultList2Value;
        itemDateTitle = dateTitleText;
        itemDateValues = dateValues;

        //if dialog view is set
        if(editDialogView != null)
        {
            //get and set displays
            editValueTitle = editDialogView.findViewById(R.id.Edit_Value_Title);
            editValueText = editDialogView.findViewById(R.id.Edit_Value_Text);
            editValue2Title = editDialogView.findViewById(R.id.Edit_Value2_Title);
            editValue2Text = editDialogView.findViewById(R.id.Edit_Value2_Text);
            valueRow = editDialogView.findViewById(R.id.Edit_Value_Row);
            if(!usingText && !usingList)
            {
                valueRow.setVisibility(View.GONE);
            }
            value2Row = editDialogView.findViewById(R.id.Edit_Value2_Row);
            value2Row.setVisibility(showText2);
            editText = editDialogView.findViewById(R.id.Edit_Text);
            editText.setVisibility(showRowText);
            editText2 = editDialogView.findViewById(R.id.Edit_Text2);
            editText2.setVisibility(showRowText2);
            editValueList =  editDialogView.findViewById(R.id.Edit_Value_List);
            if(usingList)
            {
                //set text align right and add list values
                editValueText.setGravity(Gravity.END);
                editValueList.setAdapter(new IconSpinner.CustomAdapter(currentContext, itemListValues));
            }
            else
            {
                editValueList.setVisibility(View.GONE);
            }

            editNumberTitle = editDialogView.findViewById(R.id.Edit_Number_Title);
            editNumberText = editDialogView.findViewById(R.id.Edit_Number_Text);
            editDialogView.findViewById(R.id.Edit_Number_Row).setVisibility(showNumber);

            editNumber2Title = editDialogView.findViewById(R.id.Edit_Number2_Title);
            editNumber2Text = editDialogView.findViewById(R.id.Edit_Number2_Text);
            editDialogView.findViewById(R.id.Edit_Number2_Row).setVisibility(showNumber2);

            editNumber3Title = editDialogView.findViewById(R.id.Edit_Number3_Title);
            editNumber3Text = editDialogView.findViewById(R.id.Edit_Number3_Text);
            editDialogView.findViewById(R.id.Edit_Number3_Row).setVisibility(showNumber3);

            editDate = editDialogView.findViewById(R.id.Edit_Date);
            editDateTitle = editDialogView.findViewById(R.id.Edit_Date_Title);
            editDialogView.findViewById(R.id.Edit_Date_Row).setVisibility(usingDate ? View.VISIBLE : View.GONE);

            //get list 2
            editValueList2Title = editDialogView.findViewById(R.id.Edit_List2_Title);
            editValueList2Title.setVisibility(!isEditFolder && usingList2 ? View.VISIBLE : View.GONE);
            editValueList2Title2 = editDialogView.findViewById(R.id.Edit_List2_Title2);
            editValueList2Title2.setVisibility(isEditFolder && usingList2 ? View.VISIBLE : View.GONE);
            editValueList2 = editDialogView.findViewById(R.id.Edit_Value_List2);
            editDialogView.findViewById(R.id.Edit_List2_Row).setVisibility((isLogin && usingList2) || !usingText2 ? View.VISIBLE : View.GONE);
            editDialogView.findViewById(R.id.Edit_List2_Layout).setVisibility(usingList2 ? View.VISIBLE : View.GONE);
            if(usingList2)
            {
                //if editing orbitals, have owner codes, and there is a code for every owner
                if(editType == EditType.Orbital && list2SubValues != null && list2SubValues.length == list2Values.length)
                {
                    //add owners with icons
                    IconSpinner.Item[] owners = new IconSpinner.Item[itemList2Values.length];
                    for(index = 0; index < owners.length; index++)
                    {
                        owners[index] = new IconSpinner.Item(Globals.getDrawable(currentContext, Settings.getOwnerIcons(currentContext, Integer.MAX_VALUE, list2SubValues[index])), itemList2Values[index], itemList2Values[index]);
                    }
                    editValueList2.setAdapter(new IconSpinner.CustomAdapter(currentContext, owners));
                }
                else if(usingList2Icons)
                {
                    //add items with icons
                    IconSpinner.Item[] items = new IconSpinner.Item[itemList2Values.length];
                    for(index = 0; index < items.length; index++)
                    {
                        int currentStringId;
                        float rotate = 0;
                        String currentText = "";
                        String currentValue = itemList2Values[index];
                        Drawable currentIcon;

                        currentId = list2IconIds[index];
                        currentStringId = (list2SubValues != null && index < list2SubValues.length ? Integer.parseInt(list2SubValues[index]) : -1);
                        if(currentId == R.drawable.ic_launcher && currentStringId != -1)
                        {
                            switch(currentStringId)
                            {
                                case R.string.title_name:
                                    currentText = " abc ";
                                    break;

                                case R.string.title_pass_start:
                                    currentText = Globals.Symbols.Up;
                                    break;

                                case R.string.title_pass_elevation:
                                case R.string.title_altitude:
                                    currentText = Globals.Symbols.Elevating;
                                    break;
                            }
                            currentIcon = Globals.getDrawable(currentContext, currentText, 16, (darkTheme ? Color.WHITE : Color.BLACK), Color.TRANSPARENT);
                        }
                        else
                        {
                            switch(currentStringId)
                            {
                                case R.string.title_elevation:
                                case R.string.title_latitude:
                                    rotate = 90;
                                    break;
                            }
                            currentIcon = Globals.getDrawable(currentContext, currentId, (!isLogin && currentId != R.drawable.org_gdrive && currentId != R.drawable.org_dbox));
                        }

                        items[index] = (currentId != -1 ? new IconSpinner.Item(currentIcon, currentValue, currentValue, rotate) : null);
                    }
                    editValueList2.setAdapter(new IconSpinner.CustomAdapter(currentContext, items));
                    if(isLogin)
                    {
                        editValueList2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                        {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                            {
                                boolean isSpaceTrack = itemList2Values[position].equals(Settings.Options.Sources.SpaceTrack);
                                int visible = (isSpaceTrack ? View.VISIBLE : View.GONE);

                                valueRow.setVisibility(visible);
                                value2Row.setVisibility(visible);
                                editText.setVisibility(visible);

                                positiveButton.setText(isSpaceTrack ? R.string.title_login : R.string.title_update);
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    }
                }
                else
                {
                    //add list values
                    editValueList2.setAdapter(new IconSpinner.CustomAdapter(currentContext, itemList2Values));
                    if(editType == EditType.Location)
                    {
                        editValueList2.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
                        {
                            @Override
                            public void onGlobalLayout()
                            {
                                int width = editDialogView.getWidth();

                                //if width is known
                                if(width > 0)
                                {
                                    //remove listener
                                    editValueList2.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                                    //update width
                                    editValueList2.setDropDownWidth(width);
                                }
                            }
                        });
                    }
                }
            }
        }

        //setup and show dialog
        editDialogBuilder.setView(editDialogView);
        editDialogBuilder.setPositiveButton((isLogin ? R.string.title_login : R.string.title_save), null);
        if(itemCount > 1)
        {
            editDialogBuilder.setNeutralButton(R.string.title_skip, null);
        }
        editDialogBuilder.setNegativeButton(R.string.title_cancel, null);
        editDialogBuilder.setTitle(title);
        editDialog = editDialogBuilder.create();
        editDialog.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                //if listener is set
                if(dismissListener != null)
                {
                    //send event
                    dismissListener.onDismiss(EditValuesDialog.this, savedCount);
                }

                //allow rotation
                Globals.lockScreenOrientation(currentContext, false);
            }
        });
        editDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                //if listener is set
                if(cancelListener != null)
                {
                    //send event
                    cancelListener.onCancel(EditValuesDialog.this);
                }
            }
        });
        editDialog.show();

        //set displays
        updateDisplays();

        //setup buttons
        positiveButton = editDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(createOnClickListener(DialogInterface.BUTTON_POSITIVE));
        if(isSortBy || (isLogin && !usingList2))
        {
            positiveButton.setText(R.string.title_ok);
        }
        if(itemCount > 1)
        {
            editDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(createOnClickListener(DialogInterface.BUTTON_NEUTRAL));
        }
        editDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(createOnClickListener(DialogInterface.BUTTON_NEGATIVE));

        //prevent rotation until done
        Globals.lockScreenOrientation(currentContext, true);
    }
    public void getLocation(String titleText, int[] ids, String textValueTitle, @NonNull String[] textValues, String[] numberTitles, double[] numberValues, double[] number2Values, double[] number3Values, String list2Title, String[] list2Values, String[] defaultList2Value)
    {
        show(EditType.Location, titleText, ids, textValueTitle, textValues, null, null, null, null, numberTitles, numberValues, number2Values, number3Values, null, null, list2Title, null, list2Values, null, defaultList2Value, null, null);
    }
    public void getFileLocation(String titleText, int[] ids, @NonNull String[] textValues, String[] listValues, String[] defaultListValue, String[] defaultList2Value)
    {
        show(EditType.Folder, titleText, ids, null, textValues, null, null, null, null, null, null, null, null, listValues, defaultListValue, null, Globals.fileSourceImageIds, Globals.getFileLocations(currentContext), null, defaultList2Value, null, null);
    }
    public void getOrbital(String titleText, int[] ids, String textValueTitle, @NonNull String[] textValues, String list2Title, String[] list2Values, String[] list2SubValues, String[] defaultList2Value, String dateTitleText, long[] dateValues)
    {
        show(EditType.Orbital, titleText, ids, textValueTitle, textValues, null, null, null, null, null, null, null, null, null, null, list2Title, null, list2Values, list2SubValues, defaultList2Value, dateTitleText, dateValues);
    }
    public void getLogin(String titleText, String textValueTitle, @NonNull String[] textValues, String textValue2Title, String[] text2Values, String[] textRowValues, String[] textRow2Values, String list2Title)
    {
        boolean createOnly = (list2Title == null);
        show(EditType.Login, titleText, null, textValueTitle, textValues, textValue2Title, text2Values, textRowValues, textRow2Values, null, null, null, null, null, null, list2Title, (createOnly ? null : Settings.Options.Updates.SatelliteSourceImageIds), (createOnly ? null : Settings.Options.Updates.SatelliteSourceItems), null, (createOnly ? null : new String[]{Settings.Options.Sources.SpaceTrack}), null, null);
    }
    public void getSortBy(String titleText, int page)
    {
        int index;
        int[] listIds = Current.Items.getSortByIds(page);
        int[] imageIds = new int[listIds.length];
        String[] listValues = new String[listIds.length];
        String[] listSubValues = new String[listIds.length];

        //go through each id
        for(index = 0; index < listIds.length; index++)
        {
            //get image and string values
            imageIds[index] = Current.Items.getSortByImageId(listIds[index]);
            listValues[index] = currentContext.getString(listIds[index]);
            listSubValues[index] = String.valueOf(listIds[index]);
        }

        show(EditType.SortBy, titleText, null, null, null, null, null, null, null, null, null, null, null, null, null, null, imageIds, listValues, listSubValues, new String[]{Settings.getCurrentSortByString(currentContext, page)}, null, null);
    }
}
