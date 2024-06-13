package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.mousebird.maply.Atmosphere;
import com.mousebird.maply.Billboard;
import com.mousebird.maply.BillboardInfo;
import com.mousebird.maply.ComponentObject;
import com.mousebird.maply.GlobeController;
import com.mousebird.maply.GlobeMapFragment;
import com.mousebird.maply.GlobeView;
import com.mousebird.maply.Light;
import com.mousebird.maply.MapController;
import com.mousebird.maply.BaseController;
import com.mousebird.maply.MaplyStarModel;
import com.mousebird.maply.MaplyTexture;
import com.mousebird.maply.QuadImageLoader;
import com.mousebird.maply.RenderControllerInterface;
import com.mousebird.maply.SamplingParams;
import com.mousebird.maply.Shader;
import com.mousebird.maply.MarkerInfo;
import com.mousebird.maply.Point2d;
import com.mousebird.maply.Point3d;
import com.mousebird.maply.RenderGPUType;
import com.mousebird.maply.RemoteTileInfoNew;
import com.mousebird.maply.ScreenMarker;
import com.mousebird.maply.ScreenObject;
import com.mousebird.maply.SelectedObject;
import com.mousebird.maply.Shape;
import com.mousebird.maply.ShapeCylinder;
import com.mousebird.maply.ShapeInfo;
import com.mousebird.maply.ShapeLinear;
import com.mousebird.maply.SimpleTileFetcher;
import com.mousebird.maply.SphericalMercatorCoordSystem;
import com.mousebird.maply.Sticker;
import com.mousebird.maply.StickerInfo;
import com.mousebird.maply.TileID;
import com.mousebird.maply.TileInfoNew;
import com.mousebird.maply.VectorInfo;
import com.mousebird.maply.VectorObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


class Whirly
{
    private static final float StarScale = (1.0f / 6.0f);
    private static final float DefaultImageScale = 0.15f;
    private static final float DefaultTextScale = 0.2f;
    public static final double ZoomToZValue = 6356965.15661353;

    public static abstract class ParamTypes
    {
        static final String MapLayerType = "mapLayerType";
    }

    private static abstract class DrawPriority
    {
        private static final int BoardEye           = 50000000;
        private static final int SelectedBoardFlat  = 35000000;
        private static final int BoardFlat          = 25000000;
        private static final int Path               = 400000;
        private static final int Layer              = 10000;
        private static final int LayerLatLon        = 12500;
        private static final int LayerHybridLines   = 15000;
        private static final int LayerHybridLabels  = 20000;
    }

    private static class InfoImageCreator
    {
        private boolean useText;
        private boolean useBackground;
        private boolean lastUseBackground;
        private View snippetLayout;
        private TextView snippetText;
        private TextView snippetTitle;

        InfoImageCreator(boolean showText, boolean showBackground)
        {
            useText = showText;
            useBackground = showBackground;
            lastUseBackground = !useBackground;
            snippetLayout = null;
            snippetText = null;
            snippetTitle = null;
        }

        @SuppressLint("InflateParams")
        public Bitmap get(Context context, String title, String text)
        {
            int width;
            int height;
            boolean backgroundChanged = (useBackground != lastUseBackground);
            Bitmap snippetImage;
            Canvas snippetCanvas;

            //if using of background changed and using background
            if(backgroundChanged && useBackground)
            {
                //reset layout
                snippetLayout = null;
                snippetTitle =  null;
                snippetText = null;
            }

            //if layout has not been set
            if(snippetLayout == null)
            {
                //get layout
                snippetLayout = LayoutInflater.from(context).inflate(R.layout.map_info_snippet, null).findViewById(R.id.Map_Snippet_Layout);
                snippetLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
            }

            //if title has not been set yet
            if(snippetTitle == null)
            {
                //get title
                snippetTitle = snippetLayout.findViewById(R.id.Map_Snippet_Title_Text);
                snippetTitle.setVisibility(View.VISIBLE);
            }

            //update title
            snippetTitle.setText(title);

            //if text has not been set yet
            if(snippetText == null)
            {
                //get text
                snippetText = snippetLayout.findViewById(R.id.Map_Snippet_Text);
            }

            //if using text
            if(useText)
            {
                //update text
                snippetText.setText(text);
            }

            //update text visibility
            snippetText.setVisibility(useText && text != null ? View.VISIBLE : View.GONE);

            //if using of background changed and not using background
            if(backgroundChanged && !useBackground)
            {
                snippetLayout.setBackground(null);
                Globals.setShadowedText(snippetTitle);
                if(useText)
                {
                    Globals.setShadowedText(snippetText);
                }
            }

            //update size
            try
            {
                snippetLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                width = Math.max(snippetLayout.getMeasuredWidth(), 1);
                height = Math.max(snippetLayout.getMeasuredHeight(), 1);
                snippetLayout.layout(0, 0, width, height);
            }
            catch(Exception ex)
            {
                width = height = 1;
            }

            //get image
            snippetImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            snippetCanvas = new Canvas(snippetImage);

            //draw image
            try
            {
                snippetLayout.draw(snippetCanvas);
            }
            catch(Exception ex)
            {
                //do nothing
            }

            //update status
            lastUseBackground = useBackground;

            //return image
            return(snippetImage);
        }

        public Bitmap update(Context context, String title, String text, boolean showText, boolean showBackground)
        {
            useText = showText;
            useBackground = showBackground;
            return(get(context, title, text));
        }
    }

    private static class Board
    {
        private boolean isVisible;
        private final boolean isStar;
        private final boolean tleIsAccurate;
        private int skipLayoutCount;
        private final float imageScale;
        private double zValue;
        private double rotateDegrees;
        private final BaseController controller;
        private Bitmap boardImage;
        private final Billboard board;
        private final BillboardInfo boardInfo;
        private ScreenObject boardScreen;
        private MaplyTexture boardTexture;
        private ComponentObject boardObject;
        final ArrayList<Billboard> billboardList;

        private Board(BaseController boardController, Bitmap image, double imageRotateDegrees, float markerScale, boolean tleIsAc, boolean forStar, boolean visible)
        {
            Shader eyeShader = boardController.getShader(Shader.BillboardEyeShader);

            tleIsAccurate = tleIsAc;
            zValue = skipLayoutCount = 0;
            rotateDegrees = imageRotateDegrees;
            controller = boardController;
            isStar = forStar;
            imageScale = DefaultImageScale * (isStar ? StarScale : 1);

            board = new Billboard();
            boardInfo = new BillboardInfo();
            boardImage = image;
            boardTexture = null;
            boardObject = null;
            billboardList = new ArrayList<>(0);

            boardInfo.setDrawPriority(DrawPriority.BoardEye);
            boardInfo.setZBufferWrite(true);
            boardInfo.setZBufferRead(true);
            if(eyeShader != null)
            {
                boardInfo.setShader(eyeShader);
            }
            if(image != null && imageRotateDegrees != Double.MAX_VALUE && markerScale != Float.MAX_VALUE)
            {
                rotateImage(imageRotateDegrees, markerScale);
            }
            else
            {
                boardScreen = new ScreenObject();
                board.setScreenObject(boardScreen);
            }
            board.setSelectable(false);
            billboardList.add(board);

            setVisible(visible);
        }
        Board(BaseController boardController, boolean tleIsAc, boolean forStar, boolean visible)
        {
            this(boardController, null, Double.MAX_VALUE, Float.MAX_VALUE, tleIsAc, forStar, visible);
        }
        Board(BaseController boardController, Board copyFrom, float markerScale, boolean visible)
        {
            this(boardController, copyFrom.boardImage, copyFrom.rotateDegrees, markerScale, copyFrom.tleIsAccurate, copyFrom.isStar, visible);

            copyFrom.remove();
            board.setCenter(copyFrom.board.getCenter());
        }

        private void updateImage(Bitmap image, double offsetX, double offsetY, double baseScale, double markerScale)
        {
            float width;
            float height;
            float imageWidthScale;

            //if image is set
            if(image != null)
            {
                offsetX *= markerScale;
                offsetY *= markerScale;
                baseScale *= markerScale;

                if(boardTexture != null)
                {
                    controller.removeTexture(boardTexture, BaseController.ThreadMode.ThreadAny);
                    boardTexture = null;
                }

                if(boardImage != image)
                {
                    boardImage = image;
                }
                width = image.getWidth();
                height = image.getHeight();
                if(width < 1)
                {
                    width = 1;
                }
                if(height < 1)
                {
                    height = 1;
                }
                imageWidthScale = (width / height);

                boardTexture = controller.addTexture(image, new BaseController.TextureSettings(), BaseController.ThreadMode.ThreadAny);
                boardScreen = new ScreenObject();
                boardScreen.addTexture(boardTexture, new float[]{1.0f, 1.0f, 1.0f, 1.0f}, (float)(baseScale * imageWidthScale), (float)baseScale);
                boardScreen.translateX(offsetX, offsetY);
                if(rotateDegrees != Double.MAX_VALUE)
                {
                    boardScreen.rotate(Math.toRadians(rotateDegrees));
                }
                board.setScreenObject(boardScreen);
            }
        }

        void setImage(Bitmap image, double offsetX, double offsetY, double baseScale, double markerScale)
        {
            updateImage(image, offsetX, offsetY, baseScale, markerScale);
        }
        void setImage(Bitmap image, float markerScale)
        {
            setImage(image, imageScale / -2, imageScale / -2, imageScale, markerScale);
        }

        private void rotateImage(double degrees, float markerScale)
        {
            rotateDegrees = degrees;
            updateImage(boardImage, imageScale / -2, imageScale / -2, imageScale, markerScale);
        }

        public boolean getVisible()
        {
            return(isVisible);
        }

        public void setVisible(boolean visible)
        {
            isVisible = visible;
            boardInfo.setEnable(visible);
        }

        public void setSkipLayout(boolean skip)
        {
            if(skip)
            {
                if(skipLayoutCount == 0)
                {
                    remove();
                }
                skipLayoutCount++;
            }
            else
            {
                skipLayoutCount--;
                if(skipLayoutCount == 0)
                {
                    add();
                }
            }
        }

        void moveLocation(double latitude, double longitude, double altitudeKm, boolean limitAltitude)
        {
            //remember if non negative
            boolean negativeAltitudeKm = (altitudeKm < 0);

            //remove display
            remove();

            //normalize values
            latitude = Globals.normalizeLatitude(latitude);
            longitude = Globals.normalizeLongitude(longitude);
            zValue = getScaledZMeters(altitudeKm, limitAltitude);

            //set location
            board.setCenter(new Point3d(Math.toRadians(longitude), Math.toRadians(latitude), zValue));

            //if non negative
            if(!negativeAltitudeKm)
            {
                //add display
                add();
            }
        }

        private void add()
        {
            if(skipLayoutCount <= 0 && tleIsAccurate && boardObject == null)
            {
                boardObject = controller.addBillboards(billboardList, boardInfo, BaseController.ThreadMode.ThreadAny);
            }
        }

        public void remove()
        {
            if(skipLayoutCount <= 0 && boardObject != null)
            {
                controller.removeObject(boardObject, BaseController.ThreadMode.ThreadAny);
                boardObject = null;
            }
        }
    }

    private static class FlatObject
    {
        private boolean isVisible;
        private double flatScale;
        private double zoomScale;
        private int imageId;
        private int skipLayoutCount;
        private final float imageScale;
        private final Sticker flatSticker;
        private final StickerInfo flatInfo;
        private MaplyTexture flatTexture;
        private ComponentObject flatObject;
        private final BaseController controller;
        final ArrayList<Sticker> flatList;
        final ArrayList<MaplyTexture> flatTextureList;

