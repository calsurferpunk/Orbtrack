package com.nikolaiapps.orbtrack;


import android.content.Intent;
import android.os.Environment;
import java.io.File;


public class SDCardBrowserActivity extends FileBrowserBaseActivity
{
    private static class Item extends ItemBase
    {
        private final File item;

        public Item(File setItem)
        {
            item = setItem;
        }

        @Override
        public boolean isDirectory()
        {
            return(item.isDirectory());
        }

        @Override
        public String getId()
        {
            return(item.getName());
        }

        @Override
        public String getName()
        {
            return(item.getName());
        }

        @Override
        public String getPath()
        {
            return(item.getPath());
        }

        @Override
        public String getPathId()
        {
            return(item.getPath());
        }

        @Override
        public String getAbsolutePath()
        {
            return(item.getAbsolutePath());
        }

        @Override
        public void getList(OnGotListListener listener)
        {
            int index = 0;
            String[] pathList = item.list();
            Item[] list = (pathList != null ? new Item[pathList.length] : null);

            if(pathList != null)
            {
                for(String currentPath : pathList)
                {
                    list[index] = new Item(new File(item.getPath() + "/" + currentPath));
                    index++;
                }
            }

            listener.onGotList(list, null);
        }

        @Override
        public ItemBase getParentFolder(String parentId)
        {
            Item parent = new Item(item.getParentFile());
            return(parent.item != null ? parent : null);
        }
    }

    private static class FileListAdapter extends FileListAdapterBase
    {
        private FileListAdapter(FileBrowserBaseActivity activity, File startDir, boolean selectFolder)
        {
            super(activity, new Item(new File("/")), new Item(startDir), false, selectFolder);
        }
    }

    @Override
    protected FileListAdapterBase onCreateAdapter(Intent intent, boolean selectFolder)
    {
        return(new FileListAdapter(this, Environment.getExternalStorageDirectory(), selectFolder));
    }
}
