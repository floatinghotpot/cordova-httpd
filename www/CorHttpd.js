var argscheck = require('cordova/argscheck'),
    exec = require('cordova/exec');

var corhttpd_exports = {};

corhttpd_exports.startServer = function(options, success, error) {
    var defaults = {
        'www_root': '',
        'port': 8888,
        'localhost_only': false,
        'allowDirectoryListing': false
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

corhttpd_exports.addOnServeListener = function(onServeCallback, requestHandler) {
    var reqHandler = typeof requestHandler === 'function' ? requestHandler.toString() : null;
    var args = [];
    if(reqHandler){
        args.push(reqHandler);
    }
    exec(onServeCallback, function(err) {
        console.log(err);
    }, 'CorHttpd', 'onServe', args);
};

corhttpd_exports.removeOnServeListener = function(success, error) {
    exec(success, error, 'CorHttpd', 'onServe', [null]);
};

module.exports = corhttpd_exports;