package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TableRow;
import android.widget.TextView;
import com.google.android.gms.common.api.GoogleApiClient;
import java.util.ArrayList;
import java.util.Map;


public abstract class WidgetBaseSetupActivity extends BaseInputActivity
{
    static
    {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static abstract class PreferenceName
    {
        private static final String Name = "name";
        private static final String NoradID = "noradId";
        private static final String OrbitalType = "orbitalType";
        private static final String GlobalText = "globalText";
        private static final String GlobalTextSize = "globalTextSize";
        private static final String GlobalTextColor = "globalTextColor";
        private static final String GlobalTextWeight = "globalTextWeight";
        private static final String OrbitalTextSize = "orbitalTextSize";
        private static final String OrbitalTextColor = "orbitalTextColor";
        private static final String OrbitalTextWeight = "orbitalTextWeight";
        private static final String PassStartTextSize = "passStartTextSize";
        private static final String PassStartTextColor = "passStartTextColor";
        private static final String PassStartTextWeight = "passStartTextWeight";
        private static final String PassEndTextSize = "passEndTextSize";
        private static final String PassEndTextColor = "passEndTextColor";
        private static final String PassEndTextWeight = "passEndTextWeight";
        private static final String PassElevationTextSize = "passElevationTextSize";
        private static final String PassElevationTextColor = "passElevationTextColor";
        private static final String PassElevationTextWeight = "passElevationTextWeight";
        private static final String PassAzimuthStartTextSize = "passAzimuthStartTextSize";
        private static final String PassAzimuthStartTextColor = "passAzimuthStartTextColor";
        private static final String PassAzimuthStartTextWeight = "passAzimuthStartTextWeight";
        private static final String PassAzimuthEndTextSize = "passAzimuthEndTextSize";
        private static final String PassAzimuthEndTextColor = "passAzimuthEndTextColor";
        private static final String PassAzimuthEndTextWeight = "passAzimuthEndTextWeight";
        private static final String PassDurationTextSize = "passDurationTextSize";
        private static final String PassDurationTextColor = "passDurationTextColor";
        private static final String PassDurationTextWeight = "passDurationTextWeight";
        private static final String LocationTextSize = "locationTextSize";
        private static final String LocationTextColor = "locationTextColor";
        private static final String LocationTextWeight = "locationTextWeight";
        private static final String BorderColor = "borderColor";
        private static final String BorderType = "borderType";
        private static final String GlobalImage = "globalImage";
        private static final String GlobalImageColor = "globalImageColor";
        private static final String OrbitalImageColor = "orbitalImageColor";
        private static final String SettingsImageColor = "settingsImageColor";
        private static final String LocationImageColor = "locationImageColor";
        private static final String GlobalBackground = "globalBackground";
        private static final String GlobalBackgroundColor = "globalBackgroundColor";
        private static final String TopBackgroundColor = "topBackgroundColor";
        private static final String MiddleBackgroundColor = "middleBackgroundColor";
        private static final String BottomBackgroundColor = "bottomBackgroundColor";
        private static final String LocationSource = "locationSource";
        private static final String LocationID = "locationId";
        private static final String LocationName = "locationName";
        private static final String LocationLatitude = "locationLatitude";
        private static final String LocationLongitude = "locationLongitude";
        private static final String LocationFollow = "locationFollow";
        private static final String LocationInterval = "locationInterval";
    }

    private static abstract class ParamTypes
    {
        private static final String WidgetClass = "widgetClass";
    }

    private static abstract class TabPage
    {
        static final int Data = 0;
        static final int Images = 1;
        static final int Background = 2;
        static final int Text = 3;
        static final int TabCount = 4;
    }

    private static abstract class FontWeight
    {
        private static final String BoldItalic = "boldItalic";
        private static final String Bold = "bold";
        private static final String Italic = "italic";
        private static final String Normal = "normal";
    }

    public static abstract class BorderType
    {
        static final int Round = 1;
        static final int Square = 2;
    }

    private static abstract class TextType
    {
        static final byte Global = 0;
        static final byte Orbital = 1;
        static final byte PassStart = 2;
        static final byte PassEnd = 3;
        static final byte PassElevation = 4;
        static final byte PassAzimuthStart = 5;
        static final byte PassAzimuthEnd = 6;
        static final byte PassDuration = 7;
        static final byte Location = 8;
        static final byte TextCount = 9;
    }

    private static class TextSettings
    {
        public int color;
        public float size;
        public boolean bold;
        public boolean italic;

        public TextSettings(float textSize, int textColor, int textWeight)
        {
            size = textSize;
            color = textColor;
            bold = isBold(textWeight);
            italic = isItalic(textWeight);
        }

        public int getWeight()
        {
            return(bold && italic ? Typeface.BOLD_ITALIC : bold ? Typeface.BOLD : italic ? Typeface.ITALIC : Typeface.NORMAL);
        }
    }

    private static class LocationSettings
    {
        boolean useFollow;
        boolean useInterval;
        byte source;
        int id;
        int savedId;
        long intervalMs;
        String savedName;
        String searchName;
        String locationName;
        Calculations.ObserverType savedObserver;
        Calculations.ObserverType searchObserver;

        public LocationSettings(Context context, int widgetId, int noradId)
        {
            useFollow = getLocationFollow(context, widgetId);
            useInterval = getLocationInterval(context, widgetId);

            savedId = -1;
            id = getLocationId(context, widgetId);
            source = getLocationSource(context, widgetId);
            if(source == Database.LocationType.Current && !useFollow && !useInterval && noradId != Universe.IDs.Invalid)       //if used current location but not following or on an interval
            {
                source = Database.LocationType.New;
            }
            intervalMs = getLocationGlobalInterval(context);

            savedName = null;
            locationName = getLocationName(context, widgetId);
            searchName = (id == -1 ? locationName : null);

            savedObserver = new Calculations.ObserverType();
            searchObserver = new Calculations.ObserverType();
            if(id != -1)
            {
                savedObserver = getLocation(context, widgetId);
            }
            else
            {
                searchObserver = getLocation(context, widgetId);
            }
        }
    }

    private static class WidgetSettings
    {
        boolean useGlobalText;
        boolean useGlobalImage;
        boolean useGlobalBackground;
        int noradId;
        int borderType;
        int borderColor;
        int globalImageColor;
        int orbitalImageColor;
        int settingsImageColor;
        int locationImageColor;
        int globalBackgroundColor;
        int topBackgroundColor;
        int middleBackgroundColor;
        int bottomBackgroundColor;
        final TextSettings[] text;
        final String[][] textSettingsNames;
        final LocationSettings location;

        public WidgetSettings(Context context, Class<?> widgetClass, int widgetId)
        {
            useGlobalText = getGlobalText(context, widgetId);
            useGlobalImage = getGlobalImage(context, widgetId);
            useGlobalBackground = getGlobalBackground(context, widgetId);

            noradId = getNoradID(context, widgetId);
            borderType = getBorderType(context, widgetId);
            borderColor = getBorderColor(context, widgetId);
            globalImageColor = getGlobalImageColor(context, widgetId);
            orbitalImageColor = getOrbitalImageColor(context, widgetId);
            settingsImageColor = getSettingsImageColor(context, widgetId);
            locationImageColor = getLocationImageColor(context, widgetId);
            globalBackgroundColor = getGlobalBackgroundColor(context, widgetId);
            topBackgroundColor = getTopBackgroundColor(context, widgetId);
            middleBackgroundColor = getMiddleBackgroundColor(context, widgetId);
            bottomBackgroundColor = getBottomBackgroundColor(context, widgetId);

            text = new TextSettings[TextType.TextCount];
            text[TextType.Global] = new TextSettings(getGlobalTextSize(context, widgetClass, widgetId), getGlobalTextColor(context, widgetId), getGlobalTextWeight(context, widgetId));
            text[TextType.Orbital] = new TextSettings(getOrbitalTextSize(context, widgetClass, widgetId), getOrbitalTextColor(context, widgetId), getOrbitalTextWeight(context, widgetId));
            text[TextType.PassStart] = new TextSettings(getPassStartTextSize(context, widgetClass, widgetId), getPassStartTextColor(context, widgetId), getPassStartTextWeight(context, widgetId));
            text[TextType.PassEnd] = new TextSettings(getPassEndTextSize(context, widgetClass, widgetId), getPassEndTextColor(context, widgetId), getPassEndTextWeight(context, widgetId));
            text[TextType.PassElevation] = new TextSettings(getPassElevationTextSize(context, widgetClass, widgetId), getPassElevationTextColor(context, widgetId), getPassElevationTextWeight(context, widgetId));
            text[TextType.PassAzimuthStart] = new TextSettings(getPassAzimuthStartTextSize(context, widgetClass, widgetId), getPassAzimuthStartTextColor(context, widgetId), getPassAzimuthStartTextWeight(context, widgetId));
            text[TextType.PassAzimuthEnd] = new TextSettings(getPassAzimuthEndTextSize(context, widgetClass, widgetId), getPassAzimuthEndTextColor(context, widgetId), getPassAzimuthEndTextWeight(context, widgetId));
            text[TextType.PassDuration] = new TextSettings(getPassDurationTextSize(context, widgetClass, widgetId), getPassDurationTextColor(context, widgetId), getPassDurationTextWeight(context, widgetId));
            text[TextType.Location] = new TextSettings(getLocationTextSize(context, widgetClass, widgetId), getLocationTextColor(context, widgetId), getLocationTextWeight(context, widgetId));

            textSettingsNames = new String[TextType.TextCount][];
            textSettingsNames[TextType.Global] = new String[]{PreferenceName.GlobalTextSize, PreferenceName.GlobalTextColor, PreferenceName.GlobalTextWeight};
            textSettingsNames[TextType.Orbital] = new String[]{PreferenceName.OrbitalTextSize, PreferenceName.OrbitalTextColor, PreferenceName.OrbitalTextWeight};
            textSettingsNames[TextType.PassStart] = new String[]{PreferenceName.PassStartTextSize, PreferenceName.PassStartTextColor, PreferenceName.PassStartTextWeight};
            textSettingsNames[TextType.PassEnd] = new String[]{PreferenceName.PassEndTextSize, PreferenceName.PassEndTextColor, PreferenceName.PassEndTextWeight};
            textSettingsNames[TextType.PassElevation] = new String[]{PreferenceName.PassElevationTextSize, PreferenceName.PassElevationTextColor, PreferenceName.PassElevationTextWeight};
            textSettingsNames[TextType.PassAzimuthStart] = new String[]{PreferenceName.PassAzimuthStartTextSize, PreferenceName.PassAzimuthStartTextColor, PreferenceName.PassAzimuthStartTextWeight};
            textSettingsNames[TextType.PassAzimuthEnd] = new String[]{PreferenceName.PassAzimuthEndTextSize, PreferenceName.PassAzimuthEndTextColor, PreferenceName.PassAzimuthEndTextWeight};
            textSettingsNames[TextType.PassDuration] = new String[]{PreferenceName.PassDurationTextSize, PreferenceName.PassDurationTextColor, PreferenceName.PassDurationTextWeight};
            textSettingsNames[TextType.Location] = new String[]{PreferenceName.LocationTextSize, PreferenceName.LocationTextColor, PreferenceName.LocationTextWeight};

            location = new LocationSettings(context, widgetId, noradId);
        }
    }

    public static class Page extends Selectable.ListFragment
    {
        public interface OnAllowOkayListener
        {
            void allow(boolean allow);
        }

        public interface OnSettingChangedListener
        {
            void settingChanged();
        }

        private TableRow unusedImageRow;
        private TableRow globalImageRow;
        private TableRow orbitalImageRow;
        private TableRow settingsImageRow;
        private TableRow locationImageRow;
        private TableRow globalBackgroundRow;
        private TableRow borderRow;
        private TableRow topRow;
        private TableRow middleRow;
        private TableRow bottomRow;
        private TableRow[] textRow;
        private TextView passStartText;
        private SwitchCompat globalTextSwitch;
        private SwitchCompat globalImageSwitch;
        private SwitchCompat globalBackgroundSwitch;
        private IconSpinner orbitalList;
        private IconSpinner locationList;
        private IconSpinner intervalList;
        private IconSpinner globalBorderStyleList;
        private IconSpinner borderStyleList;
        private IconSpinner[] textSizeList;
        private RadioGroup currentLocationGroup;
        private AppCompatRadioButton followRadio;
        private AppCompatRadioButton intervalRadio;
        private AppCompatRadioButton nowRadio;
        private AutoCompleteTextView searchText;
        private CheckBox[] textBoldCheckBox;
        private CheckBox[] textItalicCheckBox;
        private BorderButton globalImageColorButton;
        private BorderButton orbitalImageColorButton;
        private BorderButton settingsImageColorButton;
        private BorderButton locationImageColorButton;
        private BorderButton globalBackgroundColorButton;
        private BorderButton borderColorButton;
        private BorderButton topColorButton;
        private BorderButton middleColorButton;
        private BorderButton bottomColorButton;
        private BorderButton[] textColorButton;
        private View globalImagesDivider;
        private View orbitalImageDivider;
        private View settingsImageDivider;
        private View borderDivider;
        private View topDivider;
        private View middleDivider;
        private View outdatedText;
        private View[] textDivider;
        private ChooseColorDialog colorDialog;
        private LocationReceiver locationReceiver;
        private OnAllowOkayListener allowOkayListener;
        private OnSettingChangedListener settingChangedListener;

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            byte index;
            int page = this.getPageParam();
            final Context context = this.getContext();
            ViewGroup rootView = null;
            final ViewGroup rootViewConst;
            final Resources res = (context != null ? context.getResources() : null);
            final Database.DatabaseLocation[] locations;
            final Database.DatabaseSatellite[] satellites;
            final Float[] fontSizes = new Float[]{6.0f, 7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f, 13.0f, 14.0f, 15.0f, 16.0f, 17.0f, 18.0f, 19.0f, 20.0f, 21.0f, 22.0f, 23.0f, 24.0f};
            final IconSpinner.Item[] borderStyles = (res != null ? new IconSpinner.Item[]{new IconSpinner.Item(res.getString(R.string.title_round), BorderType.Round), new IconSpinner.Item(res.getString(R.string.title_rectangle), BorderType.Square)} : null);
            final IconSpinner.Item[] intervalItems = (res != null ? new IconSpinner.Item[]
            {
                    new IconSpinner.Item(res.getString(R.string.text_30) + " " + res.getString(R.string.title_minutes), 1800000L),
                    new IconSpinner.Item(res.getString(R.string.text_1) + " " + res.getString(R.string.title_hour), 3600000L),
                    new IconSpinner.Item(res.getString(R.string.text_2) + " " + res.getString(R.string.title_hours), 7200000L),
                    new IconSpinner.Item(res.getString(R.string.text_6) + " " + res.getString(R.string.title_hours), 21600000L),
                    new IconSpinner.Item(res.getString(R.string.text_12) + " " + res.getString(R.string.title_hours), 43200000L),
                    new IconSpinner.Item(res.getString(R.string.text_1) + " " + res.getString(R.string.title_day), 86400000L)
            } : null);
            final ArrayList<IconSpinner.Item> locationItems = new ArrayList<>(0);

            locationReceiver = null;

            switch(page)
            {
                case TabPage.Data:
                    rootView = (ViewGroup)inflater.inflate(R.layout.widget_setup_data_view, container, false);

                    satellites = Database.getOrbitals(context);
                    locations = Database.getLocations(context, "[Type] <> " + Database.LocationType.Current);

                    outdatedText = rootView.findViewById(R.id.Widget_Setup_Outdated_Text);
                    orbitalList = rootView.findViewById(R.id.Widget_Setup_Orbital_List);
                    orbitalList.setAdapter(new IconSpinner.CustomAdapter(context, satellites));
                    orbitalList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                    {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                        {
                            widgetSettings.noradId = satellites[position].noradId;

                            //update image tab and preview displays
                            TabAdapter.updatePage(TabPage.Images);
                            onSettingChanged(context, PreferenceName.NoradID, widgetSettings.noradId);

                            //update display
                            if(outdatedText != null)
                            {
                                outdatedText.setVisibility(satellites[position].tleIsAccurate ? View.GONE : View.VISIBLE);
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    locationList = rootView.findViewById(R.id.Widget_Setup_Location_Source_List);
                    if(res != null)
                    {
                        locationItems.add(new IconSpinner.Item(Globals.getDrawable(context, R.drawable.ic_my_location_black, true), res.getString(R.string.title_location_current), Database.LocationType.Current));
                        for(Database.DatabaseLocation savedLocation : locations)
                        {
                            locationItems.add(new IconSpinner.Item(Globals.getDrawable(context, Globals.getLocationIcon(savedLocation.locationType), true), savedLocation.name, savedLocation.locationType));
                        }
                        locationItems.add(new IconSpinner.Item(Globals.getDrawable(context, R.drawable.ic_search_black, true), res.getString(R.string.title_search), Database.LocationType.New));
                        locationList.setAdapter(new IconSpinner.CustomAdapter(context, locationItems.toArray(new IconSpinner.Item[0])));
                    }
                    locationList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                    {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                        {
                            byte source = (byte)locationList.getSelectedValue(Database.LocationType.Current);
                            boolean isSearch = (source == Database.LocationType.New);
                            boolean isSaved = (source != Database.LocationType.Current && !isSearch);
                            IconSpinner.Item currentItem = locationItems.get(position);

                            //update location
                            widgetSettings.location.source = source;
                            widgetSettings.location.id = (byte)currentItem.value;
                            widgetSettings.location.locationName = (isSearch ? widgetSettings.location.searchName : currentItem.text);

                            //update saved
                            if(isSaved)
                            {
                                //set saved to selected location
                                Database.DatabaseLocation currentLocation = locations[position - 1];
                                widgetSettings.location.savedId = currentLocation.id;
                                widgetSettings.location.savedName = currentLocation.name;
                                widgetSettings.location.savedObserver.geo.latitude = currentLocation.latitude;
                                widgetSettings.location.savedObserver.geo.longitude = currentLocation.longitude;
                                widgetSettings.location.savedObserver.geo.altitudeKm = currentLocation.altitudeKM;
                            }
                            else
                            {
                                widgetSettings.location.savedId = -1;
                                widgetSettings.location.savedName = null;
                            }

                            //update displays
                            updateDisplays();
                            onSettingChanged(context, PreferenceName.LocationName, widgetSettings.location.locationName);
                            onAllowOkay(!isSearch || widgetSettings.location.searchObserver.geo.latitude != 0 || widgetSettings.location.searchObserver.geo.longitude != 0 || widgetSettings.location.searchObserver.geo.altitudeKm != 0);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    currentLocationGroup = rootView.findViewById(R.id.Widget_Setup_Current_Location_Group);

                    followRadio = rootView.findViewById(R.id.Widget_Setup_Follow_Radio);
                    followRadio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                    {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                        {
                            widgetSettings.location.useFollow = isChecked;
                        }
                    });

                    intervalRadio = rootView.findViewById(R.id.Widget_Setup_Interval_Radio);
                    intervalRadio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                    {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
                        {
                            widgetSettings.location.useInterval = isChecked;
                            updateDisplays();
                        }
                    });

                    nowRadio = rootView.findViewById(R.id.Widget_Setup_Now_Radio);

                    intervalList = rootView.findViewById(R.id.Widget_Setup_Interval_List);
                    if(intervalItems != null)
                    {
                        intervalList.setAdapter(new IconSpinner.CustomAdapter(context, intervalItems));
                    }
                    intervalList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                    {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                        {
                            if(intervalItems != null)
                            {
                                widgetSettings.location.intervalMs = (long)intervalItems[position].value;
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    searchText = rootView.findViewById(R.id.Widget_Setup_Location_Search_Text);
                    searchText.addTextChangedListener(new TextWatcher()
                    {
                        private boolean firstTime = true;

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {}

                        @Override
                        public void afterTextChanged(Editable s)
                        {
                            //if not the first time
                            if(!firstTime)
                            {
                                //reset
                                widgetSettings.location.searchObserver.geo.latitude = widgetSettings.location.searchObserver.geo.longitude = widgetSettings.location.searchObserver.geo.altitudeKm = 0;
                            }

                            //update search name and status
                            widgetSettings.location.searchName = s.toString();
                            onAllowOkay(false);
                            firstTime = false;
                        }
                    });

                    //setup receiver
                    rootViewConst = rootView;
                    locationReceiver = new LocationReceiver(LocationService.FLAG_START_NONE)
                    {
                        @Override
                        protected Activity getActivity()
                        {
                            return(Page.this.getActivity());
                        }

                        @Override
                        protected View getParentView()
                        {
                            return(rootViewConst);
                        }

                        @Override
                        protected void onConnected(final Context context, GoogleApiClient locationClient, PlacesClient placesClient)
                        {
                            //setup name autocompletion
                            LocationService.setAutoCompletePlaces(searchText, context, placesClient, new LocationService.OnGotCoordinatesListener()
                            {
                                @Override
                                public void gotCoordinates(final double latitude, final double longitude)
                                {
                                    Activity activity = Page.this.getActivity();

                                    if(activity != null)
                                    {
                                        activity.runOnUiThread(new Runnable()
                                        {
                                            @Override
                                            public void run()
                                            {
                                                //update search location
                                                widgetSettings.location.searchObserver.geo.latitude = latitude;
                                                widgetSettings.location.searchObserver.geo.longitude = longitude;
                                                widgetSettings.location.searchObserver.geo.altitudeKm = 0;

                                                //update display
                                                onAllowOkay(true);
                                                onSettingChanged(context, PreferenceName.LocationName, searchText.getText().toString());
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    };
                    locationReceiver.register(context);
                    locationReceiver.startLocationService(context);
                    break;

                case TabPage.Images:
                    rootView = (ViewGroup)inflater.inflate(R.layout.widget_setup_images_view, container, false);

                    unusedImageRow = rootView.findViewById(R.id.Widget_Setup_Unused_Image_Row);

                    globalImageRow = rootView.findViewById(R.id.Widget_Setup_Global_Images_Row);
                    globalImageSwitch = rootView.findViewById(R.id.Widget_Setup_Global_Images_Switch);
                    globalImageSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                    {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                        {
                            widgetSettings.useGlobalImage = isChecked;
                            updateDisplays();
                            onSettingChanged(context, PreferenceName.GlobalImage, isChecked);
                        }
                    });
                    globalImagesDivider = rootView.findViewById(R.id.Widget_Setup_Global_Images_Divider);

                    globalImageColorButton = rootView.findViewById(R.id.Widget_Setup_Global_Images_Color_Button);
                    globalImageColorButton.setOnClickListener(createOnColorButtonClickListener(globalImageRow));

                    orbitalImageRow = rootView.findViewById(R.id.Widget_Setup_Orbital_Image_Row);
                    orbitalImageColorButton = rootView.findViewById(R.id.Widget_Setup_Orbital_Image_Color_Button);
                    orbitalImageColorButton.setOnClickListener(createOnColorButtonClickListener(orbitalImageRow));
                    orbitalImageDivider = rootView.findViewById(R.id.Widget_Setup_Orbital_Image_Divider);

                    settingsImageRow = rootView.findViewById(R.id.Widget_Setup_Settings_Image_Row);
                    settingsImageColorButton = rootView.findViewById(R.id.Widget_Setup_Settings_Image_Color_Button);
                    settingsImageColorButton.setOnClickListener(createOnColorButtonClickListener(settingsImageRow));
                    settingsImageDivider = rootView.findViewById(R.id.Widget_Setup_Settings_Image_Divider);

                    locationImageRow = rootView.findViewById(R.id.Widget_Setup_Location_Image_Row);
                    locationImageColorButton = rootView.findViewById(R.id.Widget_Setup_Location_Image_Color_Button);
                    locationImageColorButton.setOnClickListener(createOnColorButtonClickListener(locationImageRow));
                    break;

                case TabPage.Background:
                    rootView = (ViewGroup)inflater.inflate(R.layout.widget_setup_background_view, container, false);

                    globalBackgroundRow = rootView.findViewById(R.id.Widget_Setup_Global_Background_Row);
                    globalBackgroundSwitch = rootView.findViewById(R.id.Widget_Setup_Global_Background_Switch);
                    globalBackgroundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                    {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                        {
                            widgetSettings.useGlobalBackground = isChecked;
                            updateDisplays();
                            onSettingChanged(context, PreferenceName.GlobalBackground, isChecked);
                        }
                    });
                    globalBackgroundColorButton = rootView.findViewById(R.id.Widget_Setup_Global_Background_Color_Button);
                    globalBackgroundColorButton.setOnClickListener(createOnColorButtonClickListener(globalBackgroundRow, true));
                    globalBorderStyleList = rootView.findViewById(R.id.Widget_Setup_Global_Border_Style_List);
                    if(borderStyles != null)
                    {
                        globalBorderStyleList.setAdapter(new IconSpinner.CustomAdapter(context, borderStyles));
                    }
                    globalBorderStyleList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                    {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                        {
                            if(borderStyles != null)
                            {
                                widgetSettings.borderType = (int)borderStyles[position].value;
                                onSettingChanged(context, PreferenceName.BorderType, widgetSettings.borderColor, widgetSettings.borderType);
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    borderRow = rootView.findViewById(R.id.Widget_Setup_Border_Background_Row);
                    borderColorButton = rootView.findViewById(R.id.Widget_Setup_Border_Color_Button);
                    borderColorButton.setOnClickListener(createOnColorButtonClickListener(borderRow, true));
                    borderStyleList = rootView.findViewById(R.id.Widget_Setup_Border_Style_List);
                    if(borderStyles != null)
                    {
                        borderStyleList.setAdapter(new IconSpinner.CustomAdapter(context, borderStyles));
                    }
                    borderStyleList.setOnItemSelectedListener(globalBorderStyleList.getOnItemSelectedListener());
                    borderDivider = rootView.findViewById(R.id.Widget_Setup_Border_Background_Divider);

                    topRow = rootView.findViewById(R.id.Widget_Setup_Top_Background_Row);
                    topColorButton = rootView.findViewById(R.id.Widget_Setup_Top_Background_Color_Button);
                    topColorButton.setOnClickListener(createOnColorButtonClickListener(topRow, true));
                    topDivider = rootView.findViewById(R.id.Widget_Setup_Top_Background_Divider);

                    middleRow = rootView.findViewById(R.id.Widget_Setup_Middle_Background_Row);
                    middleColorButton = rootView.findViewById(R.id.Widget_Setup_Middle_Background_Color_Button);
                    middleColorButton.setOnClickListener(createOnColorButtonClickListener(middleRow, true));
                    middleDivider = rootView.findViewById(R.id.Widget_Setup_Middle_Background_Divider);

                    bottomRow = rootView.findViewById(R.id.Widget_Setup_Bottom_Background_Row);
                    bottomColorButton = rootView.findViewById(R.id.Widget_Setup_Bottom_Background_Color_Button);
                    bottomColorButton.setOnClickListener(createOnColorButtonClickListener(bottomRow, true));
                    break;

                case TabPage.Text:
                    rootView = (ViewGroup)inflater.inflate(R.layout.widget_setup_text_view, container, false);

                    textRow = new TableRow[TextType.TextCount];
                    textSizeList = new IconSpinner[TextType.TextCount];
                    textBoldCheckBox = new CheckBox[TextType.TextCount];
                    textItalicCheckBox = new CheckBox[TextType.TextCount];
                    textColorButton = new BorderButton[TextType.TextCount];
                    textDivider = new View[TextType.TextCount];

                    textRow[TextType.Global] = rootView.findViewById(R.id.Widget_Setup_Global_Text_Row);
                    globalTextSwitch = rootView.findViewById(R.id.Widget_Setup_Global_Text_Switch);
                    globalTextSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                    {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
                        {
                            widgetSettings.useGlobalText = isChecked;
                            updateDisplays();
                            onSettingChanged(context, PreferenceName.GlobalText, isChecked);
                        }
                    });
                    textSizeList[TextType.Global] = rootView.findViewById(R.id.Widget_Setup_Global_Text_Size_List);
                    textBoldCheckBox[TextType.Global] = rootView.findViewById(R.id.Widget_Setup_Global_Text_Bold_CheckBox);
                    textItalicCheckBox[TextType.Global] = rootView.findViewById(R.id.Widget_Setup_Global_Text_Italic_CheckBox);
                    textColorButton[TextType.Global] = rootView.findViewById(R.id.Widget_Setup_Global_Text_Color_Button);

                    textRow[TextType.Orbital] = rootView.findViewById(R.id.Widget_Setup_Orbital_Text_Row);
                    textSizeList[TextType.Orbital] = rootView.findViewById(R.id.Widget_Setup_Orbital_Text_Size_List);
                    textBoldCheckBox[TextType.Orbital] = rootView.findViewById(R.id.Widget_Setup_Orbital_Text_Bold_CheckBox);
                    textItalicCheckBox[TextType.Orbital] = rootView.findViewById(R.id.Widget_Setup_Orbital_Text_Italic_CheckBox);
                    textColorButton[TextType.Orbital] = rootView.findViewById(R.id.Widget_Setup_Orbital_Text_Color_Button);
                    textDivider[TextType.Orbital] = rootView.findViewById(R.id.Widget_Setup_Orbital_Text_Divider);

                    textRow[TextType.PassStart] = rootView.findViewById(R.id.Widget_Setup_Pass_Start_Text_Row);
                    passStartText = rootView.findViewById(R.id.Widget_Setup_Pass_Start_Text);
                    textSizeList[TextType.PassStart] = rootView.findViewById(R.id.Widget_Setup_Pass_Start_Text_Size_List);
                    textBoldCheckBox[TextType.PassStart] = rootView.findViewById(R.id.Widget_Setup_Pass_Start_Text_Bold_CheckBox);
                    textItalicCheckBox[TextType.PassStart] = rootView.findViewById(R.id.Widget_Setup_Pass_Start_Text_Italic_CheckBox);
                    textColorButton[TextType.PassStart] = rootView.findViewById(R.id.Widget_Setup_Pass_Start_Text_Color_Button);
                    textDivider[TextType.PassStart] = rootView.findViewById(R.id.Widget_Setup_Pass_Start_Text_Divider);

                    textRow[TextType.PassEnd] = rootView.findViewById(R.id.Widget_Setup_Pass_End_Text_Row);
                    textSizeList[TextType.PassEnd] = rootView.findViewById(R.id.Widget_Setup_Pass_End_Text_Size_List);
                    textBoldCheckBox[TextType.PassEnd] = rootView.findViewById(R.id.Widget_Setup_Pass_End_Text_Bold_CheckBox);
                    textItalicCheckBox[TextType.PassEnd] = rootView.findViewById(R.id.Widget_Setup_Pass_End_Text_Italic_CheckBox);
                    textColorButton[TextType.PassEnd] = rootView.findViewById(R.id.Widget_Setup_Pass_End_Text_Color_Button);
                    textDivider[TextType.PassEnd] = rootView.findViewById(R.id.Widget_Setup_Pass_End_Text_Divider);

                    textRow[TextType.PassElevation] = rootView.findViewById(R.id.Widget_Setup_Pass_Elevation_Text_Row);
                    textSizeList[TextType.PassElevation] = rootView.findViewById(R.id.Widget_Setup_Pass_Elevation_Text_Size_List);
                    textBoldCheckBox[TextType.PassElevation] = rootView.findViewById(R.id.Widget_Setup_Pass_Elevation_Text_Bold_CheckBox);
                    textItalicCheckBox[TextType.PassElevation] = rootView.findViewById(R.id.Widget_Setup_Pass_Elevation_Text_Italic_CheckBox);
                    textColorButton[TextType.PassElevation] = rootView.findViewById(R.id.Widget_Setup_Pass_Elevation_Text_Color_Button);
                    textDivider[TextType.PassElevation] = rootView.findViewById(R.id.Widget_Setup_Pass_Elevation_Text_Divider);

                    textRow[TextType.PassAzimuthStart] = rootView.findViewById(R.id.Widget_Setup_Pass_Azimuth_Start_Text_Row);
                    textSizeList[TextType.PassAzimuthStart] = rootView.findViewById(R.id.Widget_Setup_Pass_Azimuth_Start_Text_Size_List);
                    textBoldCheckBox[TextType.PassAzimuthStart] = rootView.findViewById(R.id.Widget_Setup_Pass_Azimuth_Start_Text_Bold_CheckBox);
                    textItalicCheckBox[TextType.PassAzimuthStart] = rootView.findViewById(R.id.Widget_Setup_Pass_Azimuth_Start_Text_Italic_CheckBox);
                    textColorButton[TextType.PassAzimuthStart] = rootView.findViewById(R.id.Widget_Setup_Pass_Azimuth_Start_Text_Color_Button);
                    textDivider[TextType.PassAzimuthStart] = rootView.findViewById(R.id.Widget_Setup_Pass_Azimuth_Start_Text_Divider);

                    textRow[TextType.PassAzimuthEnd] = rootView.findViewById(R.id.Widget_Setup_Pass_Azimuth_End_Text_Row);
                    textSizeList[TextType.PassAzimuthEnd] = rootView.findViewById(R.id.Widget_Setup_Pass_Azimuth_End_Text_Size_List);
                    textBoldCheckBox[TextType.PassAzimuthEnd] = rootView.findViewById(R.id.Widget_Setup_Pass_Azimuth_End_Text_Bold_CheckBox);
                    textItalicCheckBox[TextType.PassAzimuthEnd] = rootView.findViewById(R.id.Widget_Setup_Pass_Azimuth_End_Text_Italic_CheckBox);
                    textColorButton[TextType.PassAzimuthEnd] = rootView.findViewById(R.id.Widget_Setup_Pass_Azimuth_End_Text_Color_Button);
                    textDivider[TextType.PassAzimuthEnd] = rootView.findViewById(R.id.Widget_Setup_Pass_Azimuth_End_Text_Divider);

                    textRow[TextType.PassDuration] = rootView.findViewById(R.id.Widget_Setup_Pass_Duration_Text_Row);
                    textSizeList[TextType.PassDuration] = rootView.findViewById(R.id.Widget_Setup_Pass_Duration_Text_Size_List);
                    textBoldCheckBox[TextType.PassDuration] = rootView.findViewById(R.id.Widget_Setup_Pass_Duration_Text_Bold_CheckBox);
                    textItalicCheckBox[TextType.PassDuration] = rootView.findViewById(R.id.Widget_Setup_Pass_Duration_Text_Italic_CheckBox);
                    textColorButton[TextType.PassDuration] = rootView.findViewById(R.id.Widget_Setup_Pass_Duration_Text_Color_Button);
                    textDivider[TextType.PassDuration] = rootView.findViewById(R.id.Widget_Setup_Pass_Duration_Text_Divider);

                    textRow[TextType.Location] = rootView.findViewById(R.id.Widget_Setup_Location_Text_Row);
                    textSizeList[TextType.Location] = rootView.findViewById(R.id.Widget_Setup_Location_Text_Size_List);
                    textBoldCheckBox[TextType.Location] = rootView.findViewById(R.id.Widget_Setup_Location_Text_Bold_CheckBox);
                    textItalicCheckBox[TextType.Location] = rootView.findViewById(R.id.Widget_Setup_Location_Text_Italic_CheckBox);
                    textColorButton[TextType.Location] = rootView.findViewById(R.id.Widget_Setup_Location_Text_Color_Button);

                    for(index = 0; index < TextType.TextCount; index++)
                    {
                        final byte textIndex = index;

                        textSizeList[index].setAdapter(new IconSpinner.CustomAdapter(context, fontSizes));
                        textSizeList[index].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                        {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                            {
                                widgetSettings.text[textIndex].size = fontSizes[position];
                                onSettingChanged(context, widgetSettings.textSettingsNames[textIndex][0], widgetSettings.text[textIndex].size);
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                        textColorButton[index].setOnClickListener(createOnColorButtonClickListener(textRow[index]));
                        textBoldCheckBox[index].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                        {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                            {
                                widgetSettings.text[textIndex].bold = isChecked;
                                onSettingChanged(context, widgetSettings.textSettingsNames[textIndex][2], widgetSettings.text[textIndex].getWeight());
                            }
                        });
                        textItalicCheckBox[index].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                        {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                            {
                                widgetSettings.text[textIndex].italic = isChecked;
                                onSettingChanged(context, widgetSettings.textSettingsNames[textIndex][2], widgetSettings.text[textIndex].getWeight());
                            }
                        });
                    }
                    break;
            }
            updateDisplays();

            return(rootView);
        }

        @Override
        public void onConfigurationChanged(@NonNull Configuration newConfig)
        {
            super.onConfigurationChanged(newConfig);

            if(colorDialog != null)
            {
                colorDialog.reload();
            }
        }

        @Override
        public void onDestroy()
        {
            super.onDestroy();

            if(locationReceiver != null)
            {
                locationReceiver.unregister(this.getContext());
                locationReceiver = null;
            }
        }

        @Override
        protected boolean setupActionModeItems(MenuItem edit, MenuItem delete, MenuItem save, MenuItem sync)
        {
            return(false);
        }

        @Override
        protected void onActionModeEdit() {}

        @Override
        protected void onActionModeDelete() {}

        @Override
        protected int onActionModeConfirmDelete()
        {
            return(0);
        }

        @Override
        protected void onActionModeSave() {}

        @Override
        protected void onActionModeSync() {}

        @Override
        protected void onUpdateStarted() {}

        @Override
        protected void onUpdateFinished(boolean success) {}

        //Updates displays
        public void updateDisplays()
        {
            byte index;
            int page = this.getPageParam();
            Class<?> widgetClass = this.getWidgetClassParam();
            boolean useNormal = (widgetClass != null && !widgetClass.equals(WidgetPassTinyProvider.class));
            boolean useExtended = (widgetClass != null && widgetClass.equals(WidgetPassMediumProvider.class));

            switch(page)
            {
                case TabPage.Data:
                    boolean isCurrent = (widgetSettings.location.source == Database.LocationType.Current);
                    boolean isSearch = (widgetSettings.location.source == Database.LocationType.New);
                    String currentText;

                    orbitalList.setSelectedValue(widgetSettings.noradId);

                    if(widgetSettings.location.id != -1)
                    {
                        locationList.setSelectedText(widgetSettings.location.locationName);
                    }
                    else
                    {
                        locationList.setSelectedValue(widgetSettings.location.source == Database.LocationType.Current ? Database.LocationType.Current : Database.LocationType.New);
                    }

                    if(widgetSettings.location.useFollow)
                    {
                        followRadio.setChecked(true);
                    }
                    else if(widgetSettings.location.useInterval)
                    {
                        intervalRadio.setChecked(true);
                    }
                    else
                    {
                        nowRadio.setChecked(true);      //default to current location now
                    }

                    intervalList.setSelectedValue(widgetSettings.location.intervalMs);
                    intervalList.setVisibility(isCurrent && widgetSettings.location.useInterval ? View.VISIBLE : View.GONE);

                    if(isSearch)
                    {
                        //remember current text
                        currentText = searchText.getText().toString();

                        //if search name is set and different than current text
                        if(widgetSettings.location.searchName != null && !widgetSettings.location.searchName.equals(currentText))
                        {
                            //update text
                            searchText.setText(widgetSettings.location.searchName);
                        }

                        //focus on and select all text
                        searchText.requestFocus();
                        searchText.selectAll();
                    }
                    searchText.setVisibility(isSearch ? View.VISIBLE : View.GONE);

                    currentLocationGroup.setVisibility(isCurrent ? View.VISIBLE : View.GONE);
                    break;

                case TabPage.Images:
                    boolean useGlobalImage = widgetSettings.useGlobalImage;
                    boolean useOrbitalImage = (widgetSettings.noradId > 0);
                    boolean showGlobalImageSwitch = (useNormal || useOrbitalImage);
                    int orbitalImageVisibility  = (useOrbitalImage && !useGlobalImage ? View.VISIBLE : View.GONE);   //show if using orbital image and not using global image
                    int nonOrbitalImageVisibility = (!useNormal || useGlobalImage ? View.GONE : View.VISIBLE);       //hide if not using normal size or using global image

                    unusedImageRow.setVisibility(!useNormal && !useOrbitalImage ? View.VISIBLE : View.GONE);         //show if not using normal and not using orbital image

                    globalImageRow.setVisibility(showGlobalImageSwitch && useGlobalImage ? View.VISIBLE : View.GONE);
                    globalImageSwitch.setChecked(useGlobalImage);
                    globalImageSwitch.setVisibility(showGlobalImageSwitch ? View. VISIBLE : View.GONE);
                    globalImageColorButton.setBackgroundColor(widgetSettings.globalImageColor);
                    globalImagesDivider.setVisibility(showGlobalImageSwitch ? View.VISIBLE : View.GONE);

                    orbitalImageRow.setVisibility(orbitalImageVisibility);
                    orbitalImageColorButton.setBackgroundColor(widgetSettings.orbitalImageColor);
                    orbitalImageDivider.setVisibility(!useNormal ? View.GONE : orbitalImageVisibility);

                    settingsImageRow.setVisibility(nonOrbitalImageVisibility);
                    settingsImageColorButton.setBackgroundColor(widgetSettings.settingsImageColor);
                    settingsImageDivider.setVisibility(nonOrbitalImageVisibility);

                    locationImageRow.setVisibility(nonOrbitalImageVisibility);
                    locationImageColorButton.setBackgroundColor(widgetSettings.locationImageColor);
                    break;

                case TabPage.Background:
                    boolean useGlobalBackground = widgetSettings.useGlobalBackground;
                    int nonGlobalBackgroundVisibility = (useGlobalBackground ? View.GONE : View.VISIBLE);

                    globalBackgroundRow.setVisibility(useGlobalBackground ? View.VISIBLE : View.GONE);
                    globalBackgroundSwitch.setChecked(useGlobalBackground);
                    globalBackgroundColorButton.setBackgroundColor(widgetSettings.globalBackgroundColor);
                    globalBorderStyleList.setSelectedValue(widgetSettings.borderType);

                    borderRow.setVisibility(nonGlobalBackgroundVisibility);
                    borderColorButton.setBackgroundColor(widgetSettings.borderColor);
                    borderStyleList.setSelectedValue(widgetSettings.borderType);
                    borderDivider.setVisibility(nonGlobalBackgroundVisibility);

                    topRow.setVisibility(nonGlobalBackgroundVisibility);
                    topColorButton.setBackgroundColor(widgetSettings.topBackgroundColor);
                    topDivider.setVisibility(nonGlobalBackgroundVisibility);

                    middleRow.setVisibility(nonGlobalBackgroundVisibility);
                    middleColorButton.setBackgroundColor(widgetSettings.middleBackgroundColor);
                    middleDivider.setVisibility(nonGlobalBackgroundVisibility);

                    bottomRow.setVisibility(nonGlobalBackgroundVisibility);
                    bottomColorButton.setBackgroundColor(widgetSettings.bottomBackgroundColor);
                    break;

                case TabPage.Text:
                    boolean useGlobalText = widgetSettings.useGlobalText;
                    int visibility;
                    int nonGlobalTextVisibility = (useGlobalText ? View.GONE : View.VISIBLE);
                    int nonGlobalExtendedTextVisibility = (useGlobalText || !useExtended ? View.GONE : View.VISIBLE);

                    globalTextSwitch.setChecked(useGlobalText);

                    passStartText.setText(this.getResources().getString(useNormal ? R.string.title_pass_start : R.string.title_pass));

                    for(index = 0; index < TextType.TextCount; index++)
                    {
                        switch(index)
                        {
                            case TextType.Global:
                                visibility = (useGlobalText ? View.VISIBLE : View.GONE);
                                break;

                            case TextType.PassAzimuthStart:
                            case TextType.PassAzimuthEnd:
                            case TextType.PassDuration:
                                visibility = nonGlobalExtendedTextVisibility;
                                break;

                            default:
                                //if using tiny and not any of the used text
                                if(!useNormal && index != TextType.Orbital && index != TextType.PassStart)
                                {
                                    visibility = View.GONE;
                                }
                                else
                                {
                                    visibility = nonGlobalTextVisibility;
                                }
                                break;
                        }

                        //if on the last index
                        if(index == TextType.TextCount - 1 && visibility == View.GONE)
                        {
                            //make invisible instead of gone in order to have layout calculated correctly (android bug with 1 row in table layout)
                            visibility = View.INVISIBLE;
                        }

                        textRow[index].setVisibility(visibility);
                        textSizeList[index].setSelectedValue(widgetSettings.text[index].size);
                        textBoldCheckBox[index].setChecked(widgetSettings.text[index].bold);
                        textItalicCheckBox[index].setChecked(widgetSettings.text[index].italic);
                        textColorButton[index].setBackgroundColor(widgetSettings.text[index].color);
                        if(textDivider[index] != null)
                        {
                            textDivider[index].setVisibility(visibility);
                        }
                    }
                    break;
            }
        }

        //Gets background color of given view
        private int getViewBackgroundColor(View view)
        {
            Drawable background = view.getBackground();

            if(background instanceof ColorDrawable)
            {
                return(((ColorDrawable)background).getColor());
            }
            else
            {
                return(Color.TRANSPARENT);
            }
        }

        //Sets allow okay listener
        public void setAllowOkayListener(OnAllowOkayListener listener)
        {
            allowOkayListener = listener;
        }

        //Calls on allow okay listener
        private void onAllowOkay(boolean allow)
        {
            //if listener is set
            if(allowOkayListener != null)
            {
                //send event
                allowOkayListener.allow(allow);
            }
        }

        //Sets setting changed listener
        public void setSettingChangedListener(OnSettingChangedListener listener)
        {
            settingChangedListener = listener;
        }

        //Calls on setting changed listener
        private void onSettingChanged(Context context, String settingName, Object value, Object value2)
        {
            int index;
            int index2;
            int previewId = Integer.MAX_VALUE;
            boolean foundText = false;
            Database.DatabaseSatellite orbital;

            //update setting
            switch(settingName)
            {
                case PreferenceName.NoradID:
                case PreferenceName.Name:
                    setNoradID(context, previewId, (int)value);
                    orbital = Database.getOrbital(context, (int)value);
                    if(orbital != null)
                    {
                        setName(context, previewId, orbital.getName());
                    }
                    break;

                case PreferenceName.LocationName:
                    setLocationName(context, previewId, (String)value);
                    break;

                case PreferenceName.GlobalImage:
                    setGlobalImage(context, previewId, (boolean)value);
                    break;

                case PreferenceName.GlobalBackgroundColor:
                    setGlobalBackgroundColor(context, previewId, (int)value);
                    break;

                case PreferenceName.BorderColor:
                case PreferenceName.BorderType:
                    setBorder(context, previewId, (int)value, (int)value2);
                    break;

                case PreferenceName.TopBackgroundColor:
                    setTopBackgroundColor(context, previewId, (int)value);
                    break;

                case PreferenceName.MiddleBackgroundColor:
                    setMiddleBackgroundColor(context, previewId, (int)value);
                    break;

                case PreferenceName.BottomBackgroundColor:
                    setBottomBackgroundColor(context, previewId, (int)value);
                    break;

                case PreferenceName.GlobalImageColor:
                    setGlobalImageColor(context, previewId, (int)value);
                    break;

                case PreferenceName.OrbitalImageColor:
                    setOrbitalImageColor(context, previewId, (int)value);
                    break;

                case PreferenceName.SettingsImageColor:
                    setSettingsImageColor(context, previewId, (int)value);
                    break;

                case PreferenceName.LocationImageColor:
                    setLocationImageColor(context, previewId, (int)value);
                    break;

                case PreferenceName.GlobalBackground:
                    setGlobalBackground(context, previewId, (boolean)value);
                    break;

                case PreferenceName.GlobalText:
                    setGlobalText(context, previewId, (boolean)value);
                    break;

                default:
                    for(index = 0; index < widgetSettings.textSettingsNames.length && !foundText; index++)
                    {
                        String[] currentSettingNames = widgetSettings.textSettingsNames[index];

                        for(index2 = 0; index2 < currentSettingNames.length && !foundText; index2++)
                        {
                            if(currentSettingNames[index2].equals(settingName))
                            {
                                setTextOptions(context, previewId, widgetSettings.text[index].size, widgetSettings.text[index].color, widgetSettings.text[index].getWeight(), currentSettingNames[0], currentSettingNames[1], currentSettingNames[2]);
                                foundText = true;
                            }
                        }
                    }
                    break;
            }

            //if listener is set
            if(settingChangedListener != null)
            {
                //send event
                settingChangedListener.settingChanged();
            }
        }
        private void onSettingChanged(Context context, String settingName, Object value)
        {
            onSettingChanged(context, settingName, value, null);
        }

        //Gets widget class param
        private Class<?> getWidgetClassParam()
        {
            Bundle params = this.getArguments();
            return(params != null ? (Class<?>)params.getSerializable(ParamTypes.WidgetClass) : null);
        }

        //Creates an on color button click listener
        private View.OnClickListener createOnColorButtonClickListener(final TableRow row, final boolean allowOpacityTransparent)
        {
            return(new View.OnClickListener()
            {
                @Override
                public void onClick(final View v)
                {
                    Resources res = Page.this.getResources();
                    final Context context = row.getContext();

                    colorDialog = new ChooseColorDialog(context, getViewBackgroundColor(v));
                    colorDialog.setAllowOpacity(allowOpacityTransparent);
                    colorDialog.setAllowTransparent(allowOpacityTransparent);
                    colorDialog.setTitle(((TextView)row.getChildAt(0)).getText() + " " + res.getString(R.string.title_color));
                    colorDialog.setOnColorSelectedListener(new ChooseColorDialog.OnColorSelectedListener()
                    {
                        @Override
                        public void onColorSelected(int color)
                        {
                            byte index;
                            String settingName = null;
                            Object value2 = null;

                            //update display
                            v.setBackgroundColor(color);

                            //update color
                            if(row.equals(globalBackgroundRow))
                            {
                                widgetSettings.globalBackgroundColor = color;
                                settingName = PreferenceName.GlobalBackgroundColor;
                            }
                            else if(row.equals(borderRow))
                            {
                                widgetSettings.borderColor = color;
                                settingName = PreferenceName.BorderColor;
                                value2 = widgetSettings.borderType;
                            }
                            else if(row.equals(topRow))
                            {
                                widgetSettings.topBackgroundColor = color;
                                settingName = PreferenceName.TopBackgroundColor;
                            }
                            else if(row.equals(middleRow))
                            {
                                widgetSettings.middleBackgroundColor = color;
                                settingName = PreferenceName.MiddleBackgroundColor;
                            }
                            else if(row.equals(bottomRow))
                            {
                                widgetSettings.bottomBackgroundColor = color;
                                settingName = PreferenceName.BottomBackgroundColor;
                            }
                            else if(row.equals(globalImageRow))
                            {
                                widgetSettings.globalImageColor = color;
                                settingName = PreferenceName.GlobalImageColor;
                            }
                            else if(row.equals(orbitalImageRow))
                            {
                                widgetSettings.orbitalImageColor = color;
                                settingName = PreferenceName.OrbitalImageColor;
                            }
                            else if(row.equals(settingsImageRow))
                            {
                                widgetSettings.settingsImageColor = color;
                                settingName = PreferenceName.SettingsImageColor;
                            }
                            else if(row.equals(locationImageRow))
                            {
                                widgetSettings.locationImageColor = color;
                                settingName = PreferenceName.LocationImageColor;
                            }
                            else
                            {
                                //go through each text row
                                for(index = 0; index < TextType.TextCount; index++)
                                {
                                    if(row.equals(textRow[index]))
                                    {
                                        widgetSettings.text[index].color = color;
                                        settingName = widgetSettings.textSettingsNames[index][1];
                                    }
                                }
                            }

                            //if settings name was set
                            if(settingName != null)
                            {
                                //update preview
                                onSettingChanged(context, settingName, color, value2);
                            }
                        }
                    });
                    colorDialog.show(context);
                }
            });
        }
        private View.OnClickListener createOnColorButtonClickListener(final TableRow row)
        {
            return(createOnColorButtonClickListener(row, false));
        }
    }

    private static class TabAdapter extends Selectable.ListFragmentAdapter
    {
        private final Class<?> widgetClass;
        private final Page.OnAllowOkayListener allowOkayListener;
        private final Page.OnSettingChangedListener settingChangedListener;
        private static final Selectable.ListFragment.OnUpdatePageListener[] updatePageListeners = new Selectable.ListFragment.OnUpdatePageListener[TabPage.TabCount];

        public TabAdapter(FragmentManager fm, View parentView, Class<?> setWidgetClass, Page.OnSettingChangedListener settingListener, Page.OnAllowOkayListener allowListener)
        {
            super(fm, parentView, null, null, null, null, null, null, null, -1, null);

            widgetClass = setWidgetClass;
            allowOkayListener = allowListener;
            settingChangedListener = settingListener;
        }

        @Override
        public @NonNull Fragment getItem(final int position)
        {
            Bundle params = new Bundle();
            final Page newPage = new Page();

            params.putSerializable(ParamTypes.WidgetClass, widgetClass);
            newPage.setArguments(params);
            newPage.setAllowOkayListener(allowOkayListener);
            newPage.setSettingChangedListener(settingChangedListener);
            updatePageListeners[position] = new Selectable.ListFragment.OnUpdatePageListener()
            {
                @Override
                public void updatePage(int page, int subPage)
                {
                    newPage.updateDisplays();
                }
            };

            return(this.getItem(-1, position, -1, newPage));
        }

        @Override
        public int getCount()
        {
            return(TabPage.TabCount);
        }

        //Updates page at position
        public static void updatePage(int position)
        {
            if(position < updatePageListeners.length && updatePageListeners[position] != null)
            {
                updatePageListeners[position].updatePage(position, -1);
            }
        }
    }

    //needs to be implemented
    public abstract Class<?> getWidgetClass();
    public abstract Class<?> getAlarmReceiverClass();

    //variables
    private View parentView;
    private View widgetPreview;
    private MaterialButton okButton;
    private static int dpWidth;
    private static WidgetSettings widgetSettings;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //set layout
        this.setContentView(R.layout.widget_setup_layout);

        final Class<?> widgetClass = getWidgetClass();
        final Class<?> alarmReceiverClass = getAlarmReceiverClass();
        final TabLayout setupTabs = this.findViewById(R.id.Widget_Setup_Tab_Layout);
        final SwipeStateViewPager setupPager = this.findViewById(R.id.Widget_Setup_Pager);
        int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        Bundle extras = this.getIntent().getExtras();
        MaterialButton cancelButton;
        ViewGroup.LayoutParams previewParams;
        float[] dps;

        //if extras are set
        if(extras != null)
        {
            //get widget ID and settings
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        //if invalid widget ID
        if(widgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
        {
            //cancel and stop
            setResult(RESULT_CANCELED);
            this.finish();
        }

        //get settings
        dpWidth = Globals.getDeviceDp(this);
        widgetSettings = new WidgetSettings(this, widgetClass, widgetId);

        //get views
        parentView = this.findViewById(R.id.Widget_Setup_Layout);
        widgetPreview = this.findViewById(R.id.Widget_Pass_View);
        okButton = this.findViewById(R.id.Widget_Setup_Ok_Button);
        cancelButton = this.findViewById(R.id.Widget_Setup_Cancel_Button);

        //setup tabs
        setupTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                //update page
                setupPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        //setup pager
        setupPager.setOffscreenPageLimit(TabPage.TabCount);
        setupPager.setAdapter(new TabAdapter(this.getSupportFragmentManager(), parentView, widgetClass, new Page.OnSettingChangedListener()
        {
            @Override
            public void settingChanged()
            {
                WidgetPassBaseProvider.updatePreview(WidgetBaseSetupActivity.this, widgetClass, widgetPreview);
            }
        }, new Page.OnAllowOkayListener()
        {
            @Override
            public void allow(boolean allow)
            {
                okButton.setEnabled(allow);
            }
        }));
        setupPager.addOnPageChangeListener(new SwipeStateViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position)
            {
                TabLayout.Tab selectedTab = setupTabs.getTabAt(position);

                //if tab found
                if(selectedTab != null)
                {
                    //update tab
                    selectedTab.select();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        //setup preview
        updatePreviewSettings(this, widgetId);
        dps = Globals.dpsToPixels(this, 70, 30);
        previewParams = widgetPreview.getLayoutParams();
        previewParams.width = (int)(dps[0] * (widgetClass.equals(WidgetPassMediumProvider.class) ? 3.5 : widgetClass.equals(WidgetPassSmallProvider.class) ? 2.5 : 1.5) - dps[1]);
        previewParams.height = (int)(dps[0] * 1.65 - dps[1]);
        widgetPreview.setLayoutParams(previewParams);

        //button events
        okButton.setOnClickListener(createOnClickListener(widgetClass, alarmReceiverClass, widgetId, true));
        cancelButton.setOnClickListener(createOnClickListener(widgetClass, alarmReceiverClass, widgetId, false));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        boolean granted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        boolean retrying = (requestCode == Globals.PermissionType.LocationRetry);

        //handle response
        switch(requestCode)
        {
            case Globals.PermissionType.Location:
            case Globals.PermissionType.LocationRetry:
                //if granted
                if(granted)
                {
                    //perform ok button click again
                    okButton.callOnClick();
                }
                //else if not retrying
                else if(!retrying)
                {
                    //ask permission again
                    Globals.askLocationPermission(this, true);
                }
                else
                {
                    //show denied
                    LocationService.showDenied(WidgetBaseSetupActivity.this, parentView);
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //Creates an on click listener
    private View.OnClickListener createOnClickListener(final Class<?> widgetClass, final Class<?> alarmReceiverClass, final int widgetId, final boolean forOk)
    {
        return(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                boolean done = true;
                boolean useFollow;
                boolean useInterval;
                boolean useGlobalImage;
                boolean useGlobalBackground;
                boolean useGlobalText;
                boolean useNormal;
                boolean useExtended;
                byte usedSource = widgetSettings.location.source;
                int result = RESULT_CANCELED;
                Context context = v.getContext();
                Intent resultIntent = new Intent();
                Calculations.ObserverType usedObserver;

                //add widget ID to results
                resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

                //if for okay button
                if(forOk)
                {
                    //get satellite
                    Database.DatabaseSatellite satellite = Database.getOrbital(context, widgetSettings.noradId);
                    if(satellite != null)
                    {
                        //if using LocationType.Current and don't have location permission
                        if(widgetSettings.location.source == Database.LocationType.Current && !Globals.haveLocationPermission(context))
                        {
                            //if can ask for permission
                            if(Globals.askLocationPermission)
                            {
                                //get permission
                                Globals.askLocationPermission(context, false);
                            }
                            else
                            {
                                //show denied
                                LocationService.showDenied(context, parentView);
                            }

                            //not done yet
                            done = false;
                        }
                        else
                        {
                            //remember if using follow, interval, globals, and/or extended
                            useFollow = widgetSettings.location.useFollow;
                            useInterval = widgetSettings.location.useInterval;
                            useGlobalImage = widgetSettings.useGlobalImage;
                            useGlobalBackground = widgetSettings.useGlobalBackground;
                            useGlobalText = widgetSettings.useGlobalText;
                            useNormal = !widgetClass.equals(WidgetPassTinyProvider.class);
                            useExtended = widgetClass.equals(WidgetPassMediumProvider.class);

                            //save values
                            if(widgetSettings.location.source == Database.LocationType.New)
                            {
                                usedSource = Database.LocationType.Online;
                                usedObserver = widgetSettings.location.searchObserver;
                            }
                            else
                            {
                                usedObserver = widgetSettings.location.savedObserver;
                            }
                            setName(context, widgetId, satellite.getName());
                            setNoradID(context, widgetId, widgetSettings.noradId);
                            setOrbitalType(context, widgetId, satellite.orbitalType);
                            setLocation(context, alarmReceiverClass, widgetId, usedSource, widgetSettings.location.savedId, (widgetSettings.location.source == Database.LocationType.New ? widgetSettings.location.searchName : widgetSettings.location.savedName), usedObserver.geo.latitude, usedObserver.geo.longitude, useFollow, useInterval, widgetSettings.location.intervalMs, (widgetSettings.location.source != Database.LocationType.Current));
                            setGlobalImage(context, widgetId, useGlobalImage);
                            if(useGlobalImage)
                            {
                                setGlobalImageColor(context, widgetId, widgetSettings.globalImageColor);
                            }
                            else
                            {
                                setOrbitalImageColor(context, widgetId, widgetSettings.orbitalImageColor);
                                if(useNormal)
                                {
                                    setSettingsImageColor(context, widgetId, widgetSettings.settingsImageColor);
                                    setLocationImageColor(context, widgetId, widgetSettings.locationImageColor);
                                }
                            }
                            setGlobalBackground(context, widgetId, useGlobalBackground);
                            if(useGlobalBackground)
                            {
                                setGlobalBackgroundColor(context, widgetId, widgetSettings.globalBackgroundColor);
                                setBorder(context, widgetId, widgetSettings.borderColor, widgetSettings.borderType);
                            }
                            else
                            {
                                setBorder(context, widgetId, widgetSettings.borderColor, widgetSettings.borderType);
                                setTopBackgroundColor(context, widgetId, widgetSettings.topBackgroundColor);
                                setMiddleBackgroundColor(context, widgetId, widgetSettings.middleBackgroundColor);
                                setBottomBackgroundColor(context, widgetId, widgetSettings.bottomBackgroundColor);
                            }
                            setGlobalText(context, widgetId, useGlobalText);
                            if(useGlobalText)
                            {
                                setGlobalTextOptions(context, widgetId, widgetSettings.text[TextType.Global].size, widgetSettings.text[TextType.Global].color, widgetSettings.text[TextType.Global].getWeight());
                            }
                            else
                            {
                                setOrbitalTextOptions(context, widgetId, widgetSettings.text[TextType.Orbital].size, widgetSettings.text[TextType.Orbital].color, widgetSettings.text[TextType.Orbital].getWeight());
                                setPassStartTextOptions(context, widgetId, widgetSettings.text[TextType.PassStart].size, widgetSettings.text[TextType.PassStart].color, widgetSettings.text[TextType.PassStart].getWeight());
                                if(useNormal)
                                {
                                    setPassEndTextOptions(context, widgetId, widgetSettings.text[TextType.PassEnd].size, widgetSettings.text[TextType.PassEnd].color, widgetSettings.text[TextType.PassEnd].getWeight());
                                    setPassElevationTextOptions(context, widgetId, widgetSettings.text[TextType.PassElevation].size, widgetSettings.text[TextType.PassElevation].color, widgetSettings.text[TextType.PassElevation].getWeight());
                                    if(useExtended)
                                    {
                                        setPassAzimuthStartTextOptions(context, widgetId, widgetSettings.text[TextType.PassAzimuthStart].size, widgetSettings.text[TextType.PassAzimuthStart].color, widgetSettings.text[TextType.PassAzimuthStart].getWeight());
                                        setPassAzimuthEndTextOptions(context, widgetId, widgetSettings.text[TextType.PassAzimuthEnd].size, widgetSettings.text[TextType.PassAzimuthEnd].color, widgetSettings.text[TextType.PassAzimuthEnd].getWeight());
                                        setPassDurationTextOptions(context, widgetId, widgetSettings.text[TextType.PassDuration].size, widgetSettings.text[TextType.PassDuration].color, widgetSettings.text[TextType.PassDuration].getWeight());
                                    }
                                    setLocationTextOptions(context, widgetId, widgetSettings.text[TextType.Location].size, widgetSettings.text[TextType.Location].color, widgetSettings.text[TextType.Location].getWeight());
                                }
                            }

                            //cancel any previous alarm and update widget
                            WidgetPassBaseProvider.updatePassAlarm(context, alarmReceiverClass, widgetId, null, null, false);
                            WidgetPassBaseProvider.updateWidget(context, widgetClass, alarmReceiverClass, widgetId, AppWidgetManager.getInstance(context), WidgetPassBaseProvider.getViews(context, widgetClass, widgetId), true);

                            //set result
                            result = RESULT_OK;
                        }
                    }
                }

                //if done
                if(done)
                {
                    //set result and finish
                    setResult(result, resultIntent);
                    WidgetBaseSetupActivity.this.finish();
                }
            }
        });
    }

    //Gets settings preferences
    private static SharedPreferences getPreferences(Context context)
    {
        return(context.getSharedPreferences("WidgetSettings", Context.MODE_PRIVATE));
    }

    //Gets write settings
    private static SharedPreferences.Editor getWriteSettings(Context context)
    {
        return(getPreferences(context).edit());
    }

    //Gets widget ID string
    private static String getIdString(int widgetId)
    {
        return(widgetId == Integer.MAX_VALUE ? "Custom" : String.valueOf(widgetId));
    }

    //Gets an array list of all settings for the given widget ID
    private static ArrayList<Map.Entry<String, ?>> getSettings(Context context, int widgetId)
    {
        int idIndex;
        char lastChar;
        String widgetIdString = getIdString(widgetId);
        ArrayList<Map.Entry<String, ?>> settingsList = new ArrayList<>();
        Map<String, ?> allSettings = getPreferences(context).getAll();

        //go through all keys
        for(Map.Entry<String, ?> currentKey : allSettings.entrySet())
        {
            //get name and possible ID index
            String currentName = currentKey.getKey();
            idIndex = currentName.indexOf(widgetIdString);

            //if ID index found
            if(idIndex >= 1)
            {
                //if character before ID is a letter
                lastChar = currentName.toLowerCase().charAt(idIndex - 1);
                if(lastChar >= 'a' && lastChar <= 'z')
                {
                    //add to list
                    settingsList.add(currentKey);
                }
            }
        }

        //return list
        return(settingsList);
    }

    //Deletes settings for the given widget ID
    public static void removeSettings(Context context, int widgetId)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);
        ArrayList<Map.Entry<String, ?>> settingsList = getSettings(context, widgetId);

        //go through each setting
        for(Map.Entry<String, ?> currentSetting : settingsList)
        {
            //remove the current setting
            writeSettings.remove(currentSetting.getKey());
        }

        //apply changes
        writeSettings.apply();
    }

    //Updates preview settings to match given widget ID
    public static void updatePreviewSettings(Context context, int widgetId)
    {
        int idIndex;
        String widgetIdString = getIdString(widgetId);
        String customIdString = getIdString(Integer.MAX_VALUE);
        SharedPreferences.Editor writeSettings = getWriteSettings(context);
        ArrayList<Map.Entry<String, ?>> settingsList;

        //remove all old preview settings
        removeSettings(context, Integer.MAX_VALUE);

        //get all settings
        settingsList = getSettings(context, widgetId);

        //go through each setting
        for(Map.Entry<String, ?> currentSetting : settingsList)
        {
            //get name and ID index
            String currentName = currentSetting.getKey();
            idIndex = currentName.indexOf(widgetIdString);

            //if ID index found
            if(idIndex >= 1)
            {
                //get preview name and existing value
                String previewName = currentName.substring(0, idIndex) + customIdString;
                Object currentValue = currentSetting.getValue();
                Class<?> currentValueType = currentValue.getClass();

                //write value for correct type
                if(currentValueType.equals(Boolean.class))
                {
                    writeSettings.putBoolean(previewName, (boolean)currentValue);
                }
                else if(currentValueType.equals(Integer.class))
                {
                    writeSettings.putInt(previewName, (int)currentValue);
                }
                else if(currentValueType.equals(Long.class))
                {
                    writeSettings.putLong(previewName, (long)currentValue);
                }
                else if(currentValueType.equals(Float.class))
                {
                    writeSettings.putFloat(previewName, (float)currentValue);
                }
                else
                {
                    writeSettings.putString(previewName, (String)currentValue);
                }
                writeSettings.apply();
            }
        }
    }

    //Gets name for the given widget ID
    public static String getName(Context context, int widgetId)
    {
        return(getPreferences(context).getString(PreferenceName.Name + getIdString(widgetId), Globals.getUnknownString(context)));
    }

    //Sets name for the given widget ID
    public static void setName(Context context, int widgetId, String name)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        writeSettings.putString(PreferenceName.Name + getIdString(widgetId), name);
        writeSettings.apply();
    }

    //Gets norad ID for the given widget ID
    public static int getNoradID(Context context, int widgetId)
    {
        return(getPreferences(context).getInt(PreferenceName.NoradID + getIdString(widgetId), Universe.IDs.Invalid));
    }

    //Sets norad ID for the given widget ID
    public static void setNoradID(Context context, int widgetId, int noradId)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        writeSettings.putInt(PreferenceName.NoradID + getIdString(widgetId), noradId);
        writeSettings.apply();
    }

    //Gets orbital type for the given widget ID
    public static byte getOrbitalType(Context context, int widgetId)
    {
        return((byte)getPreferences(context).getInt(PreferenceName.OrbitalType + getIdString(widgetId), Database.OrbitalType.Satellite));
    }

    //Sets orbital type for the given widget ID
    public static void setOrbitalType(Context context, int widgetId, byte orbitalType)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        writeSettings.putInt(PreferenceName.OrbitalType + getIdString(widgetId), orbitalType);
        writeSettings.apply();
    }

    //Gets if weight is bold
    public static boolean isBold(int weight)
    {
        return(weight == Typeface.BOLD || weight == Typeface.BOLD_ITALIC);
    }

    //Gets if weight is italic
    public static boolean isItalic(int weight)
    {
        return(weight == Typeface.ITALIC || weight == Typeface.BOLD_ITALIC);
    }

    //Gets default text size
    public static float getDefaultTextSize(Class<?> widgetClass, float dpi)
    {
        float size;
        boolean useNormal = !widgetClass.equals(WidgetPassTinyProvider.class);

        if(useNormal)
        {
            if(dpi >= 900)
            {
                size = 21;
            }
            else if(dpi >= 860)
            {
                size = 20;
            }
            else if(dpi >= 820)
            {
                size = 19;
            }
            else if(dpi >= 780)
            {
                size = 18;
            }
            else if(dpi >= 740)
            {
                size = 17;
            }
            else if(dpi >= 700)
            {
                size = 16;
            }
            else if(dpi >= 620)
            {
                size = 15;
            }
            else if(dpi >= 560)
            {
                size = 14;
            }
            else if(dpi >=  520)
            {
                size = 13;
            }
            else if(dpi >= 480)
            {
                size = 12;
            }
            else if(dpi >= 440)
            {
                size = 11;
            }
            else if(dpi >= 410)
            {
                size = 10;
            }
            else if(dpi >= 380)
            {
                size = 9;
            }
            else
            {
                size = 8;
            }
        }
        else
        {
            if(dpi >= 920)
            {
                size = 17;
            }
            else if(dpi >= 860)
            {
                size = 16;
            }
            else if(dpi >= 800)
            {
                size = 15;
            }
            else if(dpi >= 740)
            {
                size = 14;
            }
            else if(dpi >= 660)
            {
                size = 13;
            }
            else if(dpi >= 560)
            {
                size = 12;
            }
            else if(dpi >= 530)
            {
                size = 11;
            }
            else if(dpi >= 480)
            {
                size = 10;
            }
            else if(dpi >= 440)
            {
                size = 9;
            }
            else if(dpi >= 360)
            {
                size = 8;
            }
            else
            {
                size = 7;
            }
        }

        return(size);
    }

    //Gets text options for given widget ID and preference name
    private static float getSize(Context context, Class<?> widgetClass, int widgetId, String preferenceName)
    {
        return(getPreferences(context).getFloat(preferenceName + getIdString(widgetId), getDefaultTextSize(widgetClass, dpWidth)));
    }
    private static int getColor(Context context, int widgetId, String preferenceName)
    {
        boolean isBackground = false;

        switch(preferenceName)
        {
            case PreferenceName.BorderColor:
            case PreferenceName.GlobalBackgroundColor:
            case PreferenceName.TopBackgroundColor:
            case PreferenceName.MiddleBackgroundColor:
            case PreferenceName.BottomBackgroundColor:
                isBackground = true;
                break;
        }

        return(getPreferences(context).getInt(preferenceName + getIdString(widgetId), (isBackground ? Color.BLACK : Color.WHITE)));
    }
    private static int getTextWeight(Context context, int widgetId, String preferenceName)
    {
        String weight = getPreferences(context).getString(preferenceName + getIdString(widgetId), FontWeight.Normal);

        if(weight == null)
        {
            weight = "";
        }

        switch(weight)
        {
            case FontWeight.BoldItalic:
                return(Typeface.BOLD_ITALIC);

            case FontWeight.Bold:
                return(Typeface.BOLD);

            case FontWeight.Italic:
                return(Typeface.ITALIC);

            default:
            case FontWeight.Normal:
                return(Typeface.NORMAL);
        }
    }

    //Sets options for the given widget ID and preference name
    private static void setOptions(Context context, int widgetId, float size, int color, int weight, int borderType, String sizePreferenceName, String colorPreferenceName, String weightPreferenceName, String borderTypePreference)
    {
        String idString = getIdString(widgetId);
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        if(sizePreferenceName != null)
        {
            writeSettings.putFloat(sizePreferenceName + idString, size);
        }
        writeSettings.putInt(colorPreferenceName + idString, color);
        if(weightPreferenceName != null)
        {
            writeSettings.putString(weightPreferenceName + idString, (weight == Typeface.BOLD_ITALIC ? FontWeight.BoldItalic : weight == Typeface.BOLD ? FontWeight.Bold : weight == Typeface.ITALIC ? FontWeight.Italic : FontWeight.Normal));
        }
        if(borderTypePreference != null)
        {
            writeSettings.putInt(borderTypePreference + idString, borderType);
        }
        writeSettings.apply();
    }
    private static void setBorderOptions(Context context, int widgetId, int color, int borderType)
    {
        setOptions(context, widgetId, 0, color, 0, borderType, null, PreferenceName.BorderColor, null, PreferenceName.BorderType);
    }
    private static void setTextOptions(Context context, int widgetId, float size, int color, int weight, String sizePreferenceName, String colorPreferenceName, String weightPreferenceName)
    {
        setOptions(context, widgetId, size, color, weight, 0, sizePreferenceName, colorPreferenceName, weightPreferenceName, null);
    }
    private static void setBackgroundOptions(Context context, int widgetId, int color, String colorPreferenceName)
    {
        setOptions(context, widgetId, 0, color, 0, 0, null, colorPreferenceName, null, null);
    }

    //Sets global text usage for the given ID
    public static void setGlobalText(Context context, int widgetId, boolean global)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        writeSettings.putBoolean(PreferenceName.GlobalText + getIdString(widgetId), global);
        writeSettings.apply();
    }

    //Gets global text usage for the given ID
    public static boolean getGlobalText(Context context, int widgetId)
    {
        return(getPreferences(context).getBoolean(PreferenceName.GlobalText + getIdString(widgetId), true));
    }

    //Gets global text options for the given widget ID
    public static float getGlobalTextSize(Context context, Class<?> widgetClass, int widgetId)
    {
        return(getSize(context, widgetClass, widgetId, PreferenceName.GlobalTextSize));
    }
    public static int getGlobalTextColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.GlobalTextColor));
    }
    public static int getGlobalTextWeight(Context context, int widgetId)
    {
        return(getTextWeight(context, widgetId, PreferenceName.GlobalTextWeight));
    }

    //Sets global text options for the given widget ID
    public static void setGlobalTextOptions(Context context, int widgetId, float size, int color, int weight)
    {
        setTextOptions(context, widgetId, size, color, weight, PreferenceName.GlobalTextSize, PreferenceName.GlobalTextColor, PreferenceName.GlobalTextWeight);
    }

    //Gets orbital text options for the given widget ID
    public static float getOrbitalTextSize(Context context, Class<?> widgetClass, int widgetId)
    {
        return(getSize(context, widgetClass, widgetId, PreferenceName.OrbitalTextSize));
    }
    public static int getOrbitalTextColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.OrbitalTextColor));
    }
    public static int getOrbitalTextWeight(Context context, int widgetId)
    {
        return(getTextWeight(context, widgetId, PreferenceName.OrbitalTextWeight));
    }

    //Sets orbital text options for the given widget ID
    public static void setOrbitalTextOptions(Context context, int widgetId, float size, int color, int weight)
    {
        setTextOptions(context, widgetId, size, color, weight, PreferenceName.OrbitalTextSize, PreferenceName.OrbitalTextColor, PreferenceName.OrbitalTextWeight);
    }

    //Gets pass start text options for the given widget ID
    public static float getPassStartTextSize(Context context, Class<?> widgetClass, int widgetId)
    {
        return(getSize(context, widgetClass, widgetId, PreferenceName.PassStartTextSize));
    }
    public static int getPassStartTextColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.PassStartTextColor));
    }
    public static int getPassStartTextWeight(Context context, int widgetId)
    {
        return(getTextWeight(context, widgetId, PreferenceName.PassStartTextWeight));
    }

    //Sets pass start text options for the given widget ID
    public static void setPassStartTextOptions(Context context, int widgetId, float size, int color, int weight)
    {
        setTextOptions(context, widgetId, size, color, weight, PreferenceName.PassStartTextSize, PreferenceName.PassStartTextColor, PreferenceName.PassStartTextWeight);
    }

    //Gets pass end text options for the given widget ID
    public static float getPassEndTextSize(Context context, Class<?> widgetClass, int widgetId)
    {
        return(getSize(context, widgetClass, widgetId, PreferenceName.PassEndTextSize));
    }
    public static int getPassEndTextColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.PassEndTextColor));
    }
    public static int getPassEndTextWeight(Context context, int widgetId)
    {
        return(getTextWeight(context, widgetId, PreferenceName.PassEndTextWeight));
    }

