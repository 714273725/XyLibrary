package com.xycode.xylibrary.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiu on 2016/4/3.
 */
public abstract class XAdapter<T> extends RecyclerView.Adapter {

    public static final int SINGLE_LAYOUT = -1;

    public static final int FOOTER_MORE = 0;
    public static final int FOOTER_LOADING = 1;
    public static final int FOOTER_NO_MORE = 2;

    private static final int LAYOUT_FOOTER = -20331;
    private List<T> mainList;
    private List<T> dataList;
    private Context context;
    private SparseArray<Integer> layoutIdList;
    // item long click on long click Listener
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;

    private ICustomerFooter iCustomerFooter;

    private final int noFooterLayout = -1;
    private int footerLayout = noFooterLayout;
    private int footerState = FOOTER_NO_MORE;

    /**
     * use single Layout
     *
     * @param context
     * @param dataList
     * @param layoutId
     */
    public XAdapter(Context context, List<T> dataList, @LayoutRes int layoutId) {
        this.context = context;
        this.dataList = new ArrayList<>();
        this.mainList = dataList;
        this.layoutIdList = new SparseArray<>();
        layoutIdList.put(SINGLE_LAYOUT, layoutId);
    }

    /**
     * use layout list to show different holders
     *
     * @param context
     * @param dataList
     * @param layoutIdList key: viewType  value: layoutId
     */
    public XAdapter(Context context, List<T> dataList, SparseArray layoutIdList) {
        this.context = context;
        this.dataList = new ArrayList<>();
        this.mainList = dataList;
        this.layoutIdList = layoutIdList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, final int viewType) {
        if (viewType == LAYOUT_FOOTER) {
            View itemView = LayoutInflater.from(context).inflate(footerLayout, parent, false);
            final CustomHolder holder = new CustomHolder(itemView) {
                @Override
                protected void createHolder(final CustomHolder holder) {

                }
            };
            return holder;
        } else {
            @LayoutRes int layoutId = (layoutIdList.size() == 1 ? layoutIdList.get(SINGLE_LAYOUT) : layoutIdList.get(viewType));
            View itemView = LayoutInflater.from(context).inflate(layoutId, parent, false);
            final CustomHolder holder = new CustomHolder(itemView) {
                @Override
                protected void createHolder(final CustomHolder holder) {
                    holder.getRootView().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            handleItemViewClick(holder, dataList.get(holder.getAdapterPosition()));
                            if (onItemClickListener != null) {
                                onItemClickListener.onItemClick(holder, dataList.get(holder.getAdapterPosition()));
                            }
                        }
                    });

                    if (onItemLongClickListener != null) {
                        holder.getRootView().setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                onItemLongClickListener.onItemLongClick(holder, dataList.get(holder.getAdapterPosition()));
                                return false;
                            }
                        });
                    }
                    creatingHolder(holder, dataList, viewType);
                }
            };
            return holder;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position == dataList.size()) {
            if (iCustomerFooter != null) {
                iCustomerFooter.bindFooter((CustomHolder) holder, footerState);
            }
            return;
        }
        bindingHolder(((CustomHolder) holder), dataList, position);
    }

    /**
     * when create Holder
     *
     * @param holder
     * @param dataList
     * @param viewType
     */
    public abstract void creatingHolder(CustomHolder holder, List<T> dataList, int viewType);

    /**
     * bind holder
     *
     * @param holder
     * @param dataList
     * @param pos
     */
    public abstract void bindingHolder(CustomHolder holder, List<T> dataList, int pos);

    public int getFooterState() {
        return footerState;
    }

    public void setFooterState(int footerState) {
        this.footerState = footerState;
        notifyDataSetChanged();
    }

    /**
     * override this method can show different holder for layout
     * don't return LAYOUT_FOOTER = -20331
     *
     * @param item
     * @return
     */
    protected int getItemType(T item) {
        return SINGLE_LAYOUT;
    }

    /**
     * when you use layout list, you can override this method when binding holder views
     * @param position
     * @return
     */
    @Override
    public int getItemViewType(int position) {
        if (position == dataList.size()) {
            return LAYOUT_FOOTER;
        }
        int type = getItemType(dataList.get(position));
        if (type == SINGLE_LAYOUT) {
            return super.getItemViewType(position);
        } else {
            return type;
        }
    }

    /**
     * Please use getDataList().size() to get items count，this method would add header and Footer if they exist
     *
     * @return
     */
    @Override
    public int getItemCount() {
        int footerCount = footerLayout == noFooterLayout ? 0 : 1;
        if (dataList != null) {
            return dataList.size() + footerCount;
        }
        return footerCount;
    }

    public T getItem(int pos) {
        if (dataList.size() > pos && pos > 0) {
            return dataList.get(pos);
        }
        return null;
    }

    public List<T> getDataList() {
        return mainList;
    }

    public List<T> getFilteredList() {
        return dataList;
    }

    public void setDataList(List<T> dataList) {
        mainList.clear();
        mainList.addAll(dataList);
        this.dataList.clear();
        this.dataList.addAll(setFilterForAdapter(mainList));
        notifyDataSetChanged();
    }

    public void resetDataList() {
        this.dataList.clear();
        this.dataList.addAll(setFilterForAdapter(mainList));
        notifyDataSetChanged();
    }

    public void removeItem(int pos) {
        dataList.remove(pos);
        notifyItemRemoved(pos);
    }

    public void addItem(int pos, T item) {
        dataList.add(pos, item);
        notifyItemInserted(pos);
    }

    public void updateItem(int pos, T item) {
        dataList.set(pos, item);
        notifyItemChanged(pos);
    }

    public void addItem(T item) {
        dataList.add(item);
        notifyItemInserted(getItemCount() - 1);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        if (this.onItemClickListener != null) this.onItemClickListener = null;
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        if (this.onItemLongClickListener != null) this.onItemLongClickListener = null;
        this.onItemLongClickListener = onItemLongClickListener;
    }

    public void setCustomerFooter(@LayoutRes int footerLayout, ICustomerFooter iCustomerFooter) {
        this.footerLayout = footerLayout;
        if (this.iCustomerFooter != null) this.iCustomerFooter = null;
        this.iCustomerFooter = iCustomerFooter;
    }

    /**
     * override this method to add holder rootView onclick event，when handle over continue to on ClickListener in creating holder set.
     * some view if it override Touch method and did't return，can let it no use,  eg：RippleView
     *
     * @param holder
     * @param item
     */
    protected void handleItemViewClick(CustomHolder holder, T item) {

    }

    /**
     * filter local main data list, it can use any time, it won't change the main data list.
     *
     * @param mainList
     * @return
     */
    protected List<T> setFilterForAdapter(List<T> mainList) {
        List<T> list = new ArrayList<>();
        list.addAll(mainList);
        return list;
    }

    public interface ICustomerFooter {
        void bindFooter(CustomHolder holder, int footerState);
    }

    /**
     *
     */
    public interface OnItemClickListener<T> {
        void onItemClick(CustomHolder holder, T item);
    }

    /**
     *
     */
    public interface OnItemLongClickListener<T> {
        void onItemLongClick(CustomHolder holder, T item);
    }

    public static abstract class CustomHolder extends RecyclerView.ViewHolder {

        private SparseArray<View> viewList;
        private View itemView;

        public CustomHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            viewList = new SparseArray<>();
            createHolder(this);
        }

        protected abstract void createHolder(CustomHolder holder);

        public <T extends View> T getView(int viewId) {
            View view = viewList.get(viewId);
            if (view == null) {
                view = itemView.findViewById(viewId);
                viewList.put(viewId, view);
            }
            return (T) view;
        }

        public View getRootView() {
            return itemView;
        }


        public CustomHolder setText(int viewId, @StringRes int textRes) {
            setText(viewId, itemView.getContext().getString(textRes));
            return this;
        }

        public CustomHolder setText(int viewId, String text) {
            View view = getView(viewId);
            if (view != null) {
                if (view instanceof EditText) {
                    ((EditText) view).setText(text);
                } else if (view instanceof Button) {
                    ((Button) view).setText(text);
                } else if (view instanceof TextView) {
                    ((TextView) view).setText(text);
                }
            }
            return this;
        }

        public CustomHolder setImageUrl(int viewId, String url) {
            View view = getView(viewId);
            if (view != null) {
                if (view instanceof SimpleDraweeView) {
                    ((SimpleDraweeView) view).setImageURI(Uri.parse(url));
                }else  if (view instanceof ImageView) {
                    ((ImageView) view).setImageURI(Uri.parse(url));
                }
            }
            return this;
        }

        public CustomHolder setImageURI(int viewId, Uri uri) {
            View view = getView(viewId);
            if (view != null) {
                if (view instanceof SimpleDraweeView) {
                    ((SimpleDraweeView) view).setImageURI(uri);
                }else  if (view instanceof ImageView) {
                    ((ImageView) view).setImageURI(uri);
                }
            }
            return this;
        }

        public CustomHolder setImageBitmap(int viewId, Bitmap bitmap) {
            View view = getView(viewId);
            if (view != null) {
                if (view instanceof ImageView) {
                    ((ImageView) view).setImageBitmap(bitmap);
                }
            }
            return this;
        }

        public CustomHolder setImageRes(int viewId, @DrawableRes int drawableRes) {
            View view = getView(viewId);
            if (view != null) {
                if (view instanceof ImageView) {
                    ((ImageView) view).setImageResource(drawableRes);
                }
            }
            return this;
        }
    }
}
