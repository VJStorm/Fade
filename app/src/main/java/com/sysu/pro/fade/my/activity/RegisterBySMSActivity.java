package com.sysu.pro.fade.my.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sysu.pro.fade.R;
import com.sysu.pro.fade.tool.UserTool;
import com.sysu.pro.fade.Const;

import java.util.Map;

/*
验证手机号码界面
 */
public class RegisterBySMSActivity extends AppCompatActivity {

    private Button btnSubmitTel;
    private EditText edTelphone;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == 2){
                Map<String,Object>map = (Map<String, Object>) msg.obj;
                    Integer ans = (Integer) map.get(Const.ANS);
                    if(ans == 0){
                        Toast.makeText(RegisterBySMSActivity.this,"该手机号没有注册",Toast.LENGTH_SHORT).show();
                        UserTool.sendIdentifyCode(handler,edTelphone.getText().toString());

                    }else{
                        Toast.makeText(RegisterBySMSActivity.this,"该手机号已经注册",Toast.LENGTH_SHORT).show();
                    }
            }

            if(msg.what == 1){
                String ans_str = (String) msg.obj;
                //Toast.makeText(RegisterBySMSActivity.this,ans_str,Toast.LENGTH_SHORT).show();
                if(ans_str.equals("{}")){
                    Intent intent = new Intent(RegisterBySMSActivity.this,CheckTelActivity.class);
                    intent.putExtra("mobilePhoneNumber",edTelphone.getText().toString());
                    startActivity(intent);
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_by_sms);
        btnSubmitTel = (Button) findViewById(R.id.btnSubmitTel);
        edTelphone = (EditText) findViewById(R.id.edTelphone);

        btnSubmitTel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //先校验该手机号是否已经被注册
                UserTool.checkTel(handler,edTelphone.getText().toString());
            }
        });

    }



}
