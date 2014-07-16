package com.rjfun.cordova.httpd;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
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
    private static final String ACTION_GET_URL = "getURL";
    private static final String ACTION_GET_LOCAL_PATH = "getLocalPath";
    
    private static final int	WWW_ROOT_ARG_INDEX = 0;
    private static final int	PORT_ARG_INDEX = 1;
    
	private String localPath = "";
	private int port = 8080;

	private WebServer server = null;
	private String	url = "";

    @Override
    public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
        PluginResult result = null;
        if (ACTION_START_SERVER.equals(action)) {
            result = startServer(inputs, callbackContext);
            
        } else if (ACTION_STOP_SERVER.equals(action)) {
            result = stopServer(inputs, callbackContext);
            
        } else if (ACTION_GET_URL.equals(action)) {
            result = getURL(inputs, callbackContext);
            
        } else if (ACTION_GET_LOCAL_PATH.equals(action)) {
            result = getLocalPath(inputs, callbackContext);
            
        } else {
            Log.d(LOGTAG, String.format("Invalid action passed: %s", action));
            result = new PluginResult(Status.INVALID_ACTION);
        }
        
        if(result != null) callbackContext.sendPluginResult( result );
        
        return true;
    }
    
    private String __getLocalIpAddress() {
    	try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (! inetAddress.isLoopbackAddress()) {
                    	String ip = inetAddress.getHostAddress();
                    	if(InetAddressUtils.isIPv4Address(ip)) {
                    		Log.w(LOGTAG, "local IP: "+ ip);
                    		return ip;
                    	}
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(LOGTAG, ex.toString());
        }
    	
		return "127.0.0.1";
    }

    private PluginResult startServer(JSONArray inputs, CallbackContext callbackContext) {
		Log.w(LOGTAG, "startServer");

		final String docRoot; 
        // Get the input data.
        try {
        	docRoot = inputs.getString( WWW_ROOT_ARG_INDEX );
            port = inputs.getInt( PORT_ARG_INDEX );
        } catch (JSONException exception) {
            Log.w(LOGTAG, String.format("JSON Exception: %s", exception.getMessage()));
            callbackContext.error( exception.getMessage() );
            return null;
        }
        
        if(docRoot.startsWith("/")) {
    		//localPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        	localPath = docRoot;
        } else {
        	//localPath = "file:///android_asset/www";
        	localPath = "www";
        	if(docRoot.length()>0) {
        		localPath += "/";
        		localPath += docRoot;
        	}
        }

        final CallbackContext delayCallback = callbackContext;
        cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
            public void run() {
				String errmsg = __startServer();
				if(errmsg != "") {
					delayCallback.error( errmsg );
				} else {
			        url = "http://" + __getLocalIpAddress() + ":" + port;
	                delayCallback.success( url );
				}
            }
        });
        
        return null;
    }
    
    private String __startServer() {
    	String errmsg = "";
    	try {
    		AndroidFile f = new AndroidFile(localPath);
    		
	        Context ctx = cordova.getActivity().getApplicationContext();
			AssetManager am = ctx.getResources().getAssets();
    		f.setAssetManager( am );
    		
			server = new WebServer(port, f);
		} catch (IOException e) {
			errmsg = String.format("IO Exception: %s", e.getMessage());
			Log.w(LOGTAG, errmsg);
		}
    	return errmsg;
    }

    private void __stopServer() {
		if (server != null) {
			server.stop();
			server = null;
		}
    }
    
   private PluginResult getURL(JSONArray inputs, CallbackContext callbackContext) {
		Log.w(LOGTAG, "getURL");
		
    	callbackContext.success( this.url );
        return null;
    }

    private PluginResult getLocalPath(JSONArray inputs, CallbackContext callbackContext) {
		Log.w(LOGTAG, "getLocalPath");
		
    	callbackContext.success( this.localPath );
        return null;
    }

    private PluginResult stopServer(JSONArray inputs, CallbackContext callbackContext) {
		Log.w(LOGTAG, "stopServer");
		
        final CallbackContext delayCallback = callbackContext;
        cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
            public void run() {
				__stopServer();
				url = "";
				localPath = "";
                delayCallback.success();
            }
        });
        
        return null;
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking		Flag indicating if multitasking is turned on for app
     */
    public void onPause(boolean multitasking) {
    	//if(! multitasking) __stopServer();
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking		Flag indicating if multitasking is turned on for app
     */
    public void onResume(boolean multitasking) {
    	//if(! multitasking) __startServer();
    }

    /**
     * The final call you receive before your activity is destroyed.
     */
    public void onDestroy() {
    	__stopServer();
    }
}
