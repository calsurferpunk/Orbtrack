package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;


public class CustomSearchView extends SearchView
{
    public interface OnSearchStateChangedListener
    {
        void onSearchStateChanged(boolean visible);
    }

    private OnSearchStateChangedListener searchStateChangedListener;

    public CustomSearchView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        SearchAutoComplete searchText = this.findViewById(R.id.search_src_text);

        if(searchText != null)
        {
            searchText.setTextColor(Globals.resolveColorID(context, R.attr.titleTextColor));
        }
    }
    public CustomSearchView(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, R.attr.searchViewStyle);
    }
    public CustomSearchView(@NonNull Context context)
    {
        this(context, null);
    }

    public void setOnSearchStateChangedListener(OnSearchStateChangedListener listener)
    {
        searchStateChangedListener = listener;
    }

    @Override
    public void onActionViewCollapsed()
    {
        if(searchStateChangedListener != null)
        {
            searchStateChangedListener.onSearchStateChanged(false);
        }

        super.onActionViewCollapsed();
    }

    @Override
    public void onActionViewExpanded()
    {
        if(searchStateChangedListener != null)
        {
            searchStateChangedListener.onSearchStateChanged(true);
        }

        super.onActionViewExpanded();
    }
}