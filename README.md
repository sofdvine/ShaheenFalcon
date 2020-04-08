# Shaheen Falcon
Android application for web application penetration testing. Shaheen Falcon is a webview based web browser + collection of tools. It can find and exploit (both manually and automated script execution) web applications through an Android phone.

## Features

### Browse websites as a web browser
You can browse the web using Shaheen Falcon as a simple web browser.<br/>
<img src="/images/browse-web-1.png" width="240px" /> <img src="/images/browse-web-2.png" width="240px" />

### Write and Execute automated scripts / exploits
You can write and execute your own scripts (in JavaScript). Along with JavaScript inbuilt functionalities, some additional global functions (eg. `httpRequest` to send http request, `getBrowserCookies` to fetch browser cookies for a url, `getRequests` to fetch all http requests sent by browser for currently opened webpage etc.) are supported through the scripts. These script files are executed through [J2V8](https://github.com/eclipsesource/J2V8).

### View Page Source
Menu > View Source to view page source of currently opened webpage on Shaheen Falcon Web Browser.<br/>
<img src="/images/view-source-1.png" width="240px" /> <img src="/images/view-source-2.png" width="240px" />

### Log, View, Edit and Send HTTP Requests
##### Log all HTTP requests sent by browser for currently opened webpage
(Menu > Page Contents)<br/>
<img src="/images/log-view-edit-requests.png" width="240px" />

##### View and edit each request method, path, parameters, headers and request body
<img src="/images/log-view-edit-requests-2.png" width="240px" /> <img src="/images/log-view-edit-requests-3.png" width="240px" />

##### Send modified request
<img src="/images/log-view-edit-requests-4.png" width="240px" />

##### View and edit the response code, path, headers and response body
<img src="/images/view-edit-response-1.png" width="240px" /> <img src="/images/view-edit-response-3.png" width="240px" />

##### Load modified response on browser <br/>
<img src="/images/view-edit-response-4.png" width="240px" />

## Features under development
- Cookie Editor
- User-Agent Switch
- Port Scanner
- Raw TCP Socket Communication
- MITM Proxy
