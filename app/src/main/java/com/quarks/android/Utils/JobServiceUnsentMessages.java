package com.quarks.android.Utils;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

public class JobServiceUnsentMessages extends JobService {

    private boolean jobCancelled = false;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d("TAG", "onStartJob");
        doBackWork(params);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d("TAG", "onStopJob");
        jobCancelled = true;
        return false;
    }

    private void doBackWork(final JobParameters params){
        Log.d("TAG", "doBackWork");
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    if(jobCancelled){
                        return;
                    }
                    Log.d("TAG", "RUN" + i);
                    try{
                        Thread.sleep(1000);
                    }catch (InterruptedException e){

                    }
                }
                Log.d("TAG", "Job Finished");
                jobFinished(params, false);
            }
        }).start();
    }
}
