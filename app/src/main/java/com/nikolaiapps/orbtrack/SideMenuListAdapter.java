package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import java.util.ArrayList;


public class SideMenuListAdapter extends BaseExpandableListAdapter
{
    public static class Item extends Selectable.ListDisplayItem
    {
        private final Drawable icon;
        private final String text;

        public Item(String txt, Drawable icn)
        {
            super(-1, -1);
            icon = icn;
            text = txt;
        }
    }

    public static class Group
    {
        private final Drawable icon;
        private final String text;
        private final Item[] items;

        public Group(Context context, String txt, int iconId, Item[] groupItems)
        {
            icon = Globals.getDrawable(context, iconId, true);
            text = txt;
            items = groupItems;
        }
    }

    private final LayoutInflater menuInflater;
    private final ArrayList<Group> groups;

    public SideMenuListAdapter(Context context, ArrayList<Group> menuGroups)
    {
        menuInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        groups = menuGroups;
    }

    @Override
    public int getGroupCount()
    {
        return(groups.size());
    }

    @Override
    public int getChildrenCount(int groupPosition)
    {
        return(groups.get(groupPosition).items.length);
    }

    @Override
    public Object getGroup(int groupPosition)
    {
        return(groups.get(groupPosition));
    }

    @Override
    public Object getChild(int groupPosition, int childPosition)
    {
        return(groups.get(groupPosition).items[childPosition]);
    }

    @Override
    public long getGroupId(int groupPosition)
    {
        return(-1);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition)
    {
        return(-1);
    }

    @Override
    public boolean hasStableIds()
    {
        return(true);
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
    {
        TextView groupTitleText;
        AppCompatImageView groupTitleImage;
        AppCompatImageView groupIndicatorImage;
        Group group = groups.get(groupPosition);

        if(convertView == null)
        {
            convertView = menuInflater.inflate(R.layout.side_menu_list_group, parent, false);
        }
        groupTitleImage = convertView.findViewById(R.id.Group_Title_Image);
        groupIndicatorImage = convertView.findViewById(R.id.Group_Indicator_Image);
        groupTitleText = convertView.findViewById(R.id.Group_Title_Text);

        groupTitleImage.setBackgroundDrawable(group.icon);
        groupTitleText.setText(group.text);
        if(groupIndicatorImage != null)
        {
            groupIndicatorImage.setImageDrawable(Globals.getDrawable(menuInflater.getContext(), (isExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more), true));
        }

        return(convertView);
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
    {
        View childView = convertView;
        AppCompatImageView titleImage;
        TextView titleTextLbl;
        Item item;

        if(childView == null)
        {
            childView = menuInflater.inflate(R.layout.side_menu_list_item, parent, false);
        }
        titleImage = childView.findViewById(R.id.Item_Title_Image);
        titleTextLbl = childView.findViewById(R.id.Item_Title_Text);
        childView.setBackground(Globals.getMenuItemStateSelector(menuInflater.getContext()));
        item = groups.get(groupPosition).items[childPosition];

        titleImage.setBackgroundDrawable(item.icon);
        titleTextLbl.setText(item.text);

        return(childView);
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition)
    {
        return(true);
    }

    public static class ItemAdapter extends Selectable.ListBaseAdapter
    {
        private final Item[] items;

        public static class ItemHolder extends RecyclerView.ViewHolder
        {
            final AppCompatImageView itemImage;
            final TextView itemTextLbl;

            public ItemHolder(View itemView, int itemImageId, int itemTextLblId)
            {
                super(itemView);
                itemImage = itemView.findViewById(itemImageId);
                itemTextLbl = itemView.findViewById(itemTextLblId);
            }
        }

        public ItemAdapter(Context context, ArrayList<Item> listItems)
        {
            super(context);
            items = listItems.toArray(new Item[0]);
        }

        @Override @NonNull
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            Context context = parent.getContext();
            View itemView = LayoutInflater.from(context).inflate(R.layout.side_menu_list_group, parent, false);
            ItemHolder itemHolder = new ItemHolder(itemView, R.id.Group_Title_Image, R.id.Group_Title_Text);

            setItemSelector(itemView, R.attr.pageHighlightBackground);
            setViewClickListeners(itemView, itemHolder);
            return(itemHolder);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
        {
            Item currentItem = items[position];
            ItemHolder itemHolder = (ItemHolder)holder;

            //set displays
            itemHolder.itemImage.setBackgroundDrawable(currentItem.icon);
            itemHolder.itemTextLbl.setText(currentItem.text);
        }

        @Override
        public int getItemCount()
        {
            return(items != null ? items.length : 0);
        }
    }
}
