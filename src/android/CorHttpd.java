package com.rjfun.cordova.httpd;

import java.io.File;
import java.io.IOException;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

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
    
	private NanoHTTPD server = null;

    @Override
    public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
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
        final String wwwRoot; 
        final int port;
        // Get the input data.
        try {
        	wwwRoot = inputs.getString( WWW_ROOT_ARG_INDEX );
            port = inputs.getInt( PORT_ARG_INDEX );
        } catch (JSONException exception) {
            Log.w(LOGTAG, String.format("Got JSON Exception: %s", exception.getMessage()));
            return new PluginResult(Status.JSON_EXCEPTION);
        }
        
        if(wwwRoot.length() >0 && wwwRoot.charAt(0)=='/') {
        	callbackContext.error("Absolute path not allowed due to security reason.");
        	return null;
        }
        
        final String fullPath = "www/" + wwwRoot;

/*        
		try {
	        Context ctx = cordova.getActivity().getApplicationContext();
			AssetManager am = ctx.getResources().getAssets();
			AssetFileDescriptor afd = am.openFd(fullPath);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
*/        
		WifiManager wifiManager = (WifiManager) cordova.getActivity().getSystemService(Context.WIFI_SERVICE);
		int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
		final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
		String url = ("http://" + formatedIpAddress + ":" + port);

        final CallbackContext delayCallback = callbackContext;
        cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
            public void run() {

				try {
					//File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
					//File f = new File( fullPath );
					File f = new File( "/" );
					server = new NanoHTTPD(port, f);
					
				} catch (IOException e) {
					delayCallback.error("failed to start httpd");
				}
				
				// trigger event 'ServerStarted'
				
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
				if (server != null) {
					server.stop();
					server = null;
				}
                delayCallback.success();
            }
        });
        
        return null;
    }

}
