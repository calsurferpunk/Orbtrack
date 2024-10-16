package com.nikolaiapps.orbtrack;


import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.VectorDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.NotificationManagerCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.LocaleList;
import android.provider.OpenableColumns;
import android.text.Html;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


public abstract class Globals
{
    static final CookieManager cookies = new CookieManager("space-track.org");

    //Progress types
    public static abstract class ProgressType
    {
        static final int Unknown = 0;
        static final int Started = 1;
        static final int Running = 2;
        static final int Failed = 3;
        static final int Success = 4;
        static final int Finished = 5;
        static final int Cancelled = 6;
        static final int Denied = 7;
    }

    //Permission types
    public static abstract class PermissionType
    {
        //static final byte LocationEnable = 0;
        static final byte Location = 1;
        static final byte LocationRetry = 2;
        static final byte Camera = 3;
        static final byte CameraRetry = 4;
        static final byte ReadExternalStorage = 5;
        static final byte ReadExternalStorageRetry = 6;
        static final byte WriteExternalStorage = 7;
        static final byte WriteExternalStorageRetry = 8;
        static final byte ExactAlarm = 9;
        static final byte ExactAlarmRetry = 10;
        static final byte PostNotifications = 11;
        static final byte PostNotificationsRetry = 12;
    }

    //Symbols
    public static abstract class Symbols
    {
        static final String Up = "▲";
        static final String Down = "▼";
        static final String Elevating = "◢";
        static final String Time = "\uD83D\uDD54";
        static final String Star = "★";
        static final String Lock = "\uD83D\uDD12";
        static final String Degree = "°";
    }

    //Strings
    public static abstract class Strings
    {
        static final String N2YO = "N2YO";
        static final String SpaceTrack = "Space-Track";
        static final String Celestrak = "Celestrak";
        static final String HeavensAbove = "Heavens Above";
    }

    //Encoding
    public static abstract class Encoding
    {
        static final String UTF8 =  "UTF-8";    //note: can't use standard charset since not always available
        static final String UTF16 = "UTF-16";   //" "
        static final String UTF32 = "UTF-32";   //note: not guaranteed to exist for all platforms
    }

    //Account types
    public static abstract class AccountType
    {
        static final int None = 0;
        static final int GoogleDrive = 1;
        static final int Dropbox = 2;
        static final int SpaceTrack = 3;
        static final int Count = 4;
    }

    //File location types
    public static abstract class FileLocationType
    {
        static final String GoogleDrive = "Google Drive™";
        static final String Dropbox = "Dropbox™";
    }

    //File source types
    public static abstract class FileSource
    {
        static final int SDCard = 0;
        static final int GoogleDrive = 1;
        static final int Dropbox = 2;
        static final int Others = 3;
    }

    //File extension types
    public static abstract class FileExtensionType
    {
        static final String CSV = ".csv";
        static final String TXT = ".txt";
        static final String JSON = ".json";
        static final String TLE = ".tle";
    }

    //File types
    public static abstract class FileType
    {
        static final int TLEs = 0;
        static final int Backup = 1;
    }

    //Languages
    public static abstract class Languages
    {
        static final String English = new Locale("en").getLanguage();
        static final String Spanish = new Locale("es").getLanguage();
    }

    //Foreground types
    public static abstract class ForegroundServiceType
    {
        static final int DATA_SYNC = (Build.VERSION.SDK_INT >= 29 ? FOREGROUND_SERVICE_TYPE_DATA_SYNC : 1);
        static final int LOCATION = (Build.VERSION.SDK_INT >= 29 ? FOREGROUND_SERVICE_TYPE_LOCATION : 8);
        static final int SPECIAL_USE = (Build.VERSION.SDK_INT >= 34 ? FOREGROUND_SERVICE_TYPE_SPECIAL_USE : 1073741824);
    }

    //Channel IDs
    public static abstract class ChannelIds
    {
        static final int Calculate = 1;
        static final int Update = 2;
        static final int Address = 3;
        static final int Location = 4;
        static final int Pass = 5;
    }

    //Notify types
    public static abstract class NotifyType
    {
        static final byte PassStart = 0;
        static final byte PassEnd = 1;
        static final byte FullMoonStart = 2;
        static final byte FullMoonEnd = 3;
        static final byte NotifyCount = 4;
    }

    //Sub page type
    public static abstract class SubPageType
    {
        static final int Input = 0;
        static final int List = 1;
        static final int CameraLens = 2;
        static final int VirtualLens = 3;
        static final int Map = 4;
        static final int Globe = 5;
    }

    //Bool object
    public static class BoolObject
    {
        public boolean value;

        public BoolObject(boolean set)
        {
            value = set;
        }
    }

    //Long object
    public static class LongObject
    {
        public long value;

        public LongObject(long set)
        {
            value = set;
        }
    }

    //Double object
    public static class DoubleObject
    {
        public double value;

        public DoubleObject(double set)
        {
            value = set;
        }
    }

    //Web page data
    public static class WebPageData
    {
        int responseCode;
        Response connection;
        String pageData;

        public WebPageData(Response https, String data, int rspCode)
        {
            responseCode = rspCode;
            connection = https;
            pageData = data;
        }

        public boolean isOkay()
        {
            return(responseCode >= 200 && responseCode <= 299);
        }

        public boolean isDenied()
        {
            return(responseCode >= 400 && responseCode <= 499);
        }

        public boolean isLoginError()
        {
            return(!isOkay() || isDenied() || pageData == null || pageData.contains("\"Login\":\"Failed\""));
        }

        public boolean gotData()
        {
            return(pageData != null && !pageData.isEmpty());
        }
    }

    //Pending file
    public static class PendingFile
    {
        public final int type;
        public final int fileSourceType;
        public String name;
        public final String extension;
        public Uri outUri;

        public PendingFile(Uri out, String nm, String ext, int typ, int flSrcType)
        {
            outUri = out;
            name = nm;
            extension = ext;
            type = typ;
            fileSourceType = flSrcType;

            //if file does not end with extension
            if(extension != null && !extension.isEmpty() && !name.endsWith(extension))
            {
                //add extension
                name += extension;
            }
        }
    }

    //Progress Listener
    public interface OnProgressChangedListener
    {
        void onProgressChanged(int progressType, String section, long updateIndex, long updateCount);
    }

    //Deny listener
    public interface OnDenyListener
    {
        void OnDeny(byte resultCode);
    }

    //Web page listener
    public interface WebPageListener
    {
        void onResult(WebPageData pageData, boolean success);
    }

    //Login task
    private static class LoginTask extends ThreadTask<Object, Void, Void>
    {
        private boolean canceled;
        private final boolean createOnly;
        private final boolean retrying;
        private final WebPageListener loginListener;

        public LoginTask(boolean createAccountOnly, boolean retryingAccount, WebPageListener listener)
        {
            createOnly = createAccountOnly;
            retrying = retryingAccount;
            loginListener = listener;
        }

