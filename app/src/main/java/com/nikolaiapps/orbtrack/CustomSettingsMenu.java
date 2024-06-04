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
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;
import java.util.ArrayList;

public class CustomSettingsMenu extends FrameLayout
{
    public interface OnExpandedStateChangedListener
    {
        void onExpandedStateChanged(CustomSettingsMenu menu, boolean isExpanded) ;
    }

    private boolean showTitles;
    private boolean showMessages;
    private int menuOffset;
    private final int imageSizePx;
    private final int buttonSizePx;
    private final int buttonTitlePx;
    private final int centerButtonCountMax;
    private final LinearLayout centerLayout;
    private final LinearLayout endLayout;
    private final ShapeAppearanceModel buttonModel;
    private final FloatingActionStateButton stateButton;
    private final FloatingActionStateButton nextButton;
    private final FloatingActionStateButton previousButton;
    private final ArrayList<View> centerViews;
    private OnExpandedStateChangedListener expandedStateChangedListener;

    public CustomSettingsMenu(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        float[] sizes;
        float scalePercent = Settings.getQuickSettingsScale(context);
        int imageSizeDp = (int)(24 * scalePercent);
        int buttonSizeDp = (int)(42 * scalePercent);
        int screenWidthDp = Globals.getDeviceDp(context);
        int usedButtonSizeDp = Math.max(buttonSizeDp, 42);
        float cornerSizeDp = (28 * scalePercent);

        //set defaults
        menuOffset = 0;
        showTitles = true;
        showMessages = false;
        centerButtonCountMax = Math.max((screenWidthDp - (usedButtonSizeDp * 4)) / usedButtonSizeDp, 2);     //note: leave room for state, previous, next, buffer space, and end button
        centerViews = new ArrayList<>(0);
        expandedStateChangedListener = null;

        //create layouts
        centerLayout = createLayout(this, Gravity.CENTER, true);
        endLayout = createLayout(this, Gravity.END | Gravity.CENTER_VERTICAL, true);

        //get sizes and model
        sizes = Globals.dpsToPixels(context, imageSizeDp, buttonSizeDp, cornerSizeDp);
        imageSizePx = (int)sizes[0];
        buttonSizePx = (int)sizes[1];
        buttonTitlePx = (int)Math.max((buttonSizePx / 11.5f), 10);
        buttonModel = ShapeAppearanceModel.builder().setAllCorners(CornerFamily.ROUNDED, sizes[2]).build();

        //create state button
        stateButton = createButton(this, R.drawable.ic_expand_less, R.drawable.ic_expand_more, -1, -1, false);
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
        nextButton = createButton(null, R.drawable.ic_arrow_right_white, -1, -1, -1, false);
        nextButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                menuOffset++;
                updateDisplay();
            }
        });
        previousButton = createButton(null, R.drawable.ic_arrow_left_white, -1, -1, -1, false);
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
    private LinearLayout createLayout(ViewGroup parent, int gravity, boolean horizontal)
    {
        Context context = getContext();
        LayoutTransition transition = (horizontal ? new LayoutTransition() : null);

        //create and add layout in given location
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, gravity));
        if(horizontal)
        {
            layout.setLayoutTransition(transition);
        }
        layout.setGravity(Gravity.CENTER);
        layout.setOrientation(horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        layout.setVisibility(horizontal ? View.GONE : View.VISIBLE);
        if(parent != null)
        {
            parent.addView(layout);
        }

        //return layout
        return(layout);
    }

    //Creates a button with a title
    private View createTitledButton(FloatingActionStateButton button)
    {
        Context context = getContext();
        Object buttonTag = button.getTag();
        LinearLayout buttonView = createLayout(null, Gravity.CENTER, false);

        buttonView.addView(button);
        if(buttonTag instanceof String)
        {
            TextView buttonTitle = new TextView(context);
            buttonTitle.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            buttonTitle.setText((String)buttonTag);
            buttonTitle.setTextSize(buttonTitlePx);
            buttonTitle.setTextColor(Color.WHITE);
            buttonView.addView(buttonTitle);
        }

        return(buttonView);
    }

    //Creates a button
    private FloatingActionStateButton createButton(ViewGroup parent, int normalImageId, int checkedImageId, int titleStringId, int messageStringId, boolean allowListAdd)
    {
        Context context = getContext();
        boolean parentIsCenter = centerLayout.equals(parent);
        View buttonView;
        Resources res = context.getResources();
        String titleString = (titleStringId > -1 ? res.getString(titleStringId) : null);
        String messageString = (messageStringId > -1 ? res.getString(messageStringId) : null);
        FloatingActionStateButton button = new FloatingActionStateButton(context);

        //create button
        button.setBackground(null);
        button.setShowChecked(allowListAdd);
        button.setChecked(false);
        button.setStateColors(Color.TRANSPARENT, Globals.getColor(127, Color.GRAY));
        if(messageString != null)
        {
            button.setOnCheckedChangedListener(new FloatingActionStateButton.OnCheckedChangedListener()
            {
                @Override
                public void onCheckedChanged(FloatingActionStateButton button, boolean isChecked)
                {
                    //if showing messages and button is on screen
                    if(showMessages && button.isShown())
                    {
                        //show enabled status
                        Toast.makeText(context,  messageString + ": " + res.getString(isChecked ? R.string.title_enabled : R.string.title_disabled), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        button.setCompatElevation(0f);
        button.setCustomSize(buttonSizePx);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setImageDrawable(Globals.getDrawableSized(context, normalImageId, imageSizePx, imageSizePx, false, false));
        button.setImageCheckedDrawable(checkedImageId > -1 ? Globals.getDrawableSized(context, checkedImageId, imageSizePx, imageSizePx, false, false) : null);
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
        button.setTag(titleString);

        //get button view
        buttonView = (showTitles && parentIsCenter ? createTitledButton(button) : button);

        //if parent exists
        if(parent != null)
        {
            //add to parent
            parent.addView(buttonView);
        }

        //if allow adding and added to center layout
        if(allowListAdd && parentIsCenter)
        {
            //add to center views
            centerViews.add(buttonView);

            //possibly update display
            updateDisplay();
        }

        //return button
        return(button);
    }

    //Add menu item to center
    public FloatingActionStateButton addMenuItem(int imageId, int titleStringId, int messageStringId)
    {
        return(createButton(centerLayout, imageId, -1, titleStringId, messageStringId, true));
    }

    //Add menu item to end
    public FloatingActionStateButton addEndItem(int normalImageId, int checkedImageId)
    {
        return(createButton(endLayout, normalImageId, checkedImageId, -1, -1, false));
    }

    //Set showing titles
    public void setTitlesEnabled(boolean show)
    {
        int index;

        //if showing titles is changing
        if(showTitles != show)
        {
            //update status
            showTitles = show;

            //go through current center views
            for(index = 0; index < centerViews.size(); index++)
            {
                //remember current view and parent
                View currentView = centerViews.get(index);
                ViewParent parentView = currentView.getParent();
                ViewGroup parentGroupView = (parentView instanceof ViewGroup ? (ViewGroup)parentView : null);
                boolean usingParent = (parentGroupView != null);

                //if showing titles
                if(showTitles)
                {
                    //if current view is a button
                    if(currentView instanceof FloatingActionStateButton)
                    {
                        View titledButton;

                        //remove button and add back with title
                        if(usingParent)
                        {
                            parentGroupView.removeView(currentView);
                        }
                        centerViews.remove(index);
                        titledButton = createTitledButton((FloatingActionStateButton)currentView);       //note: must not be created until button is removed
                        centerViews.add(index, titledButton);
                        if(usingParent)
                        {
                            parentGroupView.addView(titledButton);
                        }
                    }
                }
                else
                {
                    //if current view is a view group
                    if(currentView instanceof ViewGroup)
                    {
                        //remember current layout view
                        ViewGroup currentLayoutView = (ViewGroup)currentView;

                        //if current layout view has children
                        if(currentLayoutView.getChildCount() > 0)
                        {
                            //remember first child view
                            View currentChildView = currentLayoutView.getChildAt(0);

                            //if child view is a button
                            if(currentChildView instanceof FloatingActionStateButton)
                            {
                                //remove titled button and add button only
                                if(usingParent)
                                {
                                    parentGroupView.removeView(currentView);
                                }
                                currentLayoutView.removeAllViews();
                                centerViews.remove(index);
                                centerViews.add(index, currentChildView);
                                if(usingParent)
                                {
                                    parentGroupView.addView(currentChildView);
                                }
                            }
                        }
                    }
                }
            }

            //possibly update display
            updateDisplay();
        }
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
    private void updateDisplay()
    {
        int index;
        int buttonOffset;
        int centerViewCount = centerViews.size();

        //if center layout exists and too many views in center
        //note: allows for +1 to fill in space for what would be next
        if(centerLayout != null && centerViewCount > centerButtonCountMax + 1)
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

            //go through center views for this menu offset
            for(index = buttonOffset; index < centerViewCount && index < (buttonOffset + centerButtonCountMax); index++)
            {
                //add current view
                centerLayout.addView(centerViews.get(index));
            }

            //if there are more buttons
            if(index < centerViewCount)
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
