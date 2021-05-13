package com.nikolaiapps.orbtrack;


import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;


//Thread task
public class ThreadTask<Params, Progress, Result>
{
    private static final Executor taskExecutor = Executors.newFixedThreadPool(4);

    private boolean running;
    private boolean cancelled;
    private long runRepeatMs;
    private Runnable runTask;
    private FutureTask<Result> taskThread;

    public ThreadTask()
    {
        running = false;
        runRepeatMs = 0;
        runTask = null;
        taskThread = null;
    }
    public ThreadTask(Runnable task, long repeatMs)
    {
        this();
        runTask = task;
        runRepeatMs = repeatMs;
        if(runRepeatMs < 0)
        {
            runRepeatMs = 0;
        }
    }

    @SuppressWarnings("unchecked")
    protected Result doInBackground(Params... params)
    {
        //needs to be overridden
        return(null);
    }

    @SafeVarargs
    protected final void publishProgress(Progress... values)
    {
        //needs to be overridden
    }

    protected void onPostExecute(Result result)
    {
        //needs to be overridden
    }

    protected void onCancelled(Result result)
    {
        //needs to be overridden
    }

    public static void purgeAll()
    {
        if(taskExecutor instanceof ThreadPoolExecutor)
        {
            ((ThreadPoolExecutor)taskExecutor).purge();
        }
    }

    private void setFinished(Result result)
    {
        //update status
        running = false;

        //if cancelled
        if(cancelled)
        {
            //send cancelled result
            onCancelled(result);
        }
        else
        {
            //send finished result
            onPostExecute(result);
        }
    }

    public void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch(Exception ex)
        {
            //do nothing
        }
    }

    @SafeVarargs
    public final void execute(final Params... params)
    {
        running = true;
        cancelled = false;

        taskThread = new FutureTask<>(new Runnable()
        {
            @Override
            public void run()
            {
                Result result;
                boolean done;

                do
                {
                    //run task
                    if(runTask == null)
                    {
                        //get result and finish
                        result = doInBackground(params);
                        done = true;
                    }
                    else
                    {
                        //run and repeat if desired
                        result = null;
                        runTask.run();
                        done = (runRepeatMs == 0);
                    }

                    //if not done and not cancelled
                    if(!done & !cancelled)
                    {
                        //wait for repeat delay
                        sleep(runRepeatMs);
                    }

                    //update if done/canceled
                    done = done || (taskThread != null && taskThread.isDone());
                    cancelled = cancelled || (taskThread != null && taskThread.isCancelled());

                } while(!done && !cancelled);

                //finish
                setFinished(result);
            }
        }, null);
        taskExecutor.execute(taskThread);
    }
    public void execute()
    {
        execute((Params[])null);
    }

    public void setRepeatMs(long ms)
    {
        runRepeatMs = ms;
    }

    public void cancel(boolean allow)
    {
        //if not already cancelled
        if(!cancelled)
        {
            //cancel and remove
            cancelled = true;
            if(taskThread != null)
            {
                taskThread.cancel(allow);
                if(taskExecutor instanceof ThreadPoolExecutor)
                {
                    ((ThreadPoolExecutor)taskExecutor).remove(taskThread);
                }
            }
        }
    }

    public boolean isRunning()
    {
        return(running);
    }

    public boolean isCancelled()
    {
        return(cancelled);
    }
}
