# Guide

## Wiki



## Goal

The goal of this app is to convert your home to be the cloud the easiest way. This app sync your device
files to the SMB storage in your LAN through LAN or through WAN. for security reasons, i recommend to
use LAN.

## Features

To make life easy, this application has factory default categories. The application scans pre-defined
directories and user-configured directories and sync them to the SMB server. the files are sorted according
to extension. you can customize your extensions and the directories to be scanned.

If your network contains more than one storage, in the settings you can choose with which one to work.

The application starts a service that handle the uploading in the background. In order to save battery
and ban overheating. the thread send the files one after the other and not simultanuesly. Moreover, large
files are not loaded into your RAM as whole but in chunks.

![Alt text](app/src/main/assets/sync_now.png)

# Behaviour

* Categories are determined according to extensions
* extensions are mapped to folders. Those folders are created in the FTP
* Categories looks for relevant files in pre-configured paths

# Algorithm

## Filtered Files

When tou press the Sync button, the files are filtered according to the next algorith, after that, the filtered files will be uploaded.
```
1. Collect all the files from the followed directoried recursivly
2. For each collected path
    2.1 if the path is directory then ignore
    2.2 else if the file status is SENT SUCCESSFULLY then ignore
    2.3 else if the file status is SENDING or PREPARING TO BE SENT and it is not timed out then ignore
    3.4 else, queue the file to be uploaded
3. foreach  filtered file in the queue
    3.1 if the file exists on the server (TODO: validate file size too) then ignore
    3.2 upload the file
    3.3 update the meta-data about the file

```

# Configuration - Database Structure

The followed path status are both available as standalone and as array
The "default" tells whether it is a fixed path or not.
if the path is a default path, then it cannot be deleted by the user.

```
{
    following_paths :   [
                            {
                                path:"",
                                default:true or false,
                                status:Constants.FOLLOWING_DIR or Constants.NOT_FOLLOWING_DIR
                            },
                            ...
                        ]
    "/emulated/0" : {
                        path:"/emulated/0",
                        default:true or false,
                        status:Constants.FOLLOWING_DIR or Constants.NOT_FOLLOWING_DIR
                     }

    ...
    "smb_default_path": [{
        "path": "/bla",
        "is_selected": true
    }]
    "smb_server":"192.168.1.1"
    "smb_username":"admin"
    "smb_password":"admin"
}
```

The Categories order
```
{
    categories: {
                    music:{name:"Music", status:Constants.FOLLOWING_DIR or Constants.NOT_FOLLOWING_DIR}
                    videos:{name:"Videos", status:Constants.FOLLOWING_DIR or Constants.NOT_FOLLOWING_DIR}
                    ...
                }
}
```

# The rules order

Rules manages the extensions and map to what folder each extension goes.
the default extension is "*" and its folder_name can be a valid folder name or the value "<IGNORE FILE>"
if the value <IGNORE FILE> is mapped to * then the default extensions will ignored.

```
{
    rules: [
        {extension : "mp3", folder_name:"music", category:"", status:Constants.FOLLOWING_DIR or Constants.NOT_FOLLOWING_DIR}
        {extension : "jpeg", folder_name:"photos", category:"", status:Constants.FOLLOWING_DIR or Constants.NOT_FOLLOWING_DIR}
        {extension : "jpg", folder_name:"photos", category:"", status:Constants.FOLLOWING_DIR or Constants.NOT_FOLLOWING_DIR}
        {extension : "png", folder_name:"photos", category:"", status:Constants.FOLLOWING_DIR or Constants.NOT_FOLLOWING_DIR}
        ...
    ]
}
```

## The Logs order (TBD)

the goals of the logs is to understand the sync details, files and failures. the representations is as follows:

```
{
    sync_logs : [
            sync_date: 'Jul 15 2017',
            file_name: ab.jpg,
            file_status: 'SUCCESS',
            dist_folder: '/photos'

}

```

The total_handled can be different than total as if the app crashed or the phone turned off then it will quit in the middle.
The files service `UploadFilesService` is responsible for updating this data. the data will be read from the analytics Activity. it will be shown in a special tab

## Notes

* When scanning files for sending, if a file in status sending, it will not be resent in case the sending
was updated 10 minutes before. the constant of the timeout is DEFAULT_SENDING_TIMEOUT_MS
