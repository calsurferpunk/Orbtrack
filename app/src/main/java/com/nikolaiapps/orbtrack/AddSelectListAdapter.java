package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.res.Resources;
import androidx.appcompat.widget.AppCompatImageView;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


public class AddSelectListAdapter extends BaseAdapter
{
    static abstract class SelectType
    {
        static final byte SatelliteSource = 0;
        static final byte FileSource = 1;
        static final byte Location = 2;
        static final byte SaveAs = 3;
        static final byte EditAccount = 4;
        static final byte AddAccount = 5;
        static final byte Edit = 6;
    }

    static abstract class SatelliteSourceType
    {
        static final int Online = 0;
        static final int File = 1;
        static final int Manual = 2;
    }

    static abstract class FileSourceType
    {
        static final int SDCard = 0;
        static final int GoogleDrive = 1;
        static final int Dropbox = 2;
        static final int Others = 3;
    }

    static abstract class LocationSourceType
    {
        static final int Current = 0;
        static final int Custom = 1;
        static final int Search = 2;
    }

    static abstract class EditAccountType
    {
        static final int Edit = 0;
        static final int Remove = 1;
    }

    static abstract class EditType
    {
        static final int Color = 0;
        static final int Visibility = 1;
        static final int Details = 2;
    }

    public interface OnItemClickListener
    {
        void onItemClick(int which);
    }

    private final byte selectType;
    private final LayoutInflater listInflater;
    private String[] selections;
    private Integer[] selectionIds;

    private AddSelectListAdapter(Context context, byte listSelectType)
    {
        listInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        selectType = listSelectType;
    }
    public AddSelectListAdapter(Context context, byte listSelectType, int extra)
    {
        this(context, listSelectType);

        int updateSource;
        Resources res = context.getResources();

        switch(selectType)
        {
            case SelectType.Location:
                selections = new String[]{res.getString(R.string.title_current), res.getString(R.string.title_custom), res.getString(R.string.title_search)};
                break;

            case SelectType.SaveAs:
                selections = new String[]{res.getString(R.string.title_2_line_element), res.getString(R.string.title_database_backup)};
                break;

            case SelectType.FileSource:
                if(Build.VERSION.SDK_INT >= 29)
                {
                    selections = new String[]{Globals.FileLocationType.GoogleDrive, Globals.FileLocationType.Dropbox, res.getString(R.string.title_other)};
                }
                else
                {
                    selections = new String[]{res.getString(R.string.title_sd_card), Globals.FileLocationType.GoogleDrive, Globals.FileLocationType.Dropbox, res.getString(R.string.title_other)};
                }
                break;

            case SelectType.EditAccount:
                selections = (extra != Globals.AccountType.GoogleDrive && extra != Globals.AccountType.Dropbox ? new String[]{res.getString(R.string.title_edit), res.getString(R.string.title_remove)} : new String[]{res.getString(R.string.title_remove)});
                break;

            case SelectType.Edit:
                selections = new String[]{res.getString(R.string.title_color), res.getString(R.string.title_visible), res.getString(R.string.title_details)};
                break;

            case SelectType.SatelliteSource:
            default:
                updateSource = Settings.getSatelliteCatalogSource(context);
                selections = new String[]{(updateSource == Database.UpdateSource.N2YO ? Globals.Strings.N2YO : updateSource == Database.UpdateSource.SpaceTrack ? Globals.Strings.SpaceTrack : updateSource == Database.UpdateSource.Celestrak ? Globals.Strings.Celestrak : updateSource == Database.UpdateSource.HeavensAbove ? Globals.Strings.HeavensAbove : res.getString(R.string.title_unknown)), res.getString(R.string.title_file), res.getString(R.string.title_manual)};
                break;
        }
    }
    public AddSelectListAdapter(Context context, byte listSelectType, Integer[] itemIds)
    {
        this(context, listSelectType);

        int index;

        //if IDs are set
        if(itemIds != null)
        {
            //set Ids and selections
            selectionIds = itemIds;
            selections = new String[selectionIds.length];

            //if adding an account
            if(selectType == SelectType.AddAccount)
            {
                //go through each account
                for(index = 0; index < selectionIds.length; index++)
                {
                    //set selection based on account
                    switch(selectionIds[index])
                    {
                        case Globals.AccountType.GoogleDrive:
                            selections[index] = Globals.FileLocationType.GoogleDrive;
                            break;

                        case Globals.AccountType.Dropbox:
                            selections[index] = Globals.FileLocationType.Dropbox;
                            break;

                        case Globals.AccountType.SpaceTrack:
                        default:
                            selections[index] = Globals.Strings.SpaceTrack;
                            break;
                    }
                }
            }
        }
    }

