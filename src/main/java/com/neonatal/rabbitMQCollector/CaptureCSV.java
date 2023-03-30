package com.neonatal.rabbitMQCollector;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CaptureCSV implements Runnable{

    //Capture VS related (Input)
    private boolean shutdown = false;
    private String captureFileName;
    private int bufferSize = 1024;
    private int sleepInterval = 5;

    //Filtered export related (Output)
    private List<Integer> params;
    private BufferedWriter writer;
    private PullSignal pullController;

    public CaptureCSV(String csv, List<Integer> selectedParams, PullSignal puller) {
        captureFileName = csv;
        params = selectedParams;
        this.pullController = puller;
        Collections.sort(params);
        openNewOutput();
    }

    public CaptureCSV(String csv,List<Integer> selectedParams, int stringBuffer, int processInterval, PullSignal puller) {
        captureFileName = csv;
        params = selectedParams;
        pullController = puller;
        Collections.sort(params);
        bufferSize = stringBuffer;
        sleepInterval = processInterval;
        openNewOutput();
    }

    public void run() {
        File file = new File(captureFileName);
        RandomAccessFile readOnlyFile;
        try {
            readOnlyFile = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
            System.out.println("File not found, exiting, try again");
            return;
        }


        FileChannel channel = readOnlyFile.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        StringBuilder nextLine = new StringBuilder();

        while (true) {
            if (shutdown) {
                closeFile(false);
                System.out.println("Successfully saved current export file");
                break;
            }
            else if (pullController.getPull()) {
                //Close current file and increment output
                synchronized (pullController) {
                    closeFile(true);
                    pullController.setPull(false);
                    pullController.notify();
                }
            } else {
                try {
                    if (channel.read(buffer) != -1) {
                        buffer.flip();
                        while (buffer.hasRemaining()) {
                            char nextChar = (char) buffer.get();
                            if (nextChar == '\n') {
                                processLine(nextLine.toString());
                                nextLine.setLength(0);
                            } else {
                                nextLine.append(nextChar);
                            }
                        }
                        buffer.clear();
                    }
                    else {
                        Thread.sleep(sleepInterval);
                    }

                }
                catch (IOException e) {
                    System.out.println("An error occurred with an IO Operation when reading the capture file" + e.getMessage());
                }
                catch(InterruptedException e) {
                    System.out.println("Capturing process element was interrupted during sleep");
                }
            }
        }

        try {
            channel.close();
            readOnlyFile.close();
            System.out.println("Capture processing closed and filterer successfully shutdown");
        }
        catch(IOException e) {
            System.out.println("Error while closing processed output files.");
        }


    }

    private void processLine(String line) {
        String filteredLine = "";
        String[] lineArray = line.split(",");

        if (!(params.get(params.size()-1) < lineArray.length)) {
            System.out.println("Specified Parameters exceed raw captured parameter count.");
            System.out.println("Failed to process file - reconfigure parameters within limit.");
            System.exit(0);
        }

        for (int i=0; i<params.size()-1; i++) {
            filteredLine += lineArray[i] + ",";
        }

        filteredLine += lineArray[params.get(params.size()-1)];
        try {
            writer.write(filteredLine);
            writer.newLine();
            writer.flush();
        }
        catch(IOException e) {
            System.out.println("An IOException occurred when processing a line.");
        }
    }

    private void openNewOutput() {

        try {
            File newOutput = new File(pullController.newCurrentExport());
            writer = new BufferedWriter(new FileWriter(newOutput, true));
        }
        catch(IOException e) {
            System.out.println(e.getMessage());
            System.out.println("Error opening FileWriter for new filtered export file");
            System.out.println("Directory does not exist or insufficient space or IO Error when creating/accessing file");
            System.out.println("Please rerun");
            System.exit(0);
        }
    }

    private void closeFile(boolean createNew) {
        try {
            writer.close();
        }
        catch(IOException e) {
            System.out.println("A problem occurred closing the writer/export file.");
            System.exit(0);
        }
        if (createNew) {
            openNewOutput();
        }

        return;

    }

    public void shutdown() {
        shutdown=true;
    }

}