        @Override
        protected Void doInBackground(@NonNull Object... objects)
        {
            final Activity context = (Activity)objects[0];
            final int accountType = (int)objects[1];
            final int updateType = (Integer)objects[2];
            final String user = (String)objects[3];
            final String password = (String)objects[4];
            final boolean alwaysShow = (boolean)objects[5];
            final Resources res = context.getResources();
            final boolean forSatellites = (updateType == UpdateService.UpdateType.UpdateSatellites);
            final boolean forSpaceTrack = (accountType == AccountType.SpaceTrack);
            final String title = res.getString(forSpaceTrack && alwaysShow ? R.string.title_login : forSatellites ? R.string.title_update : R.string.title_list_update);
            final String[] createLink = (forSpaceTrack ? new String[]{context.getResources().getString(R.string.spacetrack_create_link)} : null);
            final WebPageData loginData = (alwaysShow || !forSpaceTrack ? null : loginSpaceTrack(user, password));

            //not canceled yet
            canceled = false;

            //if always showing or login failed
            if(alwaysShow || loginData == null || loginData.isLoginError())
            {
                //run on activity thread
                context.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        //show login data and retry
                        new EditValuesDialog(context, new EditValuesDialog.OnSaveListener()
                        {
                            @Override
                            public void onSave(EditValuesDialog dialog, int itemIndex, int id, String textValue, String text2Value, double number1, double number2, double number3, String listValue, String list2Value, long dateValue, int color1, int color2, boolean visible)
                            {
                                int index;

                                //if space-track
                                if(list2Value == null || list2Value.equals(Settings.Options.Sources.SpaceTrack))
                                {
                                    //update login
                                    Settings.setSpaceTrackLogin(context, textValue, text2Value);
                                }

                                //if setting source and it is valid
                                index = (list2Value == null ? -1 : Arrays.asList(Settings.Options.Updates.SatelliteSourceItems).indexOf(list2Value));
                                if(index >= 0 && index < Settings.Options.Updates.SatelliteSourceValues.length)
                                {
                                    //update source
                                    Settings.setSatelliteCatalogSource(context, Settings.Options.Updates.SatelliteSourceValues[index]);
                                }
                            }
                        },
                        new EditValuesDialog.OnDismissListener()
                        {
                            @Override
                            public void onDismiss(EditValuesDialog dialog, int saveCount)
                            {
                                //remember source
                                int source = (forSatellites ? Settings.getSatelliteDataSource(context) : Settings.getSatelliteCatalogSource(context));

                                //if listener is set and not canceled
                                if(loginListener != null && !canceled)
                                {
                                    //send any login data and success --if creating only- or -retrying- or -source not space-track--
                                    loginListener.onResult(loginData, (createOnly || retrying || (source != Database.UpdateSource.SpaceTrack)));
                                }
                            }
                        },
                        new EditValuesDialog.OnCancelListener()
                        {
                            @Override
                            public void onCancel(EditValuesDialog dialog)
                            {
                                //if listener is set
                                if(loginListener != null)
                                {
                                    //send cancellation
                                    canceled = true;
                                    loginListener.onResult(null, false);
                                }
                            }
                        }).getLogin(title, res.getString(R.string.title_username), new String[]{user}, res.getString(R.string.title_password), new String[]{password}, createLink, null, (createOnly ? null : res.getString(R.string.title_source)));
                    }
                });
            }
            else
            {
                //if listener is set
                if(loginListener != null)
                {
                    //send success
                    loginListener.onResult(loginData, true);
                }
            }

            return(null);
        }
    }

    //Translate listener
    public interface TranslateListener
    {
        void onTranslate(String text, String toLanguage, boolean success);
    }

    //Translate task
    private static class TranslateTask extends ThreadTask<Object, Void, Void>
    {
        private final TranslateListener translateListener;
        private static final String English = new Locale("en").getLanguage();
        private static final String TranslationResult = "translations";
        private static final String TextResult = "text";
        private static final String PostBody = "[{\"" + TextResult + "\":\"" + "%s" + "\"}]";
        private static final String ResultBody = "[{\"" + TranslationResult + "\":" + PostBody + "}]";

        public TranslateTask(TranslateListener listener)
        {
            translateListener = listener;
        }

        //Gets translated web page
        private static WebPageData getTranslatedWebPage(String outString, String toLanguage, String authToken)
        {
            int breakIndex;
            int breakLength;
            int currentIndex;
            int outStringLength;
            String contentType = "application/json";
            String breakParagraph = TextUtils.htmlEncode("</p>");
            String urlString = String.format("https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=%s&to=%s&textType=html", English, toLanguage);
            String outStringLower = outString.toLowerCase();
            StringBuilder result;
            WebPageData currentData;
            WebPageData fullData;

            //get length
            outStringLength = outString.length();

            //if translation will be too long
            if(outStringLength > 2500)
            {
                //start data and look at desired minimum break
                fullData = new WebPageData(null, "", 200);
                result = new StringBuilder();
                currentIndex = 0;

                //while data is okay and more to break
                while(fullData.isOkay() && currentIndex < outStringLength)
                {
                    //if under size for next break
                    if(currentIndex + 2500 < outStringLength)
                    {
                        //find next break
                        breakIndex = outStringLower.indexOf(breakParagraph, currentIndex + 2500);
                        breakLength = breakParagraph.length();

                        //if break not found or too long
                        if(breakIndex < 0 || (breakIndex - (currentIndex + 2500)) > 2500)
                        {
                            //look for another type of break
                            breakIndex = outStringLower.indexOf(" ", currentIndex + 2500);
                            breakLength = 1;

                            //if break still not found or too long
                            if(breakIndex < 0 || (breakIndex - (currentIndex + 2500)) > 2500)
                            {
                                //set to maximum
                                //note: might be mid word
                                breakIndex = currentIndex + 2500;
                                if(breakIndex >= outStringLength)
                                {
                                    breakIndex = outStringLength - 1;
                                }
                            }
                        }
                    }
                    else
                    {
                        //end break at length and stop
                        breakIndex = outStringLength - 1;
                        breakLength = 1;
                    }

                    //try up to 3 times to get section
                    currentData = getWebPage(urlString, null, null, String.format(PostBody, outString.substring(currentIndex, breakIndex)), authToken, contentType, null, 1, 3);
                    fullData.responseCode = currentData.responseCode;
                    fullData.connection = currentData.connection;

                    //if data is okay
                    if(currentData.isOkay())
                    {
                        try
                        {
                            //update data
                            result.append(TextUtils.htmlEncode(new JSONArray(currentData.pageData).getJSONObject(0).getJSONArray(TranslationResult).getJSONObject(0).getString(TextResult)));
                        }
                        catch(Exception ex)
                        {
                            //error
                            fullData.responseCode = 400;
                        }
                    }

                    //go to next section
                    currentIndex = breakIndex + breakLength;
                }

                //if successful
                if(fullData.isOkay())
                {
                    //format in json again
                    fullData.pageData = String.format(ResultBody, result);
                }
            }
            else
            {
                //try up to 3 times to get web page
                fullData = getWebPage(urlString, null, null, String.format(PostBody, outString), authToken, contentType, null, 1, 3);
            }

            //return data
            return(fullData);
        }

        @Override
        protected Void doInBackground(Object... objects)
        {
            Context context = (Context)objects[0];
            boolean success = false;
            boolean allowInformationTranslate = Settings.getTranslateInformation(context);
            String resultString = null;
            String toLanguage = getLanguage(context);
            String fromString = (String)objects[1];
            String encodedFromString = TextUtils.htmlEncode(fromString);
            Resources res = context.getResources();
            WebPageData data;

            //if listener is set
            if(translateListener != null)
            {
                //if not translating (already in English or not allowed)
                if(toLanguage.equals(English) || !allowInformationTranslate)
                {
                    //already have result
                    resultString = fromString;
                    toLanguage = English;
                    success = true;
                }
                else
                {
                    //get data
                    data = getTranslatedWebPage(encodedFromString, toLanguage, res.getString(R.string.azure_translator_api_key));

                    //if got response
                    if(data.isOkay())
                    {
                        //try to parse result
                        try
                        {
                            //update data
                            resultString = decodeHtml(new JSONArray(data.pageData).getJSONObject(0).getJSONArray(TranslationResult).getJSONObject(0).getString(TextResult));
                            success = true;
                        }
                        catch(Exception ex)
                        {
                            //do nothing
                        }
                    }
                }

                //send result
                translateListener.onTranslate((success ? resultString : fromString), toLanguage, success);
            }

            //done
            return(null);
        }
    }

    //Constants
    private static final int ZIP_FILE_MAX_DEPTH = 4;
    public static final int WEB_READ_SIZE = 1024;
    public static final long UNKNOWN_DATE_MS = 0;
    public static final long INVALID_DATE_MS = -1;
    private static final double MILES_PER_KM = 0.62137119223733;
    public static final double FEET_PER_METER = 3.280839895;
    public static final String COORDINATE_SEPARATOR = "   ";
    public static final TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");
    private static final double DaysPerYear = 365.25;
    private static final DateFormat shortDateFormatter = DateFormat.getDateInstance(DateFormat.SHORT);
    private static final DateFormat shortTimeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT);
    private static final DateFormat shortDateTimeFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    private static final DateFormat shortDateLongTimeFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
    private static final DateFormat mediumTimeFormatter = DateFormat.getTimeInstance(DateFormat.MEDIUM);
    private static final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static ArrayList<TimeZone> timeZoneList = null;
    private static final int[] fileSources = new int[]{FileSource.SDCard, FileSource.GoogleDrive, FileSource.Dropbox, FileSource.Others};
    public static final int[] fileSourceImageIds = new int[]{Build.VERSION.SDK_INT >= 29 ? -1 : R.drawable.ic_sd_card_black, R.drawable.org_google_drive, R.drawable.org_dropbox, R.drawable.ic_folder_open_black};
    public static final String[] fileDataExtensions = new String[]{".tle", ".json", ".txt"};

    //Variables
    public static boolean canAskLocationPermission = true;
    public static boolean canAskCameraPermission = true;
    public static boolean canAskWritePermission = true;
    public static boolean canAskReadPermission = true;
    public static boolean canAskExactAlarmPermission = true;
    public static boolean canAskPostNotificationsPermission = true;

    //Gets unknown string
    public static String getUnknownString(Context context)
    {
        return(context != null ? context.getResources().getString(R.string.title_unknown) : null);
    }

    //Gets file locations
    public static String[] getFileLocations(Context context)
    {
        Resources res = context.getResources();
        return new String[]{Build.VERSION.SDK_INT >= 29 ? null : res.getString(R.string.title_downloads), FileLocationType.GoogleDrive, FileLocationType.Dropbox, res.getString(R.string.title_other)};
    }

    //Show others folder browser
    public static void showOthersFolderSelect(ActivityResultLauncher<Intent> launcher)
    {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(launcher, intent, BaseInputActivity.RequestCode.OthersSave);
    }

    //Shows a confirm dialog
    public static Button[] showConfirmDialog(Context context, Drawable icon, View dialogView, String titleText, String messageText, String positiveText, String negativeText, String neutralText, Boolean canCancel, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener, DialogInterface.OnClickListener neutralListener, DialogInterface.OnDismissListener dismissListener)
    {
        ImageView iconView;
        AlertDialog confirmDialog;
        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(context, getDialogThemeId(context));

        if(icon != null)
        {
            confirmBuilder.setIcon(icon);
        }
        confirmBuilder.setCancelable(canCancel);
        if(dialogView != null)
        {
            confirmBuilder.setView(dialogView);
        }
        confirmBuilder.setTitle(titleText);
        if(messageText != null)
        {
            confirmBuilder.setMessage(messageText);
        }
        confirmBuilder.setPositiveButton(positiveText, positiveListener);
        if(negativeText != null)
        {
            confirmBuilder.setNegativeButton(negativeText, negativeListener);
        }
        confirmBuilder.setOnDismissListener(dismissListener);
        if(neutralText != null)
        {
            confirmBuilder.setNeutralButton(neutralText, neutralListener);
        }
        confirmDialog = confirmBuilder.create();
        confirmDialog.show();

        iconView = confirmDialog.findViewById(android.R.id.icon);
        if(iconView != null && !(icon instanceof VectorDrawable) && !(icon instanceof VectorDrawableCompat))
        {
            iconView.setImageTintList(null);
        }

        return(new Button[]{confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE), confirmDialog.getButton(AlertDialog.BUTTON_NEUTRAL), confirmDialog.getButton(AlertDialog.BUTTON_NEGATIVE)});
    }
    public static void showConfirmDialog(Context context, Drawable icon, String titleText, String messageText, String positiveText, String negativeText, Boolean canCancel, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener, DialogInterface.OnDismissListener dismissListener)
    {
        showConfirmDialog(context, icon, null, titleText, messageText, positiveText, negativeText, null, canCancel, positiveListener, negativeListener, null, dismissListener);
    }
    public static void showConfirmDialog(Context context, String titleText, String messageText, String positiveText, String negativeText, Boolean canCancel, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener, DialogInterface.OnDismissListener dismissListener)
    {
        showConfirmDialog(context, null, titleText, messageText, positiveText, negativeText, canCancel, positiveListener, negativeListener, dismissListener);
    }

    //Shows a notification dialog
    public static void showNotificationDialog(Context context, Drawable icon, String[] titleStrings, String[] messageStrings, int positiveStringId, int negativeStringId, boolean canCancel, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener, boolean adjustHeight)
    {
        float[] dpPixels = dpsToPixels(context, 15, 5, 364);
        int viewPadding = (int)dpPixels[0];
        int scrollPadding = (int)dpPixels[1];
        int height = (int)dpPixels[2];
        int titleCount = (titleStrings != null ? titleStrings.length : 0);
        int messageCount = (messageStrings != null ? messageStrings.length : 0);
        Window dialogWindow;
        TextView messageView;
        ScrollView messageScroll;
        AlertDialog confirmDialog;
        AlertDialog.Builder confirmBuilder;

        //if no context or messages
        if(context == null || titleCount == 0 || messageCount == 0)
        {
            //stop
            return;
        }

        messageView = new TextView(context);
        messageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        messageView.setPadding(viewPadding, viewPadding, viewPadding, viewPadding);
        messageView.setMovementMethod(LinkMovementMethod.getInstance());
        messageView.setBackgroundColor(resolveColorID(context, android.R.attr.colorBackground));
        messageView.setText(messageStrings[0]);
        messageView.setTextColor(resolveColorID(context, android.R.attr.textColor));

        messageScroll = new ScrollView(context);
        messageScroll.setPadding(scrollPadding, scrollPadding, scrollPadding, scrollPadding);
        messageScroll.setScrollbarFadingEnabled(false);
        messageScroll.addView(messageView);

        confirmBuilder = new AlertDialog.Builder(context, getDialogThemeId(context));
        confirmBuilder.setCancelable(canCancel);
        if(icon != null)
        {
            confirmBuilder.setIcon(icon);
        }
        confirmBuilder.setView(messageScroll);
        confirmBuilder.setTitle(titleStrings[0]);
        confirmBuilder.setPositiveButton((messageCount > 1 ? R.string.title_next : positiveStringId), null);
        if(negativeStringId != -1)
        {
            confirmBuilder.setNegativeButton(negativeStringId, negativeListener);
        }
        confirmDialog = confirmBuilder.create();
        confirmDialog.show();

        confirmDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            int notificationIndex = 0;

            @Override
            public void onClick(View v)
            {
                //if listener is set
                if(positiveListener != null)
                {
                    //call it
                    positiveListener.onClick(confirmDialog, notificationIndex);
                }

                //go to next notification
                notificationIndex++;

                //if there are more titles
                if(notificationIndex < titleCount)
                {
                    //set to next title
                    confirmDialog.setTitle(titleStrings[notificationIndex]);
                }

                //if there are more messages
                if(notificationIndex < messageCount)
                {
                    //set to next message
                    messageView.setText(messageStrings[notificationIndex]);

                    //if the last notification
                    if(notificationIndex + 1 >= messageCount)
                    {
                        //update button
                        confirmDialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(positiveStringId);
                    }
                }
                else
                {
                    //close dialog
                    confirmDialog.dismiss();
                }
            }
        });

        if(adjustHeight)
        {
            dialogWindow = confirmDialog.getWindow();
            dialogWindow.setLayout(dialogWindow.getAttributes().width, height);
        }
    }
    public static void showNotificationDialog(Context context, Drawable icon, String titleString, String messageString, int positiveStringId, int negativeStringId, boolean canCancel, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener, boolean adjustHeight)
    {
        showNotificationDialog(context, icon, new String[]{titleString}, new String[]{messageString}, positiveStringId, negativeStringId, canCancel, positiveListener, negativeListener, adjustHeight);
    }

    //Shows a confirm internet dialog
    public static void showConfirmInternetDialog(final Context context, final DialogInterface.OnClickListener positiveListener)
    {
        Resources res;

        //if context is set
        if(context != null)
        {
            res = context.getResources();
            showConfirmDialog(context, getDrawable(context, R.drawable.ic_error_black, true), null, res.getString(R.string.title_no_connection), res.getString(R.string.desc_no_connection), res.getString(R.string.title_yes), res.getString(R.string.title_no), res.getString(R.string.title_always_ignore), true, positiveListener, null, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //remember not to ask anymore
                    Settings.setAskInternet(context, false);

                    //perform normal positive click
                    positiveListener.onClick(dialog, AlertDialog.BUTTON_POSITIVE);
                }
            }, null);
        }
    }

    //Shows a select dialog
    private static void showSelectDialog(Context context, int titleIconId, String title, final byte selectType, final int extra, final Integer[] itemIds, final AddSelectListAdapter.OnItemClickListener listener)
    {
        boolean usingIcon = (titleIconId >= 0);
        AddSelectListAdapter listAdapter = (itemIds != null ? new AddSelectListAdapter(context, selectType, itemIds) : new AddSelectListAdapter(context, selectType, extra));
        AlertDialog dialog;
        CustomAlertDialogBuilder selectDialog = new CustomAlertDialogBuilder(context, getDialogThemeId(context), true);

        if(usingIcon)
        {
            selectDialog.setIcon(titleIconId);
        }
        selectDialog.setTitle(title);
        selectDialog.setAdapter(listAdapter, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                //close the dialog
                dialog.dismiss();

                //if for adding accounts and item IDs exist
                if(selectType == AddSelectListAdapter.SelectType.AddAccount && itemIds != null)
                {
                    //set to account
                    which = itemIds[which];
                }

                //if for file source
                if(selectType == AddSelectListAdapter.SelectType.FileSource && Build.VERSION.SDK_INT >= 29)
                {
                    //skip SD card
                    which++;
                }

                //handle based on account type
                switch(extra)
                {
                    case AccountType.GoogleDrive:
                    case AccountType.Dropbox:
                        //add offset
                        which += 1;
                        break;
                }

                //if listener exists
                if(listener != null)
                {
                    //call listener
                    listener.onItemClick(which);
                }
            }
        });
        dialog = selectDialog.show();

        if(usingIcon)
        {
            ImageView iconView = dialog.findViewById(android.R.id.icon);
            if(iconView != null)
            {
                iconView.setColorFilter(Color.TRANSPARENT);
            }
        }
    }
    public static void showSelectDialog(Context context, String title, byte selectType, Integer[] itemIds, AddSelectListAdapter.OnItemClickListener listener)
    {
        showSelectDialog(context, -1, title, selectType, AccountType.None, itemIds, listener);
    }
    public static void showSelectDialog(Context context, int titleIconId, String title, byte selectType, int extra, AddSelectListAdapter.OnItemClickListener listener)
    {
        showSelectDialog(context, titleIconId, title, selectType, extra, null, listener);
    }
    public static void showSelectDialog(Context context, String title, byte selectType, AddSelectListAdapter.OnItemClickListener listener)
    {
        showSelectDialog(context, -1, title, selectType, AccountType.None, null, listener);
    }

    //Tries to get activity from the given context
    public static AppCompatActivity getActivity(Context context)
    {
        //try to get fragment manager
        if(context instanceof AppCompatActivity)
        {
            return(((AppCompatActivity)context));
        }
        else if(context instanceof ContextThemeWrapper)
        {
            return(getActivity(((ContextThemeWrapper)context).getBaseContext()));
        }
        else
        {
            return(null);
        }
    }

    //Tries to get fragment manager from the given context
    public static FragmentManager getFragmentManager(Context context)
    {
        //try to get fragment manager
        if(context instanceof AppCompatActivity)
        {
            return(((AppCompatActivity)context).getSupportFragmentManager());
        }
        else if(context instanceof FragmentActivity)
        {
            return(((FragmentActivity)context).getSupportFragmentManager());
        }
        else if(context instanceof ContextThemeWrapper)
        {
            return(getFragmentManager(((ContextThemeWrapper)context).getBaseContext()));
        }
        else
        {
            return(null);
        }
    }

    //Shows a date dialog
    public static void showDateDialog(Context context, Calendar date, DatePickerDialog.OnDateSetListener listener)
    {
        //if context an date are set
        if(context != null && date != null)
        {
            int themeID = Globals.getDialogThemeId(context);
            int year = date.get(Calendar.YEAR);
            int month = date.get(Calendar.MONTH);
            int dayOfMonth = date.get(Calendar.DAY_OF_MONTH);
            FragmentManager manager;
            DatePickerDialog legacyDateDialog;
            MaterialDatePicker<Long> dateDialog;

            //get fragment manager
            manager = getFragmentManager(context);
            if(manager != null)
            {
                //show dialog and stop
                dateDialog = MaterialDatePicker.Builder.datePicker().setSelection(date.getTimeInMillis()).build();
                dateDialog.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<>()
                {
                    @Override
                    public void onPositiveButtonClick(Long selection)
                    {
                        //get result in ms
                        Calendar result = Globals.getGMTTime(selection);

                        //if listener is set
                        if(listener != null)
                        {
                            //call listener
                            listener.onDateSet(null, result.get(Calendar.YEAR), result.get(Calendar.MONTH), result.get(Calendar.DAY_OF_MONTH));
                        }
                    }
                });
                dateDialog.show(manager, "DateDialog");
                return;
            }

            //show legacy dialog instead
            legacyDateDialog = new DatePickerDialog(context, themeID, listener, year, month, dayOfMonth);
            legacyDateDialog.show();
        }
    }

    //Shows a snack bar progress and returns text
    public static TextView showSnackBarProgress(View parentView, LinearProgressIndicator progressBar)
    {
        Context context = parentView.getContext();
        Snackbar snackView = Snackbar.make(parentView, "", Snackbar.LENGTH_INDEFINITE);
        View snackParentView = snackView.getView();
        TextView snackText = snackParentView.findViewById(com.google.android.material.R.id.snackbar_text);
        ViewGroup snackGroup = (ViewGroup)snackText.getParent();
        float[] sizes = dpsToPixels(context, 200, 48);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int)sizes[0], (int)sizes[1]);

        snackText.setTextColor(Color.DKGRAY);
        snackParentView.setBackgroundColor(ContextCompat.getColor(context, R.color.very_light_gray));
        params.gravity = Gravity.CENTER;
        progressBar.setLayoutParams(params);
        progressBar.setTrackColor(ContextCompat.getColor(context, R.color.light_gray));
        progressBar.setIndicatorColor(resolveColorID(context, R.attr.colorAccent));
        snackGroup.addView(progressBar, 0);
        snackView.show();

        return(snackText);
    }

    //Shows a snack bar
    public static void showSnackBar(final View parentView, final String message, final String detailMessage, int imageId, boolean isError, final boolean closeOnView, int positiveTextId, int negativeTextId, DialogInterface.OnClickListener positiveListener, DialogInterface.OnDismissListener dismissListener)
    {
        //if parent view is not set
        if(parentView == null)
        {
            //stop
            return;
        }

        final Context context = parentView.getContext();
        boolean usingDetail = (detailMessage != null);
        boolean usingImagesOnly = (imageId != -1 && message == null && detailMessage == null);
        int resId = (isError ? R.drawable.ic_error_black : R.drawable.ic_check_circle_black);
        int imageColor = resolveAttributeID(context, android.R.attr.textColor);
        int textColor = resolveColorID(context, R.attr.titleTextColor);
        final Drawable icon = getDrawable(context, resId, true);
        final Drawable smallIcon = (usingImagesOnly ? null : getDrawableSized(context, resId, 16, 16, R.color.white, true));
        final Drawable imageIcon = (usingImagesOnly ? getDrawableSized(context, imageId, 12, 12, imageColor, true) : null);
        final Snackbar snackView = Snackbar.make(parentView, (message == null ? "" : message), usingDetail ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
        final View snackParentView = snackView.getView();
        final TextView snackText = snackParentView.findViewById(com.google.android.material.R.id.snackbar_text);
        final Resources res = context.getResources();

        //setup views
        if(usingImagesOnly)
        {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)snackParentView.getLayoutParams();

            snackParentView.setBackgroundColor(Color.TRANSPARENT);
            layoutParams.width = FrameLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
            snackParentView.setLayoutParams(layoutParams);
        }
        else
        {
            snackView.setBackgroundTint(resolveColorID(context, R.attr.colorAccentDark));
        }
        snackText.setTextColor(textColor);
        snackView.setActionTextColor(textColor);
        snackText.setCompoundDrawablePadding((int)dpToPixels(context, 3));
        snackText.setCompoundDrawablesWithIntrinsicBounds((usingImagesOnly ? imageIcon : smallIcon), null, null, null);

        //if using detail message
        if(usingDetail)
        {
            //set view action
            snackView.setAction(res.getString(R.string.title_view), new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    showConfirmDialog(context, icon, message, detailMessage, (positiveTextId != - 1 ? res.getString(positiveTextId) : null), (negativeTextId != -1 ? res.getString(negativeTextId) : null), true, positiveListener, null, new DialogInterface.OnDismissListener()
                    {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface)
                        {
                            if(dismissListener != null)
                            {
                                dismissListener.onDismiss(dialogInterface);
                            }

                            if(!closeOnView)
                            {
                                snackView.show();
                            }
                        }
                    });
                }
            });
        }

        //add margins to view
        BaseInputActivity.setEdgeMargins(snackParentView, BaseInputActivity.EdgeDistance.BOTTOM_BAR);

        //show view
        snackView.show();
    }
    public static void showSnackBar(final View parentView, final String message, final String detailMessage, boolean isError, final boolean closeOnView)
    {
        showSnackBar(parentView, message, detailMessage, -1, isError, closeOnView, R.string.title_ok, -1, null, null);
    }
    public static void showSnackBar(View parentView, String message, boolean isError)
    {
        showSnackBar(parentView, message, null, isError, true);
    }
    public static void showSnackBar(View parentView, String message)
    {
        showSnackBar(parentView, message, null, false, true);
    }

    //Shows a transparent snack bar with only an image
    public static void showSnackBarTransparent(View parentView, int imageId, boolean isError)
    {
        showSnackBar(parentView, null, null, imageId, isError, true, -1, -1, null, null);
    }

    //Checks if permission is granted
    private static boolean havePermission(Context context, String permission)
    {
        if(Build.VERSION.SDK_INT >= 31 && permission.equals(Manifest.permission.SCHEDULE_EXACT_ALARM))
        {
            AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            return(manager != null && manager.canScheduleExactAlarms());
        }
        else
        {
            return(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED);
        }
    }

    //Asks for permission
    private static void askPermission(final Context context, ActivityResultLauncher<Intent> launcher, final String[] permissions, final String title, final String reason, final byte resultCode, final OnDenyListener denyRetryListener)
    {
        final boolean isRetry;
        final boolean isExactAlarm;
        final boolean isNotification;
        boolean createDialog;
        final AppCompatActivity currentActivity = getActivity(context);

        //if unable to get activity
        if(currentActivity == null)
        {
            //stop
            return;
        }

        //get properties
        switch(resultCode)
        {
            case PermissionType.CameraRetry:
            case PermissionType.LocationRetry:
            case PermissionType.ReadExternalStorageRetry:
            case PermissionType.WriteExternalStorageRetry:
                isRetry = true;
                isExactAlarm = isNotification = false;
                break;

            case PermissionType.ExactAlarm:
            case PermissionType.ExactAlarmRetry:
                isRetry = (resultCode == PermissionType.ExactAlarmRetry);
                isExactAlarm = true;
                isNotification = false;
                break;

            case PermissionType.PostNotifications:
            case PermissionType.PostNotificationsRetry:
                isRetry = (resultCode == PermissionType.PostNotificationsRetry);
                isExactAlarm = false;
                isNotification = true;
                break;

            default:
                isRetry = isExactAlarm = isNotification = false;
                break;
        }

        //determine if need to create dialog
        createDialog = (isExactAlarm || isNotification);
        for(String currentPermission : permissions)
        {
            //create dialog if need to already or current permission should
            createDialog = createDialog || ActivityCompat.shouldShowRequestPermissionRationale(currentActivity, currentPermission);
        }
        if(createDialog)
        {
            Resources res = currentActivity.getResources();

            //show dialog
            showConfirmDialog(currentActivity, title + " " + res.getString(R.string.title_permission), reason, res.getString(isRetry ? R.string.title_retry : isExactAlarm ? R.string.title_open_settings : R.string.title_ok), (isRetry ? res.getString(R.string.title_deny) : isExactAlarm ? res.getString(R.string.title_ignore) : null), false, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //if an exact alarm and being used
                    if(isExactAlarm && Build.VERSION.SDK_INT >= 31)
                    {
                        //show exact alarm settings toggle
                        Intent settingsIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivityForResult(launcher, settingsIntent, resultCode);
                    }
                    else
                    {
                        //request permission
                        ActivityCompat.requestPermissions(currentActivity, permissions, resultCode);
                    }
                }
            },
            new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int which)
                {
                    //handle cases to stop asking
                    switch(resultCode)
                    {
                        case PermissionType.CameraRetry:
                            canAskCameraPermission = false;
                            break;

                        case PermissionType.LocationRetry:
                            canAskLocationPermission = false;
                            break;

                        case PermissionType.ReadExternalStorageRetry:
                            canAskReadPermission = false;
                            break;

                        case PermissionType.WriteExternalStorageRetry:
                            canAskWritePermission = false;
                            break;

                        case PermissionType.ExactAlarmRetry:
                            canAskExactAlarmPermission = false;
                            break;

                        case PermissionType.PostNotificationsRetry:
                            canAskPostNotificationsPermission = false;
                            break;
                    }

                    // if deny retry listener is set
                    if(denyRetryListener != null)
                    {
                        //send event
                        denyRetryListener.OnDeny(resultCode);
                    }
                }
            },
            null);
        }
        else
        {
            //request permission
            ActivityCompat.requestPermissions(currentActivity, permissions, resultCode);
        }
    }
    private static void askPermission(final Context context, final String permission, final String title, final String reason, final byte resultCode, final OnDenyListener denyRetryListener)
    {
        askPermission(context, null, new String[]{permission}, title, reason, resultCode, denyRetryListener);
    }

    //Ask for camera permission
    public static void askCameraPermission(Context context, boolean retrying, OnDenyListener listener)
    {
        Resources res = context.getResources();
        askPermission(context, Manifest.permission.CAMERA, res.getString(R.string.title_camera), res.getString(R.string.desc_permission_camera), (retrying ? PermissionType.CameraRetry : PermissionType.Camera), listener);
    }

    //Ask for location enable
    public static void askLocationEnable(final Context context)
    {
        Resources res = context.getResources();

        showConfirmDialog(context, res.getString(R.string.title_location_disabled), res.getString(R.string.desc_location_disable), res.getString(R.string.title_yes), res.getString(R.string.title_no), true, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Intent enableIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(enableIntent);
            }
        }, null, null);
    }

    //Ask for location permission
    public static void askLocationPermission(Context context, boolean retrying)
    {
        Resources res = context.getResources();
        askPermission(context, null, getLocationManifestPermissions(context), res.getString(R.string.title_location), res.getString(R.string.desc_permission_location), (retrying ? PermissionType.LocationRetry : PermissionType.Location), null);
    }

    //Ask for write permission
    public static void askWritePermission(Context context, boolean retrying)
    {
        Resources res = context.getResources();
        askPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE, res.getString(R.string.title_write_external_storage), res.getString(R.string.desc_permission_write_external_storage), (retrying ? PermissionType.WriteExternalStorageRetry : PermissionType.WriteExternalStorage), null);
    }

    //Ask for read permission
    public static void askReadPermission(Context context, boolean retrying)
    {
        Resources res = context.getResources();
        askPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE, res.getString(R.string.title_read_external_storage), res.getString(R.string.desc_permission_read_external_storage), (retrying ? PermissionType.ReadExternalStorageRetry : PermissionType.ReadExternalStorage), null);
    }

    //Ask for exact alarm permission
    public static void askExactAlarmPermission(Context context, ActivityResultLauncher<Intent> launcher, boolean retrying, OnDenyListener listener)
    {
        if(Build.VERSION.SDK_INT >= 31)
        {
            Resources res = context.getResources();
            askPermission(context, launcher, new String[]{Manifest.permission.SCHEDULE_EXACT_ALARM}, res.getString(R.string.title_set_exact_alarm), res.getString(R.string.desc_permission_set_exact_alarm), (retrying ? PermissionType.ExactAlarmRetry : PermissionType.ExactAlarm), listener);
        }
    }

    //Ask for post notifications permission
    public static void askPostNotificationsPermission(Context context, boolean retrying)
    {
        if(Build.VERSION.SDK_INT >= 33)
        {
            Resources res = context.getResources();
            askPermission(context, Manifest.permission.POST_NOTIFICATIONS, res.getString(R.string.title_show_notifications), res.getString(R.string.desc_permission_post_notifications), (retrying ? PermissionType.PostNotificationsRetry : PermissionType.PostNotifications), null);
        }
    }

    //Gets if google play services are available
    public static boolean getUseGooglePlayServices(Context context, boolean showError)
    {
        int result;
        boolean errorShown = false;
        Resources res;
        Dialog errorDialog;
        GoogleApiAvailability api;

        //if context exists
        if(context != null)
        {
            //check for API availability
            api = GoogleApiAvailability.getInstance();
            result = api.isGooglePlayServicesAvailable(context);

            //if available
            if(result == ConnectionResult.SUCCESS)
            {
                //available
                return(true);
            }
            //else if showing errors
            else if(showError)
            {
                //if can show user a dialog
                if(context instanceof Activity && api.isUserResolvableError(result))
                {
                    //try to show dialog
                    try
                    {
                        errorDialog = api.getErrorDialog((Activity)context, result, 10000);
                        if(errorDialog != null)
                        {
                            errorDialog.show();
                            errorShown = true;
                        }
                    }
                    catch(Exception ex)
                    {
                        //do nothing
                    }
                }

                //if no error shown yet
                if(!errorShown)
                {
                    //show error now
                    res = context.getResources();
                    showNotificationDialog(context, getDrawable(context, R.drawable.ic_warning_black, true),  res.getString(R.string.title_failed), res.getString(R.string.desc_google_play_services_fail) + "(" + result + ")", R.string.title_ok, -1, true, null, null, false);
                }
            }
        }

        //unknown/not available
        return(false);
    }
    public static boolean getUseGooglePlayServices(Context context)
    {
        return(getUseGooglePlayServices(context, false));
    }

    //Check if have camera permission
    public static boolean haveCameraPermission(Context context)
    {
        return(havePermission(context, Manifest.permission.CAMERA));
    }

    //Check if have post notifications permission
    public static boolean havePostNotificationsPermission(Context context)
    {
        return(Build.VERSION.SDK_INT < 33 || havePermission(context, Manifest.permission.POST_NOTIFICATIONS));
    }

    //Check if location services is enabled
    public static boolean haveLocationEnabled(Context context)
    {
        try
        {
            if(Build.VERSION.SDK_INT >= 28)
            {
                LocationManager manager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
                return(manager != null && manager.isLocationEnabled());
            }
            else
            {
                //noinspection deprecation
                return(android.provider.Settings.Secure.getInt(context.getContentResolver(), android.provider.Settings.Secure.LOCATION_MODE, android.provider.Settings.Secure.LOCATION_MODE_OFF) != android.provider.Settings.Secure.LOCATION_MODE_OFF);
            }
        }
        catch(Exception ex)
        {
            return(false);
        }
    }

    private static String[] getLocationManifestPermissions(Context context)
    {
        if(Build.VERSION.SDK_INT >= 31)
        {
            return(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
        else
        {
            LocationManager manager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
            List<String> providers = (manager != null ? manager.getProviders(true) : null);

            return(new String[]{providers != null && !providers.isEmpty() && providers.contains(LocationManager.GPS_PROVIDER) ? Manifest.permission.ACCESS_FINE_LOCATION : Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    //Check if have location permission
    public static boolean haveLocationPermission(Context context)
    {
        boolean have = false;
        String[] permissions = getLocationManifestPermissions(context);

        //go through each permission
        for(String currentPermission : permissions)
        {
            //have if already have or do for current permission
            have = have || havePermission(context, currentPermission);
        }

        //return status
        return(have);
    }

    //Check if have write permission
    public static boolean haveWritePermission(Context context)
    {
        return(havePermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }

    //Check if have read permission
    public static boolean haveReadPermission(Context context)
    {
        return(havePermission(context, Manifest.permission.READ_EXTERNAL_STORAGE));
    }

    //Check if have set exact alarm permission
    public static boolean haveExactAlarmPermission(Context context)
    {
        return(Build.VERSION.SDK_INT < 31 || havePermission(context, Manifest.permission.SCHEDULE_EXACT_ALARM));
    }

    //Check if have internet connection
    private static boolean haveInternetConnection(Context context)
    {
        ConnectivityManager manager;
        NetworkInfo info = null;

        //get manager
        manager = (context != null ? (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE) : null);
        if(manager != null)
        {
            //get connection info
            info = manager.getActiveNetworkInfo();
        }

        //have connection if have connection info and connected/connecting
        return(info != null && info.isConnectedOrConnecting());
    }

    //Check if should ask for internet connection
    public static boolean shouldAskInternetConnection(Context context)
    {
        //should ask if don't have connection and can ask
        return(context != null && !haveInternetConnection(context) && Settings.getAskInternet(context));
    }

    //Show permission denied
    public static void showDenied(View parentView, String denyMessage)
    {
        showSnackBar(parentView, denyMessage, true);
    }

    //Handles adding request code to callback data
    private static void handleCallbackWithCode(ActivityResult result, ActivityResultCallback<ActivityResult> callback, byte requestCode)
    {
        Intent data;

        //if callback is set
        if(callback != null)
        {
            //get data
            data = result.getData();

            //if no data yet
            if(data == null)
            {
                //create it
                data = new Intent();
            }

            //add request code
            data.putExtra(BaseInputActivity.EXTRA_REQUEST_CODE, requestCode);

            //call callback
            callback.onActivityResult(result);
        }
    }

    //Creates an activity launcher
    public static ActivityResultLauncher<Intent> createActivityLauncher(ComponentActivity activity, ActivityResultCallback<ActivityResult> callback, byte requestCode)
    {
        return(activity != null ? activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (requestCode != BaseInputActivity.RequestCode.None ? new ActivityResultCallback<>()
        {
            @Override
            public void onActivityResult(ActivityResult result)
            {
                handleCallbackWithCode(result, callback, requestCode);
            }
        } : callback)) : null);
    }
    public static ActivityResultLauncher<Intent> createActivityLauncher(ComponentActivity activity, ActivityResultCallback<ActivityResult> callback)
    {
        return(createActivityLauncher(activity, callback, BaseInputActivity.RequestCode.None));
    }

    //Create an activity request launcher
    public static ActivityResultLauncher<IntentSenderRequest> createActivityRequestLauncher(ComponentActivity activity, ActivityResultCallback<ActivityResult> callback, byte requestCode)
    {
        return(activity != null ? activity.registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), (requestCode != BaseInputActivity.RequestCode.None ?  new ActivityResultCallback<>()
        {
            @Override
            public void onActivityResult(ActivityResult result)
            {
                handleCallbackWithCode(result, callback, requestCode);
            }
        } : callback)) : null);
    }

    //Starts an activity with the given launcher
    public static void startActivityForResult(ActivityResultLauncher<Intent> launcher, Intent intent, byte requestCode)
    {
        //if launcher and intent exist
        if(launcher != null && intent != null)
        {
            //add any request code
            BaseInputActivity.setRequestCode(intent, requestCode);

            try
            {
                //launch activity
                launcher.launch(intent);
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }
    }

    //Sets a broadcast intent value
    public static void setBroadcastValue(Context context, MutableLiveData<Intent> broadcaster, Intent value)
    {
        boolean needPost = false;

        if(context instanceof Service)
        {
            needPost = true;
        }
        else
        {
            try
            {
                broadcaster.setValue(value);
            }
            catch(Exception ex)
            {
                needPost = true;
            }
        }

        if(needPost)
        {
            broadcaster.postValue(value);
        }
    }
    public static void setBroadcastValue(MutableLiveData<Intent> broadcaster, Intent value)
    {
        setBroadcastValue(null, broadcaster, value);
    }

    //Starts a service
    public static void startService(Context context, Intent intent, boolean runForeground)
    {
        try
        {
            if(runForeground && Build.VERSION.SDK_INT >= 26)
            {
                context.startForegroundService(intent);
            }
            else
            {
                context.startService(intent);
            }
        }
        catch(Exception ex)
        {
            Log.d("startService Error", ex.getMessage());
        }
    }

    //Starts the given service in foreground if needed
    public static void startForeground(Service service, int id, NotificationCompat.Builder notifyBuilder, boolean runForeground, int foregroundType)
    {
        //if need to start in foreground and android >= 8.0
        if(runForeground && Build.VERSION.SDK_INT >= 26)
        {
            //start in foreground
            if(Build.VERSION.SDK_INT >= 29)
            {
                service.startForeground(id, notifyBuilder.setChannelId(getChannelId(id)).build(), foregroundType);
            }
            else
            {
                service.startForeground(id, notifyBuilder.setChannelId(getChannelId(id)).build());
            }
        }
    }

    //Creates a notification channel
    public static void createNotifyChannel(Context context, String notifyChannelId)
    {
        NotificationManager manager;

        //if a notification channel needs to be set
        if(Build.VERSION.SDK_INT >= 26)
        {
            //get manager and create channel
            manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(manager != null)
            {
                manager.createNotificationChannel(new NotificationChannel(notifyChannelId, notifyChannelId, (getChannelId(ChannelIds.Pass).equals(notifyChannelId) ? NotificationManager.IMPORTANCE_DEFAULT : NotificationManager.IMPORTANCE_LOW)));
            }
        }
    }

    //Creates notification builder
    public static NotificationCompat.Builder createNotifyBuilder(Context context, String notifyChannelId, int largeIconId)
    {
        return(new NotificationCompat.Builder(context, notifyChannelId).setSmallIcon(R.drawable.notify_small).setLargeIcon(BitmapFactory.decodeResource(context.getResources(), largeIconId)));
    }
    public static NotificationCompat.Builder createNotifyBuilder(Context context, String notifyChannelId)
    {
        return(createNotifyBuilder(context, notifyChannelId, R.drawable.ic_launcher_clear));
    }

    //Tries to show a notification
    public static void showNotification(Context context, int channelId, Notification notification)
    {
        boolean showError = false;
        NotificationManagerCompat notifyManager = NotificationManagerCompat.from(context);

        //if have permission to use notifications
        if(havePostNotificationsPermission(context))
        {
            try
            {
                //try to show notification
                notifyManager.notify(channelId, notification);
            }
            catch(SecurityException ex)
            {
                //show error
                showError = true;
            }
        }
        else if(canAskPostNotificationsPermission && context instanceof ContextWrapper)
        {
            //ask permission again
            askPostNotificationsPermission(((ContextWrapper)context).getBaseContext(), false);
        }
        else
        {
            //show error
            showError = true;
        }

        //if need to show error
        if(showError)
        {
            //try to show permission error
            try
            {
                Toast.makeText(context, R.string.desc_permission_post_notifications, Toast.LENGTH_SHORT).show();
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }
    }

    //Sets an exact alarm
    public static void setAlarm(Context context, AlarmManager manager, long timeMs, PendingIntent intent, boolean wakeup)
    {
        int alarmType = (wakeup ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC);

        if(haveExactAlarmPermission(context))
        {
            manager.setExact(alarmType, timeMs, intent);
        }
        else
        {
            manager.set(alarmType, timeMs, intent);
        }
    }

    //Sets text color of view and all children
    public static void setTextColor(ViewGroup group, int textColor)
    {
        int index;
        int count = group.getChildCount();

        //go through each child
        for(index = 0; index < count; index++)
        {
            //get current child
            View currentChild = group.getChildAt(index);

            //if a TextView
            if(currentChild instanceof TextView)
            {
                //set color
                ((TextView)currentChild).setTextColor(textColor);
            }
            //else if a ViewGroup
            else if(currentChild instanceof ViewGroup)
            {
                //set children
                setTextColor((LinearLayout)currentChild, textColor);
            }
        }
    }

    //Gets the color with the given alpha
    public static int getColor(int alpha, int color)
    {
        return(Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)));
    }

    //Normalizes the given RGB value (0 - 255)
    private static int normalizeRGB(int value)
    {
        return(value < 0 ? 0 : Math.min(value, 255));
    }

    //Gets the color added by the given amount
    public static int getColorAdded(int rgbDelta, int color)
    {
        int alpha = Color.alpha(color);
        int red = normalizeRGB(Color.red(color) + rgbDelta);
        int green = normalizeRGB(Color.green(color) + rgbDelta);
        int blue = normalizeRGB(Color.blue(color) + rgbDelta);

        return(Color.argb(alpha, red, green, blue));
    }

    //Sets a text view to be shadowed text
    public static void setShadowedText(TextView view)
    {
        view.setBackground(null);
        view.setTextColor(Color.WHITE);
        view.setShadowLayer(6.0f, 2, 2, Color.BLACK);
    }

    //Gets file source
    public static int getFileSource(Context context, String location)
    {
        int index = (location != null ? Arrays.asList(getFileLocations(context)).indexOf(location) : -1);

        //if a valid index
        if(index >= 0 && index < fileSources.length)
        {
            //return source at index
            return(fileSources[index]);
        }
        else
        {
            //default
            return(FileSource.SDCard);
        }
    }

    //Gets channel ID
    public static String getChannelId(int id)
    {
        String channelId;

        switch(id)
        {
            case ChannelIds.Calculate:
                channelId = "Calculate";
                break;

            case ChannelIds.Update:
                channelId = "Update";
                break;

            case ChannelIds.Address:
                channelId = "Address";
                break;

            case ChannelIds.Location:
                channelId = "Location";
                break;

            case ChannelIds.Pass:
                channelId = "Pass";
                break;

            default:
                channelId = "Unknown";
                break;
        }

        return(channelId + " Service");
    }

    //Gets a pending broadcast intent
    public static PendingIntent getPendingBroadcastIntent(Context context, int requestCode, Intent intent, int flags)
    {
        if(Build.VERSION.SDK_INT >= 31)
        {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return(PendingIntent.getBroadcast(context, requestCode, intent, flags));
    }

    //Gets a byte string
    public static String getByteString(Resources res, long bytes)
    {
        //if more than 1 MB
        if(bytes > 1048576)
        {
            //show MB
            return(String.format(Locale.US, "%3.1f " + res.getString(R.string.abbrev_megabyte), bytes / 1048576f));
        }
        //else if more than 1 KB
        else if(bytes > 1024)
        {
            //show KB
            return(String.format(Locale.US, "%3.1f " + res.getString(R.string.abbrev_kilobyte), bytes / 1024f));
        }
        else
        {
            //show bytes
            return(bytes + " " + res.getString(R.string.title_bytes));
        }
    }

    //Julian date to calendar
    @SuppressWarnings("SpellCheckingInspection")
    public static Calendar julianDateToCalendar(double julianDate, boolean roundMs)
    {
        Calendar result = getGMTTime();

        if(julianDate != Double.MAX_VALUE)
        {
            //get the date from the Julian day number
            boolean addSecond = false;
            double tmp;
            double	j1, j2, j3, j4, j5;
            double intgr = Math.floor(julianDate);
            double frac = julianDate - intgr;
            double gregjd = 2299161;
            if(intgr >= gregjd)
            {
                //Gregorian calendar correction
                tmp = Math.floor(((intgr - 1867216) - 0.25) / 36524.25);
                j1 = intgr + 1 + tmp - Math.floor(0.25 * tmp);
            }
            else
            {
                j1 = intgr;
            }

            //correction for half day offset
            double dayfrac = frac + 0.5;
            if(dayfrac >= 1.0)
            {
                dayfrac -= 1.0;
                ++j1;
            }

            j2 = j1 + 1524;
            j3 = Math.floor(6680.0 + ((j2 - 2439870) - 122.1 ) / DaysPerYear);
            j4 = Math.floor(j3 * DaysPerYear);
            j5 = Math.floor( (j2 - j4) / 30.6001);

            double d = Math.floor(j2 - j4 - Math.floor(j5 * 30.6001));
            double m = Math.floor(j5 - 1);
            if(m > 12) m -= 12;
            double y = Math.floor(j3 - 4715);
            if(m > 2) --y;
            if(y <= 0) --y;

            //
            // get time of day from day fraction
            //
            double hrFrac = dayfrac * 24.0;
            double hr  = Math.floor(hrFrac);
            double mn  = Math.floor((hrFrac - hr) * 60.0);
            double sc  = ((hrFrac - hr) * 60.0 - mn) * 60.0;

            if(y < 0)
            {
                y = -y;
            }

            while(sc >= 60)
            {
                sc -= 60;
                mn++;
            }

            double ms = (sc - Math.floor(sc)) * 1000;
            if(roundMs)
            {
                addSecond = (ms >= 500);
                ms = 0.0;
            }

            result.set((int)y, (int)m - 1, (int)d, (int)hr, (int)mn, (int)Math.floor(sc));
            result.set(Calendar.MILLISECOND, (int)ms);
            if(addSecond)
            {
                result.add(Calendar.SECOND, 1);
            }
        }
        else
        {
            result.setTimeInMillis(Globals.INVALID_DATE_MS);
        }

        return(result);
    }
    public static Calendar julianDateToCalendar(double julianDate)
    {
        return(julianDateToCalendar(julianDate, false));
    }

    //Julian date with no seconds
    public static double julianDateNoSeconds(double julianDate)
    {
        //set julian date to 0 seconds at that time
        Calendar currentDate = julianDateToCalendar(julianDate);
        currentDate.set(Calendar.SECOND, 0);
        currentDate.set(Calendar.MILLISECOND, 0);
        return(Calculations.julianDateCalendar(currentDate));
    }

    //Gets formatted local day
    public static synchronized String getLocalDayString(Context context, Calendar date, Calendar now, TimeZone zone, boolean showDayAbbrev, boolean showYear)
    {
        int index;
        boolean haveDate = (date != null);
        boolean isToday = (haveDate && now != null && date.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) && date.get(Calendar.YEAR) == now.get(Calendar.YEAR));
        long timeMs = (haveDate ? date.getTimeInMillis() : 0);
        Formatter dayFormatter;
        String valueString = "";

        //if have context
        if(context != null)
        {
            //if today and showing abbreviation
            if(isToday && showDayAbbrev)
            {
                //set today title
                valueString = context.getString(R.string.title_today);
            }
            //else if have time
            else if(timeMs != 0)
            {
                //get date and time
                dayFormatter = new Formatter(Locale.getDefault());
                valueString = (DateUtils.formatDateRange(context, dayFormatter, timeMs, timeMs, DateUtils.FORMAT_NUMERIC_DATE | (showYear ? DateUtils.FORMAT_SHOW_YEAR : DateUtils.FORMAT_NO_YEAR) | (showDayAbbrev ? (DateUtils.FORMAT_ABBREV_WEEKDAY | DateUtils.FORMAT_SHOW_WEEKDAY) : 0), zone.getID())).toString();

                //if don't want to show year
                if(!showYear)
                {
                    //if year was still added (bug on older APIs)
                    index = valueString.lastIndexOf("/");
                    if(index >= 0 && index + 3 < valueString.length())
                    {
                        //remove it
                        valueString = valueString.substring(0, index);
                    }
                }
            }
        }

        //return day string
        return(valueString);
    }
    public static synchronized String getLocalDayString(Context context, Calendar date, TimeZone zone)
    {
        return(getLocalDayString(context, date, null, zone, false, false));
    }

    //Gets formatted date
    private static synchronized String getDateString(Context context, Calendar date, TimeZone zone, boolean showSeconds, boolean showDay, boolean allowSimpleDay, boolean allowForceShowDayYear, boolean shortUnknown, boolean dayLineBreak)
    {
        boolean showDayAbbrev = false;
        boolean showSimpleDay = false;
        boolean showYear = false;
        Calendar currentDate = Calendar.getInstance(zone);
        Calendar localDate = getLocalTime(date, zone);
        boolean haveContext = (context != null);
        boolean haveLocalDate = (localDate != null);
        Resources res = (haveContext ? context.getResources() : null);
        int daysAgo = 0;
        int localDay = (haveLocalDate ? localDate.get(Calendar.DAY_OF_YEAR) : -1);
        int localYear = (haveLocalDate ? localDate.get(Calendar.YEAR) : -1);
        int currentDay = currentDate.get(Calendar.DAY_OF_YEAR);
        int currentYear = currentDate.get(Calendar.YEAR);
        long msUntilDate;
        double daysUntilMidnight;

        //if an invalid date
        if(date == null)
        {
            return(shortUnknown ? "?" : getUnknownString(context));
        }
        else
        {
            //if not always showing day and can force it
            if(!showDay && allowForceShowDayYear)
            {
                //if current date is a different day than given date
                //note: need to compare in local zone time since displaying in local zone time
                if(currentYear != localYear)
                {
                    //show day and year
                    showDay = true;
                    showYear = true;
                }
                else if(currentDay != localDay)
                {
                    //show day
                    showDay = true;
                }
            }

            //if showing day and allowing simple day
            if(showDay && allowSimpleDay)
            {
                //get ms
                msUntilDate = (haveLocalDate ? localDate.getTimeInMillis() : Long.MAX_VALUE) - currentDate.getTimeInMillis();

                //if before now
                if(msUntilDate < 0)
                {
                    //if a different day
                    if(localDate == null || currentDate.get(Calendar.DAY_OF_YEAR) != localDate.get(Calendar.DAY_OF_YEAR) || currentDate.get(Calendar.YEAR) != localDate.get(Calendar.YEAR))
                    {
                        daysUntilMidnight = 1 - (((currentDate.get(Calendar.HOUR_OF_DAY) * Calculations.SecondsPerHour) + (currentDate.get(Calendar.MINUTE) * 60) + currentDate.get(Calendar.SECOND) + (currentDate.get(Calendar.MILLISECOND) / 1000.0)) / Calculations.SecondsPerDay);
                        daysAgo = (int)((Math.abs((double)msUntilDate / Calculations.MsPerDay)) + daysUntilMidnight);
                        if(daysAgo < 1)
                        {
                            daysAgo = 1;
                        }

                        //show simple day
                        showSimpleDay = true;
                    }
                    else
                    {
                        //show day abbreviation with today
                        showDayAbbrev = true;
                    }
                }
                //else if 1 week or less between now and date
                else if(msUntilDate <= Calculations.MsPerWeek)
                {
                    //show day abbreviation
                    showDayAbbrev = true;
                }
            }

            //return for desired zone
            shortTimeFormatter.setTimeZone(zone);
            mediumTimeFormatter.setTimeZone(zone);
            return(showSimpleDay ? (res != null ? res.getQuantityString(R.plurals.title_days_ago, daysAgo, daysAgo) : "") : ((showDay && haveContext ? (getLocalDayString(context, localDate, currentDate, zone, showDayAbbrev, showYear) + (dayLineBreak ? "\r\n" : ", ")) : "") + (haveLocalDate ? (showSeconds ? mediumTimeFormatter.format(localDate.getTime()) : shortTimeFormatter.format(localDate.getTime())) : "")));
        }
    }
    public static String getDateString(Context context, Calendar date, TimeZone zone, boolean showSeconds, boolean alwaysShowDay, boolean allowSimpleDay)
    {
        return(getDateString(context, date, zone, showSeconds, alwaysShowDay, allowSimpleDay, true, false, showSeconds));
    }
    public static String getDateString(Context context, Calendar date, TimeZone zone, boolean allowSimpleDay, boolean dayLineBreak)
    {
        return(getDateString(context, date, zone, true, false, allowSimpleDay, true, false, dayLineBreak));
    }
    public static String getDateString(Context context, Calendar date, TimeZone zone, boolean showDay)
    {
        return(getDateString(context, date, zone, true, showDay, false, true, false, true));
    }

    //Gets time string
    public static String getTimeString(Context context, Calendar date, TimeZone zone, boolean shortUnknown)
    {
        return(getDateString(context, date, zone, false, false, false, false, shortUnknown, false));
    }

    //Gets formatted date and time
    public static String getDateTimeString(Context context, Calendar date, boolean showSeconds, boolean showDay, boolean allowSimpleDay)
    {
        return(getDateString(context, date, date.getTimeZone(), showSeconds, showDay, allowSimpleDay, true, false, showSeconds));
    }

    //Gets formatted date and time
    private static synchronized String getDateTimeString(Calendar date, TimeZone zone, boolean showSeconds)
    {
        Calendar localTime = getLocalTime(date, zone);

        //return for desired zone
        (showSeconds ? shortDateLongTimeFormatter : shortDateTimeFormatter).setTimeZone(zone);
        return(localTime != null ? (showSeconds ? shortDateLongTimeFormatter : shortDateTimeFormatter).format(localTime.getTime()) : "");
    }
    private static String getDateTimeString(Calendar date, TimeZone zone)
    {
        return(getDateTimeString(date, zone, false));
    }
    public static String getDateTimeString(Calendar date, boolean showSeconds)
    {
        return(getDateTimeString(date, TimeZone.getDefault(), showSeconds));
    }
    public static String getDateTimeString(Calendar date)
    {
        return(getDateTimeString(date, TimeZone.getDefault()));
    }
    private static String getDateTimeString(double julianDate, TimeZone zone)
    {
        return(getDateTimeString(julianDateToCalendar(julianDate), zone));
    }

    //Gets formatted date with year
    public static synchronized String getDateYearString(Calendar date, TimeZone zone)
    {
        //return for desired zone
        shortDateFormatter.setTimeZone(zone);
        return(shortDateFormatter.format(date.getTime()));
    }

    //Clears time in given calendar
    public static Calendar clearCalendarTime(Calendar date)
    {
        if(date != null)
        {
            date.set(Calendar.HOUR_OF_DAY, 0);
            date.set(Calendar.MINUTE, 0);
            date.set(Calendar.SECOND, 0);
            date.set(Calendar.MILLISECOND, 0);
        }

        return(date);
    }

    //Gets current gmt time
    public static Calendar getGMTTime()
    {
        return(Calendar.getInstance(gmtTimeZone));
    }
    public static Calendar getGMTTime(long dateMs)
    {
        Calendar gmtTime = getGMTTime();
        gmtTime.setTimeInMillis(dateMs);

        return(gmtTime);
    }
    public static Calendar getGMTTime(Calendar date)
    {
        return(date != null ? getGMTTime(date.getTimeInMillis()) : getGMTTime());
    }
    public static Calendar getGMTTime(long dateMs, TimeZone localZone, TimeZone wantedZone, String wantedZoneId)
    {
        return(getGMTTime(getCalendar(wantedZoneId, dateMs + getTimeZoneDifferenceMs(localZone, wantedZone, dateMs))));
    }

    //Gets current julian date
    public static double getJulianDate()
    {
        return(Calculations.julianDateCalendar(getGMTTime()));
    }

    //Converts given time to given local time zone
    public static Calendar getLocalTime(Calendar time, TimeZone zone)
    {
        Calendar localTime = Calendar.getInstance(zone);

        if(time != null)
        {
            try
            {
                localTime.setTimeInMillis(time.getTimeInMillis());     //note: set/getTimeInMillis uses UTC
                return(localTime);
            }
            catch(Exception ex)
            {
                return(null);
            }
        }
        else
        {
            return(null);
        }
    }

    //Gets a calendar created from the given time and timezone ID
    public static Calendar getCalendar(String timeZoneId, long timeMs)
    {
        Calendar result;

        if(timeMs == 0)
        {
            return(null);
        }
        else
        {
            result = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId));
            result.setTimeInMillis(timeMs);
            return(result);
        }
    }

    //Gets the current time zone offset difference in milliseconds between given zones
    private static int getTimeZoneDifferenceMs(TimeZone start, TimeZone end, long gmtMs)
    {
        return(start.getOffset(gmtMs) - end.getOffset(gmtMs));
    }

    //Gets sorted list of all time zones
    public static ArrayList<TimeZone> getTimeZoneList()
    {
        String[] zoneIds;
        ArrayList<TimeZone> zones = new ArrayList<>(0);

        //if time zone list is already set and has items
        if(timeZoneList != null && !timeZoneList.isEmpty())
        {
            //return it
            return(timeZoneList);
        }

        //go through each zone ID
        zoneIds = TimeZone.getAvailableIDs();
        for(String currentId : zoneIds)
        {
            //add zone
            zones.add(TimeZone.getTimeZone(currentId));
        }

        //sort list
        Collections.sort(zones, new Comparator<>()
        {
            @Override
            public int compare(TimeZone o1, TimeZone o2)
            {
                return(o1.getRawOffset() - o2.getRawOffset());
            }
        });

        //remember time zone list and return it
        timeZoneList = zones;
        return(timeZoneList);
    }

    //Gets time between given calendars as a string in Xh XM Xs format (or "Unknown")
    public static String getTimeBetween(Context context, Calendar date1, Calendar date2)
    {
        int days;
        int hours;
        int minutes;
        int seconds;
        boolean negative = false;
        double currentVal;
        long milliseconds;
        String result = "";
        Resources res = context.getResources();

        //if either calendar is not set
        if(date1 == null || date2 == null)
        {
            return(getUnknownString(context));
        }

        //get total milliseconds
        milliseconds = date2.getTimeInMillis() - date1.getTimeInMillis();

        //if a negative value
        if(milliseconds < 0)
        {
            negative = true;
            milliseconds *= -1;
        }

        //get hours, minutes, and seconds
        currentVal = milliseconds / 86400000.0;     //fractional days
        days = (int)Math.floor(currentVal);
        currentVal = (currentVal - days) * 24;      //fractional hours
        hours = (int)Math.floor(currentVal);
        currentVal = (currentVal - hours) * 60;     //fractional minutes
        minutes = (int)Math.floor(currentVal);
        currentVal = (currentVal - minutes) * 60;   //fractional seconds
        seconds = (int)Math.floor(currentVal);

        //build string
        if(negative)
        {
            result += "-";
        }
        if(days > 0)
        {
            result += String.format(Locale.US, "%d" + res.getString(R.string.abbrev_days_lower), days);
        }
        if(hours > 0)
        {
            if(days > 0)
            {
                result += " ";
            }
            result += String.format(Locale.US, "%2d" + res.getString(R.string.abbrev_hours_lower), hours);
        }
        if(minutes > 0)
        {
            if(hours > 0)
            {
                result += " ";
            }
            result += String.format(Locale.US, "%2d" + res.getString(R.string.abbrev_minutes_short_lower), minutes);
        }
        if(seconds > 0)
        {
            if(hours > 0 || minutes > 0)
            {
                result += " ";
            }
            result += String.format(Locale.US, "%2d" + res.getString(R.string.abbrev_seconds_lower), seconds);
        }

        //return string
        return(result);
    }

    //Gets time until text
    public static String getTimeUntil(Context context, Resources res, Calendar timeNow, Calendar timeStart, Calendar timeEnd)
    {
        //if start time is set
        if(timeStart != null)
        {
            //if after start time
            if(timeNow.after(timeStart))
            {
                //if unknown end time
                if(timeEnd == null)
                {
                    return(res.getString(R.string.title_now));
                }
                //else if before end time
                else if(timeNow.before(timeEnd))
                {
                    return(getTimeBetween(context, timeStart, timeNow));
                }
                //else after end time
                else
                {
                    return(getTimeBetween(context, timeEnd, timeNow));
                }
            }
            //else before start time
            else
            {
                return(getTimeBetween(context, timeNow, timeStart));
            }
        }
        //else unknown
        else
        {
            return(getUnknownString(context));
        }
    }

    //Gets time until description
    public static String getTimeUntilDescription(Resources res, Calendar timeNow, Calendar timeStart, Calendar timeEnd)
    {
        //if start time is set
        if(timeStart != null)
        {
            //if after start time and end time is known
            if(timeNow.after(timeStart) && timeEnd != null)
            {
                //elapsed if before end, since if after
                return(res.getString(timeNow.before(timeEnd) ? R.string.title_elapsed : R.string.title_time_since));
            }
        }

        //time until
        return(res.getString(R.string.title_time_until));
    }

    //Gets if TLE data is accurate
    public static boolean getTLEIsAccurate(long tleDateMs)
    {
        return(((System.currentTimeMillis() - tleDateMs) / Calculations.MsPerDay) <= 365);
    }
    public static boolean getTLEIsAccurate(Calculations.TLEDataType tle)
    {
        return(tle != null && getTLEIsAccurate(julianDateToCalendar(tle.epochJulian).getTimeInMillis()));
    }

    //Gets the language from the given context
    public static String getLanguage(Context context)
    {
        if(context != null)
        {
            //get language
            Configuration config = context.getResources().getConfiguration();
            LocaleList locales = (Build.VERSION.SDK_INT >= 24 ? config.getLocales() : null);
            Locale currentLocale = (Build.VERSION.SDK_INT >= 24 && locales != null && !locales.isEmpty() ? locales.get(0) : Locale.getDefault());
            return(currentLocale.getLanguage());
        }
        else
        {
            return(Locale.getDefault().getLanguage());
        }
    }
    public static byte getLanguageIndex(Context context)
    {
        //get language
        String language = getLanguage(context);

        //if Spanish
        if(language.equals(Languages.Spanish))
        {
            return(Database.LanguageIndex.Spanish);
        }
        //else default to English
        else
        {
            return(Database.LanguageIndex.English);
        }
    }

    //Gets a number as a string
    public static String getNumberString(double number, int decimalPlaces, boolean allowCommas)
    {
        String separator = (allowCommas && Settings.getUsingNumberCommas() ? "," : "");

        if(number < 1.0E10)
        {
            if(decimalPlaces == 0)
            {
                return(String.format(Locale.US, "%" + separator + "3d", (long)number));
            }
            else
            {
                return(String.format(Locale.US, "%" + separator + "3." + decimalPlaces + "f", number));
            }
        }
        else
        {
            //noinspection MalformedFormatString
            return(String.format(Locale.US, "%3." + Math.max(3, decimalPlaces) + "e", number));
        }
    }
    public static String getNumberString(double number, int decimalPlaces)
    {
        return(getNumberString(number, decimalPlaces, true));
    }
    public static String getNumberString(double number, boolean allowCommas)
    {
        return(getNumberString(number, 2, allowCommas));
    }
    public static String getNumberString(double number)
    {
        return(getNumberString(number, true));
    }

    //Gets GMT offset string
    public static String getGMTOffsetString(TimeZone zone)
    {
        int offset = zone.getRawOffset();
        int minutes = Math.abs(offset % 3600000) / 60000;
        int hours = offset / 3600000;
        String name = zone.getID().replace('_', ' ');

        return(("(GMT" + (hours >= 0 ? "+" : "") + String.format(Locale.US, "%d:%02d", hours, minutes) + ") " + name));
    }

    //Gets a degree string
    public static String getDegreeString(double el, int decimalPlaces)
    {
        return(getNumberString(el, decimalPlaces) + Symbols.Degree);
    }
    public static String getDegreeString(double el)
    {
        return(getNumberString(el) + Symbols.Degree);
    }

    //Gets temperature string
    public static String getTemperatureString(double value)
    {
        return(getDegreeString(value, 0));
    }

    //Gets km unit value string
    public static String getKmUnitValueString(double value, int decimalPlaces)
    {
        return(getNumberString(getKmUnitValue(value), decimalPlaces));
    }
    public static String getKmUnitValueString(double value)
    {
        return(getNumberString(getKmUnitValue(value)));
    }

    //Gets latitude compass direction
    public static String getLatitudeDirection(Resources res, double latitude)
    {
        return(latitude > 0 ? res.getString(R.string.abbrev_north) : latitude < 0 ? res.getString(R.string.abbrev_south) : "");
    }

    //Gets latitude direction string
    public static String getLatitudeDirectionString(Resources res, double latitude, int decimals)
    {
        return(getDegreeString(Math.abs(latitude), decimals) + " " + getLatitudeDirection(res, latitude));
    }

    //Gets longitude compass direction
    public static String getLongitudeDirection(Resources res, double longitude)
    {
        return(longitude > 0 ? res.getString(R.string.abbrev_east) : longitude < 0 ? res.getString(R.string.abbrev_west) : "");
    }

    //Gets longitude direction string
    public static String getLongitudeDirectionString(Resources res, double longitude, int decimals)
    {
        return(getDegreeString(Math.abs(longitude), decimals) + " " + getLongitudeDirection(res, longitude));
    }

    //Gets coordinate string
    public static String getCoordinateString(Context context, double latitude, double longitude, double altitude)
    {
        Resources res = (context != null ? context.getResources() : null);
        return(res != null ? res.getString(R.string.abbrev_latitude) + ": " + getLatitudeDirectionString(res, latitude, 3) + "\n" + res.getString(R.string.abbrev_longitude) + ": " + getLongitudeDirectionString(res, longitude, 3) + (!Double.isNaN(altitude) && !Double.isInfinite(altitude) ? "\n" + res.getString(R.string.abbrev_altitude) + ": " + getKmUnitValueString(altitude) + " " + getKmLabel(res) : "") : "");
    }

    //Gets azimuth compass direction
    private static String getAzimuthDirection(Resources res, double az)
    {
        az = normalizePositiveAngle(az);

        if(az >= 348.75)
        {
            return(res.getString(R.string.abbrev_north));
        }
        else if(az >= 326.25)
        {
            return(res.getString(R.string.abbrev_north_north_west));
        }
        else if(az >= 303.75)
        {
            return(res.getString(R.string.abbrev_north_west));
        }
        else if(az >= 281.25)
        {
            return(res.getString(R.string.abbrev_west_north_west));
        }
        else if(az >= 258.75)
        {
            return(res.getString(R.string.abbrev_west));
        }
        else if(az >= 236.25)
        {
            return(res.getString(R.string.abbrev_west_south_west));
        }
        else if(az >= 213.75)
        {
            return(res.getString(R.string.abbrev_south_west));
        }
        else if(az >= 191.25)
        {
            return(res.getString(R.string.abbrev_south_south_west));
        }
        else if(az >= 168.75)
        {
            return(res.getString(R.string.abbrev_south));
        }
        else if(az >= 146.25)
        {
            return(res.getString(R.string.abbrev_south_south_east));
        }
        else if(az >= 123.75)
        {
            return(res.getString(R.string.abbrev_south_east));
        }
        else if(az >= 101.25)
        {
            return(res.getString(R.string.abbrev_east_south_east));
        }
        else if(az >= 78.75)
        {
            return(res.getString(R.string.abbrev_east));
        }
        else if(az >= 56.25)
        {
            return(res.getString(R.string.abbrev_east_north_east));
        }
        else if(az >= 33.75)
        {
            return(res.getString(R.string.abbrev_north_east));
        }
        else if(az >= 11.25)
        {
            return(res.getString(R.string.abbrev_north_north_east));
        }
        else
        {
            return(res.getString(R.string.abbrev_north));
        }
    }

    //Gets azimuth direction string
    public static String getAzimuthDirectionString(Resources res, double az)
    {
        return(getDegreeString(az) + " " + getAzimuthDirection(res, az));
    }

    //Gets illumination string
    public static String getIlluminationString(double illumination)
    {
        return(Globals.getNumberString(illumination, 1, false) + "%");
    }

    //Gets header text
    public static String getHeaderText(Context context, String satelliteName, double julianDateStart, double julianDateEnd, double intersection)
    {
        TimeZone zone = MainActivity.getTimeZone();
        return(context != null ? ((satelliteName != null && !satelliteName.equals("?") ? (satelliteName + "\n") : "") + getDateTimeString(julianDateStart, zone) + " " + context.getString(R.string.text_to) + " " + getDateTimeString(julianDateEnd, zone) + (intersection > -Double.MAX_VALUE ? ("\n" + context.getString(R.string.title_within) + " " + getDegreeString(intersection)) : "")) : "");
    }
    public static String getHeaderText(Context context, String satelliteName, double julianDateStart, double julianDateEnd)
    {
        return(getHeaderText(context, satelliteName, julianDateStart, julianDateEnd, -Double.MAX_VALUE));
    }
    public static String getHeaderText(Context context, String satelliteName, Calendar startGMT, Calendar endGMT)
    {
        TimeZone zone = MainActivity.getTimeZone();
        return(context != null ? ((satelliteName != null ? (satelliteName + "\n") : "") + getDateTimeString(startGMT, zone) + " " + context.getString(R.string.text_to) + " " + getDateTimeString(endGMT, zone)) : "");
    }

    //Resolves attribute IDs
    public static int[] resolveAttributeIDs(Context context, int ...resIDs)
    {
        int index;
        int[] ids = new int[resIDs.length];
        TypedValue values = new TypedValue();
        Resources.Theme currentTheme = (context != null ? context.getTheme() : null);

        //go through each resource ID
        for(index = 0; index < resIDs.length; index++)
        {
            //set default
            ids[index] = -1;

            //if theme exists
            if(currentTheme != null)
            {
                //resolve attribute ID
                currentTheme.resolveAttribute(resIDs[index], values, true);
                try(@SuppressLint({"NewApi", "LocalSuppress"}) TypedArray valueArray = context.obtainStyledAttributes(values.data, resIDs))
                {
                    ids[index] = valueArray.getResourceId(index, -1);
                    valueArray.recycle();
                }
                catch(NoSuchMethodError | Exception noMethod)
                {
                    //do nothing
                }
            }
        }

        //return IDs
        return(ids);
    }

    //Resolves an attribute
    public static int resolveAttributeID(Context context, int resAttr)
    {
        return(resolveAttributeIDs(context, resAttr)[0]);
    }

    //Gets screen orientation value
    public static int getScreenOrientation(Context context)
    {
        //if context is set
        if(context != null)
        {
            if(Build.VERSION.SDK_INT >= 30)
            {
                try
                {
                    //return orientation
                    return(context.getDisplay().getRotation());
                }
                catch(Exception ex)
                {
                    //do nothing
                }
            }
            else
            {
                //if manager is set
                WindowManager manager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
                if(manager != null)
                {
                    try
                    {
                        //return orientation
                        return(manager.getDefaultDisplay().getRotation());
                    }
                    catch(Exception ex)
                    {
                        //do nothing
                    }
                }
            }
        }

        //error
        return(Integer.MAX_VALUE);
    }

    //Gets text width
    public static int getTextWidth(Paint textPaint, String text)
    {
        return(text != null ? (int)textPaint.measureText(text) : 0);
    }

    //Gets text height
    public static int getTextHeight(Paint textPaint, String text)
    {
        int length = (text == null ? 0 : text.length());
        Rect area = new Rect();

        if(length == 0)
        {
            return(0);
        }
        else
        {
            textPaint.getTextBounds(text, 0, length, area);
            return(area.height());
        }
    }

    //Converts given unit(s) to pixels
    private static float[] unitsToPixels(Context context, int unitType, float... unitValues)
    {
        int index;
        float[] pixels = new float[unitValues.length];
        DisplayMetrics metrics = (context != null ? context.getResources().getDisplayMetrics() : null);

        if(context != null)
        {
            for(index = 0; index < unitValues.length; index++)
            {
                pixels[index] = TypedValue.applyDimension(unitType, unitValues[index], metrics);
            }
        }

        return(pixels);
    }

    //Converts dp unit(s) to pixels
    public static float[] dpsToPixels(Context context, float... dp)
    {
        return(unitsToPixels(context, TypedValue.COMPLEX_UNIT_DIP, dp));
    }
    public static float dpToPixels(Context context, float dp)
    {
        return(dpsToPixels(context, dp)[0]);
    }

    //Converts sp to pixels
    public static float spToPixels(Context context, float sp)
    {
        return(unitsToPixels(context, TypedValue.COMPLEX_UNIT_SP, sp)[0]);
    }

    //Gets pixel size of screen
    public static int[] getScreenPixels(Context context)
    {
        WindowManager manager;
        DisplayMetrics metrics = new DisplayMetrics();

        if(context != null)
        {
            //if manager is set
            manager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            if(manager != null)
            {
                try
                {
                    //get pixels
                    manager.getDefaultDisplay().getMetrics(metrics);
                    return(new int[]{metrics.widthPixels, metrics.heightPixels});
                }
                catch(Exception ex)
                {
                    //do nothing
                }
            }
        }

        //error
        return(new int[]{0, 0});
    }

    //Gets dp of screen
    public static int getScreenDp(Context context, boolean forWidth)
    {
        WindowManager manager;
        DisplayMetrics metrics = new DisplayMetrics();

        //if context is set
        if(context != null)
        {
            //if manager is set
            manager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            if(manager != null)
            {
                try
                {
                    //get dp
                    manager.getDefaultDisplay().getMetrics(metrics);
                    return(Math.round((forWidth ? metrics.widthPixels : metrics.heightPixels) / metrics.density));
                }
                catch(Exception ex)
                {
                    //do nothing
                }
            }
        }

        //error
        return(Integer.MIN_VALUE);
    }
    public static int getScreenDp(Context context)
    {
        int rotation = getScreenOrientation(context);
        return(getScreenDp(context, (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)));
    }

    //Gets current dialog theme ID
    public static int getDialogThemeId(Context context)
    {
        return(resolveAttributeID(context, R.attr.dialogStyle));
    }

    //Resolves color attributes
    public static int[] resolveColorIDs(Context context, int ...colorAttrs)
    {
        int index;
        int[] ids = resolveAttributeIDs(context, colorAttrs);
        int[] colors = new int[ids.length];

        //go through each ID
        for(index = 0; index < ids.length; index++)
        {
            //set color
            colors[index] = (context != null ? ContextCompat.getColor(context, ids[index]) : Color.BLACK);
        }

        //return colors
        return(colors);
    }

    //Resolves a color attribute
    public static int resolveColorID(Context context, int colorAttr)
    {
        return(context != null ? ContextCompat.getColor(context, resolveAttributeID(context, colorAttr)) : Color.BLACK);
    }

    //Creates a new progress dialog
    public static MultiProgressDialog createProgressDialog(Context context)
    {
        return(new MultiProgressDialog(context, getDialogThemeId(context)));
    }

    //Creates an add current location dialog
    public static AlertDialog createAddCurrentLocationDialog(Context context, DialogInterface.OnClickListener negativeListener, DialogInterface.OnCancelListener cancelListener)
    {
        LinearLayout progressLayout = new LinearLayout(context);
        CircularProgressIndicator progressView = new CircularProgressIndicator(context);
        AlertDialog.Builder addCurrentLocationDialogBuilder = new AlertDialog.Builder(context, getDialogThemeId(context));

        progressView.setIndeterminate(true);
        progressLayout.setGravity(Gravity.CENTER);
        progressLayout.addView(progressView);

        addCurrentLocationDialogBuilder.setTitle(R.string.title_location_getting);
        addCurrentLocationDialogBuilder.setView(progressLayout);
        addCurrentLocationDialogBuilder.setCancelable(true);
        addCurrentLocationDialogBuilder.setNegativeButton(R.string.title_cancel, negativeListener);
        addCurrentLocationDialogBuilder.setOnCancelListener(cancelListener);

        return(addCurrentLocationDialogBuilder.create());
    }

    //Sets update dialog events
    public static void setUpdateDialog(MultiProgressDialog updateDialog, String title, final BaseInputActivity cancelActivity, final byte updateType, final boolean closeOnBackground)
    {
        final boolean haveUpdateType = (updateType != Byte.MAX_VALUE);
        final Context context = updateDialog.getContext();
        final Resources res = context.getResources();

        updateDialog.setTag(false);
        updateDialog.setCancelable(true);
        updateDialog.setMessage(null);
        updateDialog.setTitle(title);
        updateDialog.setProgress(0);
        updateDialog.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                //if know update type
                if(haveUpdateType)
                {
                    //show notification
                    UpdateService.setNotificationVisible(updateType, true);
                }

                //if closing from going to background and activity is set
                if(closeOnBackground && (boolean)updateDialog.getTag() && cancelActivity != null)
                {
                    //close it
                    cancelActivity.finish();
                }
            }
        });
        updateDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                //if know update type
                if(haveUpdateType)
                {
                    //cancel it
                    UpdateService.cancel(updateType);
                }

                //if activity is set
                if(cancelActivity != null)
                {
                    //close it
                    cancelActivity.finish();
                }
            }
        });
        updateDialog.setButton(DialogInterface.BUTTON_POSITIVE, res.getString(R.string.title_cancel), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                //close dialog by canceling
                dialog.cancel();
            }
        });
        updateDialog.setButton(DialogInterface.BUTTON_NEGATIVE, res.getString(R.string.title_background), (DialogInterface.OnClickListener)null);
        updateDialog.setManualButtonClick(DialogInterface.BUTTON_NEGATIVE, new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //if have permission to use notifications
                if(havePostNotificationsPermission(context))
                {
                    //set as going to background and close dialog
                    updateDialog.setTag(true);
                    updateDialog.dismiss();
                }
                else if(Globals.canAskPostNotificationsPermission)
                {
                    //ask permission again
                    askPostNotificationsPermission(((ContextWrapper)context).getBaseContext(), false);
                }
            }
        });
    }
    public static void setUpdateDialog(MultiProgressDialog dialog, String title, final byte updateType)
    {
        setUpdateDialog(dialog, title, null, updateType, false);
    }

    //Updates visibility for the given button
    public static void setVisible(FloatingActionButton button, boolean visible)
    {
        //if button exists
        if(button != null)
        {
            //hide first to force update
            button.hide();

            //if want to see
            if(visible)
            {
                //show again
                button.show();
            }
        }
    }

    //Gets an image width and height
    public static int[] getImageWidthHeight(Drawable image)
    {
        int[] size = new int[2];

        //get starting width and height by type
        if(image instanceof VectorDrawable)
        {
            VectorDrawable currentImage = (VectorDrawable)image;

            size[0] = currentImage.getIntrinsicWidth();
            size[1] = currentImage.getIntrinsicHeight();
        }
        else if(image instanceof VectorDrawableCompat)
        {
            VectorDrawableCompat currentImage = (VectorDrawableCompat)image;

            size[0] = currentImage.getIntrinsicWidth();
            size[1] = currentImage.getIntrinsicHeight();
        }
        else if(image != null)
        {
            size[0] = image.getIntrinsicWidth();
            size[1] = image.getIntrinsicHeight();
        }
        else
        {
            size[0] = size[1] = 1;
        }

        //return width and height
        return(size);
    }

    private static Drawable tryGetDrawable(Context context, int resId)
    {
        return (context != null ? ContextCompat.getDrawable(context, resId) : null);
    }

    //Gets a drawable with optional tint
    public static Drawable getDrawable(Context context, int resId, int tintColor, boolean colorIsId)
    {
        int color;
        Drawable drawableItem = (resId > 0 ? tryGetDrawable(context, resId) : null);

        if(drawableItem != null && tintColor != Color.TRANSPARENT)
        {
            color = (colorIsId ? ResourcesCompat.getColor(context.getResources(), tintColor, null) : tintColor);
            DrawableCompat.setTint(DrawableCompat.wrap(drawableItem).mutate(), color);
            drawableItem.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        }

        return(drawableItem);
    }
    public static Drawable getDrawable(Context context, int resId, int tintColorId)
    {
        return(getDrawable(context, resId, tintColorId, true));
    }
    public static Drawable getDrawable(Context context, int resId, boolean useThemeTint)
    {
        return(getDrawable(context, resId, (useThemeTint ? (Settings.getDarkTheme(context) ? R.color.white : R.color.black) : Color.TRANSPARENT), useThemeTint));
    }
    public static Drawable getDrawable(Context context, int resId)
    {
        return(getDrawable(context, resId, Color.TRANSPARENT, false));
    }

    //Gets a sized drawable
    public static Drawable getDrawableSizedRotated(Context context, Drawable image, int width, int height, float rotateDegrees, boolean isDpSize)
    {
        float[] dpPixels = (isDpSize ? dpsToPixels(context, width, height) : null);
        int startWidthPx;
        int startHeightPx;
        int scaledWidthPx = (isDpSize ? (int)dpPixels[0] : width);
        int scaledHeightPx = (isDpSize ? (int)dpPixels[1] : height);
        int[] imageSize;
        Bitmap imageBitmap;
        Bitmap resultBitmap;
        Canvas imageCanvas;
        Matrix rotateMatrix;

        //get starting width and height
        imageSize = getImageWidthHeight(image);
        startWidthPx = imageSize[0];
        startHeightPx = imageSize[1];

        //create bitmap and draw it
        imageBitmap = Bitmap.createBitmap(startWidthPx, startHeightPx, Bitmap.Config.ARGB_8888);
        imageCanvas = new Canvas(imageBitmap);
        image.setBounds(0, 0, startWidthPx, startHeightPx);
        image.draw(imageCanvas);

        //if need to rotate
        if(rotateDegrees != 0)
        {
            //rotate matrix
            rotateMatrix = new Matrix();
            rotateMatrix.postRotate(rotateDegrees);

            //rotate original image
            resultBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, startWidthPx, startHeightPx, rotateMatrix, true);
        }
        else
        {
            //use original image
            resultBitmap = imageBitmap;
        }

        //return scaled result image
        return(new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(resultBitmap, scaledWidthPx, scaledHeightPx, true)));
    }
    public static Drawable getDrawableSizedRotated(Context context, int resId, int width, int height, float rotateDegrees, int tintColor, boolean colorIsId, boolean isDpSize)
    {
        return(getDrawableSizedRotated(context, getDrawable(context, resId, tintColor, colorIsId), width, height, rotateDegrees, isDpSize));
    }
    public static Drawable getDrawableSizedRotated(Context context, int resId, int width, int height, float rotateDegrees, boolean useThemeTint, boolean isDpSize)
    {
        return(getDrawableSizedRotated(context, resId, width, height, rotateDegrees, (useThemeTint ? (Settings.getDarkTheme(context) ? R.color.white : R.color.black) : Color.TRANSPARENT), useThemeTint, isDpSize));
    }
    public static Drawable getDrawableSized(Context context, int resId, int width, int height, boolean useThemeTint, boolean isDpSize)
    {
        return(getDrawableSizedRotated(context, resId, width, height, 0, useThemeTint, isDpSize));
    }
    public static Drawable getDrawableSized(Context context, int resId, int width, int height, int tintColorId, boolean isDpSize)
    {
        return(getDrawableSizedRotated(context, resId, width, height, 0, tintColorId, true, isDpSize));
    }

    //Gets a drawable with restricted size
    public static Drawable getDrawableSizeRestricted(Context context, Drawable image, int limitWidthDp, int limitHeightDp)
    {
        int widthPx;
        int heightPx;
        int maxWidthPx;
        int maxHeightPx;
        int[] iconSize;
        boolean overWidth;
        boolean overHeight;
        boolean needLimitWidth = (limitWidthDp > 0);
        boolean needLimitHeight = (limitHeightDp > 0);

        //if context and icon are set while needing to limit width or height
        if(context != null && image != null && (needLimitWidth || needLimitHeight))
        {
            //get icon size and ratio
            iconSize = getImageWidthHeight(image);
            widthPx = iconSize[0];
            heightPx = iconSize[1];

            //check if icon is too large
            maxWidthPx = (needLimitWidth ? (int)dpToPixels(context, limitWidthDp) : Integer.MAX_VALUE);
            maxHeightPx = (needLimitHeight ? (int)dpToPixels(context, limitHeightDp) : Integer.MAX_VALUE);
            overWidth = (widthPx > maxWidthPx);
            overHeight = (heightPx > maxHeightPx);
            if(overWidth || overHeight)
            {
                //if too tall
                if(overHeight)
                {
                    //limit height and adjust width by fixed ratio
                    widthPx = (int)(widthPx / (heightPx / (float)maxHeightPx));
                    overWidth = (widthPx > maxWidthPx);
                    heightPx = maxHeightPx;
                }

                //if too wide
                if(overWidth)
                {
                    //limit width
                    widthPx = maxWidthPx;
                }

                //return resized image
                return(getDrawableSizedRotated(context, image, widthPx, heightPx, 0, false));
            }
        }

        //return unmodified image
        return(image);
    }

    //Gets combined drawables
    public static Drawable getDrawableCombined(Context context, int xStackOffsets, int yStackOffsets, boolean stacked, Drawable ...images)
    {
        int x = 0;
        int index;
        int xOffset;
        int yOffset;
        int height = 0;
        int totalWidth = 0;
        int deltaWidth;
        int deltaHeight;
        int totalImages = 0;
        int lastImageIndex = 0;
        Bitmap imageBitmap;
        Canvas imageCanvas;
        int[][] sizes;

        //if no images
        if(images == null || images.length == 0)
        {
            //nothing to return
            return(null);
        }
        //else if only 1 image
        else if(images.length == 1)
        {
            //return it
            return(images[0]);
        }

        //set sizes
        sizes = new int[images.length][2];

        //go through each image
        for(index = (stacked ? (images.length - 1) : 0); (stacked ? (index >= 0) : (index < images.length)); index += (stacked ? - 1 : 1))
        {
            //if image is set
            if(images[index] != null)
            {
                //get sizes
                sizes[index] = getImageWidthHeight(images[index]);
                if(sizes[index][0] == 1 && sizes[index][1] == 1)
                {
                    //not really any size
                    sizes[index][0] = sizes[index][1] = 0;
                }

                //update width
                if(stacked)
                {
                    //get offset
                    xOffset = (index * xStackOffsets);

                    //if the largest width so far
                    if(sizes[index][0] + xOffset > totalWidth)
                    {
                        //update width
                        totalWidth = sizes[index][0] + xOffset;
                    }
                }
                else
                {
                    //add to width
                    totalWidth += sizes[index][0];
                }

                //get offset
                yOffset = (stacked ? (index * yStackOffsets) : 0);

                //if the largest height so far
                if(sizes[index][1] + yOffset > height)
                {
                    //update height
                    height = sizes[index][1] + yOffset;
                }

                //update total images and last used index
                totalImages++;
                lastImageIndex = index;
            }
        }

        //if only 1 usable image
        if(totalImages == 1)
        {
            //return only usable image
            return(images[lastImageIndex]);
        }

        //create bitmap
        if(totalWidth > 0 && height > 0)
        {
            imageBitmap = Bitmap.createBitmap(totalWidth, height, Bitmap.Config.ARGB_8888);
            imageCanvas = new Canvas(imageBitmap);
            for(index = (stacked ? (images.length - 1) : 0); (stacked ? (index >= 0) : (index < images.length)); index += (stacked ? - 1 : 1))
            {
                //if image is set
                if(images[index] != null)
                {
                    //draw image to canvas
                    if(stacked)
                    {
                        xOffset = (index * xStackOffsets);
                        yOffset = (index * yStackOffsets);
                        deltaWidth = totalWidth - sizes[index][0];
                        deltaHeight = height - sizes[index][1];
                        images[index].setBounds((deltaWidth / 2) + xOffset, (deltaHeight / 2) + yOffset, (totalWidth - (deltaWidth / 2)) + xOffset, (height - (deltaHeight / 2)) + yOffset);
                    }
                    else
                    {
                        images[index].setBounds(x, 0, x + sizes[index][0], sizes[index][1]);
                    }
                    images[index].draw(imageCanvas);
                    x += sizes[index][0];
                }
            }

            //return combined image
            return(context != null ? new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(imageBitmap, totalWidth, height, true)) : null);
        }
        else
        {
            //no visible image
            return(null);
        }
    }
    public static Drawable getDrawableCombined(Context context, Drawable ...images)
    {
        return(getDrawableCombined(context, 0, 0, false, images));
    }
    public static Drawable getDrawableCombined(Context context, int[] imageIds)
    {
        int index;
        Drawable[] images = null;

        //if there are images
        if(imageIds != null && imageIds.length > 0)
        {
            //go through each image ID
            images = new Drawable[imageIds.length];
            for(index = 0; index < imageIds.length; index++)
            {
                //remember current ID
                int currentId = imageIds[index];

                //get image
                images[index] = (currentId > 0 ? Globals.getDrawable(context, currentId) : null);
            }
        }

        //return combined image
        return(getDrawableCombined(context, 0, 0, false, images));
    }

    //Gets a drawable color
    public static BitmapDrawable getDrawableColor(Context context, ColorDrawable colorImage, int sizePx)
    {
        Paint paint = new Paint();
        Bitmap squareImage = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(squareImage);

        if(context == null || colorImage == null)
        {
            return(null);
        }

        paint.setColor(colorImage.getColor());
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(new Rect(0, 0, sizePx, sizePx), paint);

        return(new BitmapDrawable(context.getResources(), squareImage));
    }

    //Gets a tinted drawable
    public static Drawable getDrawableTinted(Drawable image, int tintColor)
    {
        Drawable tintedImage;

        if(image == null)
        {
            return(null);
        }

        tintedImage = image.mutate();
        DrawableCompat.setTint(tintedImage, tintColor);
        return(tintedImage);
    }

    //Gets text as a drawable
    public static BitmapDrawable getDrawableText(Context context, String text, float textSize, int textColor)
    {
        int textWidth;
        int textHeight;
        Bitmap textImage;
        Canvas textCanvas;
        Paint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        if(context == null)
        {
            return(null);
        }

        textPaint.setColor(textColor);
        textPaint.setStrokeWidth(2);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(Typeface.SANS_SERIF);
        textPaint.setTextSize(context.getResources().getDisplayMetrics().density * textSize);

        textWidth = getTextWidth(textPaint, text);
        textHeight = getTextHeight(textPaint, text);
        textImage = Bitmap.createBitmap(textWidth, textHeight, Bitmap.Config.ARGB_8888);
        textCanvas = new Canvas(textImage);

        textCanvas.drawText(text, 0, textHeight - 1, textPaint);

        return(new BitmapDrawable(context.getResources(), textImage));
    }

    //Gets an image with/without a "no" drawable
    public static Drawable getDrawableYesNo(Context context, int resId, int sizeDp, boolean useThemeTint, boolean isYes)
    {
        Drawable image = getDrawableSized(context, resId, sizeDp, sizeDp, useThemeTint, true);
        Drawable noImage = (isYes ? null : getDrawableSized(context, R.drawable.ic_no, sizeDp, sizeDp, useThemeTint, true));

        return(getDrawableCombined(context, 0, 0, true, image, noImage));
    }

    //Copies a bitmap
    public static Bitmap copyBitmap(Bitmap image)
    {
        return(image != null ? image.copy(image.getConfig(), image.isMutable()) : null);
    }

    //Gets a bitmap
    public static Bitmap getBitmap(Drawable image)
    {
        int[] size = getImageWidthHeight(image);
        Bitmap bitmapImage = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888);
        Canvas imageCanvas = new Canvas(bitmapImage);

        if(image != null)
        {
            image.setBounds(0, 0, imageCanvas.getWidth(), imageCanvas.getHeight());
            image.draw(imageCanvas);
        }

        return(bitmapImage);
    }
    public static Bitmap getBitmap(Context context, int resId, int tintColor)
    {
        return(getBitmap(getDrawable(context, resId, tintColor, false)));
    }
    public static Bitmap getBitmap(Context context, int resId, boolean useThemeTint)
    {
        return(getBitmap(getDrawable(context, resId, useThemeTint)));
    }

    //Gets a sized bitmap
    public static Bitmap getBitmapSized(Context context, int resId, int width, int height, int tintColorId)
    {
        return(getBitmap(getDrawableSized(context, resId, width, height, tintColorId, false)));
    }

    //Gets rotated version of given bitmap without changing size
    public static Bitmap getBitmapRotated(Bitmap image, double rotateDegrees)
    {
        boolean haveImage = (image != null);
        int width = (haveImage ? image.getWidth() : 1);
        int height = (haveImage ? image.getHeight() : 1);
        Bitmap rotatedImage;
        Matrix rotateMatrix = new Matrix();

        rotateMatrix.postRotate((float)rotateDegrees);
        rotatedImage = (haveImage && width > 1 && height > 1 ? Bitmap.createBitmap(image, 0, 0, width, height, rotateMatrix, true) : null);

        return(rotatedImage != null ? Bitmap.createBitmap(rotatedImage, (rotatedImage.getWidth() - width) / 2, (rotatedImage.getHeight() - height) / 2, width, height) : null);
    }

    //Gets a selector image
    private static Drawable getSelectorImage(int touchColor, int backgroundColor, int radiusPx)
    {
        boolean usingRadius = (radiusPx > 0);
        ShapeDrawable roundShape = null;
        ShapeDrawable rectangleShape = new ShapeDrawable(new RectShape());

        if(usingRadius)
        {
            roundShape = new ShapeDrawable(new RoundRectShape(new float[]{radiusPx, radiusPx, radiusPx, radiusPx, radiusPx, radiusPx, radiusPx, radiusPx}, null, null));
            roundShape.setAlpha(200);
            roundShape.getPaint().setColor(touchColor);
        }
        rectangleShape.getPaint().setColor(usingRadius ? backgroundColor : touchColor);

        return(usingRadius ? new LayerDrawable(new Drawable[]{rectangleShape, roundShape}) : rectangleShape);
    }

    //Gets a selector background
    private static StateListDrawable getSelector(Context context, int pressedColor, int selectedColor, int backgroundColor, int radiusDp)
    {
        int radiusPx;
        StateListDrawable selector = new StateListDrawable();

        radiusPx = (radiusDp > 0 ? (int)dpToPixels(context, radiusDp) : 0);
        selector.addState(new int[]{android.R.attr.state_pressed}, getSelectorImage(pressedColor, backgroundColor, radiusPx));
        selector.addState(new int[]{android.R.attr.state_selected}, getSelectorImage(selectedColor, backgroundColor, radiusPx));
        selector.addState(new int[]{}, getSelectorImage(backgroundColor, backgroundColor, 0));

        return(selector);
    }

    //Gets an item state selector
    private static StateListDrawable getItemStateSelector(Context context, int bgAttrId, boolean rounded)
    {
        int[] colors = resolveColorIDs(context, R.attr.itemSelectColor, R.attr.itemSelectedColor, bgAttrId);
        return(getSelector(context, colors[0], colors[1], colors[2], (rounded ? 36 : 0)));
    }

    //Gets an item selected state
    public static Drawable getItemSelectedState(Context context, boolean rounded)
    {
        int[] colors = resolveColorIDs(context, R.attr.itemSelectedColor, android.R.attr.colorBackground);
        return(getSelectorImage(colors[0], colors[1], (rounded ? 36 : 0)));
    }

    //Gets menu item selector
    public static StateListDrawable getMenuItemStateSelector(Context context)
    {
        return(getItemStateSelector(context, android.R.attr.colorBackground, true));
    }

    //Gets list item selector
    public static StateListDrawable getListItemStateSelector(Context context, boolean forDialog)
    {
        return(getItemStateSelector(context, (forDialog ? R.attr.pageHighlightBackground : android.R.attr.colorBackground), false));
    }

    //Gets data item selector
    public static StateListDrawable getDataItemStateSelector(Context context, int bgAttrId)
    {
        return(getItemStateSelector(context, bgAttrId, false));
    }

    //Get visible icon for given state
    public static Drawable getVisibleIcon(Context context, boolean isVisible)
    {
        return(Globals.getDrawable(context, (isVisible ? R.drawable.ic_remove_red_eye_white : R.drawable.ic_visibility_off_white), true));
    }

    //Applies currently used orbital type filter
    public static Database.SatelliteData[] applyOrbitalTypeFilter(Context context, int group, int subPage, Database.SatelliteData[] orbitals)
    {
        ArrayList<Byte> orbitalTypeFilterList = null;
        ArrayList<Database.SatelliteData> filteredOrbitals = new ArrayList<>(0);

        //get filter
        if(group == MainActivity.Groups.Current)
        {
            //handle based on sub page
            switch(subPage)
            {
                case Globals.SubPageType.List:
                    orbitalTypeFilterList = Settings.getListOrbitalTypeFilter(context);
                    break;

                case Globals.SubPageType.Map:
                case Globals.SubPageType.Globe:
                    orbitalTypeFilterList = Settings.getMapOrbitalTypeFilter(context);
                    break;

                case Globals.SubPageType.CameraLens:
                case Globals.SubPageType.VirtualLens:
                    orbitalTypeFilterList = Settings.getLensOrbitalTypeFilter(context);
                    break;
            }
        }

        //go through each orbital
        for(Database.SatelliteData currentOrbital : orbitals)
        {
            //update filter
            currentOrbital.setInFilter(orbitalTypeFilterList);

            //if in filter
            if(currentOrbital.getInFilter())
            {
                //add it to the list
                filteredOrbitals.add(currentOrbital);
            }
        }

        //return filtered orbitals
        return(filteredOrbitals.toArray(new Database.SatelliteData[0]));
    }

    //Removes any filter from given orbitals
    public static void clearOrbitalFilter(Database.DatabaseSatellite[] orbitals)
    {
        //go through each orbital
        for(Database.DatabaseSatellite currentOrbital : orbitals)
        {
            //remove any filter
            currentOrbital.setInFilter(true);
        }
    }

    //Gets the filtered orbital count for each orbital type
    public static int[] getOrbitalTypeFilterCount(Database.SatelliteData[] orbitals)
    {
        int index;
        int[] typeCount = new int[Database.OrbitalType.TypeCount + 1];       //note: types start at 1

        //go through each count
        for(index = 0; index < typeCount.length; index++)
        {
            //initialize
            typeCount[index] = 0;
        }

        //go through each orbital
        for(Database.SatelliteData currentOrbital : orbitals)
        {
            //if in the filter
            if(currentOrbital.getInFilter())
            {
                //get type index and validate
                index = currentOrbital.getOrbitalType();
                if(index >= 0 && index < typeCount.length)
                {
                    //add to count
                    typeCount[index]++;
                }
            }
        }

        //return counts
        return(typeCount);
    }

    //Gets an orbital icon ID
    public static int getOrbitalIconId(Context context, int satelliteNum, byte orbitalType)
    {
        int iconId;
        boolean isMoozarov;
        boolean isConstellation;

        if(satelliteNum > 0)
        {
            switch(orbitalType)
            {
                case Database.OrbitalType.RocketBody:
                    iconId = R.drawable.orbital_rocket;
                    break;

                case Database.OrbitalType.Debris:
                    iconId = R.drawable.orbital_debris;
                    break;

                default:
                    iconId = Settings.getSatelliteIconImageId(context);
                    break;
            }
        }
        else
        {
            isMoozarov = (Settings.getOrbitalIconsType(context) == Settings.Options.Display.OrbitalIcons.Moozarov);

            switch(satelliteNum)
            {
                case Universe.IDs.Saturn:
                    iconId = (isMoozarov ? R.drawable.orbital_saturn_moozarov : R.drawable.orbital_saturn_freepik);
                    break;

                case Universe.IDs.Sun:
                    iconId = (isMoozarov ? R.drawable.orbital_sun_moozarov : R.drawable.orbital_sun_freepik);
                    break;

                case Universe.IDs.Pluto:
                    iconId = (isMoozarov ? R.drawable.orbital_pluto_moozarov : R.drawable.orbital_pluto_freepik);
                    break;

                case Universe.IDs.Venus:
                    iconId = (isMoozarov ? R.drawable.orbital_venus_moozarov : R.drawable.orbital_venus_freepik);
                    break;

                case Universe.IDs.Neptune:
                    iconId = (isMoozarov ? R.drawable.orbital_neptune_moozarov : R.drawable.orbital_neptune_freepik);
                    break;

                case Universe.IDs.Mars:
                    iconId = (isMoozarov ? R.drawable.orbital_mars_moozarov : R.drawable.orbital_mars_freepik);
                    break;

                case Universe.IDs.Jupiter:
                    iconId = (isMoozarov ? R.drawable.orbital_jupiter_moozarov : R.drawable.orbital_jupiter_freepik);
                    break;

                case Universe.IDs.Mercury:
                    iconId = (isMoozarov ? R.drawable.orbital_mercury_moozarov : R.drawable.orbital_mercury_freepik);
                    break;

                case Universe.IDs.Moon:
                    iconId = (isMoozarov ? R.drawable.orbital_moon_moozarov : R.drawable.orbital_moon_freepik);
                    break;

                case Universe.IDs.Uranus:
                    iconId = (isMoozarov ? R.drawable.orbital_uranus_moozarov : R.drawable.orbital_uranus_freepik);
                    break;

                default:
                    isConstellation = (orbitalType == Database.OrbitalType.Constellation);
                    iconId = (isMoozarov ? (isConstellation ? R.drawable.orbital_constellation_moozarov : R.drawable.orbital_star_moozarov) : (isConstellation ? R.drawable.orbital_constellation_freepik : R.drawable.orbital_star_freepik));
                    break;
            }
        }

        return(iconId);
    }
    public static int getOrbitalIconId(Context context, int satelliteNum)
    {
        return(getOrbitalIconId(context, satelliteNum, Database.OrbitalType.Satellite));
    }

    //Gets an orbital icon
    public static Drawable getOrbitalIcon(Context context, Calculations.ObserverType location, int satelliteNum, byte orbitalType, long timeMs, int forceColorId)
    {
        int iconID = getOrbitalIconId(context, satelliteNum, orbitalType);
        boolean allowIconColor = (iconID == R.drawable.orbital_rocket || iconID == R.drawable.orbital_debris || (satelliteNum > 0 && Settings.getSatelliteIconImageIsThemeable(context)));

        if(context == null)
        {
            return(null);
        }
        else if(satelliteNum == Universe.IDs.Moon)
        {
            return(new BitmapDrawable(context.getResources(), Universe.Moon.getPhaseImage(context, location, timeMs)));
        }
        else if(forceColorId != 0 && allowIconColor)
        {
            return(getDrawable(context, iconID, forceColorId));
        }
        else
        {
            return(getDrawable(context, iconID, allowIconColor));
        }
    }
    public static Drawable getOrbitalIcon(Context context, Calculations.ObserverType location, int satelliteNum, byte orbitalType, int forceColorId)
    {
        return(getOrbitalIcon(context, location, satelliteNum, orbitalType, System.currentTimeMillis(), forceColorId));
    }
    public static Drawable getOrbitalIcon(Context context, Calculations.ObserverType location, int satelliteNum, byte orbitalType)
    {
        return(getOrbitalIcon(context, location, satelliteNum, orbitalType, 0));
    }

    //Gets owner icon IDs
    @SuppressWarnings("SpellCheckingInspection")
    public static int[] getOwnerIconIDs(String ownerCode)
    {
        int[] ids = new int[]{-1};

        //if owner code is set
        if(ownerCode != null)
        {
            //make case insensitive
            ownerCode = ownerCode.toLowerCase();

            //get icon(s) based on code
            switch(ownerCode)
            {
                case "ab":
                    ids[0] = R.drawable.owner_ab;
                    break;

                case "abs":
                    ids[0] = R.drawable.owner_abs;
                    break;

                case "ac":
                    ids[0] = R.drawable.owner_ac;
                    break;

                case "ago":
                    ids[0] = R.drawable.owner_ago;
                    break;

                case "alg":
                    ids[0] = R.drawable.owner_alg;
                    break;

                case "ang":
                    ids[0] = R.drawable.owner_ang;
                    break;

                case "argn":
                    ids[0] = R.drawable.owner_argn;
                    break;

                case "asra":
                    ids[0] = R.drawable.owner_asra;
                    break;

                case "aus":
                    ids[0] = R.drawable.owner_aus;
                    break;

                case "azer":
                    ids[0] = R.drawable.owner_azer;
                    break;

                case "bel":
                    ids[0] = R.drawable.owner_bel;
                    break;

                case "bela":
                    ids[0] = R.drawable.owner_bela;
                    break;

                case "berm":
                    ids[0] = R.drawable.owner_berm;
                    break;

                case "bgd":
                    ids[0] = R.drawable.owner_bgd;
                    break;

                case "bgr":
                case "bul":
                    ids[0] = R.drawable.owner_bgr;
                    break;

                case "bhut":
                case "bt":
                    ids[0] = R.drawable.owner_bhut;
                    break;

                case "bol":
                    ids[0] = R.drawable.owner_bol;
                    break;

                case "braz":
                    ids[0] = R.drawable.owner_braz;
                    break;

                case "ca":
                    ids[0] = R.drawable.owner_ca;
                    break;

                case "chle":
                    ids[0] = R.drawable.owner_chle;
                    break;

                case "chbz":
                    ids = new int[2];
                    ids[0] = R.drawable.owner_prc;
                    ids[1] = R.drawable.owner_braz;
                    break;

                case "cis":
                    ids[0] = R.drawable.owner_cis;
                    break;

                case "col":
                    ids[0] = R.drawable.owner_col;
                    break;

                case "cri":
                    ids[0] = R.drawable.owner_cri;
                    break;

                case "cz":
                case "cze":
                case "czch":
                    ids[0] = R.drawable.owner_czch;
                    break;

                case "den":
                    ids[0] = R.drawable.owner_den;
                    break;

                case "ecu":
                    ids[0] = R.drawable.owner_ecu;
                    break;

                case "egyp":
                    ids[0] = R.drawable.owner_egyp;
                    break;

                case "esa":
                    ids[0] = R.drawable.owner_esa;
                    break;

                case "esro":
                    ids[0] = R.drawable.owner_esro;
                    break;

                case "est":
                    ids[0] = R.drawable.owner_est;
                    break;

                case "eume":
                    ids[0] = R.drawable.owner_eume;
                    break;

                case "eute":
                    ids[0] = R.drawable.owner_eute;
                    break;

                case "fger":
                    ids = new int[2];
                    ids[0] = R.drawable.owner_fr;
                    ids[1] = R.drawable.owner_ger;
                    break;

                case "fin":
                    ids[0] = R.drawable.owner_fin;
                    break;

                case "fr":
                    ids[0] = R.drawable.owner_fr;
                    break;

                case "frit":
                    ids = new int[2];
                    ids[0] = R.drawable.owner_fr;
                    ids[1] = R.drawable.owner_it;
                    break;

                case "ger":
                    ids[0] = R.drawable.owner_ger;
                    break;

                case "gha":
                    ids[0] = R.drawable.owner_gha;
                    break;

                case "glob":
                    ids[0] = R.drawable.owner_glob;
                    break;

                case "grec":
                    ids[0] = R.drawable.owner_grec;
                    break;

                case "grsa":
                    ids = new int[2];
                    ids[0] = R.drawable.owner_grec;
                    ids[1] = R.drawable.owner_saud;
                    break;

                case "guat":
                    ids[0] = R.drawable.owner_guat;
                    break;

                case "hun":
                    ids[0] = R.drawable.owner_hun;
                    break;

                case "im":
                    ids[0] = R.drawable.owner_im;
                    break;

                case "ind":
                    ids[0] = R.drawable.owner_ind;
                    break;

                case "indo":
                    ids[0] = R.drawable.owner_indo;
                    break;

                case "iran":
                    ids[0] = R.drawable.owner_iran;
                    break;

                case "irak":
                case "iraq":
                    ids[0] = R.drawable.owner_iraq;
                    break;

                case "irid":
                    ids[0] = R.drawable.owner_irid;
                    break;

                case "isra":
                    ids[0] = R.drawable.owner_isra;
                    break;

                case "isro":
                    ids[0] = R.drawable.owner_isro;
                    break;

                case "iss":
                    ids[0] = R.drawable.owner_iss;
                    break;

                case "it":
                    ids[0] = R.drawable.owner_it;
                    break;

                case "itso":
                    ids[0] = R.drawable.owner_itso;
                    break;

                case "jor":
                    ids[0] = R.drawable.owner_jor;
                    break;

                case "jpn":
                    ids[0] = R.drawable.owner_jpn;
                    break;

                case "kaz":
                    ids[0] = R.drawable.owner_kaz;
                    break;

                case "ken":
                    ids[0] = R.drawable.owner_ken;
                    break;

                case "kwt":
                    ids[0] = R.drawable.owner_kwt;
                    break;

                case "laos":
                    ids[0] = R.drawable.owner_laos;
                    break;

                case "lka":
                    ids[0] = R.drawable.owner_lka;
                    break;

                case "ltu":
                    ids[0] = R.drawable.owner_ltu;
                    break;

                case "ltv":
                    ids[0] = R.drawable.owner_ltv;
                    break;

                case "luxe":
                    ids[0] = R.drawable.owner_luxe;
                    break;

                case "ma":
                    ids[0] = R.drawable.owner_ma;
                    break;

                case "mala":
                    ids[0] = R.drawable.owner_mala;
                    break;

                case "mco":
                    ids[0] = R.drawable.owner_mc;
                    break;

                case "mda":
                    ids[0] = R.drawable.owner_mda;
                    break;

                case "mex":
                    ids[0] = R.drawable.owner_mex;
                    break;

                case "mmr":
                    ids[0] = R.drawable.owner_mmr;
                    break;

                case "mng":
                    ids[0] = R.drawable.owner_mng;
                    break;

                case "mus":
                    ids[0] = R.drawable.owner_mus;
                    break;

                case "nato":
                    ids[0] = R.drawable.owner_nato;
                    break;

                case "neth":
                    ids[0] = R.drawable.owner_neth;
                    break;

                case "nico":
                    ids[0] = R.drawable.owner_nico;
                    break;

                case "nig":
                    ids[0] = R.drawable.owner_nig;
                    break;

                case "nkor":
                    ids[0] = R.drawable.owner_nkor;
                    break;

                case "nor":
                    ids[0] = R.drawable.owner_nor;
                    break;

                case "npl":
                    ids[0] = R.drawable.owner_npl;
                    break;

                case "nz":
                    ids[0] = R.drawable.owner_nz;
                    break;

                case "o3b":
                    ids[0] = R.drawable.owner_o3b;
                    break;

                case "orb":
                    ids[0] = R.drawable.owner_orb;
                    break;

                case "paki":
                    ids[0] = R.drawable.owner_paki;
                    break;

                case "per":
                case "peru":
                    ids[0] = R.drawable.owner_per;
                    break;

                case "pol":
                    ids[0] = R.drawable.owner_pol;
                    break;

                case "por":
                    ids[0] = R.drawable.owner_por;
                    break;

                case "prc":
                    ids[0] = R.drawable.owner_prc;
                    break;

                case "pres":
                    ids = new int[2];
                    ids[0] = R.drawable.owner_prc;
                    ids[1] = R.drawable.owner_esa;
                    break;

                case "pry":
                    ids[0] = R.drawable.owner_pry;
                    break;

                case "qat":
                case "qatr":
                    ids[0] = R.drawable.owner_qatr;
                    break;

                case "rasc":
                    ids[0] = R.drawable.owner_rasc;
                    break;

                case "roc":
                    ids[0] = R.drawable.owner_roc;
                    break;

                case "rom":
                    ids[0] = R.drawable.owner_rom;
                    break;

                case "rp":
                    ids[0] = R.drawable.owner_rp;
                    break;

                case "rwa":
                    ids[0] = R.drawable.owner_rwa;
                    break;

                case "safr":
                    ids[0] = R.drawable.owner_safr;
                    break;

                case "saud":
                    ids[0] = R.drawable.owner_saud;
                    break;

                case "seal":
                    ids[0] = R.drawable.owner_seal;
                    break;

                case "ses":
                    ids[0] = R.drawable.owner_ses;
                    break;

                case "sdn":
                    ids[0] = R.drawable.owner_sdn;
                    break;

                case "sgjp":
                    ids = new int[2];
                    ids[0] = R.drawable.owner_sing;
                    ids[1] = R.drawable.owner_jpn;
                    break;

                case "sing":
                    ids[0] = R.drawable.owner_sing;
                    break;

                case "sk":
                    ids[0] = R.drawable.owner_sk;
                    break;

                case "kor":
                case "skor":
                    ids[0] = R.drawable.owner_skor;
                    break;

                case "spn":
                    ids[0] = R.drawable.owner_spn;
                    break;

                case "stct":
                    ids = new int[2];
                    ids[0] = R.drawable.owner_sing;
                    ids[1] = R.drawable.owner_roc;
                    break;

                case "swed":
                    ids[0] = R.drawable.owner_swed;
                    break;

                case "swtz":
                    ids[0] = R.drawable.owner_swtz;
                    break;

                case "svn":
                    ids[0] = R.drawable.owner_svn;
                    break;

                case "thai":
                    ids[0] = R.drawable.owner_thai;
                    break;

                case "tmmc":
                    ids = new int[2];
                    ids[0] = R.drawable.owner_tm;
                    ids[1] = R.drawable.owner_mc;
                    break;

                case "tun":
                    ids[0] = R.drawable.owner_tun;
                    break;

                case "turk":
                    ids[0] = R.drawable.owner_turk;
                    break;

                case "twn":
                    ids[0] = R.drawable.owner_twn;
                    break;

                case "uae":
                    ids[0] = R.drawable.owner_uae;
                    break;

                case "uk":
                    ids[0] = R.drawable.owner_uk;
                    break;

                case "ukr":
                    ids[0] = R.drawable.owner_ukr;
                    break;

                case "ury":
                    ids[0] = R.drawable.owner_ury;
                    break;

                case "us":
                    ids[0] = R.drawable.owner_us;
                    break;

                case "usbz":
                    ids = new int[2];
                    ids[0] = R.drawable.owner_us;
                    ids[1] = R.drawable.owner_braz;
                    break;

                case "vat":
                    ids[0] = R.drawable.owner_vat;
                    break;

                case "venz":
                    ids[0] = R.drawable.owner_venz;
                    break;

                case "vtnm":
                    ids[0] = R.drawable.owner_vtnm;
                    break;
            }
        }

        //return IDs
        return(ids);
    }

    //Normalizes owner code
    @SuppressWarnings("SpellCheckingInspection")
    public static String normalizeOwnerCode(String code)
    {
        //if not set
        if(code == null)
        {
            //set empty
            code = "";
        }
        else
        {
            //make uppercase
            code = code.toUpperCase();

            //check and fix any typos
            switch(code)
            {
                case "BGL":
                    code = "BUL";
                    break;

                case "IRAK":
                    code = "IRAQ";
                    break;

                case "NEP":
                    code = "NPL";
                    break;

                case "PER":
                    code = "PERU";
                    break;

                case "SRI":
                    code = "LKA";
                    break;

                case "SVK":
                    code = "SK";
                    break;
            }
        }

        return(code);
    }

    //Normalizes owner name
    @SuppressWarnings("SpellCheckingInspection")
    public static String normalizeOwnerName(String name)
    {
        //if set
        if(name != null)
        {
            //make uppercase
            name = name.toUpperCase();

            //check and fix any typos
            if(name.equals("IRAK"))
            {
                name = "IRAQ";
            }
        }

        return(name);
    }

    public static void drawBitmap(Canvas canvas, Bitmap image, float centerX, float centerY, float offsetY, float rotateDegrees, Paint paint)
    {
        int width;
        int height;

        if(canvas != null && image != null)
        {
            width = image.getWidth();
            height = image.getHeight();

            canvas.save();
            canvas.translate(centerX, centerY);
            canvas.rotate(rotateDegrees);
            canvas.drawBitmap(image, width / -2f, (height / -2f) + offsetY, paint);
            canvas.restore();
        }
    }
    public static void drawBitmap(Canvas canvas, Bitmap image, float centerX, float centerY, float rotateDegrees, Paint paint)
    {
        drawBitmap(canvas, image, centerX, centerY, 0, rotateDegrees, paint);
    }

    //Gets location icon type icon ID
    public static int getLocationIconTypeIconID(int locationIconType)
    {
        switch(locationIconType)
        {
            case Settings.Options.Display.LocationIcon.Person:
                return(R.drawable.map_location_person_red);

            case Settings.Options.Display.LocationIcon.Home:
                return(R.drawable.map_location_home_red);

            case Settings.Options.Display.LocationIcon.City:
                return(R.drawable.map_location_city_red);

            case Settings.Options.Display.LocationIcon.Tower:
                return(R.drawable.map_location_tower_red);

            case Settings.Options.Display.LocationIcon.Radar:
                return(R.drawable.map_location_radar_red);

            case Settings.Options.Display.LocationIcon.Telescope1:
                return(R.drawable.map_location_telescope1);

            case Settings.Options.Display.LocationIcon.Telescope2:
                return(R.drawable.map_location_telescope2);

            case Settings.Options.Display.LocationIcon.Observatory1:
                return(R.drawable.map_location_observatory1);

            case Settings.Options.Display.LocationIcon.Observatory2:
                return(R.drawable.map_location_observatory2);

            case Settings.Options.Display.LocationIcon.Dish1:
                return(R.drawable.map_location_dish1);

            case Settings.Options.Display.LocationIcon.Dish2:
                return(R.drawable.map_location_dish2);

            case Settings.Options.Display.LocationIcon.Dish3:
                return(R.drawable.map_location_dish3);

            case Settings.Options.Display.LocationIcon.Dish4:
                return(R.drawable.map_location_dish4);

            case Settings.Options.Display.LocationIcon.Marker:
            default:
                return(R.drawable.map_location_marker_red);
        }
    }

    //Gets location icon
    public static int getLocationIcon(byte locationType)
    {
        switch(locationType)
        {
            case Database.LocationType.Current:
                return(R.drawable.ic_my_location_black);

            case Database.LocationType.Online:
                return(R.drawable.ic_place_black);

            case Database.LocationType.Saved:
            default                                     :
                return(R.drawable.ic_person_pin_circle_black);
        }
    }

    //Removes the given view and returns index
    private static int removeView(int removeID, ViewGroup rootView)
    {
        int viewIndex;
        View removedView;

        //remove view
        removedView = rootView.findViewById(removeID);
        viewIndex = rootView.indexOfChild(removedView);
        rootView.removeViewAt(viewIndex);

        //return index
        return(viewIndex);
    }

    //Replaces a view with the given view
    public static View replaceView(int removeID, int addID, LayoutInflater inflater, ViewGroup rootView)
    {
        //get new view
        View swapView = inflater.inflate(addID, rootView, false);

        //replace and return view
        rootView.addView(swapView, removeView(removeID, rootView));
        return(swapView);
    }
    public static void replaceView(int removeID, View addView, ViewGroup rootView)
    {
        //replace and return view
        rootView.addView(addView, removeView(removeID, rootView));
    }

    //Returns an array containing index object found at and index it would belong if not found
    private static<T> int[] divideFind(T value, ArrayList<T> sortedList, int startIndex, int endIndex, Comparator<T> comparator)
    {
        int compareResult;
        int middleIndex = (((endIndex - startIndex) + 1) / 2) + startIndex;
        T middleValue;

        //if starting outside array
        if(startIndex < 0)
        {
            //not found
            return(new int[]{-1, 0});
        }
        //else if going past start
        else if(startIndex > endIndex)
        {
            //not found
            return(new int[]{-1, startIndex});
        }
        //else if going outside array
        else if(endIndex > sortedList.size() - 1)
        {
            //not found
            return(new int[]{-1, sortedList.size()});
        }

        //get current middle value and compare result
        middleValue = sortedList.get(middleIndex);
        compareResult = comparator.compare(value, middleValue);

        //if a match
        if(compareResult == 0)
        {
            //found
            return(new int[]{middleIndex});
        }
        //else if no more room to search
        else if(startIndex == endIndex)
        {
            //not found
            return(new int[]{-1, middleIndex + (compareResult > 0 ? 1 : 0)});
        }
        //else if ahead in list
        else if(compareResult > 0)
        {
            //look ahead in list
            return(divideFind(value, sortedList, middleIndex + 1, endIndex, comparator));
        }
        //else behind in list
        else
        {
            //look behind in list
            return(divideFind(value, sortedList, startIndex, middleIndex - 1, comparator));
        }
    }
    public static<T> int[] divideFind(T value, ArrayList<T> sortedList, Comparator<T> comparator)
    {
        return(divideFind(value, sortedList, 0, sortedList.size() - 1, comparator));
    }

    //Compares 2 strings
    public static int stringCompare(String value1, String value2)
    {
        if(value1 == null && value2 == null)
        {
            return(0);
        }
        else if(value1 == null)
        {
            return(-1);
        }
        else if(value2 == null)
        {
            return(1);
        }
        else
        {
            return(value1.compareTo(value2));
        }
    }

    //Compares 2 calendars
    public static int calendarCompare(Calendar value1, Calendar value2)
    {
        if(value1 == null && value2 == null)
        {
            return(0);
        }
        if(value1 == null)
        {
            return(-1);
        }
        else if(value2 == null)
        {
            return(1);
        }
        else
        {
            return(value1.compareTo(value2));
        }
    }

    //Normalizes a pass time for use in comparison
    private static Calendar normalizePassStartCompare(Calendar timeStart, Calendar timeEnd)
    {
        Calendar timeNow = getGMTTime();

        //if start time is set
        if(timeStart != null)
        {
            //if after start time
            if(timeNow.after(timeStart))
            {
                //if unknown end time
                if(timeEnd == null)
                {
                    //now
                    return(timeNow);
                }
                else
                {
                    //return actual start
                    return(timeStart);
                }
            }
            //else before start time
            else
            {
                //return actual start
                return(timeStart);
            }
        }
        else
        {
            //unknown
            return(null);
        }
    }

    //Compares 2 pass times
    public static int passTimeCompare(Calendar timeStart1, Calendar timeEnd1, Calendar timeStart2, Calendar timeEnd2)
    {
        return(calendarCompare(normalizePassStartCompare(timeStart1, timeEnd1), normalizePassStartCompare(timeStart2, timeEnd2)));
    }

    //Normalizes pass duration for use in comparison
    private static long normalizePassDurationMs(Calendar timeStart, Calendar timeEnd)
    {
        //if start time is set
        if(timeStart != null)
        {
            //if end time is set
            if(timeEnd != null)
            {
                //return time between in ms
                return(timeEnd.getTimeInMillis() - timeStart.getTimeInMillis());
            }
            else
            {
                //forever
                return(Long.MAX_VALUE);
            }
        }
        else
        {
            //none
            return(0);
        }
    }

    //Compares 2 pass durations
    public static int passDurationCompare(Calendar timeStart1, Calendar timeEnd1, Calendar timeStart2, Calendar timeEnd2)
    {
        return(Long.compare(normalizePassDurationMs(timeStart1, timeEnd1), normalizePassDurationMs(timeStart2, timeEnd2)));
    }

    //Compares 2 pass maximum elevations
    public static int passMaxElevationCompare(double passElMax1, double passElMax2)
    {
        if(passElMax1 == Double.MAX_VALUE)
        {
            passElMax1 = -Double.MAX_VALUE;
        }
        if(passElMax2 == Double.MAX_VALUE)
        {
            passElMax2 = -Double.MAX_VALUE;
        }

        return(Double.compare(passElMax1, passElMax2));
    }

    //Tries to return a byte from the given input
    public static byte tryParseByte(String input)
    {
        try
        {
            return(Byte.parseByte(input));
        }
        catch(NumberFormatException ex)
        {
            return(Byte.MAX_VALUE);
        }
    }

    //Tries to return an int from the given input
    public static int tryParseInt(String input)
    {
        try
        {
            return(Integer.parseInt(input));
        }
        catch(NumberFormatException ex)
        {
            return(Integer.MAX_VALUE);
        }
    }

    //Tries to return a float from given input
    public static float tryParseFloat(String input)
    {
        try
        {
            return(Float.parseFloat(input));
        }
        catch(NumberFormatException ex)
        {
            return(Float.MAX_VALUE);
        }
    }

    //Tries to return a double from given input
    public static double tryParseDouble(String input)
    {
        if(input == null)
        {
            return(Double.MAX_VALUE);
        }

        try
        {
            return(Double.parseDouble(input));
        }
        catch(NumberFormatException ex)
        {
            return(Double.MAX_VALUE);
        }
    }

    //Tries to get a calendar created from the given string
    public static synchronized Calendar tryParseGMTDate(String dateString)
    {
        try
        {
            dateParser.parse(dateString);
            return(getGMTTime(dateParser.getCalendar()));
        }
        catch(Exception ex)
        {
            return(null);
        }
    }

    //Returns if given string starts with any of the given strings
    public static boolean startsWith(String value, String ...startValues)
    {
        //go through each start value
        for(String currentStartValue : startValues)
        {
            //if value starts with current
            if(value.startsWith(currentStartValue))
            {
                //yes
                return(true);
            }
        }

        //no
        return(false);
    }

    //Normalizes an angle to +/- 180 degrees
    private static double normalizeAngle(double angle, double maxDeg)
    {
        double maxDistance = maxDeg * 2;

        if(angle > maxDeg)
        {
            return(angle - maxDistance);
        }
        else if(angle < -maxDeg)
        {
            return(angle + maxDistance);
        }
        else
        {
            return(angle);
        }
    }
    public static double normalizeAngle(double angle)
    {
        return(normalizeAngle(angle, 180));
    }

    //Normalizes latitude/longitude
    public static double normalizeLatitude(double lat)
    {
        return(normalizeAngle(lat, 90));
    }
    public static double normalizeLongitude(double lon)
    {
        return(normalizeAngle(lon, 180));
    }

    //Normalizes and angle to be positive
    public static double normalizePositiveAngle(double angle)
    {
        if(angle < 0)
        {
            return(angle + 360);
        }
        else if(angle >= 360)
        {
            return(angle - 360);
        }
        else
        {
            return(angle);
        }
    }

    //Gets distance between 2 angles
    private static double degreeDistance(double start, double end, double maxDeg)
    {
        double maxDistance = maxDeg * 2;
        double distance = normalizeAngle(end, maxDeg) - normalizeAngle(start, maxDeg);

        if(distance > maxDeg)
        {
            return(distance - maxDistance);
        }
        else if(distance < -maxDeg)
        {
            return(distance + maxDistance);
        }
        else
        {
            return(distance);
        }
    }
    public static double degreeDistance(double start, double end)
    {
        return(degreeDistance(start, end, 180));
    }

    //Gets reversed angle direction from start to end angles
    public static double getReverseAngleDirection(double startAzimuth, double startElevation, double endAzimuth, double endElevation)
    {
        return(180 - Math.toDegrees(Math.atan2(Globals.degreeDistance(endElevation, startElevation), Globals.degreeDistance(endAzimuth, startAzimuth))));
    }

    //Gets distance between 2 latitude/longitude angles
    public static double latitudeDistance(double start, double end)
    {
        return(degreeDistance(start, end, 90));
    }
    public static double longitudeDistance(double start, double end)
    {
        return(degreeDistance(start, end, 180));
    }

    //Gets the unit value for the given km
    public static double getKmUnitValue(double km)
    {
        return(Settings.getUsingMetric() ? km : (km * MILES_PER_KM));
    }

    //Gets the unit label for km
    public static String getKmLabel(Resources res)
    {
        return(res.getString(Settings.getUsingMetric() ? R.string.abbrev_kilometers_lower : R.string.abbrev_miles_lower));
    }

    //Gets the unit value for the given meters
    public static double getMetersUnitValue(double meters)
    {
        return(Settings.getUsingMetric() ? meters : (meters * FEET_PER_METER));
    }

    //Gets the unit label for meters
    public static String getMetersLabel(Resources res)
    {
        return(res.getString(Settings.getUsingMetric() ? R.string.abbrev_meters_lower : R.string.abbrev_feet_lower));
    }

    //Truncates a double to the given decimal places
    public static double truncate(double value, int decimalCount)
    {
        BigDecimal roundValue = new BigDecimal(value);
        roundValue = roundValue.setScale(decimalCount, RoundingMode.FLOOR);
        return(roundValue.doubleValue());
    }

    //Normalizes ASCII file data
    public static String normalizeAsciiFileData(String value)
    {
        int index = 0;
        int length;

        //if no value
        if(value == null)
        {
            //return null
            return(null);
        }

        //remove nulls
        value = value.replace("\0", "");

        //find first valid ASCII char
        length = value.length();
        while(index < length && value.charAt(index) > 127)
        {
            //go to next
            index++;
        }

        //return characters at start of valid
        return(index < length ? value.substring(index) : "");
    }

    //Creates a file and returns URI if successful
    public static Uri createFileUri(Context context, Uri pathUri, String fileName, String extension) throws UnsupportedOperationException
    {
        DocumentFile file;
        DocumentFile filePath;

        //if file name does not end with extension
        if(!fileName.toLowerCase().endsWith(extension.toLowerCase()))
        {
            //add extension
            fileName += extension;
        }

        //try to get file path
        filePath = DocumentFile.fromTreeUri(context, pathUri);
        if(filePath != null)
        {
            //try to create file
            file = filePath.createFile("application/" + extension, fileName);
            if(file != null)
            {
                //return URI
                return(file.getUri());
            }
        }

        //error creating file
        throw(new UnsupportedOperationException(context.getString(R.string.text_file_error_saving)));
    }

    //Reads a text file into a string
    public static String readTextFile(Context context, InputStream fileStream) throws IOException
    {
        int readCount;
        int unReadCount = 4;
        int headerCount = 0;
        BufferedInputStream fileReader;
        PushbackInputStream headerStream = new PushbackInputStream(fileStream, 4);
        String encoding = Encoding.UTF8;
        StringBuilder fileData = new StringBuilder();
        byte[] header = new byte[4];
        byte[] buffer = new byte[WEB_READ_SIZE];

        //if not enough bytes for header
        if(headerStream.read(header) != 4)
        {
            //throw length error error
            throw(new IOException(context.getString(R.string.title_length)));
        }
        else
        {
            //if UTF-32
            if((header[0] == (byte)0x00 && header[1] == (byte)0x00 && header[2] == (byte)0xFE && header[3] == (byte)0xFF) || (header[0] == (byte)0xFF && header[1] == (byte)0xFE && header[2] == (byte)0x00 && header[3] == (byte)0x00))
            {
                encoding = Encoding.UTF32;
                unReadCount = 0;
                headerCount = 4;
            }
            //else UTF-16
            else if((header[0] == (byte)0xFE && header[1] == (byte)0xFF) || (header[0] == (byte)0xFF && header[1] == (byte)0xFE))
            {
                encoding = Encoding.UTF16;
                unReadCount = headerCount = 2;
            }

            //put bytes back in stream
            headerStream.unread(header, 4 - unReadCount, unReadCount);
        }

        //copy header to buffer
        for(readCount = 0; readCount < headerCount; readCount++)
        {
            buffer[readCount] = header[readCount];
        }

        //read file while more data
        fileReader = new BufferedInputStream(headerStream);
        while((readCount = fileReader.read(buffer, headerCount, buffer.length - headerCount)) > 0)
        {
            //add data
            fileData.append(new String(buffer, 0, readCount + headerCount, encoding));
        }
        fileReader.close();
        headerStream.close();

        //return normalized ASCII file data
        return(normalizeAsciiFileData(fileData.toString()));
    }

    //Gets file extension
    public static String getFileExtension(String fileName)
    {
        int index = (fileName != null ? fileName.toLowerCase().indexOf(".") : -1);
        return(index >= 0 ? fileName.substring(index) : "");
    }

    //Creates a file input stream
    public static InputStream createFileInputStream(File file) throws IOException
    {
        if(Build.VERSION.SDK_INT >= 26)
        {
            return(Files.newInputStream(file.toPath()));
        }
        else
        {
            return(new FileInputStream(file));
        }
    }

    //Gets InputStreams from files within a zip file that match any extensions
    private static InputStream[] readZipFile(Context context, String filePath, InputStream fileStream, String[] extensionFilter, int depth)
    {
        int count;
        boolean haveFilter = (extensionFilter != null && extensionFilter.length > 0);
        File cacheDir = new File(context.getCacheDir(), filePath);
        ZipEntry currentEntry;
        ZipInputStream zipStream = new ZipInputStream(fileStream);
        ArrayList<String> filterList = new ArrayList<>(0);
        ArrayList<InputStream> inputList = new ArrayList<>(0);
        byte[] fileBuffer = new byte[1024];

        //make sure cache exists
        //noinspection ResultOfMethodCallIgnored
        cacheDir.mkdir();

        //if there is a filter
        if(haveFilter)
        {
            //create filter list
            for(String currentExtension : extensionFilter)
            {
                //add lower case extension
                filterList.add(currentExtension.toLowerCase());
            }
        }

        try
        {
            //go through files
            while((currentEntry = zipStream.getNextEntry()) != null)
            {
                //remember current file, dir, name, and extension
                String currentName = currentEntry.getName();
                String currentExtension = getFileExtension(currentName).toLowerCase();
                File currentFile = new File(cacheDir, currentName);
                File currentDir = (currentEntry.isDirectory() ? currentFile : currentFile.getParentFile());
                boolean isZip = currentExtension.equals(".zip");

                //if -directory exists or able to make- and -a file-
                if(currentDir != null && (currentDir.isDirectory() || currentDir.mkdirs()) && !currentEntry.isDirectory())
                {
                    //use if no filter or extension is in list
                    boolean useExtension = (!haveFilter || filterList.contains(currentExtension));

                    //if a .zip file or using extension
                    if(isZip || useExtension)
                    {
                        //create file
                        FileOutputStream outStream = new FileOutputStream(currentFile);
                        while((count = zipStream.read(fileBuffer)) > 0)
                        {
                            //write bytes to file
                            outStream.write(fileBuffer, 0, count);
                        }
                        outStream.flush();
                        outStream.close();

                        //if an embedded .zip file
                        if(isZip)
                        {
                            //if not too deep
                            if(depth <= ZIP_FILE_MAX_DEPTH)
                            {
                                //add all filtered files inside
                                inputList.addAll(Arrays.asList(readZipFile(context, filePath + "/" + currentFile.getName() + depth, Globals.createFileInputStream(currentFile), extensionFilter, depth++)));
                            }
                        }

                        //if using extension
                        if(useExtension)
                        {
                            //add input stream to list
                            inputList.add(Globals.createFileInputStream(currentFile));
                        }
                    }
                }
            }
            zipStream.close();
        }
        catch(Exception ex)
        {
            //do nothing
        }

        return(inputList.toArray(new InputStream[0]));
    }
    public static InputStream[] readZipFile(Context context, String filePath, InputStream fileStream, String[] extensionFilter)
    {
        return(readZipFile(context, filePath, fileStream, extensionFilter, 1));
    }

    //Tries to reads the given text files into strings
    public static ArrayList<String> readTextFiles(Context context, ArrayList<Uri> fileUris) throws Exception
    {
        int index;
        InputStream fileStream;
        ContentResolver resolver = context.getContentResolver();
        ArrayList<String> fileAscii = new ArrayList<>(0);

        //go through each file
        for(index = 0; index < fileUris.size(); index++)
        {
            //remember current file, path, and extension
            Uri currentFile = fileUris.get(index);
            String currentPath = currentFile.getPath();
            String currentExtension = getFileExtension(currentPath);

            //if current scheme has content
            if(currentFile.getScheme().equals("content"))
            {
                //if able to get cursor
                Cursor currentCursor = resolver.query(currentFile, null, null, null, null);
                if(currentCursor != null)
                {
                    //if able to get first row
                    if(currentCursor.moveToFirst())
                    {
                        //if able to get display name
                        int nameIndex = currentCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        String cursorResult = (nameIndex >= 0 ? currentCursor.getString(nameIndex) : null);
                        if(cursorResult != null)
                        {
                            //update path and extension
                            currentPath = cursorResult;
                            currentExtension = getFileExtension(currentPath);
                        }
                    }
                    currentCursor.close();
                }
            }

            //read file data
            fileStream = resolver.openInputStream(fileUris.get(index));
            if(currentExtension.startsWith(".zip"))
            {
                //get all usable files in the .zip file
                InputStream[] zipFileStreams = readZipFile(context, "files", fileStream, fileDataExtensions);
                for(InputStream currentFileStream : zipFileStreams)
                {
                    //add file data
                    fileAscii.add(readTextFile(context, currentFileStream));
                    currentFileStream.close();
                }
            }
            else if(currentExtension.startsWith(".tle") || currentExtension.startsWith(".json") || currentExtension.startsWith(".txt"))
            {
                //add file data
                fileAscii.add(readTextFile(context, fileStream));
            }
            fileStream.close();
        }

        return(fileAscii);
    }
    public static ArrayList<String> readTextFiles(Context context, ArrayList<String> fileNames, ArrayList<File> files) throws Exception
    {
        int index;
        ArrayList<String> fileAscii = new ArrayList<>(0);

        //go through each file
        for(index = 0; index < fileNames.size(); index++)
        {
            //remember file name, extension, and data
            String currentName = fileNames.get(index);
            String currentExtension = getFileExtension(currentName);
            byte[] currentData = readFile(files.get(index));

            //if a .zip
            if(currentExtension.equalsIgnoreCase(".zip"))
            {
                //get all usable files in the .zip file
                InputStream[] zipFileStreams = readZipFile(context, "files", new ByteArrayInputStream(currentData), fileDataExtensions);
                for(InputStream currentFileStream : zipFileStreams)
                {
                    //add file data
                    fileAscii.add(readTextFile(context, currentFileStream));
                    currentFileStream.close();
                }
            }
            else
            {
                //try to get text file from data
                fileAscii.add(readTextFile(context, new ByteArrayInputStream(currentData)));
            }
        }

        return(fileAscii);
    }

    //Saves a file
    public static void saveFile(File file, byte[] data)
    {
        FileOutputStream outputStream;

        try
        {
            //if file exists
            if(file.exists())
            {
                //if unable to delete and can't write
                if(!file.delete() && !file.canWrite())
                {
                    //stop
                    return;
                }
            }

            //try to create file
            if(file.createNewFile())
            {
                //write data
                outputStream = new FileOutputStream(file);
                outputStream.write(data);
                outputStream.close();
            }
        }
        catch(Exception ex)
        {
            //failed
        }
    }

    //Reads a file
    public static byte[] readFile(File file)
    {
        InputStream inputStream;
        byte[] data;

        try
        {
            data = new byte[(int)file.length()];
            inputStream = Globals.createFileInputStream(file);

            //noinspection ResultOfMethodCallIgnored
            inputStream.read(data);
            inputStream.close();
            return(data);
        }
        catch(Exception ex)
        {
            //failed
            return(new byte[0]);
        }
    }

    //Gets the given number of stars
    public static String getStars(int count)
    {
        int index;
        StringBuilder stars = new StringBuilder();

        //while more stars
        for(index = 0; index < count; index++)
        {
            //add a star
            stars.append(Symbols.Star);
        }

        //return stars
        return(stars.toString());
    }

    //Gets a list
    public static ArrayList<Integer> getList(int[] values)
    {
        ArrayList<Integer> list = new ArrayList<>(values.length);
        for(int currentValue : values)
        {
            list.add(currentValue);
        }
        return(list);
    }

    /** @noinspection CharsetObjectCanBeUsed*/
    //Encodes a url string value
    public static String encodeUrlValue(String urlValue)
    {
        try
        {
            return(URLEncoder.encode(urlValue, Encoding.UTF8));
        }
        catch(Exception ex)
        {
            return(urlValue);
        }
    }

    //Converts a text string into an HTML string
    public static Spanned stringToHtml(String stringValue)
    {
        return((Build.VERSION.SDK_INT >= 24 ? Html.fromHtml(stringValue, Html.FROM_HTML_MODE_LEGACY) : Html.fromHtml(stringValue)));
    }

    //Decodes html special characters
    private static String decodeHtml(String htmlString)
    {
        String result;

        result = Html.fromHtml(stringToHtml(htmlString).toString()).toString();
        return(result.replace("< ", "<").replace(" >", ">"));
    }

    //Tries to get updates protocols for web use
    public static void updateWebProtocols(Context context)
    {
        try
        {
            ProviderInstaller.installIfNeeded(context);
        }
        catch(Exception ex)
        {
            //failed
        }
    }

    //Reads web page from stream
    private static String readWebPage(BufferedReader streamReader, long totalBytes, OnProgressChangedListener listener) throws Exception
    {
        int readCount;
        long bytesReceived = 0;
        char[] readBuffer = new char[WEB_READ_SIZE];
        StringBuilder streamString = new StringBuilder();

        //if reader is set
        if(streamReader != null)
        {
            //while there is data to read
            while((readCount = streamReader.read(readBuffer, 0, WEB_READ_SIZE)) > 0)
            {
                //add data
                streamString.append((new String(readBuffer, 0, readCount)));
                bytesReceived += readCount;
                if(listener != null)
                {
                    listener.onProgressChanged(ProgressType.Success, null, bytesReceived, totalBytes);
                }
            }
        }

        //if listener is set
        if(listener != null)
        {
            //update progress
            listener.onProgressChanged(ProgressType.Finished, null, bytesReceived, totalBytes);
        }

        //return data
        return(streamString.toString());
    }

    //Tries to get a web page
    private static WebPageData getWebPage(String urlString, String[] postNames, String[] postValues, String postData, String authToken, String contentType, OnProgressChangedListener listener, int retryCount, int maxRetryCount)
    {
        int index;
        int responseCode = -1;
        long totalBytes;
        String message;
        String pageString = "";
        HttpUrl siteURL;
        ResponseBody body;
        OkHttpClient client;
        OkHttpClient.Builder clientBuilder;
        InputStream dataStream;
        FormBody.Builder postBuilder;
        BufferedReader streamReader;
        Request.Builder siteRequestBuilder;
        Response siteHttpsConnection = null;

        //if listener is set
        if(listener != null)
        {
            //update progress
            listener.onProgressChanged(ProgressType.Started, null, 0, 0);
        }

        try
        {
            //set page
            siteURL = HttpUrl.parse(urlString);
            if(siteURL == null)
            {
                throw(new Exception("Could not parse url"));
            }

            //get page
            siteRequestBuilder = new Request.Builder().url(siteURL);
            siteRequestBuilder.addHeader("User-Agent", "Mozilla/5.0");
            if(authToken != null)
            {
                //noinspection SpellCheckingInspection
                siteRequestBuilder.addHeader("Ocp-Apim-Subscription-Key", authToken);
                siteRequestBuilder.addHeader("Content-Type", contentType);
            }
            if(postNames != null && postValues != null && postNames.length > 0 && postNames.length == postValues.length)
            {
                postBuilder = new FormBody.Builder();
                for(index = 0; index < postNames.length; index++)
                {
                    postBuilder.add(postNames[index], postValues[index]);
                }
                siteRequestBuilder.post(postBuilder.build());
            }
            if(postData != null)
            {
                siteRequestBuilder.post(RequestBody.create(postData, MediaType.parse(contentType)));
            }
            clientBuilder = new OkHttpClient.Builder().writeTimeout(12, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS);
            clientBuilder.cookieJar(cookies);
            client = clientBuilder.build();
            siteHttpsConnection = client.newCall(siteRequestBuilder.build()).execute();
            responseCode = siteHttpsConnection.code();
            body = siteHttpsConnection.body();
            if(body == null)
            {
                throw(new Exception("No data"));
            }
            dataStream = body.byteStream();
            totalBytes = body.contentLength();

            //read web page
            streamReader = new BufferedReader(new InputStreamReader(dataStream));
            pageString = readWebPage(streamReader, totalBytes, listener);
            streamReader.close();

            //return page
            return(new WebPageData(siteHttpsConnection, pageString, responseCode));
        }
        catch(Exception ex)
        {
            //get message
            message = ex.getMessage();

            //if an authentication problem
            if(message != null && message.toLowerCase().contains("authentication"))
            {
                //authentication error, stop
                responseCode = 401;
                retryCount = maxRetryCount;
            }

            //if before maximum retries
            if(retryCount < maxRetryCount)
            {
                //try to get web page again
                return(getWebPage(urlString, postNames, postValues, postData, authToken, contentType, listener, retryCount + 1, maxRetryCount));
            }
            else
            {
                //if listener is set
                if(listener != null)
                {
                    //update progress
                    listener.onProgressChanged(ProgressType.Failed, null, -1, -1);
                }

                //if connection exists
                if(siteHttpsConnection != null)
                {
                    //try to get error
                    try
                    {
                        pageString = siteHttpsConnection.message();
                    }
                    catch(Exception ex2)
                    {
                        pageString = "";
                    }
                }

                //nothing
                return(new WebPageData(null, pageString, responseCode));
            }
        }
    }
    private static WebPageData getWebPage(String urlString, String[] postNames, String[] postValues, String outString, OnProgressChangedListener listener, int retryCount, int maxRetryCount)
    {
        return(getWebPage(urlString, postNames, postValues, outString, null, "text/xml; charset=utf-8", listener, retryCount, maxRetryCount));
    }
    public static WebPageData getWebPage(String urlString, String[] postNames, String[] postValues)
    {
        //try up to 3 times to get web page
        return(getWebPage(urlString, postNames, postValues, null, null, "text/xml; charset=utf-8", null, 1, 3));
    }
    public static WebPageData getWebPage(String urlString, String[] postNames, String[] postValues, OnProgressChangedListener listener)
    {
        //try up to 3 times to get web page
        return(getWebPage(urlString, postNames, postValues, null, listener, 1, 3));
    }
    public static WebPageData getWebPage(String urlString, String outString, OnProgressChangedListener listener)
    {
        //try up to 3 times to get web page
        return(getWebPage(urlString, null, null, outString, listener, 1, 3));
    }
    public static String getWebPage(String urlString, boolean closeConnection, OnProgressChangedListener listener)
    {
        WebPageData webData = getWebPage(urlString, null, listener);

        if(closeConnection && webData.connection != null)
        {
            webData.connection.close();
        }

        return(webData.pageData);
    }
    public static WebPageData getWebPage(String urlString)
    {
        return(getWebPage(urlString, null, (OnProgressChangedListener)null));
    }

    //Tries to load multiple json objects from given json input
    public static JSONObject[] getJsonObjects(String jsonInput)
    {
        int row;
        JSONArray data;
        ArrayList<JSONObject> dataList = new ArrayList<>(0);

        try
        {
            //get array
            data = new JSONArray(jsonInput);

            //go through each row
            for(row = 0; row < data.length(); row++)
            {
                //add row
                dataList.add(data.getJSONObject(row));
            }
        }
        catch(Exception ex)
        {
            //do nothing
        }

        return(dataList.toArray(new JSONObject[0]));
    }

    //Tries to get a JSON  web page
    public static JSONObject getJSONWebPage(String urlString)
    {
        WebPageData webData = getWebPage(urlString, null, null, null, null, 0, 0);

        if(webData.connection != null)
        {
            webData.connection.close();
        }

        try
        {
            return(new JSONObject(webData.pageData));
        }
        catch(Exception ex)
        {
            return(null);
        }
    }

    //Tries to login to space-track
    public static WebPageData loginSpaceTrack(String user, String pwd)
    {
        //if no user and/or password
        if(user == null || pwd == null || user.trim().isEmpty() || pwd.trim().isEmpty())
        {
            //return invalid
            return(new WebPageData(null, null, 401));
        }
        else
        {
            //login to space track
            return(getWebPage("https://www.space-track.org/ajaxauth/login", new String[]{"identity", "password"}, new String[]{user, pwd}));
        }
    }

    //Shows a space-track login
    private static void showAccountLogin(Activity context, int accountType, int updateType, WebPageListener listener, boolean retrying)
    {
        //if context exists and not closing
        if(context != null && !context.isFinishing())
        {
            //get current user and password
            String[] loginData = Settings.getLogin(context, accountType);

            //try to login
            (new LoginTask((updateType == UpdateService.UpdateType.UpdateCount), retrying, listener)).execute(context, accountType, updateType, loginData[0], loginData[1], true, updateType);
        }
    }
    public static void showAccountLogin(Activity context, int accountType, WebPageListener listener)
    {
        showAccountLogin(context, accountType, UpdateService.UpdateType.UpdateCount, listener, false);
    }
    public static void showAccountLogin(Activity context, int accountType, int updateType, WebPageListener listener)
    {
        showAccountLogin(context, accountType, updateType, listener, true);
    }

    //Translates text with context language
    public static void translateText(Context context, String text, TranslateListener listener)
    {
        new TranslateTask(listener).execute(context, text);
    }

    //Gets pre-saved online translation
    public static String getSavedOnlineTranslation(Context context, String value, int source, String language)
    {
        String data;
        String key = context.getResources().getString(R.string.nikolai_apps_translate_key);
        WebPageData translatedData = getWebPage("https://jnikolai.dev/query/translate.php?val=" + value + "&lan=" + language + "&src=" + source +"&key=" + key, null, null, null);
        JSONObject rootNode;

        //if got data and valid
        if(translatedData.gotData() && translatedData.isOkay())
        {
            try
            {
                //if got response
                rootNode = new JSONObject(translatedData.pageData);
                if(rootNode.getString("status").equals("ok"))
                {
                    //get data
                    data = rootNode.getString("data");
                    if(!data.equals("null"))
                    {
                        //return data
                        return(data);
                    }
                }
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }

        //invalid/not found
        return(null);
    }

    //Saves online translation
    public static void saveOnlineTranslation(Context context, String value, int source, String language, String text)
    {
        String key = context.getResources().getString(R.string.nikolai_apps_translate_key);

        //try to save without waiting for a response
        getWebPage("https://jnikolai.dev/query/translate.php?val=" + value + "&lan=" + language + "&src=" + source +"&key=" + key, new String[]{"data"}, new String[]{text}, null);
    }

    //Tries to logout of space track
    public static void logoutSpaceTrack(OnProgressChangedListener listener)
    {
        WebPageData pageData = getWebPage("https://www.space-track.org/ajaxauth/logout", null, null, listener);
        if(pageData.connection != null)
        {
            pageData.connection.close();
            pageData.connection = null;
        }
    }
}
