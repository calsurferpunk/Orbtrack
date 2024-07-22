package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import java.util.TimeZone;


public class MapLocationInputActivity extends BaseInputActivity
{
    public static abstract class ParamTypes
    {
        static final String ForSearch = "forSearch";
    }

    private static class MapLocation
    {
        public double latitude;
        public double longitude;

        private MapLocation()
        {
            latitude = 0;
            longitude = 0;
        }

        public void setLocation(LatLng loc)
        {
            latitude = loc.latitude;
            longitude = loc.longitude;
        }

        public void setLatitude(double lat)
        {
            latitude = lat;
        }

        public void setLongitude(double lon)
        {
            longitude = lon;
        }
    }

    private Intent resultData;
    private LocationReceiver locationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_location_input_layout);

        //get displays and task
        String text;
        Bundle args = new Bundle();
        Intent intent = this.getIntent();
        final boolean forSearch = intent.getBooleanExtra(ParamTypes.ForSearch, false);
        final int primaryColor = Globals.resolveColorID(this, R.attr.colorPrimary);
        final int poweredByVisible = (forSearch ? View.VISIBLE : View.GONE);
        final int coordinateInputVisible = (forSearch ? View.GONE : View.VISIBLE);
        final MapLocation currentLocation = new MapLocation();
        final CoordinatesFragment mapInputView = new Whirly.MapFragment();
        final CustomSlider mapZoomBar = this.findViewById(R.id.Location_Zoom_Bar);
        final LinearLayout locationLayout = this.findViewById(R.id.Location_Layout);
        final LinearLayout altitudeLayout = this.findViewById(R.id.Location_Altitude_Layout);
        final LinearLayout timeZoneLayout = this.findViewById(R.id.Location_Time_Zone_Layout);
        final TextInputLayout altitudeTextLayout = this.findViewById(R.id.Location_Altitude_Text_Layout);
        final Resources res = this.getResources();
        final String invalidString = res.getString(R.string.title_invalid);
        final String latitudeString = res.getString(R.string.title_latitude);
        final String longitudeString = res.getString(R.string.title_longitude);
        final String altitudeString = res.getString(R.string.title_altitude);
        final AutoCompleteTextView nameText = this.findViewById(R.id.Location_Input_Name);
        final AppCompatImageView poweredGoogleImage = this.findViewById(R.id.Location_Powered_Google_Image);
        final EditText latitudeText = this.findViewById(R.id.Location_Latitude_Text);
        final EditText longitudeText = this.findViewById(R.id.Location_Longitude_Text);
        final EditText altitudeText = this.findViewById(R.id.Location_Altitude_Text);
        final SelectListInterface timeZoneList = this.findViewById(R.id.Location_Time_Zone_Text_List);
        final AppCompatImageButton altitudeButton = this.findViewById(R.id.Location_Altitude_Button);
        final AppCompatImageButton timeZoneButton = this.findViewById(R.id.Location_Time_Zone_Button);
        final MaterialButton cancelButton = this.findViewById(R.id.Location_Cancel_Button);
        final MaterialButton addButton = this.findViewById(R.id.Location_Add_Button);

        //set defaults
        resultData = new Intent();
        BaseInputActivity.setRequestCode(resultData, BaseInputActivity.getRequestCode(intent));

        //replace view
        args.putInt(Whirly.ParamTypes.MapLayerType, Settings.getMapLayerType(this, false));
        this.getSupportFragmentManager().beginTransaction().replace(R.id.Location_Map_View, (Fragment)mapInputView).commit();

        //update available inputs
        poweredGoogleImage.setVisibility(poweredByVisible);
        poweredGoogleImage.setImageResource(Settings.getDarkTheme(this) ? R.drawable.powered_by_google_on_non_white : R.drawable.powered_by_google_on_white);
        latitudeText.setVisibility(coordinateInputVisible);
        longitudeText.setVisibility(coordinateInputVisible);
        text = altitudeString + " (" + Globals.getMetersLabel(res) + ")";
        if(altitudeTextLayout != null)
        {
            altitudeTextLayout.setHint(text);
        }
        if(altitudeLayout != null)
        {
            altitudeLayout.setVisibility(coordinateInputVisible);
        }
        altitudeText.setVisibility(coordinateInputVisible);
        altitudeButton.setVisibility(coordinateInputVisible);
        timeZoneList.setVisibility(coordinateInputVisible);
        if(timeZoneLayout != null)
        {
            timeZoneLayout.setVisibility(coordinateInputVisible);
        }
        timeZoneButton.setVisibility(coordinateInputVisible);
        if(forSearch)
        {
            altitudeText.setText(R.string.text_0);
            addButton.setEnabled(false);
        }
        else
        {
            ViewCompat.setBackgroundTintList(altitudeButton, ColorStateList.valueOf(primaryColor));
            ViewCompat.setBackgroundTintList(timeZoneButton, ColorStateList.valueOf(primaryColor));
        }

        //setup and show dialog
        this.setTitle(R.string.title_location_input);
        mapInputView.setOnReadyListener(new CoordinatesFragment.OnReadyListener()
        {
            //get name of location
            private void resolveLocation(MapLocation location)
            {
                AddressUpdateService.getResolvedLocation(MapLocationInputActivity.this, location.latitude, location.longitude, new AddressUpdateService.OnLocationResolvedListener()
                {
                    @Override
                    public void onLocationResolved(String locationString, int resultCode)
                    {
                        nameText.setError(null);
                        nameText.setText(locationString);
                        addButton.setEnabled(true);
                    }
                }, false);
            }

            @Override
            public void ready()
            {
                final CoordinatesFragment.MarkerBase currentMarker = mapInputView.addMarker(MapLocationInputActivity.this, Universe.IDs.CurrentLocation, new Calculations.ObserverType());
                mapInputView.setOnLocationClickListener(new CoordinatesFragment.OnLocationClickListener()
                {
                    @Override
                    //public void onMapClick(LatLng latLng)
                    public void click(double latitude, double longitude)
                    {
                        //update location text
                        latitudeText.setError(null);
                        latitudeText.setText(String.valueOf(latitude));
                        longitudeText.setError(null);
                        longitudeText.setText(String.valueOf(longitude));
                        currentLocation.setLocation(new LatLng(latitude, longitude));
                        currentMarker.moveLocation(latitude, longitude, 0);
                        resolveLocation(currentLocation);
                    }
                });
                CoordinatesFragment.Utils.setupZoomSlider(mapInputView, mapZoomBar, false);
                latitudeText.addTextChangedListener(new TextWatcher()
                {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s)
                    {
                        //if a valid coordinate
                        double lat = Globals.tryParseDouble(s.toString());
                        if(lat >= -90 && lat <= 90)
                        {
                            //update location and move map
                            currentLocation.setLatitude(lat);
                            currentMarker.moveLocation(currentLocation.latitude, currentLocation.longitude, 0);
                            mapInputView.moveCamera(currentLocation.latitude, currentLocation.longitude);
                        }
                        else
                        {
                            //set error
                            latitudeText.setError(invalidString + " " + latitudeString);
                        }
                    }
                });
                longitudeText.addTextChangedListener(new TextWatcher()
                {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s)
                    {
                        //if a valid coordinate
                        double lon = Globals.tryParseDouble(s.toString());
                        if(lon >= -180 && lon <= 180)
                        {
                            //update location and move map
                            currentLocation.setLongitude(lon);
                            currentMarker.moveLocation(currentLocation.latitude, currentLocation.longitude, 0);
                            mapInputView.moveCamera(currentLocation.latitude, currentLocation.longitude);
                        }
                        else
                        {
                            //set error
                            longitudeText.setError(invalidString + " " + longitudeString);
                        }
                    }
                });
            }
        });
        if(!forSearch)
        {
            //add all time zones, set to default, and set button click
            timeZoneList.setAdapter(new IconSpinner.CustomAdapter(this, Globals.getTimeZoneList()));
            timeZoneList.setSelectedValue(TimeZone.getDefault().getID());
            timeZoneButton.setOnClickListener(new View.OnClickListener()
            {
                private long lastTimeMs = 0;

                @Override
                public void onClick(View v)
                {
                    double userLat = Globals.tryParseDouble(latitudeText.getText().toString());
                    double userLon = Globals.tryParseDouble(longitudeText.getText().toString());
                    long timeMs = System.currentTimeMillis();
                    long timeSinceMs = (timeMs - lastTimeMs);
                    LocationService.GetTimezoneTask timezoneTask;

                    //if enough time between clicks
                    if(timeSinceMs >= 5000)
                    {
                        //if valid latitude and longitude
                        if(userLat >= -90 && userLat <= 90 && userLon >= -180 && userLon <= 180)
                        {
                            //get timezone
                            timezoneTask = new LocationService.GetTimezoneTask(userLat, userLon, new LocationService.GetTimezoneTask.OnGotTimezoneListener()
                            {
                                @Override
                                public void onGotTimeZone(final String zoneId)
                                {
                                    MapLocationInputActivity.this.runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            //update text and button
                                            timeZoneList.setSelectedValue(zoneId);
                                            timeZoneButton.setEnabled(true);
                                        }
                                    });
                                }
                            });
                            timezoneTask.execute(MapLocationInputActivity.this);

                            //update last time and button
                            lastTimeMs = timeMs;
                            timeZoneButton.setEnabled(false);
                        }
                    }
                    else
                    {
                        //show please wait message
                        Toast.makeText(MapLocationInputActivity.this, R.string.text_please_wait, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            //set altitude button click
            altitudeButton.setOnClickListener(new View.OnClickListener()
            {
                private long lastTimeMs = 0;

                @Override
                public void onClick(View v)
                {
                    double userLat = Globals.tryParseDouble(latitudeText.getText().toString());
                    double userLon = Globals.tryParseDouble(longitudeText.getText().toString());
                    long timeMs = System.currentTimeMillis();
                    long timeSinceMs = (timeMs - lastTimeMs);
                    LocationService.GetAltitudeTask altitudeTask;

                    //if enough time between clicks
                    if(timeSinceMs >= 5000)
                    {
                        //if valid latitude and longitude
                        if(userLat >= -90 && userLat <= 90 && userLon >= -180 && userLon <= 180)
                        {
                            //reset text
                            altitudeText.setText(R.string.empty);

                            //get altitude
                            altitudeTask = new LocationService.GetAltitudeTask(userLat, userLon, new LocationService.GetAltitudeTask.OnGotAltitudeListener()
                            {
                                @Override
                                public void onGotAltitude(final Location result)
                                {
                                    MapLocationInputActivity.this.runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            //update text and button
                                            altitudeText.setText(String.valueOf(result.getAltitude()));
                                            altitudeButton.setEnabled(true);
                                        }
                                    });
                                }
                            });
                            altitudeTask.execute(MapLocationInputActivity.this);

                            //update last time and button
                            lastTimeMs = timeMs;
                            altitudeButton.setEnabled(false);
                        }
                    }
                    else
                    {
                        //show please wait message
                        Toast.makeText(MapLocationInputActivity.this, R.string.text_please_wait, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        nameText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s)
            {
                if(forSearch)
                {
                    latitudeText.setText(null);
                    longitudeText.setText(null);
                    addButton.setEnabled(false);
                }
            }
        });
        addButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                double userLat = (forSearch ? currentLocation.latitude : Globals.tryParseDouble(latitudeText.getText().toString()));
                double userLon = (forSearch ? currentLocation.longitude : Globals.tryParseDouble(longitudeText.getText().toString()));
                double userAlt = Globals.tryParseDouble(altitudeText.getText().toString());
                final String name = nameText.getText().toString().trim();
                String currentError = "";
                LocationService.GetAltitudeTask altitudeTask;

                //check for invalid values
                if(name.isEmpty() || name.equalsIgnoreCase("current"))
                {
                    currentError = invalidString + " " + res.getString(R.string.title_name);
                    nameText.setError(currentError);
                }
                if(userLat < -90 || userLat > 90)
                {
                    currentError = invalidString + " " + latitudeString;
                    latitudeText.setError(currentError);
                }
                if(userLon < -180 || userLon > 180)
                {
                    currentError = invalidString + " " + longitudeString;
                    longitudeText.setError(currentError);
                }
                if(!forSearch && userAlt == Double.MAX_VALUE)
                {
                    currentError = invalidString + " " + altitudeString;
                    altitudeText.setError(currentError);
                }

                //if there is no error
                if(currentError.isEmpty())
                {
                    //disable inputs
                    nameText.setEnabled(false);
                    latitudeText.setEnabled(false);
                    longitudeText.setEnabled(false);
                    altitudeText.setEnabled(false);
                    altitudeButton.setEnabled(false);
                    timeZoneList.setEnabled(false);
                    addButton.setEnabled(false);
                    cancelButton.setEnabled(false);

                    //if for search
                    if(forSearch)
                    {
                        //get altitude
                        altitudeTask = new LocationService.GetAltitudeTask(userLat, userLon, new LocationService.GetAltitudeTask.OnGotAltitudeListener()
                        {
                            @Override
                            public void onGotAltitude(final Location result)
                            {
                                final double latitude = result.getLatitude();
                                final double longitude = result.getLongitude();
                                final double altitudeM = result.getAltitude();

                                //get time zone
                                LocationService.GetTimezoneTask timezoneTask = new LocationService.GetTimezoneTask(latitude, longitude, new LocationService.GetTimezoneTask.OnGotTimezoneListener()
                                {
                                    @Override
                                    public void onGotTimeZone(String zoneId)
                                    {
                                        //set location with found time zone
                                        setLocation(MapLocationInputActivity.this, name, latitude, longitude, altitudeM, zoneId);
                                    }
                                });
                                timezoneTask.execute(MapLocationInputActivity.this);
                            }
                        });
                        altitudeTask.execute(MapLocationInputActivity.this);
                    }
                    else
                    {
                        //set location with given altitude
                        setLocation(MapLocationInputActivity.this, name, userLat, userLon, (Settings.getUsingMetric() ? userAlt : (userAlt / Globals.FEET_PER_METER)), (String)timeZoneList.getSelectedValue(TimeZone.getDefault().getID()));
                    }
                }
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //close activity
                setResult(RESULT_CANCELED, resultData);
                MapLocationInputActivity.this.finish();
            }
        });

        //if for searching
        if(forSearch)
        {
            if(locationReceiver != null)
            {
                locationReceiver.unregister();
            }
            locationReceiver = new LocationReceiver(LocationService.FLAG_START_NONE)
            {
                @Override
                protected Activity getActivity()
                {
                    return(MapLocationInputActivity.this);
                }

                @Override
                protected View getParentView()
                {
                    return(locationLayout);
                }

                @Override
                protected void onConnected(Context context, PlacesClient placesClient)
                {
                    //setup name autocompletion
                    LocationService.setAutoCompletePlaces(nameText, context, placesClient, new LocationService.OnGotCoordinatesListener()
                    {
                        @Override
                        public void gotCoordinates(final double latitude, final double longitude)
                        {
                            MapLocationInputActivity.this.runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    //update latitude and longitude
                                    latitudeText.setText(String.valueOf(latitude));
                                    longitudeText.setText(String.valueOf(longitude));
                                    addButton.setEnabled(true);
                                }
                            });
                        }
                    });
                }
            };
            locationReceiver.register(this);
            locationReceiver.startLocationService(this);
        }
        else
        {
            //disable autocompletion
            nameText.setAdapter(null);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if(locationReceiver != null)
        {
            locationReceiver.unregister();
        }
    }

    //Set the location and finish
    private void setLocation(Context context, String name, double latitude, double longitude, double altitudeM, String zoneId)
    {
        //save location and set result
        Database.saveLocation(context, name, latitude, longitude, altitudeM, zoneId, Database.LocationType.Saved, true);
        setResult(RESULT_OK, resultData);

        //close activity
        MapLocationInputActivity.this.finish();
    }

    //Shows activity
    public static void show(Activity context, ActivityResultLauncher<Intent> launcher, boolean forSearch)
    {
        Intent intent = new Intent(context, MapLocationInputActivity.class);
        intent.putExtra(ParamTypes.ForSearch, forSearch);
        Globals.startActivityForResult(launcher, intent, BaseInputActivity.RequestCode.MapLocationInput);
    }
}
