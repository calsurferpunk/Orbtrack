package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Parcel;
import android.os.Parcelable;
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
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;


public abstract class Selectable
{
    public static abstract class ParamTypes
    {
        static final String Group = "group";
        static final String PageNumber = "page";
        static final String SubPageNumber = "subPage";
    }

    //Select list item
    protected static class ListItem implements Parcelable
    {
        public static class Comparer implements Comparator<ListItem>
        {
            @Override
            public int compare(ListItem value1, ListItem value2)
            {
                return(Integer.compare(value1.listIndex, value2.listIndex));
            }
        }

        public int id;
        public int listIndex;
        public static final Creator<ListItem> CREATOR = new Creator<ListItem>()
        {
            @Override
            public ListItem createFromParcel(Parcel source)
            {
                return(new ListItem(source.readInt(), source.readInt()));
            }

            @Override
            public ListItem[] newArray(int size)
            {
                return new ListItem[size];
            }
        };

        public ListItem(int idNum, int index)
        {
            id = idNum;
            listIndex = index;
        }

        @Override
        public int describeContents()
        {
            return(0);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeInt(id);
            dest.writeInt(listIndex);
        }
    }

    //Select list display item
    protected static class ListDisplayItem extends ListItem
    {
        public final boolean canEdit;
        public boolean isSelected;
        public final boolean canCheck;
        public boolean isChecked;
        public CheckBox checkBoxView;

