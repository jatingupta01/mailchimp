package com.ttnd.mailchimp.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * Created by Jatin on 3/3/2016.
 */
public class HttpUtil {

    public static String getHttpResponse(String url, String username, String password, String method, JSONObject params) throws JSONException{
        if(url != null && url.trim().length()>0){
            try{
                String authString = username + ":" + password;
                byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
                String s = new String(authEncBytes);
                URL _url = new URL(url);
                HttpURLConnection urlConnection = (HttpURLConnection)_url.openConnection();
                urlConnection.setRequestProperty("Authorization", "Basic " + s);
                if(method != null && (method.trim().equalsIgnoreCase("POST") || method.trim().equalsIgnoreCase("PUT"))){
                	urlConnection.setRequestMethod(method);
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestProperty("Content-type", "application/json");
                    urlConnection.setRequestProperty("Accept", "*/*");
                    OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                    if(params != null){
                        writer.write(params.toString());
                        writer.flush();
                        writer.close();
                    }
                }
                InputStream is = urlConnection.getInputStream();
                return getStringFromInputStream(is);
            }
            catch(MalformedURLException e){
                e.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getHashString(String message, String algorithm)
    {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));

            return convertByteArrayToHexString(hashedBytes);
        }catch(NoSuchAlgorithmException ex){
            ex.printStackTrace();
        }catch(UnsupportedEncodingException ex){
            ex.printStackTrace();
        }
        return null;
    }

    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < arrayBytes.length; i++) {
            stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return stringBuffer.toString();
    }

    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }



}
