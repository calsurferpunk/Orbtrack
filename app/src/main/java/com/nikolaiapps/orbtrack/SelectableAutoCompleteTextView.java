package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.AdapterView;


public class SelectableAutoCompleteTextView extends androidx.appcompat.widget.AppCompatAutoCompleteTextView implements SelectListInterface
{
    private int textColor;
    private int textSelectedColor;
    private int backgroundColor;
    private int backgroundItemColor;
    private int backgroundItemSelectedColor;
    private IconSpinner.CustomAdapter currentAdapter;
    private AdapterView.OnItemSelectedListener selectedListener;

    public SelectableAutoCompleteTextView(Context context)
    {
        super(context);
        init(null, null);
    }

    public SelectableAutoCompleteTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    public SelectableAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
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

    private void init(Context context, AttributeSet attrs)
    {
        int inputType = getInputType();

        //if attributes are set
        if(attrs != null)
        {
            //get attribute values
            try(TypedArray valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SelectableAutoCompleteTextView, 0, 0))
            {
                inputType = valueArray.getInt(R.styleable.SelectableAutoCompleteTextView_android_inputType, inputType);
                valueArray.recycle();
            }
            catch(NoSuchMethodError noMethod)
            {
                //do nothing
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }

        //setup
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

                //if listener and adapter are set
                if(selectedListener != null && currentAdapter != null)
                {
                    //if set a selection
                    selectedIndex = getListIndex(s.toString());
                    if(selectedIndex != -1)
                    {
                        //remember selected item
                        IconSpinner.Item selectedItem = (IconSpinner.Item)currentAdapter.getItem(selectedIndex);

                        //set selection and send event
                        currentAdapter.setSelectedIndex(selectedIndex);
                        selectedId = (selectedItem != null && selectedItem.value instanceof Integer ? (int)selectedItem.value : selectedId);
                        selectedListener.onItemSelected(null, SelectableAutoCompleteTextView.this, selectedIndex, selectedId);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener)
    {
        selectedListener = listener;
    }

    public void setAdapter(IconSpinner.CustomAdapter adapter)
    {
        Object firstItem = null;

        currentAdapter = adapter;
        if(currentAdapter != null)
        {
            firstItem = currentAdapter.getItem(0);
            backgroundColor = currentAdapter.getBackgroundColor();
            backgroundItemColor = currentAdapter.getBackgroundItemColor();
            backgroundItemSelectedColor = currentAdapter.getBackgroundItemSelectedColor();
            textColor = currentAdapter.getTextColor();
            textSelectedColor = currentAdapter.getTextSelectedColor();
        }

        setBackgroundColor(backgroundColor);
        setBackgroundItemColor(backgroundItemColor);
        setBackgroundItemSelectedColor(backgroundItemSelectedColor);
        setTextColor(textColor);
        setTextSelectedColor(textSelectedColor);
        if(firstItem != null)
        {
            setSelectedText(firstItem.toString());
        }

        super.setAdapter(adapter);
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

    public void setTextColor(int color)
    {
        textColor = color;
        SelectListInterface.setTextColor(currentAdapter, textColor);
        setTextSelectedColor(textColor);
    }

    public void setTextSelectedColor(int color)
    {
        textSelectedColor = color;
        SelectListInterface.setTextSelectedColor(currentAdapter, textSelectedColor);
    }

    private void setSelectedIndex(int index)
    {
        if(currentAdapter != null && index >= 0)
        {
            IconSpinner.Item selectedItem = (IconSpinner.Item)currentAdapter.getItem(index);
            boolean haveSelectedItem = (selectedItem != null);
            String selectedText = (haveSelectedItem ? selectedItem.text : null);
            Drawable selectedIcon = (haveSelectedItem ? selectedItem.getIcon() : null);

            if(selectedText != null)
            {
                setText(selectedText, false);
            }
            else if(selectedIcon != null)
            {
                setCompoundDrawablesWithIntrinsicBounds(selectedIcon, null, null, null);
            }
        }
    }

    public void setSelectedText(String value)
    {
        setSelectedIndex(SelectListInterface.setSelectedText(currentAdapter, value));
    }

    public void setSelectedValue(Object value, Object defaultValue)
    {
        setSelectedIndex(SelectListInterface.setSelectedValue(currentAdapter, value, defaultValue));
    }
    public boolean setSelectedValue(Object value)
    {
        int index = SelectListInterface.setSelectedValue(currentAdapter, value);
        boolean setSelection = (index >= 0);

        if(setSelection)
        {
            setSelectedIndex(index);
        }

        return(setSelection);
    }

    public Object getSelectedValue(Object defaultValue)
    {
        Editable currentValue = getText();
        int index = (currentValue != null ? getListIndex(currentValue.toString()) : -1);

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
