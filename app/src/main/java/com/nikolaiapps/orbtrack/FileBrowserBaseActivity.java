package com.nikolaiapps.orbtrack;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;


public abstract class FileBrowserBaseActivity extends BaseInputActivity
{
    public interface OnPathChangedListener
    {
        void onPathChanged(String path);
    }

    public interface OnSelectedFilesChangedListener
    {
        void onSelectedFilesChanged(ArrayList<ItemBase> files);
    }

    public interface OnGotListListener
    {
        void onGotList(ItemBase[] list, String parentId);
    }

    public interface OnProgressListener
    {
        void onProgressChanged(int index, int length, long bytes, long totalBytes, double progress);
    }

    public interface OnResultListener
    {
        void onResult(int resultCode, ArrayList<String> fileNames, ArrayList<byte[]> filesData, String message);
    }

    public static abstract class ResultCode
    {
        static final int LoginFailed = 0;
        static final int Error = 1;
        static final int Success = 2;
    }

    public static abstract class ParamTypes
    {
        static final String SelectFolder = "selectFolder";
        static final String FileIds = "fileIds";
        static final String FileName = "fileName";
        static final String FileNames = "fileNames";
        static final String Files = "files";
        static final String FilesDataCount = "filesDataCount";
        static final String FolderName = "folderName";
        static final String ItemCount = "itemCount";
        static final String LoginOnly = "loginOnly";
    }

    public static abstract class ItemBase
    {
        public abstract boolean isDirectory();
        public abstract String getId();
        public abstract String getName();
        public abstract String getFullName();
        public abstract String getPath();
        public abstract String getPathId();
        public abstract String getAbsolutePath();
        public abstract void getList(OnGotListListener listener);
        public abstract ItemBase getParentFolder(String parentId);

        public static class Comparer implements Comparator<ItemBase>
        {
            @Override
            public int compare(ItemBase lhs, ItemBase rhs)
            {
                String lhsLower;
                String rhsLower;

                if(!lhs.isDirectory() && rhs.isDirectory())
                {
                    return(1);
                }
                else if(lhs.isDirectory() && !rhs.isDirectory())
                {
                    return(-1);
                }
                else
                {
                    lhsLower = lhs.getName().toLowerCase();
                    rhsLower = rhs.getName().toLowerCase();

                    if(lhsLower.equals(rhsLower))
                    {
                        return(lhs.getId().compareTo(rhs.getId()));
                    }
                    else
                    {
                        return(lhsLower.compareTo(rhsLower));
                    }
                }
            }
        }

        @Override
        public boolean equals(Object other)
        {
            if(other instanceof ItemBase)
            {
                ItemBase otherItem = (ItemBase)other;
                return(this.getId().equals(otherItem.getId()) && this.getName().equals(otherItem.getName()) && this.getPath().equals(otherItem.getPath()));
            }
            else
            {
                return(false);
            }
        }
    }

    private static class ItemHolder extends RecyclerView.ViewHolder
    {
        public TextView fileText;
        public CheckBox fileCheck;
        public AppCompatImageView fileImage;

        public ItemHolder(View itemView)
        {
            super(itemView);
            fileText = null;
            fileCheck = null;
            fileImage = null;
        }
    }

    protected static abstract class FileListAdapterBase extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    {
        public interface OnItemClickListener
        {
            void onItemClicked(View view, int position);
        }

        private static ItemBase rootDir;
        protected final FileBrowserBaseActivity currentContext;
        private final boolean allowRootItem;
        private final boolean selectFolder;
        private final Drawable fileDrawable;
        private final Drawable folderDrawable;
        private final Drawable backupFileDrawable;
        private final Drawable tleFileDrawable;
        private final Drawable zipFileDrawable;
        private final OnPathChangedListener pathChangedListener;
        private final OnSelectedFilesChangedListener selectedFilesChangedListener;
        private final OnItemClickListener itemClickedListener;
        private final ArrayList<ItemBase> selectedFiles = new ArrayList<>(0);
        private final Comparator<ItemBase> fileComparer = new ItemBase.Comparer();
        private ItemBase currentDir;
        private ItemBase parentDir;
        private ItemBase[] files;

