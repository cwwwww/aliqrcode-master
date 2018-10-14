package com.zhoup.android.aliqrcode.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.zhoup.android.aliqrcode.R;
import com.zhoup.android.aliqrcode.activity.MainActivity;
import com.zhoup.android.aliqrcode.application.MyApplication;
import com.zhoup.android.aliqrcode.consts.AppConst;
import com.zhoup.android.aliqrcode.module.model.bean.AlipayAmountEvent;
import com.zhoup.android.aliqrcode.module.model.bean.ExpandAccessibilityNodeInfo;
import com.zhoup.android.aliqrcode.module.model.bean.QRCodeBean;
import com.zhoup.android.aliqrcode.task.AlipayQRTask;
import com.zhoup.android.aliqrcode.utils.AccessibilityServiceHelper;
import com.zhoup.android.aliqrcode.utils.ImageCut;
import com.zhoup.android.aliqrcode.utils.LogUtil;
import com.zhoup.android.aliqrcode.utils.NotificationUtils;
import com.zhoup.android.aliqrcode.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhoup on 2017/6/22.
 */

public class AliQRService extends AccessibilityService {

    private static AliQRService mAliQRService;
    // 执行一系列的任务
    private AlipayQRTask mTask;
    // 是否应该结束
    private boolean quit;
    private QRCodeBean mCodeBean;
    // 是否生成二维码, 防止在生成二维码的同时多次修改数据
    private boolean generate;
    private boolean skip;
    //数据类型
    private AtomicInteger notificationId = new AtomicInteger(1);
    private PowerManager.WakeLock mWakeLock;
    //图片生成数量
    private long count = 0;
    //图片总数
    private long total = 0;
    //金额间隔
    private long interval = 0;
    //mid
    private int mid = 0;
    //理由
    private long resaon;
    //起始金额
    private BigDecimal amount;
    //上传地址
    private String postUrl;
    //处理重复提交
    private Map<String, String> tagMap;

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        PowerManager pm = (PowerManager) MyApplication.getApplicationContext(this).getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Tag");
        mTask = new AlipayQRTask();
        // foreground service
        Notification notification = NotificationUtils.buildNotification(this, R.mipmap.ic_launcher,
                getString(R.string.app_name), getString(R.string.alipay_autoservice_running), false,
                PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));
        startForeground(notificationId.incrementAndGet(), notification);

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!quit) {
            AccessibilityNodeInfo nodeInfo = event.getSource();
            ExpandAccessibilityNodeInfo target = searchNodeInfo(nodeInfo);
            //支付宝首页更多（“+”）菜单
            if (target == null) {
                if (getRootInActiveWindow() != null) {
                    mTask.clickMoreMenu(getRootInActiveWindow());
                }
                return;
            }
            switch (target.getId()) {
//                case AppConst.OPEN_MENU_ID:
//                    mTask.clickTargetView(target);
//                    break;
                case AppConst.COLLECT_MONEY:
                    mTask.clickTargetView(target);
                    break;
                case AppConst.SET_MONEY_ID: //设置金额
                    mTask.clickTargetView(target);
                    break;
                case AppConst.ADD_GATHER_REASON_ID:
                    mTask.clickTargetView(target);
                    break;
                case AppConst.REASON_RELATIVELAYOUT_ID:
                    // 模拟输入收款理由
                    if (mCodeBean != null) {
                        //设置收款理由的值
                        resaon = System.currentTimeMillis();
                        mCodeBean.setAssociatedCode(String.valueOf(resaon));
                        mTask.inputQRCodeInfo(AliQRService.this, mCodeBean, target, getRootInActiveWindow(), interval * count);
                    }
                    break;
                case AppConst.SAVE_PICTURE: // 保存图片
                    if (skip) {
                        //点击保存图片按钮
                        mTask.clickTargetView(target);
//                        Log.e("cwww", "点击保存图片按钮：" + System.currentTimeMillis());
                        //在这里上传图片 首先要获得图片数据 其次上传
                        //上传图片到服务器
                        postImage();
//                        postImage2();
                        count = count + 1;
                        mTask.goGlobalBack(this);
                        skip = false;
                        generate = false;
                        if (count >= total + 1) {
                            quit = true;
                            mTask.goGlobalBack(this);
                            this.stopSelf();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void postImage2() {
//        Bitmap bitmap = takeScreenShot();
    }

    /**
     * 查找节点
     *
     * @param nodeInfo
     * @return
     */
    private ExpandAccessibilityNodeInfo searchNodeInfo(AccessibilityNodeInfo nodeInfo) {
        ExpandAccessibilityNodeInfo target;
        if (getRootInActiveWindow() != null) {
            //打开菜单 加号
            target = AccessibilityServiceHelper.findNodeInfosById(getRootInActiveWindow(), AppConst.OPEN_MENU_ID);
            if (target != null) {
                return target;
            }
        }
        if (!skip) {
            //设置金额
            target = AccessibilityServiceHelper.findNodeInfosById(getRootInActiveWindow(), AppConst.SET_MONEY_ID);
            if (target != null) {
                skip = true;
                return target;
            }
        }
        //添加收款理由
        target = AccessibilityServiceHelper.findNodeInfosById(nodeInfo, AppConst.ADD_GATHER_REASON_ID);
        if (target != null) {
            return target;
        }
        if (!generate) {
            //收款理由
            target = AccessibilityServiceHelper.findNodeInfosById(nodeInfo, AppConst.REASON_RELATIVELAYOUT_ID);
            if (target != null) {
                generate = true;
                return target;
            }
        }
        //保存图片
        target = AccessibilityServiceHelper.findNodeInfosById(nodeInfo, AppConst.SAVE_PICTURE);
        if (target != null) {
            //判断图片是否保存成功
//            Log.e("cwwww", "保存图片成功" + System.currentTimeMillis());
            generate = false;
            return target;
        }
        //收钱
        target = AccessibilityServiceHelper.findNodeInfosByText(nodeInfo, AppConst.COLLECT_MONEY, "收钱");
        if (target != null) {
            return target;
        }
        return null;
    }

    private void postImage() {
        //selection: 指定查询条件
        String selection = MediaStore.Images.Media.DATA + " like '%Camera%'";
        Cursor c = this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
                selection, null, null);

        //遍历相册
        while (c.moveToNext()) {
            //根据时间来判断是否提交同一图片
            long photoDate = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN));
            //照片路径
            String photoPath = c.getString(c.getColumnIndex(MediaStore.Images.Media.DATA));
            //照片标题
            String photoTitle = c.getString(c.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
            if (tagMap.size() == 0) {
                tagMap.put(String.valueOf(photoDate), photoPath);
                handleImg(photoPath, photoTitle, photoDate);
            } else {
                for (String str : tagMap.keySet()) {
                    String value = tagMap.get(str);
                    if (!value.equals(photoPath)) {
                        tagMap.put(String.valueOf(photoDate), photoPath);
                        handleImg(photoPath, photoTitle, photoDate);
                        break;
                    }
                }
            }
        }
    }

    private void handleImg(String photoPath, String photoTitle, final long photoDate) {
        if (photoPath != null && photoPath.length() > 0) {
            final File file = new File(photoPath);
            if (isNumericJPG(photoTitle)) {
                //开始上传后台
                //获得图片base64编码
                Bitmap bitmap = stringToBitmap(photoPath);
                if (bitmap != null) {
                    Bitmap ic = ImageCut.zoomBitmap(bitmap, 720, 1092);
//                        ImageCut.saveBitmap(ic, String.valueOf(System.currentTimeMillis()), getBaseContext());
                    long amountCount = amount.longValue() + count * interval;
                    String base64 = bitmaptoString(ic);
                    Log.e("cwww", "mid:" + mid + "；memo:" + resaon + "；amount:" + amountCount);
//                        Log.e("cwww", "postUrl：" + postUrl + "；mid:" + mid + "；memo:" + System.currentTimeMillis() + "；amount:" + amountCount);
                    //"http://api.hqgaotong.com/api/upload/partner/10000"
                    OkGo.<String>post(postUrl)
                            .tag(this)
                            .params("image", base64)
                            .params("mid", mid)
                            .params("memo", resaon)
                            .params("amount", String.valueOf(amountCount))
                            .execute(new AbsCallback<String>() {
                                @Override
                                public String convertResponse(okhttp3.Response response) throws Throwable {
                                    return response.body().string();
                                }

                                @Override
                                public void onSuccess(Response<String> response) {
                                    Log.e("cww", "返回的数据：" + response.body().toString());
                                    //删除图片
                                    deletePictures(getApplicationContext(), file);
                                    //更新map集合
                                    tagMap.remove(String.valueOf(photoDate));
                                }

                                @Override
                                public void onError(Response<String> response) {
                                    super.onError(response);
                                    Log.e("cww", "出错：" + response.body().toString());
                                    tagMap.remove(String.valueOf(photoDate));
                                }
                            });
                }
            } else {
                //删除掉名称不是数字的图片
                deletePictures(getApplicationContext(), file);
            }
        }
    }

    @Override
    public void onInterrupt() {
        LogUtil.i("中断...");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        return super.onUnbind(intent);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        LogUtil.i("无障碍服务连接成功");
        mWakeLock.acquire();
        quit = false;
        mAliQRService = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.i("AlipayService onDestroy");
        stopForeground(true);
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mAliQRService = null;
        mTask = null;
        MyApplication.getRefWatcher(this).watch(this);
//        EventBus.getDefault().unregister(this);
    }

    //打开支付宝app
    private void gotoAlipay(QRCodeBean mQRCodeBean) {
        //初始化标记集合
        tagMap = new HashMap<>();
        //重新设置quit和count的值
        quit = false;
        count = 0;
        this.mCodeBean = mQRCodeBean;
        String packageName = AppConst.ALIPAY_PACKAGE_NAME;
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            startActivity(intent);
        } else {
            ToastUtil.showToast(this.getApplicationContext(), getString(R.string.uninstall_alipay),
                    Toast.LENGTH_SHORT);
        }
    }

    /**
     * 判断服务是否已经启动
     *
     * @return
     */
    public static boolean isRunning() {
        if (mAliQRService == null) {
            return false;
        }
        AccessibilityManager accessibilityManager = (AccessibilityManager) mAliQRService.getSystemService(Context.ACCESSIBILITY_SERVICE);
        AccessibilityServiceInfo info = mAliQRService.getServiceInfo();
        if (info == null) {
            return false;
        }
        List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        Iterator<AccessibilityServiceInfo> iterator = list.iterator();

        boolean isConnect = false;
        while (iterator.hasNext()) {
            AccessibilityServiceInfo i = iterator.next();
            if (i.getId().equals(info.getId())) {
                isConnect = true;
                break;
            }
        }
        if (!isConnect) {
            return false;
        }
        return true;
    }


    // 接收支付宝账号  订阅者
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onAlipayAmountEvent(AlipayAmountEvent event) {
        if (event != null) {
            //数量
            total = event.getCount();
            interval = event.getInterval();
            mid = event.getId();
//            resaon = System.currentTimeMillis();
            amount = event.getAmount();
            postUrl = event.getPostUrl();

            BigDecimal alipayAmount = event.getAmount();
            QRCodeBean qrCodeBean = new QRCodeBean();
            qrCodeBean.setAmount(alipayAmount.longValue());
//            //理由
//            qrCodeBean.setAssociatedCode(String.valueOf(resaon));
            gotoAlipay(qrCodeBean);
        }
    }

    //判断字符串是否是数字
    public boolean isNumericJPG(String str) {
        Pattern pattern = Pattern.compile("[0-9]*.(png|jpg)$");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }


    //根据名字清除图片

    /**
     * @param context
     * @param file
     */
    public static void deletePictures(Context context, File file) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        context.sendBroadcast(intent);
        file.delete();
    }

    //将图片转化成base64编码
    public String bitmaptoString(Bitmap bitmap) {
        String string = null;
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bStream);
        byte[] bytes = bStream.toByteArray();
        string = Base64.encodeToString(bytes, Base64.DEFAULT);
        return string;
    }

    //将图片转化成bitmap格式
    public Bitmap stringToBitmap(String string) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(string);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            return bitmap;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 获取指定Activity的截屏，保存到png文件
    public static Bitmap takeScreenShot(Activity activity) {
        // View是你需要截图的View
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap b1 = view.getDrawingCache();

        // 获取状态栏高度
        Rect frame = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;
        System.out.println(statusBarHeight);

        // 获取屏幕长和高
        int width = activity.getWindowManager().getDefaultDisplay().getWidth();
        int height = activity.getWindowManager().getDefaultDisplay()
                .getHeight();
        // 去掉标题栏
        // Bitmap b = Bitmap.createBitmap(b1, 0, 25, 320, 455);
        Bitmap b = Bitmap.createBitmap(b1, 0, statusBarHeight, width, height
                - statusBarHeight);
        view.destroyDrawingCache();
        return b;
    }

//    // 程序入口 截取当前屏幕
//    public static void shootLoacleView(Activity a, String picpath) {
//        ScreenShot.savePic(ScreenShot.takeScreenShot(a), picpath);
//    }

    // 保存到sdcard
    public static void savePic(Bitmap b, String strFileName) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(strFileName);
            if (null != fos) {
                b.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.flush();
                fos.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
