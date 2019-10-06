var enabled = true;

chrome.runtime.onMessage.addListener(function(request, sender, sendResponse){
    if (request == "getState") {
        sendResponse(enabled);
        return;
    }
    
    enabled = request == "ENABLE";
    //enabled ? chrome.browserAction.setIcon({path: "assets/icon128.png" }) : chrome.browserAction.setIcon({ path:"assets/icon128grey.png" });
});

chrome.webRequest.onBeforeRequest.addListener(function(info) { 
    if(!enabled || (!info.url.split("?")[0].split("#")[0].endsWith(".m3u8") 
        && !info.url.split("?")[0].split("#")[0].endsWith(".mpd") 
        && !info.url.split("?")[0].split("#")[0].endsWith("Manifest"))) {
        return null;
    }

    var playerUrl = chrome.runtime.getURL('src/index.html') + "#" + info.url;

    if(navigator.userAgent.toLowerCase().indexOf('firefox') > -1){
        chrome.tabs.update(info.tabId, {url: playerUrl});
        return {cancel: true};
    }
        
    return { redirectUrl:  playerUrl };
}, {urls: ["*://*/*.m3u8*", "*://*/*.mpd*", "*://*/*/Manifest*"], types:["main_frame"]}, ["blocking"]);