    @Override
    public int getCount()
    {
        return(selections.length);
    }

    @Override
    public Object getItem(int position)
    {
        return(selections[position]);
    }

    @Override
    public long getItemId(int position)
    {
        return(-1);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        int imageId = -1;
        boolean useTheme = true;
        Context context = (parent != null ? parent.getContext() : null);
        TextView addSourceItemText;
        AppCompatImageView addSourceItemImage;

        if(convertView == null)
        {
            convertView = listInflater.inflate(R.layout.list_add_select_item, parent, false);
        }
        convertView.setBackground(Globals.getListItemStateSelector(context, true));
        addSourceItemImage = convertView.findViewById(R.id.Add_Select_Item_Image);
        addSourceItemText = convertView.findViewById(R.id.Add_Select_Item_Text);

        switch(selectType)
        {
            case SelectType.FileSource:
                switch(position + (Build.VERSION.SDK_INT >= 29 ? 1 : 0))
                {
                    case FileSourceType.GoogleDrive:
                        imageId = R.drawable.org_google_drive;
                        useTheme = false;
                        break;

                    case FileSourceType.Dropbox:
                        imageId = R.drawable.org_dropbox;
                        useTheme = false;
                        break;

                    case FileSourceType.Others:
                        imageId = R.drawable.ic_folder_open_black;
                        break;

                    case FileSourceType.SDCard:
                    default:
                        imageId = R.drawable.ic_sd_card_black;
                        break;

                }
                break;

            case SelectType.Location:
                switch(position)
                {
                    case LocationSourceType.Current:
                        imageId = R.drawable.ic_my_location_black;
                        break;

                    case LocationSourceType.Custom:
                        imageId = R.drawable.ic_person_pin_circle_black;
                        break;

                    case LocationSourceType.Search:
                    default:
                        imageId = R.drawable.ic_search_black;
                        break;
                }
                break;

            case SelectType.SaveAs:
                switch(position)
                {
                    case Globals.FileType.Backup:
                        imageId = R.drawable.ic_storage_black;
                        break;

                    case Globals.FileType.TLEs:
                    default:
                        imageId = Settings.getSatelliteIconImageId(context);
                        useTheme = Settings.getSatelliteIconImageIsThemeable(context);
                        break;
                }
                break;

            case SelectType.EditAccount:
                switch(position)
                {
                    case EditAccountType.Edit:
                        //if more than 1 selection
                        if(selections.length > 1)
                        {
                            imageId = R.drawable.ic_mode_edit_black;
                            break;
                        }
                        //else fall through

                    case EditAccountType.Remove:
                    default:
                        imageId = R.drawable.ic_delete_white;
                        break;
                }
                break;

            case SelectType.AddAccount:
                if(selectionIds != null && position < selectionIds.length)
                {
                    useTheme = false;

                    switch(selectionIds[position])
                    {
                        case Globals.AccountType.GoogleDrive:
                            imageId = R.drawable.org_google_drive;
                            break;

                        case Globals.AccountType.Dropbox:
                            imageId = R.drawable.org_dropbox;
                            break;

                        case Globals.AccountType.SpaceTrack:
                        default:
                            imageId = R.drawable.org_space_track;
                            break;
                    }
                }
                break;

            case SelectType.Edit:
                switch(position)
                {
                    case EditType.Color:
                        imageId = R.drawable.ic_color_white;
                        break;

                    case EditType.Visibility:
                        imageId = R.drawable.ic_remove_red_eye_white;
                        break;

                    case EditType.Details:
                    default:
                        imageId = R.drawable.ic_list_white;
                        break;
                }
                break;

            case SelectType.SatelliteSource:
                switch(position)
                {
                    case SatelliteSourceType.Online:
                        imageId = R.drawable.ic_wifi_black;
                        break;

                    case SatelliteSourceType.File:
                        imageId = R.drawable.ic_insert_drive_file_black;
                        break;

                    case SatelliteSourceType.Manual:
                    default:
                        imageId = R.drawable.ic_mode_edit_black;
                        break;
                }
                break;
        }
        if(imageId != -1)
        {
            addSourceItemImage.setBackgroundDrawable(Globals.getDrawable(context, imageId, useTheme));
        }
        addSourceItemText.setText(selections[position]);

        return(convertView);
    }
}
