package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;


public abstract class DatabaseManager
{
    private static Database db = null;
    private static SQLiteDatabase readableDb = null;
    private static SQLiteDatabase writableDb = null;

    //Gets the database object
    public static synchronized Database getDb(Context context, Globals.OnProgressChangedListener listener)
    {
        if(db == null)
        {
            db = new Database(context);
        }
        if(listener != null)
        {
            db.setProgressListener(listener);
        }
        return(db);
    }

    //Gets the database as writable/read only
    public static synchronized SQLiteDatabase get(Context context, boolean writable)
    {
        getDb(context, null);
        if(writable)
        {
            if(writableDb == null)
            {
                writableDb = db.getWritableDatabase();
            }

            return(writableDb);
        }
        else
        {
            if(readableDb == null)
            {
                readableDb = db.getReadableDatabase();
            }

            return(readableDb);
        }
    }

    public static synchronized void close()
    {
        if(db != null)
        {
            db.close();
            db = null;
            readableDb = null;
            writableDb = null;
        }
    }
}
