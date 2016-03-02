package com.rjfun.cordova.httpd;

        import org.json.JSONObject;

        import java.util.concurrent.Callable;
/**
 * Created by vitomacchia on 02/03/16.
 */
public class ResponseCallback implements Callable<NanoHTTPD.Response> {
    NanoHTTPD.Response response;
    String mimeType;

    public void setResponse(NanoHTTPD.Response response){
        this.response = response;
    }

    public void setResponse(String payload){
        this.response = new NanoHTTPD.Response(NanoHTTPD.HTTP_OK,mimeType,payload);
    }

    public NanoHTTPD.Response getResponse(){
        return this.response;
    }

    public ResponseCallback() {
        this.mimeType = NanoHTTPD.MIME_PLAINTEXT;
    }

    public NanoHTTPD.Response call() throws Exception {
        return this.response;
    }
};
