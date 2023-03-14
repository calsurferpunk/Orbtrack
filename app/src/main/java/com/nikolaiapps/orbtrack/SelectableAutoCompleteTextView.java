package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.AdapterView;


public class SelectableAutoCompleteTextView extends androidx.appcompat.widget.AppCompatAutoCompleteTextView implements SelectListInterface
{
    private boolean allowAutoSelect;
    private int textColor;
    private int textSelectedColor;
    private int backgroundColor;
    private int backgroundItemColor;
    private int backgroundItemSelectedColor;
    private final int iconSizePx;
    private final Paint iconPaint;
    private final Rect iconStartArea;
    private final Rect iconEndArea;
    private IconSpinner.CustomAdapter currentAdapter;
    private AdapterView.OnItemSelectedListener selectedListener;

    public SelectableAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

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
        allowAutoSelect = true;
        iconPaint = new Paint();
        iconStartArea = new Rect();
        iconEndArea = new Rect();
        iconSizePx = (int)Globals.dpToPixels(context, 32);
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
    public SelectableAutoCompleteTextView(Context context, AttributeSet attrs)
    {
        this(context, attrs, R.attr.autoCompleteTextViewStyle);
    }
    public SelectableAutoCompleteTextView(Context context)
    {
        this(context, null);
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        int iconWidth;
        int iconHeight;
        int halfHeight = (getMeasuredHeight() / 2);
        int halfPaddingLeft = (getPaddingLeft() / 2);
        float iconRatio;
        IconSpinner.Item selectedItem = (currentAdapter != null ? currentAdapter.getSelectedItem() : null);
        boolean haveItem = (selectedItem != null);
        Drawable selectedIcon = (haveItem ? selectedItem.getIcon() : null);
        Bitmap selectedBitmap = (selectedIcon != null && !(selectedIcon instanceof ColorDrawable) ? Globals.getBitmap(selectedIcon) : null);
        boolean haveIconAndText = (haveItem && selectedItem.text != null && selectedBitmap != null);

        if(haveIconAndText)
        {
            canvas.save();
            canvas.translate(iconSizePx, getPaddingTop() / -3f);
        }

        super.onDraw(canvas);

        if(haveIconAndText)
        {
            canvas.restore();
        }

        //if have an icon image
        if(selectedBitmap != null)
        {
            //draw icon image centered vertically
            iconWidth = selectedBitmap.getWidth();
            iconHeight = selectedBitmap.getHeight();
            iconRatio = iconSizePx / (float)iconHeight;
            iconStartArea.set(0, 0, iconWidth, iconHeight);
            iconEndArea.set(halfPaddingLeft, halfHeight - (iconSizePx / 2), halfPaddingLeft + (int)(iconWidth * iconRatio), halfHeight + (iconSizePx / 2));
            canvas.drawBitmap(selectedBitmap, iconStartArea, iconEndArea, iconPaint);
        }
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

    public void setAdapter(IconSpinner.CustomAdapter adapter)
    {
        Context context = getContext();
        int iconHeightPx = (int)Globals.dpToPixels(context, 32);
        Object firstItem = null;
        IconSpinner.Item[] adapterItems;

        //set adapter
        currentAdapter = adapter;
        if(currentAdapter != null)
        {
            //get first item and adapter colors
            firstItem = currentAdapter.getItem(0);
            backgroundColor = currentAdapter.getBackgroundColor();
            backgroundItemColor = currentAdapter.getBackgroundItemColor();
            backgroundItemSelectedColor = currentAdapter.getBackgroundItemSelectedColor();
            textColor = currentAdapter.getTextColor();
            textSelectedColor = currentAdapter.getTextSelectedColor();

            //if context exists
            if(context != null)
            {
                //go through each item
                adapterItems = currentAdapter.getItems();
                for(IconSpinner.Item currentItem : adapterItems)
                {
                    //preload icon for text display
                    currentItem.loadIcon3(context, iconHeightPx);
                }
            }
        }

        //set colors and any selection
        setBackgroundColor(backgroundColor);
        setBackgroundItemColor(backgroundItemColor);
        setBackgroundItemSelectedColor(backgroundItemSelectedColor);
        setTextColor(textColor);
        setTextSelectedColor(textSelectedColor);
        if(firstItem != null)
        {
            if(allowAutoSelect)
            {
                setSelectedText(firstItem.toString());
            }
            if(firstItem instanceof IconSpinner.Item && adapter.getUsingIcon3Only())
            {
                Drawable firstIcon = ((IconSpinner.Item)firstItem).getIcon();
                if(firstIcon != null && firstIcon.getIntrinsicWidth() > 0)
                {
                    setDropDownWidth(firstIcon.getIntrinsicWidth() + getTotalPaddingLeft() + getTotalPaddingRight());
                }
            }
        }

        //set adapter
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

    public void setTextColor(int color)
    {
        textColor = color;
        super.setTextColor(currentAdapter != null && !currentAdapter.getUsingText() ? Color.TRANSPARENT : textColor);
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
            String selectedText = (selectedItem != null ? selectedItem.text : null);

            setText(selectedText, false);
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
