package com.rjfun.cordova.httpd;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class CorHttpd extends CordovaPlugin {

    /** Common tag used for logging statements. */
    private static final String LOGTAG = "CorHttpd";
    
    /** Cordova Actions. */
    private static final String ACTION_START_SERVER = "startServer";
    private static final String ACTION_STOP_SERVER = "stopServer";
    
    private static final int	WWW_ROOT_ARG_INDEX = 0;
    private static final int	PORT_ARG_INDEX = 1;
    
    private static final int	RESERVED_ARG_INDEX = 0;
    
    private int port = 8888;
    private String wwwRoot = "";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        PluginResult result = null;
        if (ACTION_START_SERVER.equals(action)) {
            result = startServer(inputs, callbackContext);
            
        } else if (ACTION_STOP_SERVER.equals(action)) {
            result = stopServer(inputs, callbackContext);
            
        } else {
            Log.d(LOGTAG, String.format("Invalid action passed: %s", action));
            result = new PluginResult(Status.INVALID_ACTION);
        }
        
        if(result != null) callbackContext.sendPluginResult( result );
        
        return true;
    }

    private PluginResult startServer(JSONArray inputs, CallbackContext callbackContext) {
         
        // Get the input data.
        try {
        	this.wwwRoot = inputs.getString( WWW_ROOT_ARG_INDEX );
            this.port = inputs.getBoolean( PORT_ARG_INDEX );
        } catch (JSONException exception) {
            Log.w(LOGTAG, String.format("Got JSON Exception: %s", exception.getMessage()));
            return new PluginResult(Status.JSON_EXCEPTION);
        }
        
        final CallbackContext delayCallback = callbackContext;
        cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
            public void run() {
				/* create web server and start */
                delayCallback.success();
            }
        });
        
        return null;
    }

    private PluginResult stopServer(JSONArray inputs, CallbackContext callbackContext) {
        
        // Get the input data.
        try {
        	String reserved = inputs.getString( RESERVED_ARG_INDEX );
        } catch (JSONException exception) {
            Log.w(LOGTAG, String.format("Got JSON Exception: %s", exception.getMessage()));
            return new PluginResult(Status.JSON_EXCEPTION);
        }
        
        final CallbackContext delayCallback = callbackContext;
        cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
            public void run() {
				/* stop web server */
                delayCallback.success();
            }
        });
        
        return null;
    }

}
