package com.qcloud.cmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

public class CMQHttp {
	private  int timeout ;
	private  boolean isKeepAlive;
    private URLConnection connection;
    private String url ;
	private static final Logger log = LoggerFactory.getLogger(CMQHttp.class);

    public CMQHttp()
    {
        this.connection = null;
        this.url = "";
        this.timeout = 10000;
        this.isKeepAlive = true;
    }
    /*
     * if we find the url is different with this.url we should new another connection 
     * 
     */
    private void newHttpConnection(String url) throws Exception
    {
        if(this.url != url)
        {
            URL realUrl = new URL(url);
            if(url.toLowerCase().startsWith("https")){
                HttpsURLConnection httpsConn = (HttpsURLConnection)realUrl.openConnection();
                httpsConn.setHostnameVerifier(new HostnameVerifier(){
                    @Override
                    public boolean verify(String hostname, SSLSession session){
                        return true;
                    }
                });
                connection = httpsConn;
            }
            else{
                connection = realUrl.openConnection();
            }
           	this.connection.setRequestProperty("Accept", "*/*");
			if(this.isKeepAlive) {
                this.connection.setRequestProperty("Connection", "Keep-Alive");
            }
			this.connection.setRequestProperty("User-Agent",
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");

            this.url = url ;
        }
    }
    public  String request(String method, String url, String req,
			int userTimeout) throws Exception {
		String result = "";
		BufferedReader in = null;
		try{
//            if (!this.url.equals(url))
			this.newHttpConnection(url);

            this.connection.setConnectTimeout(timeout+userTimeout);
            this.connection.setReadTimeout(timeout+userTimeout);
	
			if ("POST".equals(method)) {
				((HttpURLConnection)this.connection).setRequestMethod("POST");
	
				this.connection.setDoOutput(true);
				this.connection.setDoInput(true);
				DataOutputStream out  = null;
				try{
					out = new DataOutputStream(this.connection.getOutputStream());
				out.writeBytes(req);
				out.flush();
				}catch(Exception e){
					log.error("url:{} req {}",url,req);
				}finally{
					if(out !=null){
				out.close();
			}
				}
			}

			this.connection.connect();
			int status = ((HttpURLConnection)this.connection).getResponseCode();
			if(status != 200) {
                throw new CMQServerException(status);
            }
	
			in = new BufferedReader(new InputStreamReader(connection.getInputStream(),"utf-8"));
	
			String line;
			while ((line = in.readLine()) != null) {
				result += line;
			}
		}catch(Exception e){
			throw e;
		}finally{
			try {
				if (in != null) {
                    in.close();
                }
			} catch (Exception e2) {
				throw e2;
			}
		}
		
		return result;
	}
}
