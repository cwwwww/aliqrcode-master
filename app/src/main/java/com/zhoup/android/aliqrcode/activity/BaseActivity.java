package com.zhoup.android.aliqrcode.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.zhoup.android.aliqrcode.application.MyApplication;

import butterknife.ButterKnife;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewId());
        ButterKnife.bind(this);
        initViews(savedInstanceState);
    }
    protected abstract void initViews(Bundle savedInstanceState);

    public abstract int getContentViewId();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyApplication.getRefWatcher(this).watch(this);
    }
}