        protected FileListAdapterBase(FileBrowserBaseActivity activity, ItemBase root, ItemBase startDir, boolean showRootItem, boolean gettingFolder)
        {
            rootDir = root;
            allowRootItem = showRootItem;
            selectFolder = gettingFolder;
            currentContext = activity;
            pathChangedListener = currentContext.createOnPathChangedListener();
            selectedFilesChangedListener = currentContext.createOnSelectedFilesChangedListener();
            fileDrawable = Globals.getDrawable(currentContext, R.drawable.ic_insert_drive_file_black, true);
            folderDrawable = Globals.getDrawable(currentContext, R.drawable.ic_folder_open_black, true);
            backupFileDrawable = Globals.getDrawable(currentContext, R.drawable.ic_storage_black, true);
            tleFileDrawable = Globals.getDrawable(currentContext, Settings.getSatelliteIconImageId(currentContext), Settings.getSatelliteIconImageIsThemeable(currentContext));
            zipFileDrawable = Globals.getDrawable(currentContext, R.drawable.ic_briefcase_black, true);

            itemClickedListener = new OnItemClickListener()
            {
                @Override
                public void onItemClicked(View view, int position)
                {
                    ItemBase currentFile = getItem(position);
                    CheckBox currentCheck;

                    //if a directory
                    if(currentFile.isDirectory())
                    {
                        //update with files in this folder
                        getFiles(currentFile);
                    }
                    else
                    {
                        //reverse checked state
                        currentCheck = view.findViewById(R.id.Item_Checked_Check);
                        currentCheck.setChecked(!currentCheck.isChecked());
                    }
                }
            };

            getFiles(startDir);
        }

        @Override
        public long getItemId(int position)
        {
            return(-1);
        }

        public ItemBase getItem(int position)
        {
            return(files != null && position < files.length ? files[position] : null);
        }

        @Override
        public int getItemCount()
        {
            return(files != null ? files.length : 0);
        }

        @Override
        public @NonNull ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_checked_item, parent, false);
            final ItemHolder itemHolder = new ItemHolder(itemView);

