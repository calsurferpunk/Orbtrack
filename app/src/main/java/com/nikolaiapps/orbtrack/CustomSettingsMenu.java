package com.nikolaiapps.orbtrack;


import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;
import java.util.ArrayList;


public class CustomSettingsMenu extends FrameLayout
{
    public interface OnExpandedStateChangedListener
    {
        void onExpandedStateChanged(CustomSettingsMenu menu, boolean isExpanded) ;
    }

    private boolean showMessages;
    private int menuOffset;
    private final int buttonSizePx;
    private final int centerButtonCountMax;
    private final LinearLayout centerLayout;
    private final LinearLayout endLayout;
    private final ShapeAppearanceModel buttonModel;
    private final FloatingActionStateButton stateButton;
    private final FloatingActionStateButton nextButton;
    private final FloatingActionStateButton previousButton;
    private final ArrayList<FloatingActionStateButton> centerButtons;
    private OnExpandedStateChangedListener expandedStateChangedListener;

    public CustomSettingsMenu(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        float[] sizes;
        int buttonSizeDp = 42;
        int screenWidthDp = Globals.getDeviceDp(context);

        //set defaults
        menuOffset = 0;
        showMessages = false;
        centerButtonCountMax = (screenWidthDp - (buttonSizeDp * 5)) / buttonSizeDp;     //note: leave room for state, previous, next, buffer space, and end button
        centerButtons = new ArrayList<>(0);
        expandedStateChangedListener = null;

        //create layouts
        centerLayout = createLayout(Gravity.CENTER);
        endLayout = createLayout(Gravity.END | Gravity.CENTER_VERTICAL);

        //get sizes and model
        sizes = Globals.dpsToPixels(context, buttonSizeDp, 36);
        buttonSizePx = (int)sizes[0];
        buttonModel = ShapeAppearanceModel.builder().setAllCorners(CornerFamily.ROUNDED, sizes[1]).build();

        //create state button
        stateButton = (FloatingActionStateButton)createButton(this, R.drawable.ic_expand_less, true, -1, false);
        stateButton.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL));
        stateButton.setStateColors(Color.TRANSPARENT, Color.TRANSPARENT);
        stateButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                boolean setChecked = !stateButton.isChecked();
                int layoutVisibility = (setChecked ? View.VISIBLE : View.GONE);

                //update state button status
                stateButton.setChecked(setChecked);
                stateButton.setImageResource(setChecked ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);

                //update layout visibilities
                centerLayout.setVisibility(layoutVisibility);
                endLayout.setVisibility(layoutVisibility);

                //if listener exists
                if(expandedStateChangedListener != null)
                {
                    //call it
                    expandedStateChangedListener.onExpandedStateChanged(CustomSettingsMenu.this, setChecked);
                }
            }
        });

        //create next and previous button
        nextButton = (FloatingActionStateButton)createButton(null, R.drawable.ic_arrow_right_white, true, -1, false);
        nextButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                menuOffset++;
                updateDisplay();
            }
        });
        previousButton = (FloatingActionStateButton)createButton(null, R.drawable.ic_arrow_left_white, true, -1, false);
        previousButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                menuOffset--;
                updateDisplay();
            }
        });
    }
    public CustomSettingsMenu(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }
    public CustomSettingsMenu(Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }
    public CustomSettingsMenu(Context context)
    {
        this(context, null);
    }

    //Creates a layout
    private LinearLayout createLayout(int gravity)
    {
        Context context = getContext();
        LayoutTransition transition = new LayoutTransition();

        //create and add layout in given location
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, gravity));
        layout.setLayoutTransition(transition);
        layout.setGravity(Gravity.CENTER);
        layout.setVisibility(View.GONE);
        this.addView(layout);

        //return layout
        return(layout);
    }

    //Creates a button
    private FloatingActionButton createButton(ViewGroup parent, int imageId, boolean isState, int stringId, boolean allowListAdd)
    {
        Context context = getContext();
        Resources res = context.getResources();
        FloatingActionButton button = (isState ? new FloatingActionStateButton(context) : new FloatingActionButton(context));

        //create button
        button.setBackground(null);
        if(isState)
        {
            FloatingActionStateButton actionStateButton = (FloatingActionStateButton)button;

            actionStateButton.setChecked(false);
            actionStateButton.setStateColors(Color.TRANSPARENT, Globals.getColor(127, Color.GRAY));
            if(stringId > -1)
            {
                actionStateButton.setOnCheckedChangedListener(new FloatingActionStateButton.OnCheckedChangedListener()
                {
                    @Override
                    public void onCheckedChanged(FloatingActionStateButton button, boolean isChecked)
                    {
                        //if showing messages and button is on screen
                        if(showMessages && button.isShown())
                        {
                            //show enabled status
                            Toast.makeText(context, res.getString(stringId) + ": " + res.getString(isChecked ? R.string.title_enabled : R.string.title_disabled), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
        else
        {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        }
        button.setCompatElevation(0f);
        button.setCustomSize(buttonSizePx);
        button.setImageResource(imageId);
        button.setSupportImageTintList(ColorStateList.valueOf(Color.WHITE));
        button.setShapeAppearanceModel(buttonModel);
        button.setPadding(0, 0, 0, 0);
        if(parent instanceof LinearLayout)
        {
            LinearLayout.LayoutParams buttonLayoutParams = (LinearLayout.LayoutParams)button.getLayoutParams();

            if(buttonLayoutParams == null)
            {
                buttonLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            buttonLayoutParams.setMargins(0, 0, 0, 0);
            button.setLayoutParams(buttonLayoutParams);
        }

        //if parent exists
        if(parent != null)
        {
            //add to parent
            parent.addView(button);
        }

        //if allow adding, is a state button, and added to center layout
        if(allowListAdd && isState && centerLayout.equals(parent))
        {
            //add to center buttons
            centerButtons.add((FloatingActionStateButton)button);

            //possibly update display
            updateDisplay();
        }

        //return button
        return(button);
    }

    //Add menu item to center
    public FloatingActionStateButton addMenuItem(int imageId, int stringId)
    {
        return((FloatingActionStateButton)createButton(centerLayout, imageId, true, stringId, true));
    }
    public FloatingActionStateButton addMenuItem(int imageId)
    {
        return(addMenuItem(imageId, -1));
    }

    //Add menu item to end
    public FloatingActionButton addEndItem(int imageId)
    {
        return(createButton(endLayout, imageId, false, -1, false));
    }

    //Set showing messages
    public void setMessagesEnabled(boolean show)
    {
        showMessages = show;
    }

    //Sets on expanded state changed listener
    public void setOnExpandedStateChangedListener(OnExpandedStateChangedListener listener)
    {
        expandedStateChangedListener = listener;
    }

    //Updates display
    public void updateDisplay()
    {
        int index;
        int buttonOffset;
        int centerButtonCount = centerButtons.size();

        //if center layout exists and too many buttons in center
        //note: allows for +1 to fill in space for what would be next
        if(centerLayout != null && centerButtonCount > centerButtonCountMax + 1)
        {
            //clear buttons
            centerLayout.removeAllViews();

            //if there is an offset
            if(menuOffset > 0)
            {
                //add previous button
                centerLayout.addView(previousButton);
            }

            //get button offset
            buttonOffset = (menuOffset * centerButtonCountMax);

            //go through buttons for this menu offset
            for(index = buttonOffset; index < centerButtonCount && index < (buttonOffset + centerButtonCountMax); index++)
            {
                //add current button
                centerLayout.addView(centerButtons.get(index));
            }

            //if there are more buttons
            if(index < centerButtonCount)
            {
                //add next button
                centerLayout.addView(nextButton);
            }
        }
    }

    //Close/hide menu
    public boolean close()
    {
        //if state button exists and is open
        if(stateButton != null && stateButton.isChecked())
        {
            //close it
            stateButton.performClick();
            return(true);
        }
        else
        {
            return(false);
        }
    }
}
