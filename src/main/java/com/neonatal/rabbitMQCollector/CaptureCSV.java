package com.neonatal.rabbitMQCollector;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CaptureCSV implements Runnable{

    //Capture VS related (Input)
    private boolean pullRequested = false;
    private boolean shutdown = false;
    private String captureFileName;
    private int bufferSize = 1024;
    private int sleepInterval = 5;

    //Filtered export related (Output)
    private List<Integer> params;
    private String outputFileName = "src\\main\\java\\com\\neonatal\\rabbitMQCollector\\filteredExport";
    private int pulledCount = 0;
    private BufferedWriter writer;

    public CaptureCSV(String csv, List<Integer> selectedParams) {
        captureFileName = csv;
        params = selectedParams;
        Collections.sort(params);
        openNewOutput();
    }

    public CaptureCSV(String csv,List<Integer> selectedParams, int stringBuffer, int processInterval) {
        captureFileName = csv;
        params = selectedParams;
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
                System.out.println("Successfully saved current file and shut down");
                break;
            }
            else if (pullRequested) {
                //Close current file and increment output
                closeFile(true);
                pullRequested = false;
                pulledCount += 1;
                notify();
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
                    System.out.println("An error occurred with an IO Operation when reading the capture file");
                }
                catch(InterruptedException e) {
                    System.out.println("Capturing process element was interrupted during sleep");
                }
            }
        }

        try {
            channel.close();
            readOnlyFile.close();
            System.out.println("Capture processing closed");
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
        }
        catch(IOException e) {
            System.out.println("An IOException occurred when processing a line.");
        }
    }

    private void openNewOutput() {
        pulledCount += 1;
        try {
            File newOutput = new File(outputFileName + pulledCount + ".csv");
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

    public void setPullRequested(boolean pull) {
        pullRequested = pull;
    }

    public boolean getPullRequested() {return pullRequested;}

    public void shutdown() {
        shutdown=true;
    }

    public String getPreviousExport() {
        if (pulledCount == 1) {
            throw new IllegalStateException("There is no previous exported file. Currently capturing data into the first file. Please pull first to retrieve the name");
        }
        else {
            return outputFileName + (pulledCount-1) + ".csv";
        }
    }
}
