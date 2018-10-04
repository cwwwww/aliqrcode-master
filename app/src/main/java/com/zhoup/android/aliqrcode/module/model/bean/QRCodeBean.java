package com.zhoup.android.aliqrcode.module.model.bean;

import java.io.Serializable;

/**
 * Created by zhoup on 2017/6/23
 * 二维码信息
 */

public class QRCodeBean implements Serializable {

    //生成的金额
    private long amount;
    //生成的二维码理由
    private String associatedCode;

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getAssociatedCode() {
        return associatedCode;
    }

    public void setAssociatedCode(String associatedCode) {
        this.associatedCode = associatedCode;
    }
}
