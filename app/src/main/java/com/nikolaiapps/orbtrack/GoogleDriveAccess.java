package com.nikolaiapps.orbtrack;


import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class GoogleDriveAccess extends AppCompatActivity
{
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
            else if(path != null && path.equals(""))
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
            boolean pathIsRoot = pathId.equals("");
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
                    if(parents != null && parents.size() > 0)
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
            if(path == null || path.equals(""))
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
                resultListener.onResult(taskResult, null, message);
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
            boolean removeInvalid = true;
            String fileData;
            String message = null;
            ArrayList<String> filesData = new ArrayList<>(0);
            String[] fileNames = (String[])objects[0];

            try
            {
                //get total files
                final int totalFiles = fileNames.length;

                //go through each file name
                for(fileIndex = 0; fileIndex < totalFiles; fileIndex++)
                {
                    //download file
                    File currentFileSize = driveClient.files().get(fileNames[fileIndex]).setFields("size").execute();
                    ByteArrayOutputStream fileOutput = new ByteArrayOutputStream();
                    Drive.Files.Get download = driveClient.files().get(fileNames[fileIndex]);
                    if(downloadListener != null)
                    {
                        download.getMediaHttpDownloader().setProgressListener(new DownloadListenerClass(fileIndex, totalFiles, currentFileSize.getSize(), downloadListener)).setChunkSize(4096);
                    }
                    download.executeMediaAndDownloadTo(fileOutput);

                    //read file data
                    fileData = fileOutput.toString().replace("\0", "");
                    fileOutput.close();

                    //remove any starting invalid chars while there are chars left
                    while(removeInvalid && fileData.length() > 1)
                    {
                        //if start char is invalid
                        removeInvalid = (fileData.charAt(0) > 127);
                        if(removeInvalid)
                        {
                            //remove it
                            fileData = fileData.substring(1);
                        }
                    }

                    //add file
                    filesData.add(fileData);
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
                resultListener.onResult(taskResult, filesData, message);
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

    private static class GetPermissionTask extends ThreadTask<Object, Integer, Void>
    {
        public interface OnResultListener
        {
            void onResult(Drive drive, int resultCode);
        }

        private final OnResultListener resultListener;

        public GetPermissionTask(OnResultListener listener)
        {
            resultListener = listener;
        }

        @Override
        protected Void doInBackground(Object... objects)
        {
            int taskResult = FileBrowserBaseActivity.ResultCode.LoginFailed;
            Drive driveClient = null;
            Context currentContext = (Context)objects[0];

            //get account and credential
            GoogleSignInAccount driveAccount = Globals.getGoogleDriveAccount(currentContext);
            Account googleAccount = (driveAccount != null ? driveAccount.getAccount() : null);
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(currentContext, Arrays.asList(DriveScopes.DRIVE_READONLY, DriveScopes.DRIVE_FILE));

            //if account is set
            if(googleAccount != null)
            {
                //setup service
                credential.setSelectedAccount(googleAccount);
                driveClient = new Drive.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential).build();

                try
                {
                    //test connection
                    driveClient.files().get("root").setFields("id").execute();
                    taskResult = FileBrowserBaseActivity.ResultCode.Success;
                }
                catch(Exception ex)
                {
                    //retry later
                    driveClient = null;
                }
            }

            //if listener is set
            if(resultListener != null)
            {
                //send event
                resultListener.onResult(driveClient, taskResult);
            }

            //end task
            return(null);
        }
    }

    private static Drive client = null;

    //private boolean showList;
    private boolean selectFolder;
    //private boolean loginOnly;
    private int itemCount;
    private java.io.File saveFile;

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
        //loginOnly = intent.getBooleanExtra(ParamTypes.LoginOnly, false);

        /*//want to show list if not logging in only
        showList = !loginOnly;*/

        /*//if -logging in only- or -client not set-
        if(loginOnly || client == null)
        {*/
            //get permission
            getPermission();
        //}
    }

    /*@Override
    protected void onResume()
    {
        super.onResume();
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        boolean isOkay = (resultCode == RESULT_OK);
        boolean finished = false;
        String folderName;
        ArrayList<String> fileNames;
        final Intent resultData = new Intent();

        switch(requestCode)
        {
            case BaseInputActivity.RequestCode.GoogleDriveSignIn:
                if(isOkay)
                {
                    //try to get account
                    Task<GoogleSignInAccount> getAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);

                    //if got account
                    if(getAccountTask.isSuccessful())
                    {
                        //retry setting up permission
                        getPermission();
                    }
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
                    final ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                    final TextView progressText = Globals.showSnackBarProgress(this.findViewById(android.R.id.content), progressBar);
                    final Resources res = this.getResources();

                    //if getting folder
                    if(requestCode == BaseInputActivity.RequestCode.GoogleDriveOpenFolder)
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
                            public void onResult(int resultCode, ArrayList<String> filesData, String message)
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
                        fileNames = data.getStringArrayListExtra(FileBrowserBaseActivity.ParamTypes.FileNames);
                        getFiles(fileNames, new FileBrowserBaseActivity.OnProgressListener()
                        {
                            @Override
                            public void onProgressChanged(int index, int length, long bytes, long totalBytes, double progress)
                            {
                                FileBrowserBaseActivity.updateProgress(GoogleDriveAccess.this, res, progressBar, progressText, index, length, bytes, totalBytes, progress);
                            }
                        }, new FileBrowserBaseActivity.OnResultListener()
                        {
                            @Override
                            public void onResult(int resultCode, ArrayList<String> filesData, String message)
                            {
                                //pass information back to caller
                                int passedResultCode = (resultCode == FileBrowserBaseActivity.ResultCode.Success ? RESULT_OK : RESULT_CANCELED);
                                resultData.putStringArrayListExtra(FileBrowserBaseActivity.ParamTypes.FilesData, filesData);
                                setResult(passedResultCode, resultData);
                                GoogleDriveAccess.this.finish();
                            }
                        });
                    }
                }
                else
                {
                    finished = true;
                }
                break;
        }

        if(finished)
        {
            setResult(resultCode);
            this.finish();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
    }

    //Gets permission
    private void getPermission()
    {
        new GetPermissionTask(new GetPermissionTask.OnResultListener()
        {
            @Override
            public void onResult(Drive drive, int resultCode)
            {
                switch(resultCode)
                {
                    case FileBrowserBaseActivity.ResultCode.Success:
                        Intent intent = new Intent(GoogleDriveAccess.this, BrowserActivity.class);
                        intent.putExtra(BrowserActivity.ParamTypes.RootFolder, "");
                        intent.putExtra(BrowserActivity.ParamTypes.SelectFolder, selectFolder);

                        client = drive;
                        GoogleDriveAccess.this.startActivityForResult(intent, (selectFolder ? BaseInputActivity.RequestCode.GoogleDriveOpenFolder : BaseInputActivity.RequestCode.GoogleDriveOpenFile));
                        break;

                    case FileBrowserBaseActivity.ResultCode.LoginFailed:
                        Globals.askGoogleDriveAccount(GoogleDriveAccess.this, BaseInputActivity.RequestCode.GoogleDriveSignIn);
                        break;
                }
            }
        }).execute(this);
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
    private void saveFile(String path, java.io.File fileName, FileBrowserBaseActivity.OnProgressListener svListener, FileBrowserBaseActivity.OnResultListener resListener)
    {
        SaveFileTask task = new SaveFileTask(client, svListener, resListener);
        task.execute(this.getApplicationContext(), path, fileName);
    }

    //Starts an instance
    public static void start(Activity activity, java.io.File fileName, int count, boolean selectFolder)
    {
        boolean useFile = (fileName != null);
        Intent intent = new Intent(activity, GoogleDriveAccess.class);

        intent.putExtra(FileBrowserBaseActivity.ParamTypes.SelectFolder, selectFolder);
        if(useFile)
        {
            intent.putExtra(FileBrowserBaseActivity.ParamTypes.FileName, fileName);
        }
        intent.putExtra(FileBrowserBaseActivity.ParamTypes.ItemCount, count);
        activity.startActivityForResult(intent, (useFile ? BaseInputActivity.RequestCode.GoogleDriveSave : selectFolder ? BaseInputActivity.RequestCode.GoogleDriveOpenFolder : BaseInputActivity.RequestCode.GoogleDriveOpenFile));
    }
    public static void start(Activity activity, boolean selectFolder)
    {
        start(activity, null, 1, selectFolder);
    }
    public static void start(Activity activity)
    {
        Intent intent = new Intent(activity, GoogleDriveAccess.class);

        intent.putExtra(FileBrowserBaseActivity.ParamTypes.LoginOnly, true);
        activity.startActivityForResult(intent, BaseInputActivity.RequestCode.GoogleDriveAddAccount);
    }
}