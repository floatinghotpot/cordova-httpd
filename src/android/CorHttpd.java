package com.rjfun.cordova.httpd;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import java.net.Inet4Address;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
//import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.usage.UsageEvents;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.webkit.ValueCallback;
import android.webkit.WebView;

/**
 * This class echoes a string called from JavaScript.
 */
public class CorHttpd extends CordovaPlugin {

    @android.webkit.JavascriptInterface
    public void onData(String value) {
        Log.w(LOGTAG,"Returned value from JS ="+ value);
    }


    /** Common tag used for logging statements. */
    private static final String LOGTAG = "CorHttpd";
    
    /** Cordova Actions. */
    private static final String ACTION_START_SERVER = "startServer";
    private static final String ACTION_STOP_SERVER = "stopServer";
    private static final String ACTION_GET_URL = "getURL";
    private static final String ACTION_GET_LOCAL_PATH = "getLocalPath";
    private static final String ACTION_SET_ON_SERVE_CALLBACK = "onServe";
    
    private static final String OPT_WWW_ROOT = "www_root";
    private static final String OPT_PORT = "port";
    private static final String OPT_LOCALHOST_ONLY = "localhost_only";
    private static final String OPT_ALLOW_DIRECTORY_LISTING = "allowDirectoryListing";

    private String www_root = "";
	private int port = 8888;
	private boolean localhost_only = false;
    private boolean allowDirectoryListing = false;

	private String localPath = "";
	private WebServer server = null;
	private String	url = "";

    private CallbackContext _callback = null;
    private String _javascriptHandlerFunctionString = null;
    private ResponseCallback _onserverResponse = new ResponseCallback();

    private PluginResult setOnServeCallback(JSONArray inputs,CallbackContext callbackContext) {
        Log.w(LOGTAG, "setOnServeCallback "+inputs.toString());
        boolean shouldAdd = true;
        if(inputs.length() == 0) {
            this._callback = callbackContext;
        }
        else{
            try{
                String val = inputs.getString(0);
                Log.w(LOGTAG, "setOnServeCallback VAL = "+val);
                shouldAdd = !val.equals("null");
                if(shouldAdd){
                    this._callback = callbackContext;
                    this._javascriptHandlerFunctionString = val;
                }
                else{
                    this._callback =null;
                }
            }
            catch(JSONException ex){
                Log.w(LOGTAG,"JSON Exception setOnServeCallback "+ex.toString());
            }

        }
        PluginResult result = new PluginResult(PluginResult.Status.OK, "On Server Callback "+ (shouldAdd == true? "SET": "UNset"));
        result.setKeepCallback(shouldAdd);
        callbackContext.sendPluginResult(result);
        return result;
    }

    // This method will be fired later.
    public Void onServeEvent(JSONObject parameters) {
        Log.d(LOGTAG, "onServeEvent: " + (_callback == null ? "No Callback" : "will call back!"));
        if(_callback != null) {
                PluginResult result = new PluginResult(PluginResult.Status.OK, parameters);
                result.setKeepCallback(true);
                Log.d(LOGTAG, "Passing parameters in callback" + parameters.toString());
            _callback.sendPluginResult(result);
            CountDownLatch signal = new CountDownLatch(1); //1 to wait
            _onserverResponse.setSignal(signal);
            Log.i(LOGTAG, "---------------------- Signal set");
            try{
                getDynamicResponse();
                signal.await();
                Log.i(LOGTAG, "++++++++++++++++++++++ Signal UNLOCKED!");
            }
            catch(Exception ex){
                Log.e(LOGTAG, "Signal broken "+ex.toString());
            }

        }
        return null;
    }

    public void getDynamicResponse(){
//        final ResponseCallback respCb = this._onserverResponse;
        final CordovaWebView webView = this.webView;
        //Stupid cordova hides precious Android WebView
        final WebView androidWebView = (WebView)webView.getEngine().getView();
        final String jsHandler = this._javascriptHandlerFunctionString;
        //-------------------------
        if(jsHandler!= null){
            androidWebView.post(new Runnable() {
                @Override
                public void run() {
                    androidWebView.evaluateJavascript("(" + jsHandler + ")();", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String returnedValue) {
                            Log.d(LOGTAG, "VALUE RECVD from JAVASCRIPT"+returnedValue); //s is Java null
                            _onserverResponse.setResponse(returnedValue);
                            _onserverResponse._signal.countDown();
  //                          _onserverResponse.setResponse(returnedValue);
                        }
                    });
                }
            });
        }
    };

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
        }
        else if(ACTION_SET_ON_SERVE_CALLBACK.equals(action)){
            result = setOnServeCallback(inputs, callbackContext);
        }
        else {
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
                    	//if(InetAddressUtils.isIPv4Address(ip)) {
                        if(inetAddress instanceof Inet4Address){
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

        JSONObject options = inputs.optJSONObject(0);
        if(options == null) return null;
        
        www_root = options.optString(OPT_WWW_ROOT);
        port = options.optInt(OPT_PORT, 8888);
        localhost_only = options.optBoolean(OPT_LOCALHOST_ONLY, false);
        allowDirectoryListing = options.optBoolean(OPT_ALLOW_DIRECTORY_LISTING, false);
        
        if(www_root.startsWith("/")) {
    		//localPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        	localPath = www_root;
        } else {
        	//localPath = "file:///android_asset/www";
        	localPath = "www";
        	if(www_root.length()>0) {
        		localPath += "/";
        		localPath += www_root;
        	}
        }

        final CallbackContext delayCallback = callbackContext;
        cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
            public void run() {
				String errmsg = __startServer();
				if (errmsg.length() > 0) {
					delayCallback.error( errmsg );
				} else {
                    if (localhost_only) {
                        url = "http://127.0.0.1:" + port;
                    } else {
                        url = "http://" + __getLocalIpAddress() + ":" + port;
                    }
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

            ResponseCallback responseCallback = this._onserverResponse;

            EventCallBack eventCallBack = new EventCallBack(){
                public Void call(){
                    onServeEvent(this.getParameters());
                    return null;
                }
            };

            if(localhost_only) {
                InetSocketAddress localAddr = new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}), port);
                server = new WebServer(localAddr, f, allowDirectoryListing, eventCallBack, responseCallback);
            } else {
                server = new WebServer(port, f, allowDirectoryListing, eventCallBack, responseCallback);
            }
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
