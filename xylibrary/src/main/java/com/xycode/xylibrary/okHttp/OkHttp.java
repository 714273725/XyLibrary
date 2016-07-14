package com.xycode.xylibrary.okHttp;

import android.support.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by XY on 2016/7/7.
 */
public class OkHttp {

    public static final MediaType MEDIA_TYPE_MARKDOWN = MediaType.parse("text/x-markdown; charset=utf-8");
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType MEDIA_TYPE_MULTI_DATA = MediaType.parse("multipart/form-data; charset=utf-8");

    public static final int RESULT_ERROR = 0;
    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_VERIFY_ERROR = -1;
    public static final int RESULT_OTHER = 2;

    public static final long READ_TIMEOUT = 30;
    public static final long CONNECT_TIMEOUT = 10;
    public static final long WRITE_TIMEOUT = 60;

    private static OkHttpClient client;
    private static IOkInit okInit;

    private static OkHttp instance;

    public static OkHttp getInstance() {
        if (instance == null) {
            instance = new OkHttp();
        }
        return instance;
    }

    /**
     * 初始化
     */
    public static void init(IOkInit iOkInit) {
        if (okInit == null) {
            okInit = iOkInit;
        }
    }

    public static void setMaxTransFileCount(int max) {
        getClient().dispatcher().setMaxRequestsPerHost(max);
    }

