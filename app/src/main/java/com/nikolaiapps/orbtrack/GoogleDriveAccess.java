package com.nikolaiapps.orbtrack;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.widget.TextView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class GoogleDriveAccess extends AppCompatActivity implements ActivityResultCallback<ActivityResult>
{
    private static final String GOOGLE_DRIVE_SETTINGS = "googleDriveSettings";
    private static final List<String> GOOGLE_DRIVE_SCOPE_STRINGS = Arrays.asList(DriveScopes.DRIVE_READONLY, DriveScopes.DRIVE_FILE, "https://www.googleapis.com/auth/userinfo.email");

    public interface OnRemoveAccountListener
    {
        void onFinished(ClearCredentialException ex);
    }

    private static abstract class PreferenceName
    {
        static final String UserEmail = "userEmail";
    }

    private static abstract class MimeTypes
    {
        static final String Folder = "application/vnd.google-apps.folder";
        static final String TextFile = "text/plain";
    }

    private static class Item extends FileBrowserBaseActivity.ItemBase
    {
        private final String id;
        private final String path;
        private final String pathId;
        private final String name;
        private final boolean isDir;

        public Item(String dirPath, String parentPathId)
        {
            id = pathId = parentPathId;
            name = path = dirPath;
            isDir = true;
        }
        public Item(File setItem, String displayPathId, String displayPath)
        {
            String mimeType = setItem.getMimeType();

            id = setItem.getId();
            name = setItem.getName();
            isDir = (mimeType != null && mimeType.equals(MimeTypes.Folder));
            pathId = displayPathId;
            path = displayPath + (isDir ? ("/" + name) : "");
        }

        @Override
        public boolean isDirectory()
        {
            return(isDir);
        }

        @Override
        public String getId()
        {
            return(id);
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
            return(isDir ? id : pathId);
        }

        @Override
        public String getAbsolutePath()
        {
            return(path + (isDir ? "" : ("/" + name)));
        }

        @Override
        public void getList(final FileBrowserBaseActivity.OnGotListListener listener)
        {
            GetItemsTask task = new GetItemsTask(client, new GetItemsTask.OnResultListener()
            {
                @Override
                public void onResult(int resultCode, Item[] items, String parentId, String message)
                {
                    listener.onGotList(items, parentId);
                }
            });
            task.execute(id, path);
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

            return(new Item(parentPath, parentId));
        }
    }

    private static class GetItemsTask extends ThreadTask<Object, Integer, Void>
    {
        public interface OnResultListener
        {
            void onResult(int resultCode, Item[] items, String parentId, String message);
        }

        private final Drive driveClient;
        private final OnResultListener resultListener;

        public GetItemsTask(Drive drive, OnResultListener listener)
        {
            driveClient = drive;
            resultListener = listener;
        }

        @Override
        protected Void doInBackground(Object... objects)
        {
            int index = 0;
            int taskResult = FileBrowserBaseActivity.ResultCode.Error;
            File pathFolder;
            FileList files;
            String message = null;
            String pathId = (String)objects[0];
            String parentPathId = "";
            String displayPath = (String)objects[1];
            boolean pathIsRoot = pathId.isEmpty();
            Item[] items = null;
            List<File> fileList;
            List<String> parents;

            try
            {
                //try to get items
                files = driveClient.files().list().setFields("files(id, name, mimeType, version)").setQ("'" + (pathIsRoot ? "root" : pathId) + "' in parents and trashed=false").set("orderBy", "folder,name").execute();
                fileList = files.getFiles();
                items = new Item[fileList.size()];

                //go through each item
                for(File currentFile : fileList)
                {
                    items[index++] = new Item(currentFile, pathId, displayPath);
                }

                //if path is not root
                if(!pathIsRoot)
                {
                    //get parent ID of path
                    pathFolder = driveClient.files().get(pathId).setFields("parents").execute();
                    parents = pathFolder.getParents();
                    if(parents != null && !parents.isEmpty())
                    {
                        parentPathId = parents.get(0);
                    }
                }

                taskResult = FileBrowserBaseActivity.ResultCode.Success;
            }
            catch(Exception ex)
            {
                //unknown error
                message = ex.getMessage();
            }

            //if listener is set
            if(resultListener != null)
            {
                //send event
                resultListener.onResult(taskResult, items, parentPathId, message);
            }

            //end task
            return(null);
        }
    }

    private static class DownloadListenerClass implements MediaHttpDownloaderProgressListener
    {
        private final int index;
        private final int totalFiles;
        private final long totalBytes;
        private final FileBrowserBaseActivity.OnProgressListener progressListener;

        public DownloadListenerClass(int fileIndex, int fileCount, long fileTotalBytes, FileBrowserBaseActivity.OnProgressListener listener)
        {
            index = fileIndex;
            totalFiles = fileCount;
            totalBytes = fileTotalBytes;
            progressListener = listener;
        }

        @Override
        public void progressChanged(MediaHttpDownloader downloader)
        {
            if(progressListener != null)
            {
                progressListener.onProgressChanged(index, totalFiles, downloader.getNumBytesDownloaded(), totalBytes,((index / (double)totalFiles) + (downloader.getProgress() / (double)totalFiles)) * 100);
            }
        }
    }

    private static class SaveListenerClass implements MediaHttpUploaderProgressListener
    {
        private final long totalBytes;
        private final FileBrowserBaseActivity.OnProgressListener progressListener;

        public SaveListenerClass(long fileTotalBytes, FileBrowserBaseActivity.OnProgressListener listener)
        {
            totalBytes = fileTotalBytes;
            progressListener = listener;
        }

        @Override
        public void progressChanged(MediaHttpUploader uploader)
        {
            if(progressListener != null)
            {
                double progress;

                try
                {
                    progress = uploader.getProgress();
                }
                catch(Exception ex)
                {
                    progress = 0;
                }

                progressListener.onProgressChanged(0, 1, uploader.getNumBytesUploaded(), totalBytes, progress * 100);
            }
        }
    }

    private static class SaveFileTask extends ThreadTask<Object, Integer, Void>
    {
        private final Drive driveClient;
        private final FileBrowserBaseActivity.OnProgressListener saveListener;
        private final FileBrowserBaseActivity.OnResultListener resultListener;

        public SaveFileTask(Drive drive, FileBrowserBaseActivity.OnProgressListener svListener, FileBrowserBaseActivity.OnResultListener resListener)
        {
            saveListener = svListener;
            driveClient = drive;
            resultListener = resListener;
        }

        @Override
        protected Void doInBackground(Object... objects)
        {
            int taskResult;
            String message = null;
            Context context = (Context)objects[0];
            String path = (String)objects[1];
            File driveFile = new File();
            Drive.Files.Create save;
            java.io.File tempFile = (java.io.File)objects[2];

            //if path is not set or root
            if(path == null || path.isEmpty())
            {
                //set as root
                path = "root";
            }

            try
            {
                //upload file
                driveFile.setName(tempFile.getName());
                driveFile.setParents(Collections.singletonList(path));
                save = driveClient.files().create(driveFile, new FileContent(MimeTypes.TextFile, tempFile)).setFields("id, parents");
                if(saveListener != null)
                {
                    save.getMediaHttpUploader().setProgressListener(new SaveListenerClass(tempFile.length(), saveListener)).setChunkSize(262144);
                }
                save.execute();

                //success
                taskResult = FileBrowserBaseActivity.ResultCode.Success;

                //try to delete temp file
                if(!tempFile.delete())
                {
                    //error deleting, but okay
                    message = context.getResources().getString(R.string.text_old_file_delete_failed);
                }
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
        private final Drive driveClient;
        private final FileBrowserBaseActivity.OnResultListener resultListener;
        private final FileBrowserBaseActivity.OnProgressListener downloadListener;

        public DownloadFilesTask(Drive drive, FileBrowserBaseActivity.OnProgressListener dlListener, FileBrowserBaseActivity.OnResultListener resListener)
        {
            driveClient = drive;
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
            String[] fileIds = (String[])objects[0];
            String[] fileNames = (String[])objects[1];

            try
            {
                //get total files
                final int totalFiles = fileIds.length;

                //go through each file name
                for(fileIndex = 0; fileIndex < totalFiles; fileIndex++)
                {
                    //download file
                    String currentFileId = fileIds[fileIndex];
                    String currentFileName = fileNames[fileIndex];
                    File currentFileSize = driveClient.files().get(currentFileId).setFields("size").execute();
                    ByteArrayOutputStream fileOutput = new ByteArrayOutputStream();
                    Drive.Files.Get download = driveClient.files().get(currentFileId);
                    if(downloadListener != null)
                    {
                        download.getMediaHttpDownloader().setProgressListener(new DownloadListenerClass(fileIndex, totalFiles, currentFileSize.getSize(), downloadListener)).setChunkSize(4096);
                    }
                    download.executeMediaAndDownloadTo(fileOutput);

                    //read file, add name, then close
                    filesData.add(fileOutput.toByteArray());
                    filesDataNames.add(currentFileName);
                    fileOutput.close();
                }

                //success
                taskResult = FileBrowserBaseActivity.ResultCode.Success;
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
                super(activity, new Item("", ""), new Item(startDir, ""), true, selectFolder);
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

    private static class GetClientTask extends ThreadTask<Object, Integer, Void>
    {
        public interface OnResultListener
        {
            void onResult(Drive drive, int resultCode, String message);
        }

        private final OnResultListener resultListener;

        public GetClientTask(OnResultListener listener)
        {
            resultListener = listener;
        }

        @Override
        protected Void doInBackground(Object... objects)
        {
            int taskResult = FileBrowserBaseActivity.ResultCode.LoginFailed;
            Context context = (Context)objects[0];
            String message = null;
            String accessToken = (String)objects[1];
            String currentEmail = getUserEmail(context);
            GoogleCredentials accountCredential = (accessToken != null ? ServiceAccountCredentials.create(new AccessToken(accessToken, null)).createScoped(GOOGLE_DRIVE_SCOPE_STRINGS) : null);
            HttpRequestInitializer credential = (accountCredential != null ? new HttpCredentialsAdapter(accountCredential) : null);
            Drive driveClient = (credential != null ? new Drive.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential).setApplicationName(context.getPackageName()).build() : null);

            //if client was set
            if(driveClient != null)
            {
                try
                {
                    //test connection
                    driveClient.files().get("root").setFields("id").execute();
                    taskResult = FileBrowserBaseActivity.ResultCode.Success;
                }
                catch(UserRecoverableAuthIOException | IllegalStateException userOrStateEx)
                {
                    //retry later
                    driveClient = null;
                    message = (userOrStateEx instanceof IllegalStateException ? context.getString(R.string.desc_google_account_denied) : userOrStateEx.getMessage());
                }
                catch(Exception ex)
                {
                    try
                    {
                        //test for a basic file listing
                        driveClient.files().list();
                        taskResult = FileBrowserBaseActivity.ResultCode.Success;
                    }
                    catch(Exception subEx)
                    {
                        //retry later
                        driveClient = null;
                        message = subEx.getMessage();
                    }
                }
            }

            //if success getting client and email is unknown
            if(driveClient != null && (currentEmail == null || currentEmail.isEmpty()))
            {
                //try to get user email
                Globals.WebPageData userData = Globals.getWebPage("https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + Globals.encodeUrlValue(accessToken));
                if(userData.gotData() && userData.isOkay())
                {
                    try
                    {
                        //try to parse email
                        JSONObject userInfo = new JSONObject(userData.pageData);
                        String userEmail = userInfo.getString("email");

                        //if got email
                        if(!userEmail.isEmpty())
                        {
                            //save it for display
                            setUserEmail(context, userEmail);
                        }
                    }
                    catch(Exception ex)
                    {
                        //do nothing
                    }
                }
            }

            //if listener is set
            if(resultListener != null)
            {
                //send event
                resultListener.onResult(driveClient, taskResult, message);
            }

            //end task
            return(null);
        }
    }

    private static Drive client = null;

    private boolean selectFolder;
    private boolean loginOnly;
    private int itemCount;
    private byte requestCode;
    private java.io.File saveFile;
    private ActivityResultLauncher<Intent> resultLauncher;
    private ActivityResultLauncher<IntentSenderRequest> accountRequestLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = this.getIntent();

        //if intent not set
        if(intent == null)
        {
            //create empty
            intent = new Intent();
        }

        //get params
        selectFolder = intent.getBooleanExtra(FileBrowserBaseActivity.ParamTypes.SelectFolder, false);
        saveFile = (java.io.File)intent.getSerializableExtra(FileBrowserBaseActivity.ParamTypes.FileName);
        itemCount = intent.getIntExtra(FileBrowserBaseActivity.ParamTypes.ItemCount, 0);
        loginOnly = intent.getBooleanExtra(FileBrowserBaseActivity.ParamTypes.LoginOnly, false);
        requestCode = BaseInputActivity.getRequestCode(intent);

        //create launchers
        resultLauncher = Globals.createActivityLauncher(this, this);
        accountRequestLauncher = Globals.createActivityRequestLauncher(GoogleDriveAccess.this, GoogleDriveAccess.this, BaseInputActivity.RequestCode.GoogleDriveSignIn);

        //get permission
        getPermission();
    }

    @Override
    public void onActivityResult(ActivityResult result)
    {
        int resultCode = result.getResultCode();
        byte localRequestCode;
        boolean isOkay = (resultCode == RESULT_OK);
        boolean finished = false;
        String folderName;
        String accessToken;
        ArrayList<String> fileIds;
        ArrayList<String> fileNames;
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
            case BaseInputActivity.RequestCode.GoogleDriveSignIn:
                if(isOkay)
                {
                    //try to get access token
                    accessToken = data.getStringExtra(FileBrowserBaseActivity.ParamTypes.AccessToken);
                    if(accessToken == null)
                    {
                        try
                        {
                            //get result
                            AuthorizationResult authorizationResult = Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data);

                            //get access token and set default email
                            accessToken = authorizationResult.getAccessToken();
                            setUserEmail(this, "");
                        }
                        catch(Exception ex)
                        {
                            //do nothing
                        }
                    }

                    //try to get client
                    new GetClientTask(new GetClientTask.OnResultListener()
                    {
                        @Override
                        public void onResult(Drive drive, int resultCode, String message)
                        {
                            switch(resultCode)
                            {
                                case FileBrowserBaseActivity.ResultCode.Success:
                                    Intent intent = null;

                                    //if not logging in only
                                    if(!loginOnly)
                                    {
                                        //set params
                                        intent = new Intent(GoogleDriveAccess.this, BrowserActivity.class);
                                        intent.putExtra(BrowserActivity.ParamTypes.RootFolder, "");
                                        intent.putExtra(BrowserActivity.ParamTypes.SelectFolder, selectFolder);
                                    }

                                    //remember client
                                    client = drive;

                                    //if logging in only
                                    if(loginOnly)
                                    {
                                        //pass information back to caller
                                        setResult(RESULT_OK, resultData);
                                        GoogleDriveAccess.this.finish();
                                    }
                                    else
                                    {
                                        //show selection activity
                                        Globals.startActivityForResult(resultLauncher, intent, (selectFolder ? BaseInputActivity.RequestCode.GoogleDriveOpenFolder : BaseInputActivity.RequestCode.GoogleDriveOpenFile));
                                    }
                                    break;

                                case FileBrowserBaseActivity.ResultCode.LoginFailed:
                                    //pass any message back to caller
                                    resultData.putExtra(FileBrowserBaseActivity.ParamTypes.Message, message);
                                    setResult(RESULT_CANCELED, resultData);
                                    GoogleDriveAccess.this.finish();
                                    break;
                            }
                        }
                    }).execute(this, accessToken);
                }
                else
                {
                    finished = true;
                }
                break;

            case BaseInputActivity.RequestCode.GoogleDriveOpenFolder:
            case BaseInputActivity.RequestCode.GoogleDriveOpenFile:
                if(isOkay)
                {
                    //add progress bar
                    final LinearProgressIndicator progressBar = new LinearProgressIndicator(this);
                    final TextView progressText = Globals.showSnackBarProgress(this.findViewById(android.R.id.content), progressBar);
                    final Resources res = this.getResources();

                    //if getting folder
                    if(localRequestCode == BaseInputActivity.RequestCode.GoogleDriveOpenFolder)
                    {
                        //get folder and save file
                        folderName = data.getStringExtra(FileBrowserBaseActivity.ParamTypes.FolderName);
                        saveFile(folderName, saveFile, new FileBrowserBaseActivity.OnProgressListener()
                        {
                            @Override
                            public void onProgressChanged(int index, int length, long bytes, long totalBytes, double progress)
                            {
                                FileBrowserBaseActivity.updateProgress(GoogleDriveAccess.this, res, progressBar, progressText, index, length, bytes, totalBytes, progress);
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
                                GoogleDriveAccess.this.finish();
                            }
                        });
                    }
                    else
                    {
                        //get files
                        fileIds = data.getStringArrayListExtra(FileBrowserBaseActivity.ParamTypes.FileIds);
                        fileNames = data.getStringArrayListExtra(FileBrowserBaseActivity.ParamTypes.FileNames);
                        getFiles(fileIds, fileNames, new FileBrowserBaseActivity.OnProgressListener()
                        {
                            @Override
                            public void onProgressChanged(int index, int length, long bytes, long totalBytes, double progress)
                            {
                                FileBrowserBaseActivity.updateProgress(GoogleDriveAccess.this, res, progressBar, progressText, index, length, bytes, totalBytes, progress);
                            }
                        }, new FileBrowserBaseActivity.OnResultListener()
                        {
                            @Override
                            public void onResult(int resultCode, ArrayList<String> fileNames, ArrayList<byte[]> filesData, String message)
                            {
                                int index;
                                int count = filesData.size();
                                Activity activity = GoogleDriveAccess.this;

                                //pass information back to caller
                                int passedResultCode = (resultCode == FileBrowserBaseActivity.ResultCode.Success ? RESULT_OK : RESULT_CANCELED);
                                resultData.putStringArrayListExtra(FileBrowserBaseActivity.ParamTypes.FileNames, fileNames);
                                resultData.putExtra(FileBrowserBaseActivity.ParamTypes.FilesDataCount, count);
                                for(index = 0; index < count; index++)
                                {
                                    //save file and add it
                                    java.io.File cacheFile = new java.io.File(activity.getCacheDir(), "filesData" + index + ".txt");
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
                    finished = true;
                }
                break;

            case BaseInputActivity.RequestCode.None:
                finished = true;
                break;
        }

        //if finished
        if(finished)
        {
            //pass information back to caller
            setResult(resultCode, resultData);
            this.finish();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
    }

    //Gets permission
    private void getPermission()
    {
        AuthorizationRequest signInRequest;
        ArrayList<Scope> requiredScopes = new ArrayList<>();

        //get required scopes and build request
        for(String currentScopeString : GOOGLE_DRIVE_SCOPE_STRINGS)
        {
            //add scope
            requiredScopes.add(new Scope(currentScopeString));
        }
        signInRequest = AuthorizationRequest.builder().setRequestedScopes(requiredScopes).build();

        //request permission
        Identity.getAuthorizationClient(this).authorize(signInRequest).addOnSuccessListener(new OnSuccessListener<>()
        {
            @Override
            public void onSuccess(AuthorizationResult authorizationResult)
            {
                //if need to ask for account still
                if(authorizationResult.hasResolution())
                {
                    //create request
                    PendingIntent pendingRequestIntent = authorizationResult.getPendingIntent();
                    IntentSenderRequest request = (pendingRequestIntent != null ? new IntentSenderRequest.Builder(pendingRequestIntent).build() : null);

                    //send request
                    accountRequestLauncher.launch(request);
                }
                else
                {
                    Intent data = new Intent();

                    //handle as if request was just sent and approved, then add access token manually
                    data.putExtra(BaseInputActivity.EXTRA_REQUEST_CODE, BaseInputActivity.RequestCode.GoogleDriveSignIn);
                    data.putExtra(FileBrowserBaseActivity.ParamTypes.AccessToken, authorizationResult.getAccessToken());
                    onActivityResult(new ActivityResult(RESULT_OK, data));
                }
            }
        }).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                //denied or no request client
            }
        });
    }

    //Gets settings preferences
    private static SharedPreferences getPreferences(Context context)
    {
        return(context.getSharedPreferences(GOOGLE_DRIVE_SETTINGS, MODE_PRIVATE));
    }

    //Gets write settings
    private static SharedPreferences.Editor getWriteSettings(Context context)
    {
        return(getPreferences(context).edit());
    }

    //Gets user email
    public static String getUserEmail(Context context)
    {
        return(getPreferences(context).getString(PreferenceName.UserEmail, null));
    }

    //Sets user email
    private static void setUserEmail(Context context, String email)
    {
        SharedPreferences.Editor writeSettings = getWriteSettings(context);
        writeSettings.putString(PreferenceName.UserEmail, email);
        writeSettings.apply();
    }

    //Remove current account
    public static void removeAccount(Context context, OnRemoveAccountListener listener)
    {
        CredentialManager manager = CredentialManager.create(context);
        ClearCredentialStateRequest request = new ClearCredentialStateRequest();
        manager.clearCredentialStateAsync(request, new CancellationSignal(), ContextCompat.getMainExecutor(context), new CredentialManagerCallback<>()
        {
            @Override
            public void onResult(Void unused)
            {
                //remove email
                setUserEmail(context, null);

                //if listener exists
                if(listener != null)
                {
                    //call with no error
                    listener.onFinished(null);
                }
            }

            @Override
            public void onError(@NonNull ClearCredentialException ex)
            {
                //if listener exists
                if(listener != null)
                {
                    //call with error
                    listener.onFinished(ex);
                }
            }
        });
    }

    //Gets the given files
    private void getFiles(ArrayList<String> fileIds, ArrayList<String> fileNames, FileBrowserBaseActivity.OnProgressListener dlListener, FileBrowserBaseActivity.OnResultListener resListener)
    {
        DownloadFilesTask task = new DownloadFilesTask(client, dlListener, resListener);

        if(fileIds == null)
        {
            fileIds = new ArrayList<>(0);
        }
        if(fileNames == null)
        {
            fileNames = new ArrayList<>(0);
        }
        task.execute(fileIds.toArray(new String[0]), fileNames.toArray(new String[0]));
    }

    //Save the given file
    private void saveFile(String path, java.io.File fileName, FileBrowserBaseActivity.OnProgressListener svListener, FileBrowserBaseActivity.OnResultListener resListener)
    {
        SaveFileTask task = new SaveFileTask(client, svListener, resListener);
        task.execute(this.getApplicationContext(), path, fileName);
    }

    //Starts an instance
    private static void start(Activity activity, ActivityResultLauncher<Intent> launcher, java.io.File fileName, int count, boolean selectFolder, boolean loginOnly)
    {
        boolean useFile = (fileName != null);
        Intent intent = new Intent(activity, GoogleDriveAccess.class);

        //if can use google play services
        if(Globals.getUseGooglePlayServices(activity, true))
        {
            //setup and start google drive access
            intent.putExtra(FileBrowserBaseActivity.ParamTypes.SelectFolder, selectFolder);
            if(useFile)
            {
                intent.putExtra(FileBrowserBaseActivity.ParamTypes.FileName, fileName);
            }
            intent.putExtra(FileBrowserBaseActivity.ParamTypes.ItemCount, count);
            if(loginOnly)
            {
                intent.putExtra(FileBrowserBaseActivity.ParamTypes.LoginOnly, true);
            }
            Globals.startActivityForResult(launcher, intent, (loginOnly ? BaseInputActivity.RequestCode.GoogleDriveAddAccount : useFile ? BaseInputActivity.RequestCode.GoogleDriveSave : selectFolder ? BaseInputActivity.RequestCode.GoogleDriveOpenFolder : BaseInputActivity.RequestCode.GoogleDriveOpenFile));
        }
    }
    public static void start(Activity activity, ActivityResultLauncher<Intent> launcher, java.io.File fileName, int count, boolean selectFolder)
    {
        start(activity, launcher, fileName, count, selectFolder, false);
    }
    public static void start(Activity activity, ActivityResultLauncher<Intent> launcher, boolean loginOnly)
    {
        start(activity, launcher, null, 0, false, loginOnly);
    }
}