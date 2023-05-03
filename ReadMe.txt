
Latest version of this project can be found at my github :
https://github.com/prav123248/RabbitMQ-Wireless-Collector

The JAR Build folder contains JARs of the System.


If you want to run this system, see Appendix A of my final report which is the same as below but properly formatted and with images.



Appendix A – Guide to setup the System
The following is a guide to setting up the necessary libraries and frameworks required for the system. There are four elements in this system – controllers, nodes, a RabbitMQ broker and a MySQL server. Controllers and nodes can run on any device but it is recommended to use a Windows machine for Controllers. The RabbitMQ broker is a server that receives and forwards messages to the right location and thus should be a Windows machine or dedicated server. The database server similarly should be a Windows machine or dedicated server due to the high resource usage that may be required. Both the broker and server can be used on the same machine as a Controller, and is the way in which the system was developed in.
For this guide, the configuration will be : 
•	One Controller : This controller runs on a computer running Windows 10.
•	Three Nodes :  Each node runs on a Raspberry PI. Make sure it is a Model B as the Model A is not supported by the Raspberry PI and overall performance is almost unusable due to the limited RAM. The guide will use the captureStub instead of VSCapture.
•	Broker and Server : Both of these components will run on a separate desktop machine running Windows 10. 
By the end of this guide, a system will be developed where three nodes are connected to a single controller. They will filter data from VSCapture and respond to controller requests like pulls. The controller add the pulled data to a database.
Step 1. Setting up the RabbitMQ Broker
To setup the RabbitMQ Broker, we need to install Erlang OTP and RabbitMQ Server. We will then use the RabbitMQ management interface to create an administrator account with the necessary access permissions. Detailed instructions below.
a.	Download the latest version Erlang/OTP available at erlang.org/downloads. The version that was used during development was 25.2.2 but the latest should work.
a.	Open the executable and follow the wizard instructions. No component selection changes should be necessary.
b.	Download RabbitMQ server executable from https://github.com/rabbitmq/rabbitmq-server/releases/ . Further details can be found on https://www.rabbitmq.com/download.html . Keep the “service” as ticked during the install wizard. Version 3.11.7 was used during development.
c.	Open CMD in administrator mode and use “cd” to navigate to your RabbitMQ installation directory. By default, this should be “Program Files/ RabbitMQ Server”. CD into “rabbitmq_server-[Your version]”/sbin.
d.	Enable the management GUI as shown below by doing “rabbitmq-plugins enable rabbitmq_management. (Other useful commands described in Appendix X).
 
e.	Visit http://localhost:15672/ where the RabbitMQ management interface is. Login using the default account username “guest”, password “guest”.
f.	As shown below, select the “Admin” tab on the navigation bar.
 
g.	Add an administrator user as shown below. Enter the desired username and password details then click “Admin” on the set panel. 
 	
      Adding the user should produce this in the list of users :
 

h.	Finally, click on your added user. Set the permission so that this account can perform actions in the “/” virtual host which the system uses.

 

Created account with the permission will now be : 

 
Having completed these steps, the broker has been setup successfully. There is no need to redo these steps after restarts as RabbitMQ runs as a service in the background as shown in the Figure below.
 
Step 2. Setting up the MySQL Database
On the same desktop machine as the broker, a MySQL server will be installed and run. For ease, both MySQL Server and MySQL workbench will be installed. The latter is not necessary but makes the process of setting up a database easier as it provides an interface, as well as providing a clear view of the results. Note that there is extra configuration if the server is on a different machine than the Controller such as making a new privileged user and setting up the right bindings for remote hosting. This guide will cover installing on the same device as the Controller.
a.	Download MySQL installer from https://dev.mysql.com/downloads/installer/ . Version 8.0.33 32-bit was used during setup. 
b.	MySQL installer makes it easier to install MySQL workbench and MySQL server. Install the x64 architecture with the latest version, preferably with the version for both matching. The system used version 8.0.32 during development.
c.	Open MySQL workbench and create a new connection by clicking on the “+” icon shown below (Ignore neonatal1, that was my existing connection). 

 

d.	Name the connection in the dialog menu and make sure standard TCP is selected as shown below.
 
e.	Enter the connection by clicking on it and signing in if necessary. Setting a password may be prompted on the first run.
 
f.	In query, run the SQL command above to ensure a database is created. 
The SQL server has now been configured and similar to the RabbitMQ server, can be found in services. No reconfigurations on restarts are necessary.
 
