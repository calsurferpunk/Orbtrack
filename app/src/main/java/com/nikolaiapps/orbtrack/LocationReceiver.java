package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.view.View;
import com.google.android.libraries.places.api.net.PlacesClient;
import java.util.TimeZone;


public abstract class LocationReceiver extends BroadcastReceiver
{
    int startFlags;

    public LocationReceiver(int setStartFlags)
    {
        startFlags = setStartFlags;
    }

    //Get activity
    protected Activity getActivity()
    {
        //usually needs to be overridden
        return(null);
    }

    //Get parent view
    protected View getParentView()
    {
        //usually needs to be overridden
        return(null);
    }

    //On denied
    protected void onDenied(Context context)
    {
        //needs to be overridden
    }

    //On connected
    protected void onConnected(Context context, PlacesClient placesClient)
    {
        //needs to be overridden
    }

    //On start location service
    protected void onRestart(Context context)
    {
        //if were starting with high power once
        if((startFlags & LocationService.FLAG_START_HIGH_POWER_ONCE) == LocationService.FLAG_START_HIGH_POWER_ONCE)
        {
            //remove high power once
            startFlags &= ~LocationService.FLAG_START_HIGH_POWER_ONCE;
        }

        //start again
        startLocationService(context);
    }

    //On got location
    protected void onGotLocation(Context context, Calculations.ObserverType observer)
    {
        //needs to be overridden
    }

    //On save location result
    protected void onSaveLocation(boolean success)
    {
        //needs to be overridden
    }

    //Register receiver
    public void register(Context context)
    {
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(LocationService.LOCATION_FILTER));
    }

    //Unregister receiver
    public void unregister(Context context)
    {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
    }

    //Start location service
    public void startLocationService(Context context)
    {
        //start with activity and flags
        LocationService.start(context, startFlags);
    }

    //Saves a location and resolves it
    public void saveResolvedLocation(final Context context, final Calculations.ObserverType observerLocation)
    {
        final Resources res = context.getResources();

        //if location is set
        if(observerLocation != null)
        {
            //resolve location
            AddressUpdateService.getResolvedLocation(context, observerLocation.geo.latitude, observerLocation.geo.longitude, new AddressUpdateService.OnLocationResolvedListener()
            {
                @Override
                public void onLocationResolved(String locationString, int resultCode)
                {
                    boolean isSuccess = (resultCode == AddressUpdateService.RESULT_SUCCESS);

                    switch(resultCode)
                    {
                        case AddressUpdateService.RESULT_SUCCESS:
                            //save location
                            Database.saveLocation(context, locationString, observerLocation.geo.latitude, observerLocation.geo.longitude, observerLocation.geo.altitudeKm * 1000, TimeZone.getDefault().getID(), Database.LocationType.Saved, true);
                            //fall through

                        default:
                        case AddressUpdateService.RESULT_FAIL:
                            //show any error and send result
                            if(!isSuccess)
                            {
                                Globals.showSnackBar(getParentView(), res.getString(R.string.text_location_failed), true);
                            }
                            onSaveLocation(isSuccess);
                            break;
                    }
                }
            }, false);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        byte messageType = intent.getByteExtra(LocationService.ParamTypes.MessageType, Byte.MAX_VALUE);
        Calculations.ObserverType observer = intent.getParcelableExtra(LocationService.ParamTypes.Observer);
        Activity activity;

        //handle based on message type
        switch(messageType)
        {
            case LocationService.MessageTypes.NeedPermission:
            case LocationService.MessageTypes.NeedEnable:
                //get activity
                activity = getActivity();

                //if activity is set and can ask for permission
                if(activity != null && Globals.canAskLocationPermission)
                {
                    if(messageType == LocationService.MessageTypes.NeedEnable)
                    {
                        //get enabling
                        Globals.askLocationEnable(activity);
                    }
                    else
                    {
                        //get permission
                        Globals.askLocationPermission(activity, false);
                    }
                    break;
                }
                //else fall through

            case LocationService.MessageTypes.Denied:
                //show denied
                LocationService.showDenied(context, getParentView());

                //call on denied
                onDenied(context);
                break;

            case LocationService.MessageTypes.Connected:
                //call on connected
                onConnected(context, LocationService.getGooglePlacesClient());
                break;

            case LocationService.MessageTypes.NeedRestart:
                //call on restart
                onRestart(context);
                break;

            case LocationService.MessageTypes.Location:
                //call on got location
                onGotLocation(context, observer);
                break;
        }
    }
}
