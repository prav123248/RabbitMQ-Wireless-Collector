package com.neonatal.rabbitMQCollector;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class scheduleManager {

    private Timer scheduler  = new Timer();

    //Periodic schedule contains timers for pull requests that occur every X interval
    private HashMap<String, TimerTask> periodicSchedule = new HashMap<>();

    //Single schedule contains timers for pull requests that occur at X then ends.
    private HashMap<String, TimerTask> singleSchedule = new HashMap<>();

    public scheduleManager() {};

    public boolean schedulePull(Controller control, String IP, String name, long start, long interval) {
        TimerTask pullJob = new TimerTask() {
            public void run() {
                System.out.println("Sending scheduled pull request in " + start);
                control.sendPullRequest(IP, name);
            };
        };
        String identifier = name + "-" + IP;

        //Periodic tasks
        if (interval != 0) {
            try {
                scheduler.schedule(pullJob, start, interval);
            }
            catch(IllegalArgumentException e) {
                System.out.println(e.getMessage() + " - Job was not scheduled.");
                return false;
            }
            catch(IllegalStateException e) {
                System.out.println(e.getMessage() + " - Job was not scheduled.");
                return false;
            }

            if (periodicSchedule.containsKey(identifier)) {
                System.out.println("Removing existing periodic interval");
                cancelJob(identifier, false);
            }
            System.out.println("Adding periodic interval");
            periodicSchedule.put(identifier, pullJob);


        }
        //Single schedule
        else {
            try {
                scheduler.schedule(pullJob, start);
            }
            catch(IllegalArgumentException e) {
                System.out.println(e.getMessage() + " - Job was not scheduled.");
                return false;
            }
            catch(IllegalStateException e) {
                System.out.println(e.getMessage() + " - Job was not scheduled.");
                return false;
            }

            if (singleSchedule.containsKey(identifier)) {
                System.out.println("Removing existing interval job " + singleSchedule.get(identifier));
                cancelJob(identifier, false);
            }
            System.out.println("Adding periodic interval job " + singleSchedule.get(identifier));
            singleSchedule.put(identifier, pullJob);



        }

        return true;
    }

    private void cancelJob(String identifier, boolean periodic) {
        if (periodic) {
            periodicSchedule.remove(identifier).cancel();
        }
        else {
            singleSchedule.remove(identifier).cancel();
        }
    }

    public boolean cancelSchedule(String IP, String name, boolean periodic) {
        String identifier = name + "-" + IP;
        TimerTask pullJob;
        if (periodic) {
            pullJob = periodicSchedule.remove(identifier);
        }
        else {
            pullJob = singleSchedule.remove(identifier);
        }

        if (pullJob != null) {
            pullJob.cancel();
            System.out.println("Successfully cancelled scheduled task.");
            return true;
        }
        else {
            System.out.println("Could not find the scheduled pull job for " + identifier);
            return false;
        }
    }






}