        FlatObject(BaseController boardController, boolean forStar)
        {
            controller = boardController;

            skipLayoutCount = 0;

            flatTexture = null;
            flatObject = null;

            flatList = new ArrayList<>(0);
            flatTextureList = new ArrayList<>(0);

            flatSticker = new Sticker();
            flatSticker.setLowerLeft(new Point2d(0, 0));
            flatSticker.setUpperRight(new Point2d(0, 0));
            flatInfo = new StickerInfo();
            flatInfo.setDrawPriority(DrawPriority.BoardFlat);
            flatList.add(flatSticker);
            flatScale = zoomScale = 1;
            imageScale = DefaultImageScale * (forStar ? StarScale : 1);
            imageId = -1;

            setVisible(true);
        }

        void setImage(Bitmap image)
        {
            if(image != null)
            {
                if(flatTexture != null)
                {
                    controller.removeTexture(flatTexture, BaseController.ThreadMode.ThreadAny);
                    flatTextureList.clear();
                    flatTexture = null;
                }

                flatTexture = controller.addTexture(image, new BaseController.TextureSettings(), BaseController.ThreadMode.ThreadAny);
                flatTextureList.add(flatTexture);
                flatSticker.setTextures(flatTextureList);
            }
        }
        void setImage(Context context, int id, int color)
        {
            Bitmap image = (id > 0 ? Globals.getBitmap(context, id, color) : null);

            imageId = id;
            setImage(image);
        }

        void setRotation(double rotation)
        {
            flatSticker.setRotation(Math.toRadians(360 - rotation));
        }

        void setScale(double flatScaling)
        {
            flatScale = flatScaling;
        }

        void setZoomScale(double zoomScaling)
        {
            zoomScale = zoomScaling;
        }

        @SuppressWarnings("SameParameterValue")
        void setDrawPriority(int priority)
        {
            flatInfo.setDrawPriority(priority);
        }

        public boolean getVisible()
        {
            return(isVisible);
        }

        public void setVisible(boolean visible)
        {
            isVisible = visible;
            flatInfo.setEnable(visible);
        }

        public void setSkipLayout(boolean skip)
        {
            if(skip)
            {
                if(skipLayoutCount == 0)
                {
                    remove();
                }
                skipLayoutCount++;
            }
            else
            {
                skipLayoutCount--;
                if(skipLayoutCount == 0)
                {
                    add();
                }
            }
        }

        private void move(double latitude, double longitude, double halfLatitudeRadsWidth, double halfLongitudeRadsWidth, boolean show)
        {
            double latitudeRads;
            double longitudeRads;

            remove();

            //if showing
            if(show)
            {
                //normalize values
                latitude = Globals.normalizeLatitude(latitude);
                longitude = Globals.normalizeLongitude(longitude);
                latitudeRads = Math.toRadians(latitude);
                longitudeRads = Math.toRadians(longitude);

                //update sticker
                flatSticker.setLowerLeft(new Point2d(longitudeRads - halfLongitudeRadsWidth, latitudeRads - halfLatitudeRadsWidth));
                flatSticker.setUpperRight(new Point2d(longitudeRads + halfLongitudeRadsWidth, latitudeRads + halfLatitudeRadsWidth));

                add();
            }
        }

        void moveLocation(double latitude, double longitude, double latitudeWidth, double longitudeWidth)
        {
            double halfLatRadsWidth = Math.toRadians(latitudeWidth);
            double halfLonRadsWidth = Math.toRadians(longitudeWidth);

            move(latitude, longitude, halfLatRadsWidth, halfLonRadsWidth, true);
        }
        void moveLocation(double latitude, double longitude, boolean show)
        {
            double halfRadsWidth = (imageScale / 3.0) * flatScale * Math.min(zoomScale, 1);

            move(latitude, longitude, halfRadsWidth, halfRadsWidth, show);
        }

        private void add()
        {
            if(skipLayoutCount <= 0 && flatObject == null)
            {
                flatObject = controller.addStickers(flatList, flatInfo, BaseController.ThreadMode.ThreadAny);
            }
        }

        private void remove()
        {
            if(skipLayoutCount <= 0 && flatObject != null)
            {
                controller.removeObject(flatObject, BaseController.ThreadMode.ThreadAny);
                flatObject = null;
            }
        }
    }

    private static class ShapeObject
    {
        private boolean isVisible;
        private int skipLayoutCount;
        private final BaseController controller;
        private final ShapeCylinder shape;
        private final ShapeInfo shapeInfo;
        private ComponentObject shapeObject;
        private final ArrayList<Shape> cylinderList;

        ShapeObject(BaseController shapeController, int sampleCount, float radius, int alpha, int color, boolean visible)
        {
            skipLayoutCount = 0;
            controller = shapeController;
            shape = new ShapeCylinder();
            shapeInfo = new ShapeInfo();
            shapeObject = null;
            cylinderList = new ArrayList<>(0);

            shape.setBaseHeight(0.0);
            shape.setRadius(radius);
            shape.setSample(sampleCount);
            shapeInfo.setColor(Globals.getColor(alpha, Color.rgb(Color.red(color) / 2, Color.green(color) / 2, Color.blue(color) / 2)));
            shapeInfo.setDrawPriority(DrawPriority.Path);
            shapeInfo.setZBufferWrite(true);
            shapeInfo.setZBufferRead(true);
            cylinderList.add(shape);

            setVisible(visible);
        }

        public boolean getVisible()
        {
            return(isVisible);
        }

        public void setVisible(boolean visible)
        {
            isVisible = visible;
            shapeInfo.setEnable(visible);
        }

        public void setSkipLayout(boolean skip)
        {
            if(skip)
            {
                if(skipLayoutCount == 0)
                {
                    remove();
                }
                skipLayoutCount++;
            }
            else
            {
                skipLayoutCount--;
                if(skipLayoutCount == 0)
                {
                    add();
                }
            }
        }

        private void move(double latitude, double longitude, double altitudeKm)
        {
            remove();

            shape.setBaseCenter(Point2d.FromDegrees(longitude, latitude));
            shape.setHeight(getScaledZEarthRadiusPercent(altitudeKm));

            add();
        }

        private void add()
        {
            if(skipLayoutCount <= 0 && shapeObject == null)
            {
                shapeObject = controller.addShapes(cylinderList, shapeInfo, BaseController.ThreadMode.ThreadCurrent);
            }
        }

        public void remove()
        {
            if(skipLayoutCount <= 0 && shapeObject != null)
            {
                controller.removeObject(shapeObject, BaseController.ThreadMode.ThreadCurrent);
                shapeObject = null;
            }
        }
    }

    private static class Path
    {
        private boolean isVisible;
        private final boolean useVectors;
        private final boolean tleIsAccurate;
        private int skipLayoutCount;
        private final int selectedColor;
        private final int nonSelectedColor;
        private VectorObject flatPath;
        private ShapeLinear elevatedPath;
        private final VectorInfo flatPathInfo;
        private final ShapeInfo elevatedPathInfo;
        private ComponentObject pathObject;
        private final BaseController controller;

        Path(BaseController orbitalController, boolean isFlat, boolean tleIsAcc, int color)
        {
            skipLayoutCount = 0;
            useVectors = isFlat;
            tleIsAccurate = tleIsAcc;
            controller = orbitalController;
            selectedColor = color;
            nonSelectedColor = Globals.getColor(10, Color.rgb(Color.red(selectedColor) / 4, Color.green(selectedColor) / 4, Color.blue(selectedColor) / 4));

            if(useVectors)
            {
                flatPath = new VectorObject();
                flatPathInfo = new VectorInfo();
                flatPathInfo.setDrawPriority(DrawPriority.Path);

                elevatedPathInfo = null;
            }
            else
            {
                elevatedPath = new ShapeLinear();
                elevatedPathInfo = new ShapeInfo();
                elevatedPathInfo.setDrawPriority(DrawPriority.Path);

                flatPathInfo = null;
            }
            setColor(false);

            pathObject = null;

            //set to opposite to allow change
            isVisible = true;
            setVisible(false);
        }

        public void setPath(ArrayList<CoordinatesFragment.Coordinate> points)
        {
            int index;
            int pointsLength = points.size();
            double z;
            double slope;
            double nextLat;
            double nextLon;
            double currentLat;
            double currentLon;
            double currentAltKm;
            double splitLat;
            ArrayList<Point2d> setFlatPoints = new ArrayList<>(0);
            ArrayList<Point3d> setElevatedPoints = new ArrayList<>(0);

            remove();

            if(useVectors)
            {
                flatPath = new VectorObject();
            }
            else
            {
                elevatedPath = new ShapeLinear();
            }

            //add linear path
            for(index = 0; index < pointsLength; index++)
            {
                CoordinatesFragment.Coordinate currentPoint = points.get(index);
                CoordinatesFragment.Coordinate nextPoint = (index + 1 < pointsLength ? points.get(index + 1) : null);
                Point2d[] flatSplitPoints = null;

                //remember current location
                currentLat = currentPoint.latitude;
                currentLon = currentPoint.longitude;
                currentAltKm = currentPoint.altitudeKm;

                //if using vectors
                if(useVectors)
                {
                    //if there is a next point
                    if(nextPoint != null)
                    {
                        //get next latitude, longitude, and slope
                        nextLat = nextPoint.latitude;
                        nextLon = nextPoint.longitude;
                        slope = Globals.degreeDistance(currentLon, nextLon) / Globals.degreeDistance(currentLat, nextLat);

                        //if going across 180 eastward
                        if(currentLon >= 90 && currentLon <= 180 && nextLon >= -180 && nextLon <= -90)
                        {
                            //get latitude 180 point
                            splitLat = (Globals.degreeDistance(currentLon, 180) / slope) + currentLat;

                            //add points up to 180
                            setFlatPoints.add(Point2d.FromDegrees(currentLon, currentLat));
                            setFlatPoints.add(Point2d.FromDegrees(180, splitLat));

                            //set points past 180
                            flatSplitPoints = new Point2d[]{Point2d.FromDegrees(-180, splitLat), Point2d.FromDegrees(nextLon, nextLat)};
                        }
                        //else if going across 180 westward
                        else if(currentLon >= -180 && currentLon <= -90 && nextLon >= 90 && nextLon <= 180)
                        {
                            //get latitude -180 point
                            splitLat = (Globals.degreeDistance(currentLon, -180) / slope) + currentLat;

                            //add points up to -180
                            setFlatPoints.add(Point2d.FromDegrees(currentLon, currentLat));
                            setFlatPoints.add(Point2d.FromDegrees(-180, splitLat));

                            //set points past -180
                            flatSplitPoints = new Point2d[]{Point2d.FromDegrees(180, splitLat), Point2d.FromDegrees(nextLon, nextLat)};
                        }
                    }

                    //if there are split points
                    if(flatSplitPoints != null)
                    {
                        //add other points and reset
                        flatPath.addLinear(setFlatPoints.toArray(new Point2d[0]));
                        setFlatPoints.clear();

                        //add split points
                        setFlatPoints.add(flatSplitPoints[0]);
                        setFlatPoints.add(flatSplitPoints[1]);

                        //skip next point
                        index++;
                    }
                    else
                    {
                        //add point
                        setFlatPoints.add(Point2d.FromDegrees(currentLon, currentLat));
                    }
                }
                else
                {
                    //get z value in scaled earth radius percent
                    z = getScaledZEarthRadiusPercent(currentAltKm);

                    //add point
                    setElevatedPoints.add(new Point3d(Point2d.FromDegrees(currentLon, currentLat), z));
                }
            }

            //if there are points
            if(useVectors && !setFlatPoints.isEmpty())
            {
                //add them
                flatPath.addLinear(setFlatPoints.toArray(new Point2d[0]));
            }
            else if(!useVectors && !setElevatedPoints.isEmpty())
            {
                //add them
                elevatedPath.setCoords(setElevatedPoints.toArray(new Point3d[0]));
            }

            add();
        }

