package com.nikolaiapps.orbtrack;


import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;
import java.util.ArrayList;


public class FloatingActionStateButtonMenu extends LinearLayout
{
    private static final LinearLayout.LayoutParams mainMenuLayoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    private static final LinearLayout.LayoutParams defaultLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    private static final int SmallDp = 36;
    private static final int LargeDp = 44;

    private final int menuLevel;
    private int menuImageId;
    private int imageTintColor;
    private int checkedTintColor;
    private int backgroundTintColor;
    private FloatingActionStateButton mainMenu;
    private ArrayList<View> subMenus;

    public FloatingActionStateButtonMenu(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);

        menuLevel = 1;
        menuImageId = -1;
        init(attrs);
    }
    public FloatingActionStateButtonMenu(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        menuLevel = 1;
        menuImageId = -1;
        init(attrs);
    }

    private void init(AttributeSet attrs)
    {
        int spacingDp = (int)Globals.dpToPixels(getContext(), 3);

        subMenus = new ArrayList<>(0);
        if(menuLevel == 1)
        {
            mainMenuLayoutParams.setMargins(0, spacingDp, spacingDp, 0);
            defaultLayoutParams.setMargins(spacingDp, spacingDp, spacingDp, spacingDp);
        }

        if(attrs == null)
        {
            imageTintColor = Color.BLACK;
            backgroundTintColor = Color.WHITE;
            checkedTintColor = Color.GRAY;
            if(menuImageId == -1)
            {
                menuImageId = R.drawable.ic_settings_black;
            }
        }
        else
        {
            try(TypedArray valueArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.FloatingActionStateButtonMenu, 0, 0))
            {
                imageTintColor = valueArray.getColor(R.styleable.FloatingActionStateButtonMenu_imageTint, Color.BLACK);
                backgroundTintColor = valueArray.getColor(R.styleable.FloatingActionStateButtonMenu_backgroundTint, Color.WHITE);
                checkedTintColor = valueArray.getColor(R.styleable.FloatingActionStateButtonMenu_checkedTint, Color.GRAY);
                if(menuImageId == -1)
                {
                    menuImageId = valueArray.getInt(R.styleable.FloatingActionStateButtonMenu_menuImageId, R.drawable.ic_settings_black);
                }
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

        setClipChildren(false);
        setClipToPadding(false);
        setLayoutTransition(new LayoutTransition());
        getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        addMainMenu();
    }

    private FloatingActionStateButton createButton(int imageId, boolean showChecked)
    {
        Context context = getContext();
        FloatingActionStateButton menuButton = new FloatingActionStateButton(context);

        menuButton.setAlpha(1.0f);
        menuButton.setCompatElevation(1f);
        menuButton.setCustomSize((int)Globals.dpToPixels(context, SmallDp));
        menuButton.setStateColors(backgroundTintColor, checkedTintColor);
        menuButton.setShapeAppearanceModel(ShapeAppearanceModel.builder().setAllCorners(CornerFamily.ROUNDED, Globals.dpToPixels(context, LargeDp)).build());
        menuButton.setShowChecked(showChecked);
        menuButton.setImageDrawable(Globals.getDrawable(context, imageId, imageTintColor, false));
        menuButton.setSupportImageTintList(ColorStateList.valueOf(imageTintColor));
        menuButton.setUseCompatPadding(true);
        menuButton.setClickable(true);

        return(menuButton);
    }

    private TextView createText(int stringId)
    {
        Context context = getContext();
        int padding = (int)Globals.dpToPixels(context, 5);
        TextView menuText = new TextView(context);
        LinearLayout.LayoutParams currentLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        currentLayoutParams.gravity = Gravity.CENTER;

        menuText.setBackgroundColor(backgroundTintColor);
        menuText.setPadding(padding, padding, padding, padding);
        menuText.setLayoutParams(currentLayoutParams);
        if(stringId != -1)
        {
            menuText.setText(context.getResources().getString(stringId));
        }
        menuText.setTextColor(imageTintColor);

        return(menuText);
    }

    private void addMainMenu()
    {
        mainMenu = createButton(menuImageId, (menuLevel == 1));
        addView(mainMenu, mainMenuLayoutParams);
        mainMenu.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int visibility;
                boolean mainChecked;
                FloatingActionStateButton main = ((FloatingActionStateButton)v);

                //update status and visibility setting
                main.setChecked(!main.isChecked());
                mainChecked = main.isChecked();
                visibility = (mainChecked ? View.VISIBLE : View.GONE);

                //update size
                mainMenu.setCustomSize((int)Globals.dpToPixels(getContext(), (mainChecked ? LargeDp : SmallDp)));

                //go through each sub menu
                for(View currentView : subMenus)
                {
                    //update visibility
                    currentView.setVisibility(visibility);

                    //if view is a menu
                    if(currentView instanceof FloatingActionStateButtonMenu)
                    {
                        //if not checked
                        if(!mainChecked)
                        {
                            //make sure to close it
                            ((FloatingActionStateButtonMenu)currentView).close();
                        }
                    }
                    else if(currentView instanceof IconSpinner)
                    {
                        setBackgroundColor(mainChecked ? ((IconSpinner)currentView).getBackgroundColor() : Color.TRANSPARENT);
                    }
                }
            }
        });

        //fix background display glitch on older APIs
        mainMenu.hide();
        mainMenu.show();
    }

    public FloatingActionStateButton addMenuItem(int imageId, int stringId)
    {
        Context context = getContext();
        float[] dps = Globals.dpsToPixels(context, 7, (Build.VERSION.SDK_INT >= 21 ? 20 : 7));
        int padding = (int)dps[0];
        int offset = (int)dps[1];
        LinearLayout.LayoutParams textParams;
        LinearLayout.LayoutParams itemParams;
        LinearLayout menuLayout = new LinearLayout(context);
        final FloatingActionStateButton menuItem = createButton(imageId, true);
        TextView menuText = createText(stringId);

        menuLayout.setVisibility(View.GONE);
        if(subMenus.add(menuLayout))
        {
            //add item
            menuLayout.addView(menuItem, defaultLayoutParams);
            menuLayout.addView(menuText);
            addView(menuLayout, defaultLayoutParams);

            //setup text
            textParams = (LayoutParams)menuText.getLayoutParams();
            textParams.setMargins(-offset, 0, 0, 0);
            menuText.setLayoutParams(textParams);
            menuText.setPadding(padding + offset, padding, padding + offset, padding);
            menuText.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if(menuItem.isEnabled())
                    {
                        menuItem.performClick();
                    }
                }
            });
            menuText.setClickable(true);

            //setup button
            itemParams = (LayoutParams)menuItem.getLayoutParams();
            itemParams.setMargins((int)(offset / (Build.VERSION.SDK_INT >= 21 ? 2.5 : 1.4)), -2, 0, -2);
            menuItem.setPadding(0, 0, 0, 0);
            menuItem.setLayoutParams(itemParams);
        }

        return(menuItem);
    }

    public boolean close()
    {
        //if main menu exists and is open
        if(mainMenu != null && mainMenu.isChecked())
        {
            //close it
            mainMenu.performClick();
            return(true);
        }
        else
        {
            return(false);
        }
    }

}
