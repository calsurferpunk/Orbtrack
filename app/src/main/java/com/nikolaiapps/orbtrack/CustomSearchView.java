package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Filter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;


public class CustomSearchView extends SearchView
{
    public interface OnSearchStateChangedListener
    {
        void onSearchStateChanged(boolean visible);
    }

    public interface OnSuggestionSelectedListener
    {
        void onSuggestionSelected(IconSpinner.Item item);
    }

    private IconSpinner.CustomAdapter adapter;
    private OnSearchStateChangedListener searchStateChangedListener;

    public CustomSearchView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        //get text
        SearchAutoComplete searchText = this.findViewById(R.id.search_src_text);

        //if text exists
        if(searchText != null)
        {
            //set text color
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

    public void setOnSuggestionSelectedListener(OnSuggestionSelectedListener listener)
    {
        //use suggestion listener for listener
        setOnSuggestionListener(new OnSuggestionListener()
        {
            private void handleSuggestedSelected(int position)
            {
                //if listener is set
                if(listener != null)
                {
                    //send selected item
                    listener.onSuggestionSelected(adapter != null ? (IconSpinner.Item)adapter.getItem(position) : null);
                }

                //clear text/query
                CustomSearchView.this.setQuery("", false);
            }

            @Override
            public boolean onSuggestionSelect(int position)
            {
                handleSuggestedSelected(position);
                return(true);
            }

            @Override
            public boolean onSuggestionClick(int position)
            {
                handleSuggestedSelected(position);
                return(true);
            }
        });
    }

    @Override
    public void onActionViewCollapsed()
    {
        //if listener exists
        if(searchStateChangedListener != null)
        {
            //send closed event
            searchStateChangedListener.onSearchStateChanged(false);
        }

        super.onActionViewCollapsed();
    }

    @Override
    public void onActionViewExpanded()
    {
        //if listener exists
        if(searchStateChangedListener != null)
        {
            //send opened event
            searchStateChangedListener.onSearchStateChanged(true);
        }

        super.onActionViewExpanded();
    }

    public boolean close()
    {
        //if visible
        if(getVisibility() == View.VISIBLE)
        {
            //clear and close
            setQuery("", false);
            setIconified(true);
            return(true);
        }
        else
        {
            //unable to close
            return(false);
        }
    }

    private void setSuggestionsAdapter()
    {
        //set suggestions adapter by creating cursor from current adapter
        setSuggestionsAdapter(new IconSpinner.CustomCursorAdapter(getContext(), IconSpinner.CustomCursorAdapter.getCursor(adapter), adapter));
    }

    public void setAdapter(IconSpinner.CustomAdapter adapter)
    {
        //set adapter
        this.adapter = adapter;
        setSuggestionsAdapter();
    }

    public void updateSuggestions(String query)
    {
        //if adapter is set
        if(adapter != null)
        {
            //set filter
            adapter.getFilter().filter(query, new Filter.FilterListener()
            {
                @Override
                public void onFilterComplete(int count)
                {
                    //set adapter with filtered results
                    setSuggestionsAdapter();
                }
            });
        }
    }
}