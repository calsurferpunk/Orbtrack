package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.google.android.material.textfield.TextInputLayout;


public class SelectableAutoCompleteTextView extends androidx.appcompat.widget.AppCompatAutoCompleteTextView implements SelectListInterface
{
    private boolean allowAutoSelect;
    private int textColor;
    private int textSelectedColor;
    private int backgroundColor;
    private int backgroundItemColor;
    private int backgroundItemSelectedColor;
    private Object lastValue;
    private Object lastDefaultValue;
    private IconSpinner.CustomAdapter currentAdapter;
    private AdapterView.OnItemSelectedListener selectedListener;

    public SelectableAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        int inputType = getInputType();
        int iconPadding = (int)Globals.dpToPixels(context, 12);

        //if attributes are set
        if(attrs != null)
        {
            //get attribute values
            try(@SuppressLint({"NewApi", "LocalSuppress"}) TypedArray valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SelectableAutoCompleteTextView, 0, 0))
            {
                inputType = valueArray.getInt(R.styleable.SelectableAutoCompleteTextView_android_inputType, inputType);
                valueArray.recycle();
            }
            catch(NoSuchMethodError | Exception noMethod)
            {
                //do nothing
            }
        }

        //setup
        setLast(null, null);
        allowAutoSelect = true;
        setInputType(inputType);
        addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                int selectedId = -1;
                int selectedIndex;

                //if adapter is set
                if(currentAdapter != null)
                {
                    //if set a selection
                    selectedIndex = getListIndex(s.toString());
                    if(selectedIndex < 0)
                    {
                        //try to get selected index
                        selectedIndex = currentAdapter.getSelectedIndex();
                    }
                    if(selectedIndex >= 0)
                    {
                        //remember selected item
                        IconSpinner.Item selectedItem = (IconSpinner.Item)currentAdapter.getItem(selectedIndex);
                        boolean haveItem = (selectedItem != null);

                        //if have selected item
                        if(haveItem)
                        {
                            //try to get parent frame
                            ViewParent itemParent = getParent();
                            if(itemParent instanceof FrameLayout)
                            {
                                //try to get parent input layout
                                ViewParent frameParent = itemParent.getParent();
                                if(frameParent instanceof TextInputLayout)
                                {
                                    //get layout and icon view
                                    TextInputLayout inputLayout = (TextInputLayout)frameParent;
                                    View iconView = inputLayout.findViewById(R.id.text_input_start_icon);
                                    ImageView iconButton = (iconView instanceof ImageView ? (ImageView)iconView : null);

                                    //set icon to selected item icon
                                    inputLayout.setStartIconDrawable(selectedItem.getIcon(context, 96, 32));
                                    if(iconButton != null)
                                    {
                                        //update color filter and padding
                                        iconButton.setColorFilter(Color.TRANSPARENT);
                                        iconButton.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
                                    }
                                }
                            }
                        }

                        //set selection and send event
                        currentAdapter.setSelectedIndex(selectedIndex);
                        selectedId = (haveItem && selectedItem.value instanceof Integer ? (int)selectedItem.value : selectedId);
                        if(selectedListener != null)
                        {
                            //call listener
                            selectedListener.onItemSelected(null, SelectableAutoCompleteTextView.this, selectedIndex, selectedId);
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    public SelectableAutoCompleteTextView(Context context, AttributeSet attrs)
    {
        this(context, attrs, R.attr.autoCompleteTextViewStyle);
    }
    public SelectableAutoCompleteTextView(Context context)
    {
        this(context, null);
    }

    private int getListIndex(String stringValue)
    {
        int listIndex = -1;
        int intValue = Globals.tryParseInt(stringValue);

        //if adapter is set
        if(currentAdapter != null)
        {
            //go through each item while list index not found
            int index;
            IconSpinner.Item[] items = currentAdapter.getItems();
            for(index = 0; index < items.length && listIndex == -1; index++)
            {
                //if current item is set
                IconSpinner.Item currentItem = items[index];
                if(currentItem != null)
                {
                    //remember current item value
                    Object currentItemValue = currentItem.value;

                    //if text matches current item text or value
                    if(stringValue.equals(currentItem.text) || stringValue.equals(currentItemValue) || (currentItemValue instanceof Integer && intValue == (int)currentItemValue))
                    {
                        //found list index
                        listIndex = index;
                    }
                }
            }
        }

        return(listIndex);
    }

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener)
    {
        selectedListener = listener;
    }

    public AdapterView.OnItemSelectedListener getOnItemSelectedListener()
    {
        return(selectedListener);
    }

    public void loadAdapter()
    {
        Context context = getContext();
        Object firstItem = null;
        Object savedLastValue = lastValue;
        Object savedLastDefaultValue = lastDefaultValue;

        //clear any old text
        setText(null);

        //if adapter is set
        if(currentAdapter != null)
        {
            //get first item, adapter colors, and icons
            firstItem = currentAdapter.getItem(0);
            backgroundColor = currentAdapter.getBackgroundColor();
            backgroundItemColor = currentAdapter.getBackgroundItemColor();
            backgroundItemSelectedColor = currentAdapter.getBackgroundItemSelectedColor();
            textColor = currentAdapter.getTextColor();
            textSelectedColor = currentAdapter.getTextSelectedColor();
            SelectListInterface.loadAdapterIcons(context, currentAdapter);
        }

        //set colors and any selection
        setBackgroundColor(backgroundColor);
        setBackgroundItemColor(backgroundItemColor);
        setBackgroundItemSelectedColor(backgroundItemSelectedColor);
        setTextColor(textColor);
        setTextSelectedColor(textSelectedColor);
        if(firstItem != null)
        {
            //if allowing auto selection
            if(allowAutoSelect)
            {
                //set to first item
                setSelectedText(firstItem.toString());
            }

            //if first item is using icon 3 only
            if(firstItem instanceof IconSpinner.Item && currentAdapter.getUsingIcon3Only())
            {
                //get icon and size
                Drawable firstIcon = ((IconSpinner.Item)firstItem).getIcon(context);
                int[] firstIconSize = Globals.getImageWidthHeight(firstIcon);
                if(firstIcon != null && firstIconSize[0] > 0)
                {
                    //set dropdown size to icon with padding
                    setDropDownWidth(firstIconSize[0] + getTotalPaddingLeft() + getTotalPaddingRight());
                }
            }
        }

        //try to set value again
        if(savedLastDefaultValue != null)
        {
            setSelectedValue(savedLastValue, savedLastDefaultValue);
        }
        else if(savedLastValue instanceof String)
        {
            setSelectedText((String)savedLastValue);
        }
        else if(savedLastValue != null)
        {
            setSelectedValue(savedLastValue);
        }
    }

    public void setAdapter(IconSpinner.CustomAdapter adapter)
    {
        //set and load adapter
        currentAdapter = adapter;
        loadAdapter();
        super.setAdapter(adapter);
    }

    public void setAllowAutoSelect(boolean allow)
    {
        allowAutoSelect = allow;
    }

    public IconSpinner.CustomAdapter getAdapter()
    {
        return(currentAdapter);
    }

    public int getBackgroundColor()
    {
        return(SelectListInterface.getBackgroundColor(currentAdapter));
    }

    public void setBackgroundColor(int color)
    {
        backgroundColor = color;
        SelectListInterface.setBackgroundColor(currentAdapter, backgroundColor);
        setDropDownBackgroundDrawable(new ColorDrawable(backgroundColor));
    }

    public void setBackgroundItemColor(int color)
    {
        backgroundItemColor = color;
        SelectListInterface.setBackgroundItemColor(currentAdapter, backgroundItemColor);
        setBackgroundItemSelectedColor(backgroundItemColor);
    }

    public void setBackgroundItemSelectedColor(int color)
    {
        backgroundItemSelectedColor = color;
        SelectListInterface.setBackgroundItemSelectedColor(currentAdapter, backgroundItemSelectedColor);
    }

    public void setTextColor(int color, int superColor)
    {
        textColor = color;
        super.setTextColor(superColor != color ? superColor : currentAdapter != null && !currentAdapter.getUsingText() ? Color.TRANSPARENT : textColor);
        SelectListInterface.setTextColor(currentAdapter, textColor);
        setTextSelectedColor(textColor);
    }
    public void setTextColor(int color)
    {
        setTextColor(color, color);
    }

    public void setTextSelectedColor(int color)
    {
        textSelectedColor = color;
        SelectListInterface.setTextSelectedColor(currentAdapter, textSelectedColor);
    }

    private void setLast(Object value, Object defaultValue)
    {
        lastValue = value;
        lastDefaultValue = defaultValue;
    }

    private void setSelectedIndex(int index)
    {
        if(currentAdapter != null && index >= 0)
        {
            IconSpinner.Item selectedItem = (IconSpinner.Item)currentAdapter.getItem(index);
            Editable currentEditable = getText();
            String currentText = (currentEditable != null ? currentEditable.toString() : null);
            String selectedText = (selectedItem != null ? selectedItem.text : null);

            //if -current is not null and not equal to selected- or -current is null and selected is not-
            if((currentText != null && !currentText.equals(selectedText)) || (currentText == null && selectedText != null))
            {
                setText(selectedText, false);
            }
        }
    }

    public void setSelectedText(String value)
    {
        setSelectedIndex(SelectListInterface.setSelectedText(currentAdapter, value));
        setLast(value, null);
    }

    public void setSelectedValue(Object value, Object defaultValue)
    {
        setSelectedIndex(SelectListInterface.setSelectedValue(currentAdapter, value, defaultValue));
        setLast(value, defaultValue);
    }
    public void setSelectedValue(Object value)
    {
        int index = SelectListInterface.setSelectedValue(currentAdapter, value);

        if(index >= 0)
        {
            setSelectedIndex(index);
        }
        setLast(value, null);
    }

    public int getSelectedItemPosition()
    {
        Editable currentValue = getText();
        return(currentValue != null ? getListIndex(currentValue.toString()) : -1);
    }

    public Object getSelectedValue(Object defaultValue)
    {
        int index = getSelectedItemPosition();

        if(currentAdapter != null && index >= 0)
        {
            IconSpinner.Item selectedItem = (IconSpinner.Item)currentAdapter.getItem(index);
            if(selectedItem != null && selectedItem.value != null)
            {
                return(selectedItem.value);
            }
        }

        return(defaultValue);
    }
}
