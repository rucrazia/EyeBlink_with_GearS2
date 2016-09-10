package com.hyechon.etrackermv;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SettingActivity extends AppCompatActivity {

    FaceTrackerActivity FTA = new FaceTrackerActivity();
    TextView counting;
    private boolean Noti_flag = false;
    private boolean Devi_flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        counting = (TextView) findViewById(R.id.counting);
        counting.setText(String.valueOf(FTA.count_put_get_FTAtoSA));

        final Button btn_device = (Button) findViewById(R.id.button_device);
        final Button btn_Noti = (Button) findViewById(R.id.Notification_setting);
        final Button btn_setting = (Button) findViewById(R.id.Counting_reset);
        final Button btn_connect = (Button) findViewById(R.id.setting_connect);
        Button btn_close = (Button) findViewById(R.id.close_activity);

        if(FTA.device_flag == false){
            btn_device.setText("PHONE");
            Devi_flag = true;
            btn_Noti.setEnabled(true);
            btn_setting.setEnabled(true);
        }
        else{
            btn_device.setText("WATCH");
            Devi_flag = false;
            btn_Noti.setEnabled(false);
            btn_setting.setEnabled(false);
        }

        btn_device.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(Devi_flag == false){
                    btn_device.setText("PHONE");
                    FTA.device_flag = false;
                    Devi_flag = true;
                    btn_Noti.setEnabled(true);
                    btn_setting.setEnabled(true);
                }
                else{
                    btn_device.setText("WATCH");
                    FTA.device_flag = true;
                    Devi_flag = false;
                    btn_Noti.setEnabled(false);
                    btn_setting.setEnabled(false);
                }

                Toast.makeText(SettingActivity.this, "Device_change", Toast.LENGTH_SHORT).show();

            }
        });


        if(Noti_flag == false){
            btn_Noti.setText("Top");
            //FTA.Noti_flag_main = 1;
            Noti_flag = true;
        }
        else{
            btn_Noti.setText("Center");
            //FTA.Noti_flag_main = 0;
            Noti_flag = false;
        }

        btn_Noti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(Noti_flag == false){
                    btn_Noti.setText("Top");
                    //FTA.Noti_flag_main = 1;
                    Noti_flag = true;
                }
                else{
                    btn_Noti.setText("Center");
                    //FTA.Noti_flag_main = 0;
                    Noti_flag = false;
                }
            }
        });

        btn_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(SettingActivity.this, "Reset counting", Toast.LENGTH_SHORT).show();

                counting = (TextView)findViewById(R.id.counting);
                FTA.count_eye_blink = 0;
                counting.setText(String.valueOf(FTA.count_eye_blink));
            }
        });

        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FTA.toast_smartphone();
            }
        });


        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(SettingActivity.this, "Activity close", Toast.LENGTH_SHORT).show();

                finish();
            }
        });

    }
}