        private void setColor(boolean selected)
        {
            int color = (selected ? selectedColor : nonSelectedColor);
            float width = (selected ? 4f : 2f);

            if(flatPathInfo != null)
            {
                flatPathInfo.setColor(color);
                flatPathInfo.setLineWidth(width);
            }

            if(elevatedPathInfo != null)
            {
                elevatedPathInfo.setColor(color);
                elevatedPathInfo.setLineWidth(width);
            }
        }

        public void setSelected(boolean selected)
        {
            remove();
            setColor(selected);
            add();
        }

        public void setVisible(boolean visible)
        {
            //if changing
            if(isVisible != visible)
            {
                isVisible = visible;

                remove();
                if(useVectors)
                {
                    flatPathInfo.setEnable(visible);
                }
                else
                {
                    elevatedPathInfo.setEnable(visible);
                }
                add();
            }
        }

        public void setSkipLayout(boolean skip)
        {
            if(skip)
            {
                if(skipLayoutCount == 0)
                {
                    remove();
                }
                skipLayoutCount++;
            }
            else
            {
                skipLayoutCount--;
                if(skipLayoutCount == 0)
                {
                    add();
                }
            }
        }

        public void add()
        {
            if(skipLayoutCount <= 0 && tleIsAccurate && pathObject == null)
            {
                pathObject = (useVectors ? controller.addVector(flatPath, flatPathInfo, BaseController.ThreadMode.ThreadCurrent) : controller.addShapes(Collections.singletonList(elevatedPath), elevatedPathInfo, RenderControllerInterface.ThreadMode.ThreadCurrent));
            }
        }

        public void remove()
        {
            if(skipLayoutCount <= 0 && pathObject != null)
            {
                controller.removeObject(pathObject, BaseController.ThreadMode.ThreadCurrent);
                pathObject = null;
            }
        }
    }

    public static class MarkerObject extends CoordinatesFragment.MarkerBase
    {
        private static WeakReference<InfoImageCreator> infoCreator;

        private Context currentContext;
        private Shared common;
        private ScreenMarker marker;
        private ScreenMarker titleMarker;
        private ScreenMarker infoMarker;
        private MarkerInfo markerInfo;
        private MarkerInfo titleMarkerInfo;
        private MarkerInfo infoMarkerInfo;
        private ComponentObject markerObj;
        private ComponentObject titleMarkerObj;
        private ComponentObject infoMarkerObj;
        private boolean showInfo;
        private boolean usingInfo;
        private boolean alwaysShowTitle;
        private boolean showBackground;
        private int skipLayoutCount;
        private float markerScale;
        private double markerBaseSizeValue;
        private String titleText;
        private String infoText;
        private BaseController controller;

        MarkerObject(Context context, BaseController markerController, int noradId, String title, Bitmap image, Calculations.ObserverType markerLocation, float markerScaling, boolean usingBackground, boolean startWithTitleShown, boolean tleIsAccurate, int infoLocation, boolean isStar)
        {
            int locationIconType;
            float sizeDp;

            if(markerController != null)
            {
                currentContext = context;

                common = new Shared();
                common.noradId = noradId;
                common.tleIsAccurate = tleIsAccurate;

                controller = markerController;
                skipLayoutCount = 0;
                markerScale = markerScaling;
                switch(noradId)
                {
                    case Universe.IDs.Earth:
                    case Universe.IDs.Sun:
                    case Universe.IDs.Moon:
                    case Universe.IDs.Mars:
                    case Universe.IDs.Mercury:
                    case Universe.IDs.Venus:
                    case Universe.IDs.Jupiter:
                    case Universe.IDs.Saturn:
                    case Universe.IDs.Uranus:
                    case Universe.IDs.Neptune:
                    case Universe.IDs.Pluto:
                        sizeDp = 46;
                        break;

                    default:
                        sizeDp = 46 * (isStar ? StarScale : 1);
                        break;
                }
                markerBaseSizeValue = Globals.dpToPixels(currentContext, sizeDp);

                marker = new ScreenMarker();
                markerInfo = new MarkerInfo();
                locationIconType = Settings.getMapMarkerLocationIcon(context);
                marker.size = new Point2d(markerBaseSizeValue * markerScale, markerBaseSizeValue * markerScale);
                marker.userObject = noradId;
                marker.selectable = true;
                marker.rotation = Math.toRadians(noradId > 0 && noradId != Universe.IDs.CurrentLocation ? 135 : 0);
                markerObj = null;
                markerInfo.setDrawPriority(DrawPriority.BoardFlat + 100);

                showInfo = false;
                showBackground = usingBackground;
                alwaysShowTitle = startWithTitleShown && (noradId != Universe.IDs.CurrentLocation);
                usingInfo = (infoLocation == CoordinatesFragment.MapMarkerInfoLocation.UnderTitle);
                infoMarker = new ScreenMarker();
                infoMarkerInfo = new MarkerInfo();
                infoMarker.selectable = false;
                infoMarkerObj = null;
                infoText = "...";
                infoMarkerInfo.setDrawPriority(DrawPriority.BoardFlat + 200);

                titleMarker = new ScreenMarker();
                titleMarkerInfo = new MarkerInfo();
                titleMarker.selectable = false;
                titleMarkerObj = null;
                titleText = "...";
                titleMarkerInfo.setDrawPriority(DrawPriority.BoardFlat + 300);

                setSkipLayout(true);
                moveLocation(markerLocation.geo.latitude, markerLocation.geo.longitude, markerLocation.geo.altitudeKm);
                if(title != null)
                {
                    setTitle(title);
                }
                setImage((noradId == Universe.IDs.CurrentLocation ? Globals.getBitmap(currentContext, Globals.getLocationIconTypeIconID(locationIconType), Settings.getMapMarkerLocationIconUsedTintColor(context, locationIconType)) : image));
                setSkipLayout(false);
            }
        }

        private InfoImageCreator getInfoCreator()
        {
            if(infoCreator == null || infoCreator.get() == null)
            {
                infoCreator = new WeakReference<>(new InfoImageCreator(usingInfo, showBackground));
            }
            return(infoCreator.get());
        }

        @Override
        int getNoradId()
        {
            return(common.noradId);
        }

        @Override
        void setImage(Bitmap image)
        {
            remove();

            marker.image = image;
            markerObj = controller.addScreenMarker(marker, markerInfo, BaseController.ThreadMode.ThreadAny);

            add();
        }

        @Override
        void setRotation(double rotation)
        {
            marker.rotation = Math.toRadians(360 - rotation);
        }

        @Override
        void setTitle(String title)
        {
            remove();

            titleText = title;
            infoMarker.image = getInfoCreator().get(currentContext, titleText, infoText);
            infoMarker.size = new Point2d(infoMarker.image.getWidth() * markerScale, infoMarker.image.getHeight() * markerScale);

            titleMarker.image = getInfoCreator().get(currentContext, titleText, null);
            titleMarker.size = new Point2d(titleMarker.image.getWidth() * markerScale, titleMarker.image.getHeight() * markerScale);
            titleMarker.offset = new Point2d(0, (titleMarker.size.getY() / 2) + (marker.size.getY() / 2));

            add();
        }

        @Override
        void setText(String text)
        {
            remove();

            infoText = text;
            infoMarker.image = getInfoCreator().get(currentContext, titleText, infoText);
            infoMarker.size = new Point2d(infoMarker.image.getWidth() * markerScale, infoMarker.image.getHeight() * markerScale);
            infoMarker.offset = new Point2d(0, (infoMarker.size.getY() / 2) + (marker.size.getY() / 2));

            add();
        }

        @Override
        void setScale(float markerScaling)
        {
            remove();

            markerScale = markerScaling;
            marker.size = new Point2d(markerBaseSizeValue * markerScale, markerBaseSizeValue * markerScale);
            setTitle(titleText);

            add();
        }

        @Override
        void setShowBackground(boolean show)
        {
            showBackground = show;

            setTitle(titleText);
            setText(infoText);
        }

        @Override
        void setTitleAlwaysVisible(boolean visible)
        {
            alwaysShowTitle = visible && (common.noradId != Universe.IDs.CurrentLocation);

            remove();
            add();
        }

        @Override
        void setUsingInfo(boolean using)
        {
            usingInfo = using;

            setTitle(titleText);
            setText(infoText);
        }

        @Override
        void setInfoVisible(boolean visible)
        {
            showInfo = visible;

            remove();
            add();
        }

        @Override
        void setInfoLocation()
        {
            Point2d loc = Point2d.FromDegrees(Math.toDegrees(marker.loc.getX()), Globals.normalizeLatitude(Math.toDegrees(marker.loc.getY())));

            infoMarker.loc = loc;
            titleMarker.loc = loc;
        }

        @Override
        boolean getInfoVisible()
        {
            return(showInfo);
        }

        @Override
        void setVisible(boolean visible)
        {
            if(visible)
            {
                add();
            }
            else
            {
                remove();
            }
        }

        @Override
        void setSkipLayout(boolean skip)
        {
            if(skip)
            {
                if(skipLayoutCount == 0)
                {
                    remove();
                }
                skipLayoutCount++;
            }
            else
            {
                skipLayoutCount--;
                if(skipLayoutCount == 0)
                {
                    add();
                }
            }
        }

        @Override
        void moveLocation(double latitude, double longitude, double altitudeKm)
        {
            remove();

            marker.loc = Point2d.FromDegrees(longitude, latitude);
            setInfoLocation();

            if(altitudeKm < 0)
            {
                common.tleIsAccurate = false;
            }

            add();
        }

        public void add()
        {
            if(skipLayoutCount <= 0 && common.tleIsAccurate && controller != null)
            {
                if(markerObj == null && markerInfo != null)
                {
                    markerObj = controller.addScreenMarker(marker, markerInfo, BaseController.ThreadMode.ThreadAny);
                }
                if(infoMarkerObj == null && infoMarkerInfo != null && showInfo)
                {
                    setInfoLocation();
                    infoMarkerObj = controller.addScreenMarker(infoMarker, infoMarkerInfo, BaseController.ThreadMode.ThreadAny);
                }
                if(titleMarkerObj == null && titleMarkerInfo != null && !showInfo && alwaysShowTitle)
                {
                    setInfoLocation();
                    titleMarkerObj = controller.addScreenMarker(titleMarker, titleMarkerInfo, BaseController.ThreadMode.ThreadAny);
                }
            }
        }

        @Override
        public void remove()
        {
            if(skipLayoutCount <= 0 && controller != null)
            {
                if(markerObj != null)
                {
                    controller.removeObject(markerObj, BaseController.ThreadMode.ThreadAny);
                    markerObj = null;
                }
                if(infoMarkerObj != null)
                {
                    controller.removeObject(infoMarkerObj, BaseController.ThreadMode.ThreadAny);
                    infoMarkerObj = null;
                }
                if(titleMarkerObj != null)
                {
                    controller.removeObject(titleMarkerObj, BaseController.ThreadMode.ThreadAny);
                    titleMarkerObj = null;
                }
            }
        }
    }

