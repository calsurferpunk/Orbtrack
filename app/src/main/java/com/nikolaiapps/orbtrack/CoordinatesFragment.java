package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Locale;


public interface CoordinatesFragment
{
    class MapDisplayType
    {
        final static int Globe = 0;
        final static int Map = 1;
    }

    class MapLayerType
    {
        final static int None = 0;
        final static int Normal = 1;
        final static int Satellite = 2;
        //final static int Terrain = 3;
        final static int Hybrid = 4;
        final static int Moon = 5;
        final static int Mars = 6;
        final static int Venus = 7;
        final static int Mercury = 8;
        final static int Jupiter = 9;
        final static int Saturn = 10;
        final static int Uranus = 11;
        final static int Neptune = 12;
        final static int Sun = 13;
        final static int Pluto = 14;
    }

    class MapMarkerInfoLocation
    {
        final static int None = 0;
        final static int UnderTitle = 1;
        final static int ScreenBottom = 2;
    }

    class Coordinate
    {
        final double latitude;
        final double longitude;
        final double altitudeKm;

        public Coordinate(double lat, double lon, double altKm)
        {
            latitude = lat;
            longitude = lon;
            altitudeKm = altKm;
        }
    }

    abstract class MarkerBase
    {
        static class Shared
        {
            int noradId;
            boolean tleIsAccurate;

            Shared()
            {
                noradId = Integer.MIN_VALUE;
                tleIsAccurate = true;
            }
        }

        abstract int getNoradId();
        abstract void setImage(Bitmap image);
        abstract void setRotation(double rotation);
        abstract void setTitle(String title);
        abstract void setText(String text);
        abstract void setScale(float markerScaling);
        abstract void setShowBackground(boolean show);
        abstract void setTitleAlwaysVisible(boolean visible);
        abstract void setUsingInfo(boolean using);
        abstract void setInfoVisible(boolean visible);
        abstract void setInfoLocation();
        abstract boolean getInfoVisible();
        abstract void setVisible(boolean visible);
        abstract void moveLocation(double latitude, double longitude, double altitudeKm);
        abstract void remove();
    }

    abstract class OrbitalBase
    {
        static class Shared
        {
            double bearing;
            Database.SatelliteData data;
            Calculations.GeodeticDataType geo;
            Calculations.GeodeticDataType lastGeo;

            Shared()
            {
                bearing = 0;
                data = null;
                geo = null;
                lastGeo = null;
            }
        }

        abstract Calculations.GeodeticDataType getGeo();
        abstract Database.SatelliteData getData();
        abstract void setImage(Bitmap image);
        abstract void setRotation(double rotationRads);
        abstract void setText(String text);
        abstract void setScale(float markerScaling);
        abstract void setShowBackground(boolean show);
        abstract void setShowShadow(boolean show);
        abstract void setTitleAlwaysVisible(boolean visible);
        abstract void setUsingInfo(boolean using);
        abstract void setInfoVisible(boolean visible);
        abstract boolean getInfoVisible();
        abstract void setVisible(boolean visible);
        abstract void setPath(ArrayList<Coordinate> points);
        abstract void setPathVisible(boolean visible);
        abstract void moveLocation(double latitude, double longitude, double altitudeKm);
        abstract void remove();
    }

    interface OnReadyListener
    {
        void ready();
    }

    interface OnMovedListener
    {
        void moved(boolean userMotion);
    }

    interface OnRotatedListener
    {
        void rotated(double degrees);
    }

    interface OnLocationClickListener
    {
        void click(double latitude, double longitude);
    }

    interface OnItemSelectionChangedListener
    {
        void itemSelected(int noradId);
    }

    class Shared
    {
        private int infoLocation;
        private boolean showShadow;
        private boolean showSunlight;
        private boolean showBackground;
        private boolean showTitlesAlways;
        private boolean showOrbitalDirection;
        private long lastSelectEventDateMs;
        private float markerScale;
        private OnReadyListener readyListener;
        private OnMovedListener movedListener;
        private OnRotatedListener rotatedListener;
        private OnLocationClickListener locationClickListener;
        private OnItemSelectionChangedListener itemSelectionChangedListener;

        boolean started;
        int selectedOrbitalIndex;
        int selectedNoradId;

        final ArrayList<MarkerBase> markerObjects;
        final ArrayList<OrbitalBase> orbitalObjects;

