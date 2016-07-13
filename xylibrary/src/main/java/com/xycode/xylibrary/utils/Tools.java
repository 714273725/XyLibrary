package com.xycode.xylibrary.utils;

import android.app.ActivityManager;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.Html;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Utils
 * Created by Administrator on 2014/7/27.
 */
public class Tools {

    private static File currentPhotoFile;

    private static Tools instance = null;

    private static int ScreenWidth = 0;
    private static Point screenSize = null;

    private static AtomicInteger atomicCounter = new AtomicInteger(0);

    public static Tools getInstance() {
        if (instance == null) {
            instance = new Tools();
        }
        return instance;
    }

    public static int randomInt(int min, int max) {
        return (int) (Math.random() * max) + min;
    }


    //判断手机格式是否正确
    public static boolean isMobileNO(String mobiles) {

        Pattern p = Pattern.compile("^((13[0-9])|(15[^4,\\D])|(18[0,5-9]))\\d{8}$");

        Matcher m = p.matcher(mobiles);

        return m.matches();

    }

    //判断email格式是否正确

    public static boolean isEmail(String email) {

        String str = "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$";

        Pattern p = Pattern.compile(str);

        Matcher m = p.matcher(email);

        return m.matches();

    }

    //判断是否全是数字

    public static boolean isNumeric(String str) {

        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);

        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    //截取数字
    public static List<String> getNumbers(String content) {
        List<String> list = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\d+");
//        Pattern pattern = Pattern.compile("[0]");
        Matcher matcher = pattern.matcher(content);
//        if (matcher.find()) {
        /*if (matcher.matches()) {
            L.e("matcher.find count"+"("+ matcher.groupCount()+")");
        for (int i = 0; i < matcher.groupCount(); i++) {
            L.e("matcher.find"+"("+ matcher.group(i)+")");
        }

        }*/
        while (matcher.find()) {
            if (matcher.group().length() >= 4) {
                list.add(matcher.group());
            }
//            L.e("matcher.find" + "(" + matcher.group() + ")");
        }
        return list;
    }

    /**
     * 实现文本复制功能
     * add by wangqianzhou
     *
     * @param content
     */
    public static void copyToClipboard(String content, Context context) {
// 得到剪贴板管理器
        ClipboardManager cmb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cmb.setText(content);
    }

    public static long getLongID() {
        if (atomicCounter.get() > 9999) {
            atomicCounter.set(1);
        }
        return System.currentTimeMillis() * 10000 + atomicCounter.incrementAndGet();
    }