    public static class OrbitalObject extends CoordinatesFragment.OrbitalBase
    {
        private static Bitmap starImage;
        private static Bitmap debrisImage;
        private static Bitmap satelliteImage;
        private static Bitmap rocketBodyImage;
        private static Bitmap constellationImage;
        private static Bitmap selectedFootprintImage;
        private static WeakReference<InfoImageCreator> infoCreator;

        boolean alwaysShowTitle;
        private final boolean forMap;
        private final int noradId;
        private boolean isStar;
        private boolean showPin;
        private boolean showShadow;
        private boolean showFootprint;
        private boolean showSelectedFootprint;
        private boolean showBackground;
        private boolean usingInfo;
        private boolean showingInfo;
        private boolean showingDirection;
        private boolean firstBearing;
        private boolean lastShowingDirection;
        private boolean lastMoveWithinZoom;
        private final boolean tleIsAccurate;
        private float markerScale;
        private double lastMoveZoom;
        private double orbitalRotation;
        private double lastOrbitalRotation;
        private String lastInfo;
        private final Context currentContext;
        private final Shared common;
        private Board infoBoard;
        private Board orbitalBoard;
        private FlatObject orbitalShadow;
        private FlatObject orbitalFootprint;
        private FlatObject orbitalSelectedFootprint;
        private MarkerObject orbitalMarker;
        private ShapeObject orbitalPin;
        private final Path orbitalPath;
        private final BaseController controller;

        OrbitalObject(Context context, BaseController orbitalController, Database.SatelliteData orbitalData, Calculations.ObserverType observerLocation, float markerScaling, boolean usingBackground, boolean usingDirection, boolean usingPin, boolean usingShadow, boolean startWithTitleShown, int infoLocation)
        {
            int iconId;
            byte orbitalType;
            boolean haveOrbital;
            Bitmap titleImage;
            Bitmap orbitalImage = null;
            Drawable orbitalBgImage;
            Drawable orbitalDrawable;
            String name;

            currentContext = context;
            common = new Shared();
            controller = orbitalController;
            forMap = (controller instanceof MapController);
            showPin = usingPin;
            showShadow = usingShadow;
            showFootprint = showSelectedFootprint = false;
            showBackground = usingBackground;
            showingDirection = usingDirection;
            firstBearing = true;
            alwaysShowTitle = startWithTitleShown;
            usingInfo = (infoLocation == CoordinatesFragment.MapMarkerInfoLocation.UnderTitle);
            showingInfo = lastShowingDirection = lastMoveWithinZoom = false;
            markerScale = markerScaling;
            orbitalRotation = lastMoveZoom = lastOrbitalRotation = 0;
            lastInfo = null;
            common.data = orbitalData;
            haveOrbital = (common.data != null);
            orbitalPin = null;
            orbitalShadow = null;
            orbitalFootprint = null;
            orbitalSelectedFootprint = null;
            name = (haveOrbital ? common.data.getName() : null);
            noradId = (haveOrbital ? common.data.getSatelliteNum() : Universe.IDs.Invalid);
            orbitalType = (haveOrbital ? common.data.getOrbitalType() : Database.OrbitalType.Star);
            isStar = false;

            //if have orbital
            if(haveOrbital)
            {
                //try to use saved image
                switch(orbitalType)
                {
                    case Database.OrbitalType.Star:
                        orbitalImage = Globals.copyBitmap(starImage);
                        isStar = true;
                        break;

                    case Database.OrbitalType.Satellite:
                        orbitalImage = Globals.copyBitmap(satelliteImage);
                        break;

                    case Database.OrbitalType.RocketBody:
                        orbitalImage = Globals.copyBitmap(rocketBodyImage);
                        break;

                    case Database.OrbitalType.Debris:
                        orbitalImage = Globals.copyBitmap(debrisImage);
                        break;

                    case Database.OrbitalType.Constellation:
                        orbitalImage = Globals.copyBitmap(constellationImage);
                        break;
                }

                //if image not set yet
                if(orbitalImage == null)
                {
                    //get image
                    iconId = Globals.getOrbitalIconId(context, noradId, orbitalType);
                    if(isStar)
                    {
                        orbitalDrawable = Globals.getOrbitalIcon(context, observerLocation, noradId, orbitalType);
                        orbitalImage = Globals.getBitmapSized(context, iconId, (int)(orbitalDrawable.getIntrinsicWidth() * StarScale), (int)(orbitalDrawable.getIntrinsicHeight() * StarScale), 0);
                    }
                    else
                    {
                        orbitalImage = (noradId == Universe.IDs.Moon ? Universe.Moon.getPhaseImage(context, observerLocation, System.currentTimeMillis()) : Globals.getBitmap(context, iconId, (noradId > 0 && (orbitalType != Database.OrbitalType.Satellite || Settings.getSatelliteIconImageIsThemeable(context)) ? Color.WHITE : 0)));
                        if(noradId > 0)
                        {
                            //add outline
                            orbitalBgImage = Globals.getDrawableSized(context, iconId, orbitalImage.getWidth(), orbitalImage.getHeight(), R.color.black, false);
                            orbitalImage = Globals.getBitmap(Globals.getDrawableCombined(context, 2, 2, true, new BitmapDrawable(context.getResources(), orbitalImage), orbitalBgImage));
                        }
                    }

                    //save image for repeat use
                    switch(orbitalType)
                    {
                        case Database.OrbitalType.Star:
                            starImage = Globals.copyBitmap(orbitalImage);
                            break;

                        case Database.OrbitalType.Satellite:
                            satelliteImage = Globals.copyBitmap(orbitalImage);
                            break;

                        case Database.OrbitalType.RocketBody:
                            rocketBodyImage = Globals.copyBitmap(orbitalImage);
                            break;

                        case Database.OrbitalType.Debris:
                            debrisImage = Globals.copyBitmap(orbitalImage);
                            break;

                        case Database.OrbitalType.Constellation:
                            constellationImage = Globals.copyBitmap(orbitalImage);
                            break;
                    }
                }
                if(selectedFootprintImage == null)
                {
                    //set/draw footprint image for repeat use
                    selectedFootprintImage = createFootprintImage(Settings.getMapFootprintType(context), Settings.getMapSelectedFootprintColor(context));
                }
            }

            //remember if old information and initialize path
            tleIsAccurate = (haveOrbital && (common.data.database == null || common.data.database.tleIsAccurate));
            orbitalPath = (haveOrbital ? new Path(controller, forMap || !Settings.getMapShow3dPaths(context), tleIsAccurate, common.data.getPathColor()) : null);

            if(forMap)
            {
                //create marker
                orbitalMarker = new MarkerObject(context, controller, noradId, (haveOrbital ? name : null), (haveOrbital ? orbitalImage : null), observerLocation, markerScale, usingBackground, alwaysShowTitle, tleIsAccurate, infoLocation, isStar);
            }
            else
            {
                //create board
                orbitalBoard = new Board(controller, tleIsAccurate, isStar, true);
                if(haveOrbital)
                {
                    orbitalBoard.setImage(orbitalImage, markerScale);
                }

                //add/remove pin and shadow
                setShowPin(showPin);
                setShowShadow(showShadow);

                //set title
                infoBoard = new Board(controller, tleIsAccurate, isStar, (showingInfo || alwaysShowTitle));
                if(haveOrbital)
                {
                    titleImage = getInfoCreator().get(context, name, null);
                    updateInfoBoard(titleImage, orbitalImage, 1);
                }
            }

            //set defaults
            common.bearing = (noradId > 0 ? 225 : 0);
            common.geo = new Calculations.GeodeticDataType();
            common.lastBearingGeo = new Calculations.GeodeticDataType(common.geo);

            //don't show anything until used
            setSkipLayout(true);
            setVisible(false);
            setInfoVisible(false);
            setSkipLayout(false);
        }

        public static void clearImages()
        {
            starImage = null;
            debrisImage = null;
            satelliteImage = null;
            rocketBodyImage = null;
            constellationImage = null;
            selectedFootprintImage = null;
        }

        private static Bitmap createFootprintImage(int footprintStyle, int color)
        {
            int alpha;
            boolean drawFill = false;
            boolean drawOutline = false;
            Paint footprintPaint;
            Canvas footprintCanvas;
            Bitmap footprintImage;

            //setup
            alpha = Color.alpha(color);
            footprintImage = Bitmap.createBitmap(1600, 1600, Bitmap.Config.ARGB_8888);
            footprintCanvas = new Canvas(footprintImage);
            footprintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            switch(footprintStyle)
            {
                case Settings.Options.MapView.FootprintType.Filled:
                case Settings.Options.MapView.FootprintType.OutlineFilled:
                    drawFill = true;
                    break;
            }
            switch(footprintStyle)
            {
                case Settings.Options.MapView.FootprintType.Outline:
                case Settings.Options.MapView.FootprintType.OutlineFilled:
                    drawOutline = true;
                    break;
            }

            //if filling
            if(drawFill)
            {
                //draw footprint
                footprintPaint.setColor(color);
                footprintPaint.setStyle(Paint.Style.FILL);
                footprintCanvas.drawCircle(800, 800, 800, footprintPaint);
            }

            //if drawing outline
            if(drawOutline)
            {
                //draw border
                footprintPaint.setColor(Globals.getColor(Math.min(alpha, 240) + 15, color));
                footprintPaint.setStyle(Paint.Style.STROKE);
                footprintPaint.setStrokeWidth(10f);
                footprintCanvas.drawCircle(800, 800, 795, footprintPaint);
            }

            //return image
            return(footprintImage);
        }

        private void updateInfoBoard(Bitmap titleImage, Bitmap orbitalImage, double zoomScale)
        {
            infoBoard.setImage(titleImage, (titleImage.getWidth() / 2f) * DefaultImageScale * -0.0093, (orbitalImage != null ? (orbitalImage.getHeight() / 2f) : 1) * DefaultImageScale * 0.0093 /* (1 / zoomScale)*/, (DefaultTextScale * (usingInfo && showingInfo ? 1.5 : 0.5)), markerScale * zoomScale);
        }

        private InfoImageCreator getInfoCreator()
        {
            if(infoCreator == null || infoCreator.get() == null)
            {
                infoCreator = new WeakReference<>(new InfoImageCreator(usingInfo, showBackground));
            }
            return(infoCreator.get());
        }

        private Point3d getCurrentPosition()
        {
            MapController mapController;

            if(controller != null)
            {
                if(controller instanceof GlobeController)
                {
                    GlobeView currentView = ((GlobeController)controller).getGlobeView();

                    if(currentView != null)
                    {
                        return(currentView.getLoc());
                    }
                }
                else if(controller instanceof MapController)
                {
                    mapController = (MapController)controller;
                    return(mapController.getPositionGeo());
                }
            }

            return(new Point3d(0, 0, 1));
        }

        @Override
        public Calculations.GeodeticDataType getGeo()
        {
            return(common.geo);
        }

        @Override
        public Database.SatelliteData getData()
        {
            return(common.data);
        }

        @Override
        int getSatelliteNum()
        {
            return(common.data != null ? common.data.getSatelliteNum() : Universe.IDs.Invalid);
        }

        @Override
        void setImage(Bitmap image)
        {
            if(forMap)
            {
                orbitalMarker.setImage(image);
            }
            else
            {
                orbitalBoard.setImage(image, markerScale);
            }
        }

        @Override
        void setRotation(double rotationRads)
        {
            orbitalRotation = Math.toDegrees(rotationRads);
        }

