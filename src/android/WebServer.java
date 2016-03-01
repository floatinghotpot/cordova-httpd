package com.rjfun.cordova.httpd;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.Callable;

public class WebServer extends NanoHTTPD
{
	private final String LOGTAG = "WebServer";

    private EventCallBack _callback = null;

	public WebServer(InetSocketAddress localAddr, AndroidFile wwwroot, EventCallBack callback) throws IOException {
		super(localAddr, wwwroot);
        this._callback = callback;
	}

	public WebServer(int port, AndroidFile wwwroot, EventCallBack callback) throws IOException {
		super(port, wwwroot);
        this._callback = callback;
	}

	@Override
	public Response serve( String uri, String method, Properties header, Properties parms, Properties files )
	{
		Log.i(LOGTAG, method + " '" + uri + "' ");

        if(this._callback!=null){
            try{
                this._callback.setUri(uri);
                Log.d(LOGTAG, "Calling callback "+uri);
                this._callback.call();
            }
            catch(Exception ex){
                Log.e(LOGTAG,"Exception invoking callback: "+ ex.toString());
            }
        }

        Response res =  super.serve(uri, method, header, parms, files);
        if(res == null){
            Log.i( LOGTAG, "response is null");
            return new Response(HTTP_NOTFOUND, MIME_PLAINTEXT, "No such file or directory"+(uri == null ? "":uri.toString()));
        }
        else{
            return res;
        }
	}
}