            //get displays and set event
            itemHolder.fileImage = itemView.findViewById(R.id.Item_Checked_Image1);
            itemHolder.fileText = itemView.findViewById(R.id.Item_Checked_Text);
            itemHolder.fileCheck = itemView.findViewById(R.id.Item_Checked_Check);
            itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if(itemClickedListener != null)
                    {
                        itemClickedListener.onItemClicked(view, itemHolder.getAdapterPosition());
                    }
                }
            });

            return(itemHolder);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
        {
            final ItemBase currentFile = files[position];
            boolean isDir = currentFile.isDirectory();
            final String currentFileName = (currentFile.equals(parentDir) ? ".." : currentFile.getName());
            final String lowerFileName = currentFileName.toLowerCase();
            final ItemHolder itemHolder = (ItemHolder)holder;

            //update displays
            itemHolder.fileImage.setBackgroundDrawable(isDir ? folderDrawable : lowerFileName.endsWith(".tle") ? tleFileDrawable : lowerFileName.endsWith(".json") ? backupFileDrawable : lowerFileName.endsWith(".zip") ? zipFileDrawable : fileDrawable);
            itemHolder.fileText.setText(currentFileName);
            itemHolder.fileCheck.setOnCheckedChangeListener(null);
            itemHolder.fileCheck.setChecked(selectedFiles.contains(currentFile));
            itemHolder.fileCheck.setVisibility(isDir ? View.GONE : View.VISIBLE);

            //if not a directory
            if(!isDir)
            {
                //set events
                itemHolder.fileCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
                    {
                        setFileSelected(currentFile, isChecked);
                    }
                });
            }
        }

        //Gets current directory
        private String getCurrentDirectory()
        {
            return(currentDir != null ? currentDir.getPathId() : null);
        }

        //Gets selected files
        private ArrayList<ItemBase> getSelectedFiles()
        {
            return(selectedFiles);
        }

        //Gets selected file IDs
        private ArrayList<String> getSelectedFileIds()
        {
            ArrayList<String> idList = new ArrayList<>(selectedFiles.size());

            for(ItemBase currentFile : selectedFiles)
            {
                idList.add(currentFile.getId());
            }

            return(idList);
        }

        //Gets selected full file names
        private ArrayList<String> getSelectedFileFullNames()
        {
            ArrayList<String> nameList = new ArrayList<>(selectedFiles.size());

            for(ItemBase currentFile : selectedFiles)
            {
                nameList.add(currentFile.getFullName());
            }

            return(nameList);
        }

        //Sets selected status of given file
        private void setFileSelected(ItemBase fileName, boolean add)
        {
            boolean fileIsSelected = selectedFiles.contains(fileName);

            //if adding and file is not in the list
            if(add && !fileIsSelected)
            {
                //add file
                selectedFiles.add(fileName);
            }
            //else if removing and file is in the list
            else if(!add && fileIsSelected)
            {
                //remove file
                selectedFiles.remove(fileName);
            }

            //if selected files change listener is set
            if(selectedFilesChangedListener != null)
            {
                //update files changed
                selectedFilesChangedListener.onSelectedFilesChanged(selectedFiles);
            }
        }

        //Gets all files from the given directory
        private void getFiles(ItemBase fromDir)
        {
            final ArrayList<ItemBase> fileList = new ArrayList<>(0);

            //update current and reset parent dir
            currentDir = fromDir;
            parentDir = null;

            //get directories and files
            currentContext.setLoading(true);
            currentDir.getList(new OnGotListListener()
            {
                @Override
                public void onGotList(ItemBase[] list, String parentId)
                {
                    //update parent dir
                    parentDir = currentDir.getParentFolder(parentId);
                    if(parentDir != null)
                    {
                        //add existing directory
                        fileList.add(parentDir);
                    }

                    //if list exists and has items
                    if(list != null && list.length > 0)
                    {
                        //go through each item
                        for(ItemBase currentFile : list)
                        {
                            //remember lowercase name
                            String currentLowerName = currentFile.getName().toLowerCase();

                            //if a directory, backup, TLE, or zip
                            if(currentFile.isDirectory() || (!selectFolder && (currentLowerName.endsWith(".json") || currentLowerName.endsWith(".tle") || currentLowerName.endsWith(".zip"))))
                            {
                                //add to list
                                fileList.add(currentFile);
                            }
                        }
                    }

                    //if root dir not allowed and in list
                    if(!allowRootItem && fileList.contains(rootDir))
                    {
                        //remove it
                        fileList.remove(rootDir);
                    }

                    //get and sort files
                    files = fileList.toArray(new ItemBase[0]);
                    if(files.length > 1)
                    {
                        Arrays.sort(files, 1, files.length, fileComparer);
                    }

                    //if path changed listener is set
                    if(pathChangedListener != null)
                    {
                        pathChangedListener.onPathChanged(currentDir.getPath());
                    }

                    //update list
                    currentContext.runOnUiThread(new Runnable()
                    {
                        @Override @SuppressLint("NotifyDataSetChanged")
                        public void run()
                        {
                            FileListAdapterBase.this.notifyDataSetChanged();
                            currentContext.setLoading(false);
                        }
                    });
                }
            });
        }

        //Goes back to parent directory
        private boolean goBack()
        {
            //if parent directory exists and -can view root folder or not on root folder-
            if(parentDir != null && (allowRootItem || !parentDir.equals(rootDir)))
            {
                //go to parent directory
                getFiles(parentDir);
                return(true);
            }

            //can't go back
            return(false);
        }
    }

    protected abstract FileListAdapterBase onCreateAdapter(Intent intent, boolean selectFolder);

    private boolean selectFolder;
    private View listLayout;
    private View listLoadingView;
    private TextView browseTitleText;
    private MaterialButton cancelButton;
    private MaterialButton selectButton;
    private FileListAdapterBase filesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final byte requestCode;
        Intent intent = this.getIntent();
        RecyclerView browseListView;

        //if intent is not set
        if(intent == null)
        {
            //create empty
            intent = new Intent();
        }

        //setup request code and cancel intent
        requestCode = BaseInputActivity.getRequestCode(intent);

        //get params
        selectFolder = intent.getBooleanExtra(ParamTypes.SelectFolder, false);

        //set layout
        setContentView(R.layout.list_title_view);

        //get displays
        listLayout = this.findViewById(R.id.List_Layout);
        listLoadingView = this.findViewById(R.id.List_Loading_View);
        browseTitleText = this.findViewById(R.id.List_Title_Text);
        browseListView = this.findViewById(R.id.List_View);
        cancelButton = this.findViewById(R.id.List_Cancel_Button);
        selectButton = this.findViewById(R.id.List_Select_Button);

        //setup displays
        browseListView.setHasFixedSize(true);
        browseListView.addItemDecoration(new DividerItemDecoration(this.getBaseContext(), LinearLayoutManager.VERTICAL));
        browseListView.setLayoutManager(new LinearLayoutManager(this.getBaseContext()));

        //set events
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent cancelIntent = new Intent();

                //set result
                BaseInputActivity.setRequestCode(cancelIntent, requestCode);
                setResult(RESULT_CANCELED, cancelIntent);
                FileBrowserBaseActivity.this.finish();
            }
        });
        selectButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent data = new Intent();

                //if selecting folder
                if(selectFolder)
                {
                    //add folder
                    data.putExtra(ParamTypes.FolderName, filesAdapter.getCurrentDirectory());
                }
                else
                {
                    //add files
                    data.putStringArrayListExtra(ParamTypes.FileIds, filesAdapter.getSelectedFileIds());
                    data.putStringArrayListExtra(ParamTypes.FileNames, filesAdapter.getSelectedFileFullNames());
                }

                //set result
                BaseInputActivity.setRequestCode(data, requestCode);
                setResult(RESULT_OK, data);
                FileBrowserBaseActivity.this.finish();
            }
        });

        //set title
        this.setTitle(selectFolder ? R.string.title_select_folder : R.string.title_select_file_or_files);

        //create adapter
        filesAdapter = onCreateAdapter(intent, selectFolder);
        browseListView.setAdapter(filesAdapter);
    }

    @Override
    public void onBackPressed()
    {
        //if couldn't go back
        if(!filesAdapter.goBack())
        {
            //call super
            super.onBackPressed();
        }
    }

    private void setLoading(boolean isLoading)
    {
        if(listLoadingView != null)
        {
            listLoadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if(selectButton != null)
        {
            selectButton.setEnabled(!isLoading && (selectFolder || (filesAdapter != null && filesAdapter.getSelectedFiles().size() > 0)));
        }
        if(cancelButton != null)
        {
            cancelButton.setEnabled(!isLoading);
        }
    }

    public static void updateProgress(Activity activity, final Resources res, final LinearProgressIndicator barView, final TextView textView, final int index, final int length, final long bytes, final long totalBytes, final double progress)
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                String text = (length > 1 ? ("(" + (index + 1) + "/" + length + ") ") : "") + Globals.getByteString(res, bytes) + res.getString(R.string.text_space_of_space) + Globals.getByteString(res, totalBytes);

                barView.setProgress((int)progress);
                textView.setText(text);
            }
        });
    }

    //Creates on path changed listener
    private OnPathChangedListener createOnPathChangedListener()
    {
        return(new OnPathChangedListener()
        {
            @Override
            public void onPathChanged(String path)
            {
                //if empty root
                if(path.equals(""))
                {
                    //display root
                    path = "/";
                }

                //update display
                final String setPath = path;
                FileBrowserBaseActivity.this.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        browseTitleText.setText(setPath);
                    }
                });
            }
        });
    }

    //Creates on selected files changed listener
    private OnSelectedFilesChangedListener createOnSelectedFilesChangedListener()
    {
        return(new OnSelectedFilesChangedListener()
        {
            @Override
            public void onSelectedFilesChanged(ArrayList<ItemBase> files)
            {
                int index;
                final int fileCount = files.size();
                boolean haveFiles = (fileCount > 0);
                StringBuilder filesString = new StringBuilder();
                Resources res = FileBrowserBaseActivity.this.getResources();

                //get files string
                for(index = 0; index < fileCount; index++)
                {
                    if(index > 0)
                    {
                        filesString.append("\r\n");
                    }
                    filesString.append(files.get(index).getAbsolutePath());
                }

                //update displays
                Globals.showSnackBar(listLayout, res.getQuantityString(R.plurals.text_files, fileCount, fileCount) + " " + res.getString(R.string.text_selected), (haveFiles ? filesString.toString() : null), false, !haveFiles);
                selectButton.setEnabled(haveFiles);
            }
        });
    }
}
