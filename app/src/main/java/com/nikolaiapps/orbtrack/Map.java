/*package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class Map
{
    public static class OrbitalObject extends CoordinatesFragment.OrbitalBase
    {
        private Shared common;
        private Polyline[] pathPoints;
        private Marker marker;

        public OrbitalObject(Context context, GoogleMap map, Database.SatelliteData newSat, Calculations.ObserverType observerLocation)
        {
            int index;
            int iconId;
            int satelliteNum = newSat.getSatelliteNum();

            common = new Shared();
            common.data = newSat;
            common.geo = new Calculations.GeodeticDataType(0, 0, 0, 0, 0);
            pathPoints = new Polyline[4];

            marker = map.addMarker(new MarkerOptions().title(newSat.getName()).position(new LatLng(0, 0)).anchor(0.5f, 0.5f).visible(false));
            marker.setTag(newSat.getSatelliteNum());

            iconId = Globals.getOrbitalIconID(satelliteNum, common.data.getOrbitalType());
            if(iconId > -1)
            {
                if(satelliteNum == Universe.IDs.Moon)
                {
                    setImage(Universe.Moon.getPhaseImage(context, observerLocation, System.currentTimeMillis()));
                }
                else
                {
                    setImage(Globals.getBitmap(context, iconId, 0));
                }
            }

            for(index = 0; index < pathPoints.length; index++)
            {
                pathPoints[index] = map.addPolyline(new PolylineOptions().geodesic(true).width(3).add(new LatLng(0, 0)).visible(false));
            }
        }

        @Override
        Calculations.GeodeticDataType getGeo()
        {
            return(common.geo);
        }

        @Override
        Database.SatelliteData getData()
        {
            return(common.data);
        }

        @Override
        void setImage(Bitmap image)
        {
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(image));
        }

        @Override
        void setText(String text)
        {
            marker.setSnippet(text);

            //if info window shown
            if(getInfoVisible())
            {
                //refresh
                setInfoVisible(true);
            }
        }

        @Override
        void setTitleAlwaysVisible(boolean visible)
        {
            //N/A
        }

        @Override
        void setInfoVisible(boolean visible)
        {
            if(visible)
            {
                marker.showInfoWindow();
            }
            else
            {
                marker.hideInfoWindow();
            }
        }

        @Override
        boolean getInfoVisible()
        {
            return(marker.isInfoWindowShown());
        }

        @Override
        void setVisible(boolean visible)
        {
            marker.setVisible(visible);
        }

        @Override
        void setPath(ArrayList<CoordinatesFragment.Coordinate> newPoints)
        {
            int index;
            int endIndex;
            int splitLength;
            int mapPathColor = (common.data.database != null ? common.data.database.pathColor : Color.DKGRAY);
            int locationIndex;
            int locationsLength = newPoints.size();
            ArrayList<LatLng> newPath = new ArrayList<>(0);

            //if not enough points
            if(locationsLength < 2)
            {
                //stop
                return;
            }

            //go through each point
            for(CoordinatesFragment.Coordinate currentPoint : newPoints)
            {
                //add new coordinate
                newPath.add(new LatLng(currentPoint.latitude, currentPoint.longitude));
            }

            //split up points between locations
            locationIndex = 0;
            splitLength = newPath.size() / pathPoints.length;
            for(index = 0; index < locationsLength && locationIndex < pathPoints.length; index += splitLength)
            {
                //remember current lines
                Polyline currentLines = pathPoints[locationIndex];

                //get section end
                //note: points overlap on end to be continuous for next line
                endIndex = (index + splitLength) - 1;
                if(endIndex >= locationsLength)
                {
                    endIndex = locationsLength - 2;        //note: -2 since adding 1 later
                    if(index > endIndex)
                    {
                        index = endIndex;
                    }
                }
                else if(endIndex + 1 < locationsLength)
                {
                    endIndex += 1;
                }
                if(index >= endIndex)
                {
                    index = endIndex - 1;
                }

                //set points and visibility
                currentLines.setColor(mapPathColor);
                currentLines.setPoints(newPath.subList(index, endIndex + 1));
                currentLines.setVisible(false);
                locationIndex++;
            }
        }

        @Override
        void setPathVisible(boolean visible)
        {
            for(Polyline currentPoint : pathPoints)
            {
                currentPoint.setVisible(visible);
            }
        }

        @Override
        void moveLocation(double latitude, double longitude, double altitudeKm)
        {
            common.geo = new Calculations.GeodeticDataType(latitude, longitude, altitudeKm, 0, 0);
            marker.setPosition(new LatLng(latitude, longitude));
        }

        @Override
        void remove()
        {
            marker.remove();
        }
    }

    public static class MarkerObject extends CoordinatesFragment.MarkerBase
    {
        private Shared common;
        public Marker marker;

        public MarkerObject(GoogleMap map, int noradId, Calculations.ObserverType markerLocation)
        {
            common = new Shared();
            common.noradId = noradId;

            marker = map.addMarker(new MarkerOptions().position(new LatLng(markerLocation.geo.latitude, markerLocation.geo.longitude)).anchor(0.5f, 0.5f).visible(false));
            marker.setTag(noradId);
        }

        @Override
        int getNoradId()
        {
            return(common.noradId);
        }

        @Override
        void setImage(Bitmap image)
        {
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(image));
        }

        @Override
        void setTitle(String title)
        {
            marker.setTitle(title);
        }

        @Override
        void setText(String text)
        {
            marker.setSnippet(text);
        }

        @Override
        void setTitleAlwaysVisible(boolean visible)
        {
            //N/A
        }

        @Override
        void setInfoVisible(boolean visible)
        {
            if(visible)
            {
                marker.showInfoWindow();
            }
            else
            {
                marker.hideInfoWindow();
            }
        }

        @Override
        void setInfoLocation()
        {
            //do nothing
        }

        @Override
        boolean getInfoVisible()
        {
            return(marker.isInfoWindowShown());
        }

        @Override
        void setVisible(boolean visible)
        {
            marker.setVisible(visible);
        }

        @Override
        void moveLocation(double latitude, double longitude, double altitudeKm)
        {
            marker.setPosition(new LatLng(latitude, longitude));
        }

        @Override
        void remove()
        {
            marker.remove();
        }
    }

    public static class LatLonTileProvider implements TileProvider
    {
        private CoordinatesFragment.TileShared common;

        public LatLonTileProvider(Context context)
        {
            common = new CoordinatesFragment.TileShared(context);
        }

        @Override
        public Tile getTile(int x, int y, int zoom)
        {
            Bitmap image = common.getLatitudeLongitudeTile(x, y, zoom, 512, 2, false);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] imageData;

            //create image data
            image.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            imageData = outStream.toByteArray();
            try
            {
                outStream.close();
            }
            catch(IOException ex)
            {
                //do nothing
            }
            image.recycle();

            //return tile
            return(new Tile(512, 512, imageData));
        }
    }

    public static class MapFragment extends SupportMapFragment implements CoordinatesFragment
    {
        private Shared common;
        private GoogleMap coordinateMap;
        private TileOverlay latLonOverlay;
        private TileOverlayOptions latLonOverlayOptions;

        public MapFragment()
        {
            super();

            common = new Shared();
            coordinateMap = null;
            latLonOverlay = null;
            latLonOverlayOptions = null;

            getMapAsync(new OnMapReadyCallback()
            {
                private int lastNoradId = Universe.IDs.Invalid;

                @Override
                public void onMapReady(GoogleMap googleMap)
                {
                    final Context context = MapFragment.this.getContext();

                    //if unable to get view
                    if(MapFragment.this.getView() == null)
                    {
                        //stop
                        return;
                    }

                    coordinateMap = googleMap;
                    coordinateMap.setMapType(Settings.getGoogleMapType(context));
                    coordinateMap.getUiSettings().setMapToolbarEnabled(false);
                    coordinateMap.getUiSettings().setRotateGesturesEnabled(false);
                    coordinateMap.getUiSettings().setZoomControlsEnabled(false);
                    coordinateMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter()
                    {
                        @Override
                        public View getInfoWindow(Marker marker)
                        {
                            return(null);
                        }

                        @Override
                        public View getInfoContents(Marker marker)
                        {
                            int noradId = (int)marker.getTag();
                            View view = LayoutInflater.from(context).inflate(R.layout.current_map_snippet, null);
                            TextView titleText = view.findViewById(R.id.Snippet_Title_Text);
                            TextView text = view.findViewById(R.id.Snippet_Text);
                            String title = marker.getTitle();
                            String info = marker.getSnippet();

                            //if title is set and has a length
                            if(title != null && title.trim().length() > 0)
                            {
                                //set title
                                titleText.setText(title);
                                titleText.setVisibility(View.VISIBLE);
                            }

                            //if info is set and has a length
                            if(info != null && info.trim().length() > 0)
                            {
                                //set info
                                text.setText(marker.getSnippet());
                                text.setVisibility(View.VISIBLE);
                            }

                            //if changed
                            if(noradId != lastNoradId)
                            {
                                //update status
                                lastNoradId = noradId;

                                //send event
                                common.itemSelectionChanged(noradId, true);
                            }

                            //return view
                            return(view);
                        }
                    });
                    coordinateMap.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener()
                    {
                        @Override
                        public void onInfoWindowClose(Marker marker)
                        {
                            //deselect current
                            deselectCurrent();
                            lastNoradId = Universe.IDs.None;
                        }
                    });
                    coordinateMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener()
                    {
                        @Override
                        public void onCameraMoveStarted(int reason)
                        {
                            //update status
                            common.moved(reason == REASON_GESTURE);
                        }
                    });
                    coordinateMap.setOnMapClickListener(new GoogleMap.OnMapClickListener()
                    {
                        @Override
                        public void onMapClick(LatLng latLng)
                        {
                            //send event
                            common.locationClick(latLng.latitude, latLng.longitude);
                        }
                    });

                    //update status
                    common.ready();
                }
            });
        }

        @Override
        public void onDestroy()
        {
            super.onDestroy();
            //common.destroyed();
        }

        @Override
        public void setOnReadyListener(OnReadyListener listener)
        {
            common.setOnReadyListener(listener);
        }

        @Override
        public void setOnMovedListener(OnMovedListener listener)
        {
            common.setOnMovedListener(listener);
        }

        @Override
        public void setOnLocationClickListener(OnLocationClickListener listener)
        {
            common.setOnLocationClickListener(listener);
        }

        @Override
        public void setOnItemSelectionChangedListener(OnItemSelectionChangedListener listener)
        {
            common.setOnItemSelectionChangedListener(listener);
        }

        @Override
        public boolean isMap()
        {
            return(true);
        }

        @Override
        public View getView()
        {
            return(super.getView());
        }

        @Override
        public void setArguments(Bundle args)
        {
            super.setArguments(args);
        }

        @Override
        public double getCameraZoom()
        {
            return(coordinateMap.getCameraPosition().zoom);
        }

        @Override
        public void moveCamera(double latitude, double longitude)
        {
            moveCamera(latitude, longitude, getCameraZoom());
        }

        @Override
        public void moveCamera(double latitude, double longitude, double zoom)
        {
            coordinateMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(latitude, longitude)).zoom((float)zoom).build()));
        }

        @Override
        public void setPath(int orbitalIndex, ArrayList<Coordinate> points)
        {
            common.setPath(orbitalIndex, points);
        }

        @Override
        public void setPathVisible(int orbitalIndex, boolean visible)
        {
            common.setPathVisible(orbitalIndex, visible);
        }

        @Override
        public void setShowTitlesAlways(boolean show)
        {
            common.setShowTitlesAlways(show);
        }

        @Override
        public void setStarsEnabled(boolean enabled)
        {
            //N/A
        }

        @Override
        public void setLatitudeLongitudeGridEnabled(boolean enabled)
        {
            if(enabled)
            {
                if(latLonOverlayOptions == null)
                {
                    latLonOverlayOptions = new TileOverlayOptions().tileProvider(new LatLonTileProvider(getContext()));
                }
                latLonOverlay = coordinateMap.addTileOverlay(latLonOverlayOptions);
            }
            else if(latLonOverlay != null)
            {
                latLonOverlay.remove();
            }
        }

        @Override
        public void zoom(boolean in)
        {
            coordinateMap.moveCamera(in ? CameraUpdateFactory.zoomIn() : CameraUpdateFactory.zoomOut());
        }

        @Override
        public MarkerBase addMarker(Context context, int noradId, Calculations.ObserverType markerLocation)
        {
            MarkerObject newMarker = (coordinateMap != null ? new MarkerObject(coordinateMap, noradId, markerLocation) : null);

            if(newMarker != null)
            {
                common.markerObjects.add(newMarker);
            }

            return(newMarker);
        }

        @Override
        public void removeMarker(MarkerBase object)
        {
            object.remove();
            common.markerObjects.remove(object);
        }

        @Override
        public int getOrbitalCount()
        {
            return(common.getOrbitalCount());
        }

        @Override
        public int getOrbitalNoradId(int orbitalIndex)
        {
            return(common.getOrbitalNoradId(orbitalIndex));
        }

        @Override
        public OrbitalBase getOrbital(int orbitalIndex)
        {
            return(common.getOrbital(orbitalIndex));
        }

        @Override
        public OrbitalBase addOrbital(Context context, Database.SatelliteData newSat, Calculations.ObserverType observerLocation)
        {
            OrbitalObject newObject = (coordinateMap != null ? new OrbitalObject(context, coordinateMap, newSat, observerLocation) : null);

            if(newObject != null)
            {
                common.orbitalObjects.add(newObject);
            }

            return(newObject);
        }

        @Override
        public void removeOrbital(OrbitalBase object)
        {
            object.remove();
            common.orbitalObjects.remove(object);
        }

        @Override
        public int getSelectedNoradId()
        {
            return(common.selectedNoradId);
        }

        @Override
        public void selectOrbital(int noradId)
        {
            common.selectCurrent(noradId);
        }

        @Override
        public void deselectCurrent()
        {
            common.deselectCurrent();
        }

        @Override
        public void removeOrbitals()
        {
            common.removeOrbitals();
        }
    }
}*/