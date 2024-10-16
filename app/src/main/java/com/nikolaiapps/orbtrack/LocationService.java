package com.nikolaiapps.orbtrack;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class LocationService extends Service implements LocationListener
{
    //Message types
    static abstract class MessageTypes
    {
        static final byte Location = 0;
        static final byte Connected = 1;
        static final byte NeedRestart = 2;
        static final byte NeedPermission = 3;
        static final byte NeedEnable = 4;
        static final byte Denied = 5;
    }

    //Param types
    static abstract class ParamTypes
    {
        static final String MessageType = "messageType";
        static final String Observer = "observer";
        static final String GetLocation = "getLocation";
        static final String RunForeground = "runForeground";
        static final String PowerType = "powerType";
        static final String ForceStop = "forceStop";
    }

    //Power type
    static abstract class PowerTypes
    {
        static final byte Balanced = 0;
        static final byte HighPower = 1;
        static final byte HighPowerThenBalanced = 2;
    }

    //Online source
    static abstract class OnlineSource
    {
        static final byte None = 0;
        static final byte Saved = 1;
        static final byte Google = 2;
        static final byte MapQuest = 3;
        static final byte GeoNames = 4;
    }

    public interface OnGotCoordinatesListener
    {
        void gotCoordinates(double latitude, double longitude);
    }

    private static class GetPlaceIdCoordinatesTask extends ThreadTask<Object, Void, Void>
    {
        @Override @SuppressWarnings("SpellCheckingInspection")
        protected Void doInBackground(Object... objects)
        {
            Context context = (Context)objects[0];
            String placeId = (String)objects[1];
            OnGotCoordinatesListener listener = (OnGotCoordinatesListener)objects[2];

            JSONObject resultObject = Globals.getJSONWebPage("https://maps.googleapis.com/maps/api/place/details/json?key=" + context.getResources().getString(R.string.google_places_api_web_key) + "&placeid=" + placeId);
            if(resultObject != null)
            {
                try
                {
                    JSONObject location = resultObject.getJSONObject("result").getJSONObject("geometry").getJSONObject("location");
                    listener.gotCoordinates(location.getDouble("lat"), location.getDouble("lng"));
                }
                catch(Exception ex)
                {
                    //do nothing
                }
            }

            return(null);
        }
    }

    //Get timezone task
    static class GetTimezoneTask extends ThreadTask<Object, Void, Void>
    {
        public interface OnGotTimezoneListener
        {
            void onGotTimeZone(String zoneId);
        }

        private String zoneId;
        private final Location timezoneLocation;
        private final GetTimezoneTask.OnGotTimezoneListener gotTimezoneListener;

        GetTimezoneTask(double latitude, double longitude, GetTimezoneTask.OnGotTimezoneListener listener)
        {
            zoneId = null;
            timezoneLocation = new Location("");
            timezoneLocation.setLatitude(latitude);
            timezoneLocation.setLongitude(longitude);
            timezoneLocation.setAltitude(0);
            gotTimezoneListener = listener;
        }

        //Try to get timezone of location from source and returns if success
        private boolean getTimezone(Context context, int source)
        {
            String id;
            String status;
            String latString = String.format(Locale.US, "%.5f", timezoneLocation.getLatitude());
            String lonString = String.format(Locale.US, "%.5f", timezoneLocation.getLongitude());
            String urlString = null;
            JSONObject parsedData;
            Globals.WebPageData tzData;

            //get api key
            String apiKey = context.getResources().getString(source == OnlineSource.Google ? R.string.google_elevation_timezone_api_key : R.string.geonames_timezone_api_key);

            //handle based on source
            switch(source)
            {
                case OnlineSource.Google:
                    urlString = "https://maps.googleapis.com/maps/api/timezone/json?location=" + latString + "," + lonString + "&timestamp=" + (System.currentTimeMillis() / 1000) + "&key=" + apiKey;
                    break;

                case OnlineSource.GeoNames:
                    urlString = "https://api.geonames.org/timezoneJSON?lat=" + latString + "&lng=" + lonString + "&username=" + apiKey;
                    break;
            }
            if(urlString != null)
            {
                //try to get timezone data
                tzData = Globals.getWebPage(urlString);

                //if data was received
                if(tzData.gotData() && tzData.isOkay())
                {
                    //try to pare data
                    try
                    {
                        //get data
                        parsedData = new JSONObject(tzData.pageData);
                        switch(source)
                        {
                            case OnlineSource.Google:
                                status = parsedData.getString("status");
                                id = parsedData.getString("timeZoneId");

                                //if valid
                                if(status.equalsIgnoreCase("ok"))
                                {
                                    //set timezone
                                    zoneId = id;
                                    return(true);
                                }
                                break;

                            case OnlineSource.GeoNames:
                                id = parsedData.getString("timeZoneId");

                                //if valid
                                if(!id.isEmpty())
                                {
                                    //set timezone
                                    zoneId = id;
                                    return(true);
                                }
                                break;
                        }
                    }
                    catch(Exception ex)
                    {
                        //do nothing
                    }
                }
            }

            //failed
            return(false);
        }

        //Gets timezone and returns source (if any)
        private int getTimezone(Context context)
        {
            int source = Settings.getTimeZoneSource(context);
            int altSource = (source == OnlineSource.Google ? OnlineSource.GeoNames : OnlineSource.Google);
            double lat = timezoneLocation.getLatitude();
            double lon = timezoneLocation.getLongitude();
            String id;

            //try to get closest saved location timezone
            id = Database.getClosestTimeZone(context, lat, lon, 0.075);
            if(id != null)
            {
                //use close saved
                zoneId = id;
                return(OnlineSource.Saved);
            }
            //else try source
            else if(getTimezone(context, source))
            {
                //found from source
                return(source);
            }
            //else try alternate source
            else if(getTimezone(context, altSource))
            {
                //found from alternate source
                return(altSource);
            }
            else
            {
                //not found
                return(OnlineSource.None);
            }
        }

        @Override
        protected Void doInBackground(Object... params)
        {
            int timezoneSource;
            Context context = (Context)params[0];

            //try to get timezone
            timezoneSource = getTimezone(context);
            switch(timezoneSource)
            {
                case OnlineSource.Google:
                case OnlineSource.GeoNames:
                    //save it
                    Database.addTimeZone(context, timezoneLocation.getLatitude(), timezoneLocation.getLongitude(), zoneId);
                    break;
            }

            gotTimezoneListener.onGotTimeZone(zoneId);
            return(null);
        }
    }

    //Get altitude task
    static class GetAltitudeTask extends ThreadTask<Object, Void, Void>
    {
        public interface OnGotAltitudeListener
        {
            void onGotAltitude(Location result);
        }

        private final Location altitudeLocation;
        private final GetAltitudeTask.OnGotAltitudeListener gotAltitudeListener;

        @SuppressWarnings("unused")
        GetAltitudeTask(Location location, GetAltitudeTask.OnGotAltitudeListener listener)
        {
            altitudeLocation = location;
            gotAltitudeListener = listener;
        }
        GetAltitudeTask(double latitude, double longitude, GetAltitudeTask.OnGotAltitudeListener listener)
        {
            altitudeLocation = new Location("");
            altitudeLocation.setLatitude(latitude);
            altitudeLocation.setLongitude(longitude);
            altitudeLocation.setAltitude(0);
            gotAltitudeListener = listener;
        }

        //Try to gets altitude of location from source and returns if success
        private boolean getAltitude(Context context, int source)
        {
            int statusCode;
            double altitudeM;
            String status;
            String apiKey;
            String latString = String.format(Locale.US, "%.4f", altitudeLocation.getLatitude());
            String lonString = String.format(Locale.US, "%.4f", altitudeLocation.getLongitude());
            String urlString = null;
            JSONObject parsedData;
            JSONArray results;
            Globals.WebPageData altData;

            //get api key
            apiKey = context.getResources().getString(source == OnlineSource.Google ? R.string.google_elevation_timezone_api_key : R.string.mapquest_elevation_api_key);

            //handle based on source
            switch(source)
            {
                case OnlineSource.Google:
                    urlString = "https://maps.googleapis.com/maps/api/elevation/json?locations=" + latString + "," + lonString + "&key=" + apiKey;
                    break;

                case OnlineSource.MapQuest:
                    urlString = "https://open.mapquestapi.com/elevation/v1/profile?key=" + apiKey + "&shapeFormat=raw&latLngCollection=" + latString + "," + lonString + "," + latString + "," + lonString;
                    break;
            }
            if(urlString != null)
            {
                //try to get altitude data
                altData = Globals.getWebPage(urlString);

                //if data was received
                if(altData.gotData() && altData.isOkay())
                {
                    //try to pare data
                    try
                    {
                        //get data
                        parsedData = new JSONObject(altData.pageData);
                        switch(source)
                        {
                            case OnlineSource.Google:
                                status = parsedData.getString("status");
                                results = parsedData.getJSONArray("results");
                                altitudeM = results.getJSONObject(0).getDouble("elevation");

                                //if valid
                                if(status.equalsIgnoreCase("ok"))
                                {
                                    //set altitude
                                    altitudeLocation.setAltitude(altitudeM);
                                    return(true);
                                }
                                break;

                            case OnlineSource.MapQuest:
                                results = parsedData.getJSONArray("elevationProfile");
                                altitudeM = results.getJSONObject(0).getDouble("height");
                                statusCode = parsedData.getJSONObject("info").getInt("statusCode");

                                //if valid
                                if(statusCode == 0 && altitudeM != -32768)
                                {
                                    //set altitude
                                    altitudeLocation.setAltitude(altitudeM);
                                    return(true);
                                }
                                break;
                        }
                    }
                    catch(Exception ex)
                    {
                        //do nothing
                    }
                }
            }

            //failed
            return(false);
        }

        //Gets altitude and returns source (if any)
        private int getAltitude(Context context)
        {
            int source = Settings.getAltitudeSource(context);
            int altSource = (source == OnlineSource.Google ? OnlineSource.MapQuest : OnlineSource.Google);
            double altitudeM;
            double lat = altitudeLocation.getLatitude();
            double lon = altitudeLocation.getLongitude();

            //try to get closest saved location altitude
            altitudeM = Database.getClosestAltitude(context, lat, lon, 0.15);
            if(altitudeM != Double.MAX_VALUE)
            {
                //use close saved
                altitudeLocation.setAltitude(altitudeM);
                return(OnlineSource.Saved);
            }
            //else try from source
            else if(getAltitude(context, source))
            {
                //found from source
                return(source);
            }
            //else try alternate source
            else if(getAltitude(context, altSource))
            {
                //found form alternate source
                return(altSource);
            }
            else
            {
                //not found
                return(OnlineSource.None);
            }
        }

        @Override
        protected Void doInBackground(Object... params)
        {
            int altitudeSource;
            Context context = (Context)params[0];

            //try to get altitude
            altitudeSource = getAltitude(context);
            switch(altitudeSource)
            {
                case OnlineSource.Google:
                case OnlineSource.MapQuest:
                    //save it
                    Database.addAltitude(context, altitudeLocation.getLatitude(), altitudeLocation.getLongitude(), altitudeLocation.getAltitude());
                    break;
            }

            gotAltitudeListener.onGotAltitude(altitudeLocation);
            return(null);
        }
    }

    //Notify receiver
    public static class NotifyReceiver extends NotifyService.NotifyReceiver
    {
        static final int StopID = 1;
        static final int RetryID = 2;

        @Override
        protected void onBootCompleted(Context context, long timeNowMs, AlarmManager manager) {}

        @Override
        protected void onRunService(Context context, Intent intent) {}

        @Override
        protected void onCloseNotification(Context context, Intent intent, NotificationManagerCompat manager)
        {
            String action = intent.getAction();

            //if retrying
            if(action != null && action.equals(RetryAction))
            {
                //start again
                start(context, FLAG_START_GET_LOCATION | FLAG_START_RUN_FOREGROUND);
            }
            else
            {
                //stop service
                restart(context, true);
            }

            //close notification
            manager.cancel(Globals.ChannelIds.Location);
        }
    }

    //Constants
    static final long LOCATION_UPDATE_RATE_MS = 3600000;        //1 hour
    static final String LOCATION_FILTER = "locationServiceFilter";
    static final byte FLAG_START_NONE = 0x00;
    static final byte FLAG_START_GET_LOCATION = 0x01;
    static final byte FLAG_START_RUN_FOREGROUND = 0x02;
    static final byte FLAG_START_HIGH_POWER = 0x04;
    static final byte FLAG_START_HIGH_POWER_ONCE = 0x08;

    //Status
    private static byte usePowerType = PowerTypes.Balanced;
    private static byte runningPowerType = PowerTypes.Balanced;
    private static boolean pendingLowPower = false;
    private static boolean useLegacy = false;
    private static boolean testingAPI = false;
    private static boolean needTestAPI = true;
    private static boolean useForeground = false;
    private static boolean useLocationUpdates = false;
    private static boolean startedLocationUpdates = false;
    private static String legacyProvider = null;
    private static Handler timeoutHandler = null;
    private static Runnable timeoutTask = null;
    private static PlacesClient googlePlacesClient = null;
    private static LocationManager legacyClient = null;
    private static LocationRequest locationRequester = null;
    private static android.location.LocationListener legacyListener = null;
    private static LocationCallback locationListener = null;
    private static MutableLiveData<Intent> localBroadcast = null;
    private static FusedLocationProviderClient locationClient = null;

    private boolean runForeground;
    private NotificationCompat.Builder notifyBuilder = null;

    @Nullable @Override
    public IBinder onBind(Intent intent)
    {
        return(null);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId)
    {
        final Resources res = this.getResources();
        byte powerType = intent.getByteExtra(ParamTypes.PowerType, PowerTypes.Balanced);
        byte messageType = intent.getByteExtra(ParamTypes.MessageType, Byte.MAX_VALUE);
        boolean forceStop = intent.getBooleanExtra(ParamTypes.ForceStop, true);
        boolean gettingLocation = intent.getBooleanExtra(ParamTypes.GetLocation, false);
        String notifyChannelId = Globals.getChannelId(Globals.ChannelIds.Location);
        runForeground = intent.getBooleanExtra(ParamTypes.RunForeground, false);

        //if need to test for available APIs
        if(needTestAPI)
        {
            //if not already testing
            if(!testingAPI)
            {
                //testing
                testingAPI = true;

                //get if using legacy
                useLegacy = !Globals.getUseGooglePlayServices(this);

                //done testing
                needTestAPI = testingAPI = false;

                //restart
                return(onStartCommand(intent, flags, startId));
            }
        }
        else
        {
            //update status
            usePowerType = powerType;
            if(usePowerType == PowerTypes.HighPowerThenBalanced)
            {
                //switch to low power when done
                pendingLowPower = true;
            }

            //if not just restarting/stopping
            if(messageType != MessageTypes.NeedRestart)
            {
                //get notification manager
                NotificationManagerCompat notifyManager = NotificationManagerCompat.from(this);

                //if running in foreground
                if(runForeground)
                {
                    Intent dismissIntent;

                    //update status
                    useForeground = true;

                    //create notification channel
                    Globals.createNotifyChannel(this, notifyChannelId);

                    //setup notification
                    notifyBuilder = Globals.createNotifyBuilder(this, notifyChannelId);
                    notifyBuilder.setContentTitle("Orbtrack " + res.getString(R.string.title_widget)).setContentText(res.getString(R.string.text_location_watching));

                    //add stop button
                    dismissIntent = new Intent(this, NotifyReceiver.class);
                    dismissIntent.setAction(NotifyReceiver.DismissAction);
                    notifyBuilder.addAction(new NotificationCompat.Action(0, res.getString(R.string.title_stop), Globals.getPendingBroadcastIntent(this, NotifyReceiver.StopID, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                }
                if(!useForeground)
                {
                    //close notification
                    notifyManager.cancel(Globals.ChannelIds.Location);
                }
                Globals.startForeground(this, Globals.ChannelIds.Location, notifyBuilder, useForeground, Globals.ForegroundServiceType.LOCATION);

                //setup local broadcast
                getLocalBroadcast();

                //make sure connected
                connect(gettingLocation);
            }
            else
            {
                //stop and send need restart
                if(useForeground)
                {
                    stopForeground(true);
                }
                stopSelf();
                useForeground = false;
                if(!forceStop)
                {
                    sendNeedRestart();
                }
            }
        }

        return(START_NOT_STICKY);
    }

    @Override
    public void onDestroy()
    {
        stopLocationUpdates();
        super.onDestroy();
    }

    //Gets local broadcast
    @NonNull
    private static MutableLiveData<Intent> getLocalBroadcast()
    {
        if(localBroadcast == null)
        {
            localBroadcast = new MutableLiveData<>();
            Globals.setBroadcastValue(localBroadcast, null);
        }

        return(localBroadcast);
    }

    public static void observeForever(Observer<Intent> observer)
    {
        getLocalBroadcast().observeForever(observer);
    }

    public static void observe(Context context, Observer<Intent> observer)
    {
        if(context instanceof LifecycleOwner)
        {
            getLocalBroadcast().observe((LifecycleOwner)context, observer);
        }
    }

    public static void removeObserver(Observer<Intent> observer)
    {
        if(observer != null)
        {
            MutableLiveData<Intent> localBroadcast = getLocalBroadcast();

            localBroadcast.removeObserver(observer);
            Globals.setBroadcastValue(localBroadcast, null);
        }
    }

    //Gets a new broadcast intent
    private static Intent getBroadcastIntent(byte messageType)
    {
        Intent broadcastIntent = new Intent(LOCATION_FILTER);
        broadcastIntent.putExtra(ParamTypes.MessageType, messageType);
        return(broadcastIntent);
    }

    //Sends a need enable broadcast
    private void sendNeedEnable()
    {
        if(localBroadcast != null)
        {
            Globals.setBroadcastValue(localBroadcast, getBroadcastIntent(MessageTypes.NeedEnable));
        }
    }

    //Sends a need permission broadcast
    private void sendNeedPermission()
    {
        if(localBroadcast != null)
        {
            Globals.setBroadcastValue(localBroadcast, getBroadcastIntent(MessageTypes.NeedPermission));
        }
    }

    //Sends a denied broadcast
    private void sendDenied()
    {
        if(localBroadcast != null)
        {
            Globals.setBroadcastValue(localBroadcast, getBroadcastIntent(MessageTypes.Denied));
        }
    }

    //Sends on connected broadcast
    private void sendConnected()
    {
        if(localBroadcast != null)
        {
            Globals.setBroadcastValue(localBroadcast, getBroadcastIntent(MessageTypes.Connected));
        }
    }

    //Sends a need restart
    private void sendNeedRestart()
    {
        if(localBroadcast != null)
        {
            Globals.setBroadcastValue(localBroadcast, getBroadcastIntent(MessageTypes.NeedRestart));
        }
    }

    //Sends location
    private void sendLocation(Calculations.ObserverType observer)
    {
        Intent locationIntent = getBroadcastIntent(MessageTypes.Location);

        //if location is set
        if(observer != null && observer.geo.isSet())
        {
            locationIntent.putExtra(ParamTypes.Observer, observer);
            if(localBroadcast != null)
            {
                Globals.setBroadcastValue(localBroadcast, locationIntent);
            }

            //if not getting location updates and not running in foreground
            if(!useLocationUpdates && !runForeground)
            {
                //stop and send need restart
                stopSelf();
                sendNeedRestart();
            }
        }
    }

    //Gets last saved location
    private Location getSavedLocation()
    {
        Location savedLocation = Settings.getLastLocation(this);
        return(savedLocation.getLatitude() != 0 || savedLocation.getLongitude() != 0 || savedLocation.getAltitude() != 0 ? savedLocation : null);
    }

    //Sends last client location
    private void sendLastClientLocation() throws SecurityException
    {
        boolean finished = false;
        Location lastLocation = null;

        //if using legacy and client is set
        if(useLegacy && legacyClient != null && legacyProvider != null)
        {
            //try to get last location
            lastLocation = legacyClient.getLastKnownLocation(legacyProvider);
        }
        //else if non legacy client is set
        else if(locationClient != null)
        {
            //try to get last location
            locationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<>()
            {
                @Override
                public void onSuccess(Location location)
                {
                    //update with last known location
                    handleLocationChanged(location);
                }
            });

            //wait for client task
            finished = true;
        }

        //if not finished
        if(!finished)
        {
            //update with possible location
            handleLocationChanged(lastLocation);
        }
    }

    //Creates a google places client
    private PlacesClient createPlacesClient()
    {
        Places.initialize(this.getApplicationContext(), this.getResources().getString(R.string.google_places_api_key));
        return(Places.createClient(this));
    }

    //Create location requester for continuous updates
    private void createLocationRequester()
    {
        int priority = (usePowerType == PowerTypes.HighPower || usePowerType == PowerTypes.HighPowerThenBalanced ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY);

        //if not using legacy
        if(!useLegacy)
        {
            //if running PowerTypes.Balanced and need a higher PowerTypes
            if(runningPowerType == PowerTypes.Balanced && (usePowerType == PowerTypes.HighPower || usePowerType == PowerTypes.HighPowerThenBalanced))
            {
                //stop current updates
                stopLocationUpdates();
            }

            //if requester does not exist yet
            if(locationRequester == null)
            {
                //create requester
                locationRequester = new LocationRequest.Builder(priority, LOCATION_UPDATE_RATE_MS).setMinUpdateIntervalMillis(LOCATION_UPDATE_RATE_MS / 4).build();
            }
        }
    }

    //Create location listener for continuous updates
    private void createLocationListener()
    {
        //if using legacy
        if(useLegacy)
        {
            //if the listener does not exist yet
            if(legacyListener == null)
            {
                legacyListener = new android.location.LocationListener()
                {
                    @Override
                    public void onLocationChanged(@NonNull Location location)
                    {
                        handleLocationChanged(location);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {}

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {}
                };
            }
        }
        else
        {
            //if the listener does not exist yet
            if(locationListener == null)
            {
                locationListener = new LocationCallback()
                {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult)
                    {
                        handleLocationChanged(locationResult.getLastLocation(), true);
                    }
                };
            }
        }
    }

    //Starts/stops timeout timer
    private void setTimeoutTimer(boolean run)
    {
        //if timer exists
        if(timeoutHandler != null)
        {
            //if task is set
            if(timeoutTask != null)
            {
                //cancel it
                timeoutHandler.removeCallbacks(timeoutTask);
                timeoutTask = null;
            }

            //clear timer
            timeoutHandler = null;
        }

        //if want to run
        if(run)
        {
            //set timeout to run in 15 seconds
            timeoutTask = new Runnable()
            {
                @Override
                public void run()
                {
                    //if still waiting for power update
                    if(pendingLowPower)
                    {
                        Context context = LocationService.this;

                        //if notification builder exists
                        if(notifyBuilder != null)
                        {
                            Resources res = context.getResources();

                            //show retry
                            Intent retryIntent = new Intent(context, NotifyReceiver.class);
                            retryIntent.setAction(NotifyReceiver.RetryAction);
                            notifyBuilder.setContentText(res.getString(R.string.text_location_failed));
                            notifyBuilder.addAction(new NotificationCompat.Action(0, res.getString(R.string.title_retry), Globals.getPendingBroadcastIntent(context, NotifyReceiver.RetryID, retryIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                            Globals.showNotification(context, Globals.ChannelIds.Location, notifyBuilder.build());
                        }

                        //restart service
                        pendingLowPower = false;
                        restart(context, false);
                    }
                }
            };
            timeoutHandler = new Handler();
            timeoutHandler.postDelayed(timeoutTask, 15000);
        }
    }

    //Stop getting updates
    private void stopLocationUpdates()
    {
        //stop getting updates
        startedLocationUpdates = false;
        if(legacyClient != null && legacyListener != null)
        {
            legacyClient.removeUpdates(legacyListener);
            legacyClient = null;
            legacyListener = null;
        }
        if(locationClient != null && locationListener != null)
        {
            locationClient.removeLocationUpdates(locationListener);
            locationClient = null;
            locationRequester = null;
            locationListener = null;
        }
    }

    //Start getting location updates
    private void startLocationUpdates()
    {
        //if have permission to get location
        if(Globals.haveLocationPermission(this))
        {
            //create location requester
            createLocationRequester();

            //if not already getting updates
            if(!startedLocationUpdates)
            {
                //start getting updates
                startedLocationUpdates = true;
                try
                {
                    //create listener
                    createLocationListener();

                    //if using legacy
                    if(useLegacy)
                    {
                        //get updates
                        if(legacyProvider != null)
                        {
                            legacyClient.requestLocationUpdates(legacyProvider, LOCATION_UPDATE_RATE_MS, 0, legacyListener);
                        }

                        //send last client location
                        sendLastClientLocation();
                    }
                    else
                    {
                        //get updates
                        locationClient = LocationServices.getFusedLocationProviderClient(this);
                        locationClient.requestLocationUpdates(locationRequester, locationListener, Looper.myLooper()).addOnCompleteListener(new OnCompleteListener<>()
                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {
                                //send last client location
                                sendLastClientLocation();
                            }
                        });

                        //if using high power but switching later
                        if(usePowerType == PowerTypes.HighPowerThenBalanced)
                        {
                            //start timeout timer
                            setTimeoutTimer(true);
                        }

                        //update status
                        runningPowerType = usePowerType;
                    }
                }
                catch(SecurityException ex)
                {
                    //send denied
                    stopLocationUpdates();
                    sendDenied();
                }
            }
            //else
            else
            {
                //try to send last client location
                try
                {
                    sendLastClientLocation();
                }
                catch(Exception ex)
                {
                    //do nothing
                }
            }
        }
    }

    //Connect to get location
    private void connect(boolean gettingLocation)
    {
        //if getting location
        if(gettingLocation)
        {
            //update status
            useLocationUpdates = true;

            //if need permission
            if(!Globals.haveLocationPermission(this))
            {
                //if can ask
                if(Globals.canAskLocationPermission)
                {
                    //send need permission broadcast
                    sendNeedPermission();
                }
                else
                {
                    //send denied
                    sendDenied();
                }

                //stop
                return;
            }
            //else if location services are not enabled
            else if(!Globals.haveLocationEnabled(this))
            {
                //send need enable broadcast and stop
                sendNeedEnable();
                return;
            }
        }

        //if using legacy
        if(useLegacy)
        {
            //if client is not set
            if(legacyClient == null)
            {
                //set it
                legacyClient = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
            }

            //if provider is not set
            if(legacyProvider == null)
            {
                //get all available providers
                List<String> legacyProviders = (legacyClient != null ? legacyClient.getAllProviders() : new ArrayList<>(0));

                //try to get lowest battery usage provider that will return now
                if(legacyProviders.contains(LocationManager.NETWORK_PROVIDER))
                {
                    legacyProvider = LocationManager.NETWORK_PROVIDER;
                }
                else if(legacyProviders.contains(LocationManager.GPS_PROVIDER))
                {
                    legacyProvider = LocationManager.GPS_PROVIDER;
                }
                else if(legacyProviders.contains(LocationManager.PASSIVE_PROVIDER))
                {
                    legacyProvider = LocationManager.PASSIVE_PROVIDER;
                }
            }
        }
        else
        {
            //if need to connect
            if(googlePlacesClient == null)
            {
                //create places client and connect
                googlePlacesClient = createPlacesClient();
            }
        }

        //handle on connected
        onConnected();
    }

    public void onConnected()
    {
        //send broadcast
        sendConnected();

        //if using location and have location permission
        if(useLocationUpdates && Globals.haveLocationPermission(this))
        {
            //start getting updates
            startLocationUpdates();
        }
    }

    //Handle a possible location change
    private void handleLocationChanged(Location location, boolean checkPower)
    {
        //if location is set
        if(location != null)
        {
            //remember location
            Settings.setLastLocation(this, location);
        }
        else
        {
            //try to get saved location
            location = getSavedLocation();
        }

        //if location is set now
        if(location != null)
        {
            //send location
            sendLocation(new Calculations.ObserverType(TimeZone.getDefault().getID(), location.getLatitude(), location.getLongitude(), location.getAltitude() / 1000.0));
        }

        //if checking power usage and switching
        if(checkPower && pendingLowPower)
        {
            //restart and cancel any running timer
            pendingLowPower = false;
            setTimeoutTimer(false);
            restart(this, false);
        }
    }
    private void handleLocationChanged(Location location)
    {
        handleLocationChanged(location, false);
    }

    @Override
    public void onLocationChanged(@NonNull Location location)
    {
        handleLocationChanged(location, true);
    }

    //Gets google places client
    public static PlacesClient getGooglePlacesClient()
    {
        return(useLegacy ? null : googlePlacesClient);
    }

    //Sets an autocomplete text to resolved locations
    public static void setAutoCompletePlaces(AutoCompleteTextView textView, final Context context, final PlacesClient client, final OnGotCoordinatesListener listener)
    {
        final AutoPlacesAdapter placesAdapter = new AutoPlacesAdapter(context, client);

        //setup autocompletion and resolving
        textView.setThreshold(3);
        textView.setAdapter(placesAdapter);
        textView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                AutoPlacesAdapter.Item currentPlace = placesAdapter.getItem(position);

                if(currentPlace != null)
                {
                    String placeId = currentPlace.ID.toString();
                    List<Place.Field> placeResult = Collections.singletonList(Place.Field.LAT_LNG);

                    if(googlePlacesClient != null)
                    {
                        googlePlacesClient.fetchPlace(FetchPlaceRequest.builder(placeId, placeResult).build()).addOnSuccessListener(new OnSuccessListener<>()
                        {
                            @Override
                            public void onSuccess(FetchPlaceResponse fetchPlaceResponse)
                            {
                                Place resultPlace = fetchPlaceResponse.getPlace();
                                LatLng placeLatLon = resultPlace.getLatLng();

                                if(placeLatLon != null)
                                {
                                    listener.gotCoordinates(placeLatLon.latitude, placeLatLon.longitude);
                                }
                            }
                        });
                    }
                    else
                    {
                        new GetPlaceIdCoordinatesTask().execute(context, placeId, new OnGotCoordinatesListener()
                        {
                            @Override
                            public void gotCoordinates(double latitude, double longitude)
                            {
                                listener.gotCoordinates(latitude, longitude);
                            }
                        });
                    }
                }
            }
        });
    }

    //Show permission denied
    public static void showDenied(Context context, View parentView)
    {
        //show denied and don't ask again
        Globals.showDenied(parentView, context.getResources().getString(R.string.desc_permission_location_deny));
    }

    //Gets a new start intent
    private static Intent getStartIntent(Context context, boolean gettingLocation, boolean runForeground, byte powerType)
    {
        Intent startIntent = new Intent(context, LocationService.class);
        startIntent.putExtra(ParamTypes.GetLocation, gettingLocation);
        startIntent.putExtra(ParamTypes.RunForeground, runForeground);
        startIntent.putExtra(ParamTypes.PowerType, powerType);
        return(startIntent);
    }

    //Gets current location
    public static void getCurrentLocation(Context context, boolean runForeground, byte powerType)
    {
        Intent startIntent = getStartIntent(context, true, runForeground, powerType);
        Globals.startService(context, startIntent, runForeground);
    }

    //Stops the service and restarts if allowed
    public static void restart(Context context, boolean forceStop)
    {
        Intent restartIntent = getStartIntent(context, false, false, PowerTypes.Balanced);
        restartIntent.putExtra(ParamTypes.MessageType, MessageTypes.NeedRestart);
        restartIntent.putExtra(ParamTypes.ForceStop, forceStop);
        Globals.startService(context, restartIntent, false);
    }

    //Starts service from the given context
    public static void start(Context context, int flags)
    {
        boolean gettingLocation = ((flags & FLAG_START_GET_LOCATION) == FLAG_START_GET_LOCATION);
        boolean runForeground = ((flags & FLAG_START_RUN_FOREGROUND) == FLAG_START_RUN_FOREGROUND);
        boolean highPower = ((flags & FLAG_START_HIGH_POWER) == FLAG_START_HIGH_POWER);
        boolean highPowerOnce = ((flags & FLAG_START_HIGH_POWER_ONCE) == FLAG_START_HIGH_POWER_ONCE);
        Globals.startService(context, getStartIntent(context, gettingLocation, runForeground, highPowerOnce ? PowerTypes.HighPowerThenBalanced : highPower ? PowerTypes.HighPower : PowerTypes.Balanced), runForeground);
    }
}
