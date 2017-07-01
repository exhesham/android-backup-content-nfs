# Guide

This Application categorize your files and let you choose what category to be synced into the FTP. The categories are determined according to extensions. Those extensions are predefined and you can customize them to support more file types.

The App will create folders in the FTP. Those folders are configured in the Rules section of this app. The files will be uploaded each time you press the sync button on the main screen.


![Alt text](app/src/main/assets/sync_now.png)

# Behaviour

* Categories are determined according to extensions
* extensions are mapped to folders. Those folders are created in the FTP
* Categories looks for relevant files in pre-configured paths

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

The Categories order:
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
Rules manages the extenstions and map to what folder each extenstion goes.
the default extension is "*" and its folder_name can be a valid folder name or the value "<IGNORE FILE>"
if the value <IGNORE FILE> is mapped to * then the default extensions will ignored
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



## Notes:
* When scanning files for sending, if a file in status sending, it will not be resent in case the sending
was updated 10 minutes before. the constant of the timeout is DEFAULT_SENDING_TIMEOUT_MS