    //Sets pass end text options for the given widget ID
    public static void setPassEndTextOptions(Context context, int widgetId, float size, int color, int weight)
    {
        setTextOptions(context, widgetId, size, color, weight, PreferenceName.PassEndTextSize, PreferenceName.PassEndTextColor, PreferenceName.PassEndTextWeight);
    }

    //Gets pass elevation text options for the given widget ID
    public static float getPassElevationTextSize(Context context, Class<?> widgetClass, int widgetId)
    {
        return(getSize(context, widgetClass, widgetId, PreferenceName.PassElevationTextSize));
    }
    public static int getPassElevationTextColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.PassElevationTextColor));
    }
    public static int getPassElevationTextWeight(Context context, int widgetId)
    {
        return(getTextWeight(context, widgetId, PreferenceName.PassElevationTextWeight));
    }

    //Sets pass elevation text options for the given widget ID
    public static void setPassElevationTextOptions(Context context, int widgetId, float size, int color, int weight)
    {
        setTextOptions(context, widgetId, size, color, weight, PreferenceName.PassElevationTextSize, PreferenceName.PassElevationTextColor, PreferenceName.PassElevationTextWeight);
    }

    //Gets pass azimuth start text options for the given widget ID
    public static float getPassAzimuthStartTextSize(Context context, Class<?> widgetClass, int widgetId)
    {
        return(getSize(context, widgetClass, widgetId, PreferenceName.PassAzimuthStartTextSize));
    }
    public static int getPassAzimuthStartTextColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.PassAzimuthStartTextColor));
    }
    public static int getPassAzimuthStartTextWeight(Context context, int widgetId)
    {
        return(getTextWeight(context, widgetId, PreferenceName.PassAzimuthStartTextWeight));
    }

    //Sets pass azimuth start text options for the given widget ID
    public static void setPassAzimuthStartTextOptions(Context context, int widgetId, float size, int color, int weight)
    {
        setTextOptions(context, widgetId, size, color, weight, PreferenceName.PassAzimuthStartTextSize, PreferenceName.PassAzimuthStartTextColor, PreferenceName.PassAzimuthStartTextWeight);
    }

    //Gets pass azimuth end text options for the given widget ID
    public static float getPassAzimuthEndTextSize(Context context, Class<?> widgetClass, int widgetId)
    {
        return(getSize(context, widgetClass, widgetId, PreferenceName.PassAzimuthEndTextSize));
    }
    public static int getPassAzimuthEndTextColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.PassAzimuthEndTextColor));
    }
    public static int getPassAzimuthEndTextWeight(Context context, int widgetId)
    {
        return(getTextWeight(context, widgetId, PreferenceName.PassAzimuthEndTextWeight));
    }

    //Sets pass azimuth end text options for the given widget ID
    public static void setPassAzimuthEndTextOptions(Context context, int widgetId, float size, int color, int weight)
    {
        setTextOptions(context, widgetId, size, color, weight, PreferenceName.PassAzimuthEndTextSize, PreferenceName.PassAzimuthEndTextColor, PreferenceName.PassAzimuthEndTextWeight);
    }

    //Gets pass duration text options for the given widget ID
    public static float getPassDurationTextSize(Context context, Class<?> widgetClass, int widgetId)
    {
        return(getSize(context, widgetClass, widgetId, PreferenceName.PassDurationTextSize));
    }
    public static int getPassDurationTextColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.PassDurationTextColor));
    }
    public static int getPassDurationTextWeight(Context context, int widgetId)
    {
        return(getTextWeight(context, widgetId, PreferenceName.PassDurationTextWeight));
    }

    //Sets pass duration text options for the given widget ID
    public static void setPassDurationTextOptions(Context context, int widgetId, float size, int color, int weight)
    {
        setTextOptions(context, widgetId, size, color, weight, PreferenceName.PassDurationTextSize, PreferenceName.PassDurationTextColor, PreferenceName.PassDurationTextWeight);
    }

    //Gets location text options for the given widget ID
    public static float getLocationTextSize(Context context, Class<?> widgetClass, int widgetId)
    {
        return(getSize(context, widgetClass, widgetId, PreferenceName.LocationTextSize));
    }
    public static int getLocationTextColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.LocationTextColor));
    }
    public static int getLocationTextWeight(Context context, int widgetId)
    {
        return(getTextWeight(context, widgetId, PreferenceName.LocationTextWeight));
    }

    //Sets location text options for the given widget ID
    public static void setLocationTextOptions(Context context, int widgetId, float size, int color, int weight)
    {
        setTextOptions(context, widgetId, size, color, weight, PreferenceName.LocationTextSize, PreferenceName.LocationTextColor, PreferenceName.LocationTextWeight);
    }

    //Sets global background usage for the given ID
    public static void setGlobalBackground(Context context, int widgetId, boolean global)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        writeSettings.putBoolean(PreferenceName.GlobalBackground + getIdString(widgetId), global);
        writeSettings.apply();
    }

    //Gets global background usage for the given ID
    public static boolean getGlobalBackground(Context context, int widgetId)
    {
        return(getPreferences(context).getBoolean(PreferenceName.GlobalBackground + getIdString(widgetId), true));
    }

    //Sets global background color for the given widget ID
    public static void setGlobalBackgroundColor(Context context, int widgetId, int color)
    {
        setBackgroundOptions(context, widgetId, color, PreferenceName.GlobalBackgroundColor);
    }

    //Gets global background color for the given widget ID
    public static int getGlobalBackgroundColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.GlobalBackgroundColor));
    }

    //Sets the border for the given ID
    public static void setBorder(Context context, int widgetId, int color, int borderType)
    {
        setBorderOptions(context, widgetId, color, borderType);
    }

    //Gets border options for the given ID
    public static int getBorderColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.BorderColor));
    }
    public static int getBorderType(Context context, int widgetId)
    {
        return(getPreferences(context).getInt(PreferenceName.BorderType + getIdString(widgetId), BorderType.Round));
    }

    //Sets top background color for the given widget ID
    public static void setTopBackgroundColor(Context context, int widgetId, int color)
    {
        setBackgroundOptions(context, widgetId, color, PreferenceName.TopBackgroundColor);
    }

    //Gets top background color for the given widget ID
    public static int getTopBackgroundColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.TopBackgroundColor));
    }

    //Sets middle background color for the given widget ID
    public static void setMiddleBackgroundColor(Context context, int widgetId, int color)
    {
        setBackgroundOptions(context, widgetId, color, PreferenceName.MiddleBackgroundColor);
    }

    //Gets middle background color for the given widget ID
    public static int getMiddleBackgroundColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.MiddleBackgroundColor));
    }

    //Sets bottom background color for the given widget ID
    public static void setBottomBackgroundColor(Context context, int widgetId, int color)
    {
        setBackgroundOptions(context, widgetId, color, PreferenceName.BottomBackgroundColor);
    }

    //Gets bottom background color for the given widget ID
    public static int getBottomBackgroundColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.BottomBackgroundColor));
    }

    //Sets global image usage for the given ID
    public static void setGlobalImage(Context context, int widgetId, boolean global)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        writeSettings.putBoolean(PreferenceName.GlobalImage + getIdString(widgetId), global);
        writeSettings.apply();
    }

    //Gets global image usage for the given ID
    public static boolean getGlobalImage(Context context, int widgetId)
    {
        return(getPreferences(context).getBoolean(PreferenceName.GlobalImage + getIdString(widgetId), true));
    }

    //Sets global image color for the given widget ID
    public static void setGlobalImageColor(Context context, int widgetId, int color)
    {
        setBackgroundOptions(context, widgetId, color, PreferenceName.GlobalImageColor);
    }

    //Gets global background color for the given widget ID
    public static int getGlobalImageColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.GlobalImageColor));
    }

    //Sets orbital image color for the given widget ID
    public static void setOrbitalImageColor(Context context, int widgetId, int color)
    {
        setBackgroundOptions(context, widgetId, color, PreferenceName.OrbitalImageColor);
    }

    //Gets orbital image color for the given widget ID
    public static int getOrbitalImageColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.OrbitalImageColor));
    }

    //Sets settings image color for the given widget ID
    public static void setSettingsImageColor(Context context, int widgetId, int color)
    {
        setBackgroundOptions(context, widgetId, color, PreferenceName.SettingsImageColor);
    }

    //Gets settings image color for the given widget ID
    public static int getSettingsImageColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.SettingsImageColor));
    }

    //Sets location image color for the given widget ID
    public static void setLocationImageColor(Context context, int widgetId, int color)
    {
        setBackgroundOptions(context, widgetId, color, PreferenceName.LocationImageColor);
    }

    //Gets location image color for the given widget ID
    public static int getLocationImageColor(Context context, int widgetId)
    {
        return(getColor(context, widgetId, PreferenceName.LocationImageColor));
    }

    //Sets location
    public static void setLocation(Context context, Class<?> alarmReceiverClass, int widgetId, byte locationSource, int locationId, String locationName, double latitude, double longitude, boolean useFollow, boolean useInterval, long interval, boolean saveLatLonName)
    {
        boolean updateInterval;
        boolean isCurrent = (locationSource == Database.LocationType.Current);
        String idString = getIdString(widgetId);
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        //write widget settings
        writeSettings.putInt(PreferenceName.LocationSource + idString, locationSource);
        writeSettings.putInt(PreferenceName.LocationID + idString, locationId);
        writeSettings.putBoolean(PreferenceName.LocationFollow + idString, (isCurrent && useFollow));
        writeSettings.putBoolean(PreferenceName.LocationInterval + idString, (isCurrent && useInterval));
        if(saveLatLonName)
        {
            writeSettings.putString(PreferenceName.LocationName + idString, locationName);
            writeSettings.putFloat(PreferenceName.LocationLatitude + idString, (float)latitude);
            writeSettings.putFloat(PreferenceName.LocationLongitude + idString, (float)longitude);
        }

        //if using current, interval, and not saving lat, lon, name yet
        updateInterval = (isCurrent && useInterval && !saveLatLonName) ;
        if(updateInterval)
        {
            //write interval setting
            writeSettings.putLong(PreferenceName.LocationInterval, interval);
        }

        //write all settings
        writeSettings.apply();

        //if need to update interval alarm
        if(updateInterval)
        {
            //update alarm
            WidgetPassBaseProvider.updateLocationIntervalAlarm(context, alarmReceiverClass, interval, true);
        }
    }

    //Gets location source
    public static byte getLocationSource(Context context, int widgetId)
    {
        return((byte)getPreferences(context).getInt(PreferenceName.LocationSource + getIdString(widgetId), Database.LocationType.Current));
    }

    //Sets location source
    public static void setLocationSource(Context context, int widgetId, byte source)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        writeSettings.putInt(PreferenceName.LocationSource + getIdString(widgetId), source);
        writeSettings.apply();
    }

    //Gets location ID
    public static int getLocationId(Context context, int widgetId)
    {
        return(getPreferences(context).getInt(PreferenceName.LocationID + getIdString(widgetId), -1));
    }

    //Gets if following location
    public static boolean getLocationFollow(Context context, int widgetId)
    {
        return(getPreferences(context).getBoolean(PreferenceName.LocationFollow + getIdString(widgetId), false));
    }

    //Gets location interval
    public static boolean getLocationInterval(Context context, int widgetId)
    {
        return(getPreferences(context).getBoolean(PreferenceName.LocationInterval + getIdString(widgetId), false));
    }

    //Gets global location interval
    public static long getLocationGlobalInterval(Context context)
    {
        return(getPreferences(context).getLong(PreferenceName.LocationInterval, 7200000L));       //default to 2 hours
    }

    //Gets location
    public static Calculations.ObserverType getLocation(Context context, int widgetId)
    {
        SharedPreferences prefs = getPreferences(context);
        Calculations.ObserverType location = new Calculations.ObserverType();

        location.geo.latitude = prefs.getFloat(PreferenceName.LocationLatitude + widgetId, 0);
        location.geo.longitude = prefs.getFloat(PreferenceName.LocationLongitude + widgetId, 0);

        return(location);
    }

    //Gets location name
    public static String getLocationName(Context context, int widgetId)
    {
        int locationId = getLocationId(context, widgetId);
        String unknown = Globals.getUnknownString(context);

        if(locationId != -1)
        {
            Database.DatabaseLocation[] locations = Database.getLocations(context, "[ID]=" + locationId);
            return(locations.length > 0 ? locations[0].name : unknown);
        }
        else
        {
            return(getPreferences(context).getString(PreferenceName.LocationName + getIdString(widgetId), unknown));
        }
    }

    //Sets location name
    public static void setLocationName(Context context, int widgetId, String name)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);

        writeSettings.putString(PreferenceName.LocationName + getIdString(widgetId), name);
        writeSettings.apply();
    }
}
