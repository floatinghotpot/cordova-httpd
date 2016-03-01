package com.rjfun.cordova.httpd;

import java.util.concurrent.Callable;

/**
 * Created by vitomacchia on 01/03/16.
 */
public class EventCallBack implements Callable<Void> {
    private String uri;

    public void setUri(String uri){
        this.uri = uri;
    }

    public String getUri(){
        return this.uri;
    }

    public EventCallBack() {
    }

    public Void call() throws Exception {
        return null;
    }
};