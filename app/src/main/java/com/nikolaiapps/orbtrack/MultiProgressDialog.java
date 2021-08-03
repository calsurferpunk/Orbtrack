package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.progressindicator.LinearProgressIndicator;


public class MultiProgressDialog extends AlertDialog
{
    private TextView messageText;
    private TextView messageText2;
    private TextView percentText;
    private TextView percentText2;
    private LinearProgressIndicator bar;
    private LinearProgressIndicator bar2;
    private LinearLayout barLayout;
    private LinearLayout barLayout2;

    protected MultiProgressDialog(@NonNull Context context)
    {
        super(context);
    }

    protected MultiProgressDialog(@NonNull Context context, int themeResId)
    {
        super(context, themeResId);
    }

    protected MultiProgressDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener)
    {
        super(context, cancelable, cancelListener);
    }

    @Override @SuppressLint("InflateParams")
    protected void onCreate(Bundle savedInstanceState)
    {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.multi_progress_dialog, null);

        messageText = view.findViewById(R.id.Multi_Progress_Message_Text);
        messageText2 = view.findViewById(R.id.Multi_Progress_Message_Text2);
        percentText = view.findViewById(R.id.Multi_Progress_Percent_Text);
        percentText2 = view.findViewById(R.id.Multi_Progress_Percent_Text2);
        bar = view.findViewById(R.id.Multi_Progress_Bar);
        bar2 = view.findViewById(R.id.Multi_Progress_Bar2);
        barLayout = view.findViewById(R.id.Multi_Progress_Bar_Layout);
        barLayout2 = view.findViewById(R.id.Multi_Progress_Bar_Layout2);

        setView(view);

        super.onCreate(savedInstanceState);
    }

    private void setProgressDisplays(LinearLayout lyt, LinearProgressIndicator br, TextView txt, int percent)
    {
        if(percent > 100)
        {
            percent = 100;
        }
        else if(percent < 0)
        {
            percent = 0;
        }

        if(lyt != null)
        {
            lyt.setVisibility(View.VISIBLE);
        }
        if(br != null)
        {
            br.setProgress(percent);
        }
        if(txt != null)
        {
            txt.setText(String.format(Locale.US, "%1d%%", percent));
        }
    }

    private void setMessageDisplay(LinearLayout lyt, TextView msgTxt, CharSequence message)
    {
        if(msgTxt != null)
        {
            msgTxt.setText(message);
            msgTxt.setVisibility(View.VISIBLE);
        }
        if(lyt != null)
        {
            lyt.setVisibility(View.INVISIBLE);
        }
    }

    public void setProgress(int percent)
    {
        setProgressDisplays(barLayout, bar, percentText, percent);
    }
    public void setProgress(long value, long total)
    {
        setProgress(total != 0 ? (int)((value / (float)total) * 100) : 0);
    }

    public void setProgress2(int percent)
    {
        setProgressDisplays(barLayout2, bar2, percentText2, percent);
    }
    public void setProgress2(long value, long total)
    {
        setProgress2(total != 0 ? (int)((value / (float)total) * 100) : 0);
    }

    public void setMessage(CharSequence message)
    {
        setMessageDisplay(barLayout, messageText, message);
    }

    public void setMessage2(CharSequence message)
    {
        setMessageDisplay(barLayout2, messageText2, message);
    }
}
