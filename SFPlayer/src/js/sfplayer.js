var manifestUri;
const licMap = {
    'widevine': 'com.widevine.alpha',
    'playready': 'com.microsoft.playready'
}, modalContainer = document.getElementById('modal-container');
let splitted = document.URL.split('#'),
    videoUrl, licenseUrl, licenseMode, extraHeaders;
if (splitted.length > 1) {
    //ok
    videoUrl = splitted[1];
    manifestUri = videoUrl;
}
if (splitted.length > 3) {
    licenseUrl = splitted[2].length ? splitted[2] : undefined;
    licenseMode = splitted[3].length ? splitted[3] : undefined;
}
if (splitted.length > 4) {
    extraHeaders = JSON.parse(window.atob(splitted[4]));
}
console.log(videoUrl, licenseMode, licenseUrl);

function sanitizeLIC(licMode) {
    return licMap[licMode] || licMode;
}

function getTitleStr(elem, titleString) {
    if (titleString instanceof Array) {
        let titleStr = '';
        titleString.forEach(k => {
            if (k.startsWith('.')) {
                titleStr += k.slice(1);
            } else {
                titleStr += elem[k];
            }
        });
        return titleStr;
    } else {
        return elem[titleString];
    }
}

function openListDialog(list, viewMap, callback) {
    let titleString = viewMap['title'],
        subtitleString = viewMap['subtitle'];
    let root = document.createElement('div');
    root.className = 'list';

    list.forEach((elem, idx) => {
        let item = document.createElement('div'),
            title = document.createElement('div'),
            subtitle = document.createElement('div');
        item.className = 'item';
        title.className = 'item-title';
        subtitle.className = 'item-subtitle';
        title.innerHTML = getTitleStr(elem, titleString);
        subtitle.innerHTML = getTitleStr(elem, subtitleString);
        item.appendChild(title);
        item.appendChild(subtitle);
        if (elem.active) {
            item.classList.add('active');
        }
        if (typeof callback === 'function') {
            item.onclick = () => {
                callback(elem, idx, item);
                modalContainer.removeAttribute('style');
            };
        }
        root.appendChild(item);
    })
    modalContainer.innerHTML = '';
    modalContainer.appendChild(root);
    modalContainer.style.display = 'block';
}

function initApp() {
    // Install built-in polyfills to patch browser incompatibilities.
    shaka.polyfill.installAll();

    // Check to see if the browser supports the basic APIs Shaka needs.
    if (shaka.Player.isBrowserSupported()) {
        // Everything looks good!
        initPlayer();
    } else {
        // This browser does not have the minimum set of APIs we need.
        console.error('Browser not supported!');
    }
}

function initPlayer() {
    // Create a Player instance.
    var video = document.getElementById('video');
    var player = new shaka.Player(video);

    // Attach player to the window to make it easy to access in the JS console.
    window.player = player;

    // Listen for error events.
    player.addEventListener('error', onErrorEvent);

    if (licenseMode) {
        let servers = {};
        servers[sanitizeLIC(licenseMode)] = licenseUrl;
        player.configure({
            drm: {
                servers
            }
        });
    }

    if (extraHeaders) {
        player.getNetworkingEngine().registerRequestFilter(function (type, request) {
            // Only add headers to license requests:
            if (extraHeaders["LICENSE"] && type == shaka.net.NetworkingEngine.RequestType.LICENSE) {
                for (let headerName in extraHeaders["LICENSE"]) {
                    request.headers[headerName] = extraHeaders["LICENSE"][headerName];
                }
            }
            if (extraHeaders["MANIFEST"] && type == shaka.net.NetworkingEngine.RequestType.MANIFEST) {
                for (let headerName in extraHeaders["MANIFEST"]) {
                    request.headers[headerName] = extraHeaders["MANIFEST"][headerName];
                }
            }
            if (extraHeaders["SEGMENT"] && type == shaka.net.NetworkingEngine.RequestType.SEGMENT) {
                for (let headerName in extraHeaders["SEGMENT"]) {
                    request.headers[headerName] = extraHeaders["SEGMENT"][headerName];
                }
            }
            if (extraHeaders["APP"] && type == shaka.net.NetworkingEngine.RequestType.APP) {
                for (let headerName in extraHeaders["APP"]) {
                    request.headers[headerName] = extraHeaders["APP"][headerName];
                }
            }
        });
    }

    document.getElementById('btn-list-tracks').addEventListener('click', () => {
        openListDialog(player.getVariantTracks(), {
            title: ['width', '.x', 'height'],
            subtitle: 'codecs'
        }, (track, idx, domItem) => {
            let abr = player.getConfiguration().abr;
            abr["enabled"] = false;
            player.configure("abr", abr);
            document.getElementsByClassName("active")[0].classList.remove('active');
            domItem.classList.add('active');
            player.selectVariantTrack(track);
        });
    })

    // Try to load a manifest.
    // This is an asynchronous process.
    player.load(manifestUri).then(function () {
        // This runs if the asynchronous load is successful.
        console.log('The video has now been loaded!');
    }).catch(onError); // onError is executed if the asynchronous load fails.
}

function onErrorEvent(event) {
    // Extract the shaka.util.Error object from the event.
    onError(event.detail);
}

function onError(error) {
    // Log the error.
    console.error('Error code', error.code, 'object', error);
}

document.addEventListener('DOMContentLoaded', initApp);