Step 3. Setting up the Controller
The controller will be running on the same desktop machine as the broker and server. This step involves downloading the system and using Maven to download the necessary libraries. We will then run the system in the Controller profile. 
a.	Download/Clone the latest repository of the system. It can be found at https://github.com/prav123248/RabbitMQ-Wireless-Collector.git . 
b.	Download IntelliJ Community edition and open the downloaded repository. IntelliJ will automatically use Maven and install necessary dependencies from the POM files. Go to File->Project Structure and select Java 17 SDK if it is not already selected. You may need to download the SDK if it not already present as an option.
c.	If a JAR already exists in the target folder (or one called JARBuild), you can choose to run the system from it. If you choose to do this, skip to sub-step (h).
d.	If you want to build or rebuild the JAR file, the first stage is obtaining maven. Visit https://dlcdn.apache.org/maven/maven-3/3.6.3/binaries/ and download the ZIP folder. Alternatively, use the latest version.
e.	Extract the ZIP to any location. Go inside the bin folder. Copy the path to that folder. Search “environment variables” on the Windows Search bar, click on “Path” then edit and paste the copied path to the end of the list. 
f.	Open a new command prompt in the rabbitMQCollector folder (it should be the current working directory) and type “mvn –version”. It should not give an error if it was added to the path correctly.
g.	Run “mvn clean package -DskipTests”. This creates a JAR file.
h.	Run the following command. “java -Dspring.profiles.active=”Controller” -jar target/rabbitMQ-Collector-0.0.1-SNAPSHOT.jar” (use the appropriate JAR directory/file name). This will run the build in Controller mode. 
i.	If any changes are made to the code, a new JAR will have to be built.

The controller has now been correctly setup. 
Step 4. Setting up the Node
As the node is on a Raspberry PI, the following steps are for the Raspbian OS and need to be repeated for each node of the system. The system and necessary technologies will be installed, specifically Java and a version of the JavaFX library for the Raspberry PI that needs to be installed. A JAR file will then be built which the system can use to run. For a node on Windows, the sub-steps for setting up the controller can be followed but changing “-Dspring.profiles.active=Controller” to “-Dspring.profiles.active=Node” in the Java command to run the JAR. The same JAR can be used for either type of profile. The following guide assumes Raspbian has been setup and is running.
a.	Download/Clone the latest repository of the system. It can be found at https://github.com/prav123248/RabbitMQ-Wireless-Collector.git . 
b.	Open a terminal and enter the command “sudo apt install openjdk-17-jdk” to install JDK 17.
c.	In the terminal, install maven with “sudo apt-get install maven”
d.	Visit “gluonhq.com/products/javafx” and scroll down and find a Linux aarch64 SDK as shown below.
 
e.	Extract the folder, placing it in the RabbitMQ-Collector folder (outside src). Below is a picture of what the folder containing the system should look like.

 
f.	If there is a JAR already within target (or a folder called JARBuild), then you can skip to sub-step (i) if you want to use that.
g.	Run “mvn clean package -DskipTests”. This will build a JAR build to run from. Skipping tests is necessary as without properties, the system will fail tests and thus the build. If you already have a target folder with a JAR, you don’t need to do this as you can run that build – as long as it represents the updated system.
h.	Once the command ends, there should be a target folder with a JAR.
i.	“java -Dspring.profiles.active="Node" --module-path /home/[USERNAME]/Desktop/NeonatalProject/rabbitMQ-Collector/javafx-sdk-20.0.1/lib --add-modules javafx.controls -jar /home/[USERNAME]/Desktop/NeonatalProject/rabbitMQ-Collector/target/rabbitMQ-Collector-0.0.1-SNAPSHOT.jar”. Run that command in the terminal. Make sure the the directories are the path to where the files are for you, it may vary. 
j.	If you make changes to the code, a new JAR build has to be built.








Step 4. Making the Connections
 
For the controller, enter the administrator username and password for the account created on the management interface for RabbitMQ. The IP address and server port will be the IP address of the device acting as the broker (in this case, the current device). The port number that RabbitMQ uses is typically 5672 but you should be able to check on the management interface if this fails. The controller name can be anything, as long as it is unique and not shared by other devices within the system. The DataSource URL is the JDBC connection that was defined. This will be in the form “jdbc:mysql://127.0.0.1:3306/neonatal” and can be found on MySQL workbench. The DataSource username and password will be the ones defined when setting up the server. The table name can be any string that can make a valid SQL table name, preferably one that does not already exist as the schema might not match when a controller receives data from a node. If you need to use a specific table that you have created, then it is fine to do so as long as the nodes are sending data that does not exceed the columns defined for that table. The driver for MySQL is “com.mysql.cj.jdbc.Driver”.
 
For the node, use the same details for the first four rows as you did for the controller. For controller name, enter the name of the controller created as that is what the node will initially try to authenticate with. At this point, VSCapture can be started. Once it has and is outputting CSV data, browse export path and select that raw VSCapture output. In order for the system to know what parameters to filter, check the VSCapture output and note the column index of the parameters you are interested in (starting from 0). 
 
For example, for the raw export output (from the Capture stub), to select the highlighted columns the parameters indexes field should be 0,1,2,6. This captures the first three columns (Line counter, time and ECG_HR) and the 6th which is SpO2. Avoid opening this file while the system is running as it may disrupt it and cause data loss. Make sure all nodes use the same parameters value with the same controller or there will be schema errors meaning invalid data will not be recorded. An example is if a Controller works with the parameters (0,1,2) but one node gives it data with (0,1,2,3). This is invalid and data from this node will not be added to the database.
Once each nodes have been configured, the system is ready for use. The controller should receive the authentication prompts from each of the nodes. 