        @Override
        void setText(String text)
        {
            double useZoom = 1;
            double currentZoom;
            double deltaZ;
            double deltaPercent;
            boolean withinZoom;
            boolean textChanged = (lastInfo == null && text != null) || (lastInfo != null && !lastInfo.equals(text));
            boolean textCleared = (lastInfo != null && text == null);
            Bitmap infoImage;

            //if showing info or text was just cleared
            if(showingInfo || textCleared)
            {
                //remember last info
                lastInfo = text;
            }

            if(forMap)
            {
                //set text
                orbitalMarker.setText(text);
            }
            else
            {
                //get zoom and z distance to orbital
                currentZoom = getCurrentPosition().getZ();
                deltaZ = (currentZoom * ZoomToZValue) - orbitalBoard.zValue;
                withinZoom = (deltaZ > 0);

                //if showing info or not within zoom
                if(showingInfo || !withinZoom)
                {
                    //get zoom to use
                    useZoom = currentZoom;

                    //if a very distant orbital and within zoom
                    if(getGeo().altitudeKm >= 300000 && withinZoom)
                    {
                        //scale based on distance percentage
                        deltaPercent = (deltaZ / orbitalBoard.zValue);
                        useZoom *= (deltaPercent < 1 ? (deltaPercent * 0.7) : 0.6);
                    }
                }

                //if -text changed and using it- or -text cleared-
                if((textChanged && usingInfo && showingInfo) || textCleared)
                {
                    //recreate board
                    infoBoard = new Board(controller, infoBoard, markerScale, (showingInfo || alwaysShowTitle));

                    //set image
                    infoImage = getInfoCreator().get(currentContext, common.data.getName(), (usingInfo && showingInfo ? text : null));
                    if(orbitalBoard.boardImage != null)
                    {
                        updateInfoBoard(infoImage, orbitalBoard.boardImage, useZoom);
                    }
                }
            }
        }

        @Override
        void setScale(float markerScaling)
        {
            Bitmap orbitalImage;
            Bitmap infoImage;

            markerScale = markerScaling;

            if(forMap)
            {
                orbitalMarker.setScale(markerScale);
            }
            else if(orbitalBoard.boardImage != null)
            {
                //recreate orbital
                orbitalImage = orbitalBoard.boardImage.copy(orbitalBoard.boardImage.getConfig(), true);
                orbitalBoard = new Board(controller, orbitalBoard, markerScale, true);
                orbitalBoard.setImage(orbitalImage, markerScale);

                //if showing shadow
                if(showShadow && orbitalShadow != null)
                {
                    //update shadow
                    orbitalShadow.setScale(markerScale);
                }

                //recreate info
                infoImage = getInfoCreator().get(currentContext, common.data.getName(), (usingInfo && showingInfo ? lastInfo : null));
                infoBoard = new Board(controller, infoBoard, markerScale, (showingInfo || alwaysShowTitle));
                updateInfoBoard(infoImage, orbitalImage, 1);
            }
        }

        @Override
        void setShowBackground(boolean show)
        {
            showBackground = show;

            if(forMap)
            {
                orbitalMarker.setShowBackground(showBackground);
            }
            else
            {
                infoBoard.setImage(getInfoCreator().update(currentContext, common.data.getName(), (showingInfo ? lastInfo : null), usingInfo, showBackground), markerScale);
            }
        }

        @Override
        void setShowPin(boolean show)
        {
            showPin = show;

            if(!forMap && tleIsAccurate)
            {
                if(!showPin && orbitalPin != null)
                {
                    orbitalPin.remove();
                    orbitalPin = null;
                }
                else if(showPin && orbitalPin == null)
                {
                    orbitalPin = new ShapeObject(controller, Settings.getMapPinsSampleType(currentContext), Settings.getMapPinsSize(currentContext), Settings.getMapPinsAlpha(currentContext), common.data.getPathColor(), showPin);
                }
            }
        }

        @Override
        void setShowShadow(boolean show)
        {
            int noradId;

            showShadow = show;

            if(!forMap && tleIsAccurate)
            {
                if(!showShadow && orbitalShadow != null)
                {
                    orbitalShadow.remove();
                    orbitalShadow = null;
                }
                else if(showShadow && orbitalShadow == null)
                {
                    noradId = common.data.getSatelliteNum();

                    orbitalShadow = new FlatObject(controller, isStar);
                    orbitalShadow.setImage(currentContext, Globals.getOrbitalIconId(currentContext, noradId, common.data.getOrbitalType()), Color.argb((noradId < 0 ? 144 : 192), 0, 0, 0));
                }
            }
        }

        @Override
        void setShowFootprint(boolean show)
        {
            showFootprint = show;

            if(tleIsAccurate)
            {
                if(!showFootprint && orbitalFootprint != null)
                {
                    orbitalFootprint.remove();
                    orbitalFootprint = null;
                }
                else if(showFootprint && orbitalFootprint == null)
                {
                    orbitalFootprint = new FlatObject(controller, isStar);
                    orbitalFootprint.setImage(createFootprintImage(Settings.getMapFootprintType(currentContext), Globals.getColor(Settings.getMapFootprintAlpha(currentContext), common.data.getPathColor())));
                }
            }
        }

        @Override
        void setShowSelectedFootprint(boolean show)
        {
            showSelectedFootprint = show;

            if(tleIsAccurate)
            {
                if(!showSelectedFootprint && orbitalSelectedFootprint != null)
                {
                    orbitalSelectedFootprint.remove();
                    orbitalSelectedFootprint = null;
                }
                else if(showSelectedFootprint && orbitalSelectedFootprint == null)
                {
                    orbitalSelectedFootprint = new FlatObject(controller, isStar);
                    orbitalSelectedFootprint.setDrawPriority(DrawPriority.SelectedBoardFlat);
                    orbitalSelectedFootprint.setImage(selectedFootprintImage);
                }
            }
        }

        @Override
        void setTitleAlwaysVisible(boolean visible)
        {
            alwaysShowTitle = visible;

            if(forMap)
            {
                //update visibility
                orbitalMarker.setTitleAlwaysVisible(visible);
            }
            else
            {
                //force refresh
                setInfoVisible(getInfoVisible());
            }
        }

        @Override
        void setUsingInfo(boolean using)
        {
            usingInfo = using;

            if(forMap)
            {
                orbitalMarker.setUsingInfo(usingInfo);
            }
            else
            {
                infoBoard.setImage(getInfoCreator().update(currentContext, common.data.getName(), (showingInfo ? lastInfo : null), usingInfo, showBackground), markerScale);
            }
        }

        @Override
        void setUsingDirection(boolean using)
        {
            //update last and current showing direction
            lastShowingDirection = showingDirection;
            showingDirection = using;
        }

        @Override
        void setInfoVisible(boolean visible)
        {
            showingInfo = visible;

            if(forMap)
            {
                //pause updates
                orbitalMarker.setSkipLayout(true);

                //if want visible and not visible yet
                if(visible && !orbitalMarker.getInfoVisible())
                {
                    //update text with current position
                    orbitalMarker.setText(Globals.getCoordinateString(currentContext, common.geo.latitude, common.geo.longitude, common.geo.altitudeKm));
                }

                //update visibility
                orbitalMarker.setInfoVisible(showingInfo);

                //resume updates
                orbitalMarker.setSkipLayout(false);
            }
            else
            {
                //update size
                setText(showingInfo ? lastInfo : null);

                //update visibility
                infoBoard.setVisible(showingInfo || alwaysShowTitle);
            }
        }

        @Override
        boolean getInfoVisible()
        {
            return(forMap ? orbitalMarker.getInfoVisible() : showingInfo);
        }

        @Override
        void setVisible(boolean visible)
        {
            if(forMap)
            {
                orbitalMarker.setVisible(visible);
            }
            else
            {
                orbitalBoard.setVisible(visible);
                if(showPin && orbitalPin != null)
                {
                    orbitalPin.setVisible(visible);
                }
                if(showShadow && orbitalShadow != null)
                {
                    orbitalShadow.setVisible(visible);
                }
            }

            if(showFootprint && orbitalFootprint != null)
            {
                orbitalFootprint.setVisible(visible);
            }
            if(showSelectedFootprint && orbitalSelectedFootprint != null)
            {
                orbitalSelectedFootprint.setVisible(visible);
            }
        }

        @Override
        void setSkipLayout(boolean skip)
        {
            if(forMap)
            {
                if(orbitalMarker != null)
                {
                    orbitalMarker.setSkipLayout(skip);
                }
            }
            else
            {
                if(infoBoard != null)
                {
                    infoBoard.setSkipLayout(skip);
                }
                if(orbitalBoard != null)
                {
                    orbitalBoard.setSkipLayout(skip);
                }
                if(orbitalPin != null)
                {
                    orbitalPin.setSkipLayout(skip);
                }
                if(orbitalShadow != null)
                {
                    orbitalShadow.setSkipLayout(skip);
                }
            }

            if(orbitalFootprint != null)
            {
                orbitalFootprint.setSkipLayout(skip);
            }
            if(orbitalSelectedFootprint != null)
            {
                orbitalSelectedFootprint.setSkipLayout(skip);
            }
            if(orbitalPath != null)
            {
                orbitalPath.setSkipLayout(skip);
            }
        }

        @Override
        void setPath(ArrayList<CoordinatesFragment.Coordinate> points)
        {
            if(orbitalPath != null)
            {
                orbitalPath.setPath(points);
            }
        }

        @Override
        void setPathSelected(boolean selected)
        {
            if(orbitalPath != null)
            {
                orbitalPath.setSelected(selected);
            }
        }

        @Override
        void setPathVisible(boolean visible)
        {
            if(orbitalPath != null)
            {
                orbitalPath.setVisible(visible);
            }
        }

