package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.google.android.material.progressindicator.CircularProgressIndicator;


public class ChooseColorDialog
{
    public interface OnColorSelectedListener
    {
        void onColorSelected(int color);
    }

    //Gets background color image
    public static class GetColorBackgroundTask extends ThreadTask<Void, Void, Void>
    {
        public interface OnCreatedBackgroundListener
        {
            void onCreatedBackground(Bitmap image);
        }

        private final int imageWidth;
        private final int imageHeight;
        private final OnCreatedBackgroundListener onCreatedListener;

        public GetColorBackgroundTask(int width, int height, OnCreatedBackgroundListener listener)
        {
            imageWidth = width;
            imageHeight = height;
            onCreatedListener = listener;
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            onCreatedListener.onCreatedBackground(createColorImageBg(imageWidth, imageHeight));
            return(null);
        }
    }

    //displays
    private TextView colorBeforeText;
    private TextView colorCurrentText;
    private BorderButton colorBeforeView;
    private BorderButton colorCurrentView;
    private BorderButton colorCurrent2View;
    private CursorImageView colorImage;
    private CursorImageView brightnessBar;
    private CheckBox transparentCheck;
    private LinearLayout colorTableLayout;
    private LinearLayout colorImageLayout;
    private LinearLayout redLayout;
    private LinearLayout greenLayout;
    private LinearLayout blueLayout;
    private LinearLayout opacityLayout;
    private SeekBar[] barDisplays;
    private EditText[] textDisplays;
    private static BitmapDrawable colorImageBackground;
    private static final int[] colorIdList = new int[]{R.color.red_100, R.color.red, R.color.red_900, R.color.brown_200,
                                                       R.color.orange_100, R.color.orange, R.color.orange_900, R.color.brown,
                                                       R.color.yellow_100, R.color.yellow, R.color.yellow_700, R.color.brown_800,
                                                       R.color.green_100, R.color.green, R.color.green_900, R.color.white,
                                                       R.color.blue_100, R.color.blue, R.color.blue_900, R.color.grey,
                                                       R.color.purple_100, R.color.purple, R.color.purple_900, R.color.black };
    //
    //variables
    private Drawable icon;
    private boolean closed;
    private boolean reloading;
    private boolean showAdvanced;
    private boolean allowOpacity;
    private boolean allowTransparent;
    private int startColor;
    private int currentColor;
    private int lastColor;
    private int imageWidth;
    private int imageHeight;
    private int brightWidth;
    private int brightHeight;
    private int rgbOffset;
    private int rgbTextOffset;
    private float[] rgbBase;
    private String title;
    private Button negativeButton;
    private OnColorSelectedListener colorSelectedListener;

    public ChooseColorDialog(Context context, int defaultColor)
    {
        icon = null;
        closed = reloading = showAdvanced = allowOpacity = allowTransparent = false;
        setColor(defaultColor);
        colorSelectedListener = null;
        rgbOffset = rgbTextOffset = 0;
        rgbBase = new float[]{0, 0, 0, 0};
        title = (context != null ? context.getResources().getString(R.string.title_select_color) : null);
    }

