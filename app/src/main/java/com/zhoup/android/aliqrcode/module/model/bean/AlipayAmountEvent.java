package com.zhoup.android.aliqrcode.module.model.bean;

import java.math.BigDecimal;

/**
 * Created by zhoup on 2017/6/25.
 */

public class AlipayAmountEvent {
    private int id;
    private String postUrl;
    private BigDecimal amount;
    private String reason;
    private long count;
    private long interval;


//    public AlipayAmountEvent(BigDecimal amount) {
//        this.amount = amount;
//    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }
}
