package com.example.gps_tracking_transtu;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DobackgroundTask extends AsyncTask<Void,Void,String> {
    private double lat;
    private double lang;
    private double alt;
    private double bearing;
    private float speed;
    private float acc;
    private String time;
    private String addr;
    private String versionandroid;
    private String baseURL;
    private String matricule;
    private TaskListener listener;

    public DobackgroundTask(String vecid,double lat, double lang, double alt, float speed,double bearing, float acc, String addr,String time, String versionandroid,String baseURL,TaskListener listener) {
        this.lat = lat;
        this.lang = lang;
        this.alt = alt;
        this.speed = speed;
        this.acc = acc;
        this.addr = addr;
        this.matricule = vecid;
        this.versionandroid = versionandroid;
        this.baseURL = baseURL;
        this.listener = listener;
        this.time =time;
        this.bearing = bearing;

    }
    @Override
    protected String doInBackground(Void... voids) {
        return null;
    }
    @Override
    protected void onPostExecute(String result) {
        senddata();
    }
    private void senddata(){
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String data = "{" +
                "\"Matricule\":\"" + matricule + "\"," +
                "\"lat\":" + lat + "," +
                "\"lang\":" + lang + "," +
                "\"alt\":" + alt + "," +
                "\"speed\":" + speed + "," +
                "\"bearing\":" + bearing + "," +
                "\"acc\":" + acc + "," +
                "\"addr\":\"" + addr + "\"," +
                "\"runningTime\":\"" + time + "\"," +
                "\"versionandroid\":\"" + versionandroid + "\"" +
                "}";

        RequestBody requestBody = RequestBody.create(data, JSON);

        String URL = baseURL;
        Log.i("serveraddress backend: ", data);
        Log.i("serveraddress uri: ", baseURL);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Request request = new Request.Builder()
                        .url(URL)
                        .post(requestBody)
                        .build();

                try {
                    Response response = client.newCall(request).execute();
                    String responseBody = response.body().string();
                    if (responseBody.equals("Location data received successfully")) {
                        listener.onSuccess("sent with success");
                    } else if (responseBody.equals("error")) {
                        listener.onError("error sending data to server");
                    }
                 else{
                        listener.onError("failed to send data to server");
                }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("OkHttp Error", "Request failed");
                    listener.onError("Request failed");
                }
            }
        }).start();

    }





}
