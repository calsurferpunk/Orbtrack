package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


//Adapter to hold auto complete places
public class AutoPlacesAdapter extends ArrayAdapter<AutoPlacesAdapter.Item> implements Filterable
{
    public static class Item
    {
        private final CharSequence description;
        public final CharSequence ID;

        public Item(CharSequence placeID, CharSequence placeDesc)
        {
            ID = placeID;
            description = placeDesc;
        }

        @Override
        public @NonNull String toString()
        {
            return(description.toString());
        }
    }

    private final boolean darkTheme;
    private final String uuid;
    private final PlacesClient placesClient;
    private ArrayList<Item> places;

    AutoPlacesAdapter(Context context, PlacesClient client)
    {
        super(context, android.R.layout.simple_list_item_1);

        darkTheme = Settings.getDarkTheme(context);

        if(client == null)
        {
            placesClient = null;
            uuid = UUID.randomUUID().toString();
        }
        else
        {
            placesClient = client;
            uuid = null;
        }
    }

    @Override
    public int getCount()
    {
        return(places != null ? places.size() : 0);
    }

    @Override
    public Item getItem(int position)
    {
        return(places.get(position));
    }

    @Override
    public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        TextView itemText = (TextView)super.getView(position, convertView, parent);
        itemText.setTextColor((darkTheme ? Color.WHITE : Color.BLACK));
        itemText.setBackgroundColor((darkTheme ? Color.BLACK : Color.WHITE));
        return(itemText);
    }

    private ArrayList<Item> getPlaces(CharSequence text)
    {
        int index;
        JSONObject resultsObject;
        JSONArray predictions;
        AutocompleteSessionToken token;
        FindAutocompletePredictionsRequest request;
        FindAutocompletePredictionsResponse resultStatus;
        Task<FindAutocompletePredictionsResponse> requestTask;
        ArrayList<Item> resultList = null;
        List<AutocompletePrediction> predictionList;

        //if client is set
        if(placesClient != null)
        {
            //try to get results
            token = AutocompleteSessionToken.newInstance();
            request = FindAutocompletePredictionsRequest.builder().setSessionToken(token).setQuery(text.toString()).build();
            requestTask = placesClient.findAutocompletePredictions(request);
            try
            {
                //wait for results
                Tasks.await(requestTask, 30, TimeUnit.SECONDS);
                if(requestTask.isSuccessful())
                {
                    //get results
                    resultStatus = requestTask.getResult();
                    if(resultStatus != null)
                    {
                        //go through each result
                        predictionList = resultStatus.getAutocompletePredictions();
                        resultList = new ArrayList<>(predictionList.size());
                        for(AutocompletePrediction currentPrediction : predictionList)
                        {
                            //add result to list
                            resultList.add(new Item(currentPrediction.getPlaceId(), currentPrediction.getFullText(null)));
                        }
                    }
                }
            }
            catch(Exception ex)
            {
                //no results
            }
        }
        else
        {
            try
            {
                //get predictions
                //noinspection SpellCheckingInspection
                resultsObject = Globals.getJSONWebPage("https://maps.googleapis.com/maps/api/place/autocomplete/json?key=" + getContext().getResources().getString(R.string.google_places_api_web_key) + "&sessiontoken=" + uuid + "&input=" + URLEncoder.encode(text.toString(), Globals.Encoding.UTF8));
                if(resultsObject != null)
                {
                    predictions = resultsObject.getJSONArray("predictions");

                    //go through each prediction
                    resultList = new ArrayList<>(predictions.length());
                    for(index = 0; index < predictions.length(); index++)
                    {
                        //remember current prediction
                        JSONObject currentPrediction = predictions.getJSONObject(index);

                        //add result
                        resultList.add(new Item(currentPrediction.getString("place_id"), currentPrediction.getString("description")));
                    }
                }
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }

        //return list
        return(resultList);
    }

    @Override @NonNull
    public Filter getFilter()
    {
        return(new Filter()
        {
            @Override
            protected FilterResults performFiltering(CharSequence constraint)
            {
                FilterResults results = new FilterResults();

                //if a constraint is set
                if(constraint != null)
                {
                    //get places
                    places = getPlaces(constraint);
                    if(places != null)
                    {
                        results.values = places;
                        results.count = places.size();
                    }
                }
                return(results);
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results)
            {
                //if there was at least 1 result
                if(results != null && results.count > 0)
                {
                    //update data
                    notifyDataSetChanged();
                }
                else
                {
                    //invalidate data
                    notifyDataSetInvalidated();
                }
            }
        });
    }
}
