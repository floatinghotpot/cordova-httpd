var argscheck = require('cordova/argscheck'),
    exec = require('cordova/exec');

var corhttpd_exports = {};

corhttpd_exports.startServer = function(options, success, error) {
    var defaults = {
        'www_root': '',
        'port': 8888,
        'localhost_only': false,
        'allowDirectoryListing': false,  //android only
        'serverTimeout': 10 //android only
    };

    // Merge optional settings into defaults.
    for (var key in defaults) {
        if (typeof options[key] !== 'undefined') {
            defaults[key] = options[key];
        }
    }

    exec(success, error, "CorHttpd", "startServer", [defaults]);
};

corhttpd_exports.stopServer = function(success, error) {
    exec(success, error, "CorHttpd", "stopServer", []);
};

corhttpd_exports.getURL = function(success, error) {
    exec(success, error, "CorHttpd", "getURL", []);
};

corhttpd_exports.getLocalPath = function(success, error) {
    exec(success, error, "CorHttpd", "getLocalPath", []);
};

//--------- ANDROID ONLY ------------------------
var _setResponse = function(response){
    var resp = null;
    if(response!==null && typeof response !== 'undefined'){
        if(response.hasOwnProperty('mimeType') 
        && response.hasOwnProperty('statusCode') 
        && response.hasOwnProperty('content')){
            resp = response;
        }
    }
    exec(function(){
      //  console.log("Server running again...", arguments);
    },function(err){
        console.error("Error setting server response", err);
    },'CorHttpd','serverRun', [resp]);
}

corhttpd_exports.setRequestListener = function(requestHandler, success, error) {
    //This function will be called on each request
    function onRequest(request){
        if(typeof request === 'boolean'){
            success(request);
        }
        else{
            requestHandler(request, _setResponse);
        }
    }
    exec(onRequest, error, 'CorHttpd', 'onServe', [true]);
};

corhttpd_exports.unsetRequestListener = function(success, error) {
    exec(success, error, 'CorHttpd', 'onServe', [false]);
};

module.exports = corhttpd_exports;