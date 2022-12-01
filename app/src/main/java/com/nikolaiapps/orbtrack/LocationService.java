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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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


public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener
{
    //Constants
    static final long LOCATION_UPDATE_RATE_MS = 3600000;        //1 hour
    static final String LOCATION_FILTER = "locationServiceFilter";
    static final byte FLAG_START_NONE = 0x00;
    static final byte FLAG_START_GET_LOCATION = 0x01;
    static final byte FLAG_START_RUN_FOREGROUND = 0x02;
    static final byte FLAG_START_REOPEN = 0x04;
    static final byte FLAG_START_HIGH_POWER = 0x08;
    static final byte FLAG_START_HIGH_POWER_ONCE = 0x10;

    interface OnGotCoordinatesListener
    {
        void gotCoordinates(double latitude, double longitude);
    }

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
        static final String RunForeGround = "runForeGround";
        static final String Reopen = "reopen";
        static final String PowerType = "powerType";
        static final String CloseOnChange = "closeOnChange";
        static final String ForceClose = "forceClose";
        static final String CheckClose = "checkClose";
        static final String FromUser = "fromUser";
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
                                if(!id.equals(""))
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
            if(action != null && action.equals(NotifyReceiver.RetryAction))
            {
                //start again
                LocationService.start(context, FLAG_START_GET_LOCATION | FLAG_START_RUN_FOREGROUND | FLAG_START_REOPEN);
            }
            else
            {
                //restart without being in foreground
                LocationService.restart(context, intent.getBooleanExtra(ParamTypes.ForceClose, false), true);
            }

            //close notification
            manager.cancel(Globals.ChannelIds.Location);
        }
    }

    private static byte usePowerType = PowerTypes.Balanced;
    private static boolean useLegacy = false;
    private static boolean testingAPI = false;
    private static boolean needTestAPI = true;
    private static String legacyProvider = null;
    private static Runnable timeoutTask = null;
    private static Handler timeoutTimer = null;
    private static Location currentLocation = null;
    private static GoogleApiClient googleLocationApiClient = null;
    private static PlacesClient googlePlacesClient = null;
    private static LocationManager legacyClient = null;
    private boolean getUpdates = false;
    private boolean getLocation = false;
    private boolean closeAfterChange = false;
    private android.location.LocationListener legacyListener;
    private LocationCallback locationListener = null;
    private LocationRequest locationRequester = null;
    private FusedLocationProviderClient locationClient = null;
    private LocalBroadcastManager localBroadcast = null;
    private NotificationCompat.Builder notifyBuilder = null;

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId)
    {
        byte messageType = intent.getByteExtra(ParamTypes.MessageType, Byte.MAX_VALUE);
        byte powerType = intent.getByteExtra(ParamTypes.PowerType, PowerTypes.Balanced);
        boolean gettingLocation = intent.getBooleanExtra(ParamTypes.GetLocation, false);
        boolean runForeGround = intent.getBooleanExtra(ParamTypes.RunForeGround, false);
        boolean reopen = intent.getBooleanExtra(ParamTypes.Reopen, false);
        boolean closeOnChange = intent.getBooleanExtra(ParamTypes.CloseOnChange, false);
        boolean forceClose = intent.getBooleanExtra(ParamTypes.ForceClose, false);
        boolean fromUser = intent.getBooleanExtra(ParamTypes.FromUser, true);
        String notifyChannelId = Globals.getChannelId(Globals.ChannelIds.Location);
        final Resources res = this.getResources();

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

            //if getting location
            if(gettingLocation)
            {
                //stop any running timer
                stopTimeoutTimer(this);
            }

            //if not just restarting/stopping
            if(messageType != MessageTypes.NeedRestart)
            {
                //if reopening
                if(reopen)
                {
                    //stop all updates
                    stopAllUpdates();
                }

                //if closing after location change
                if(closeOnChange)
                {
                    //close if not already getting updates or starting in foreground
                    closeAfterChange = (!getUpdates || runForeGround);
                }

                //if running in foreground
                if(runForeGround)
                {
                    Intent dismissIntent;
                    final NotificationManagerCompat notifyManager;

                    //create notification channel
                    Globals.createNotifyChannel(this, notifyChannelId);

                    //setup notification
                    notifyManager = NotificationManagerCompat.from(this);
                    notifyBuilder = Globals.createNotifyBuilder(this, notifyChannelId);
                    notifyBuilder.setContentTitle("Orbtrack " + res.getString(R.string.title_widget)).setContentText(res.getString(R.string.text_location_watching));

                    //add stop button
                    dismissIntent = new Intent(this, NotifyReceiver.class);
                    dismissIntent.setAction(NotifyReceiver.DismissAction);
                    dismissIntent.putExtra(ParamTypes.ForceClose, true);
                    notifyBuilder.addAction(new NotificationCompat.Action(0, res.getString(R.string.title_stop), Globals.getPendingBroadcastIntent(this, NotifyReceiver.StopID, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT)));

                    //if getting location
                    if(gettingLocation)
                    {
                        //set timeout to run in 15 seconds
                        timeoutTimer = new Handler();
                        timeoutTask = new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                //show retry
                                Intent retryIntent = new Intent(LocationService.this, NotifyReceiver.class);
                                retryIntent.setAction(NotifyReceiver.RetryAction);
                                notifyBuilder.setContentText(res.getString(R.string.text_location_failed));
                                notifyBuilder.addAction(new NotificationCompat.Action(0, res.getString(R.string.title_retry), Globals.getPendingBroadcastIntent(LocationService.this, NotifyReceiver.RetryID, retryIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                                notifyManager.notify(Globals.ChannelIds.Location, notifyBuilder.build());
                            }
                        };
                        timeoutTimer.postDelayed(timeoutTask, 15000);
                    }
                }
                Globals.startForeground(this, Globals.ChannelIds.Location, notifyBuilder, runForeGround);

                //if broadcaster does not exist yet
                if(localBroadcast == null)
                {
                    //get broadcaster
                    localBroadcast = LocalBroadcastManager.getInstance(this);
                }

                //make sure connected
                connect(gettingLocation);

                //if getting location
                if(gettingLocation)
                {
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
                    }
                    //else if location services are not enabled
                    else if(!Globals.haveLocationEnabled(this))
                    {
                        //send need enable broadcast
                        sendNeedEnable();
                    }
                }
            }
            else
            {
                //stop
                stopSelf();

                //send need restart
                sendNeedRestart(forceClose, !fromUser);
            }
        }

        return(START_NOT_STICKY);
    }

    @Override
    public void onDestroy()
    {
        stopAllUpdates();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return(null);
    }

    //Creates a google API client
    private GoogleApiClient createLocationClient(GoogleApiClient.ConnectionCallbacks connectionCallbacks, GoogleApiClient.OnConnectionFailedListener connectionFailedListener)
    {
        //return(new GoogleApiClient.Builder(this).addConnectionCallbacks(connectionCallbacks).addOnConnectionFailedListener(connectionFailedListener).addApi(LocationServices.API).addApi(Places.GEO_DATA_API).build());
        return(new GoogleApiClient.Builder(this).addConnectionCallbacks(connectionCallbacks).addOnConnectionFailedListener(connectionFailedListener).addApi(LocationServices.API).build());
    }

    //Creates a google places client
    private PlacesClient createPlacesClient()
    {
        Places.initialize(this.getApplicationContext(), this.getResources().getString(R.string.google_places_api_key));
        return(Places.createClient(this));
    }

    //Connect to get location
    private void connect(boolean gettingLocation)
    {
        List<String> legacyProviders;

        //if getting location
        if(gettingLocation)
        {
            //update status
            getLocation = true;
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
                legacyProviders = (legacyClient != null ? legacyClient.getAllProviders() : new ArrayList<>(0));

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

            //handle on connected
            onConnected(null);
        }
        else
        {
            //if need to connect
            if(googleLocationApiClient == null)
            {
                //create location API client and connect
                googleLocationApiClient = createLocationClient(this, this);
                googleLocationApiClient.connect();

                //create places client and connect
                googlePlacesClient = createPlacesClient();
            }
            //else if already connected
            else if(googleLocationApiClient.isConnected())
            {
                //handle on connected again
                onConnected(null);
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        //send broadcast
        sendConnected();

        //if getting location and have location permission
        if(getLocation && Globals.haveLocationPermission(this))
        {
            //start getting updates
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        //reset
        stopLocationUpdates();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        //reset
        stopLocationUpdates();
    }

    @Override
    public void onLocationChanged(@NonNull Location location)
    {
        //stop timer and any running task
        stopTimeoutTimer(this);

        //set current location and remember it
        currentLocation = location;
        Settings.setLastLocation(LocationService.this, currentLocation);

        //try to send existing/last location
        sendLocation();
    }

    //Gets a new start intent
    private static Intent getStartIntent(Context context, boolean gettingLocation, boolean runForeground, boolean reopen, byte powerType)
    {
        Intent startIntent = new Intent(context, LocationService.class);
        startIntent.putExtra(ParamTypes.GetLocation, gettingLocation);
        startIntent.putExtra(ParamTypes.RunForeGround, runForeground);
        startIntent.putExtra(ParamTypes.Reopen, reopen);
        startIntent.putExtra(ParamTypes.PowerType, powerType);
        return(startIntent);
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
            localBroadcast.sendBroadcast(getBroadcastIntent(MessageTypes.NeedEnable));
        }
    }

    //Sends a need permission broadcast
    private void sendNeedPermission()
    {
        if(localBroadcast != null)
        {
            localBroadcast.sendBroadcast(getBroadcastIntent(MessageTypes.NeedPermission));
        }
    }

    //Sends a denied broadcast
    private void sendDenied()
    {
        if(localBroadcast != null)
        {
            localBroadcast.sendBroadcast(getBroadcastIntent(MessageTypes.Denied));
        }
    }

    //Sends a need restart
    private void sendNeedRestart(boolean forceClose, boolean checkClose)
    {
        Intent restartIntent = getBroadcastIntent(MessageTypes.NeedRestart);
        restartIntent.putExtra(ParamTypes.ForceClose, forceClose);
        restartIntent.putExtra(ParamTypes.CheckClose, checkClose);

        if(localBroadcast != null)
        {
            localBroadcast.sendBroadcast(restartIntent);
        }
    }

    //Sends a location broadcast
    private void sendLocation(Calculations.ObserverType observer)
    {
        Intent locationIntent = getBroadcastIntent(MessageTypes.Location);

        //if location is set
        if(observer != null)
        {
            locationIntent.putExtra(ParamTypes.Observer, observer);
            if(localBroadcast != null)
            {
                localBroadcast.sendBroadcast(locationIntent);
            }
        }
    }
    private void sendLocation()
    {
        Calculations.ObserverType lastLocation = getLastLocation();
        NotificationManagerCompat notifyManager;

        //if a valid location
        if(lastLocation.geo.isSet())
        {
            //send last known
            sendLocation(lastLocation);

            //if were using high power and now want balanced
            if(usePowerType == PowerTypes.HighPowerThenBalanced)
            {
                //use balanced now
                usePowerType = PowerTypes.Balanced;
                closeAfterChange = true;
            }

            //if closing after change
            if(closeAfterChange)
            {
                notifyManager = NotificationManagerCompat.from(this);
                notifyManager.cancel(Globals.ChannelIds.Location);

                //stop
                stopSelf();
                closeAfterChange = false;

                //send need restart
                sendNeedRestart(true, true);
            }
        }
    }

    //Sends on connected broadcast
    private void sendConnected()
    {
        if(localBroadcast != null)
        {
            localBroadcast.sendBroadcast(getBroadcastIntent(MessageTypes.Connected));
        }
    }

    //Stop any running timeout timer
    private static void stopTimeoutTimer(Context context)
    {
        NotificationManagerCompat notifyManager = NotificationManagerCompat.from(context);

        //if timer was running
        if(timeoutTimer != null)
        {
            //if task is set
            if(timeoutTask != null)
            {
                //cancel it
                timeoutTimer.removeCallbacks(timeoutTask);
            }

            //clear it
            timeoutTask = null;
        }

        //cancel any location notification
        notifyManager.cancel(Globals.ChannelIds.Location);
    }

    //Stop getting location updates
    private void stopLocationUpdates()
    {
        //stop any running task
        stopTimeoutTimer(this);

        //if getting updates
        if(getUpdates)
        {
            //stop getting updates
            getUpdates = false;
            if(legacyClient != null && legacyListener != null)
            {
                legacyClient.removeUpdates(legacyListener);
            }
            if(locationClient != null && locationRequester != null)
            {
                locationClient.removeLocationUpdates(locationListener);
            }
        }
    }

    //Stop all updates
    private void stopAllUpdates()
    {
        //stop and reset
        stopLocationUpdates();
        getLocation = closeAfterChange = false;
        localBroadcast = null;
        if(useLegacy && legacyClient != null)
        {
            legacyClient.removeUpdates(legacyListener);
            legacyClient = null;
        }
        if(!useLegacy && googleLocationApiClient != null)
        {
            googleLocationApiClient.unregisterConnectionCallbacks(this);
            googleLocationApiClient.unregisterConnectionFailedListener(this);
            googleLocationApiClient.disconnect();
            googleLocationApiClient = null;
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
                        LocationService.this.onLocationChanged(location);
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
                        super.onLocationResult(locationResult);
                        LocationService.this.onLocationChanged(locationResult.getLastLocation());
                    }
                };
            }
        }
    }

    //Create location requester for continuous updates
    private void createLocationRequester()
    {
        //if not using legacy and location requester does not exist yet
        if(!useLegacy && locationRequester == null)
        {
            locationRequester = LocationRequest.create();
            locationRequester.setInterval(LOCATION_UPDATE_RATE_MS);
            locationRequester.setFastestInterval(LOCATION_UPDATE_RATE_MS / 4);
            locationRequester.setPriority(usePowerType == PowerTypes.HighPower || usePowerType == PowerTypes.HighPowerThenBalanced ? LocationRequest.PRIORITY_HIGH_ACCURACY : LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        }
    }

    //Sends last client location
    private void sendLastClientLocation() throws SecurityException
    {
        //if using legacy
        if(useLegacy)
        {
            //if client is set
            if(legacyClient != null && legacyProvider != null)
            {
                //try to send last location
                LocationService.this.onLocationChanged(legacyClient.getLastKnownLocation(legacyProvider));
            }
        }
        else
        {
            //if client is set
            if(locationClient != null)
            {
                //try to send last location
                locationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>()
                {
                    @Override
                    public void onSuccess(Location location)
                    {
                        //update with last known location
                        LocationService.this.onLocationChanged(location);
                    }
                });
            }
        }
    }

    //Start getting location updates
    private void startLocationUpdates()
    {
        //if have permission to get location
        if(Globals.haveLocationPermission(this))
        {
            //if not already getting updates
            if(!getUpdates)
            {
                //start getting updates
                getUpdates = true;
                createLocationRequester();
                try
                {
                    //if using legacy
                    if(useLegacy)
                    {
                        //get updates
                        createLocationListener();
                        if(legacyProvider != null)
                        {
                            legacyClient.requestLocationUpdates(legacyProvider, LOCATION_UPDATE_RATE_MS, 0, legacyListener);
                        }

                        //send last client location
                        sendLastClientLocation();
                    }
                    else
                    {
                        //if connected already
                        if(googleLocationApiClient.isConnected())
                        {
                            //get updates
                            createLocationListener();
                            locationClient = LocationServices.getFusedLocationProviderClient(this);
                            locationClient.requestLocationUpdates(locationRequester, locationListener, Looper.myLooper()).addOnCompleteListener(new OnCompleteListener<Void>()
                            {
                                @Override
                                public void onComplete(@NonNull Task<Void> task)
                                {
                                    //send last client location
                                    sendLastClientLocation();
                                }
                            });
                        }
                        else
                        {
                            //allow for retry when connected
                            getUpdates = false;
                        }
                    }
                }
                catch(SecurityException ex)
                {
                    //send denied
                    getUpdates = false;
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

    //Return last known location
    private Calculations.ObserverType getLastLocation()
    {
        String zoneId = TimeZone.getDefault().getID();
        Location lastLocationSaved;
        Calculations.ObserverType lastLocation;

        //if have a location
        if(currentLocation != null)
        {
            //get last received location
            lastLocation = Calculations.loadObserver(currentLocation.getLatitude(), currentLocation.getLongitude(), currentLocation.getAltitude() / 1000f, zoneId);
        }
        else
        {
            //try to get last saved location
            lastLocationSaved = Settings.getLastLocation(LocationService.this);
            lastLocation = Calculations.loadObserver(lastLocationSaved.getLatitude(), lastLocationSaved.getLongitude(), lastLocationSaved.getAltitude() / 1000f, zoneId);
        }

        //return last location
        return(lastLocation);
    }

    //Gets current location
    public static void getCurrentLocation(Context context, boolean runForeground, boolean keepOpen, byte powerType)
    {
        Intent startIntent = getStartIntent(context, true, runForeground, false, powerType);
        startIntent.putExtra(ParamTypes.CloseOnChange, !keepOpen);
        Globals.startService(context, startIntent, runForeground);
    }
    public static void getCurrentLocation(Context context, boolean runForeground, boolean keepOpen)
    {
        getCurrentLocation(context, runForeground, keepOpen, PowerTypes.Balanced);
    }

    //Gets google location api client
    public static GoogleApiClient getGoogleLocationApiClient()
    {
        return(useLegacy ? null : googleLocationApiClient);
    }

    //Gets google places client
    public static PlacesClient getGooglePlacesClient()
    {
        return(useLegacy ? null : googlePlacesClient);
    }

    //Starts service from the given context
    public static void start(Context context, int flags)
    {
        boolean gettingLocation = ((flags & FLAG_START_GET_LOCATION) == FLAG_START_GET_LOCATION);
        boolean runForeground = ((flags & FLAG_START_RUN_FOREGROUND) == FLAG_START_RUN_FOREGROUND);
        boolean reopen = ((flags & FLAG_START_REOPEN) == FLAG_START_REOPEN);
        boolean highPower = ((flags & FLAG_START_HIGH_POWER) == FLAG_START_HIGH_POWER);
        boolean highPowerOnce = ((flags & FLAG_START_HIGH_POWER_ONCE) == FLAG_START_HIGH_POWER_ONCE);
        Globals.startService(context, getStartIntent(context, gettingLocation, runForeground, reopen, highPowerOnce ? PowerTypes.HighPowerThenBalanced : highPower ? PowerTypes.HighPower : PowerTypes.Balanced), runForeground);
    }

    //Stops the service and sends a need restart
    public static void restart(Context context, boolean forceClose, boolean fromUser)
    {
        Intent restartIntent = getStartIntent(context, false, false, false, PowerTypes.Balanced);
        restartIntent.putExtra(ParamTypes.MessageType, MessageTypes.NeedRestart);
        restartIntent.putExtra(ParamTypes.ForceClose, forceClose);
        restartIntent.putExtra(ParamTypes.FromUser, fromUser);
        Globals.startService(context, restartIntent, false);
    }

    //Show permission denied
    public static void showDenied(Context context, View parentView)
    {
        //show denied and don't ask again
        Globals.showDenied(parentView, context.getResources().getString(R.string.desc_permission_location_deny));
        Globals.canAskLocationPermission = false;
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
                        googlePlacesClient.fetchPlace(FetchPlaceRequest.builder(placeId, placeResult).build()).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>()
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
}
