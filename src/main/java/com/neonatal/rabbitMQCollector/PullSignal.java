package com.neonatal.rabbitMQCollector;

import java.util.LinkedList;

public class PullSignal {

    private boolean pull = false;
    private String currentExportPath = "";
    private String rawOutputPath = "";
    private LinkedList<String> exportQueue = new LinkedList<>();
    private Integer pullCount = 0;

    public PullSignal(String rawPath) {
        this.rawOutputPath = rawPath;
    }

    public synchronized void setPull(boolean bool) {
        pull = bool;
    }

    public Integer getPullCount() {
        return pullCount;
    }

    private void incrementPullCount() {pullCount += 1;}

    public boolean getPull() {
        return pull;
    }

    public synchronized String newCurrentExport() {
        if (pull) {
            //Add current path to export queue
            exportQueue.add(currentExportPath);
        }

        //Increments counter for new file.
        incrementPullCount();

        //Change current path to new file
        currentExportPath = rawOutputPath + "/filteredExport" + (pullCount) + ".csv";
        return currentExportPath;

    }

    public synchronized String nextExport() {
        if (exportQueue.size() > 0) {
            return exportQueue.pop();
        }
        else {
            throw new IllegalStateException("No exports available.");
        }
    }
}
