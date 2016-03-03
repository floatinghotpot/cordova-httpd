package com.rjfun.cordova.httpd;

        import android.util.Log;

        import org.json.JSONException;
        import org.json.JSONObject;

        import java.util.concurrent.Callable;
        import java.util.concurrent.CountDownLatch;

/**
 * Created by vitomacchia on 02/03/16.
 */
public class ResponseCallback implements Callable<NanoHTTPD.Response> {
    private final String LOGTAG = "ResponseCallback";

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
            this._response = new NanoHTTPD.Response( NanoHTTPD.HTTP_INTERNALERROR,NanoHTTPD.MIME_PLAINTEXT,"Internal Server error: "+ ex.toString());
        }
    }

    public void setNullResponse(){
        this._response = null;
    }

    public NanoHTTPD.Response getResponse(){
        return this._response;
    }

    public ResponseCallback() {
    }

    public NanoHTTPD.Response call() throws Exception {
        return this._response;
    }
};
