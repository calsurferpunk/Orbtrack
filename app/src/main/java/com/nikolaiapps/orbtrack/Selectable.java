package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public abstract class Selectable
{
    public static abstract class ParamTypes
    {
        static final String Group = "group";
        static final String PageNumber = "page";
        static final String SubPageNumber = "subPage";
        static final String Id = "id";
        static final String ListIndex = "listIndex";
        static final String CanEdit = "canEdit";
        static final String IsSelected = "isSelected";
        static final String CanCheck = "canCheck";
        static final String IsChecked = "isChecked";
    }

    //Select list item
    protected static class ListItem implements Parcelable
    {
        public int id;
        public int listIndex;
        public boolean canEdit;
        public boolean isSelected;
        public boolean canCheck;
        public boolean isChecked;
        public CheckBox checkBoxView;
        public static final Creator<ListItem> CREATOR =  new Parcelable.Creator<ListItem>()
        {
            @Override
            public ListItem createFromParcel(Parcel source)
            {
                Bundle bundle = source.readBundle(getClass().getClassLoader());
                if(bundle == null)
                {
                    bundle = new Bundle();
                }

                return(new ListItem(bundle.getInt(ParamTypes.Id, Integer.MAX_VALUE), bundle.getInt(ParamTypes.ListIndex, -1), bundle.getBoolean(ParamTypes.CanEdit, false), bundle.getBoolean(ParamTypes.IsSelected, false), bundle.getBoolean(ParamTypes.CanCheck, false),  bundle.getBoolean(ParamTypes.IsChecked, false)));
            }

            @Override
            public ListItem[] newArray(int size)
            {
                return(new ListItem[size]);
            }
        };

        public ListItem(int idNum, int index, boolean canEd, boolean isSel, boolean canCh, boolean isCh)
        {
            id = idNum;
            listIndex = index;
            canEdit = canEd;
            isSelected = isSel;
            canCheck = canCh;
            isChecked = isCh;
        }

        @Override
        public int describeContents()
        {
            return(0);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            Bundle bundle = new Bundle();

            bundle.putInt(ParamTypes.Id, id);
            bundle.putInt(ParamTypes.ListIndex, listIndex);
            bundle.putBoolean(ParamTypes.CanEdit, canEdit);
            bundle.putBoolean(ParamTypes.IsSelected, isSelected);
            bundle.putBoolean(ParamTypes.CanCheck, canCheck);
            bundle.putBoolean(ParamTypes.IsChecked, isChecked);

            dest.writeBundle(bundle);
        }

        public void setChecked(boolean checked)
        {
            isChecked = checked;

            if(checkBoxView != null)
            {
                if(checkBoxView.getVisibility() != View.VISIBLE)
                {
                    checkBoxView.setVisibility(View.VISIBLE);
                    checkBoxView.setClickable(false);
                    checkBoxView.setFocusable(false);
                }
                checkBoxView.setChecked(isChecked);
            }
        }
    }

    //Select list view holder
    protected static class ListItemHolder extends RecyclerView.ViewHolder
    {
        public CheckBox checkBoxView;

        public ListItemHolder(View itemView, int checkBoxID)
        {
            super(itemView);
            if(checkBoxID != -1)
            {
                checkBoxView = itemView.findViewById(checkBoxID);
            }
        }
    }

    public static abstract class ListBaseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    {
        static final int EXTENDED_COLUMN_1_WIDTH_DP = 510;
        static final int EXTENDED_COLUMN_1_SHORT_WIDTH_DP = 450;
        static final int EXTENDED_COLUMN_2_SHORT_WIDTH_DP = 500;

        public interface OnItemClickListener
        {
            void onItemClicked(View view, int position);
        }
        private interface OnItemLongClickListener
        {
            void onItemLongClicked(View view, int position);
        }
        public interface OnItemDetailButtonClickListener
        {
            void onClick(int pageType, int itemID, ListItem item, int buttonNum);
        }

        //Detail button type
        public static abstract class DetailButtonType
        {
            static final int LensView = 0;
            static final int MapView = 1;
            static final int GlobeView = 2;
            static final int Graph = 3;
            static final int Notify = 4;
            static final int Preview3d = 5;
            static final int Info = 6;
        }

        //Item detail dialog
        public class ItemDetailDialog
        {
            private boolean canShow;
            private final int noradId;
            private final long timerDelay;
            private final Context currentContext;
            private ViewGroup itemDetailsGroup;
            private AlertDialog dialog;
            private final Timer updateTimer = new Timer();
            private Whirly.PreviewFragment itemDetail3dView = null;
            private OnItemDetailButtonClickListener itemDetailButtonClickListener;
            private final ArrayList<TextView> detailTexts = new ArrayList<>();
            private final ArrayList<TextView> detailTitles = new ArrayList<>();
            private final ArrayList<AppCompatImageButton> detailButtons = new ArrayList<>();
            private ArrayList<DialogInterface.OnDismissListener> dismissListeners;

            public ItemDetailDialog(Context context, LayoutInflater inflater, int id, String title, Drawable icon, OnItemDetailButtonClickListener listener)
            {
                final TableLayout itemDetailTable;
                final FrameLayout itemDetail3dFrame;
                final FrameLayout itemDetail3dCloseFrame;
                final LinearLayout itemDetailButtonLayout;
                final FloatingActionButton itemDetail3dCloseButton;
                final FloatingActionButton itemDetail3dFullscreenButton;
                final FragmentManager fm;
                final int[] screenSize = Globals.getDevicePixels(context);
                AlertDialog.Builder itemDetailDialog;

                canShow = true;
                currentContext = context;
                noradId = id;
                timerDelay = Settings.getPreferences(currentContext).getInt(Settings.PreferenceName.ListUpdateDelay, 1000);

                try
                {
                    itemDetailsGroup = (ViewGroup)inflater.inflate(R.layout.item_detail_dialog, null);
                }
                catch(Exception ex)
                {
                    canShow = false;
                    return;
                }

                itemDetailTable = itemDetailsGroup.findViewById(R.id.Item_Detail_Table);
                itemDetail3dFrame = itemDetailsGroup.findViewById(R.id.Item_Detail_3d_Frame);
                itemDetail3dCloseFrame = itemDetailsGroup.findViewById(R.id.Item_Detail_3d_Close_Frame);
                itemDetailButtonLayout = itemDetailsGroup.findViewById(R.id.Item_Detail_Button_Layout);
                itemDetail3dCloseButton = itemDetailsGroup.findViewById(R.id.Item_Detail_3d_Close_Button);
                itemDetail3dFullscreenButton = itemDetailsGroup.findViewById(R.id.Item_Detail_3d_Fullscreen_Button);
                itemDetailDialog = new AlertDialog.Builder(currentContext, Globals.getDialogThemeID(currentContext));
                fm = (currentContext instanceof FragmentActivity ? ((FragmentActivity)currentContext).getSupportFragmentManager() : null);
                itemDetailButtonClickListener = listener;
                dismissListeners = new ArrayList<>(0);

                //setup and show dialog
                if(title != null)
                {
                    itemDetailDialog.setIcon(icon);
                    itemDetailDialog.setTitle(title);
                }
                itemDetailDialog.setView(itemDetailsGroup);
                itemDetailDialog.setOnDismissListener(new DialogInterface.OnDismissListener()
                {
                    @Override
                    public void onDismiss(DialogInterface dialog)
                    {
                        for(DialogInterface.OnDismissListener currentListener : dismissListeners)
                        {
                            currentListener.onDismiss(dialog);
                        }
                    }
                });
                itemDetail3dCloseButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        dialog.dismiss();
                    }
                });
                itemDetail3dFullscreenButton.setOnClickListener(new View.OnClickListener()
                {
                    private int startFrameHeight = 0;
                    private int expandFrameHeight = 0;

                    @Override
                    public void onClick(View v)
                    {
                        boolean showingDetails = (itemDetailTable.getVisibility() == View.GONE);
                        int detailsVisibility = (showingDetails ? View.VISIBLE : View.GONE);
                        ViewGroup.LayoutParams frameParams = itemDetail3dFrame.getLayoutParams();

                        //update button
                        itemDetail3dFullscreenButton.setImageDrawable(Globals.getDrawable(currentContext, (showingDetails ? R.drawable.ic_fullscreen_white : R.drawable.ic_fullscreen_exit_white)));

                        //update size
                        if(startFrameHeight == 0)
                        {
                            ///get sizes
                            startFrameHeight = itemDetail3dFrame.getHeight();
                            expandFrameHeight = (screenSize[1] - (itemDetailButtonLayout.getHeight() * 2)); //note: since unknown how to get title height, button layout used instead in addition (thus 2x)
                        }
                        frameParams.height = (showingDetails ? startFrameHeight : expandFrameHeight);
                        itemDetail3dFrame.setLayoutParams(frameParams);

                        //update visibility
                        itemDetail3dCloseFrame.setVisibility(showingDetails ? View.GONE : View.VISIBLE);
                        itemDetailTable.setVisibility(detailsVisibility);
                        itemDetailButtonLayout.setVisibility(detailsVisibility);
                    }
                });
                itemDetail3dCloseFrame.setVisibility(View.GONE);
                dialog = itemDetailDialog.create();

                addOnDismissListener(new DialogInterface.OnDismissListener()
                {
                    @Override
                    public void onDismiss(DialogInterface dialog)
                    {
                        //cancel any getting of information
                        UpdateService.cancel(UpdateService.UpdateType.GetInformation);

                        //if fragment manager and item detail 3d preview are set
                        if(fm != null && itemDetail3dView != null)
                        {
                            //remove it
                            fm.beginTransaction().remove(itemDetail3dView).commit();
                        }
                    }
                });
                addOnDismissListener(new DialogInterface.OnDismissListener()
                {
                    @Override
                    public void onDismiss(DialogInterface dialog)
                    {
                        updateTimer.cancel();
                        updateTimer.purge();
                    }
                });

                //if fragment manager is set
                if(fm != null)
                {
                    //set item detail 3d preview
                    itemDetail3dView = (Whirly.PreviewFragment)fm.findFragmentByTag("itemDetail3dView");
                }
            }

            private void addDivider(ViewGroup view, int index, boolean vertical)
            {
                TableRow.LayoutParams params;
                float[] dpPixels;

                TextView emptyText = new TextView(new ContextThemeWrapper(currentContext, vertical ? R.style.DetailVerticalDivider : R.style.Divider));
                if(vertical)
                {
                    dpPixels = Globals.dpsToPixels(currentContext, 2, 5);
                    params = new TableRow.LayoutParams();
                    params.width = (int)dpPixels[0];
                    params.leftMargin = params.rightMargin = (int)dpPixels[1];
                    emptyText.setLayoutParams(params);
                }
                else
                {
                    emptyText.setTextSize(1);
                }
                if(index >= 0)
                {
                    view.addView(emptyText, index);
                }
                else
                {
                    view.addView(emptyText);
                }
            }
            private void addDivider(ViewGroup view, boolean vertical)
            {
                addDivider(view, -1, vertical);
            }

            public void moveProgress()
            {
                if(canShow)
                {
                    TableLayout itemDetailTable = itemDetailsGroup.findViewById(R.id.Item_Detail_Table);
                    final View passProgressLayout = this.findViewById(R.id.Item_Detail_Progress_Layout);

                    addDivider(itemDetailTable, false);
                    ((ViewGroup)passProgressLayout.getParent()).removeView(passProgressLayout);
                    itemDetailTable.addView(passProgressLayout);
                }
            }

            public void addGroup(int groupId, int ...titleIds)
            {
                int row = 0;
                int column = 0;
                int titleIndex;
                int titleCount = titleIds.length;
                boolean addedRow = false;
                String text;
                Resources res = currentContext.getResources();

                if(canShow)
                {
                    TableLayout itemDetailTable = itemDetailsGroup.findViewById(R.id.Item_Detail_Table);
                    TableRow groupRow = new TableRow(new ContextThemeWrapper(currentContext, R.style.DetailTableRow));
                    TextView groupText = new TextView(new ContextThemeWrapper(currentContext, R.style.DetailText));
                    TableRow.LayoutParams detailParams;
                    TableRow.LayoutParams groupParams = new TableRow.LayoutParams();
                    TableRow[] detailRows = new TableRow[(titleCount / 2) + (titleCount % 2)];

                    //if using group
                    if(groupId != R.string.empty)
                    {
                        //add divider
                        addDivider(itemDetailTable, false);

                        //setup and add group text
                        groupParams.span = 5;
                        groupParams.gravity = Gravity.CENTER;
                        groupText.setLayoutParams(groupParams);
                        groupText.setText(groupId);
                        groupRow.setBackgroundColor(Globals.resolveColorID(currentContext, R.attr.viewPagerBackground));
                        groupRow.addView(groupText);
                        itemDetailTable.addView(groupRow);

                        //add each title
                        for(titleIndex = 0; titleIndex < titleCount; titleIndex++)
                        {
                            int currentTitleId = titleIds[titleIndex];
                            int previousTitleId = (titleIndex > 0 ? titleIds[titleIndex - 1] : R.string.empty);
                            int nextTitleId = (titleIndex + 1 < titleCount ? titleIds[titleIndex + 1] : R.string.empty);
                            boolean usingCurrent = (currentTitleId != R.string.empty);
                            boolean extendColumn = false;
                            TextView currentTitleText = new TextView(new ContextThemeWrapper(currentContext, R.style.DetailTitle));
                            TextView currentDetailText = new TextView(new ContextThemeWrapper(currentContext, R.style.DetailText));

                            //if need to start a new row
                            if(column == 0)
                            {
                                //if not an empty row
                                if(usingCurrent || nextTitleId != R.string.empty)
                                {
                                    //add divider
                                    addDivider(itemDetailTable, false);
                                }

                                //create row
                                detailRows[row] = new TableRow(new ContextThemeWrapper(currentContext, R.style.DetailTableRow));
                            }

                            //set title and text
                            text = res.getString(currentTitleId) + ":";
                            currentTitleText.setText(text);
                            currentTitleText.setVisibility(usingCurrent ? View.VISIBLE : View.GONE);
                            currentDetailText.setText("-");
                            currentDetailText.setVisibility(usingCurrent ? View.VISIBLE : View.GONE);

                            //add current title and text
                            detailRows[row].addView(currentTitleText);
                            detailRows[row].addView(currentDetailText);

                            //if done with row or on last title
                            if(++column >= 2 || titleIndex + 1 >= titleCount)
                            {
                                //update row and column
                                if(column < 2)
                                {
                                    extendColumn = true;
                                }
                                detailRows[row].setVisibility(previousTitleId != R.string.empty || usingCurrent ? View.VISIBLE : View.GONE);
                                addedRow = addedRow || (detailRows[row].getVisibility() == View.VISIBLE);
                                itemDetailTable.addView(detailRows[row++]);
                                column = 0;
                            }
                            else
                            {
                                //add divider and update column
                                if(nextTitleId != R.string.empty)
                                {
                                    addDivider(detailRows[row], true);
                                }
                                else
                                {
                                    extendColumn = true;
                                }
                                column++;
                            }

                            //if need to extend column
                            if(extendColumn)
                            {
                                detailParams = (TableRow.LayoutParams)currentDetailText.getLayoutParams();
                                detailParams.span = 4;
                                currentDetailText.setLayoutParams(detailParams);
                            }

                            //add detail title and text
                            this.detailTitles.add(currentTitleText);
                            this.detailTexts.add(currentDetailText);
                        }

                        //update group title visibility
                        groupRow.setVisibility(addedRow ? View.VISIBLE : View.GONE);
                    }
                }
            }

            public void addOnDismissListener(DialogInterface.OnDismissListener listener)
            {
                dismissListeners.add(listener);
            }

            public void setUpdateTask(TimerTask task)
            {
                updateTimer.schedule(task, 0, timerDelay);
            }

            //Gets a detail button drawable
            private Drawable getDetailButtonDrawable(Context context, int infoButtonType)
            {
                int imageId;

                switch(infoButtonType)
                {
                    case DetailButtonType.LensView:
                        imageId = R.drawable.ic_photo_camera_white;
                        break;

                    case DetailButtonType.MapView:
                        imageId = R.drawable.ic_map_white;
                        break;

                    case DetailButtonType.GlobeView:
                        imageId = R.drawable.ic_globe_white;
                        break;

                    case DetailButtonType.Graph:
                        imageId = R.drawable.ic_timeline_black;
                        break;

                    case DetailButtonType.Notify:
                        imageId = R.drawable.ic_notifications_white;
                        break;

                    case DetailButtonType.Preview3d:
                        imageId = R.drawable.ic_3d_rotation_black;
                        break;

                    default:
                    case DetailButtonType.Info:
                        imageId = R.drawable.ic_info_black;
                        break;
                }

                return(Globals.getDrawable(context, imageId, true));
            }

            public void setupItemDetailButton(AppCompatImageButton button, final ListBaseAdapter listAdapter, final int pageType, final int itemID, final ListItem item, final int buttonNum)
            {
                int mapLayerType = ListBaseAdapter.getMapLayerType(noradId);

                //if view is set, for DetailButtonType.Preview3d, and map layer type is set
                if(itemDetail3dView != null && buttonNum == DetailButtonType.Preview3d && mapLayerType != CoordinatesFragment.MapLayerType.None)
                {
                    //set map layer type
                    itemDetail3dView.setMapLayerType(mapLayerType);
                }

                //set image and listener
                button.setBackgroundDrawable(getDetailButtonDrawable(currentContext, buttonNum));
                button.setOnClickListener(new View.OnClickListener()
                {
                    private void setPreviewVisible(int visibility)
                    {
                        if(itemDetail3dView != null && currentContext instanceof FragmentActivity)
                        {
                            FragmentManager fm = ((FragmentActivity)currentContext).getSupportFragmentManager();

                            if(visibility == View.VISIBLE)
                            {
                                fm.beginTransaction().show(itemDetail3dView).commit();
                            }
                            else
                            {
                                fm.beginTransaction().hide(itemDetail3dView).commit();
                            }
                        }
                    }

                    private int toggleVisible(int buttonNum)
                    {
                        int viewId;
                        int layoutViewId;
                        int progressViewId;
                        int oldVisible;
                        int newVisible;
                        LinearLayout layoutGroup;

                        if(!canShow)
                        {
                            return(View.GONE);
                        }

                        //get view IDs
                        switch(buttonNum)
                        {
                            case DetailButtonType.Graph:
                                viewId = R.id.Item_Detail_Graph;
                                layoutViewId = R.id.Item_Detail_Graph_Layout;
                                progressViewId = R.id.Item_Detail_Graph_Progress;
                                break;

                            case DetailButtonType.Preview3d:
                                viewId = R.id.Item_Detail_3d_View;
                                layoutViewId = R.id.Item_Detail_3d_Layout;
                                progressViewId = R.id.Item_Detail_3d_Progress;
                                break;

                            default:
                                viewId = R.id.Item_Detail_Info_Text;
                                layoutViewId = R.id.Item_Detail_Info_Layout;
                                progressViewId = R.id.Item_Detail_Info_Progress;
                                break;
                        }

                        //get layout group
                        layoutGroup = itemDetailsGroup.findViewById(layoutViewId);

                        oldVisible = layoutGroup.getVisibility();
                        newVisible = (oldVisible == View.VISIBLE ? View.GONE : View.VISIBLE);

                        //toggle visibility
                        layoutGroup.setVisibility(newVisible);
                        layoutGroup.findViewById(progressViewId).setVisibility((buttonNum == DetailButtonType.Info && oldVisible == View.GONE) ? View.VISIBLE : oldVisible);        //always show R.id.Item_Detail_Info_Progress if starting to show information display
                        if(buttonNum == DetailButtonType.Preview3d)
                        {
                            setPreviewVisible(newVisible);
                        }
                        else
                        {
                            layoutGroup.findViewById(viewId).setVisibility(oldVisible);
                        }

                        //return new visibility
                        return(newVisible);
                    }

                    @Override
                    public void onClick(View v)
                    {
                        boolean isGraph = (buttonNum == DetailButtonType.Graph);
                        boolean isPreview3d = (buttonNum == DetailButtonType.Preview3d);
                        boolean isInfo = (buttonNum == DetailButtonType.Info);
                        int newVisible = View.VISIBLE;
                        int layoutViewId;

                        if(canShow)
                        {
                            LinearLayout graphGroup = (itemDetailsGroup != null ? (LinearLayout)itemDetailsGroup.findViewById(R.id.Item_Detail_Graph_Layout) : null);
                            LinearLayout preview3dGroup = (itemDetailsGroup != null ? (LinearLayout)itemDetailsGroup.findViewById(R.id.Item_Detail_3d_Layout) : null);
                            LinearLayout infoGroup = (itemDetailsGroup != null ? (LinearLayout)itemDetailsGroup.findViewById(R.id.Item_Detail_Info_Layout) : null);
                            int[] buttons = new int[]{DetailButtonType.Info, DetailButtonType.Preview3d, DetailButtonType.Graph};

                            //if not graph, preview 3d, or info
                            if(!isGraph && !isPreview3d && !isInfo)
                            {
                                //close dialog
                                dialog.dismiss();
                            }

                            //if listener is set
                            if(itemDetailButtonClickListener != null)
                            {
                                //if graph
                                if(isGraph)
                                {
                                    //set graph
                                    listAdapter.setGraphGroup(graphGroup);
                                }
                                //else if preview 3d
                                else if(isPreview3d)
                                {
                                    //set preview 3d
                                    listAdapter.setPreview3dGroup(preview3dGroup);
                                }
                                //else if information
                                else if(isInfo)
                                {
                                    //set info
                                    listAdapter.setInformationGroup(infoGroup);
                                }

                                //hide any other visible
                                for(int currentButton : buttons)
                                {
                                    LinearLayout currentLayout;

                                    //get layout view ID
                                    switch(currentButton)
                                    {
                                        case DetailButtonType.Graph:
                                            layoutViewId = R.id.Item_Detail_Graph_Layout;
                                            break;

                                        case DetailButtonType.Preview3d:
                                            layoutViewId = R.id.Item_Detail_3d_Layout;
                                            break;

                                        default:
                                            layoutViewId = R.id.Item_Detail_Info_Layout;
                                            break;
                                    }

                                    //get current layout
                                    currentLayout = (itemDetailsGroup != null ? (LinearLayout)itemDetailsGroup.findViewById(layoutViewId) : null);

                                    //if on another button and layout exists
                                    if(currentButton != buttonNum && currentLayout != null && currentLayout.getVisibility() == View.VISIBLE)
                                    {
                                        //toggle visibility
                                        toggleVisible(currentButton);
                                    }
                                }
                                if(itemDetailsGroup != null)
                                {
                                    newVisible = toggleVisible(buttonNum);
                                    itemDetailsGroup.postDelayed(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            itemDetailsGroup.scrollTo(0, itemDetailsGroup.getBottom());
                                        }
                                    }, 200);
                                }

                                //if still visible
                                if(newVisible == View.VISIBLE)
                                {
                                    //send event
                                    itemDetailButtonClickListener.onClick(pageType, itemID, item, buttonNum);
                                }
                            }
                        }
                    }
                });
            }

            public AppCompatImageButton addButton(final int pageType, final int itemID, final ListItem item, final int buttonNum)
            {
                AppCompatImageButton button = new AppCompatImageButton(new ContextThemeWrapper(currentContext, R.style.DetailButton));

                if(canShow)
                {
                    LinearLayout buttonLayout = itemDetailsGroup.findViewById(R.id.Item_Detail_Button_Layout);
                    LinearLayout detailLayout = itemDetailsGroup.findViewById(R.id.Item_Detail_Layout);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                    if(this.detailButtons.size() == 0)
                    {
                        addDivider(detailLayout, detailLayout.indexOfChild(buttonLayout), false);
                    }

                    setupItemDetailButton(button, ListBaseAdapter.this, pageType, itemID, item, buttonNum);

                    params.leftMargin = params.rightMargin = params.topMargin = params.bottomMargin = (int)Globals.dpToPixels(currentContext, 8);
                    button.setLayoutParams(params);
                    buttonLayout.setVisibility(View.VISIBLE);
                    buttonLayout.addView(button);
                    this.detailButtons.add(button);
                }

                return(button);
            }

            //Hides vertical item detail divider at given offset
            public void hideVerticalItemDetailDivider(int offset)
            {
                if(canShow)
                {
                    View verticalDivider = itemDetailsGroup.findViewWithTag("vd" + offset);

                    if(verticalDivider != null)
                    {
                        verticalDivider.setVisibility(View.GONE);
                    }
                }
            }

            //Shows item detail row up to offset if non skip index or ignoring skip
            public void showItemDetailRows(int offset, int skipIndex, boolean ignoreSkip)
            {
                int index;
                boolean setAny = false;

                if(!canShow)
                {
                    return;
                }

                //go through unused displays
                for(index = 0; index < (offset + 1) / 2; index++)
                {
                    View currentView = itemDetailsGroup.findViewWithTag(String.valueOf(index));
                    View horizontalDivider = itemDetailsGroup.findViewWithTag("hd" + index);

                    //set row to visible if exists and want to show
                    if(currentView != null && (index != skipIndex || ignoreSkip))
                    {
                        //show it
                        currentView.setVisibility(View.VISIBLE);

                        //if divider exists
                        if(horizontalDivider != null)
                        {
                            //show it
                            horizontalDivider.setVisibility(View.VISIBLE);
                        }

                        //update status
                        setAny = true;
                    }
                }

                //if set any
                if(setAny)
                {
                    View buttonDivider = itemDetailsGroup.findViewById(R.id.Item_Detail_Button_Divider);

                    //if divider exists
                    if(buttonDivider != null)
                    {
                        //show it
                        buttonDivider.setVisibility(View.VISIBLE);
                    }
                }

                itemDetailsGroup.findViewById(R.id.Item_Detail_Button_Layout).setVisibility(View.VISIBLE);

            }
            public void showItemDetailRows(int offset)
            {
                showItemDetailRows(offset, -1, true);
            }

            //Gets item detail titles
            public TextView[] getItemDetailTitles()
            {
                TextView[] titles = new TextView[12];

                if(canShow)
                {
                    titles[0] = itemDetailsGroup.findViewById(R.id.Item_Detail_Title1);
                    titles[1] = itemDetailsGroup.findViewById(R.id.Item_Detail_Title2);
                    titles[2] = itemDetailsGroup.findViewById(R.id.Item_Detail_Title3);
                    titles[3] = itemDetailsGroup.findViewById(R.id.Item_Detail_Title4);
                    titles[4] = itemDetailsGroup.findViewById(R.id.Item_Detail_Title5);
                    titles[5] = itemDetailsGroup.findViewById(R.id.Item_Detail_Title6);
                    titles[6] = itemDetailsGroup.findViewById(R.id.Item_Detail_Title7);
                    titles[7] = itemDetailsGroup.findViewById(R.id.Item_Detail_Title8);
                    titles[8] = itemDetailsGroup.findViewById(R.id.Item_Detail_Title9);
                    titles[9] = itemDetailsGroup.findViewById(R.id.Item_Detail_Title10);
                    titles[10] = itemDetailsGroup.findViewById(R.id.Item_Detail_Title11);
                    titles[11] = itemDetailsGroup.findViewById(R.id.Item_Detail_Title12);
                }

                return(titles);
            }

            //Get detail titles
            public TextView[] getDetailTitles()
            {
                return(detailTitles.toArray(new TextView[0]));
            }

            //Gets detail texts
            public TextView[] getDetailTexts()
            {
                return(detailTexts.toArray(new TextView[0]));
            }

            //Gets item detail texts
            public TextView[] getItemDetailTexts()
            {
                TextView[] texts = new TextView[12];

                if(canShow)
                {
                    texts[0] = itemDetailsGroup.findViewById(R.id.Item_Detail_Text1);
                    texts[1] = itemDetailsGroup.findViewById(R.id.Item_Detail_Text2);
                    texts[2] = itemDetailsGroup.findViewById(R.id.Item_Detail_Text3);
                    texts[3] = itemDetailsGroup.findViewById(R.id.Item_Detail_Text4);
                    texts[4] = itemDetailsGroup.findViewById(R.id.Item_Detail_Text5);
                    texts[5] = itemDetailsGroup.findViewById(R.id.Item_Detail_Text6);
                    texts[6] = itemDetailsGroup.findViewById(R.id.Item_Detail_Text7);
                    texts[7] = itemDetailsGroup.findViewById(R.id.Item_Detail_Text8);
                    texts[8] = itemDetailsGroup.findViewById(R.id.Item_Detail_Text9);
                    texts[9] = itemDetailsGroup.findViewById(R.id.Item_Detail_Text10);
                    texts[10] = itemDetailsGroup.findViewById(R.id.Item_Detail_Text11);
                    texts[11] = itemDetailsGroup.findViewById(R.id.Item_Detail_Text12);
                }

                return(texts);
            }

            //Gets item detail buttons
            public AppCompatImageButton[] getItemDetailButtons()
            {
                AppCompatImageButton[] buttons = new AppCompatImageButton[7];

                if(canShow)
                {
                    buttons[0] = itemDetailsGroup.findViewById(R.id.Item_Detail_Button1);
                    buttons[1] = itemDetailsGroup.findViewById(R.id.Item_Detail_Button2);
                    buttons[2] = itemDetailsGroup.findViewById(R.id.Item_Detail_Button3);
                    buttons[3] = itemDetailsGroup.findViewById(R.id.Item_Detail_Button4);
                    buttons[4] = itemDetailsGroup.findViewById(R.id.Item_Detail_Button5);
                    buttons[5] = itemDetailsGroup.findViewById(R.id.Item_Detail_Button6);
                    buttons[6] = itemDetailsGroup.findViewById(R.id.Item_Detail_Button7);
                }

                return(buttons);
            }

            //Gets a view from the item group
            public <T extends View> T findViewById(@IdRes int id)
            {
                if(!canShow)
                {
                    return(null);
                }
                else
                {
                    return(itemDetailsGroup.findViewById(id));
                }
            }

            public void show()
            {
                if(canShow)
                {
                    dialog.show();
                }
            }
        }


        private boolean enableItemClicks;
        //protected boolean horizontal = false;
        protected int dataID = Integer.MAX_VALUE;
        protected int itemsRefID = -1;
        protected int itemsRootViewID = -1;
        protected int widthDp = Globals.getDeviceDp(null, true);
        protected String categoryTitle;
        protected Context currentContext;
        protected LayoutInflater listInflater;
        private OnItemClickListener itemClickedListener;
        private OnItemLongClickListener itemLongClickedListener;
        protected OnItemDetailButtonClickListener itemDetailButtonClickListener;
        public View headerView;
        public ViewGroup graphGroup;
        public ViewGroup preview3dGroup;
        public ViewGroup informationGroup;

        public ListBaseAdapter(Context context)
        {
            categoryTitle = null;
            currentContext = context;
            listInflater = (currentContext != null ? (LayoutInflater)currentContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) : null);
            enableItemClicks = true;
        }
        public ListBaseAdapter(Context context, String categoryTitle)
        {
            this(context);
            this.categoryTitle = categoryTitle;
        }
        public ListBaseAdapter(View parentView, String categoryTitle)
        {
            this((parentView != null ? parentView.getContext() : null), categoryTitle);
        }

        //Sets header view
        protected void setHeader(View header)
        {
            //set and hide until overridden
            headerView = header;
            headerView.setVisibility(View.GONE);
        }

        //Sets graph group
        private void setGraphGroup(ViewGroup graph)
        {
            graphGroup = graph;
        }

        //Sets preview 3d group
        private void setPreview3dGroup(ViewGroup preview3d)
        {
            preview3dGroup = preview3d;
        }

        //Sets information group
        private void setInformationGroup(ViewGroup information)
        {
            informationGroup = information;
        }

        //Returns if showing column titles
        protected boolean showColumnTitles(int page)
        {
            return(true);
        }

        //Sets column titles
        protected void setColumnTitles(ViewGroup listColumns, TextView categoryText, int page)
        {
            boolean haveContext = (currentContext != null);
            int[] colors = (haveContext ? Globals.resolveAttributeIDs(currentContext, new int[]{R.attr.columnBackground, R.attr.columnTitleTextColor}) : new int[]{Color.BLACK, Color.WHITE});
            colors[1] = (haveContext ? currentContext.getResources().getColor(colors[1]) : colors[1]);

            if(showColumnTitles(page))
            {
                if(haveContext)
                {
                    listColumns.setBackgroundResource(colors[0]);
                }
                else
                {
                    listColumns.setBackgroundColor(colors[0]);
                }
                Globals.setTextColor(listColumns, colors[1]);
            }
            else
            {
                listColumns.setVisibility(View.GONE);
            }
        }

        //Handles a non editable item click
        protected void onItemNonEditClick(ListItem item, int pageNum)
        {
            //needs to be overridden
        }

        //Sets item clicks enabled
        public void setItemClicksEnabled(boolean enable)
        {
            enableItemClicks = enable;
        }

        //Sets item background
        private void setItemBackground(View itemView, int bgId)
        {
            int index;

            //if not an AppCompatButton
            if(!(itemView instanceof AppCompatButton))
            {
                //set view background
                itemView.setBackgroundResource(bgId);
            }

            //if a view group
            if(itemView instanceof ViewGroup)
            {
                //go through each child
                ViewGroup itemGroup = (ViewGroup)itemView;
                for(index = 0; index < itemGroup.getChildCount(); index++)
                {
                    //set child background
                    setItemBackground(itemGroup.getChildAt(index), bgId);
                }
            }
        }
        public void setItemBackground(View itemView, boolean isSelected)
        {
            int[] ids;

            //if view and context exist
            if(itemView != null && currentContext != null)
            {
                //get around compiler bug by storing result in array
                ids = new int[]{Globals.resolveAttributeID(currentContext, (isSelected ? R.attr.itemSelected : R.attr.itemSelect))};
                setItemBackground(itemView, ids[0]);
            }
        }

        //Sets view click listeners
        protected void setViewClickListeners(View itemView, final RecyclerView.ViewHolder itemHolder)
        {
            itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if(itemClickedListener != null && enableItemClicks)
                    {
                        itemClickedListener.onItemClicked(view, itemHolder.getAdapterPosition());
                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View view)
                {
                    if(itemLongClickedListener != null && enableItemClicks)
                    {
                        itemLongClickedListener.onItemLongClicked(view, itemHolder.getAdapterPosition());
                        return(true);
                    }

                    return(false);
                }
            });
        }

        //Sets on item clicked listener
        public void setOnItemClickedListener(OnItemClickListener listener)
        {
            itemClickedListener = listener;
        }

        //Sets on item long clicked listener
        public void setOnItemLongClickedListener(OnItemLongClickListener listener)
        {
            itemLongClickedListener = listener;
        }

        //Sets on item detail clicked listener
        public void setOnItemDetailButtonClickedListener(OnItemDetailButtonClickListener listener)
        {
            itemDetailButtonClickListener = listener;
        }

        //Gets an item
        protected ListItem getItem(int position)
        {
            //needs to be overridden
            return(null);
        }

        //Gets all items
        protected ListItem[] getItems()
        {
            //needs to be overridden
            return(null);
        }

        //Returns true if 3d preview available for norad ID
        protected static boolean have3dPreview(int noradId)
        {
            switch(noradId)
            {
                case Universe.IDs.Moon:
                case Universe.IDs.Mars:
                case Universe.IDs.Venus:
                case Universe.IDs.Mercury:
                case Universe.IDs.Jupiter:
                case Universe.IDs.Saturn:
                case Universe.IDs.Uranus:
                case Universe.IDs.Neptune:
                case Universe.IDs.Sun:
                case Universe.IDs.Pluto:
                    return(true);
            }

            return(false);
        }

        protected static int getMapLayerType(int noradId)
        {
            //get map layer type
            switch(noradId)
            {
                case Universe.IDs.Moon:
                    return(CoordinatesFragment.MapLayerType.Moon);

                case Universe.IDs.Mars:
                    return(CoordinatesFragment.MapLayerType.Mars);

                case Universe.IDs.Venus:
                    return(CoordinatesFragment.MapLayerType.Venus);

                case Universe.IDs.Mercury:
                    return(CoordinatesFragment.MapLayerType.Mercury);

                case Universe.IDs.Jupiter:
                    return(CoordinatesFragment.MapLayerType.Jupiter);

                case Universe.IDs.Saturn:
                    return(CoordinatesFragment.MapLayerType.Saturn);

                case Universe.IDs.Uranus:
                    return(CoordinatesFragment.MapLayerType.Uranus);

                case Universe.IDs.Neptune:
                    return(CoordinatesFragment.MapLayerType.Neptune);

                case Universe.IDs.Sun:
                    return(CoordinatesFragment.MapLayerType.Sun);

                case Universe.IDs.Pluto:
                    return(CoordinatesFragment.MapLayerType.Pluto);

                default:
                    return(CoordinatesFragment.MapLayerType.None);
            }
        }
    }

    public static abstract class ListFragment extends Fragment implements ActionMode.Callback
    {
        //On edit mode changed listener
        public interface OnEditModeChangedListener
        {
            void editModeChanged(boolean editMode);
        }

        //On update needed
        public interface OnUpdateNeededListener
        {
            void updateNeeded();
        }

        //On update page listener
        public interface OnUpdatePageListener
        {
            void updatePage(int page, int subPage);
        }

        //On item checked listener
        public interface OnItemCheckChangedListener
        {
            void itemCheckedChanged(int page, ListItem item);
        }

        //On items changed listener
        public interface OnItemsChangedListener
        {
            void itemsChanged();
        }

        //On orientation changed listener
        public interface OnOrientationChangedListener
        {
            void orientationChanged();
        }

        //On header changed listener
        public interface OnHeaderChangedListener
        {
            void headerChanged(int id, String text);
        }

        //On item detail button click listener
        public interface OnItemDetailButtonClickListener
        {
            void onClick(int group, int pageType, int itemID, ListItem item, int buttonNum);
        }

        //On graph changed listener
        public interface OnGraphChangedListener
        {
            void graphChanged(Database.SatelliteData orbital1, ArrayList<CalculateViewsTask.OrbitalView> pathPoints, Database.SatelliteData orbital2, ArrayList<CalculateViewsTask.OrbitalView> path2Points);
        }

        //On preview 3d changed listener
        public interface OnPreview3dChangedListener
        {
            void preview3dChanged(int noradId);
        }

        //On information changed listener
        public interface OnInformationChangedListener
        {
            void informationChanged(Spanned text);
        }

        //On adapter set listener
        public interface OnAdapterSetListener
        {
            void setAdapter(int group, int position, RecyclerView.Adapter adapter);
        }

        //On item selected listener
        public interface OnItemSelectedListener
        {
            void itemSelected(int group, int page, int subPage, int position, boolean selected);
        }

        //On resume listener
        public interface OnPageResumeListener
        {
            void resumed(ListFragment page);
        }

        //On page destroy listener
        public interface OnPageDestroyListener
        {
            void destroyed(ListFragment page);
        }

        //Sets action mode item properties
        protected abstract boolean setupActionModeItems(MenuItem edit, MenuItem delete, MenuItem save, MenuItem sync);

        //Handles on action mode edit
        protected abstract void onActionModeEdit();

        //Handles action mode delete
        protected abstract void onActionModeDelete();
        protected abstract int onActionModeConfirmDelete();

        //Handles action mode save
        protected abstract void onActionModeSave();

        //Handles action mode sync
        protected abstract void onActionModeSync();

        //Handles update progress
        protected abstract void onUpdateStarted();
        protected abstract void onUpdateFinished(boolean success);

        private OnEditModeChangedListener editModeChangedListener;
        private OnItemCheckChangedListener itemCheckChangedListener;
        private OnItemDetailButtonClickListener itemDetailButtonClickListener;
        private OnAdapterSetListener adapterSetListener;
        private OnItemSelectedListener itemSelectedListener;
        private OnUpdateNeededListener updateNeededListener;
        private OnUpdatePageListener updatePageListener;
        private OnPageResumeListener pageResumeListener;
        private OnPageDestroyListener pageDestroyListener;
        private BroadcastReceiver updateReceiver;

        private Menu actionMenu;
        private ActionMode actionModeMenu;
        private ListBaseAdapter selectListAdapter;
        protected TextView categoryText;
        protected RecyclerView selectList;
        protected int group;
        protected int pageNum;
        protected boolean inEditMode;
        protected View listParentView;
        protected PlayBar playBar;
        protected PlayBar scaleBar;
        protected ArrayList<ListItem> selectedItems;

        public ListFragment()
        {
            super();
            group = pageNum = -1;
            inEditMode = false;
            categoryText = null;
            selectList = null;
            selectListAdapter = null;
            selectedItems = new ArrayList<>(0);
        }

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            Context context = this.getContext();

            updateReceiver = createLocalBroadcastReceiver();
            if(context != null)
            {
                LocalBroadcastManager.getInstance(context).registerReceiver(updateReceiver, new IntentFilter(UpdateService.UPDATE_FILTER));
            }

            this.setRetainInstance(true);       //keep adapter on orientation changes
            this.setHasOptionsMenu(true);
        }

        @Override
        public void onResume()
        {
            super.onResume();

            if(pageResumeListener != null)
            {
                pageResumeListener.resumed(this);
            }
        }

        @Override
        public void onDestroy()
        {
            Context context = this.getContext();

            if(pageDestroyListener != null)
            {
                pageDestroyListener.destroyed(this);
            }

            cancelEditMode();

            if(playBar != null)
            {
                playBar.stopPlayTimer();
            }

            if(context != null)
            {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(updateReceiver);
            }

            super.onDestroy();
        }

        @Override
        public void setArguments(@Nullable Bundle args)
        {
            try
            {
                super.setArguments(args);
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }

        protected int getListColumns(Context context, int page)
        {
            return(1);
        }

        protected void setListColumns(Context context, View listColumns, int page)
        {
            int columns;
            int currentColumns;
            boolean darkTheme;
            Object tagObject;
            DividerItemDecoration verticalDivider;
            DividerItemDecoration horizontalDivider;

            //if context and list are set
            if(context != null && selectList != null)
            {
                //get theme and desired columns
                darkTheme = Settings.getDarkTheme(context);
                columns = getListColumns(context, page);

                //if there are existing columns
                tagObject = selectList.getTag();
                if(tagObject != null)
                {
                    //remove all column decorations
                    currentColumns = (int)tagObject;
                    selectList.removeItemDecorationAt(0);
                    if(currentColumns > 1)
                    {
                        selectList.removeItemDecorationAt(0);
                    }
                }

                //add columns and decorations
                horizontalDivider = new DividerItemDecoration(context, LinearLayoutManager.VERTICAL);
                horizontalDivider.setDrawable(Globals.getDrawable(context, (darkTheme ? R.drawable.divider_horizontal_dark : R.drawable.divider_horizontal_light)));
                selectList.setLayoutManager(columns > 1 ? (new GridLayoutManager(context, columns)) : (new LinearLayoutManager(context)));
                selectList.addItemDecoration(horizontalDivider);
                if(columns > 1)
                {
                    verticalDivider = new DividerItemDecoration(context, LinearLayoutManager.HORIZONTAL);
                    verticalDivider.setDrawable(Globals.getDrawable(context, (darkTheme ? R.drawable.divider_vertical_dark : R.drawable.divider_vertical_light)));
                    selectList.addItemDecoration(verticalDivider);
                }

                //force list refresh
                selectList.setAdapter(null);
                selectList.setAdapter(selectListAdapter);

                //update column count
                selectList.setTag(columns);

                //if columns are set
                if(listColumns != null)
                {
                    //update column titles
                    selectListAdapter.setColumnTitles((ViewGroup)listColumns, categoryText, pageNum);
                }
            }
        }

        protected View onCreateView(LayoutInflater inflater, ViewGroup container, ListBaseAdapter listAdapter, int grp, int page)
        {
            int viewIndex;
            View header;
            View listColumns;
            Context context = this.getContext();
            ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.list_view, container, false);

            //remember group and page
            group = grp;
            pageNum = page;

            //setup header
            header = Globals.replaceView(R.id.List_Header, R.layout.header_text_view, inflater, rootView);

            //setup columns
            listColumns = rootView.findViewById(R.id.List_View_Columns);
            viewIndex = rootView.indexOfChild(listColumns);
            rootView.removeViewAt(viewIndex);
            if(listAdapter != null && listAdapter.itemsRefID > -1)
            {
                listColumns = inflater.inflate(listAdapter.itemsRefID, rootView, false);
                rootView.addView(listColumns, viewIndex);
                listAdapter.itemsRootViewID = listColumns.getId();
            }
            else
            {
                listColumns = null;
            }

            //setup category
            categoryText = rootView.findViewById(android.R.id.title);
            ((View)categoryText.getParent()).setVisibility(View.GONE);

            //setup list
            selectList = rootView.findViewById(R.id.List_View_List);
            selectList.setHasFixedSize(true);
            selectListAdapter = listAdapter;
            if(selectListAdapter != null)
            {
                setListColumns(context, listColumns, pageNum);

                selectListAdapter.setHeader(header);
                selectList.setAdapter(selectListAdapter);
                if(adapterSetListener != null)
                {
                    adapterSetListener.setAdapter(group, pageNum, selectListAdapter);
                }
                selectListAdapter.setOnItemClickedListener(createOnItemClickListener());
                selectListAdapter.setOnItemLongClickedListener(createOnItemLongClickListener());
                selectListAdapter.setOnItemDetailButtonClickedListener(createOnItemDetailButtonClickListener());
            }

            //return view
            return(rootView);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu)
        {
            //create action menu
            mode.getMenuInflater().inflate(R.menu.menu_action_layout, menu);
            actionMenu = menu;
            actionModeMenu = mode;
            return(true);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu)
        {
            return(setupActionModeItems(menu.findItem(R.id.menu_edit), menu.findItem(R.id.menu_delete), menu.findItem(R.id.menu_save), menu.findItem(R.id.menu_update)));
        }

        //Stops the play timer
        public void stopPlayTimer()
        {
            if(playBar != null)
            {
                playBar.stopPlayTimer();
            }
        }

        //Refresh action mode displays
        public void refreshActionMode()
        {
            //if action mode and menu are set
            if(actionModeMenu != null && actionMenu != null)
            {
                //update action mode
                onPrepareActionMode(actionModeMenu, actionMenu);
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item)
        {
            int id = item.getItemId();

            //handle item
            switch(id)
            {
                case R.id.menu_edit:
                    onActionModeEdit();
                    break;

                case R.id.menu_delete:
                    onActionModeDelete();
                    break;

                case R.id.menu_save:
                    onActionModeSave();
                    break;

                case R.id.menu_update:
                    onActionModeSync();
                    break;
            }

            return(true);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode)
        {
            actionMenu = null;
            actionModeMenu = null;
            cancelEditMode();
        }

        //Updates the action menu mode
        protected void updateActionModeMenu(boolean show)
        {
            AppCompatActivity activity;

            //if showing
            if(show)
            {
                //if not already using
                if(actionModeMenu == null)
                {
                    //show menu and update it
                    activity = (AppCompatActivity)this.getActivity();
                    actionModeMenu = (activity != null ? activity.startSupportActionMode(this) : null);
                }

                //if menu exists
                if(actionModeMenu != null)
                {
                    //update it
                    actionModeMenu.invalidate();
                }
            }
            else
            {
                //if menu exists
                if(actionModeMenu != null)
                {
                    //close it
                    actionModeMenu.finish();
                }
            }

            //prevent rotation until done
            lockScreenOrientation(show);
        }

        //Sets the parent view
        public void setParentView(View parentView)
        {
            listParentView = parentView;
        }

        //Sets item clicked enabled
        public void setItemClicksEnabled(boolean enabled)
        {
            if(selectListAdapter != null)
            {
                selectListAdapter.setItemClicksEnabled(enabled);
            }
        }

        //Sets selected state of item
        public void setItemSelected(int position, boolean selected)
        {
            int itemIndex;

            //if a valid position
            if(position >= 0 && position < selectListAdapter.getItemCount())
            {
                //remember current item and view
                ListItem currentItem = selectListAdapter.getItem(position);
                RecyclerView.LayoutManager selectListManager = selectList.getLayoutManager();
                View itemView = (selectListManager != null ? selectListManager.findViewByPosition(position) : null);

                //update item
                currentItem.isSelected = selected;
                selectListAdapter.setItemBackground(itemView, selected);

                //update selected list
                itemIndex = selectedItems.indexOf(currentItem);
                if(selected && itemIndex < 0)
                {
                    selectedItems.add(currentItem);
                }
                else if(!selected && itemIndex >= 0)
                {
                    selectedItems.remove(currentItem);
                }

                //send event
                onItemSelected(group, pageNum, getSubPageParam(), position, selected);
            }

            //if deselecting and no more selected items
            if(!selected && selectedItems.size() == 0)
            {
                //cancel editing
                cancelEditMode();
            }
        }

        //Sets selected state of all items
        public void setItemsSelected(boolean selected)
        {
            int index;

            //go through each item
            for(index = 0; index < selectListAdapter.getItemCount(); index++)
            {
                setItemSelected(index, selected);
            }
        }

        //Sets checked state of an item
        public void setItemChecked(int position)
        {
            int index;
            boolean wasChecked;

            //go through each item
            for(index = 0; index < selectListAdapter.getItemCount(); index++)
            {
                //remember current item
                ListItem currentItem = selectListAdapter.getItem(index);

                //update item
                wasChecked = currentItem.isChecked;
                currentItem.setChecked(index == position);
                if(wasChecked != currentItem.isChecked)
                {
                    onItemCheckChanged(currentItem);
                }
            }
        }

        //Gets positions of given ID
        protected int getPosition(int id)
        {
            int position;
            int count = selectListAdapter.getItemCount();

            //try to find position
            for(position = 0; position < count; position++)
            {
                //if current item ID matches
                if(selectListAdapter.getItem(position).id == id)
                {
                    //found position
                    return(position);
                }
            }

            //not found
            return(-1);
        }

        //Gets int param
        private int getIntParam(String paramName)
        {
            Bundle params = this.getArguments();

            //if there are params
            if(params != null)
            {
                //get param
                return(params.getInt(paramName, -1));
            }
            else
            {
                //no param
                return(-1);
            }
        }

        //Gets group param
        protected int getGroupParam()
        {
            return(getIntParam(ParamTypes.Group));
        }

        //Gets page param
        protected int getPageParam()
        {
            return(getIntParam(ParamTypes.PageNumber));
        }

        //Gets sub page param
        protected int getSubPageParam()
        {
            return(getIntParam(ParamTypes.SubPageNumber));
        }

        //Gets adapter
        protected ListBaseAdapter getAdapter()
        {
            return(selectListAdapter);
        }

        //Gets list item count
        public int getListItemCount()
        {
            return(selectListAdapter != null ? selectListAdapter.getItemCount() : 0);
        }

        //Gets selected items
        public ArrayList<ListItem> getSelectedItems()
        {
            return(selectedItems);
        }

        //Cancels edit mode
        public void cancelEditMode()
        {
            //if in edit mode
            if(inEditMode)
            {
                inEditMode = false;
                setItemsSelected(false);
                selectedItems.clear();
                updateActionModeMenu(false);
                onEditModeChanged();
            }
        }

        //Shows a confirm delete dialog
        protected void showConfirmDeleteDialog(int pluralsId)
        {
            final int count;
            final Context context = this.getContext();
            final Resources res = this.getResources();
            final String itemString;

            //get selected item count and string
            count = selectedItems.size();
            itemString = res.getQuantityString(pluralsId, count);

            //show dialog
            Globals.showConfirmDialog(context, res.getString(R.string.title_selected_delete) + " " + itemString +  " (" + count + ")?", null, res.getString(R.string.title_delete), res.getString(R.string.title_cancel), true, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //delete items
                    int deleteCount = onActionModeConfirmDelete();

                    //update page
                    cancelEditMode();
                    onUpdateNeeded();

                    //show status
                    Globals.showSnackBar(listParentView, res.getString(R.string.title_deleted) + " " + deleteCount + " " + itemString);
                }
            }, null, null);
        }

        //Creates an item click listener
        protected ListBaseAdapter.OnItemClickListener createOnItemClickListener()
        {
            return(new ListBaseAdapter.OnItemClickListener()
            {
                @Override
                public void onItemClicked(View view, int position)
                {
                    ListItem currentItem = selectListAdapter.getItem(position);
                    if(currentItem != null)
                    {
                        //if not in edit mode and item can be checked
                        if(!inEditMode && currentItem.canCheck && !currentItem.isChecked)
                        {
                            //set item checked
                            setItemChecked(position);
                        }
                        //else if in edit mode and item can be edited
                        else if(inEditMode && currentItem.canEdit)
                        {
                            //reverse the selected state
                            setItemSelected(position, !currentItem.isSelected);
                        }
                        else
                        {
                            //handle click
                            selectListAdapter.onItemNonEditClick(currentItem, ListFragment.this.getPageParam());
                        }
                    }
                }
            });
        }

        //Creates an item long click listener
        protected ListBaseAdapter.OnItemLongClickListener createOnItemLongClickListener()
        {
            return(new ListBaseAdapter.OnItemLongClickListener()
            {
                @Override
                public void onItemLongClicked(View view, int position)
                {
                    ListItem currentItem = selectListAdapter.getItem(position);

                    //if not in edit mode and item can be edited
                    if(!inEditMode && currentItem != null && currentItem.canEdit)
                    {
                        inEditMode = true;
                        setItemSelected(position, true);
                        updateActionModeMenu(true);
                        onEditModeChanged();
                    }
                }
            });
        }

        //Creates an on item detail button click listener
        private ListBaseAdapter.OnItemDetailButtonClickListener createOnItemDetailButtonClickListener()
        {
            return(new ListBaseAdapter.OnItemDetailButtonClickListener()
            {
                @Override
                public void onClick(int pageType, int itemID, ListItem item, int buttonNum)
                {
                    //if listener is set
                    if(itemDetailButtonClickListener != null)
                    {
                        itemDetailButtonClickListener.onClick(group, pageType, itemID, item, buttonNum);
                    }
                }
            });
        }

        //Creates an on graph changed listener
        protected OnGraphChangedListener createOnGraphChangedListener(final ListBaseAdapter listAdapter)
        {
            return(new OnGraphChangedListener()
            {
                @Override
                public void graphChanged(final Database.SatelliteData orbital1, ArrayList<CalculateViewsTask.OrbitalView> pathPoints, final Database.SatelliteData orbital2, ArrayList<CalculateViewsTask.OrbitalView> path2Points)
                {
                    int length;
                    int pointIndex;
                    TypedValue colorValue = new TypedValue();
                    final boolean usingOrbital2 = (orbital2 != null && path2Points != null);
                    final Activity activity = ListFragment.this.getActivity();
                    final Resources res = (activity != null ? activity.getResources() : null);
                    final ArrayList<Double> timePoints = new ArrayList<>(0);
                    final ArrayList<Double> elevationPoints = new ArrayList<>(0);
                    final ArrayList<Double> elevation2Points = new ArrayList<>(0);

                    //if view and activity exist
                    if(listAdapter.graphGroup != null && activity != null)
                    {
                        //get displays
                        final ProgressBar graphProgress = listAdapter.graphGroup.findViewById(R.id.Item_Detail_Graph_Progress);
                        final Graph elevationGraph = listAdapter.graphGroup.findViewById(R.id.Item_Detail_Graph);

                        //setup graph
                        activity.getTheme().resolveAttribute(R.attr.colorAccent, colorValue, true);
                        elevationGraph.setColors((Settings.getDarkTheme(activity) ? Color.WHITE : Color.BLACK), colorValue.data);
                        elevationGraph.setTitles(res.getString(R.string.title_time), res.getString(R.string.title_elevation));
                        elevationGraph.setUnitTypes(Graph.UnitType.JulianDate, Graph.UnitType.Number);

                        //if -points are set- and -equal if using secondary path-
                        length = (pathPoints != null ? pathPoints.size() : 0);
                        if(length > 0 && (!usingOrbital2 || path2Points.size() == length))
                        {
                            //go through each point
                            for(pointIndex = 0; pointIndex < length; pointIndex++)
                            {
                                //remember current point(s)
                                CalculateViewsTask.OrbitalView currentPoint = pathPoints.get(pointIndex);
                                CalculateViewsTask.OrbitalView currentPoint2 = (usingOrbital2 ? path2Points.get(pointIndex) : null);

                                //add elevation and time
                                timePoints.add(currentPoint.julianDate);
                                elevationPoints.add(currentPoint.elevation);
                                if(usingOrbital2)
                                {
                                    elevation2Points.add(currentPoint2.elevation);
                                }
                            }
                        }

                        //set graph
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                int id1 = orbital1.getSatelliteNum();
                                int id2 = (usingOrbital2 ? orbital2.getSatelliteNum() : 0);

                                //hide graph progress
                                graphProgress.setVisibility(View.GONE);

                                //set graph data and show
                                elevationGraph.setData(timePoints, elevationPoints, (usingOrbital2 ? elevation2Points : null), MainActivity.getObserver().timeZone);
                                elevationGraph.setRangeX(timePoints.get(0), timePoints.get(timePoints.size() - 1), 6);
                                elevationGraph.setRangeY(0, 90, 6);
                                elevationGraph.setSelectedType(Graph.SelectType.Image);
                                elevationGraph.setSelectedImage(Globals.getBitmap(activity, Globals.getOrbitalIconID(id1, orbital1.getOrbitalType()), id1 > 0));
                                elevationGraph.setSelectedImage2(usingOrbital2 ? Globals.getBitmap(activity, Globals.getOrbitalIconID(id2, orbital2.getOrbitalType()), id2 > 0) : null);
                                elevationGraph.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
            });
        }

        //Creates an on preview 3d changed listener
        protected OnPreview3dChangedListener createOnPreview3dChangedListener(final ListBaseAdapter listAdapter)
        {
            return(new OnPreview3dChangedListener()
            {
                @Override
                public void preview3dChanged(int noradId)
                {
                    Bundle args = new Bundle();
                    FragmentActivity activity = ListFragment.this.getActivity();

                    //if view and activity exist
                    if(listAdapter.preview3dGroup != null && activity != null)
                    {
                        //get displays
                        final ProgressBar preview3dProgress = listAdapter.preview3dGroup.findViewById(R.id.Item_Detail_3d_Progress);
                        final Whirly.GlobeFragment mapView = new Whirly.GlobeFragment();

                        //setup preview
                        args.putInt(Whirly.ParamTypes.MapLayerType, ListBaseAdapter.getMapLayerType(noradId));
                        mapView.setArguments(args);

                        //update displays
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                //hide info progress
                                preview3dProgress.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            });
        }

        //Creates an on information changed listener
        protected OnInformationChangedListener createOnInformationChangedListener(final Selectable.ListBaseAdapter listAdapter)
        {
            return(new OnInformationChangedListener()
            {
                @Override
                public void informationChanged(final Spanned text)
                {
                    Activity activity = ListFragment.this.getActivity();

                    //if view and activity exist
                    if(listAdapter.informationGroup != null && activity != null)
                    {
                        //get displays
                        final ProgressBar infoProgress = listAdapter.informationGroup.findViewById(R.id.Item_Detail_Info_Progress);
                        final ScrollTextView infoText = listAdapter.informationGroup.findViewById(R.id.Item_Detail_Info_Text);

                        //update displays
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                //hide info progress
                                infoProgress.setVisibility(View.GONE);

                                //show info text
                                infoText.setText(text);
                                infoText.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
            });
        }

        //Creates a local broadcast listener
        private BroadcastReceiver createLocalBroadcastReceiver()
        {
            return(new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    int progressType = intent.getIntExtra(UpdateService.ParamTypes.ProgressType, Byte.MAX_VALUE);

                    switch(progressType)
                    {
                        case Globals.ProgressType.Started:
                            onUpdateStarted();
                            break;

                        case Globals.ProgressType.Finished:
                        case Globals.ProgressType.Cancelled:
                        case Globals.ProgressType.Denied:
                        case Globals.ProgressType.Failed:
                            onUpdateFinished(progressType == Globals.ProgressType.Finished);
                            break;
                    }
                }
            });
        }

        //Sets on edit mode changed listener
        public void setOnEditModeChangedListener(OnEditModeChangedListener listener)
        {
            editModeChangedListener = listener;
        }

        //Calls on edit mode changed listener
        private void onEditModeChanged()
        {
            if(editModeChangedListener != null)
            {
                editModeChangedListener.editModeChanged(inEditMode);
            }
        }

        //Sets on item selected listener
        public void setOnItemSelectedListener(OnItemSelectedListener listener)
        {
            itemSelectedListener = listener;
        }

        //Sets on updated needed listener
        public void setOnUpdateNeededListener(OnUpdateNeededListener listener)
        {
            updateNeededListener = listener;
        }

        //Sets on update page listener
        public void setOnUpdatePageListener(OnUpdatePageListener listener)
        {
            updatePageListener = listener;
        }

        //Call on item selected listener
        public void onItemSelected(int group, int page, int subPage, int position, boolean selected)
        {
            if(itemSelectedListener != null)
            {
                itemSelectedListener.itemSelected(group, page, subPage, position, selected);
            }
        }

        //Call on update needed listener
        public void onUpdateNeeded()
        {
            if(updateNeededListener != null)
            {
                updateNeededListener.updateNeeded();
            }
        }

        //Call on page update listener
        public void onUpdatePage(int page, int subPage)
        {
            if(updatePageListener != null)
            {
                updatePageListener.updatePage(page, subPage);
            }
        }

        //Sets on page resumed listener
        public void setOnPageResumeListener(OnPageResumeListener listener)
        {
            pageResumeListener = listener;
        }

        //Sets on page destroy listener
        public void setOnPageDestroyListener(OnPageDestroyListener listener)
        {
            pageDestroyListener = listener;
        }

        //Sets on item checked listener
        public void setOnItemCheckChangedListener(OnItemCheckChangedListener listener)
        {
            itemCheckChangedListener = listener;
        }

        //Calls on item check changed listener
        private void onItemCheckChanged(ListItem item)
        {
            if(itemCheckChangedListener != null)
            {
                itemCheckChangedListener.itemCheckedChanged(pageNum, item);
            }
        }

        //Sets on detail button click listener
        public void setOnDetailButtonClickListener(OnItemDetailButtonClickListener listener)
        {
            itemDetailButtonClickListener = listener;
        }

        //Sets on set adapter listener
        public void setOnSetAdapterListener(OnAdapterSetListener adapter)
        {
            adapterSetListener = adapter;
        }

        //Locks screen orientation
        public void lockScreenOrientation(boolean lock)
        {
            Globals.lockScreenOrientation(this.getActivity(), lock);
        }
    }

    //Page adapter
    public static class ListFragmentAdapter extends FragmentStatePagerAdapter
    {
        protected int group;
        protected Context currentContext;
        protected int[] subPage;
        private final View currentParentVIew;
        private final ListFragment.OnUpdateNeededListener updateNeededListener;
        private final ListFragment.OnItemSelectedListener itemSelectedListener;
        private final ListFragment.OnUpdatePageListener updatePageListener;
        private final ListFragment.OnItemCheckChangedListener itemCheckChangedListener;
        private final ListFragment.OnItemDetailButtonClickListener itemDetailButtonClickListener;
        private final ListFragment.OnAdapterSetListener adapterSetListener;
        private final ListFragment.OnPageResumeListener pageResumeListener;

        public ListFragmentAdapter(FragmentManager fm, View parentView, ListFragment.OnItemSelectedListener selectedListener, ListFragment.OnUpdateNeededListener updateListener, ListFragment.OnUpdatePageListener updatePgListener, ListFragment.OnItemCheckChangedListener checkChangedListener, ListFragment.OnItemDetailButtonClickListener detailButtonClickListener, ListFragment.OnAdapterSetListener adapterListener, ListFragment.OnPageResumeListener resumeListener, int grp, int[] subPg)
        {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            currentParentVIew = parentView;
            currentContext = (parentView != null ? parentView.getContext() : null);
            group = grp;
            subPage = subPg;
            itemSelectedListener = selectedListener;
            updateNeededListener = updateListener;
            updatePageListener = updatePgListener;
            itemCheckChangedListener = checkChangedListener;
            itemDetailButtonClickListener = detailButtonClickListener;
            adapterSetListener = adapterListener;
            pageResumeListener = resumeListener;
        }

        @Override
        public Fragment getItem(int position)
        {
            return(null);
        }
        protected Fragment getItem(int group, int position, int subPosition, ListFragment newPage)
        {
            Bundle params = newPage.getArguments();

            if(params == null)
            {
                params = new Bundle();
            }
            params.putInt(ParamTypes.Group, group);
            params.putInt(ParamTypes.PageNumber, (group == MainActivity.Groups.Current && Settings.getCombinedCurrentLayout(currentContext) ? Current.PageType.Combined : position));
            params.putInt(ParamTypes.SubPageNumber, subPosition);

            newPage.setArguments(params);
            return(setupItem(newPage));
        }

        //Sets up item
        protected Fragment setupItem(ListFragment newPage)
        {
            newPage.setParentView(currentParentVIew);
            newPage.setOnItemSelectedListener(itemSelectedListener);
            newPage.setOnUpdateNeededListener(updateNeededListener);
            newPage.setOnUpdatePageListener(updatePageListener);
            newPage.setOnItemCheckChangedListener(itemCheckChangedListener);
            newPage.setOnDetailButtonClickListener(itemDetailButtonClickListener);
            newPage.setOnSetAdapterListener(adapterSetListener);
            newPage.setOnPageResumeListener(pageResumeListener);

            return(newPage);
        }

        @Override
        public int getCount()
        {
            return(0);
        }

        @Override
        public int getItemPosition(@NonNull Object item)
        {
            //note: allows for notifyDataSetChanged() to refresh list
            return(POSITION_NONE);
        }

        /*public Bundle getParams()
        {
            return(currentParams);
        }*/

        //Returns the given page
        public ListFragment getPage(ViewGroup container, int pageNum)
        {
            return(pageNum < this.getCount() ? (ListFragment)this.instantiateItem(container, pageNum) : null);
        }

        //Returns selected items
        public ArrayList<ListItem> getSelectedItems(ViewGroup container, int pageNum)
        {
            ListFragment page = getPage(container, pageNum);
            return(page != null ? page.getSelectedItems() : new ArrayList<ListItem>(0));
        }

        //Selects all items
        public void selectItems(ViewGroup container, int pageNum, boolean selected)
        {
            ListFragment page = getPage(container, pageNum);
            if(page != null)
            {
                page.setItemsSelected(selected);
            }
        }

        //Cancels edit mode
        public void cancelEditMode(ViewGroup container)
        {
            int index;

            //go through each page
            for(index = 0; index < this.getCount(); index++)
            {
                //cancel edit mode
                getPage(container, index).cancelEditMode();
            }
        }

        //Stops play timer
        public void stopPlayTimer(ViewGroup container, int pageNum)
        {
            ListFragment page = getPage(container, pageNum);
            if(page != null)
            {
                page.stopPlayTimer();
            }
        }

        //Sets sub page
        public void setSubPage(int page, int subPg)
        {
            if(page < subPage.length)
            {
                subPage[page] = subPg;
            }
        }
    }
}
