package com.zhoup.android.aliqrcode.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zhoup.android.aliqrcode.R;
import com.zhoup.android.aliqrcode.module.model.bean.AlipayAmountEvent;
import com.zhoup.android.aliqrcode.module.presenter.MainPresenter;
import com.zhoup.android.aliqrcode.module.view.IMainView;
import com.zhoup.android.aliqrcode.utils.DialogUtils;
import com.zhoup.android.aliqrcode.utils.SnackbarUtil;

import org.greenrobot.eventbus.EventBus;

import java.math.BigDecimal;

import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends BaseActivity implements IMainView {
    @BindView(R.id.tv_info)
    TextView mTextView;
    @BindView(R.id.btn_service)
    Button mServiceButton;
    @BindView(R.id.edit_amount_begin)
    TextInputEditText mAmountBeginEditText;
//    @BindView(R.id.edit_reason)
//    TextInputEditText mReasonEditText;
    @BindView(R.id.edit_amount_end)
    TextInputEditText mAmountEndEditText;
    @BindView(R.id.edit_amount_interval)
    TextInputEditText mAmountIntervalEditText;
    @BindView(R.id.edit_post_url)
    TextInputEditText mPostUrl;
    @BindView(R.id.edit_id)
    TextInputEditText mId;
    @BindView(R.id.btn_submit)
    Button mSubmitButton;
    private MainPresenter mMainPresenter;
    int id = 1;

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mMainPresenter = new MainPresenter();
        mMainPresenter.attachView(this);
        // check runtime permissions
        mMainPresenter.checkPermissions(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE);

    }

    @Override
    public int getContentViewId() {
        return R.layout.activity_main;
    }

    @OnClick({R.id.btn_service, R.id.btn_submit})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_service:
                openAccessibilityServiceSettings();
                break;
            case R.id.btn_submit:
                //这边填写传送的所有数据到支付宝客户端
                AlipayAmountEvent alipayAmountEvent = new AlipayAmountEvent();
                //上传地址不能为空
                String postUrl = mPostUrl.getText().toString().trim();
                if (postUrl.isEmpty() || "".equals(postUrl)) {
                    Toast.makeText(this, "请输入上传地址！", Toast.LENGTH_SHORT).show();
                    return;
                }
                //自定义id
                if (!mId.getText().toString().trim().isEmpty() && !"".equals(mId.getText().toString().trim()) && 0 != Integer.valueOf(mId.getText().toString().trim())) {
                    id = Integer.valueOf(mId.getText().toString().trim());
                }

                //收款理由
//                String reason = mReasonEditText.getText().toString().trim();
//                if (reason.isEmpty() || "".equals(reason)) {
//                    alipayAmountEvent.setReason("收钱");
//                } else {
//                    alipayAmountEvent.setReason(reason);
//                }
                //金额填写处理
                if (mAmountBeginEditText.getText().toString().isEmpty() || mAmountEndEditText.getText().toString().isEmpty()) {
                    Toast.makeText(this, "请填入正确的信息!", Toast.LENGTH_SHORT).show();
                    return;
                }
                BigDecimal amountBegin = new BigDecimal(mAmountBeginEditText.getText().toString().trim());
                BigDecimal amountEnd = new BigDecimal(mAmountEndEditText.getText().toString().trim());
                long i = 1;
                if (!mAmountIntervalEditText.getText().toString().isEmpty()) {
                    BigDecimal amountInterval = new BigDecimal(mAmountIntervalEditText.getText().toString().trim());
                    i = amountInterval.setScale(0, BigDecimal.ROUND_UP).longValue();
                }
                long b = amountBegin.setScale(0, BigDecimal.ROUND_UP).longValue();
                long e = amountEnd.setScale(0, BigDecimal.ROUND_UP).longValue();

                if (b < 0 || e < 0 || e < b) {
                    Toast.makeText(this, "请填入正确的信息!", Toast.LENGTH_SHORT).show();
                    return;
                }
//                Log.e("cww", "postUrl：" + postUrl + "；reason：" + reason);
                long count = (long) Math.ceil((e - b) / i);
                alipayAmountEvent.setPostUrl(postUrl);
                alipayAmountEvent.setId(id);
                alipayAmountEvent.setCount(count);
                alipayAmountEvent.setAmount(amountBegin);
                alipayAmountEvent.setInterval(i);
                EventBus.getDefault().postSticky(alipayAmountEvent);
                break;
        }
    }

    // 显示打开辅助服务的对话框
    @Override
    public void showOpenServiceDialog() {
        DialogUtils.showMessage(this, R.string.use_tip, R.string.open_additional_function_service,
                true, R.string.open_function, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openAccessibilityServiceSettings();
                    }
                });
    }

    @Override
    public void checkService() {
        // check accessibilityservice
        mMainPresenter.checkService();
    }

    @Override
    public void showErrorMessage(String errorMsg) {
        SnackbarUtil.showSnackbar(mTextView, errorMsg, Snackbar.LENGTH_SHORT);
    }

    // 打开辅助服务的设置
    private void openAccessibilityServiceSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
