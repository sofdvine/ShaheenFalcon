# Shaheen Falcon Scripts

Automate exploits by writing JavaScript code.

## Features
- Automate exploits by writing a JavaScript file.
- Execute inbrowser script like console.
- Encrypt and Upload your script online then share the file url and password.
  Only those people can execute your script who has the file url with password


## Usage

### Online
The online scripts follows the JSON format as mentioned bellow
```JavaScript
{
    "type": "script",
    "type_code": 0, // 0 for script and 1 for bundle of scripts
    "script_name": "Name of the script",
    "content": "Base64 encoded encrypted (AES/CBC/PKCS5PADDING) JavaScript Code",
    "match_host": "For which host this script will be suggested to run (eg. www.duckduckgo.com)",
    "first_run": "Base64 encoded encrypted (AES/CBC/PKCS5PADDING) JavaScript Code to be executed in current webpage like console",
}
```

### Offline
