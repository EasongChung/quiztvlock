package com.quiztv.lock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * App 进入密码门禁页。
 * <p>打开桌面图标先要求输入「进入密码」（独立于 WebUI 账号和答题 PIN），
 * 防止孩子进入设置页。默认 1234，可在 WebUI「密码管理」修改。
 */
public class AppLockActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        final PrefManager prefs = new PrefManager(this);
        final EditText et = findViewById(R.id.appPwd);

        findViewById(R.id.appPwdOk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String in = et.getText().toString().trim();
                if (in.isEmpty()) {
                    Toast.makeText(AppLockActivity.this, "请输入进入密码", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (in.equals(prefs.getAppPwd())) {
                    startActivity(new Intent(AppLockActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(AppLockActivity.this, "密码错误", Toast.LENGTH_SHORT).show();
                    et.setText("");
                }
            }
        });
    }
}
