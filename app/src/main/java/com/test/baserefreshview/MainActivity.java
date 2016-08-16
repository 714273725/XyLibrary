package com.test.baserefreshview;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.view.View;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.test.baserefreshview.ListBean.Content.ContentBean;
import com.xycode.xylibrary.adapter.XAdapter;
import com.xycode.xylibrary.base.BaseActivity;
import com.xycode.xylibrary.okHttp.OkHttp;
import com.xycode.xylibrary.okHttp.Param;
import com.xycode.xylibrary.uiKit.views.MultiImageView;
import com.xycode.xylibrary.uiKit.views.loopview.AdLoopView;
import com.xycode.xylibrary.uiKit.views.loopview.internal.BaseLoopAdapter;
import com.xycode.xylibrary.unit.ViewTypeUnit;
import com.xycode.xylibrary.unit.WH;
import com.xycode.xylibrary.utils.ImageUtils;
import com.xycode.xylibrary.utils.TS;
import com.xycode.xylibrary.utils.Tools;
import com.xycode.xylibrary.utils.downloadHelper.DownloadHelper;
import com.xycode.xylibrary.xRefresher.XRefresher;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Response;

/**
 * new
 */
public class MainActivity extends BaseActivity {

    private XRefresher xRefresher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xRefresher = (XRefresher) findViewById(R.id.xRefresher);

        XAdapter<ContentBean> adapter = new XAdapter<ContentBean>(this, new ArrayList<ContentBean>()) {
            @Override
            protected ViewTypeUnit getViewTypeUnitForLayout(ContentBean item) {
                switch (item.getId()) {
                    case "1":

                        break;
                    default:
                        break;
                }

                return new ViewTypeUnit(item.getId(), R.layout.item_house);
            }

            @Override
            public void creatingHolder(final CustomHolder holder, final List<ContentBean> dataList, ViewTypeUnit viewType) {
                switch (viewType.getLayoutId()) {
                    case R.layout.item_house:
                        MultiImageView mvItem = holder.getView(R.id.mvItem);
            /*    mvItem.setLoadImageListener(new MultiImageView.OnImageLoadListener() {
                    @Override
                    public Uri setPreviewUri(int position) {
                        WH wh = Tools.getWidthHeightFromFilename(list.get(position), "_wh", "x");
                        return Uri.parse(list.get(position)+"!"+(wh.getAspectRatio()*20)+"!20");
                    }
                });*/
                        mvItem.setOverlayDrawableListener(new MultiImageView.OnImageOverlayListener() {
                            @Override
                            public Drawable setOverlayDrawable(int position) {
                                if (position == 8) {
                                    return getResources().getDrawable(R.drawable.more_images);
                                }
                                return null;
                            }
                        });
                        mvItem.setOnItemClickListener(new MultiImageView.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                WH wh = Tools.getWidthHeightFromFilename(dataList.get(holder.getAdapterPosition()).getCoverPicture(), "_wh", "x");
                                TS.show(getThis(), "wh:"+ wh.width + " h:" + wh.height+ " r:"+wh.getAspectRatio());
                            }
                        });
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void bindingHolder(CustomHolder holder, final List<ContentBean> dataList, final int pos) {
                ContentBean item = dataList.get(pos);
                switch (getLayoutId(item.getId())) {
                    case R.layout.item_house:
                        holder.setText(R.id.tvName, item.getTitle())
//                        .setImageUrl(R.id.sdvItem, item.getCoverPicture())
                                .setText(R.id.tvText, pos + "");
                        MultiImageView mvItem = holder.getView(R.id.mvItem);

                        final List<String> list = new ArrayList<>();
                        for (int i = 0; i <= pos; i++) {
                            list.add(item.getCoverPicture() /*+"!"+ (int)(60*ratio)+ "!60"*/);
                        }
                        mvItem.setList(list);
                        break;
                    default:
                        break;
                }
            }

            @Override
            protected void creatingHeader(final CustomHolder holder, int headerKey) {
                switch (headerKey) {
                    case 1:
                        AdLoopView bannerView = holder.getView(R.id.banner);
                        setBanner(bannerView);
                        ImageUtils.loadBitmapFromFresco(getThis(), Uri.parse("http://mxycsku.qiniucdn.com/group5/M00/5B/0C/wKgBfVXdYkqAEzl0AAL6ZFMAdKk401.jpg"), new ImageUtils.IGetFrescoBitmap() {
                            @Override
                            public void afterGotBitmap(Bitmap bitmap) {

                                Bitmap bmp = ImageUtils.doGaussianBlur(bitmap, 30, false);
                                holder.setImageBitmap(R.id.iv, bmp);
                            }
                        });
                        break;
                    default:
                        break;
                }
            }
        };

        adapter.addHeader(1, R.layout.layout_banner);
        adapter.setFooter(R.layout.footer);

        xRefresher.setup(this, adapter, true, new XRefresher.RefreshRequest<ContentBean>() {
            @Override
            public String setRequestParamsReturnUrl(Param params) {

              /*  int count = 0;
                try {
                    count = Integer.getInteger("666", 1234);
                    TS.show(count+"");
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    TS.show("fhfhfhfhf");
                }*/
//                params.add("a", "b");
                return "http://192.168.1.222:9000/append/store_recommend/sell_house_page";
            }

            @Override
            public List<ContentBean> setListData(JSONObject json) {
                return JSON.parseObject(json.toString(), ListBean.class).getContent().getContent();
            }

            @Override
            protected boolean ignoreSameItem(ContentBean newItem, ContentBean listItem) {
                return newItem.getId().equals(listItem.getId());
            }
        });
//        xRefresher.refreshList();

        Param param = new Param("phone", "123").add("pw", "askfja;s");
        OkHttp.getInstance().postForm("", OkHttp.setFormBody(param, false), false, new OkHttp.OkResponseListener() {
            @Override
            public void handleJsonSuccess(Call call, Response response, JSONObject json) {

            }

            @Override
            public void handleJsonError(Call call, Response response, JSONObject json) {

            }

            @Override
            protected void handleNoServerNetwork(Call call, boolean isCanceled) {

            }

            @Override
            protected void handleResponseFailure(Call call, Response response) {
                super.handleResponseFailure(call, response);
            }
        });
    }

    private void setBanner(AdLoopView bannerView) {
        List<String> bannerList = new ArrayList<>();
        bannerList.add("http://mxycsku.qiniucdn.com/group5/M00/5B/0C/wKgBfVXdYkqAEzl0AAL6ZFMAdKk401.jpg");
        bannerList.add("http://mxycsku.qiniucdn.com/group6/M00/98/E9/wKgBjVXdGPiAUmMHAALfY_C7_7U637.jpg");
        bannerList.add("http://mxycsku.qiniucdn.com/group6/M00/96/F7/wKgBjVXbxnCABW_iAAKLH0qKKXo870.jpg");

        bannerView.setOnImageClickListener(new BaseLoopAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(PagerAdapter parent, View view, int position, int realPosition) {
                DownloadHelper.getInstance().update(getThis(), "http://m.bg114.cn/scene/api/public/down_apk/1/driver1.0.20.apk", "有新版本了啊！！");
//                TS.show(getThis(), "Hi + " + position + " real:" + realPosition);
            }
        });
        bannerView.initData(bannerList);
    }


}