    public static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }

    public static RequestBody setFormBody(Param params) {
        return setFormBody(params, true);
    }

    public static RequestBody setFormBody(Param params, boolean addDefaultParams) {
        FormBody.Builder builder = new FormBody.Builder();
        for (String key : params.keySet()) {
            builder.add(key, params.get(key));
        }
        if (addDefaultParams) {
            Param defaultParams = okInit.setDefaultParams(new Param());
            for (String key : defaultParams.keySet()) {
                builder.add(key, params.get(key));
            }
        }
        return builder.build();
    }

    public Call get(String url, boolean addDefaultHeader, OkResponseListener okResponseListener) {
       return get(url, null, addDefaultHeader, okResponseListener);
    }

    public Call get(String url, Header header, boolean addDefaultHeader, final OkResponseListener okResponseListener) {
        return postOrGet(url, null, header, addDefaultHeader, okResponseListener);
    }

    public Call postForm(String url, @NonNull RequestBody body, OkResponseListener okResponseListener) {
      return  postForm(url, body, null, true, okResponseListener);
    }

    public Call postForm(String url, @NonNull RequestBody body, boolean addDefaultHeader, OkResponseListener okResponseListener) {
        return postForm(url, body, null, addDefaultHeader, okResponseListener);
    }

    public Call postForm(String url, @NonNull RequestBody body, Header header, boolean addDefaultHeader,  OkResponseListener okResponseListener) {
        return postOrGet(url, body, header, addDefaultHeader, okResponseListener);
    }

    private Call postOrGet(String url, RequestBody body, Header header, boolean addDefaultHeader, final OkResponseListener okResponseListener) {
        final Request.Builder builder = new Request.Builder().url(url);
        if (body != null) {
            builder.post(body);
        } else {
            builder.get();
        }
        if (addDefaultHeader) {
            Header defaultHeader = okInit.setDefaultHeader(new Header());
            for (String key : defaultHeader.keySet()) {
                builder.addHeader(key, header.get(key));
            }
        }
        if (header != null) {
            for (String key : header.keySet()) {
                builder.addHeader(key, header.get(key));
            }
        }
        final Request request = builder.build();
        final Call call = getClient().newCall(request);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Response response = call.execute();
                    if (response != null) {
                        responseResult(response, call, okResponseListener);
                    } else {
                        responseResultFailure(call, okResponseListener);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    responseResultFailure(call, okResponseListener);
                }
            }
        }).start();
        return call;

        /**
         * 普通请求通过Execute()调用线程执行
         * execute也会通过okHttp的connectionPool来做请求，文件传输则使用线程池做enqueue请求可以限制文件同时上传的数量
         * 下面注释的使用线程池来做请求
         */
        /*try {
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                }
                @Override
                public void onFailure(Call call, IOException e) {
                    okInit.noNetwork(call);
                    okResponseListener.handleNoNetwork(call);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    /**
     * 文件上传，通过修改setMaxTransFileCount()来设置同时上传文件的数量
     *
     * @param url
     * @param file
     * @param okResponseListener
     * @param fileProgressListener
     */
    public static Call uploadFile(String url, File file, final OkResponseListener okResponseListener, OkFileHelper.FileProgressListener fileProgressListener) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(MEDIA_TYPE_MULTI_DATA, file))
                .build();
        OkFileHelper.ProgressRequestBody progressRequestBody = new OkFileHelper.ProgressRequestBody(requestBody, fileProgressListener);
        Request request = new Request.Builder()
                .url(url)
                .post(progressRequestBody)
                .build();
        Call call = OkHttp.getClient().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                responseResult(response, call, okResponseListener);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                responseResultFailure(call, okResponseListener);
            }
        });
        return call;
    }

    /**
     * 响应处理返回请求的结果
     *
     * @param response
     * @param call
     * @param okResponseListener
     */
    private static void responseResult(Response response, Call call, OkResponseListener okResponseListener) {
        if (response.isSuccessful()) {
            try {
                String strResult = response.body().string();
                JSONObject jsonObject = JSON.parseObject(strResult);
                int resultCode = okInit.judgeResponse(call, response, jsonObject);
                if (okInit.responseSuccess(call, response, jsonObject, resultCode))
                    return;
                switch (resultCode) {
                    case RESULT_SUCCESS:
                        if (okResponseListener != null)
                            okResponseListener.handleJsonSuccess(call, response, jsonObject);
                        break;
                    case RESULT_ERROR:
                        if (okResponseListener != null)
                            okResponseListener.handleJsonError(call, response, jsonObject);
                        break;
                    case RESULT_VERIFY_ERROR:
                        if (okResponseListener != null)
                            okResponseListener.handleJsonVerifyError(call, response, jsonObject);
                        break;
                    default:
                        if (okResponseListener != null)
                            okResponseListener.handleJsonOther(call, response, jsonObject);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                okInit.parseResponseFailed(call, response);
                if (okResponseListener != null) okResponseListener.handleParseError(call, response);
            }
        } else {
            okInit.networkError(call, response);
        }
    }

    private static void responseResultFailure(Call call, OkResponseListener okResponseListener) {
        okInit.noNetwork(call);
        if (okResponseListener != null) okResponseListener.handleNoNetwork(call);
    }

    public static abstract class OkResponseListener implements IOkResponseListener {

        protected void handleJsonVerifyError(Call call, Response response, JSONObject json) {

        }

        protected void handleJsonOther(Call call, Response response, JSONObject json) {

        }

        protected void handleParseError(Call call, Response response) {

        }

        protected void handleNoNetwork(Call call) {

        }
    }

    interface IOkResponseListener {
        void handleJsonSuccess(Call call, Response response, JSONObject json);

        void handleJsonError(Call call, Response response, JSONObject json);
    }

    public interface IOkInit {
        /**
         * 对返回的结果进行判断
         * 0：结果错误
         * 1：正确
         * 2：其它
         * 在IOkResponse接口中处理上面的情况
         *
         * @param call
         * @param response
         * @param json
         * @return
         */
        int judgeResponse(Call call, Response response, JSONObject json);

        /**
         * 没有网络
         *
         * @param call
         */
        void noNetwork(Call call);

        /**
         * 返回结果不为 200
         *
         * @param call
         * @param response
         */
        void networkError(Call call, Response response);

        /**
         * 结果成功后的操作，
         * false：断续执行下面的操作
         * true：中断操作
         *
         * @param call
         * @param response
         * @param json
         * @param resultCode
         * @return
         */
        boolean responseSuccess(Call call, Response response, JSONObject json, int resultCode);

        /**
         * 解释JSON时失败，如果不为JSON
         *
         * @param call
         * @param response
         */
        void parseResponseFailed(Call call, Response response);

        /**
         * 设置默认参数
         * 当使用setFormBody等方法设置requestBody时，可选是否加入默认参数
         *
         * @param defaultParams
         * @return
         */
        Param setDefaultParams(Param defaultParams);

        /**
         * 设置默认Header
         * 当使用setFormBody等方法设置requestBody时，可选是否加入默认请求头
         *
         * @param defaultHeader
         * @return
         */
        Header setDefaultHeader(Header defaultHeader);

    }


}
