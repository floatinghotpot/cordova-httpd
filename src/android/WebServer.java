package com.rjfun.cordova.httpd;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.Override;
import java.lang.String;
import java.net.MalformedURLException;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;


import android.util.Log;

public class WebServer extends NanoHTTPD
{
    private Map customPaths = null;
    private String[] customURIs = new String[0];
    private final String LOGTAG = "NanoHTTPD-Cordova";
    
	public WebServer(InetSocketAddress localAddr, AndroidFile wwwroot, Map customPaths ) throws IOException {
		super(localAddr, wwwroot);
        addCustomPaths(customPaths);
	}

	public WebServer(int port, AndroidFile wwwroot, Map customPaths ) throws IOException {
		super(port, wwwroot);
        addCustomPaths(customPaths);
	}

    private void addCustomPaths(Map customPaths) {
        this.customPaths = customPaths;
        customURIs = new String[customPaths.keySet().size()];
        int i = 0;
        Iterator keys = customPaths.keySet().iterator();
        while (keys.hasNext()) {
            String path = (String) keys.next();
            customURIs[i] = path;
            i++;
        }
        Arrays.sort(customURIs, new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                return t1.length() - s.length();
            }
        });
        for (i = 0; i < customURIs.length; i++) {
            Log.i( LOGTAG, "Custom Path: " + customURIs[i]);
        }
    }
    
    public Response serve( String uri, String method, Properties header, Properties parms, Properties files )
    {
        if (uri == null || method == null) {
            return null;
        }
        Log.i( LOGTAG, method + " '" + uri + "' " );
        for (int i = 0; i < customURIs.length; i++) {
            String testURI = customURIs[i];
            if (uri.startsWith(testURI)) {
                Log.i( LOGTAG, method + " '" + uri + "' " );
                String newURI = uri.substring(testURI.length());
                Object customPath = customPaths.get(testURI);
                if (customPath instanceof String) {
                    URL url = null;
                    HttpURLConnection connection = null;
                    InputStream in = null;

                    // Open the HTTP connection
                    try {
                        url = new URL(((String) customPath) + newURI);
                        connection = (HttpURLConnection) url.openConnection();
                        connection.connect();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        return null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        in = new InputStreamWithOverloadedClose(connection.getInputStream(), connection);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String datatype = connection.getContentType(); //NanoHTTPD.MIME_DEFAULT_BINARY
                    Response response = new NanoHTTPD.Response(NanoHTTPD.HTTP_OK, datatype, in);
                    if (connection.getContentEncoding() != null)
                        response.addHeader("Content-Encoding", connection.getContentEncoding());
                    if (connection.getContentLength() != -1)
                        response.addHeader("Content-Length", "" + connection.getContentLength());
                    if (connection.getHeaderField("Date") != null)
                        response.addHeader("Date", connection.getHeaderField("Date"));
                    if (connection.getHeaderField("Last-Modified") != null)
                        response.addHeader("Last-Modified", connection.getHeaderField("Last-Modified"));
                    if (connection.getHeaderField("Cache-Control") != null)
                        response.addHeader("Cache-Control", connection.getHeaderField("Cache-Control"));
                    return response;
                } else {
                    return serveFile( newURI, header, (AndroidFile) customPath, true );
                }
            }
        }
        return super.serve( uri, method, header, parms, files );
    }
    
    public class InputStreamWithOverloadedClose extends InputStream {
        protected InputStream is;
        protected HttpURLConnection connection; 
        
        public InputStreamWithOverloadedClose(InputStream is, HttpURLConnection connection) {
            super();
            this.is = is;
            this.connection = connection;
        }

        @Override
        public int available() throws IOException {
            return is.available();
        }

        @Override
        public void close() throws IOException {
            is.close();
            connection.disconnect();
        }

        @Override
        public void mark(int readlimit) {
            is.mark(readlimit);
        }

        @Override
        public boolean markSupported() {
            return is.markSupported();
        }

        @Override
        public int read() throws IOException {
            return is.read();
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            return is.read(buffer);
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            return is.read(buffer, offset, length);
        }

        @Override
        public synchronized void reset() throws IOException {
            is.reset();
        }

        @Override
        public long skip(long byteCount) throws IOException {
            return is.skip(byteCount);
        }
    }
}