        public ListDisplayItem(int idNum, int index, boolean canEd, boolean isSel, boolean canCh, boolean isCh)
        {
            super(idNum, index);
            canEdit = canEd;
            isSelected = isSel;
            canCheck = canCh;
            isChecked = isCh;
        }
        public ListDisplayItem(int idNum, int index)
        {
            this(idNum, index, false, false, false, false);
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

    //Select list display item holder
    protected static class ListDisplayItemHolder extends RecyclerView.ViewHolder
    {
        public final CheckBox checkBoxView;

        public ListDisplayItemHolder(View itemView, int checkBoxID)
        {
            super(itemView);
            checkBoxView = (checkBoxID != -1 ? itemView.findViewById(checkBoxID) : null);
        }
    }

    public static abstract class ListBaseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    {
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
            void onClick(int pageType, int itemID, ListDisplayItem item, int buttonNum);
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
        @SuppressLint("InflateParams")
        public class ItemDetailDialog
        {
            private boolean canShow;
            private final boolean usingMaterial;
            private int groupCount;
            private final int[] noradIds;
            private final long timerDelay;
            private final Context currentContext;
            private ViewGroup itemDetailsGroup;
            private TableLayout itemDetailTable;
            private LinearLayout itemDetailCardsLayout;
            private AlertDialog dialog;
            private final Timer updateTimer = new Timer();
            private Whirly.PreviewFragment itemDetail3dView = null;
            private OnItemDetailButtonClickListener itemDetailButtonClickListener;
            private final ArrayList<TextView> detailTexts = new ArrayList<>();
            private final ArrayList<TextView> detailTitles = new ArrayList<>();
            private final ArrayList<AppCompatImageButton> detailButtons = new ArrayList<>();
            private ArrayList<DialogInterface.OnDismissListener> dismissListeners;

            public ItemDetailDialog(Context context, LayoutInflater inflater, int[] ids, String title, String[] ownerCodes, Drawable[] icons, OnItemDetailButtonClickListener listener)
            {
                final FrameLayout itemDetail3dFrame;
                final FrameLayout itemDetail3dCloseFrame;
                final LinearLayout itemDetailButtonLayout;
                final FloatingActionButton itemDetail3dCloseButton;
                final FloatingActionButton itemDetail3dFullscreenButton;
                final FragmentManager manager;
                final int[] screenSize = Globals.getDevicePixels(context);
                CustomAlertDialogBuilder itemDetailDialog;

                canShow = true;
                groupCount = 0;
                usingMaterial = Settings.getMaterialTheme(context);
                currentContext = context;
                noradIds = ids;
                timerDelay = Settings.getListUpdateDelay(currentContext);

                try
                {
                    itemDetailsGroup = (ViewGroup)inflater.inflate((usingMaterial ? R.layout.item_detail_material_dialog : R.layout.item_detail_dialog), null);
                }
                catch(Exception ex)
                {
                    canShow = false;
                    return;
                }

                itemDetailCardsLayout = itemDetailsGroup.findViewById(R.id.Item_Detail_Cards_Layout);
                itemDetailTable = itemDetailsGroup.findViewById(R.id.Item_Detail_Table);
                itemDetail3dFrame = itemDetailsGroup.findViewById(R.id.Item_Detail_3d_Frame);
                itemDetail3dCloseFrame = itemDetailsGroup.findViewById(R.id.Item_Detail_3d_Close_Frame);
                itemDetailButtonLayout = itemDetailsGroup.findViewById(R.id.Item_Detail_Button_Layout);
                itemDetail3dCloseButton = itemDetailsGroup.findViewById(R.id.Item_Detail_3d_Close_Button);
                itemDetail3dFullscreenButton = itemDetailsGroup.findViewById(R.id.Item_Detail_3d_Fullscreen_Button);
                itemDetailDialog = new CustomAlertDialogBuilder(currentContext, Globals.getDialogThemeId(currentContext), usingMaterial, false);
                manager = Globals.getFragmentManager(currentContext);
                itemDetailButtonClickListener = listener;
                dismissListeners = new ArrayList<>(0);

                //setup and show dialog
                if(title != null)
                {
                    int index;
                    ArrayList<Drawable> resultIcons = new ArrayList<>(0);

                    //if same number of ids, owner codes, and icons
                    if(ids.length == ownerCodes.length && ownerCodes.length == icons.length)
                    {
                        //go through owner codes and icons
                        for(index = 0; index < ownerCodes.length; index++)
                        {
                            //if icon is set
                            if(icons[index] != null)
                            {
                                //add combination of owner and icon
                                int[] ownerIconIds = Globals.getOwnerIconIDs(ownerCodes[index]);
                                for(int currentId : ownerIconIds)
                                {
                                    if(currentId > 0)
                                    {
                                        resultIcons.add(Globals.getDrawable(context, currentId));
                                    }
                                }
                                resultIcons.add(Globals.getDrawableCombined(context, icons[index].getConstantState().newDrawable().mutate()));  //note: makes icon copy so that original is not altered
                            }
                        }

                        //set icon to combination of all previous
                        itemDetailDialog.setIcon(Globals.getDrawableCombined(context, resultIcons.toArray(new Drawable[0])));
                    }

                    //set title
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
                        View detailsLayout = (usingMaterial ? itemDetailCardsLayout : itemDetailTable);
                        boolean showingDetails = (detailsLayout.getVisibility() == View.GONE);
                        int detailsVisibility = (showingDetails ? View.VISIBLE : View.GONE);
                        ViewGroup.LayoutParams frameParams = itemDetail3dFrame.getLayoutParams();

                        //update button
                        itemDetail3dFullscreenButton.setImageDrawable(Globals.getDrawable(currentContext, (showingDetails ? R.drawable.ic_fullscreen_white : R.drawable.ic_fullscreen_exit_white)));

                        //update size
                        if(startFrameHeight == 0)
                        {
                            ///get sizes
                            startFrameHeight = itemDetail3dFrame.getHeight();
                            expandFrameHeight = (screenSize[1] - (int)(itemDetailButtonLayout.getHeight() * (usingMaterial ? 3.5 : 2))); //note: since unknown how to get title height, button layout used instead in addition (thus 2x)
                        }
                        frameParams.height = (showingDetails ? startFrameHeight : expandFrameHeight);
                        itemDetail3dFrame.setLayoutParams(frameParams);

                        //update visibility
                        itemDetail3dCloseFrame.setVisibility(showingDetails ? View.GONE : View.VISIBLE);
                        detailsLayout.setVisibility(detailsVisibility);
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
                        if(manager != null && itemDetail3dView != null)
                        {
                            //remove it
                            manager.beginTransaction().remove(itemDetail3dView).commit();
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
                if(manager != null)
                {
                    //set item detail 3d preview
                    itemDetail3dView = (Whirly.PreviewFragment)manager.findFragmentByTag("itemDetail3dView");
                }
            }
            public ItemDetailDialog(Context context, LayoutInflater inflater, int id, String title, String ownerCode, Drawable icon, OnItemDetailButtonClickListener listener)
            {
                this(context, inflater, new int[]{id}, title, new String[]{ownerCode}, new Drawable[]{icon}, listener);
            }

            private void addDivider(ViewGroup view, int index, boolean vertical)
            {
                TextView emptyText;
                TableRow.LayoutParams params;
                LinearLayout dividerHolder = null;
                float dpPixels;

                if(vertical || !usingMaterial)
                {
                    emptyText = new TextView(new ContextThemeWrapper(currentContext, (vertical ? (usingMaterial ? R.style.DetailVerticalDividerMaterial : R.style.DetailVerticalDivider) : R.style.Divider)));
                    if(vertical)
                    {
                        dividerHolder = new LinearLayout(currentContext);
                        params = new TableRow.LayoutParams();
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                        params.gravity = Gravity.CENTER;
                        dividerHolder.setLayoutParams(params);

                        dpPixels = Globals.dpToPixels(currentContext, 2);
                        params = new TableRow.LayoutParams();
                        params.width = (int)dpPixels;
                        params.gravity = Gravity.CENTER;
                        emptyText.setLayoutParams(params);
                    }
                    else
                    {
                        emptyText.setTextSize(1);
                    }
                    if(index >= 0)
                    {
                        if(dividerHolder != null)
                        {
                            dividerHolder.addView(emptyText);
                            view.addView(dividerHolder, index);
                        }
                        else
                        {
                            view.addView(emptyText, index);
                        }
                    }
                    else
                    {
                        if(dividerHolder != null)
                        {
                            dividerHolder.addView(emptyText);
                            view.addView(dividerHolder);
                        }
                        else
                        {
                            view.addView(emptyText);
                        }
                    }
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
                    ViewGroup detailGroup = (usingMaterial ? itemDetailCardsLayout.findViewWithTag("group" + groupCount) : itemDetailTable);
                    final View passProgressLayout = this.findViewById(R.id.Item_Detail_Progress_Layout);

                    if(detailGroup != null)
                    {
                        addDivider(detailGroup, false);
                        ((ViewGroup)passProgressLayout.getParent()).removeView(passProgressLayout);
                        detailGroup.addView(passProgressLayout);
                    }
                }
            }

            private void addGroup(int groupId, String[] titleStrings, int[] titleIds)
            {
                boolean addedRow = false;
                boolean usingGroupTitle = (groupId != R.string.empty);
                boolean usingTitleStrings = (titleStrings != null);
                int row = 0;
                int column = 0;
                int dpPixels;
                int titleIndex;
                int titleCount = (usingTitleStrings ? titleStrings.length : titleIds.length);
                int bgColor = Globals.resolveColorID(currentContext, android.R.attr.colorBackground);
                String text;
                Resources res = currentContext.getResources();

                //if can show display
                if(canShow)
                {
                    MaterialCardView groupCard = (usingMaterial ? new MaterialCardView(currentContext) : null);
                    TableLayout currentDetailTable = (usingMaterial ? new TableLayout(new ContextThemeWrapper(currentContext, R.style.DetailTableMaterial)) : itemDetailTable);
                    TableRow groupRow = new TableRow(new ContextThemeWrapper(currentContext, R.style.DetailTableRow));
                    TextView groupText = new TextView(new ContextThemeWrapper(currentContext, (usingMaterial ? R.style.DetailTextMaterial : R.style.DetailText)));
                    TableRow.LayoutParams detailParams;
                    TableRow.LayoutParams groupParams = new TableRow.LayoutParams();
                    TableRow[] detailRows = new TableRow[(titleCount / 2) + (titleCount % 2)];

                    //update group count
                    groupCount++;

                    //if using material
                    if(usingMaterial)
                    {
                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                        //setup group card
                        dpPixels = (int)Globals.dpToPixels(currentContext, 6);
                        cardParams.setMargins(0, 0, 0, dpPixels);
                        groupCard.setCardBackgroundColor(bgColor);
                        groupCard.setContentPadding(dpPixels, dpPixels, dpPixels , dpPixels);
                        groupCard.setRadius(dpPixels);
                        groupCard.setLayoutParams(cardParams);
                        groupCard.setPreventCornerOverlap(true);
                    }
                    else
                    {
                        //add divider and set row color
                        addDivider(currentDetailTable, false);
                        groupRow.setBackgroundColor(bgColor);
                    }
                    currentDetailTable.setStretchAllColumns(true);
                    currentDetailTable.setTag("group" + groupCount);

                    //if using group title
                    if(usingGroupTitle)
                    {
                        //add group text
                        groupParams.span = 5;
                        groupParams.gravity = Gravity.CENTER;
                        groupText.setLayoutParams(groupParams);
                        groupText.setText(groupId);
                        groupRow.addView(groupText);
                        currentDetailTable.addView(groupRow);
                    }

                    //add each title
                    for(titleIndex = 0; titleIndex < titleCount; titleIndex++)
                    {
                        boolean usingPrevious = (titleIndex > 0) && (usingTitleStrings ? (titleStrings[titleIndex - 1] != null) : (titleIds[titleIndex - 1] != R.string.empty));
                        boolean usingCurrent = (usingTitleStrings ? (titleStrings[titleIndex] != null) : (titleIds[titleIndex] != R.string.empty));
                        boolean usingNext = (titleIndex + 1 < titleCount) && (usingTitleStrings ? (titleStrings[titleIndex + 1] != null) : (titleIds[titleIndex + 1] != R.string.empty));
                        boolean extendColumn = false;
                        TextView currentTitleText = new TextView(new ContextThemeWrapper(currentContext, R.style.DetailTitle));
                        TextView currentDetailText = new TextView(new ContextThemeWrapper(currentContext, R.style.DetailText));

                        //if need to start a new row
                        if(column == 0)
                        {
                            //if not an empty row
                            if(usingCurrent || usingNext)
                            {
                                //add divider
                                addDivider(currentDetailTable, false);
                            }

                            //create row
                            detailRows[row] = new TableRow(new ContextThemeWrapper(currentContext, R.style.DetailTableRow));
                        }

                        //set title and text
                        text = (usingTitleStrings ? titleStrings[titleIndex] : res.getString(titleIds[titleIndex])) + ":";
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
                            detailRows[row].setVisibility(usingPrevious || usingCurrent ? View.VISIBLE : View.GONE);
                            addedRow = addedRow || (detailRows[row].getVisibility() == View.VISIBLE);
                            currentDetailTable.addView(detailRows[row++]);
                            column = 0;
                        }
                        else
                        {
                            //add divider and update column
                            if(usingNext)
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

                    //if using material
                    if(usingMaterial)
                    {
                        //add group card
                        groupCard.addView(currentDetailTable);
                        itemDetailCardsLayout.addView(groupCard);
                    }

                    //update group title visibility
                    groupRow.setVisibility(addedRow ? View.VISIBLE : View.GONE);
                }
            }
            public void addGroup(String ...titleStrings)
            {
                addGroup(R.string.empty, titleStrings, null);
            }
            public void addGroup(int groupId, int ...titleIds)
            {
                addGroup(groupId, null, titleIds);
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

            public void setupItemDetailButton(AppCompatImageButton button, final ListBaseAdapter listAdapter, final int pageType, final int itemID, final ListDisplayItem item, final int buttonNum)
            {
                int mapLayerType = ListBaseAdapter.getMapLayerType(noradIds.length > 0 ? noradIds[0] : Universe.IDs.Invalid);

                //if button does not exist
                if(button == null)
                {
                    //stop
                    return;
                }

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
                        FragmentManager manager = Globals.getFragmentManager(currentContext);

                        if(itemDetail3dView != null && manager != null)
                        {
                            FragmentTransaction transaction = manager.beginTransaction();

                            if(visibility == View.VISIBLE)
                            {
                                transaction.show(itemDetail3dView);
                            }
                            else
                            {
                                transaction.hide(itemDetail3dView);
                            }
                            transaction.commit();
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

            public AppCompatImageButton addButton(final int pageType, final int itemID, final ListDisplayItem item, final int buttonNum)
            {
                AppCompatImageButton button = new AppCompatImageButton(new ContextThemeWrapper(currentContext, R.style.DetailButton));

                if(canShow)
                {
                    LinearLayout buttonLayout = itemDetailsGroup.findViewById(R.id.Item_Detail_Button_Layout);
                    LinearLayout itemDetailLayout = itemDetailsGroup.findViewById(R.id.Item_Detail_Layout);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                    if(this.detailButtons.size() == 0)
                    {
                        addDivider(itemDetailLayout, itemDetailLayout.indexOfChild(buttonLayout), false);
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

        protected boolean hasItems;
        protected boolean forSubItems;
        protected boolean loadingItems;
        final protected boolean usingMaterial;
        private boolean enableItemClicks;
        protected int dataID = Integer.MAX_VALUE;
        protected int itemsRefID = -1;
        protected int itemsRefSubId = -1;
        protected int itemsRootViewID = -1;
        protected int widthDp = Globals.getDeviceDp(null, true);
        protected String categoryTitle;
        protected final Context currentContext;
        protected final LayoutInflater listInflater;
        private OnItemClickListener itemClickedListener;
        private OnItemLongClickListener itemLongClickedListener;
        protected OnItemDetailButtonClickListener itemDetailButtonClickListener;
        public View headerView;
        public ViewGroup graphGroup;
        public ViewGroup preview3dGroup;
        public ViewGroup informationGroup;

        public ListBaseAdapter(Context context)
        {
            boolean haveContext = (context != null);

            categoryTitle = null;
            currentContext = context;
            listInflater = (haveContext ? (LayoutInflater)currentContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) : null);
            usingMaterial = Settings.getMaterialTheme(currentContext);
            hasItems = forSubItems = loadingItems = false;
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

        //Returns if using context
        protected boolean haveContext()
        {
            return(currentContext != null);
        }

        //Returns if showing column titles
        protected boolean showColumnTitles(int page)
        {
            return(true);
        }

        //Sets column titles
        protected void setColumnTitles(ViewGroup listColumns, TextView categoryText, int page)
        {
            boolean usingContext = haveContext();
            int[] colors = (usingContext ? Globals.resolveAttributeIDs(currentContext, R.attr.colorAccentVariant, R.attr.colorAccentLightest) : new int[]{Color.BLACK, Color.WHITE});
            colors[1] = (usingContext ? currentContext.getResources().getColor(colors[1]) : colors[1]);

            if(showColumnTitles(page))
            {
                if(usingContext)
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
        protected void onItemNonEditClick(ListDisplayItem item, int pageNum)
        {
            //needs to be overridden
        }

        //Sets item clicks enabled
        public void setItemClicksEnabled(boolean enable)
        {
            enableItemClicks = enable;
        }

        //Sets item selector
        protected void setItemSelector(View itemView, int bgAttrId)
        {
            itemView.setBackground(Globals.getDataItemStateSelector(currentContext, bgAttrId));
        }
        protected void setItemSelector(View itemView)
        {
            setItemSelector(itemView, android.R.attr.colorBackground);
        }

        //Sets item background
        public void setItemBackground(View itemView, boolean isSelected)
        {
            boolean haveItem = (itemView != null);
            Object tag = (haveItem ? itemView.getTag() : null);
            boolean setBackground = (haveItem && (tag == null || !tag.equals("keepBg")) && !(itemView instanceof AppCompatButton));

            //if setting background and and context exists
            if(setBackground && haveContext())
            {
                //set background according to selected state
                itemView.setBackground(isSelected ? Globals.getItemSelectedState(currentContext, false) : Globals.getListItemStateSelector(currentContext, false));
            }
        }

        //Sets view click listeners
        protected void setViewClickListeners(View itemView, final RecyclerView.ViewHolder itemHolder)
        {
            if(!forSubItems)
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
        protected ListDisplayItem getItem(int position)
        {
            //needs to be overridden
            return(null);
        }

        //Returns if loading items
        public boolean isLoadingItems()
        {
            return(loadingItems);
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

        //On update needed listener
        public interface OnUpdateNeededListener
        {
            void updateNeeded();
        }

        //On page set listener
        public interface OnPageSetListener
        {
            void onPageSet(ListFragment page, int pageNum, int subPageNum);
        }

        //On update page listener
        public interface OnUpdatePageListener
        {
            void updatePage(int page, int subPage);
        }

        //On item checked listener
        public interface OnItemCheckChangedListener
        {
            void itemCheckedChanged(int page, ListDisplayItem item);
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
            void onClick(int group, int pageType, int itemID, ListDisplayItem item, int buttonNum);
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

        //On update options menu listener
        public interface OnUpdateOptionsMenuListener
        {
            void update();
        }

        //On adapter set listener
        public interface OnAdapterSetListener
        {
            void setAdapter(ListFragment fragment, int group, int position, ListBaseAdapter adapter);
        }

        //On item selected listener
        public interface OnItemSelectedListener
        {
            void itemSelected(int group, int page, int subPage, int position, boolean selected);
        }

        //On pause listener
        public interface OnPagePauseListener
        {
            void paused(ListFragment page);
        }

        //On resume listener
        public interface OnPageResumeListener
        {
            void resumed(ListFragment page);
        }

        //Creates view
        protected abstract View createView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

        //Sets action mode item properties
        protected abstract boolean setupActionModeItems(MenuItem all, MenuItem none, MenuItem edit, MenuItem delete, MenuItem save, MenuItem sync);

        //Handles action mode select
        protected abstract void onActionModeSelect(boolean all);

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
        protected OnPageSetListener pageSetListener;
        private OnPagePauseListener pagePauseListener;
        private OnPageResumeListener pageResumeListener;
        private Observer<Intent> updateReceiver;

        protected boolean usingMaterial;
        private Menu actionMenu;
        protected Menu optionsMenu;
        private ActionMode actionModeMenu;
        private ListBaseAdapter selectListAdapter;
        protected TextView categoryText;
        protected RecyclerView selectList;
        protected int group;
        protected int pageNum;
        protected boolean inEditMode;
        protected boolean playBarWasRunning;
        protected View listParentView;
        protected PlayBar playBar;
        protected PlayBar scaleBar;
        protected final ArrayList<ListDisplayItem> selectedItems;

        public ListFragment()
        {
            super();
            group = pageNum = -1;
            inEditMode = playBarWasRunning = false;
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

            usingMaterial = Settings.getMaterialTheme(context);
            updateReceiver = createLocalBroadcastReceiver();
            if(context != null)
            {
                UpdateService.observe(context, updateReceiver);
            }

            //this.setRetainInstance(true);       //keep adapter on orientation changes
            this.setHasOptionsMenu(true);
        }

        @Override
        public void onPause()
        {
            super.onPause();

            if(playBar != null)
            {
                playBarWasRunning = playBar.stopPlayTimer(false);
            }
            if(pagePauseListener != null)
            {
                pagePauseListener.paused(this);
            }
        }

        @Override
        public void onResume()
        {
            super.onResume();

            if(playBarWasRunning && playBar != null)
            {
                playBar.setPlayActivity(this.getActivity());
                playBar.start();
                playBarWasRunning = false;
            }
            if(pageResumeListener != null)
            {
                pageResumeListener.resumed(this);
            }
        }

        @Override
        public void onDestroy()
        {
            cancelEditMode();

            if(playBar != null)
            {
                playBar.stopPlayTimer();
                playBarWasRunning = false;
            }

            if(updateReceiver != null)
            {
                UpdateService.removeObserver(updateReceiver);
            }

            super.onDestroy();
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            //create view
            View newView = createView(inflater, container, savedInstanceState);

            //if listener is set
            if(pageSetListener != null)
            {
                //send event
                pageSetListener.onPageSet(this, getPageParam(), getSubPageParam());
            }

            //return view
            return(newView);
        }
        protected View onCreateView(LayoutInflater inflater, ViewGroup container, ListBaseAdapter listAdapter, int grp, int page, boolean useListColumns)
        {
            int viewIndex;
            View header;
            View listColumns;
            Context context = this.getContext();
            ViewGroup rootView = (ViewGroup)inflater.inflate((usingMaterial ? R.layout.list_material_view : R.layout.list_view), container, false);

            //remember group and page
            group = grp;
            pageNum = page;

            //setup header
            header = Globals.replaceView(R.id.List_Header, R.layout.header_text_view, inflater, rootView);

            //setup columns
            listColumns = rootView.findViewById(R.id.List_View_Columns);
            viewIndex = rootView.indexOfChild(listColumns);
            rootView.removeViewAt(viewIndex);
            if(useListColumns && listAdapter != null && listAdapter.itemsRefID > -1)
            {
                listColumns = inflater.inflate(listAdapter.forSubItems ? listAdapter.itemsRefSubId : listAdapter.itemsRefID, rootView, false);
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
                    adapterSetListener.setAdapter(this, group, pageNum, selectListAdapter);
                }
                selectListAdapter.setOnItemClickedListener(createOnItemClickListener());
                selectListAdapter.setOnItemLongClickedListener(createOnItemLongClickListener());
                selectListAdapter.setOnItemDetailButtonClickedListener(createOnItemDetailButtonClickListener());
                if(selectListAdapter.forSubItems)
                {
                    rootView.setBackgroundColor(Globals.resolveColorID(context, R.attr.pageHighlightBackground));
                }
            }

            //return view
            return(rootView);
        }
        protected View onCreateView(LayoutInflater inflater, ViewGroup container, ListBaseAdapter listAdapter, int grp, int page)
        {
            return(onCreateView(inflater, container, listAdapter, grp, page, true));
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
            int dividerInsetPx;
            boolean darkTheme;
            Object tagObject;
            DividerItemDecoration verticalDivider;
            DividerItemDecoration horizontalDivider;
            MaterialDividerItemDecoration horizontalMaterialDivider;

            //if context and list are set
            if(context != null && selectList != null)
            {
                //get theme, desired columns, and sizes
                darkTheme = Settings.getDarkTheme(context);
                columns = getListColumns(context, page);

                //if there are existing columns
                tagObject = selectList.getTag();
                if(tagObject != null)
                {
                    //remove all column decorations
                    currentColumns = (int)tagObject;
                    selectList.removeItemDecorationAt(0);
                    if(currentColumns > 1 && !usingMaterial)
                    {
                        selectList.removeItemDecorationAt(0);
                    }
                }

                //add columns and decorations
                selectList.setLayoutManager(columns > 1 ? (new GridLayoutManager(context, columns)) : (new LinearLayoutManager(context)));
                if(usingMaterial)
                {
                    dividerInsetPx = (int)Globals.dpToPixels(context, 16);
                    horizontalMaterialDivider = new MaterialDividerItemDecoration(context, MaterialDividerItemDecoration.VERTICAL);
                    horizontalMaterialDivider.setDividerColorResource(context, darkTheme ? R.color.dark_gray : R.color.light_gray);
                    horizontalMaterialDivider.setDividerInsetStart(dividerInsetPx);
                    horizontalMaterialDivider.setDividerInsetEnd(dividerInsetPx);
                    selectList.addItemDecoration(horizontalMaterialDivider);
                }
                else
                {
                    horizontalDivider = new DividerItemDecoration(context, LinearLayoutManager.VERTICAL);
                    horizontalDivider.setDrawable(Globals.getDrawable(context, (darkTheme ? R.drawable.divider_horizontal_dark : R.drawable.divider_horizontal_light)));
                    selectList.addItemDecoration(horizontalDivider);
                }
                if(columns > 1 && !usingMaterial)
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

                //if columns and adapter are set
                if(listColumns != null && selectListAdapter != null)
                {
                    //update column titles
                    selectListAdapter.setColumnTitles((ViewGroup)listColumns, categoryText, pageNum);
                }
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu)
        {
            //create action menu
            mode.getMenuInflater().inflate(R.menu.menu_action_selectable_layout, menu);
            actionMenu = menu;
            actionModeMenu = mode;
            return(true);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu)
        {
            return(setupActionModeItems(menu.findItem(R.id.menu_all), menu.findItem(R.id.menu_none), menu.findItem(R.id.menu_edit), menu.findItem(R.id.menu_delete), menu.findItem(R.id.menu_save), menu.findItem(R.id.menu_update)));
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
            boolean isAll = (id == R.id.menu_all);

            //handle item
            if(isAll || id == R.id.menu_none)
            {
                onActionModeSelect(isAll);
            }
            else if(id == R.id.menu_delete)
            {
                onActionModeDelete();
            }
            else if(id == R.id.menu_save)
            {
                onActionModeSave();
            }
            else if(id == R.id.menu_update)
            {
                onActionModeSync();
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
        }

        //Updates the options menu
        public void updateOptionsMenu()
        {
            //if menu exists
            if(optionsMenu != null)
            {
                //refresh menu
                onPrepareOptionsMenu(optionsMenu);
            }
        }

        //Sets the parent view
        public void setParentView(View parentView)
        {
            listParentView = parentView;
        }

        //Sets item clicked enabled
        public void setItemClicksEnabled(boolean enabled)
        {
            //if adapter exists
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
            if(position >= 0 && selectListAdapter != null && position < selectListAdapter.getItemCount())
            {
                //remember current item and view
                ListDisplayItem currentItem = selectListAdapter.getItem(position);
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

            //if adapter exists
            if(selectListAdapter != null)
            {
                //go through each item
                for(index = 0; index < selectListAdapter.getItemCount(); index++)
                {
                    setItemSelected(index, selected);
                }
            }
        }

        //Sets checked state of an item
        public void setItemChecked(int position)
        {
            int index;
            boolean wasChecked;

            //if adapter exists
            if(selectListAdapter != null)
            {
                //go through each item
                for(index = 0; index < selectListAdapter.getItemCount(); index++)
                {
                    //remember current item
                    ListDisplayItem currentItem = selectListAdapter.getItem(index);

                    //update item
                    wasChecked = currentItem.isChecked;
                    currentItem.setChecked(index == position);
                    if(wasChecked != currentItem.isChecked)
                    {
                        onItemCheckChanged(currentItem);
                    }
                }
            }
        }

        //Gets positions of given ID
        protected int getPosition(int id)
        {
            int position;
            int count = getListItemCount();

            //try to find position
            for(position = 0; position < count; position++)
            {
                //if current item ID matches
                ListDisplayItem currentItem = selectListAdapter.getItem(position);
                if(currentItem != null && currentItem.id == id)
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

        //Gets list
        protected RecyclerView getList()
        {
            return(selectList);
        }

        //Gets if list is loading items
        public boolean isListLoadingItems()
        {
            return(selectListAdapter != null && selectListAdapter.isLoadingItems());
        }

        //Gets list item count
        public int getListItemCount()
        {
            return(selectListAdapter != null ? selectListAdapter.getItemCount() : 0);
        }

        //Gets selected items
        public ArrayList<ListDisplayItem> getSelectedItems()
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
                    ListDisplayItem currentItem = (selectListAdapter != null ? selectListAdapter.getItem(position) : null);
                    if(currentItem != null)
                    {
                        //if not in edit mode and item can be checked
                        if(!inEditMode && currentItem.canCheck && currentItem.checkBoxView != null && !currentItem.isChecked)
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
                    ListDisplayItem currentItem = (selectListAdapter != null ? selectListAdapter.getItem(position) : null);

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
                public void onClick(int pageType, int itemID, ListDisplayItem item, int buttonNum)
                {
                    //if listener is set
                    if(itemDetailButtonClickListener != null)
                    {
                        itemDetailButtonClickListener.onClick(group, pageType, itemID, item, buttonNum);
                    }
                }
            });
        }

        //Creates an orientation changed listener
        protected OnOrientationChangedListener createOnOrientationChangedListener(final RecyclerView list, final ListBaseAdapter listAdapter, final int page)
        {
            return(null);
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
                        final CircularProgressIndicator graphProgress = listAdapter.graphGroup.findViewById(R.id.Item_Detail_Graph_Progress);
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
                                elevationGraph.setData(timePoints, elevationPoints, (usingOrbital2 ? elevation2Points : null), MainActivity.getTimeZone());
                                elevationGraph.setRangeX(timePoints.get(0), timePoints.get(timePoints.size() - 1), 6);
                                elevationGraph.setRangeY(0, 90, 6);
                                elevationGraph.setSelectedType(Graph.SelectType.Image);
                                elevationGraph.setSelectedImage(Globals.getBitmap(activity, Globals.getOrbitalIconId(activity, id1, orbital1.getOrbitalType()), id1 > 0));
                                elevationGraph.setSelectedImage2(usingOrbital2 ? Globals.getBitmap(activity, Globals.getOrbitalIconId(activity, id2, orbital2.getOrbitalType()), id2 > 0) : null);
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
                        final CircularProgressIndicator preview3dProgress = listAdapter.preview3dGroup.findViewById(R.id.Item_Detail_3d_Progress);
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
                    if(listAdapter != null && listAdapter.informationGroup != null && activity != null)
                    {
                        //get displays
                        final CircularProgressIndicator infoProgress = listAdapter.informationGroup.findViewById(R.id.Item_Detail_Info_Progress);
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

        //Creates and on update options menu listener
        protected OnUpdateOptionsMenuListener createOnUpdateOptionsMenuListener()
        {
            return(new OnUpdateOptionsMenuListener()
            {
                @Override public void update()
                {
                    updateOptionsMenu();
                }
            });
        }

        //Creates a local broadcast listener
        private Observer<Intent> createLocalBroadcastReceiver()
        {
            return(new Observer<Intent>()
            {
                @Override
                public void onChanged(Intent intent)
                {
                    //if intent isn't set
                    if(intent == null)
                    {
                        //stop
                        return;
                    }

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

        //Sets on page set listener
        public void setOnPageSetListener(OnPageSetListener listener)
        {
            pageSetListener = listener;
        }

        //Sets on page paused listener
        public void setOnPagePausedListener(OnPagePauseListener listener)
        {
            pagePauseListener = listener;
        }

        //Sets on page resumed listener
        public void setOnPageResumeListener(OnPageResumeListener listener)
        {
            pageResumeListener = listener;
        }

        //Sets on item checked listener
        public void setOnItemCheckChangedListener(OnItemCheckChangedListener listener)
        {
            itemCheckChangedListener = listener;
        }

        //Calls on item check changed listener
        private void onItemCheckChanged(ListDisplayItem item)
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
    }

    //Page adapter
    public static class ListFragmentAdapter extends FragmentStatePagerAdapter
    {
        protected final int group;
        protected final Context currentContext;
        protected final int[] subPage;
        private final View currentParentVIew;
        private final ListFragment.OnUpdateNeededListener updateNeededListener;
        private final ListFragment.OnItemSelectedListener itemSelectedListener;
        private final ListFragment.OnItemCheckChangedListener itemCheckChangedListener;
        private final ListFragment.OnItemDetailButtonClickListener itemDetailButtonClickListener;
        private final ListFragment.OnAdapterSetListener adapterSetListener;
        protected final ListFragment.OnPageSetListener pageSetListener;
        private final ListFragment.OnPageResumeListener pageResumeListener;
        private final Selectable.ListFragment.OnOrientationChangedListener[] orientationChangedListeners = new Selectable.ListFragment.OnOrientationChangedListener[getCount()];
        private final Selectable.ListFragment.OnGraphChangedListener[] graphChangedListeners = new ListFragment.OnGraphChangedListener[getCount()];
        private final Selectable.ListFragment.OnPreview3dChangedListener[] preview3dChangedListeners = new ListFragment.OnPreview3dChangedListener[getCount()];
        private final Selectable.ListFragment.OnInformationChangedListener[] informationChangedListeners = new Selectable.ListFragment.OnInformationChangedListener[getCount()];
        private final Selectable.ListFragment.OnUpdateOptionsMenuListener[] updateOptionsMenuListeners = new Selectable.ListFragment.OnUpdateOptionsMenuListener[getCount()];

        public ListFragmentAdapter(FragmentManager fm, View parentView, ListFragment.OnItemSelectedListener selectedListener, ListFragment.OnUpdateNeededListener updateListener, ListFragment.OnItemCheckChangedListener checkChangedListener, ListFragment.OnItemDetailButtonClickListener detailButtonClickListener, ListFragment.OnAdapterSetListener adapterListener, ListFragment.OnPageSetListener setListener, ListFragment.OnPageResumeListener resumeListener, int grp, int[] subPg)
        {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            currentParentVIew = parentView;
            currentContext = (parentView != null ? parentView.getContext() : null);
            group = grp;
            subPage = subPg;
            itemSelectedListener = selectedListener;
            updateNeededListener = updateListener;
            itemCheckChangedListener = checkChangedListener;
            itemDetailButtonClickListener = detailButtonClickListener;
            adapterSetListener = adapterListener;
            pageSetListener = new ListFragment.OnPageSetListener()
            {
                @Override
                public void onPageSet(ListFragment page, int pageNum, int subPageNum)
                {
                    RecyclerView pageList = page.getList();
                    ListBaseAdapter pageAdapter = page.getAdapter();

                    ListFragmentAdapter.this.setOrientationChangedListener(pageNum, page.createOnOrientationChangedListener(pageList, pageAdapter, pageNum));
                    ListFragmentAdapter.this.setGraphChangedListener(pageNum, page.createOnGraphChangedListener(pageAdapter));
                    ListFragmentAdapter.this.setPreview3dChangedListener(pageNum, page.createOnPreview3dChangedListener(pageAdapter));
                    ListFragmentAdapter.this.setInformationChangedListener(pageNum, page.createOnInformationChangedListener(pageAdapter));
                    ListFragmentAdapter.this.setOnUpdateOptionsMenuListener(pageNum, page.createOnUpdateOptionsMenuListener());

                    if(setListener != null)
                    {
                        setListener.onPageSet(page, pageNum, subPageNum);
                    }
                }
            };
            pageResumeListener = resumeListener;
        }

        @Override @NonNull
        public Fragment getItem(int position)
        {
            return(new Fragment());
        }
        protected Fragment getItem(int group, int position, int subPosition, ListFragment newPage)
        {
            Bundle params = newPage.getArguments();

            if(params == null)
            {
                params = new Bundle();
            }
            params.putInt(ParamTypes.Group, group);
            params.putInt(ParamTypes.PageNumber, (group == MainActivity.Groups.Current ? Current.PageType.Combined : position));
            params.putInt(ParamTypes.SubPageNumber, subPosition);

            newPage.setArguments(params);
            return(setupItem(newPage));
        }

        //Sets up item
        private Fragment setupItem(ListFragment newPage)
        {
            newPage.setParentView(currentParentVIew);
            newPage.setOnPageSetListener(pageSetListener);
            newPage.setOnItemSelectedListener(itemSelectedListener);
            newPage.setOnUpdateNeededListener(updateNeededListener);
            newPage.setOnItemCheckChangedListener(itemCheckChangedListener);
            newPage.setOnDetailButtonClickListener(itemDetailButtonClickListener);
            newPage.setOnSetAdapterListener(adapterSetListener);
            newPage.setOnPageResumeListener(pageResumeListener);

            return(newPage);
        }

        @Override
        public @NonNull Object instantiateItem(@NonNull ViewGroup container, int position)
        {
            int pageNum;
            int subPageNum;
            boolean needCallPageSet;
            Object item = super.instantiateItem(container, position);
            ListFragment page = null;

            //if a page with ListFragment base
            if(item instanceof ListFragment)
            {
                //set page properties
                page = (ListFragment)item;
                needCallPageSet = (page.pageSetListener == null);
                pageNum = page.getPageParam();
                subPageNum = page.getSubPageParam();
                setupItem(page);

                //if need to call page set listener and it exists
                if(needCallPageSet && pageSetListener != null)
                {
                    //send event
                    pageSetListener.onPageSet(page, pageNum, subPageNum);
                }
            }

            //if page exists, return it, otherwise item
            return(page != null ? page : item);
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

        //Returns the given page
        public ListFragment getPage(ViewGroup container, int pageNum)
        {
            return(pageNum < this.getCount() ? (ListFragment)this.instantiateItem(container, pageNum) : null);
        }

        //Returns selected items
        public ArrayList<ListDisplayItem> getSelectedItems(ViewGroup container, int pageNum)
        {
            ListFragment page = getPage(container, pageNum);
            return(page != null ? page.getSelectedItems() : new ArrayList<>(0));
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

        //Returns if page is a valid number
        private boolean validPage(int position)
        {
            return(position >= 0 && position < getCount());
        }

        //Sets orientation changed listener for the given page
        public void setOrientationChangedListener(int position, Selectable.ListFragment.OnOrientationChangedListener listener)
        {
            //if a valid page
            if(validPage(position))
            {
                //set listener
                orientationChangedListeners[position] = listener;
            }
        }

        //Calls orientation changed listener for the given page
        public void notifyOrientationChangedListener(int position)
        {
            //if a valid page and listener exists
            if(validPage(position) && orientationChangedListeners[position] != null)
            {
                //call listener
                orientationChangedListeners[position].orientationChanged();
            }
        }

        //Sets graph changed listener for the given page
        public void setGraphChangedListener(int position, Selectable.ListFragment.OnGraphChangedListener listener)
        {
            //if a valid page
            if(validPage(position))
            {
                //set listener
                graphChangedListeners[position] = listener;
            }
        }

        //Call graph changed listener for the given page
        public void notifyGraphChanged(int position, Database.SatelliteData orbital, ArrayList<CalculateViewsTask.OrbitalView> pathPoints, Database.SatelliteData orbital2, ArrayList<CalculateViewsTask.OrbitalView> path2Points)
        {
            //if a valid page and listener exists
            if(validPage(position) && graphChangedListeners[position] != null)
            {
                //call listener
                graphChangedListeners[position].graphChanged(orbital, pathPoints, orbital2, path2Points);
            }
        }

        //Sets preview 3d changed listener for the given page
        public void setPreview3dChangedListener(int position, Selectable.ListFragment.OnPreview3dChangedListener listener)
        {
            //if a valid page
            if(validPage(position))
            {
                //set listener
                preview3dChangedListeners[position] = listener;
            }
        }

        //Call preview 3d changed listener for the given page
        public void notifyPreview3dChanged(int position, int noradId)
        {
            //if a valid page and listener exists
            if(validPage(position) && preview3dChangedListeners[position] != null)
            {
                //call listener
                preview3dChangedListeners[position].preview3dChanged(noradId);
            }
        }

        //Sets information changed listener for the given page
        public void setInformationChangedListener(int position, Selectable.ListFragment.OnInformationChangedListener listener)
        {
            //if a valid page
            if(validPage(position))
            {
                //set listener
                informationChangedListeners[position] = listener;
            }
        }

        //Calls information changed listener for the given page
        public void notifyInformationChanged(int position, Spanned text)
        {
            //if a valid page and listener exists
            if(validPage(position) && informationChangedListeners[position] != null)
            {
                //call listener
                informationChangedListeners[position].informationChanged(text);
            }
        }

        //Sets on update options menu listener for the given page
        public void setOnUpdateOptionsMenuListener(int position, Selectable.ListFragment.OnUpdateOptionsMenuListener listener)
        {
            //if a valid page
            if(validPage(position))
            {
                //set listener
                updateOptionsMenuListeners[position] = listener;
            }
        }

        //Calls update options menu listener for the given page
        public void notifyUpdateOptionsMenu(int position)
        {
            //if a valid page and listener exists
            if(validPage(position) && updateOptionsMenuListeners[position] != null)
            {
                //call listener
                updateOptionsMenuListeners[position].update();
            }
        }
    }
}
