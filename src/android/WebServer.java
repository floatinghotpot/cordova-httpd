package com.rjfun.cordova.httpd;

import java.io.File;
import java.io.IOException;

import android.os.Environment;

public class WebServer extends NanoHTTPD
{
	public WebServer(int port, String wwwroot ) throws IOException {
		super(port, new File( wwwroot ));
	}
}
