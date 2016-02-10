package net.tuxed.vpnconfigimporter;

import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downloader {

    public String downloadFile(String vpnUrl, String userName, String userPass, String configName) {
        try {
            URL url = new URL(vpnUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            String authToken = userName + ":" + userPass;
            String encodedAuthToken = Base64.encodeToString(authToken.getBytes(), Base64.DEFAULT);
            Log.i("Downloader", encodedAuthToken);
            urlConnection.setRequestProperty("Authorization", "Basic " + encodedAuthToken);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try {
                String body = "name=" + configName;
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(body.length());
                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                out.write(body.getBytes());
                out.flush();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader r = new BufferedReader(new InputStreamReader(in));

                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line + "\n");
                }

                return total.toString();
            } finally {
                urlConnection.disconnect();
            }


        } catch (Exception e) {
            Log.e("Downloader", e.getMessage());
            return null;
        }
    }
}