        Shared()
        {
            infoLocation = MapMarkerInfoLocation.ScreenBottom;
            showBackground = false;
            showShadow = showSunlight = showTitlesAlways = showOrbitalDirection = true;
            lastSelectEventDateMs = 0;
            markerScale = 0.75f;
            readyListener = null;
            movedListener = null;
            rotatedListener = null;
            locationClickListener = null;
            itemSelectionChangedListener = null;
            selectedOrbitalIndex = -1;
            selectedNoradId = Universe.IDs.None;
            started = false;
            markerObjects = new ArrayList<>(0);
            orbitalObjects = new ArrayList<>(0);
        }

        void setOnReadyListener(OnReadyListener listener)
        {
            readyListener = listener;

            //if late setting
            if(readyListener != null && started)
            {
                //call now
                readyListener.ready();
            }
        }

        void ready()
        {
            //update status
            started = true;

            //if listener is set
            if(readyListener != null)
            {
                //call it
                readyListener.ready();
            }
        }

        void setOnMovedListener(OnMovedListener listener)
        {
            movedListener = listener;
        }

        void setOnRotatedListener(OnRotatedListener listener)
        {
            rotatedListener = listener;
        }

        void moved(boolean userMotion)
        {
            //if listener is set
            if(movedListener != null)
            {
                //call it
                movedListener.moved(userMotion);
            }
        }

        void rotated(double degrees)
        {
            //if listener is set
            if(rotatedListener != null)
            {
                //call it
                rotatedListener.rotated(degrees);
            }
        }

        void setOnLocationClickListener(OnLocationClickListener listener)
        {
            locationClickListener = listener;
        }

        void locationClick(double latitude, double longitude)
        {
            //if listener is set
            if(locationClickListener != null)
            {
                //call it
                locationClickListener.click(latitude, longitude);
            }
        }

        void setOnItemSelectionChangedListener(OnItemSelectionChangedListener listener)
        {
            itemSelectionChangedListener = listener;
        }

        boolean itemSelectionChanged(int noradId, boolean selected)
        {
            long nowMs = System.currentTimeMillis();

            //if deselecting and not enough time has passed (currently 150 ms)
            if(!selected && (nowMs - lastSelectEventDateMs) < 150)
            {
                //stop
                return(false);
            }

            //if selecting
            if(selected)
            {
                //update last event time
                lastSelectEventDateMs = nowMs;
            }

            //if listener is set
            if(itemSelectionChangedListener != null)
            {
                //call it
                itemSelectionChangedListener.itemSelected(noradId);
            }

            //allowed
            return(true);
        }

        void setPath(int orbitalIndex, ArrayList<Coordinate> points)
        {
            if(orbitalIndex >= 0 && orbitalIndex < orbitalObjects.size())
            {
                orbitalObjects.get(orbitalIndex).setPath(points);
            }
        }

        void setPathVisible(int orbitalIndex, boolean visible)
        {
            if(orbitalIndex >= 0 && orbitalIndex < orbitalObjects.size())
            {
                orbitalObjects.get(orbitalIndex).setPathVisible(visible);
            }
        }

        void setInfoLocation(int location)
        {
            boolean using;

            infoLocation = location;
            using = (infoLocation == MapMarkerInfoLocation.UnderTitle);

            //go through each marker
            for(MarkerBase currentMarker : markerObjects)
            {
                //update using
                currentMarker.setUsingInfo(using);
            }

            //go through each orbital
            for(OrbitalBase currentOrbital : orbitalObjects)
            {
                //update using
                currentOrbital.setUsingInfo(using);
            }
        }

        int getInfoLocation()
        {
            return(infoLocation);
        }

        void setShowBackground(boolean show)
        {
            showBackground = show;

            //go through each marker
            for(MarkerBase currentMarker : markerObjects)
            {
                //update visibility
                currentMarker.setShowBackground(showBackground);
            }

            //go through each orbital
            for(OrbitalBase currentOrbital : orbitalObjects)
            {
                //update visibility
                currentOrbital.setShowBackground(showBackground);
            }
        }

        boolean getShowBackground()
        {
            return(showBackground);
        }

        void setShowShadow(boolean show)
        {
            showShadow = show;

            //go through each orbital
            for(OrbitalBase currentOrbital : orbitalObjects)
            {
                //update visibility
                currentOrbital.setShowShadow(showShadow);
            }
        }

        boolean getShowShadow()
        {
            return(showShadow);
        }

        void setShowSunlight(boolean show)
        {
            showSunlight = show;
        }

        boolean getShowSunlight()
        {
            return(showSunlight);
        }