    public static CharSequence getHtmlDrawableText(final Context context, int drawableId, String text, final int drawableHeightDP) {
        final String html = "<img src='" + drawableId + "'/>&nbsp;" + text;
        CharSequence charSequence = Html.fromHtml(html, new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String source) {
                Drawable drawable = context.getResources().getDrawable(Integer.parseInt(source));
                int dh = Tools.dp2px(context, (float) drawableHeightDP);
                drawable.setBounds(0, 0, dh * drawable.getIntrinsicWidth() / drawable.getIntrinsicHeight(), dh);
                return drawable;
            }
        }, null);
        return charSequence;
    }

    public static CharSequence getHtmlDrawableText(final Context context, int drawableId, String text, int textColerRes, final int drawableHeightDP) {

        StringBuffer tc = new StringBuffer("#" + Integer.toHexString(textColerRes));
        if (tc.length() == 9) tc.delete(1, 2);
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("<font color='");
        stringBuffer.append(tc);
        stringBuffer.append("'>");
        stringBuffer.append(text);
        stringBuffer.append("</font>");
        final String html = "<img src='" + drawableId + "'/>" + stringBuffer.toString(); //&nbsp;
        CharSequence charSequence = Html.fromHtml(html, new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String source) {
                Drawable drawable = context.getResources().getDrawable(Integer.parseInt(source));
                int dh = Tools.dp2px(context, (float) drawableHeightDP);
                drawable.setBounds(0, 0, dh * drawable.getIntrinsicWidth() / drawable.getIntrinsicHeight(), dh);
                return drawable;
            }
        }, null);
        return charSequence;
    }

    public static String returnString(StringBuffer stringBuffer) {
        if (stringBuffer == null) {
            stringBuffer = new StringBuffer();
        }
        return stringBuffer.toString();
    }

    public static class MD5 {
        public static String getMD5(String value) throws NoSuchAlgorithmException {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(value.getBytes());
            byte[] m = md5.digest();  // 加密
            return getString(m);
        }

        private static String getString(byte[] b) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < b.length; i++) {
                sb.append(b[i]);
            }
            return sb.toString();
        }

        /**
         * 将字符串转成MD5值
         *
         * @param string
         * @return string
         */
        public static String stringToMD5(String string) {
            byte[] hash;
            try {
                hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                if ((b & 0xFF) < 0x10)
                    hex.append("0");
                hex.append(Integer.toHexString(b & 0xFF));
            }
            return hex.toString();
        }
    }

    /**
     * 判断网络状态
     *
     * @return true 有网络，false 没有网络
     */
    public static boolean isAvailableNetWork(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo[] info = connectivityManager.getAllNetworkInfo();
            if (info != null) {
                for (NetworkInfo network : info) {
                    if (network.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 程序是否在前台运行
     *
     * @return
     */
    public static boolean isAppOnForeground(Context context) {
        // Returns a list of application processes that are running on the device
        List<ActivityManager.RunningAppProcessInfo> appProcesses = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getRunningAppProcesses();
        if (appProcesses == null) return false;

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            // The name of the process that this object is associated with.

            if (appProcess.processName.equals(context.getPackageName()) && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

    public static String getTopActivityClassName(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);
        for (ActivityManager.RunningTaskInfo info : list) {
            if (info.topActivity.getPackageName().equals(context.getPackageName()) && info.baseActivity.getPackageName().equals(context.getPackageName())) {
                return info.topActivity.getClassName();
//                L.e("baseActivity.getPackageName()"+"("+info.baseActivity.getPackageName()+")"+"baseActivity.getClassName()"+"("+info.baseActivity.getClassName()+")");
//                break;
            }
        }
//        L.e("isAppRunning"+"("+isAppRunning+")");
        return "";
    }

    public static List<String> getActivityClassNames(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);
        List<String> stringList = new ArrayList<>();
        for (ActivityManager.RunningTaskInfo info : list) {
            if (info.baseActivity.getPackageName().equals(context.getPackageName())) {
                stringList.add(info.baseActivity.getClassName());
            }
        }
        return stringList;
    }



    public static File checkFile(String path, String aFileNameWithAnyPath) {
        final String fileName = new File(aFileNameWithAnyPath).getName();
        File file = new File(path, fileName);
        return file;
    }

    public static void copyFile(String oldPath, String newPath) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldFile = new File(oldPath);
            if (oldFile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1024];
                int length;
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; //字节数 文件大小
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
            }
        } catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();
        }
    }

    public static File saveFileFromInputStream(InputStream inputStream, File file) {

        File dir = new File(file.getParent());
        if (!dir.exists())
            dir.mkdirs();// 创建照片的存储目录
//        if (file.exists()) {
//            return file;
//        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            // 文件写入
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            // 关闭文件流
            fos.flush();
            fos.close();
            inputStream.close();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getRealFilePath(final Context context, final Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dp(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static void scrollToView(final ScrollView scrollView, final View view) {
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                int offset = location[1] - scrollView.getMeasuredHeight();
                scrollView.smoothScrollTo(0, offset);
            }
        });
    }

    public static int[] getViewLocation(View v) {
        int[] loc = new int[4];
        int[] location = new int[2];
        v.getLocationOnScreen(location);
        loc[0] = location[0];
        loc[1] = location[1];
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        v.measure(w, h);

        loc[2] = v.getMeasuredWidth();
        loc[3] = v.getMeasuredHeight();

        return loc;
    }

    public static Point getScreenSize(Context context) {
        if (screenSize == null) {
            screenSize = new Point();
            ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(screenSize);
        }
        return screenSize;
    }

    public static class Cal {
        public static int getPosInArray(int[] arr, int value) {
            for (int i : arr) {
                if (i == value) {
                    return i;
                }
            }
            return -1;
        }
    }



}