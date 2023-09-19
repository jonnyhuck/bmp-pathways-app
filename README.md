
***Please note that this software is now quite outdated and is unlikely to work on a modern device. We expect that it would require substantial work to make this application operational again. We leave it here purely for the reference of those reading the associated scientific publications.***

# Belfast Pathways GPS Tracker for Android

Android application for GPS tracking of participants, used by the ESRC-funded [Belfast Mobility Project]() in 2016-7.

The scripts and database structure that we used on the server are also available [here](https://github.com/jonnyhuck/bmp-pathways-server)

## Background

This software can be used to track participants at any temporal resolution that the researcher requires, though based upon our testing we found that collecting a location every 4-seconds provides a good compromise between detail and device battery life.

In order to facilitate good results, the Belfast Pathways application has a number of distinctive features:

* The app runs 'in the background' on the device, preventing accidental deactivation from the 
participant. If the app is closed, it will continue to collect data, and it will even resume data 
collection if the device is re-started.
* Should the participant not wish to be tracked, then they can use the 'Pause Tracking' functionality, whereby they can specify a period of time that they would like to tracking to pause for. After this time 
period has elapsed, tracking will resume.
* Data is logged to a local database, which is bulk-uploaded to the server when the device connects to a WiFi network with Internet connectivity. If the device remains connected to WiFi, then uploads will take place at regular intervals (we recommend two hours). Furthermore, the local data will only be deleted once the server has confirmed that it has been successfully uploaded. 

These measures minimise the potential for data loss, whilst also avoiding any cost from mobile data use on the part of the participant.

## Setup

To use the app it, set up the database and scripts on the server and set the server base URL at 
`Belfast-Tracker/app/src/main/res/values/strings.xml`. From there, you should be able to install the app to android devices and start collecting data!
