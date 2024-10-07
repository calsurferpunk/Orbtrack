package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.TextUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONObject;


public class AddressUpdateService extends NotifyService
{
    //Constants
    private static final String LOCATION_DATA_EXTRA = "OrbTrack.Location_Data_Extra";
    private static final String RECEIVER = "OrbTrack.Receiver";
    private static final String RESULT_DATA_KEY = "OrbTrack.Result_Data_Key";
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_FAIL = 1;

    public interface OnLocationResolvedListener
    {
        void onLocationResolved(String locationString, int resultCode);
    }

    @SuppressLint("ParcelCreator")
    private static class AddressUpdateReceiver extends ResultReceiver
    {
        private final OnLocationResolvedListener locationResolvedListener;

        public AddressUpdateReceiver(Handler handler, OnLocationResolvedListener listener)
        {
            super(handler);
            locationResolvedListener = listener;
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData)
        {
            String locationString;

            switch(resultCode)
            {
                case RESULT_SUCCESS      :
                    //get results
                    locationString = getLocationName(resultData.getString(RESULT_DATA_KEY));

                    //if location was received
                    if(!locationString.isEmpty())
                    {
                        //send location string
                        locationResolvedListener.onLocationResolved(locationString, resultCode);
                        break;
                    }
                    //else fall through
                    resultCode = RESULT_FAIL;

                case RESULT_FAIL:
                default:
                    //send location string
                    locationResolvedListener.onLocationResolved(null, resultCode);
                    break;
            }
        }
    }

    private ResultReceiver updateReceiver;

    public AddressUpdateService()
    {
        super("AddressUpdateService", Globals.getChannelId(Globals.ChannelIds.Address));
    }

    @Override
    protected void onClearIntent(byte index) {}

    @Override
    protected void onRunIntent(Intent intent)
    {
        int index;
        int index2;
        int index3;
        boolean runForeground = intent.getBooleanExtra(ParamTypes.RunForeground, false);
        double latitude;
        double longitude;
        String city = null;
        String state = null;
        String countryCode = null;
        String message = "";
        String dataLines;
        JSONObject resultsObject;
        JSONArray results;
        Resources res = this.getResources();
        Address resultAddress;
        Geocoder addressDecoder;
        Location addressLocation;
        NotificationCompat.Builder notifyBuilder = Globals.createNotifyBuilder(this, notifyChannelId);
        List<Address> addresses = null;
        ArrayList<String> addressData;

        //handle if need to start in foreground
        Globals.startForeground(this, Globals.ChannelIds.Address, notifyBuilder, runForeground, Globals.ForegroundServiceType.DATA_SYNC);

        //get location and receiver
        addressLocation = intent.getParcelableExtra(LOCATION_DATA_EXTRA);
        if(addressLocation == null)
        {
            addressLocation = new Location("");
        }
        updateReceiver = intent.getParcelableExtra(RECEIVER);

        //try to get address
        latitude = addressLocation.getLatitude();
        longitude = addressLocation.getLongitude();
        if(Geocoder.isPresent())
        {
            addressDecoder = new Geocoder(this, Locale.getDefault());

            try
            {
                addresses = addressDecoder.getFromLocation(latitude, longitude, 1);
            }
            catch(IOException ioEx)
            {
                message = res.getString(R.string.text_address_decoder_error);
            }
            catch(IllegalArgumentException argEx)
            {
                message = res.getString(R.string.text_invalid_address_error) + " (" + latitude + ", " + longitude + ")";
            }
            catch(Exception ex)
            {
                message = res.getString(R.string.text_unknown_address_decoder_error);
            }
        }
        else
        {
            try
            {
                //get results
                //noinspection SpellCheckingInspection
                resultsObject = Globals.getJSONWebPage("https://maps.googleapis.com/maps/api/geocode/json?key=" + res.getString(R.string.google_geocoding_api_web_key) + "&latlng=" + latitude + "," + longitude);
                if(resultsObject != null)
                {
                    results = resultsObject.getJSONArray("results");

                    //go through each result while data is needed
                    for(index = 0; index < results.length() && (city == null || state == null || countryCode == null); index++)
                    {
                        //remember current result
                        JSONArray currentResult = results.getJSONObject(index).getJSONArray("address_components");

                        //go through data in result
                        for(index2 = 0; index2 < currentResult.length(); index2++)
                        {
                            //get data and types
                            JSONObject currentResultItem = currentResult.getJSONObject(index2);
                            JSONArray currentResultTypes = currentResultItem.getJSONArray("types");
                            String data;
                            String dataType = null;

                            //go through each type while data type not found
                            for(index3 = 0; index3 < currentResultTypes.length() && dataType == null; index3++)
                            {
                                //get type
                                String currentType = currentResultTypes.getString(index3);

                                //if a usable type
                                if(currentType.equals("locality") || currentType.equals("administrative_area_level_1") || currentType.equals("country"))
                                {
                                    //set data type
                                    dataType = currentType;
                                }
                            }

                            //if data type was found
                            if(dataType != null)
                            {
                                //get data
                                data = currentResultItem.getString(dataType.equals("country") ? "short_name" : "long_name");

                                //set data if not set already
                                if(dataType.equals("locality") && city == null)
                                {
                                    city = data;
                                }
                                else if(dataType.equals("administrative_area_level_1") && state == null)
                                {
                                    state = data;
                                }
                                else if(dataType.equals("country") && countryCode == null)
                                {
                                    countryCode = data;
                                }
                            }
                        }
                    }

                    //if any data found
                    if(city != null || state != null || countryCode != null)
                    {
                        //set new address
                        resultAddress = new Address(Locale.getDefault());

                        //set found data
                        if(city != null)
                        {
                            resultAddress.setLocality(city);
                        }
                        if(state != null)
                        {
                            resultAddress.setAdminArea(state);
                        }
                        if(countryCode != null)
                        {
                            resultAddress.setCountryCode(countryCode);
                        }

                        //add address to list
                        addresses = new ArrayList<>(1);
                        addresses.add(resultAddress);
                    }
                }
            }
            catch(Exception ex)
            {
                message = ex.getMessage();
            }
        }

        //if no address found
        if(addresses == null || addresses.isEmpty())
        {
            //if no other message set yet
            if(message != null && message.isEmpty())
            {
                message = res.getString(R.string.text_no_address_found_error);
            }

            //send result
            sendResult(RESULT_FAIL, message);
        }
        else
        {
            //get address
            resultAddress = addresses.get(0);

            //add data
            addressData = new ArrayList<>(3);
            addressData.add(resultAddress.getLocality());           //city
            addressData.add(resultAddress.getAdminArea());          //state
            addressData.add(resultAddress.getCountryCode());        //country

            //combine lines
            dataLines = TextUtils.join("\n", addressData);

            //save data
            Database.addLocationName(this, latitude, longitude, getLocationName(dataLines));

            //send result
            sendResult(RESULT_SUCCESS, dataLines);
        }

    }

