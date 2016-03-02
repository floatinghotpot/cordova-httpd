package com.rjfun.cordova.httpd;

import org.json.JSONObject;

import java.util.concurrent.Callable;

/**
 * Created by vitomacchia on 01/03/16.
 */
public class EventCallBack implements Callable<Void> {
    JSONObject parameters;

    public void setParameters(JSONObject parameters){
        this.parameters = parameters;
    }

    public JSONObject getParameters(){
        return this.parameters;
    }

    public EventCallBack() {
    }

    public Void call() throws Exception {
        return null;
    }
};