        void setShowOrbitalDirection(boolean show)
        {
            showOrbitalDirection = show;
        }

        boolean getShowOrbitalDirection()
        {
            return(showOrbitalDirection);
        }

        void setMarkerScale(float markerScaling)
        {
            markerScale = markerScaling;

            //go through each marker
            for(MarkerBase currentMarker : markerObjects)
            {
                //update scaling
                currentMarker.setScale(markerScale);
            }

            //go through each orbital
            for(OrbitalBase currentOrbital : orbitalObjects)
            {
                //update scaling
                currentOrbital.setScale(markerScale);
            }
        }

        float getMarkerScale()
        {
            return(markerScale);
        }

        void setShowTitlesAlways(boolean show)
        {
            showTitlesAlways = show;

            //go through each marker
            for(MarkerBase currentMarker : markerObjects)
            {
                //update visibility
                currentMarker.setTitleAlwaysVisible(show);
            }

            //go through each orbital
            for(OrbitalBase currentOrbital : orbitalObjects)
            {
                //update visibility
                currentOrbital.setTitleAlwaysVisible(show);
            }
        }

        boolean getShowTitleAlways()
        {
            return(showTitlesAlways);
        }

        public MarkerBase getMarker(int noradId)
        {
            //go through each marker
            for(MarkerBase currentMarker : markerObjects)
            {
                //if ID matches
                if(currentMarker.getNoradId() == noradId)
                {
                    //return it
                    return(currentMarker);
                }
            }

            //not found
            return(null);
        }

        public int getOrbitalCount()
        {
            return(orbitalObjects.size());
        }

        public int getOrbitalIndex(int noradId)
        {
            int index;

            //go through each orbital
            for(index = 0; index < orbitalObjects.size(); index++)
            {
                //get data
                Database.SatelliteData currentData = orbitalObjects.get(index).getData();

                //if norad ID matches
                if(currentData.getSatelliteNum() == noradId)
                {
                    //return index
                    return(index);
                }
            }

            //not found
            return(-1);
        }

        public int getOrbitalNoradId(int orbitalIndex)
        {
            return(orbitalIndex >= 0 && orbitalIndex < orbitalObjects.size() ? orbitalObjects.get(orbitalIndex).getData().getSatelliteNum() : Universe.IDs.Invalid);
        }

        public OrbitalBase getOrbital(int orbitalIndex)
        {
            return(orbitalIndex >= 0 && orbitalIndex < orbitalObjects.size() ? orbitalObjects.get(orbitalIndex) : null);
        }

        private void selectOrbital(int noradId)
        {
            int index;
            boolean selected = false;

            //if a valid ID
            if(noradId != Universe.IDs.Invalid && noradId != Universe.IDs.None)
            {
                //if able to get index
                index = getOrbitalIndex(noradId);
                if(index >= 0)
                {
                    //select orbital index
                    selectedOrbitalIndex = index;
                    selected = true;
                }
                //else if able to get marker
                else if(getMarker(noradId) != null)
                {
                    //select marker
                    selectedOrbitalIndex = -1;
                    selected = true;
                }
            }

            //if selected
            if(selected)
            {
                //update ID
                selectedNoradId = noradId;

                //send event
                itemSelectionChanged(noradId, true);
            }
            //else if not found and allowed to send event
            else if(itemSelectionChanged(Universe.IDs.None, false))
            {
                //reset
                selectedOrbitalIndex = -1;
                selectedNoradId = Universe.IDs.None;
            }
        }

        private void setSelectedInfoVisible(int noradId, boolean visible)
        {
            OrbitalBase selectOrbital;
            MarkerBase selectMarker;

            //if able to get orbital
            selectOrbital = getOrbital(selectedOrbitalIndex);
            if(selectOrbital != null)
            {
                //show/hide info window
                selectOrbital.setInfoVisible(visible);
            }
            //else if none selected and there are markers
            else if(selectedOrbitalIndex == -1 && markerObjects.size() > 0)
            {
                //if able to get marker
                selectMarker = getMarker(noradId);
                if(selectMarker != null)
                {
                    //show/hide info window
                    selectMarker.setInfoVisible(visible);
                }
            }
        }

        public void selectCurrent(int noradId)
        {
            //if changing
            if(noradId != selectedNoradId)
            {
                //update selected
                selectOrbital(noradId);

                //show info window
                setSelectedInfoVisible(noradId, true);
            }
        }