    //Sends result of address lookup
    private void sendResult(int result, String message)
    {
        Bundle params = new Bundle();
        params.putString(RESULT_DATA_KEY, message);
        updateReceiver.send(result, params);
    }

    //Gets a formatted location name from the given string containing line breaks
    private static String getLocationName(String locationLines)
    {
        boolean foundCity;
        boolean foundState;
        boolean foundCountry;
        String city;
        String state;
        String country;
        String locationString = "";
        String[] lines = TextUtils.split(locationLines, "\n");

        //if got all data
        if(lines.length == 3)
        {
            //get city, state, and country
            city = lines[0];
            state = lines[1];
            country = lines[2];
            foundCity = !city.equals("null");
            foundState = !state.equals("null");
            foundCountry = !country.equals("null");

            //if city not found
            if(!foundCity)
            {
                //if state found
                if(foundState)
                {
                    //set as state
                    locationString = state;

                    //if country found
                    if(foundCountry)
                    {
                        //add separator
                        locationString += ", ";
                    }
                }

                //if country found
                if(foundCountry)
                {
                    //add country
                    locationString += country;
                }
            }
            else
            {
                //set as city
                locationString = city;

                //if state found
                if(foundState)
                {
                    //add state
                    locationString += (", " + state);
                }
                //else if country found
                else if(foundCountry)
                {
                    //add country
                    locationString += (", " + country);
                }
            }
        }

        return(locationString);
    }

    //Gets city and country for the given location
    public static void getResolvedLocation(Context context, double latitude, double longitude, OnLocationResolvedListener listener, boolean runForeground)
    {
        String locationName = Database.getClosestLocationName(context, latitude, longitude, 0.025);

        //if found a close enough location
        if(locationName != null)
        {
            //use it
            listener.onLocationResolved(locationName, RESULT_SUCCESS);
        }
        else
        {
            Intent addressRetrieverIntent = new Intent(context, AddressUpdateService.class);
            AddressUpdateReceiver addressReceiver = new AddressUpdateReceiver(new Handler(), listener);
            Location resolveLocation = new Location(Globals.getUnknownString(context));

            //set data
            resolveLocation.setLatitude(latitude);
            resolveLocation.setLongitude(longitude);
            addressRetrieverIntent.putExtra(RECEIVER, addressReceiver);
            addressRetrieverIntent.putExtra(LOCATION_DATA_EXTRA, resolveLocation);
            addressRetrieverIntent.putExtra(ParamTypes.RunForeground, runForeground);

            //start resolving
            Globals.startService(context, addressRetrieverIntent, runForeground);
        }
    }
}
