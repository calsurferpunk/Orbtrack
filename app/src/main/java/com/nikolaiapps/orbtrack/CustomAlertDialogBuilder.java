package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;


public class CustomAlertDialogBuilder extends AlertDialog.Builder
{
    private TextView titleText = null;

    public CustomAlertDialogBuilder(@NonNull Context context, int themeResId, boolean useMaterial)
    {
        super(context, themeResId);
        setTitleView(context, useMaterial);
    }

    @SuppressLint("InflateParams")
    private void setTitleView(Context context, boolean useMaterial)
    {
        View titleView = LayoutInflater.from(context).inflate((useMaterial ? R.layout.dialog_title_view_material : R.layout.dialog_title_view), null, false);

        if(titleView instanceof TextView)
        {
            titleText = (TextView)titleView;
        }
        else if(titleView instanceof LinearLayout)
        {
            titleText = titleView.findViewById(R.id.Dialog_Title_Text);
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
