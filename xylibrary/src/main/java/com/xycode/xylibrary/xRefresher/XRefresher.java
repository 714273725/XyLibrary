package com.xycode.xylibrary.xRefresher;

/**
 * Created by XY on 2016/6/18.
 */

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.xycode.xylibrary.R;
import com.xycode.xylibrary.adapter.XAdapter;
import com.xycode.xylibrary.okHttp.OkHttp;
import com.xycode.xylibrary.okHttp.Param;
import com.xycode.xylibrary.uiKit.recyclerview.HorizontalDividerItemDecoration;
import com.xycode.xylibrary.utils.L;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by XY on 2016/6/17.
 */
public class XRefresher<T> extends RelativeLayout {

    private static final int REFRESH = 1;
    private static final int LOAD = 2;
    private static String PAGE = "page";
    private static String PAGE_SIZE = "pageSize";

    private static XAdapter.ICustomerFooter iCustomerFooter;
    private static int footerLayout = -1;
    private static Dialog loadingDialog;
    private int background;
    private int hintColor;
    private float hintSize;
    private String hint;

    private Activity activity;

    private RefreshState state;
    private XAdapter<T> adapter;

    private SwipeRefreshLayout swipe;
    private RecyclerView recyclerView;
    private TextView textView;

    private RefreshRequest refreshRequest;

    private int lastVisibleItem = 0;
    private boolean loadMore;
    private RelativeLayout rlMain;

    public static void setCustomerFooterView(@LayoutRes int footerLayout, XAdapter.ICustomerFooter iCustomerFooter) {
        XRefresher.footerLayout = footerLayout;
        XRefresher.iCustomerFooter = iCustomerFooter;
    }

    public XRefresher(Context context) {
        super(context);
    }