        @Override
        public void moveLocation(double latitude, double longitude, double altitudeKm)
        {
            boolean withinZoom;
            boolean updateBearing;
            boolean usedBearing = false;
            boolean canUseBearing = (showingDirection && noradId > 0);
            boolean canShowFootprint = (showFootprint && orbitalFootprint != null);
            boolean showingDirectionChanged = (lastShowingDirection != showingDirection);
            boolean canShowSelectedFootprint = (showSelectedFootprint && orbitalSelectedFootprint != null);
            double currentZoom;
            double currentLatitude;
            double currentLongitude;
            double bearing;
            double bearingDelta;
            double orbitalRotationDelta;
            Point3d currentLocation;
            Calculations.GeodeticAreaType selectedFootprint;

            //remember last and current location
            if(canUseBearing)
            {
                common.lastBearingGeo = new Calculations.GeodeticDataType(common.geo);
            }
            common.geo = new Calculations.GeodeticDataType(latitude, longitude, altitudeKm, 0, 0);

            //if can use bearing
            canUseBearing = (canUseBearing && common.lastBearingGeo.isSet() && !common.geo.equals(common.lastBearingGeo));
            if(canUseBearing)
            {
                //get bearing and delta
                bearing = Calculations.getBearing(common.lastBearingGeo, common.geo);
                bearingDelta = Globals.degreeDistance(common.bearing, bearing);
                orbitalRotationDelta = Globals.degreeDistance(orbitalRotation, lastOrbitalRotation);
            }
            else
            {
                //don't use
                bearing = bearingDelta = orbitalRotationDelta = 0;
            }

            //update bearing if -using and -first bearing, enough to see, or rotation changed-- or -changed showing direction-
            updateBearing = (showingDirection && ((canUseBearing && firstBearing) || (Math.abs(bearingDelta) >= 2 || Math.abs(orbitalRotationDelta) >= 2))) || (showingDirectionChanged && canUseBearing);
            if(updateBearing)
            {
                //update value
                common.bearing = bearing;
            }

            if(forMap)
            {
                //if updating bearing
                if(updateBearing)
                {
                    //rotate marker
                    orbitalMarker.setRotation(bearing + 135);
                    usedBearing = true;
                }

                //move orbital
                orbitalMarker.moveLocation(latitude, longitude, altitudeKm);
            }
            else
            {
                //if updating bearing and image is set
                if(updateBearing && orbitalBoard.boardImage != null)
                {
                    //rotate image and recreate board
                    orbitalBoard.rotateDegrees = (bearing + 135 + orbitalRotation);
                    orbitalBoard = new Board(controller, orbitalBoard, markerScale, true);
                    usedBearing = true;
                }

                //get current location and if orbital within zoom
                currentLocation = getCurrentPosition();
                currentLatitude = Math.toDegrees(currentLocation.getY());
                currentLongitude = Math.toDegrees(currentLocation.getX());
                currentZoom = currentLocation.getZ();
                withinZoom = (((currentZoom * ZoomToZValue) - orbitalBoard.zValue) > 0) || (Math.abs(Globals.degreeDistance(currentLatitude, latitude)) >= 100) || (Math.abs(Globals.degreeDistance(currentLongitude, longitude)) >= 100);

                //if -there is a last zoom- and -showing shadow- and --within zoom changed- or -not within zoom and zoom changed--
                if(lastMoveZoom != 0 && showShadow && ((withinZoom != lastMoveWithinZoom) || (!withinZoom && currentZoom != lastMoveZoom)))
                {
                    //update info size
                    setText(showingInfo ? lastInfo : null);
                }
                if(currentZoom != lastMoveZoom)
                {
                    updateInfoBoard(infoBoard.boardImage, infoBoard.boardImage, (withinZoom || !showShadow ? 1 : (float)currentZoom));
                    lastMoveZoom = currentZoom;
                }
                infoBoard.moveLocation(latitude, longitude, (withinZoom || !showShadow ? altitudeKm : 0.5), withinZoom || !showShadow);

                //if showing pin and it exists
                if(showPin && orbitalPin != null)
                {
                    //move pin
                    orbitalPin.move(latitude, longitude, altitudeKm);
                }

                //if showing shadow
                if(showShadow)
                {
                    //reset
                    usedBearing = false;

                    //if shadow exists
                    if(orbitalShadow != null)
                    {
                        //if updating bearing and image is set
                        if(updateBearing && orbitalShadow.imageId > 0)
                        {
                            //rotate image
                            orbitalShadow.setRotation(bearing + 135);
                            usedBearing = true;
                        }

                        //move shadow
                        orbitalShadow.setZoomScale(currentZoom);
                        orbitalShadow.moveLocation(latitude, longitude, (altitudeKm > 0));
                    }
                }

                //move orbital
                orbitalBoard.moveLocation(latitude, longitude, altitudeKm, true);

                //update last move status
                lastMoveZoom = currentZoom;
                lastMoveWithinZoom = withinZoom;
            }

            //if showing a footprint that exists
            if(canShowFootprint || canShowSelectedFootprint)
            {
                //move footprints
                selectedFootprint = Calculations.getFootprint(latitude, altitudeKm);
                if(canShowFootprint)
                {
                    orbitalFootprint.moveLocation(latitude, longitude, selectedFootprint.latitudeWidth, selectedFootprint.longitudeWidth);
                }
                if(canShowSelectedFootprint)
                {
                    orbitalSelectedFootprint.moveLocation(latitude, longitude, selectedFootprint.latitudeWidth, selectedFootprint.longitudeWidth);
                }
            }

            //if updated bearing
            if(usedBearing)
            {
                //update last rotation and showing direction
                lastOrbitalRotation = orbitalRotation;
                lastShowingDirection = showingDirection;

                //if not updating from showing direction changing
                if(!showingDirectionChanged)
                {
                    //got first bearing
                    firstBearing = false;
                }
            }
        }

        @Override
        public void remove()
        {
            if(forMap)
            {
                orbitalMarker.remove();
            }
            else
            {
                orbitalBoard.remove();
                if(showPin && orbitalPin != null)
                {
                    orbitalPin.remove();
                }
                if(showShadow && orbitalShadow != null)
                {
                    orbitalShadow.remove();
                }
                infoBoard.remove();
            }

            if(showFootprint && orbitalFootprint != null)
            {
                orbitalFootprint.remove();
            }
            if(showSelectedFootprint && orbitalSelectedFootprint != null)
            {
                orbitalSelectedFootprint.remove();
            }

            if(orbitalPath != null)
            {
                orbitalPath.remove();
            }
        }
    }

    private static class LatLonTileSource extends SimpleTileFetcher
    {
        private final int thickness;
        final CoordinatesFragment.TileShared common;

        LatLonTileSource(Context context, BaseController controller, int borderThickness)
        {
            super(controller, "LatLonTileSource");

            minZoom = 0;
            maxZoom = 18;
            thickness = borderThickness;
            common = new CoordinatesFragment.TileShared(context);

            initWithName(name, minZoom, maxZoom);

            valid = true;
            start();
        }

        @Override
        public byte[] dataForTile(Object fetchInfo, TileID tileID)
        {
            Bitmap tileImage;
            ByteArrayOutputStream tileStream = new ByteArrayOutputStream();
            byte[] data;

            if(tileID.level == 0 || thickness < 0)
            {
                tileImage = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
            }
            else
            {
                tileImage = common.getLatitudeLongitudeTile(tileID.x, tileID.y - 1, tileID.level, 512, thickness, true);
            }

            tileImage.compress(Bitmap.CompressFormat.PNG, 1, tileStream);
            data = tileStream.toByteArray();
            tileImage.recycle();

            return(data);
        }
    }

    private static abstract class GlobeMapBase extends GlobeMapFragment implements CoordinatesFragment
    {
        protected boolean allowRotate;
        protected double lastRotationRads;
        protected final CoordinatesFragment.Shared common;
        protected MaplyStarModel stars;
        QuadImageLoader latLonLoader;

        GlobeMapBase()
        {
            super();

            allowRotate = true;
            lastRotationRads = 0;
            common = new Shared();
            stars = null;
            latLonLoader = null;
        }

        @Override
        public boolean isMap()
        {
            return(chooseDisplayType() == GlobeMapFragment.MapDisplayType.Map);
        }

        private BaseController getControl()
        {
            return(isMap() ? mapControl : globeControl);
        }

        @Override
        public View getView()
        {
            return(super.getView());
        }

        @Override
        public void setArguments(Bundle args)
        {
            try
            {
                super.setArguments(args);
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }

        @Override
        protected void preControlCreated()
        {
            if(isMap())
            {
                mapSettings.modelsHaveDepth = false;
                mapSettings.trianglesHaveDepth = false;
            }
            else
            {
                globeSettings.modelsHaveDepth = false;
                globeSettings.trianglesHaveDepth = false;
            }
        }

        @Override
        protected void controlHasStarted()
        {
            final Activity activity = this.getActivity();
            int mapLayerType;
            byte gpuType;
            String gpuModel;
            boolean forMap = isMap();
            boolean forPreview = false;
            boolean useCompatibility;
            boolean useOrbitalDirection = Settings.getMapShowOrbitalDirection(activity);
            boolean useOrbitalDirectionLimit = Settings.getMapShowOrbitalDirectionUseLimit(activity);
            Bundle args = this.getArguments();
            BaseController baseControl = getControl();

            if(args == null)
            {
                args = new Bundle();
            }
            mapLayerType = args.getInt(ParamTypes.MapLayerType, MapLayerType.Normal);

            //reset static images
            OrbitalObject.clearImages();

            //set if showing sunlight
            setShowSunlight(Settings.getMapShowSunlight(activity));

            //get if preview
            switch(mapLayerType)
            {
                case MapLayerType.Moon:
                case MapLayerType.Mars:
                case MapLayerType.Venus:
                case MapLayerType.Mercury:
                case MapLayerType.Jupiter:
                case MapLayerType.Saturn:
                case MapLayerType.Uranus:
                case MapLayerType.Neptune:
                case MapLayerType.Sun:
                case MapLayerType.Pluto:
                    forPreview = true;
                    break;
            }

            //setup base layers
            addLayer(mapLayerType, !forPreview, !forPreview && common.getShowSunlight());

            //set control
            if(forMap)
            {
                mapControl.setViewExtents(Point2d.FromDegrees(-180 * 100, -90), Point2d.FromDegrees(180 * 100, 90));
                mapControl.setZoomLimits(MinZoom, MaxMapZoom);
                mapControl.gestureDelegate = this;
            }
            else
            {
                globeControl.setClearColor(Color.BLACK);
                globeControl.setZoomLimits(MinZoom, MaxGlobeZoom);
                globeControl.gestureDelegate = this;

                //if activity is set
                if(activity != null)
                {
                    //when surface is visible
                    globeControl.addPostSurfaceRunnable(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            //try to add stars
                            setStarsEnabled(Settings.getMapShowStars(activity));
                        }
                    });
                }
            }
            baseControl.setHeightChangedListener(new BaseController.HeightListener()
            {
                public void onHeightChanged(double z)
                {
                    //send event
                    common.heightChanged(z);
                }
            });
            baseControl.setLocationChangedListener(new BaseController.LocationListener()
            {
                public void onLocationChanged(double x, double y, double z)
                {
                    //send event
                    common.locationChanged(x, y, z);
                }
            });

            //set frame rate
            baseControl.setTargetFPS(Settings.getMapFrameRate(activity));

            //set sensitivity and speed scale
            setSensitivityScale(Settings.getMapSensitivityScale(activity, !forMap));
            setSpeedScale(Settings.getMapSpeedScale(activity, !forMap));

            //set if showing orbital direction
            gpuType = baseControl.getRenderGpuType();
            gpuModel = baseControl.getRenderGpuModel();
            switch(gpuType)
            {
                case RenderGPUType.Mali:
                    if(gpuModel != null)
                    {
                        gpuModel = gpuModel.toLowerCase();
                        if(!gpuModel.startsWith("t") && !gpuModel.endsWith("mp"))
                        {
                            useCompatibility = false;
                            break;
                        }
                    }
                    //else fall through

                case RenderGPUType.PowerVr:
                    useCompatibility = true;
                    break;

                default:
                    useCompatibility = false;
                    break;
            }
            Settings.setUseGlobeCompatibility(activity, useCompatibility);
            if(useCompatibility)
            {
                //force limit on
                useOrbitalDirectionLimit = true;
                Settings.setMapShowOrbitalDirectionUseLimit(activity, true);

                //disable shadows
                Settings.setMapMarkerShowShadow(activity, false);
            }
            setShowOrbitalDirection(useOrbitalDirection);
            setShowOrbitalDirectionLimitCount(useOrbitalDirection && useOrbitalDirectionLimit ? Settings.getMapShowOrbitalDirectionLimit(activity) : 0);

            //set if tilt allowed
            setRotateAllowed(Settings.getMapRotateAllowed(activity));

            //if for preview
            if(forPreview)
            {
                //move to almost center (bug with display) and zoom out to show entire area
                moveCamera(0.01, 0.01, 2.25);
            }

            //update status
            common.ready();
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
        public void setOnRotatedListener(OnRotatedListener listener)
        {
            common.setOnRotatedListener(listener);
        }

        @Override
        public void setOnHeightChangedListener(OnHeightChangedListener listener)
        {
            common.setOnHeightChangedListener(listener);
        }

        @Override
        public void setOnLocationClickListener(OnLocationClickListener listener)
        {
            common.setOnLocationClickListener(listener);
        }