        public void deselectCurrent()
        {
            //if changing
            if(selectedNoradId != Universe.IDs.None)
            {
                //hide info window
                setSelectedInfoVisible(selectedNoradId, false);
            }

            //update selected
            selectOrbital(Universe.IDs.None);
        }

        public void removeOrbitals()
        {
            for(OrbitalBase currentObject : orbitalObjects)
            {
                currentObject.remove();
            }
        }
    }

    class TileShared
    {
        final float textSpScale;
        final Resources res;
        final Context currentContext;

        TileShared(Context context)
        {
            currentContext = context;
            res = currentContext.getResources();
            textSpScale = res.getDisplayMetrics().scaledDensity;
        }

        Bitmap getLatitudeLongitudeTile(int x, int y, int zoom, int sideLength, int thickness, boolean forGlobe)
        {
            int index;
            int tileColor = Settings.getMapGridColor(currentContext);
            int textHalfWidth;
            boolean quad = !forGlobe;
            double zoomN = Math.pow(2, zoom);
            double minLon = x / zoomN * 360 - 180;
            double maxLon = (x + 1) / zoomN * 360 - 180;
            double minLat = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * y / zoomN)))) * (forGlobe ? -1 : 1);
            double maxLat = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * (y + 1) / zoomN)))) * (forGlobe ? -1 : 1);
            double lonDelta = Math.abs(maxLon - minLon);
            double latDelta = Math.abs(maxLat - minLat);
            double midLon = minLon + (lonDelta / 2);
            double midLat = maxLat + (latDelta / 2);
            Bitmap image = Bitmap.createBitmap(sideLength, sideLength, Bitmap.Config.ARGB_8888);
            Canvas imageCanvas = new Canvas(image);
            Paint imagePaint = new Paint();
            String textFormat = (latDelta < 0.01 ? (latDelta < 0.001 ? "%.4f " : "%.3f ") : "%.2f ");
            String minLonString = String.format(Locale.US, textFormat, Math.abs(minLon)) + Globals.getLongitudeDirection(res, minLon);
            String midLonString = String.format(Locale.US, textFormat, Math.abs(midLon)) + Globals.getLongitudeDirection(res, midLon);
            String midLatString = String.format(Locale.US, textFormat, Math.abs(midLat)) + Globals.getLatitudeDirection(res, midLat);
            String maxLatString = String.format(Locale.US, textFormat, Math.abs(maxLat)) + Globals.getLatitudeDirection(res, maxLat);

            //set default paint
            imagePaint.setAntiAlias(true);
            imagePaint.setStyle(Paint.Style.FILL);
            imagePaint.setColor(tileColor);
            image.eraseColor(Color.TRANSPARENT);

            //draw lines
            imagePaint.setStrokeWidth(thickness);
            if(quad)
            {
                imageCanvas.drawLines(new float[]{0, 0, 0, sideLength, sideLength, 0, sideLength, sideLength, 0, 0, sideLength, 0, sideLength, 0, sideLength, sideLength, (sideLength / 2f), 0, (sideLength / 2f), sideLength, 0, (sideLength / 2f), sideLength, (sideLength / 2f)}, imagePaint);
            }
            else
            {
                imageCanvas.drawLines(new float[]{0, 0, 0, sideLength, sideLength, 0, sideLength, sideLength, 0, 0, sideLength, 0, sideLength, 0, sideLength, sideLength}, imagePaint);
            }

            //draw text
            for(index = 0; index <= 1; index++)
            {
                imagePaint.setStyle(index == 0 ? Paint.Style.STROKE : Paint.Style.FILL);
                imagePaint.setColor(index == 0 ? Color.WHITE : tileColor);
                imagePaint.setStrokeWidth((index == 0 ? 4 : 1) * (quad ? 1 : 2));
                imagePaint.setTextSize((quad ? 8 : 28) * textSpScale);

                //longitude text
                if(quad)
                {
                    imageCanvas.drawText(minLonString, 4, (sideLength / 4f), imagePaint);
                    imageCanvas.drawText(minLonString, 4, (sideLength * 0.75f), imagePaint);
                    imageCanvas.drawText(midLonString, (sideLength / 2f) + 4, (sideLength / 4f), imagePaint);
                    imageCanvas.drawText(midLonString, (sideLength / 2f) + 4, (sideLength * 0.75f), imagePaint);
                }
                else
                {
                    imageCanvas.drawText(minLonString, 4, (sideLength / 2f), imagePaint);
                }

                //latitude text
                if(quad)
                {
                    textHalfWidth = Globals.getTextWidth(imagePaint, midLatString) / 2;
                    imageCanvas.drawText(midLatString, (sideLength / 4f) - textHalfWidth, (sideLength / 2f) - 4, imagePaint);
                    imageCanvas.drawText(midLatString, (sideLength * 0.75f) - textHalfWidth, (sideLength / 2f) - 4, imagePaint);
                }

                textHalfWidth = Globals.getTextWidth(imagePaint, maxLatString) / 2;
                if(quad)
                {
                    imageCanvas.drawText(maxLatString, (sideLength / 4f) - textHalfWidth, sideLength - 4, imagePaint);
                    imageCanvas.drawText(maxLatString, (sideLength * 0.75f) - textHalfWidth, sideLength - 4, imagePaint);
                }
                else
                {
                    imageCanvas.drawText(maxLatString, (sideLength / 2f) - textHalfWidth, sideLength - 4, imagePaint);
                }
            }

            return(image);
        }
    }

    abstract class Utils
    {
        static void setupZoomButton(Context context, final CoordinatesFragment mapView, FloatingActionButton zoomButton, final boolean zoomIn)
        {
            int marginDp = (int)Globals.dpToPixels(context, -(Build.VERSION.SDK_INT >= 21 ? 20 : 10));
            LinearLayout.LayoutParams zoomParams = (LinearLayout.LayoutParams)zoomButton.getLayoutParams();

            zoomParams.setMargins(0, 0, 0, (zoomIn ? marginDp : 0));
            zoomButton.setLayoutParams(zoomParams);
            zoomButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mapView.zoom(zoomIn);
                }
            });
        }

        static double getZoom(double altitudeKm)
        {
            double z = altitudeKm * 100;
            double zoom = (z / Whirly.ZoomToZValue) * 3;

            if(altitudeKm >= MaxDrawDistanceKm)
            {
                return(DefaultFarZoom);
            }
            else if(zoom < DefaultNearZoom)
            {
                return(DefaultNearZoom);
            }
            else
            {
                return(zoom);
            }
        }
    }

    double WhirlyEarthRadiusKm = 6371.0;
    double WhirlyZScale = 0.1;
    double DefaultNearZoom = 1.25;
    double DefaultFarZoom = 4.25;
    double MaxDrawDistanceKm = (WhirlyEarthRadiusKm * 16);
    double MinDrawDistanceZ = 500000;
    double MaxDrawDistanceZ = (MaxDrawDistanceKm * 200.0);

    void setOnReadyListener(OnReadyListener listener);
    void setOnMovedListener(OnMovedListener listener);
    void setOnRotatedListener(OnRotatedListener listener);
    void setOnLocationClickListener(OnLocationClickListener listener);
    void setOnItemSelectionChangedListener(OnItemSelectionChangedListener listener);

    boolean isMap();

    View getView();

    void setArguments(Bundle args);

    double getCameraZoom();
    void moveCamera(double latitude, double longitude);
    void moveCamera(double latitude, double longitude, double zoom);

    void setPath(int orbitalIndex, ArrayList<Coordinate> points);
    void setPathVisible(int orbitalIndex, boolean visible);
    void setInfoLocation(int location);
    void setMarkerScale(float markerScaling);
    void setMarkerShowBackground(boolean show);
    void setMarkerShowShadow(boolean show);
    void setShowSunlight(boolean show);
    void setShowOrbitalDirection(boolean show);
    void setShowTitlesAlways(boolean show);
    void setStarsEnabled(boolean enabled);
    void setSensitivityScale(float sensitivityScaling);
    void setSpeedScale(float speedScaling);
    void setRotateAllowed(boolean enabled);
    void setLatitudeLongitudeGridEnabled(boolean enabled);
    void setHeading(double degs);
    double getHeading();
    void zoom(boolean in);

    MarkerBase addMarker(Context context, int noradId, Calculations.ObserverType markerLocation);
    void removeMarker(MarkerBase object);

    int getOrbitalCount();
    int getOrbitalNoradId(int orbitalIndex);
    OrbitalBase getOrbital(int orbitalIndex);
    OrbitalBase addOrbital(Context context, Database.SatelliteData newSat, Calculations.ObserverType observerLocation);
    void removeOrbital(OrbitalBase object);

    int getSelectedNoradId();
    void selectOrbital(int noradId);
    void deselectCurrent();

    void removeOrbitals();
}