    public XRefresher(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.layout_refresher, this, true);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.XRefresher);

        hint = typedArray.getString(R.styleable.XRefresher_hint);
        hintSize = typedArray.getDimension(R.styleable.XRefresher_hintSize, 1);
        hintColor = typedArray.getColor(R.styleable.XRefresher_hintColor, 1);
        background = typedArray.getColor(R.styleable.XRefresher_bg, 1);

        typedArray.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        rlMain = (RelativeLayout) findViewById(R.id.rlMain);
        swipe = (SwipeRefreshLayout) findViewById(R.id.swipe);
        recyclerView = (RecyclerView) findViewById(R.id.rvMain);
        textView = (TextView) findViewById(R.id.tvMain);

        textView.setText(hint);
        if (hintSize != 1) textView.setTextSize(hintSize);
        if (hintColor != 1) textView.setTextColor(hintColor);
        if (background != 1) textView.setBackgroundColor(background);
        if (background != 1) rlMain.setBackgroundColor(background);

    }

    public void setup(Activity activity, XAdapter<T> adapter, boolean loadMore, @NonNull RefreshRequest refreshRequest) {
        setup(activity, adapter, loadMore, refreshRequest, 10);
    }

    public void setup(Activity activity, XAdapter<T> adapter, boolean loadMore, @NonNull RefreshRequest refreshRequest, int refreshPageSize) {
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        this.loadMore = loadMore;
        this.activity = activity;
        this.refreshRequest = refreshRequest;
        this.state = new RefreshState(refreshPageSize);
        this.adapter = adapter;
        this.recyclerView.setAdapter(adapter);
        ((SwipeRefreshLayout) findViewById(R.id.swipe)).setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList();
            }
        });
        if (loadMore) {
            if (iCustomerFooter != null && footerLayout != -1) {
                this.adapter.setCustomerFooter(footerLayout, iCustomerFooter);
            }
            setLoadMoreListener();
        }
    }

    private void setLoadMoreListener() {
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            private boolean swipeMore = false;

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE && swipeMore && lastVisibleItem + 1 == getAdapter().getItemCount()) {
                    if (!state.lastPage && getAdapter().getFooterState() == XAdapter.FOOTER_MORE) {
                        getAdapter().setFooterState(XAdapter.FOOTER_LOADING);
                        getDataByRefresh(state.pageIndex + 1, state.pageDefaultSize);
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                swipeMore = dy > 0;
                lastVisibleItem = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
            }
        });
    }

    /**
     * 获取小区
     *
     * @param pageSize 页面大小
     */
    private void getDataByRefresh(int pageSize) {
        getDataByRefresh(1, pageSize, REFRESH);
    }

    private void getDataByRefresh(int page, int pageSize) {
        getDataByRefresh(page, pageSize, LOAD);
    }

    private void getDataByRefresh(final int page, final int pageSize, final int refreshType) {
        Param params = new Param();
        params.put(PAGE, refreshType == REFRESH ? "1" : String.valueOf(page));
        params.put(PAGE_SIZE, String.valueOf(pageSize));
        String url = refreshRequest.setRequestParamsReturnUrl(params);
        OkHttp.getInstance().postForm(url, OkHttp.setFormBody(params, false), new OkHttp.OkResponseListener() {
            @Override
            public void handleJsonSuccess(Call call, Response response, JSONObject json) {
                if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
                final List<T> newList = refreshRequest.setListData(json);
                if (newList.size() < pageSize) state.setLastPage(true);
                final List<T> list = new ArrayList<>();

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (refreshType) {
                            case REFRESH:
                                swipe.setRefreshing(false);
                                if (state.pageIndex == 0) state.pageIndex++;
                                break;
                            default:
                                list.addAll(getAdapter().getDataList());
                                state.pageIndex++;
                                break;
                        }
                        getAdapter().setFooterState(state.lastPage ? XAdapter.FOOTER_NO_MORE : XAdapter.FOOTER_MORE);
                        if (newList.size() > 0) {
                            for (T newItem : newList) {
                                boolean hasSameItem = false;
                                for (T listItem : list) {
                                    if (refreshRequest.ignoreSameItem(newItem, listItem)) {
                                        hasSameItem = true;
                                        break;
                                    }
                                }
                                if (!hasSameItem) list.add(newItem);
                            }
                            Collections.sort(list, new Comparator<T>() {
                                public int compare(T arg0, T arg1) {
                                    return refreshRequest.compareTo(arg0, arg1);
                                }
                            });
                            getAdapter().setDataList(list);
                        }
                        textView.setVisibility(getAdapter().getDataList().size() == 0 ? VISIBLE : GONE);
                    }
                });
            }

            @Override
            public void handleJsonError(Call call, Response response, JSONObject json) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadingDialog != null && loadingDialog.isShowing())
                            loadingDialog.dismiss();
                        switch (refreshType) {
                            case REFRESH:
                                swipe.setRefreshing(false);
                                break;
                            case LOAD:
                                getAdapter().setFooterState(state.lastPage ? XAdapter.FOOTER_NO_MORE : XAdapter.FOOTER_MORE);
                                break;
                        }
                    }
                });
            }

            @Override
            protected void handleNoNetwork(Call call) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadingDialog != null && loadingDialog.isShowing())
                            loadingDialog.dismiss();
                        switch (refreshType) {
                            case REFRESH:
                                swipe.setRefreshing(false);
                                break;
                            case LOAD:
                                getAdapter().setFooterState(state.lastPage ? XAdapter.FOOTER_NO_MORE : XAdapter.FOOTER_MORE);
                                break;
                        }
                    }
                });
            }

        });
    }

    public void setNoDataHint(String hint) {
        textView.setText(hint);
    }

    /**
     * 刷新列表
     */
    public void refreshList() {
        refreshList(false);
    }

    public void refreshList(boolean showDialog) {
        if (showDialog && showDialog) {
            if (loadingDialog != null)
                loadingDialog.show();
        }
        if (getAdapter().getDataList().size() > 0) {
            getDataByRefresh(getAdapter().getDataList().size());
        } else {
            getDataByRefresh(state.pageDefaultSize);
            swipe.setRefreshing(false);
        }
    }

    public XAdapter<T> getAdapter() {
        return adapter;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public SwipeRefreshLayout getSwipeRefreshLayout() {
        return swipe;
    }

    public void setRecyclerViewDivider(@ColorRes int dividerColor, @DimenRes int dividerHeight, @DimenRes int marginLeft, @DimenRes int marginRight) {
        recyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(activity)
                .colorResId(dividerColor).sizeResId(dividerHeight)
                .marginResId(marginLeft, marginRight)
                .build()
        );
    }

    public static void setLoadingDialog(Dialog loadingDialog) {
        XRefresher.loadingDialog = loadingDialog;
    }

    public static void resetPageParamsNames(String page, String pageSize) {
        XRefresher.PAGE = page;
        XRefresher.PAGE_SIZE = pageSize;
    }

    public static abstract class RefreshRequest<T> implements IRefreshRequest<T> {

        /**
         * 去重，可以通过 return newItem.getId().equals(listItem.getId()); 返回结果，
         * 如果不去重就直接返回false;
         *
         * @param newItem
         * @param listItem
         * @return
         */
        protected boolean ignoreSameItem(T newItem, T listItem) {
            return false;
        }

        /**
         * 排列表中的数据，返回值：
         * -1 从大到小
         * 1 从小到大
         * 0 相同
         * 也可以通过： 如 long 的实例   Long.compareTo()来比较，item0:item1的话，默认从小到大
         * 若不进行比较可以直接返回 0
         *
         * @param item0
         * @param item1
         * @return
         */
        protected int compareTo(T item0, T item1) {
            return 0;
        }

        /**
         * 处理Json 把JSON中的lastPage通过state.setLastPage() 传进去，
         *
         * @param json
         * @return
         */
        /*protected boolean setIsLastPageWhenGotJson(JSONObject json) {
            return false;
        }*/
    }


    private interface IRefreshRequest<T> {
        /**
         * 把对应要返回的url要设置的params设置好，Return url;
         *
         * @param params
         * @return
         */
        String setRequestParamsReturnUrl(Param params);

        /**
         * 最后把JSON中的List return
         *
         * @param json
         * @return
         */
        List<T> setListData(JSONObject json);
    }

    public static class RefreshState implements Serializable {
        boolean lastPage = false;
        int pageIndex = 0;
        int pageDefaultSize = 10;

        public RefreshState(int pageDefaultSize) {
            this.pageDefaultSize = pageDefaultSize;
        }

        public void setLastPage(boolean lastPage) {
            this.lastPage = lastPage;
        }
    }
}
