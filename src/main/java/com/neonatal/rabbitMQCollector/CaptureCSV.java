package com.neonatal.rabbitMQCollector;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

public class CaptureCSV implements Runnable{

    //Capture VS related (Input)
    private boolean shutdown = false;
    private String captureFileName;
    private int bufferSize = 1024;
    private int sleepInterval = 5;
    private boolean pause = false;
    private UUID patientID;

    //Used to determine if patient UUID should be written in file
    private boolean patientWriteID= true;

    //Filtered export related (Output)
    private List<Integer> params;
    private BufferedWriter writer;
    private PullSignal pullController;

    @Value("${rabbitmq.guiMode}")
    private boolean guiMode;


    public CaptureCSV(String csv, List<Integer> selectedParams, PullSignal puller) {
        captureFileName = csv;
        params = selectedParams;
        this.pullController = puller;
        Collections.sort(params);
        openNewOutput();
        patientID = UUID.randomUUID();
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

        //Loop until correct input provided
        while (true) {
            try {
                readOnlyFile = new RandomAccessFile(file, "r");
                break;
            } catch (FileNotFoundException e) {
                if (guiMode == true) {
                    Platform.runLater(() -> {
                        Alert approvalDialog = new Alert(Alert.AlertType.CONFIRMATION);
                        approvalDialog.setTitle("File not found");
                        approvalDialog.setHeaderText("Cannot find export file. Make sure VSCapture export file is at the specified path.");
                        approvalDialog.setContentText("Press ok when VSCapture file is in the correct location");

                        ButtonType ready = new ButtonType("Ready");
                        approvalDialog.getButtonTypes().setAll(ready);
                        approvalDialog.showAndWait();

                    });

                }
                else {
                    System.out.println(e.getMessage());
                    System.out.println("File not found, exiting, try again");
                    Scanner scan = new Scanner(System.in);
                    System.out.println("Enter any key when capture has started.");
                    scan.nextLine();
                }
            }
        }

        System.out.println("Capture file located.");

        FileChannel channel = readOnlyFile.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        StringBuilder nextLine = new StringBuilder();

        //Main Capture Loop
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
                    patientWriteID = true;
                }
            } else {
                //Read each line and sleep if nothing to read
                try {
                    if (channel.read(buffer) != -1) {
                        buffer.flip();
                        while (buffer.hasRemaining()) {
                            char nextChar = (char) buffer.get();
                            if (nextChar == '\n') {
                                if (!pause) {
                                    processLine(nextLine.toString());
                                }
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

    public void setPause(boolean pauseBool) {
        pause = pauseBool;
    }

    public boolean getPause() {return pause;}

    private void processLine(String line) {
        String filteredLine;

        //Write patient ID if required
        if (patientWriteID) {
            filteredLine = patientID.toString() + ",";
            patientWriteID = false;
        }
        else {
            filteredLine = ",";
        }


        String[] lineArray = line.split(",");



        if ((Collections.max(params) >= lineArray.length)) {
            System.out.println("Specified Parameters exceed raw captured parameter count.");
            System.out.println("Failed to process file - reconfigure parameters within limit.");
            System.exit(0);
        }

        for (int i=0; i<params.size()-1; i++) {
            filteredLine += lineArray[params.get(i)] + ",";
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

    //Opens a new file for filter exports
    private void openNewOutput() {

        while (true) {
            try {
                File newOutput = new File(pullController.newCurrentExport());
                writer = new BufferedWriter(new FileWriter(newOutput, true));
                break;
            } catch (IOException e) {
                //GUIMODE is used in case support for console returns
                if (guiMode == true) {
                    Alert approvalDialog = new Alert(Alert.AlertType.CONFIRMATION);
                    approvalDialog.setTitle("Error opening FileWriter for new filtered export file");
                    approvalDialog.setHeaderText("Directory does not exist or insufficient space or IO Error when creating/accessing file.");
                    approvalDialog.setContentText("The system will pause capture. Please pull any data and clear the system.");
                    ButtonType ready = new ButtonType("Ok");
                    approvalDialog.getButtonTypes().setAll(ready);
                    approvalDialog.showAndWait();
                    pause=true;

                }
                else {
                    System.out.println(e.getMessage());
                    System.out.println("Error opening FileWriter for new filtered export file");
                    System.out.println("Directory does not exist or insufficient space or IO Error when creating/accessing file");
                    Scanner scan = new Scanner(System.in);
                    System.out.println("Enter a key to retry - if file is already open this will increment to another file");
                    scan.nextLine();
                }
            }
        }
    }

    //Current filter export closes, create new will make a new one after closing
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

    public void switchPatient() {
        patientID = UUID.randomUUID();
        patientWriteID = true;
    }

    public void shutdown() {
        shutdown=true;
    }

}
