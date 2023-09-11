package com.example.gps_tracking_transtu;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ConfigActivity extends AppCompatActivity {
    EditText vecID , updtdist,updttime,url,portn;
    String veculeID,linkurl,port;
    Float distance;
    public static final String sharedprefrences = "settings";
    SharedPreferences sp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        vecID  = findViewById(R.id.etvid);
        updtdist = findViewById(R.id.etUd);
        updttime = findViewById(R.id.etUt);
        url = findViewById(R.id.url);
        portn = findViewById(R.id.port);
        Button btncancle =findViewById(R.id.btncncl);
        Button btnvalid = findViewById(R.id.btnsave);
        sp = getSharedPreferences(sharedprefrences, Context.MODE_PRIVATE);

        vecID.setText(sp.getString("Vid", ""));
        updtdist.setText(String.valueOf(sp.getFloat("distance", 0.0f)));
        updttime.setText(String.valueOf(sp.getLong("time", 0L)));
        url.setText(sp.getString("url", "http://localhost"));
        portn.setText(sp.getString("port", ""));

        btncancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });
        btnvalid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                veculeID = vecID.getText().toString();
                linkurl = url.getText().toString();
                port = portn.getText().toString();
                distance = Float.valueOf( updtdist.getText().toString());
                if (TextUtils.isEmpty(String.valueOf(distance))){
                    return;
                }
                if (TextUtils.isEmpty(vecID.getText().toString())){
                    vecID.setError("Vehicule id required");
                    return;

                }

                SharedPreferences.Editor editor = sp.edit();
                editor.putString("Vid",veculeID);
                editor.putString("url",linkurl);
                editor.putString("port",port);
                editor.putFloat("distance",distance);
                float floatValue = Float.parseFloat(updttime.getText().toString()); // Parse the Float value
                long longValue = (long) floatValue; // Convert the Float to a Long
                editor.putLong("time", longValue);
                editor.commit();
                Toast.makeText(ConfigActivity.this,"Configuration saved",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ConfigActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }
    private static void removedata(Context context){
        SharedPreferences pref = context.getSharedPreferences(sharedprefrences,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor  = pref.edit();
        editor.clear();
        editor.apply();
    }
    private void cancel(){
        this.finish();
    }

}