package com.rjfun.cordova.httpd;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.Callable;

public class WebServer extends NanoHTTPD
{
	private final String LOGTAG = "WebServer";

    private EventCallBack _callback = null;
	private Boolean _allowDirectoryListing;
	private AndroidFile _rootDirectory;
    private ResponseCallback _responseCallback = null;

	public WebServer(InetSocketAddress localAddr, AndroidFile wwwroot, Boolean allowDirectoryListing, EventCallBack callback, ResponseCallback responseCallback) throws IOException {
		super(localAddr, wwwroot);
        this._rootDirectory=wwwroot;
        this._callback = callback;
        this._responseCallback = responseCallback;
		this._allowDirectoryListing = allowDirectoryListing;
	}

	public WebServer(int port, AndroidFile wwwroot, Boolean allowDirectoryListing, EventCallBack callback, ResponseCallback responseCallback) throws IOException {
		super(port, wwwroot);
        this._rootDirectory=wwwroot;
        this._callback = callback;
        this._responseCallback = responseCallback;
		this._allowDirectoryListing = allowDirectoryListing;
	}

	@Override
	public Response serve( String uri, String method, Properties header, Properties parms, Properties files )
	{
		Log.i(LOGTAG, method + " '" + uri + "' ");

        if(this._callback!=null){
            try{
                JSONObject cbParams = new JSONObject();
                cbParams.put("uri",uri);
                cbParams.put("method",method);
                Enumeration e = parms.propertyNames();
                JSONObject properties = new JSONObject();
                while (e.hasMoreElements()){
                    String value = (String)e.nextElement();
                    properties.put(value,parms.getProperty(value));
                }
                cbParams.put("properties",properties);

                e = header.propertyNames();
                JSONObject headers = new JSONObject();
                while (e.hasMoreElements()){
                    String value = (String)e.nextElement();
                    headers.put(value,header.getProperty(value));
                }
                cbParams.put("headers",headers);

                e = files.propertyNames();
                JSONObject _files = new JSONObject();
                while (e.hasMoreElements()){
                    String value = (String)e.nextElement();
                    _files.put(value,files.getProperty(value));
                }
                cbParams.put("files",_files);

                this._callback.setParameters(cbParams);
                Log.d(LOGTAG, "Calling callback "+uri);
                this._callback.call();
            }
            catch(Exception ex){
                Log.e(LOGTAG,"Exception invoking callback: "+ ex.toString());
            }
        }

        if(_responseCallback!=null){
            try {
                Response res = _responseCallback.call();
                if(res == null){
                    Log.i( LOGTAG, "response is null");
                    return new Response(HTTP_INTERNALERROR,MIME_PLAINTEXT,"Internal Server error" );
                }
                else{
                    return res;
                }
            }
            catch(Exception ex){
                Log.e(LOGTAG, "Server error:"+ ex.toString());
                return new Response(HTTP_INTERNALERROR,MIME_PLAINTEXT,"Server error: "+ ex.toString() );
            }
        }
        else{
            Response res = super.serveFile(uri, header, _rootDirectory, _allowDirectoryListing);
            if(res == null){
                Log.i( LOGTAG, "response is null");
                return new Response(HTTP_NOTFOUND, MIME_PLAINTEXT, "No such file or directory"+(uri == null ? "":uri.toString()));
            }
            else{
                return res;
            }
        }

	}
}
