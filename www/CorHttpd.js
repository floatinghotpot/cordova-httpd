
var argscheck = require('cordova/argscheck'),
    exec = require('cordova/exec');

var corhttpd_exports = {};

corhttpd_exports.startServer = function(options, success, error) {
    var defaults = {
        // For Cordova this is `www`. For Capacitor it's `public`.
        'www_dir_name': 'www',  
        'www_root': '',
        'port': 8888,
        'localhost_only': false
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

// Compatibility for @ionic-native/httpd plugin. Keep this until the following issue is resolved:
// https://github.com/danielsogl/awesome-cordova-plugins/issues/3805
corhttpd_exports.getUrl = corhttpd_exports.getURL

corhttpd_exports.getLocalPath = function(success, error) {
    exec(success, error, "CorHttpd", "getLocalPath", []);
};

module.exports = corhttpd_exports;