    public void show(final Context context)
    {
        //if closed
        if(closed)
        {
            //stop
            return;
        }

        int index;
        int column;
        int orientation = Globals.getScreenOrientation(context);
        int paddingSize = (int)Globals.dpToPixels(context, 3);
        final Button neutralButton;
        final Button[] buttons;
        LayoutInflater viewInflater = (context != null ? (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) : null);
        TableRow currentRow = null;
        TableLayout colorTable;
        BorderButton colorBefore2View;
        ViewGroup.LayoutParams colorImageParams;
        ViewGroup.LayoutParams brightBarParams;
        View colorDialogView = (viewInflater != null ? viewInflater.inflate(orientation == Surface.ROTATION_90 || orientation == Surface.ROTATION_270 ? R.layout.choose_color_landscape_dialog : R.layout.choose_color_portrait_dialog, null, false) : null);
        final Resources res = (context != null ? context.getResources() : null);
        final CircularProgressIndicator colorProgress = (colorDialogView != null ? colorDialogView.findViewById(R.id.Color_Progress) : null);
        float[] buttonSize = Globals.dpsToPixels(context, 45, 35);

        //if context and view are set
        if(context != null && colorProgress != null)
        {
            colorBeforeText = colorDialogView.findViewById(R.id.Color_Before_Text);
            colorCurrentText = colorDialogView.findViewById(R.id.Color_Current_Text);
            colorBeforeView = colorDialogView.findViewById(R.id.Color_Before_View);
            colorBefore2View = colorDialogView.findViewById(R.id.Color_Before2_View);
            colorCurrentView = colorDialogView.findViewById(R.id.Color_Current_View);
            colorCurrent2View = colorDialogView.findViewById(R.id.Color_Current2_View);
            colorImage = colorDialogView.findViewById(R.id.Color_Image);
            brightnessBar = colorDialogView.findViewById(R.id.Color_Brightness_Bar);
            transparentCheck = colorDialogView.findViewById(R.id.Color_Transparent_Check);
            colorTableLayout = colorDialogView.findViewById(R.id.Color_Table_Layout);
            colorImageLayout = colorDialogView.findViewById(R.id.Color_Image_Layout);
            redLayout = colorDialogView.findViewById(R.id.Color_Red_Layout);
            greenLayout = colorDialogView.findViewById(R.id.Color_Green_Layout);
            blueLayout = colorDialogView.findViewById(R.id.Color_Blue_Layout);
            opacityLayout = colorDialogView.findViewById(R.id.Color_Opacity_Layout);
            colorTable = colorDialogView.findViewById(R.id.Color_Table);
            barDisplays = new SeekBar[]{colorDialogView.findViewById(R.id.Color_Red_Bar), colorDialogView.findViewById(R.id.Color_Green_Bar), colorDialogView.findViewById(R.id.Color_Blue_Bar), colorDialogView.findViewById(R.id.Color_Opacity_Bar)};
            textDisplays = new EditText[]{colorDialogView.findViewById(R.id.Color_Red_Text), colorDialogView.findViewById(R.id.Color_Green_Text), colorDialogView.findViewById(R.id.Color_Blue_Text), colorDialogView.findViewById(R.id.Color_Opacity_Text)};
            colorImageParams = colorImage.getLayoutParams();
            brightBarParams = brightnessBar.getLayoutParams();
            imageWidth = colorImageParams.width;
            imageHeight = colorImageParams.height;
            brightWidth = brightBarParams.width;
            brightHeight = brightBarParams.height;
            lastColor = Integer.MIN_VALUE;

            //setup rgb base
            rgbBase = new float[]{Color.red(currentColor), Color.green(currentColor), Color.blue(currentColor), Color.alpha(currentColor)};

            //setup color table
            column = 0;
            for(final int currentColorId : colorIdList)
            {
                //get color and create button
                final int currentListColor = ContextCompat.getColor(context, currentColorId);
                BorderButton currentButton = new BorderButton(new ContextThemeWrapper(context, R.style.ColorButton), null, 0);
                TableRow.LayoutParams buttonLayoutParams = new TableRow.LayoutParams((int)buttonSize[0], (int)buttonSize[1]);
                buttonLayoutParams.setMargins(paddingSize, paddingSize, paddingSize, paddingSize);
                currentButton.setBackgroundColor(currentListColor);
                currentButton.setLayoutParams(buttonLayoutParams);
                currentButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        //set color and displays
                        rgbBase[0] = Color.red(currentListColor);
                        rgbBase[1] = Color.green(currentListColor);
                        rgbBase[2] = Color.blue(currentListColor);
                        rgbBase[3] = 255;
                        updateBars(true);
                        updateTexts(true);
                        updateBrightnessBarColor(res);
                        updateCurrentColor();
                        updateCursors(true);
                    }
                });

                //if row needs to be created
                if(currentRow == null)
                {
                    //create row
                    currentRow = new TableRow(context);
                }


                //add button to row
                currentRow.addView(currentButton);

                //go to next column
                column++;
                if(column >= 4)
                {
                    //need to add row and start over
                    colorTable.addView(currentRow, new TableLayout.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                    currentRow = null;
                    column = 0;
                }
            }

