package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.android.Auth;
import com.dropbox.core.util.IOUtil;
import com.dropbox.core.util.ProgressOutputStream;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class DropboxAccess extends AppCompatActivity implements ActivityResultCallback<ActivityResult>
{
    private static final String DROPBOX_SETTINGS = "dropboxSettings";

    private static abstract class PreferenceName
    {
        static final String AccessToken = "accessToken";
        static final String UserId = "userId";
        static final String UserEmail = "userEmail";
    }

    private static class Item extends FileBrowserBaseActivity.ItemBase
    {
        private final String path;
        private final String name;
        private final boolean isDir;

        public Item(String dirPath)
        {
            name = path = dirPath;
            isDir = true;
        }
        public Item(Metadata setItem)
        {
            name = setItem.getName();
            path = setItem.getPathDisplay();
            isDir = (setItem instanceof FolderMetadata);
        }

        @Override
        public boolean isDirectory()
        {
            return(isDir);
        }

        @Override
        public String getId()
        {
            return(path);
        }

        @Override
        public String getName()
        {
            return(name);
        }

        @Override
        public String getFullName()
        {
            return(name);
        }

        @Override
        public String getPath()
        {
            return(path);
        }

        @Override
        public String getPathId()
        {
            return(path);
        }

        @Override
        public String getAbsolutePath()
        {
            return(path);
        }

        @Override
        public void getList(final FileBrowserBaseActivity.OnGotListListener listener)
        {
            GetItemsTask task = new GetItemsTask(client, new GetItemsTask.OnResultListener()
            {
                @Override
                public void onResult(int resultCode, Item[] items, String message)
                {
                    listener.onGotList(items, null);
                }
            });
            task.execute(path);
        }

        @Override
        public FileBrowserBaseActivity.ItemBase getParentFolder(String parentId)
        {
            int index = (path != null ? path.lastIndexOf("/") : -1);
            String parentPath = "";

            if(index >= 1)
            {
                parentPath = path.substring(0, index);
            }
            else if(path != null && path.isEmpty())
            {
                return(null);
            }

            return(new Item(parentPath));
        }
    }

    private static class GetItemsTask extends ThreadTask<Object, Integer, Void>
    {
        public interface OnResultListener
        {
            void onResult(int resultCode, Item[] items, String message);
        }

        private final DbxClientV2 dbClient;
        private final OnResultListener resultListener;

        public GetItemsTask(DbxClientV2 useClient, OnResultListener listener)
        {
            dbClient = useClient;
            resultListener = listener;
        }

        @Override
        protected Void doInBackground(Object... objects)
        {
            int index = 0;
            int taskResult;
            String message = null;
            String path = (String)objects[0];
            Item[] items = null;

            try
            {
                //try to get items
                List<Metadata> itemList = dbClient.files().listFolder(path).getEntries();
                items = new Item[itemList.size()];

                //go through each item
                for(Metadata currentItem : itemList)
                {
                    items[index] = new Item(currentItem);
                    index++;
                }

                //success
                taskResult = FileBrowserBaseActivity.ResultCode.Success;
            }
            catch(InvalidAccessTokenException ex)
            {
                //failed to login
                taskResult = FileBrowserBaseActivity.ResultCode.LoginFailed;
                message = ex.getMessage();
            }
            catch(Exception ex)
            {
                //unknown error
                taskResult = FileBrowserBaseActivity.ResultCode.Error;
                message = ex.getMessage();
            }

            //if listener is set
            if(resultListener != null)
            {
                //send event
                resultListener.onResult(taskResult, items, message);
            }

            //end task
            return(null);
        }
    }

    private static class SaveFileTask extends ThreadTask<Object, Integer, Void>
    {
        private final DbxClientV2 dbClient;
        private final FileBrowserBaseActivity.OnProgressListener saveListener;
        private final FileBrowserBaseActivity.OnResultListener resultListener;

        public SaveFileTask(DbxClientV2 useClient, FileBrowserBaseActivity.OnProgressListener svListener, FileBrowserBaseActivity.OnResultListener resListener)
        {
            dbClient = useClient;
            saveListener = svListener;
            resultListener = resListener;
        }

        @Override
        protected Void doInBackground(Object... objects)
        {
            int taskResult;
            String message = null;
            Context context = (Context)objects[0];
            String path = (String)objects[1];
            File fileName = (File)objects[2];
            InputStream inStream;

            try
            {
                final long totalBytes = fileName.length();

                //upload file
                inStream = Globals.createFileInputStream(fileName);
                dbClient.files().uploadBuilder(path + "/" + fileName.getName()).withMode(WriteMode.OVERWRITE).uploadAndFinish(inStream, new IOUtil.ProgressListener()
                {
                    @Override
                    public void onProgress(long bytesWritten)
                    {
                        if(saveListener != null)
                        {
                            saveListener.onProgressChanged(0, 1, bytesWritten, totalBytes, totalBytes > 0 ? ((bytesWritten / (double)totalBytes) * 100) : 0);
                        }
                    }
                });

                //success
                taskResult = FileBrowserBaseActivity.ResultCode.Success;

                //try to delete temp file
                if(!fileName.delete())
                {
                    //error deleting, but okay
                    message = context.getResources().getString(R.string.text_old_file_delete_failed);
                }
            }
            catch(InvalidAccessTokenException ex)
            {
                //failed to login
                taskResult = FileBrowserBaseActivity.ResultCode.LoginFailed;
                message = ex.getMessage();
            }
            catch(Exception ex)
            {
                //error saving
                taskResult = FileBrowserBaseActivity.ResultCode.Error;
                message = ex.getMessage();
            }

            //if listener is set
            if(resultListener != null)
            {
                //send event
                resultListener.onResult(taskResult, null, null, message);
            }

            //end task
            return(null);
        }
    }

    private static class DownloadFilesTask extends ThreadTask<Object, Integer, Void>
    {
        private final DbxClientV2 dbClient;
        private final FileBrowserBaseActivity.OnResultListener resultListener;
        private final FileBrowserBaseActivity.OnProgressListener downloadListener;

        public DownloadFilesTask(DbxClientV2 useClient, FileBrowserBaseActivity.OnProgressListener dlListener, FileBrowserBaseActivity.OnResultListener resListener)
        {
            dbClient = useClient;
            downloadListener = dlListener;
            resultListener = resListener;
        }

        @Override
        protected Void doInBackground(Object... objects)
        {
            int fileIndex;
            int taskResult;
            String message = null;
            ArrayList<String> filesDataNames = new ArrayList<>(0);
            ArrayList<byte[]> filesData = new ArrayList<>(0);
            String[] fileNames = (String[])objects[0];

            try
            {
                //get total files
                final double totalFiles = fileNames.length;

                //go through each file name
                for(fileIndex = 0; fileIndex < totalFiles; fileIndex++)
                {
                    //download file
                    final int index = fileIndex;
                    final String currentFileName = fileNames[fileIndex];
                    final DbxDownloader<FileMetadata> download = dbClient.files().download(currentFileName.toLowerCase());
                    ByteArrayOutputStream fileOutput = new ByteArrayOutputStream();
                    download.download(new ProgressOutputStream(fileOutput, new IOUtil.ProgressListener()
                    {
                        final long totalBytes = download.getResult().getSize();

                        @Override
                        public void onProgress(long bytesWritten)
                        {
                            if(downloadListener != null)
                            {
                                downloadListener.onProgressChanged(index, (int)totalFiles, bytesWritten, totalBytes, ((index / totalFiles) + ((totalBytes > 0 ? (bytesWritten / (double)totalBytes) : 0) / totalFiles)) * 100);
                            }
                        }
                    }));

                    //read file, add name, then close
                    filesData.add(fileOutput.toByteArray());
                    filesDataNames.add(currentFileName);
                    fileOutput.close();
                }

                //success
                taskResult = FileBrowserBaseActivity.ResultCode.Success;
            }
            catch(InvalidAccessTokenException ex)
            {
                //failed to login
                taskResult = FileBrowserBaseActivity.ResultCode.LoginFailed;
                message = ex.getMessage();
            }
            catch(Exception ex)
            {
                //error downloading
                taskResult = FileBrowserBaseActivity.ResultCode.Error;
                message = ex.getMessage();
            }

            //if listener is set
            if(resultListener != null)
            {
                //send event
                resultListener.onResult(taskResult, filesDataNames, filesData, message);
            }

            //end task
            return(null);
        }
    }

    private static class GetEmailTask extends ThreadTask<Object, Integer, Void>
    {
        public interface OnResultListener
        {
            void onResult(String email);
        }

        private final DbxClientV2 dbClient;
        private final OnResultListener resultListener;

        public GetEmailTask(DbxClientV2 useClient, OnResultListener listener)
        {
            dbClient = useClient;
            resultListener = listener;
        }

        @Override
        protected Void doInBackground(Object... objects)
        {
            String email;

            //try to get email
            try
            {
                email = dbClient.users().getCurrentAccount().getEmail();
            }
            catch(Exception ex)
            {
                email = null;
            }

            //if listener is set
            if(resultListener != null)
            {
                resultListener.onResult(email);
            }

            //end task
            return(null);
        }
    }

    public static class BrowserActivity extends FileBrowserBaseActivity
    {
        public static abstract class ParamTypes extends FileBrowserBaseActivity.ParamTypes
        {
            static final String RootFolder = "rootFolder";
        }

        private static class FileListAdapter extends FileListAdapterBase
        {
            public FileListAdapter(FileBrowserBaseActivity activity, String startDir, boolean selectFolder)
            {
                super(activity, new Item(""), new Item(startDir), true, selectFolder);
            }
        }

        @Override
        protected FileListAdapterBase onCreateAdapter(Intent intent, boolean selectFolder)
        {
            String rootFolder = intent.getStringExtra(ParamTypes.RootFolder);

            //setup list
            return(new FileListAdapter(this, rootFolder, selectFolder));
        }
    }

    private static DbxClientV2 client;

    private boolean showList;
    private boolean haveResumed;
    private boolean selectFolder;
    private boolean loginOnly;
    private int itemCount;
    private byte requestCode;
    private File saveFile;
    private ActivityResultLauncher<Intent> resultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        String accessToken = getAccessToken();
        String userId = getUserId();
        Intent intent = this.getIntent();

        //if intent not set
        if(intent == null)
        {
            //create empty
            intent = new Intent();
        }

        //have not resumed yet
        haveResumed = false;

        //get params
        selectFolder = intent.getBooleanExtra(FileBrowserBaseActivity.ParamTypes.SelectFolder, false);
        saveFile = (File)intent.getSerializableExtra(FileBrowserBaseActivity.ParamTypes.FileName);
        itemCount = intent.getIntExtra(FileBrowserBaseActivity.ParamTypes.ItemCount, 0);
        loginOnly = intent.getBooleanExtra(FileBrowserBaseActivity.ParamTypes.LoginOnly, false);
        requestCode = BaseInputActivity.getRequestCode(intent);

        //want to show list if not logging in only
        showList = !loginOnly;

        //create receiver
        resultLauncher = Globals.createActivityLauncher(this, this);

        //if -logging in only- or -token or user ID not set-
        if(loginOnly || accessToken == null || userId == null)
        {
            //get permission
            getPermission(false);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        String userId;
        String accessToken = getAccessToken();
        GetEmailTask emailTask;
        Intent resultData = new Intent();

        //set defaults
        BaseInputActivity.setRequestCode(resultData, requestCode);

        //if no token
        if(accessToken == null)
        {
            //if have resumed
            if(haveResumed)
            {
                //get token
                accessToken = Auth.getOAuth2Token();
            }

            //if token is set
            if(accessToken != null)
            {
                //use it
                setup(accessToken);
            }
        }
        else
        {
            //use it
            setup(accessToken);
        }

        //if have resumed
        if(haveResumed)
        {
            //get user ID
            userId = Auth.getUid();
            if(userId != null && !userId.equals(getUserId()))
            {
                //update user ID
                setUserId(userId);
            }

            //if client and token exist
            if(client != null && accessToken != null)
            {
                //get user email
                emailTask = new GetEmailTask(client, new GetEmailTask.OnResultListener()
                {
                    @Override
                    public void onResult(String email)
                    {
                        //if email exists and different than stored
                        if(email != null && !email.equals(getUserEmail(DropboxAccess.this)))
                        {
                            //update user email
                            setUserEmail(email);
                        }

                        //if logging in only
                        if(loginOnly)
                        {
                            //finish
                            setResult(email != null ? RESULT_OK : RESULT_CANCELED, resultData);
                            DropboxAccess.this.finish();
                        }
                    }
                });
                emailTask.execute();
            }
            //else if logging in only
            else if(loginOnly)
            {
                //finish
                setResult(RESULT_CANCELED, resultData);
                DropboxAccess.this.finish();
            }
        }

        //if token not set and already resumed
        if(accessToken == null && haveResumed)
        {
            //cancel
            this.finish();
        }
        //else if showing list
        else if(showList)
        {
            //get items in root
            getRootItems();
        }

        //have resumed
        haveResumed = true;
    }

    @Override
    public void onActivityResult(ActivityResult result)
    {
        int resultCode = result.getResultCode();
        byte localRequestCode;
        boolean isOkay = (resultCode == RESULT_OK);
        String folderName;
        ArrayList<String> fileIds;
        Intent data = result.getData();
        final Intent resultData = new Intent();

        //if no data
        if(data == null)
        {
            //set to empty
            data = new Intent();
        }

        //get local request code
        localRequestCode = BaseInputActivity.getRequestCode(data);

        //set defaults
        BaseInputActivity.setRequestCode(resultData, requestCode);

        switch(localRequestCode)
        {
            case BaseInputActivity.RequestCode.DropboxOpenFolder:
            case BaseInputActivity.RequestCode.DropboxOpenFile:
                //if set
                if(isOkay)
                {
                    //add progress bar
                    final LinearProgressIndicator progressBar = new LinearProgressIndicator(this);
                    final TextView progressText = Globals.showSnackBarProgress(this.findViewById(android.R.id.content), progressBar);
                    final Resources res = this.getResources();

                    //don't want to show list
                    showList = false;

                    //if getting folder
                    if(localRequestCode == BaseInputActivity.RequestCode.DropboxOpenFolder)
                    {
                        //get folder and save file
                        folderName = data.getStringExtra(FileBrowserBaseActivity.ParamTypes.FolderName);
                        saveFile(folderName, saveFile, new FileBrowserBaseActivity.OnProgressListener()
                        {
                            @Override
                            public void onProgressChanged(int index, int length, long bytes, long totalBytes, double progress)
                            {
                                FileBrowserBaseActivity.updateProgress(DropboxAccess.this, res, progressBar, progressText, index, length, bytes, totalBytes, progress);
                            }
                        }
                        , new FileBrowserBaseActivity.OnResultListener()
                        {
                            @Override
                            public void onResult(int resultCode, ArrayList<String> fileNames, ArrayList<byte[]> filesData, String message)
                            {
                                //pass information back to caller
                                int passedResultCode = (resultCode == FileBrowserBaseActivity.ResultCode.Success ? RESULT_OK : RESULT_CANCELED);
                                resultData.putExtra(FileBrowserBaseActivity.ParamTypes.ItemCount, itemCount);
                                setResult(passedResultCode, resultData);
                                DropboxAccess.this.finish();
                            }
                        });
                    }
                    else
                    {
                        //get files
                        fileIds = data.getStringArrayListExtra(FileBrowserBaseActivity.ParamTypes.FileIds);
                        getFiles(fileIds, new FileBrowserBaseActivity.OnProgressListener()
                        {
                            @Override
                            public void onProgressChanged(int index, int length, long bytes, long totalBytes, double progress)
                            {
                                FileBrowserBaseActivity.updateProgress(DropboxAccess.this, res, progressBar, progressText, index, length, bytes, totalBytes, progress);
                            }
                        }, new FileBrowserBaseActivity.OnResultListener()
                        {
                            @Override
                            public void onResult(int resultCode, ArrayList<String> fileNames, ArrayList<byte[]> filesData, String message)
                            {
                                int index;
                                int count = filesData.size();
                                Activity activity = DropboxAccess.this;

                                //pass information back to caller
                                int passedResultCode = (resultCode == FileBrowserBaseActivity.ResultCode.Success ? RESULT_OK : RESULT_CANCELED);
                                resultData.putStringArrayListExtra(FileBrowserBaseActivity.ParamTypes.FileNames, fileNames);
                                resultData.putExtra(FileBrowserBaseActivity.ParamTypes.FilesDataCount, count);
                                for(index = 0; index < count; index++)
                                {
                                    //save file and add it
                                    File cacheFile = new File(activity.getCacheDir(), "filesData" + index + ".txt");
                                    Globals.saveFile(cacheFile, filesData.get(index));
                                    resultData.putExtra(FileBrowserBaseActivity.ParamTypes.Files + index, cacheFile);
                                }
                                setResult(passedResultCode, resultData);
                                activity.finish();
                            }
                        });
                    }
                }
                else
                {
                    //pass information back to caller
                    setResult(resultCode, resultData);
                    this.finish();
                }
                break;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
    }

    //Gets permission
    private void getPermission(boolean reset)
    {
        //if resetting
        if(reset)
        {
            //clear token
            setAccessToken(null);
        }

        //get permission
        Auth.startOAuth2Authentication(this, getResources().getString(R.string.dropbox_api_key));
    }

    //Sets up the login with given token
    private void setup(String tokenValue)
    {
        DbxRequestConfig requestConfig = new DbxRequestConfig(getApplicationContext().getPackageName());

        setAccessToken(tokenValue);
        client = new DbxClientV2(requestConfig, tokenValue);
    }

    //Gets settings preferences
    private static SharedPreferences getPreferences(Context context)
    {
        return(context.getSharedPreferences(DROPBOX_SETTINGS, MODE_PRIVATE));
    }

    //Gets write settings
    private static SharedPreferences.Editor getWriteSettings(Context context)
    {
        return(getPreferences(context).edit());
    }

    //Gets access token
    private String getAccessToken()
    {
        return(getPreferences(this).getString(PreferenceName.AccessToken, null));
    }

    //Sets access token
    private void setAccessToken(String tokenValue)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(this);
        writeSettings.putString(PreferenceName.AccessToken, tokenValue);
        writeSettings.apply();
    }

    //Gets UID
    private String getUserId()
    {
        return(getPreferences(this).getString(PreferenceName.UserId, null));
    }

    //Sets user ID
    private void setUserId(String userId)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(this);
        writeSettings.putString(PreferenceName.UserId, userId);
        writeSettings.apply();
    }

    //Gets user email
    public static String getUserEmail(Context context)
    {
        return(getPreferences(context).getString(PreferenceName.UserEmail, null));
    }

    //Sets user email
    private void setUserEmail(String email)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(this);
        writeSettings.putString(PreferenceName.UserEmail, email);
        writeSettings.apply();
    }

    //Removes current account
    public static void removeAccount(Context context)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);
        writeSettings.remove(PreferenceName.AccessToken);
        writeSettings.remove(PreferenceName.UserId);
        writeSettings.remove(PreferenceName.UserEmail);
        writeSettings.apply();

        client = null;
    }

    //Gets items in the root folder
    private void getRootItems()
    {
        GetItemsTask task = new GetItemsTask(client, new GetItemsTask.OnResultListener()
        {
            @Override
            public void onResult(int resultCode, Item[] items, String message)
            {
                switch(resultCode)
                {
                    case FileBrowserBaseActivity.ResultCode.Success:
                        Intent intent = new Intent(DropboxAccess.this, BrowserActivity.class);
                        intent.putExtra(BrowserActivity.ParamTypes.RootFolder, "");
                        intent.putExtra(BrowserActivity.ParamTypes.SelectFolder, selectFolder);
                        Globals.startActivityForResult(resultLauncher, intent, (selectFolder ? BaseInputActivity.RequestCode.DropboxOpenFolder : BaseInputActivity.RequestCode.DropboxOpenFile));
                        break;

                    case FileBrowserBaseActivity.ResultCode.LoginFailed:
                        getPermission(true);
                        break;
                }
            }
        });
        task.execute("");
    }

    //Gets the given files
    private void getFiles(ArrayList<String> fileNames, FileBrowserBaseActivity.OnProgressListener dlListener, FileBrowserBaseActivity.OnResultListener resListener)
    {
        DownloadFilesTask task = new DownloadFilesTask(client, dlListener, resListener);

        if(fileNames == null)
        {
            fileNames = new ArrayList<>(0);
        }
        task.execute((Object)fileNames.toArray(new String[0]));
    }

    //Save the given file
    private void saveFile(String path, File fileName, FileBrowserBaseActivity.OnProgressListener svListener, FileBrowserBaseActivity.OnResultListener resListener)
    {
        SaveFileTask task = new SaveFileTask(client, svListener, resListener);
        task.execute(this.getApplicationContext(), path, fileName);
    }

    //Starts an instance
    public static void start(Activity activity, ActivityResultLauncher<Intent> launcher, File fileName, int count, boolean selectFolder)
    {
        boolean useFile = (fileName != null);
        Intent intent = new Intent(activity, DropboxAccess.class);

        intent.putExtra(FileBrowserBaseActivity.ParamTypes.SelectFolder, selectFolder);
        if(useFile)
        {
            intent.putExtra(FileBrowserBaseActivity.ParamTypes.FileName, fileName);
        }
        intent.putExtra(FileBrowserBaseActivity.ParamTypes.ItemCount, count);
        Globals.startActivityForResult(launcher, intent, (useFile ? BaseInputActivity.RequestCode.DropboxSave : selectFolder ? BaseInputActivity.RequestCode.DropboxOpenFolder : BaseInputActivity.RequestCode.DropboxOpenFile));
    }
    public static void start(Activity activity, ActivityResultLauncher<Intent> launcher, boolean selectFolder)
    {
        start(activity, launcher, null, 1, selectFolder);
    }
    public static void start(Activity activity, ActivityResultLauncher<Intent> launcher)
    {
        Intent intent = new Intent(activity, DropboxAccess.class);

        intent.putExtra(FileBrowserBaseActivity.ParamTypes.LoginOnly, true);
        Globals.startActivityForResult(launcher, intent, BaseInputActivity.RequestCode.DropboxAddAccount);
    }
}
