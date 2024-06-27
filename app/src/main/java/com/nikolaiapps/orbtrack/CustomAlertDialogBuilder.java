package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.card.MaterialCardView;


public class CustomAlertDialogBuilder extends AlertDialog.Builder
{
    private TextView titleText = null;

    public CustomAlertDialogBuilder(@NonNull Context context, int themeResId, boolean forSelection)
    {
        super(context, themeResId);
        setTitleView(context, forSelection);
    }

    @SuppressLint("InflateParams")
    private void setTitleView(Context context, boolean forSelection)
    {
        int backgroundColor;
        FrameLayout.LayoutParams params;
        View titleView = LayoutInflater.from(context).inflate(R.layout.dialog_title_view, null, false);
        MaterialCardView titleGroup;

        if(titleView instanceof TextView)
        {
            titleText = (TextView)titleView;
        }
        else if(titleView instanceof LinearLayout)
        {
            titleGroup = titleView.findViewById(R.id.Dialog_Title_Group);
            titleText = titleView.findViewById(R.id.Dialog_Title_Text);

            if(forSelection)
            {
                backgroundColor = Globals.resolveColorID(context, R.attr.pageHighlightBackground);
                titleGroup.setStrokeColor(Color.TRANSPARENT);
                titleGroup.setBackgroundColor(backgroundColor);
                titleText.setBackgroundColor(backgroundColor);

                params = (FrameLayout.LayoutParams)titleText.getLayoutParams();
                if(params != null)
                {
                    params.gravity = Gravity.LEFT;
                    titleText.setLayoutParams(params);
                }
            }
        }

        setCustomTitle(titleView);
    }

    @Override
    public CustomAlertDialogBuilder setTitle(CharSequence text)
    {
        if(titleText != null)
        {
            titleText.setText(text);
        }

        return(this);
    }

    @Override
    public CustomAlertDialogBuilder setIcon(Drawable icon)
    {
        if(titleText != null)
        {
            titleText.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }

        return(this);
    }
}
