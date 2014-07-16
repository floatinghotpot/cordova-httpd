package com.rjfun.cordova.httpd;

import java.io.IOException;

public class WebServer extends NanoHTTPD
{
	public WebServer(int port, AndroidFile wwwroot ) throws IOException {
		super(port, wwwroot);
	}
}
