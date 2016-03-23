## CorHttpd: embeded httpd for cordova ##

Supported platform:
* iOS
* Android

You can browse (locally or remotely) to access files in android/ios phone/pad:

* browse the files in mobile device with a browser in PC.
* copy files from mobile device to PC quickly, just with Wifi.
* use cordova webview to access the assets/www/ content with http protocol.

Why http access is good?

* Use wifi instead of cable, more convenient.
* For security reason, browser does not support local AJAX calls. It's big bottle neck to deploy HTML5 games to Cordova platform. 
* The most popular phaser.js game engine, a httpd is always required, as it use AJAX to load resource. 

## How to use CorHttpd? ##

Add the plugin to your cordova project, from npm repository (faster):
```bash
    cordova plugin add cordova-plugin-httpd
```
Or, add plugin directly from GitHub repo:
```bash
    cordova plugin add https://github.com/floatinghotpot/cordova-httpd.git
```
Quick start, copy the demo files, and just build to play.

    cp -r plugins/com.rjfun.cordova.httpd/test/* www/
    
## Javascript APIs ##

```javascript
startServer( options, success_callback, error_callback );

stopServer( success_callback, error_callback );

getURL( success_callback, error_callback );

getLocalPath( success_callback, error_callback );
```

Example code: (read the comments)

```javascript
    var httpd = null;
    function onDeviceReady() {
        httpd = ( cordova && cordova.plugins && cordova.plugins.CorHttpd ) ? cordova.plugins.CorHttpd : null;
    }
    function updateStatus() {
    	document.getElementById('location').innerHTML = "document.location.href: " + document.location.href;
    	if( httpd ) {
    	  /* use this function to get status of httpd
    	  * if server is up, it will return http://<server's ip>:port/
    	  * if server is down, it will return empty string ""
    	  */
    		httpd.getURL(function(url){
    			if(url.length > 0) {
    				document.getElementById('url').innerHTML = "server is up: <a href='" + url + "' target='_blank'>" + url + "</a>";
    			} else {
    				document.getElementById('url').innerHTML = "server is down.";
    			}
    		});
    		// call this function to retrieve the local path of the www root dir
    		httpd.getLocalPath(function(path){
    			document.getElementById('localpath').innerHTML = "<br/>localPath: " + path;
        	});
    	} else {
    		alert('CorHttpd plugin not available/ready.');
    	}
    }
    function startServer( wwwroot ) {
    	if ( httpd ) {
    	    // before start, check whether its up or not
    	    httpd.getURL(function(url){
    	    	if(url.length > 0) {
    	    		document.getElementById('url').innerHTML = "server is up: <a href='" + url + "' target='_blank'>" + url + "</a>";
	    	    } else {
	    	        /* wwwroot is the root dir of web server, it can be absolute or relative path
	    	        * if a relative path is given, it will be relative to cordova assets/www/ in APK.
	    	        * "", by default, it will point to cordova assets/www/, it's good to use 'htdocs' for 'www/htdocs'
	    	        * if a absolute path is given, it will access file system.
	    	        * "/", set the root dir as the www root, it maybe a security issue, but very powerful to browse all dir
	    	        */
    	    	    httpd.startServer({
    	    	    	'www_root' : wwwroot,
    	    	    	'port' : 8080,
    	    	    	'localhost_only' : false
    	    	    }, function( url ){
    	    	      // if server is up, it will return the url of http://<server ip>:port/
    	    	      // the ip is the active network connection
    	    	      // if no wifi or no cell, "127.0.0.1" will be returned.
        	    		document.getElementById('url').innerHTML = "server is started: <a href='" + url + "' target='_blank'>" + url + "</a>";
    	    	    }, function( error ){
    	    	    	document.getElementById('url').innerHTML = 'failed to start server: ' + error;
    	    	    });
    	    	}
    	    	
    	    });
    	} else {
    		alert('CorHttpd plugin not available/ready.');
    	}
    }
    function stopServer() {
    	if ( httpd ) {
    	    // call this API to stop web server
    	    httpd.stopServer(function(){
    	    	document.getElementById('url').innerHTML = 'server is stopped.';
    	    },function( error ){
    	    	document.getElementById('url').innerHTML = 'failed to stop server' + error;
    	    });
    	} else {
    		alert('CorHttpd plugin not available/ready.');
    	}
    }
```

# Credits #

This Cordova plugin is built based on following 2 projects, and thanks to the authors.

* [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd), written in java, for java / android, by psh.
* [CocoaHTTPServer](https://github.com/robbiehanson/CocoaHTTPServer), written in Obj-C, for iOS / Mac OS X, by robbiehanson.

You can use this plugin for FREE. Feel free to fork, improve and send pull request. 

If need prompt support, please [buy a license](http://rjfun.github.io/), you will be supported with high priority.

More Cordova/PhoneGap plugins by Raymond Xie, [find them in plugin registry](http://plugins.cordova.io/#/search?search=rjfun).

Project outsourcing and consulting service is also available. Please [contact us](mailto:rjfun.mobile@gmail.com) if you have the business needs.

