
var argscheck = require('cordova/argscheck'),
    exec = require('cordova/exec');

var corhttpd_exports = {};

corhttpd_exports.startServer = function(options, success, error) {
	  var defaults = {
			    'www_root': 'www',
			    'port': 8888
			  };
	  
	  var requiredOptions = ['www_root'];
	  // Merge optional settings into defaults.
	  for (var key in defaults) {
	    if (typeof options[key] !== 'undefined') {
	      defaults[key] = options[key];
	    }
	  }
	  // Check for and merge required settings into defaults.
	  requiredOptions.forEach(function(key) {
	    if (typeof options[key] === 'undefined') {
	      error('Failed to specify key: ' + key + '.');
	      return;
	    }
	    defaults[key] = options[key];
	  });
			  
    exec(success, error, "CorHttpd", "startServer", [ defaults[0], defaults[1] ]);
};

corhttpd_exports.stopServer = function(options, success, error) {
	  var defaults = {
			    'reserved': true
			    };
	  var requiredOptions = [];
	  // Merge optional settings into defaults.
	  for (var key in defaults) {
	    if (typeof options[key] !== 'undefined') {
	      defaults[key] = options[key];
	    }
	  }
	  // Check for and merge required settings into defaults.
	  requiredOptions.forEach(function(key) {
	    if (typeof options[key] === 'undefined') {
	      error('Failed to specify key: ' + key + '.');
	      return;
	    }
	    defaults[key] = options[key];
	  });
	  
	  exec(success, error, "CorHttpd", "startServer", [ defaults[0] ]);
};

module.exports = corhttpd_exports;