        @Override
        public void setOnLocationChangedListener(OnLocationChangedListener listener)
        {
            common.setOnLocationChangedListener(listener);
        }

        @Override
        public void setOnItemSelectionChangedListener(OnItemSelectionChangedListener listener)
        {
            common.setOnItemSelectionChangedListener(listener);
        }

        @Override
        public void globeDidStartMoving(GlobeController controller, boolean userMotion)
        {
            common.moved(userMotion);
        }

        @Override
        public void mapDidStartMoving(MapController controller, boolean userMotion)
        {
            common.moved(userMotion);
        }

        @Override
        public void globeDidMove(GlobeController globeControl, Point3d[] corners, boolean userMotion)
        {
            super.globeDidMove(globeControl, corners, userMotion);
            handleHeadingChanged();
        }

        @Override
        public void mapDidMove(MapController mapControl, Point3d[] corners, boolean userMotion)
        {
            super.mapDidMove(mapControl, corners, userMotion);
            handleHeadingChanged();
        }

        @Override
        public double getCameraZoom()
        {
            if(isMap())
            {
                return(mapControl != null && mapControl.getPositionGeo() != null ? mapControl.getPositionGeo().getZ() : 1);
            }
            else
            {
                return(globeControl != null && globeControl.getGlobeView() != null ? globeControl.getGlobeView().getLoc().getZ() : 1);
            }
        }

        @Override
        public void moveCamera(double latitude, double longitude, double zoom)
        {
            //if control exists
            if(getControl() != null)
            {
                //if map
                if(isMap())
                {
                    //move to position
                    mapControl.setPositionGeo(Math.toRadians(longitude), Math.toRadians(latitude), zoom);

                    //if allowing rotation
                    if(allowRotate)
                    {
                        //reset tilt
                        mapControl.setHeading(0);
                    }
                }
                else
                {
                    //if allowing rotation
                    if(allowRotate)
                    {
                        //reset rotation before move
                        globeControl.setKeepNorthUp(true);
                    }

                    //move to position
                    globeControl.setPositionGeo(Math.toRadians(longitude), Math.toRadians(latitude), zoom);

                    //if allowing rotation
                    if(allowRotate)
                    {
                        //allow rotation again
                        globeControl.setKeepNorthUp(false);
                    }
                }
            }
        }

        @Override
        public void moveCamera(double latitude, double longitude)
        {
            moveCamera(latitude, longitude, getCameraZoom());
        }

        @Override
        public void userDidTap(MapController controller, Point2d loc, Point2d screenLoc)
        {
            common.locationClick(Math.toDegrees(loc.getY()), Math.toDegrees(loc.getX()));
        }