            //setup color image
            if(colorImageBackground != null && colorImageBackground.getBitmap() != null && colorImageBackground.getBitmap().getWidth() == imageWidth && colorImageBackground.getBitmap().getHeight() == imageHeight)
            {
                //use existing
                colorImage.setBackgroundDrawable(colorImageBackground);
            }
            else
            {
                //update displays
                colorImage.setVisibility(View.GONE);
                colorProgress.setVisibility(View.VISIBLE);

                //create background
                GetColorBackgroundTask colorBackgroundTask = new GetColorBackgroundTask(imageWidth, imageHeight, new GetColorBackgroundTask.OnCreatedBackgroundListener()
                {
                    @Override
                    public void onCreatedBackground(final Bitmap image)
                    {
                        ((Activity)context).runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                //use created
                                colorImageBackground = new BitmapDrawable(res, image);
                                colorImage.setBackgroundDrawable(colorImageBackground);

                                //update displays
                                colorProgress.setVisibility(View.GONE);
                                colorImage.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
                colorBackgroundTask.execute(null, null, null);
            }
            colorImage.setOnTouchListener(new View.OnTouchListener()
            {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    int index;
                    int width = v.getWidth();
                    int height = v.getHeight();
                    float x = event.getX();
                    float y = event.getY();
                    float[] rgb;

                    //normalize position
                    if (x < 0)
                    {
                        x = 0;
                    }
                    else if (x > width)
                    {
                        x = width;
                    }
                    if(y < 0)
                    {
                        y = 0;
                    }
                    else if(y > height)
                    {
                        y = height;
                    }

                    //get color components
                    rgb = getRGB((int) x, (int) y, imageWidth, imageHeight);

                    //go through each component color
                    for (index = 0; index < rgb.length; index++)
                    {
                        //copy to rgb base
                        rgbBase[index] = rgb[index];
                    }
                    rgbOffset = 0;

                    //move cursor
                    colorImage.setCursor(x, y);

                    //update displays
                    updateBars(false);
                    updateTexts(false);
                    updateBrightnessBarColor(res);
                    updateCurrentColor();
                    updateCursors(false, false);

                    //if down
                    if(event.getAction() == MotionEvent.ACTION_DOWN)
                    {
                        //perform click
                        colorImage.performClick();
                    }

                    //handled
                    return (true);
                }
            });

            //setup brightness bar
            brightnessBar.setFixed(true, false);
            brightnessBar.setOnTouchListener(new View.OnTouchListener()
            {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    int height = v.getHeight();
                    int rawOffset;
                    float y = event.getY();

                    //normalize y
                    if(y < 0)
                    {
                        y = 0;
                    }
                    else if(y > height)
                    {
                        y = height;
                    }

                    //update color component offsets
                    rawOffset = yToBrightnessOffset(y);
                    rgbOffset = normalize(rawOffset - rgbTextOffset);

                    //move cursor
                    brightnessBar.setCursor(0, y);

                    //notify from user
                    brightnessBar.setTag(true);

                    //update displays
                    updateBars(false);
                    updateTexts(false);
                    updateCurrentColor();

                    //end notify from user
                    brightnessBar.setTag(null);

                    //if down
                    if(event.getAction() == MotionEvent.ACTION_DOWN)
                    {
                        //perform click
                        brightnessBar.performClick();
                    }

                    //handled
                    return (true);
                }
            });
            updateBrightnessBarColor(res, currentColor);
            updateCursors(true);

            //setup component bars and opacity
            for(index = 0; index < barDisplays.length; index++)
            {
                final int textIndex = index;
                barDisplays[index].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
                {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                    {
                        boolean fromBrightness = (brightnessBar.getTag() != null && brightnessBar.getTag().equals(true));

                        //update display
                        updateDisplay(res, textIndex, progress, true, false, fromBrightness, fromUser);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar){}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar){}
                });
            }
            setAllowOpacity(allowOpacity);
            updateBars(true);

            //setup text
            for(index = 0; index < textDisplays.length; index++)
            {
                final int barIndex = index;
                textDisplays[index].addTextChangedListener(new TextWatcher()
                {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after){}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count){}

                    @Override
                    public void afterTextChanged(Editable s)
                    {
                        boolean fromBrightness = (brightnessBar.getTag() != null && brightnessBar.getTag().equals(true));
                        boolean fromUser = (textDisplays[barIndex].getTag() == null || !textDisplays[barIndex].getTag().equals(false));
                        String text = s.toString();
                        int intValue = Integer.parseInt(!text.isEmpty() ? text : "0");
                        int textValue = (barIndex != 3 ? intValue : (int)((intValue / 100.0f) * 255));

                        //update display
                        updateDisplay(res, barIndex, textValue, false, true, fromBrightness, fromUser);
                    }
                });
            }
            updateTexts(true);

            //setup transparent
            setAllowTransparent(allowTransparent);
            transparentCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    //if not checked but still transparent
                    if(!isChecked && currentColor == Color.TRANSPARENT)
                    {
                        //default to black
                        currentColor = Color.BLACK;
                    }
                }
            });
            transparentCheck.setChecked(currentColor == Color.TRANSPARENT);

            //update before and current color
            colorBeforeView.setBackgroundColor(startColor);
            colorBefore2View.setBackgroundColor(startColor);
            colorCurrentView.setBackgroundColor(currentColor);
            colorCurrent2View.setBackgroundColor(currentColor);

            //update showing advanced displays
            setShowAdvanced(showAdvanced);

            //show dialog
            buttons = Globals.showConfirmDialog(context, icon, colorDialogView, title, null, res.getString(R.string.title_ok), res.getString(R.string.title_cancel), res.getString(R.string.title_advanced), true, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //if listener is set
                    if(colorSelectedListener != null)
                    {
                        //call listener
                        colorSelectedListener.onColorSelected(allowTransparent && transparentCheck.isChecked() ? Color.TRANSPARENT : currentColor);
                    }
                }
            }, null, null, new DialogInterface.OnDismissListener()
            {
                @Override
                public void onDismiss(DialogInterface dialog)
                {
                    //update status
                    closed = !reloading;

                    //if reloading
                    if(reloading)
                    {
                        //show again
                        show(context);
                    }

                    //reset
                    reloading = false;
                }
            });

            //get buttons
            neutralButton = buttons[1];
            negativeButton = buttons[2];

            //set button event
            neutralButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    //toggle advanced
                    setShowAdvanced(!showAdvanced);
                    neutralButton.setText(showAdvanced ? R.string.title_basic : R.string.title_advanced);
                }
            });
            neutralButton.setText(showAdvanced ? R.string.title_basic : R.string.title_advanced);
        }
    }

    //reloads display
    public void reload()
    {
        //close and show again
        reloading = true;
        negativeButton.callOnClick();
    }

    //sets the icon
    public void setIcon(Drawable icn)
    {
        icon = icn;
    }

    //sets the title
    public void setTitle(String dialogTitle)
    {
        title = dialogTitle;
    }

    //sets color
    private void setColor(int color)
    {
        startColor = currentColor = color;
    }

    //sets allowing opacity
    public void setAllowOpacity(boolean allow)
    {
        //update status
        allowOpacity = allow;

        //if display exists
        if(opacityLayout != null)
        {
            opacityLayout.setVisibility(allowOpacity ? View.VISIBLE : View.GONE);
        }
    }

    //sets allowing transparent
    public void setAllowTransparent(boolean allow)
    {
        //update status
        allowTransparent = allow;

        //if display exists
        if(transparentCheck != null)
        {
            transparentCheck.setVisibility(allowTransparent ? View.VISIBLE : View.GONE);
        }
    }

    //sets showing of advanced displays
    private void setShowAdvanced(boolean show)
    {
        int basicVisibility = (show ? View.GONE : View.VISIBLE);
        int advancedVisibility = (show ? View.VISIBLE : View.GONE);

        //update status
        showAdvanced = show;

        //update displays
        if(colorTableLayout != null)
        {
            colorTableLayout.setVisibility(basicVisibility);
        }
        if(colorImageLayout != null)
        {
            colorImageLayout.setVisibility(advancedVisibility);
        }
        if(colorBeforeText != null)
        {
            colorBeforeText.setVisibility(advancedVisibility);
        }
        if(colorBeforeView != null)
        {
            colorBeforeView.setVisibility(advancedVisibility);
        }
        if(colorCurrentText != null)
        {
            colorCurrentText.setVisibility(advancedVisibility);
        }
        if(colorCurrentView != null)
        {
            colorCurrentView.setVisibility(advancedVisibility);
        }
        if(redLayout != null)
        {
            redLayout.setVisibility(advancedVisibility);
        }
        if(greenLayout != null)
        {
            greenLayout.setVisibility(advancedVisibility);
        }
        if(blueLayout != null)
        {
            blueLayout.setVisibility(advancedVisibility);
        }
        if(opacityLayout != null)
        {
            opacityLayout.setVisibility(!allowOpacity ? View.GONE : advancedVisibility);
        }
    }

    //sets on color selected listener
    public void setOnColorSelectedListener(OnColorSelectedListener listener)
    {
        colorSelectedListener = listener;
    }

    //updates brightness bar color
    private void updateBrightnessBarColor(Resources res, int color)
    {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        int nonAlphaColor = Color.rgb(red, green, blue);

        //if color is changing
        if(nonAlphaColor != lastColor)
        {
            //update brightness bar with color
            brightnessBar.setBackgroundDrawable(createBrightnessImageBg(res, brightWidth, brightHeight, nonAlphaColor));

            //update last color
            lastColor = color;
        }
    }
    private void updateBrightnessBarColor(Resources res)
    {
        //update with current base
        updateBrightnessBarColor(res, Color.rgb(normalizePositive(rgbBase[0]), normalizePositive(rgbBase[1]), normalizePositive((rgbBase[2]))));
    }

    //converts brightness y to rgb offset
    private int yToBrightnessOffset(float y)
    {
        return((int)((((brightHeight / 2) - y) / (float)brightHeight) * 255 * 2));
    }

    //updates component bar display
    private void updateBar(int index, int value)
    {
        //if value is changing
        if(barDisplays[index].getProgress() != value)
        {
            //set progress
            barDisplays[index].setProgress(value);
        }
    }

    //updates component bar displays
    private void updateBars(boolean updateAlpha)
    {
        int index;

        //go through each display (possibly not using opacity)
        for(index = 0; index < barDisplays.length - (updateAlpha ? 0 : 1); index++)
        {
            //update component bar
            updateBar(index, addOffset(rgbBase[index]));
        }
    }

    //updates text display
    private void updateText(int index, int value)
    {
        String valueString = String.valueOf(index != 3 ? value : (int)Math.ceil((value / 255.0f) * 100));

        //if value is changing
        if(valueString.length() > 0 && !textDisplays[index].getText().toString().equals(valueString))
        {
            //set text and move cursor to the end
            textDisplays[index].setTag(false);
            textDisplays[index].setText(valueString);
            try
            {
                textDisplays[index].setSelection(valueString.length());
            }
            catch(Exception ex)
            {
                //do nothing
            }
            textDisplays[index].setTag(null);
        }
    }

    //updates text displays
    private void updateTexts(boolean updateAlpha)
    {
        int index;

        //go through each display (possibly not using opacity)
        for(index = 0; index < textDisplays.length - (updateAlpha ? 0 : 1); index++)
        {
            //update text
            updateText(index, addOffset(rgbBase[index]));
        }
    }

    //update current color
    private void updateCurrentColor()
    {
        int newColor = Color.argb((int)rgbBase[3], addOffset(rgbBase[0]), addOffset(rgbBase[1]), addOffset(rgbBase[2]));

        //if color is changing
        if(currentColor != newColor)
        {
            //update current color
            currentColor = newColor;
            colorCurrentView.setBackgroundColor(currentColor);
            colorCurrent2View.setBackgroundColor(currentColor);
            transparentCheck.setChecked(false);
        }
    }

    //updates cursors
    @SuppressWarnings("SpellCheckingInspection")
    private void updateCursors(boolean force, boolean updateImage)
    {
        int[] coords = getCoordinates(Color.rgb((int) rgbBase[0], (int) rgbBase[1], (int) rgbBase[2]), imageWidth, imageHeight);
        int y = (int)((brightHeight / 2f) - ((coords[2] / 255f / 2f) * brightHeight));

        //if updating image and position is changing
        if(updateImage && (force || (colorImage.getCursorX() != coords[0] || colorImage.getCursorY() != coords[1])))
        {
            //update cursor
            colorImage.setCursor(coords[0], coords[1]);
        }

        //if forcing or it is changing
        if(force || (brightnessBar.getCursorY() != y))
        {
            //update cursor
            brightnessBar.setCursor(0, y);
        }
    }
    private void updateCursors(boolean force)
    {
        updateCursors(force, true);
    }

    //updates values and displays
    private void updateDisplay(Resources res, int displayIndex, int value, boolean updateText, boolean updateBar, boolean fromBrightness, boolean fromUser)
    {
        int index;

        //if no from updating brightness
        if(!fromBrightness)
        {
            //if user modified directly
            if(fromUser)
            {
                //update rgb base and offset
                rgbBase[displayIndex] = value;

                //if not from opacity
                if(displayIndex != 3)
                {
                    //go through each component (not using opacity)
                    for(index = 0; index < rgbBase.length - 1; index++)
                    {
                        //if not the updated component
                        if(index != displayIndex)
                        {
                            //update base with offset
                            rgbBase[index] += rgbOffset;
                        }
                    }

                    //go through each component (not using opacity)
                    for(index = 0; index < rgbBase.length - 1; index++)
                    {
                        //make sure component is normalized
                        rgbBase[index] = normalize(rgbBase[index]);
                    }

                    //reset offset
                    rgbOffset = 0;
                }
                else
                {
                    //make sure opacity is normalized
                    rgbBase[3] = normalize(rgbBase[3]);
                }
            }

            //update brightness text offset
            rgbTextOffset = calculateOffset();
        }

        //if user modified directly
        if(fromUser)
        {
            //update displays
            if(updateText)
            {
                updateText(displayIndex, value);
            }
            if(updateBar)
            {
                updateBar(displayIndex, value);
            }
            updateCurrentColor();
            updateBrightnessBarColor(res);
            updateCursors(false);
        }
    }

    //normalizes a component value
    private int normalize(float component)
    {
        if(component < -255)
        {
            return(-255);
        }
        else if(component > 255)
        {
            return(255);
        }

        return((int)component);
    }

    //normalizes a component value to be positive
    private int normalizePositive(float component)
    {
        if(component < 0)
        {
            return(0);
        }
        else if(component > 255)
        {
            return(255);
        }

        return((int)component);
    }

    //adds offset to component
    private int addOffset(float component)
    {
        return(normalizePositive(component + rgbOffset));
    }

    //gets extreme (min or max) value in components
    @SuppressWarnings("SameParameterValue")
    private float getExtremeRGB(boolean max)
    {
        int index;
        float value = (max ? 0 : 255);

        //go through each component (not using opacity)
        for(index = 0; index < rgbBase.length - 1; index++)
        {
            //if -looking for max and largest so far- or -looking for min and smallest so far-
            if((max && rgbBase[index] > value) || (!max && rgbBase[index] < value))
            {
                value = rgbBase[index];
            }
        }

        return(value);
    }

    //calculates brightness offset (smallest in components)
    private int calculateOffset()
    {
        return((int)getExtremeRGB(false));
    }

    //gets x, y, and brightness coordinates for given color
    private int[] getCoordinates(int color, int width, int height)
    {
        int index;
        int delta;
        int section;
        int subIndex;
        int largestIndex = 0;
        int largestValue = 0;
        int[] rgb = new int[]{Color.red(color), Color.green(color), Color.blue(color)};
        int[] coordinates = new int[]{0, 0, 255};
        float widthDivide = width / 6f;

        //set brightness offset
        coordinates[2] = calculateOffset();

        //go through each color component
        for(index = 0; index < rgb.length; index++)
        {
            //subtract brightness from color component
            rgb[index] -= coordinates[2];

            //if the largest component so far
            if(rgb[index] > largestValue)
            {
                //remember largest index and value
                largestIndex = index;
                largestValue = rgb[index];
            }
        }

        //get delta from highest component value
        delta = 255 - largestValue;

        //get section and sub index
        switch(largestIndex)
        {
            //mostly red
            case 0:
                //if has green, 0, else has blue, 5
                section = (rgb[1] > 0 ? 0 : 5);
                subIndex = (rgb[1] > 0 ? 1 : 2);
                break;

            //mostly green
            case 1:
                //if has red, 1, else has blue, 2
                section = (rgb[0] > 0 ? 1 : 2);
                subIndex = (rgb[0] > 0 ? 0 : 2);
                break;

            //mostly blue
            default:
            case 2:
                //if has green, 3, else has red, 4
                section = (rgb[1] > 0 ? 3 : 4);
                subIndex = (rgb[1] > 0 ? 1 : 0);
                break;
        }

        //get x, y
        rgb[subIndex] += delta;
        if(section % 2 != 0)
        {
            rgb[subIndex] = 255 - rgb[subIndex];
        }
        coordinates[0] = (int)(((rgb[subIndex] / 255f) * widthDivide) + (widthDivide * section));
        coordinates[1] = (int)((delta / 255f) * height);

        return(coordinates);
    }

    //gets color components
    private static float[] getRGB(int x, int width)        //note: does not apply darkness
    {
        float red = 0;
        float green = 0;
        float blue = 0;
        float widthDivide = (float)(width / 6);

        //red to yellow
        if(x < widthDivide * 1)
        {
            red = 255;
            green = ((x - (widthDivide * 0)) / widthDivide) * 255;
            if(green > 255)
            {
                green = 255;
            }
        }
        //yellow to green
        else if(x < widthDivide * 2)
        {
            green = 255;
            red = 255 - (((x - (widthDivide * 1)) / widthDivide) * 255);
            if(red < 0)
            {
                red = 0;
            }
        }
        //green to cyan
        else if(x < widthDivide * 3)
        {
            green = 255;
            blue = ((x - (widthDivide * 2)) / widthDivide) * 255;
            if(blue > 255)
            {
                blue = 255;
            }
        }
        //cyan to blue
        else if(x < widthDivide * 4)
        {
            blue = 255;
            green = 255 - (((x - (widthDivide * 3)) / widthDivide) * 255);
            if(green < 0)
            {
                green = 0;
            }
        }
        //blue to purple
        else if(x < widthDivide * 5)
        {
            blue = 255;
            red = ((x - (widthDivide * 4)) / widthDivide) * 255;
            if(red > 255)
            {
                red = 255;
            }
        }
        //purple to red
        else
        {
            red = 255;
            blue = 255 - (((x - (widthDivide * 5)) / widthDivide) * 255);
            if(blue < 0)
            {
                blue = 0;
            }
        }

        //return color components
        return(new float[]{red, green, blue});
    }
    private static float[] getRGB(int x, int y, int width, int height)
    {
        int index;
        float[] rgb = getRGB(x, width);

        //go through each color component
        for(index = 0; index < rgb.length; index++)
        {
            //apply darkness
            rgb[index] -= ((255f / height) * y);
            if(rgb[index] < 0)
            {
                rgb[index] = 0;
            }
        }

        //return color components
        return(rgb);
    }

    //creates the background color selection image
    private static Bitmap createColorImageBg(int width, int height)
    {
        int x;
        int y;
        int index;
        float heightScale = 255f / height;
        float[] rgb;
        Paint colorPaint = new Paint();
        Bitmap imageBg = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvasBg = new Canvas(imageBg);

        colorPaint.setStyle(Paint.Style.STROKE);

        //go through each color
        for(x = 0; x < width; x++)
        {
            //get red, green, and blue
            rgb = getRGB(x, width);

            //go through color brightness to darkness
            for(y = 0; y < height; y++)
            {
                //draw point
                colorPaint.setColor(Color.rgb((int) rgb[0], (int)rgb[1], (int)rgb[2]));
                canvasBg.drawPoint(x, y, colorPaint);

                //go through each color component
                for(index = 0; index < rgb.length; index++)
                {
                    //remove more of color component
                    rgb[index] -= heightScale;
                    if(rgb[index] < 0)
                    {
                        rgb[index] = 0;
                    }
                }
            }
        }

        return(imageBg);
    }

    //creates the brightness image
    private static BitmapDrawable createBrightnessImageBg(Resources res, int width, int height, int color)
    {
        Paint fadePaint = new Paint();
        Bitmap imageBg = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvasBg = new Canvas(imageBg);
        LinearGradient colorFade = new LinearGradient(0, 0, 0, height, new int[]{Color.WHITE, color, Color.BLACK}, null, Shader.TileMode.CLAMP);

        fadePaint.setShader(colorFade);
        canvasBg.drawRect(0, 0, width, height, fadePaint);

        return(new BitmapDrawable(res, imageBg));
    }
}
