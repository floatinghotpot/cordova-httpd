package com.rjfun.cordova.httpd;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WebServer extends NanoHTTPD
{
	private final String LOGTAG = "WebServer";
	private Boolean _allowDirectoryListing;
	private AndroidFile _rootDirectory;

    private CorHttpd _plugin;

    private int responseTimeout = 10;

    private ResponseHandler responseHandler = new ResponseHandler();

	public WebServer(InetSocketAddress localAddr, AndroidFile wwwroot, Boolean allowDirectoryListing, int responseTimeout, CorHttpd plugin) throws IOException {
		super(localAddr, wwwroot);
        this._rootDirectory=wwwroot;
		this._allowDirectoryListing = allowDirectoryListing;
        this._plugin = plugin;
        this.responseTimeout = responseTimeout;
	}

	public WebServer(int port, AndroidFile wwwroot, Boolean allowDirectoryListing, int responseTimeout, CorHttpd plugin) throws IOException {
		super(port, wwwroot);
        this._rootDirectory=wwwroot;
		this._allowDirectoryListing = allowDirectoryListing;
        this._plugin = plugin;
        this.responseTimeout = responseTimeout;
	}

    /**
     * Created by vitomacchia on 02/03/16.
     */
    public class ResponseHandler{
        private final String LOGTAG = "ResponseHandler";

        NanoHTTPD.Response _response = null;
        String _mimeType = NanoHTTPD.MIME_PLAINTEXT;
        JSONObject _params = null;
        CountDownLatch _signal=null;

        public void setSignal(CountDownLatch signal){
            this._signal=signal;
        }

        private String mapStatusCode(int code){
            if(code <= 200 ){
                return NanoHTTPD.HTTP_OK;
            }
            else if(code==206){
                return NanoHTTPD.HTTP_PARTIALCONTENT;
            }
            else if(code==416){
                return NanoHTTPD.HTTP_RANGE_NOT_SATISFIABLE;
            }
            else if(code==301){
                return NanoHTTPD.HTTP_REDIRECT;
            }
            else if(code==304){
                return NanoHTTPD.HTTP_NOTMODIFIED;
            }
            else if(code==403){
                return NanoHTTPD.HTTP_FORBIDDEN;
            }
            else if(code==404){
                return NanoHTTPD.HTTP_NOTFOUND;
            }
            else if(code==400){
                return NanoHTTPD.HTTP_BADREQUEST;
            }
            else if(code==500){
                return NanoHTTPD.HTTP_INTERNALERROR;
            }
            else{//501
                return NanoHTTPD.HTTP_NOTIMPLEMENTED;
            }
        }

        public void setParams(JSONObject params){
            this._params = params;
            try{
                this._mimeType = _params.getString("mimeType");
                String payload = _params.getString("content");
                int statusCode = _params.getInt("statusCode");
                this._response = new NanoHTTPD.Response(mapStatusCode(statusCode),_mimeType,payload);
            }
            catch(JSONException ex){
                Log.w(LOGTAG, "JSON Exception parsing params: "+ex.toString());
                this.setInternalServerErrorResponse(ex.toString());
            }
        }

        public void setInternalServerErrorResponse(String errorString){
            this._response = new NanoHTTPD.Response( NanoHTTPD.HTTP_INTERNALERROR,NanoHTTPD.MIME_PLAINTEXT,"Internal Server error"+ errorString);
        }

        public void setNullResponse(){
            this._response = null;
        }

        public NanoHTTPD.Response getResponse(){
            return this._response;
        }

        public ResponseHandler() {
        }
    };


    public void setRun(JSONObject responseParams){
        if(responseHandler._signal!=null){
            try {
                if(responseParams == null){
                    Log.i(LOGTAG,"Received null: attempt to serve static content");
                    //Will cause the server to try to find a file on the disk with normal static behavior
                    responseHandler.setNullResponse();
                }else{
                    Log.i(LOGTAG,"Received response params from app: "+responseParams.toString());
                    //JSONObject returnedParams = responseParams;// new JSONObject(returnedValue);
                    responseHandler.setParams(responseParams);
                }
            }
            catch(Exception ex){
                Log.e(LOGTAG,"Error parsing returned value: "+ex.toString());
                responseHandler.setInternalServerErrorResponse(ex.toString());
            }
            responseHandler._signal.countDown();
        }
    }

    public Void onServeEvent(JSONObject parameters) {
        CountDownLatch signal = new CountDownLatch(1); //1 to wait
        responseHandler.setSignal(signal);
        Log.i(LOGTAG, "---------------------- Signal set");
        try{
            _plugin.sendRequestBackToJavascript(parameters);

            signal.await(responseTimeout, TimeUnit.SECONDS);
            Log.i(LOGTAG, "++++++++++++++++++++++ Signal UNLOCKED!");
        }
        catch(InterruptedException iex){
            Log.e(LOGTAG, "Server response timeout ");
            responseHandler.setInternalServerErrorResponse("Operation timeout");
        }
        catch(Exception ex){
            Log.e(LOGTAG, "Signal broken "+ex.toString());
            responseHandler.setInternalServerErrorResponse(ex.toString());
        }
        return null;
    }

	@Override
	public Response serve( String uri, String method, Properties header, Properties parms, Properties files )
	{
		Log.i(LOGTAG, method + " '" + uri + "' ");

        //Dynamic behavior
        if(this._plugin.getOnServeCallback() != null){
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
                cbParams.put("files", _files);
                Log.d(LOGTAG, "Calling Javascript callback ");
                //Call javascript and return request information
                onServeEvent(cbParams);
                //Will wait here until Javascript responds
                Response res = responseHandler.getResponse();
                if(res == null){ //Try static behavior
                    return super.serveFile(uri, header, _rootDirectory, _allowDirectoryListing);
                }
                else{
                    return res;
                }
            }
            catch(Exception ex){
                Log.e(LOGTAG,"Exception invoking callback: "+ ex.toString());
                return new Response(HTTP_INTERNALERROR,MIME_PLAINTEXT,"Internal Server error: "+ ex.toString() );
            }
        }
        //Static behavior
        else{
            return super.serveFile(uri, header, _rootDirectory, _allowDirectoryListing);
        }
	}
}