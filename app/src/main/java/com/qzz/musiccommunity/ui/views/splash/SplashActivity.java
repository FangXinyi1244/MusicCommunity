package com.qzz.musiccommunity.ui.views.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.ui.views.home.HomeActivity;

public class SplashActivity extends AppCompatActivity implements TermsDialogFragment.TermsDialogListener {

    private SplashViewModel viewModel;
    private ImageView logoImage;
    private TextView appNameTextView;
    private ProgressBar progressBar;
    private TextView loadingStatusTextView;
    private TextView retryTextView;
    private TextView retryTermsTextView;
    private LinearLayout verticalFlowContainer;

    // 用于判断是否首次启动的键值
    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_FIRST_START = "isFirstStart";

    @Override
    public void onTermsAccepted() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish(); // 结束当前SplashActivity
    }

    @Override
    public void onTermsDeclined() {
        // 用户拒绝条款，可以选择关闭应用或返回上一步
        Toast.makeText(this, "You must accept the terms to continue.", Toast.LENGTH_SHORT).show();
        // 这里可以选择关闭应用或返回到上一个界面
        finish(); // 结束当前SplashActivity
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_activity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initView();

        // 判断是否首次启动应用，如果是则显示条款弹窗
        if (isFirstStart()) {
            showTermsDialog();
            // 记录已非首次启动（实际应用中才需要，这里按要求默认总是首次启动）
            // setFirstStartCompleted();
        }
    }

    /**
     * 判断是否首次启动应用
     * 根据需求，此处默认返回true，即默认总是认为是首次启动
     */
    private boolean isFirstStart() {
        // 按照需求，直接返回true，表示始终是首次启动
        return true;

        /*
        // 以下是实际判断首次启动的代码，如果需要真实判断，可以启用
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return preferences.getBoolean(KEY_FIRST_START, true);
        */
    }

    /**
     * 标记首次启动已完成
     * 实际应用中使用，当前按需求未启用
     */
    private void setFirstStartCompleted() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_FIRST_START, false);
        editor.apply();
    }

    private void initView() {
        // 初始化视图组件
        verticalFlowContainer = findViewById(R.id.vertical_flow_container);

        // 设置Frame 4点击事件（可选，如果需要手动触发弹窗）
        if (verticalFlowContainer != null) {
            verticalFlowContainer.setOnClickListener(v -> showTermsDialog());
        }
    }

    /**
     * 显示条款对话框
     */
    private void showTermsDialog() {
        TermsDialogFragment dialog = TermsDialogFragment.newInstance();
        dialog.show(getSupportFragmentManager(), "TermsDialog");
    }
}