        @Override
        public void userDidTap(GlobeController controller, Point2d loc, Point2d screenLoc)
        {
            common.locationClick(Math.toDegrees(loc.getY()), Math.toDegrees(loc.getX()));
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

        private SamplingParams createTileParams(TileInfoNew sourceInfo, boolean forBase)
        {
            SamplingParams tileParams = new SamplingParams();

            tileParams.setCoordSystem(new SphericalMercatorCoordSystem());
            tileParams.setCoverPoles(forBase && !isMap());
            tileParams.setEdgeMatching(!isMap());
            tileParams.setMinZoom(sourceInfo.minZoom);
            tileParams.setMaxZoom(sourceInfo.maxZoom);
            if(!forBase)
            {
                tileParams.setMaxTiles(32);
            }
            tileParams.setSingleLevel(true);

            return(tileParams);
        }

        private void addLayer(int mapLayerType, boolean addAtmosphere, boolean addSunlight)
        {
            Activity activity = this.getActivity();
            boolean haveActivity = (activity != null);
            int index;
            int maxZoom;
            String name;
            String maptiler_key_string = (haveActivity ? activity.getString(R.string.maptiler_api_key) : "");
            String stamen_key_string = (haveActivity ? activity.getString(R.string.stamen_map_key) : "");
            Light sunLight;
            Point3d sunDirection;
            Atmosphere atmosphere;
            ArrayList<Integer> drawPriorities = new ArrayList<>(0);
            ArrayList<String> cacheDirNames = new ArrayList<>(0);
            ArrayList<RemoteTileInfoNew> layerSources = new ArrayList<>(0);

            //if activity is set
            if(haveActivity)
            {
                //if not a map and adding atmosphere or sunlight
                if(!isMap() && (addAtmosphere || addSunlight))
                {
                    //get sun direction
                    sunDirection = (new com.mousebird.maply.Sun(new Date())).getDirection();

                    //if adding atmosphere
                    if(addAtmosphere)
                    {
                        //create atmosphere
                        atmosphere = new Atmosphere(globeControl, BaseController.ThreadMode.ThreadAny);
                        atmosphere.seteSun(10f);
                        atmosphere.setWaveLength(0.65f, 0.57f, 0.475f);
                        atmosphere.setSunPosition(sunDirection);
                    }

                    //if adding sunlight
                    if(addSunlight)
                    {
                        //create sunlight
                        sunLight = new Light();
                        sunLight.setPos(new Point3d(sunDirection.getX(), sunDirection.getZ(), sunDirection.getY()));
                        sunLight.setAmbient(0.4f, 0.4f, 0.4f, 0.7f);
                        sunLight.setDiffuse(0.8f, 0.8f, 0.8f, 1f);
                        sunLight.setViewDependent(true);
                        globeControl.clearLights();
                        globeControl.addLight(sunLight);
                    }
                }

                //get layer
                switch(mapLayerType)
                {
                    case MapLayerType.Hybrid:
                        drawPriorities.add(DrawPriority.LayerHybridLabels);
                        cacheDirNames.add("hybridLabels_stamen");
                        layerSources.add(new RemoteTileInfoNew("https://tiles.stadiamaps.com/tiles/stamen_terrain_labels/{z}/{x}/{y}@2x.png?api_key=" + stamen_key_string, 0, 17));

                        drawPriorities.add(DrawPriority.LayerHybridLines);
                        cacheDirNames.add("hybridLines_stamen");
                        layerSources.add(new RemoteTileInfoNew("https://tiles.stadiamaps.com/tiles/stamen_terrain_lines/{z}/{x}/{y}@2x.png?api_key=" + stamen_key_string, 0, 12));
                        //fall through

                    case MapLayerType.Satellite:
                        drawPriorities.add(DrawPriority.Layer);
                        if(Settings.getSatelliteClouds(activity, isMap()))
                        {
                            cacheDirNames.add("satellite");
                            layerSources.add(new RemoteTileInfoNew("https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/VIIRS_SNPP_CorrectedReflectance_TrueColor/default/2019-03-18/GoogleMapsCompatible_Level9/{z}/{y}/{x}.jpg", 0, 9));
                        }
                        else
                        {
                            cacheDirNames.add("satelliteLand");
                            layerSources.add(new RemoteTileInfoNew("https://api.maptiler.com/tiles/satellite/{z}/{x}/{y}.jpg?key=" + maptiler_key_string, 0, 10));
                        }
                        break;

                    case MapLayerType.Moon:
                        drawPriorities.add(DrawPriority.Layer);
                        cacheDirNames.add("moon");
                        layerSources.add(new RemoteTileInfoNew("https://trek.nasa.gov/tiles/Moon/EQ/LRO_WAC_Mosaic_Global_303ppd/1.0.0/default/default028mm/{z}/{y}/{x}.jpg", 0, 7));
                        break;

                    case MapLayerType.Mars:
                        drawPriorities.add(DrawPriority.Layer);
                        cacheDirNames.add("mars");
                        layerSources.add(new RemoteTileInfoNew("https://api.nasa.gov/mars-wmts/catalog/Mars_Viking_MDIM21_ClrMosaic_global_232m/1.0.0/default/default028mm/{z}/{y}/{x}.jpg", 0, 9));
                        break;

                    case MapLayerType.Venus:
                    case MapLayerType.Mercury:
                    case MapLayerType.Jupiter:
                    case MapLayerType.Saturn:
                    case MapLayerType.Uranus:
                    case MapLayerType.Neptune:
                    case MapLayerType.Sun:
                    case MapLayerType.Pluto:
                        maxZoom = 5;

                        switch(mapLayerType)
                        {
                            case MapLayerType.Mercury:
                                name = "mercury";
                                break;

                            case MapLayerType.Jupiter:
                                name = "jupiter";
                                maxZoom = 4;
                                break;

                            case MapLayerType.Saturn:
                                name = "saturn";
                                maxZoom = 4;
                                break;

                            case MapLayerType.Uranus:
                                name = "uranus";
                                maxZoom = 3;
                                break;

                            case MapLayerType.Neptune:
                                name = "neptune";
                                maxZoom = 3;
                                break;

                            case MapLayerType.Sun:
                                name = "sun";
                                break;

                            case MapLayerType.Pluto:
                                name = "pluto";
                                maxZoom = 4;
                                break;

                            default:
                            case MapLayerType.Venus:
                                name = "venus";
                                break;
                        }
                        drawPriorities.add(DrawPriority.Layer);
                        cacheDirNames.add(name);
                        layerSources.add(new RemoteTileInfoNew("https://github.com/calsurferpunk/tiles/raw/main/" + name + "/{z}/{x}/{y}.jpg", 0, maxZoom));
                        break;

                    default:
                    case MapLayerType.Normal:
                        drawPriorities.add(DrawPriority.Layer);
                        cacheDirNames.add("normal_stamen");
                        layerSources.add(new RemoteTileInfoNew("https://tiles.stadiamaps.com/tiles/stamen_terrain_background/{z}/{x}/{y}@2x.png?api_key=" + stamen_key_string, 0, 17));
                        break;
                }

                //go through each cache dir and source
                for(index = 0; index < cacheDirNames.size() && index < layerSources.size(); index++)
                {
                    File cacheDir;
                    RemoteTileInfoNew sourceInfo;
                    QuadImageLoader tileLoader;

                    //setup cache
                    cacheDir = new File(activity.getCacheDir(), cacheDirNames.get(index));
                    //noinspection ResultOfMethodCallIgnored
                    cacheDir.mkdir();

                    //setup layer
                    sourceInfo = layerSources.get(index);
                    sourceInfo.cacheDir = cacheDir;
                    tileLoader = new QuadImageLoader(createTileParams(sourceInfo, true), sourceInfo, getControl());
                    tileLoader.setBaseDrawPriority(drawPriorities.get(index));
                }
            }
        }

        @Override @SuppressWarnings("SpellCheckingInspection")
        public void setStarsEnabled(boolean enabled)
        {
            Activity activity;

            //if for globe
            if(!isMap())
            {
                //if enabled
                if(enabled)
                {
                    activity = this.getActivity();
                    if(stars == null && activity != null)
                    {
                        try
                        {
                            //add stars
                            globeControl.makeLayerThread(false, "Particles");
                            stars = new MaplyStarModel("starcatalog_orig.txt", "star_background.png", activity);
                            stars.addToViewc(globeControl, BaseController.ThreadMode.ThreadCurrent);
                        }
                        catch(Exception ex)
                        {
                            //do nothing
                        }
                    }
                }
                //else if stars are set
                else if(stars != null)
                {
                    //remove stars
                    stars.removeFromView();
                    stars = null;
                }
            }
        }

        @Override
        public void setSensitivityScale(float sensitivityScaling)
        {
            //if for map
            if(isMap())
            {
                mapControl.setScrollScale(sensitivityScaling);
            }
            else
            {
                globeControl.setScrollScale(sensitivityScaling);
            }
        }

        @Override
        public void setSpeedScale(float speedScaling)
        {
            //if for map
            if(isMap())
            {
                mapControl.setAccelerationMomentumScale(speedScaling);
            }
            else
            {
                globeControl.setAccelerationMomentumScale(speedScaling);
            }
        }

        @Override
        public void setRotateAllowed(boolean enabled)
        {
            allowRotate = enabled;

            if(getControl() != null)
            {
                if(isMap())
                {
                    mapControl.setAllowRotateGesture(enabled);
                }
                else
                {
                    globeControl.setKeepNorthUp(!enabled);
                }
            }
        }

        @Override
        public void setLatitudeLongitudeGridEnabled(boolean enabled)
        {
            BaseController controller = getControl();
            LatLonTileSource latLonSource = new LatLonTileSource(getContext(), controller, (enabled ? 6 : -1));
            TileInfoNew sourceInfo = latLonSource.getTileInfo();

            if(latLonLoader == null)
            {
                latLonLoader = new QuadImageLoader(createTileParams(sourceInfo, false), sourceInfo, controller);
                latLonLoader.setBaseDrawPriority(DrawPriority.LayerLatLon);
            }
            latLonLoader.setTileFetcher(latLonSource);
            latLonLoader.changeTileInfo(sourceInfo);
        }

        @Override
        public void setHeading(double degrees)
        {
            double rads;
            Point3d geo;

            //if control exists
            if(getControl() != null)
            {
                //get heading in radians and remember value
                rads = Math.toRadians(Globals.normalizeAngle(degrees));
                lastRotationRads = rads;

                //if for map
                if(isMap())
                {
                    //set map heading
                    mapControl.setHeading(rads);
                }
                else
                {
                    //set globe heading
                    globeControl.setHeading(rads);
                    geo = globeControl.getPositionGeo();
                    moveCamera(Math.toDegrees(geo.getY()), Math.toDegrees(geo.getX()), geo.getZ());
                }
            }
        }

        private double getHeadingRads()
        {
            return(getControl() != null ? (isMap() ? mapControl.getHeading() : globeControl.getGlobeView().getHeading()) : 0);
        }

        @Override
        public double getHeading()
        {
            return(Globals.normalizeAngle(Math.toDegrees(getHeadingRads())));
        }

        private void handleHeadingChanged()
        {
            double currentHeadingRads = getHeadingRads();

            //if rotation in radians changed
            if(currentHeadingRads != lastRotationRads)
            {
                //update last rotation
                lastRotationRads = currentHeadingRads;

                if(!isMap())
                {
                    //update orbitals
                    for(OrbitalBase currentOrbital : common.orbitalObjects)
                    {
                        //counteract rotation
                        currentOrbital.setRotation(-currentHeadingRads);
                    }
                }

                //send event
                common.rotated(getHeading() * (isMap() ? 1 : -1));
            }
        }

        @Override
        public void setInfoLocation(int location)
        {
            common.setInfoLocation(location);
        }

        @Override
        public void setMarkerScale(float markerScaling)
        {
            common.setMarkerScale(markerScaling);
        }

        @Override
        public void setMarkerShowBackground(boolean show)
        {
            common.setShowBackground(show);
        }

        @Override
        public void setMarkerShowPin(boolean show)
        {
            common.setShowPin(show);
        }

        @Override
        public void setMarkerShowShadow(boolean show)
        {
            common.setShowShadow(show);
        }

        @Override
        public void setShowSunlight(boolean show)
        {
            common.setShowSunlight(show);
        }

        @Override
        public void setShowOrbitalDirection(boolean show)
        {
            common.setShowOrbitalDirection(show);
        }

        @Override
        public void setShowOrbitalDirectionLimitCount(int limitCount)
        {
            common.setOrbitalDirectionLimitCount(limitCount);
        }

        @Override
        public void setShowTitlesAlways(boolean show)
        {
            common.setShowTitlesAlways(show);
        }

        @Override
        public void setZoom(double zoom)
        {
            double maxZoom = 1;
            Point3d currentPosition = null;

            if(getControl() != null)
            {
                //get current position
                if(isMap())
                {
                    currentPosition = mapControl.getPositionGeo();
                    maxZoom = MaxMapZoom;
                }
                else if(globeControl.getGlobeView() != null)
                {
                    currentPosition = globeControl.getGlobeView().getLoc();
                    maxZoom = MaxGlobeZoom;
                }

                //if got current position
                if(currentPosition != null)
                {
                    //keep zoom within range
                    if(zoom < MinZoom)
                    {
                        zoom = MinZoom;
                    }
                    else if(zoom > maxZoom)
                    {
                        zoom = maxZoom;
                    }

                    //update zoom
                    moveCamera(Math.toDegrees(currentPosition.getY()), Math.toDegrees(currentPosition.getX()), zoom);
                }
            }
        }

        @Override
        public MarkerObject addMarker(Context context, int noradId, Calculations.ObserverType markerLocation)
        {
            MarkerObject newMarker = (getControl() != null ? new MarkerObject(context, getControl(), noradId, null, null, markerLocation, common.getMarkerScale(), common.getShowBackground(), common.getShowTitleAlways(), true, common.getInfoLocation(), false) : null);

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

        //Set using direction for all orbitals
        private void setUsingDirection(boolean using)
        {
            //go through each orbital
            for(OrbitalBase currentObject : common.orbitalObjects)
            {
                //set using direction
                currentObject.setUsingDirection(using);
            }
        }

        @Override
        public OrbitalObject addOrbital(Context context, Database.SatelliteData orbitalData, Calculations.ObserverType observerLocation)
        {
            int currentCount = common.getOrbitalCount();
            int limitCount = common.getOrbitalDirectionLimitCount();
            boolean useDirection = common.getShowOrbitalDirection();
            boolean overLimit = (useDirection && (limitCount > 0) && (currentCount + 1 > limitCount));
            OrbitalObject newObject = (getControl() != null ? new OrbitalObject(context, getControl(), orbitalData, observerLocation, common.getMarkerScale(), common.getShowBackground(), (useDirection && !overLimit), common.getShowPin(), common.getShowShadow(), common.getShowTitleAlways(), common.getInfoLocation()) : null);

            //if able to create object
            if(newObject != null)
            {
                //if currently at limit to show direction
                if(overLimit && (currentCount == limitCount))
                {
                    //remove direction from all previous
                    setUsingDirection(false);
                }

                //add it
                common.orbitalObjects.add(newObject);
            }

            return(newObject);
        }

        @Override
        public void removeOrbital(OrbitalBase object)
        {
            int limitCount = common.getOrbitalDirectionLimitCount();
            boolean useDirection = common.getShowOrbitalDirection();

            //remove object
            object.remove();
            common.orbitalObjects.remove(object);

            //if now at limit to show direction
            if(useDirection && (limitCount > 0) && (common.getOrbitalCount() == limitCount))
            {
                //add direction to all current
                setUsingDirection(true);
            }
        }

        @Override
        public int getSelectedNoradId()
        {
            return(common.selectedNoradId);
        }

        @Override
        public OrbitalBase getSelectedOrbital()
        {
            return(common.getOrbital(common.selectedOrbitalIndex));
        }

        @Override
        public void selectOrbital(int noradId)
        {
            //update selected
            common.selectCurrent(noradId);
        }

        @Override
        public void deselectCurrent()
        {
            //deselect current
            common.deselectCurrent();
        }

        @Override
        public boolean getFollowSelected()
        {
            return(common.followSelected);
        }

        @Override
        public void setFollowSelected(boolean follow)
        {
            //update following selected
            common.setFollowSelected(follow);
        }

        @Override
        public void removeOrbitals()
        {
            common.removeOrbitals();
        }
    }

    public static class MapFragment extends GlobeMapBase implements CoordinatesFragment, MapController.GestureDelegate
    {
        @Override
        protected GlobeMapFragment.MapDisplayType chooseDisplayType()
        {
            return(GlobeMapFragment.MapDisplayType.Map);
        }

        @Override
        public void userDidSelect(MapController mapControl, SelectedObject[] selectedObjects, Point2d loc, Point2d screenLoc)
        {
            //go through selected objects
            for(SelectedObject currentObject : selectedObjects)
            {
                //if a screen marker
                if(currentObject.selObj instanceof ScreenMarker)
                {
                    //get screen marker
                    ScreenMarker currentMarker = (ScreenMarker)currentObject.selObj;

                    //if user object is set
                    if(currentMarker.userObject != null)
                    {
                        //select norad ID
                        common.itemSelectionChanged((int)currentMarker.userObject, true);
                    }
                }
            }
        }
    }

    public static class GlobeFragment extends GlobeMapBase implements CoordinatesFragment, GlobeController.GestureDelegate
    {
        @Override
        protected GlobeMapFragment.MapDisplayType chooseDisplayType()
        {
            return(GlobeMapFragment.MapDisplayType.Globe);
        }
    }

    public static class PreviewFragment extends GlobeFragment
    {
        public void setMapLayerType(int mapLayerType)
        {
            Bundle args = this.getArguments();

            if(args == null)
            {
                args = new Bundle();
            }
            args.putInt(ParamTypes.MapLayerType, mapLayerType);
            this.setArguments(args);
        }
    }

    //Gets z value in meters scaled to display
    static double getScaledZMeters(double altitudeKm, boolean allowLimit)
    {
        boolean negativeAltitudeKm = (altitudeKm < 0);
        double boundaryPercent;
        double z = (negativeAltitudeKm ? 0 : (altitudeKm * 1000 * CoordinatesFragment.WhirlyZScale));

        //if not negative altitude
        if(!negativeAltitudeKm)
        {
            //if within satellite boundary
            if(z < CoordinatesFragment.SatelliteBoundaryMeters)
            {
                boundaryPercent = 1 - ((CoordinatesFragment.SatelliteBoundaryMeters - z) / CoordinatesFragment.SatelliteBoundaryMeters);
                z = boundaryPercent * CoordinatesFragment.SatelliteBoundaryRangePercent;
            }
            //else if within moon boundary
            else if(z < CoordinatesFragment.MoonSpaceBoundaryMeters)
            {
                boundaryPercent = 1 - ((CoordinatesFragment.MoonSpaceBoundaryMeters - z) / CoordinatesFragment.MoonSpaceBoundaryMeters);
                z = (boundaryPercent * CoordinatesFragment.MoonSpaceBoundaryRangePercent) + CoordinatesFragment.MoonSpaceBoundaryOffsetPercent;
            }
            //else deep space
            else
            {
                boundaryPercent = 1 - ((CoordinatesFragment.SolarSystemBoundaryMeters - z) / CoordinatesFragment.SolarSystemBoundaryMeters);
                z = (boundaryPercent * CoordinatesFragment.SolarSystemBoundaryRangePercent) + CoordinatesFragment.SolarSystemBoundaryOffsetPercent;
            }
        }

        //if allowing limit
        if(allowLimit)
        {
            //keep within draw range
            if(z < CoordinatesFragment.MinDrawDistanceMeters)
            {
                z = CoordinatesFragment.MinDrawDistanceMeters;
            }
            else if(z > CoordinatesFragment.MaxDrawDistanceMeters)
            {
                z = CoordinatesFragment.MaxDrawDistanceMeters;
            }
        }

        //return scaled z
        return(z);
    }

    //Gets z value in earth radius percent scaled to display
    static double getScaledZEarthRadiusPercent(double altitudeKm)
    {
        //return radius percent
        return(getScaledZMeters(altitudeKm, true) / (CoordinatesFragment.WhirlyEarthRadiusKm * 1000));
    }
}
