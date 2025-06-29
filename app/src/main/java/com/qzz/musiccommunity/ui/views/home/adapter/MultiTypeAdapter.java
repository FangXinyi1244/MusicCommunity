package com.qzz.musiccommunity.ui.views.home.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.model.BannerItem;
import com.qzz.musiccommunity.model.HorizontalCardItem;
import com.qzz.musiccommunity.model.OneColumnItem;
import com.qzz.musiccommunity.model.TwoColumnItem;
import com.qzz.musiccommunity.model.iface.ListItem;
import com.qzz.musiccommunity.ui.views.MusicPlayer.iface.OnMusicItemClickListener;
import com.qzz.musiccommunity.ui.views.home.viewholder.BannerViewHolder;
import com.qzz.musiccommunity.ui.views.home.viewholder.HorizontalCardViewHolder;
import com.qzz.musiccommunity.ui.views.home.viewholder.OneColumnViewHolder;
import com.qzz.musiccommunity.ui.views.home.viewholder.TwoColumnViewHolder;

import java.util.ArrayList;
import java.util.List;

public class MultiTypeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "MultiTypeAdapter";

    private List<ListItem> items;
    private Context context;
    private LayoutInflater inflater;
    private OnMusicItemClickListener musicItemClickListener;

    public MultiTypeAdapter(Context context, List<ListItem> items, OnMusicItemClickListener listener) {
        this.context = context;
        this.items = items != null ? items : new ArrayList<>();
        this.inflater = LayoutInflater.from(context);
        this.musicItemClickListener = listener;
    }

    /**
     * 设置新数据
     */
    public void setItems(List<ListItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * 添加数据
     */
    public void addItems(List<ListItem> newItems) {
        if (newItems != null && !newItems.isEmpty()) {
            int startPosition = this.items.size();
            this.items.addAll(newItems);
            notifyItemRangeInserted(startPosition, newItems.size());
        }
    }

    /**
     * 插入单个项目
     */
    public void addItem(int position, ListItem item) {
        if (item != null && position >= 0 && position <= items.size()) {
            items.add(position, item);
            notifyItemInserted(position);
        }
    }

    /**
     * 移除项目
     */
    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    /**
     * 清空数据
     */
    public void clearItems() {
        int size = items.size();
        items.clear();
        notifyItemRangeRemoved(0, size);
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= items.size()) {
            Log.e(TAG, "Invalid position: " + position);
            return ListItem.TYPE_ONE_COLUMN; // 返回默认类型
        }
        return items.get(position).getItemType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            switch (viewType) {
                case ListItem.TYPE_BANNER:
                    View bannerView = inflater.inflate(R.layout.item_banner, parent, false);
                    return new BannerViewHolder(bannerView);

                case ListItem.TYPE_HORIZONTAL_CARD:
                    View horizontalView = inflater.inflate(R.layout.item_hhdk, parent, false);
                    return new HorizontalCardViewHolder(horizontalView);

                case ListItem.TYPE_ONE_COLUMN:
                    View oneColumnView = inflater.inflate(R.layout.item_one_column, parent, false);
                    return new OneColumnViewHolder(oneColumnView);

                case ListItem.TYPE_TWO_COLUMN:
                    View twoColumnView = inflater.inflate(R.layout.item_two_column, parent, false);
                    return new TwoColumnViewHolder(twoColumnView);

                default:
                    Log.w(TAG, "未知的视图类型: " + viewType + "，使用默认类型");
                    View defaultView = inflater.inflate(R.layout.item_one_column, parent, false);
                    return new OneColumnViewHolder(defaultView);
            }
        } catch (Exception e) {
            Log.e(TAG, "创建ViewHolder时发生错误", e);
            // 创建默认ViewHolder作为降级方案
            View defaultView = inflater.inflate(R.layout.item_one_column, parent, false);
            return new OneColumnViewHolder(defaultView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position < 0 || position >= items.size()) {
            Log.e(TAG, "Invalid position in onBindViewHolder: " + position);
            return;
        }

        try {
            ListItem item = items.get(position);
            if (item == null) {
                Log.w(TAG, "Item at position " + position + " is null");
                return;
            }

            switch (holder.getItemViewType()) {
                case ListItem.TYPE_BANNER:
                    if (holder instanceof BannerViewHolder && item instanceof BannerItem) {
                        ((BannerViewHolder) holder).bind((BannerItem) item, musicItemClickListener);
                    }
                    break;

                case ListItem.TYPE_HORIZONTAL_CARD:
                    if (holder instanceof HorizontalCardViewHolder && item instanceof HorizontalCardItem) {
                        ((HorizontalCardViewHolder) holder).bind((HorizontalCardItem) item, musicItemClickListener);
                    }
                    break;

                case ListItem.TYPE_ONE_COLUMN:
                    if (holder instanceof OneColumnViewHolder && item instanceof OneColumnItem) {
                        ((OneColumnViewHolder) holder).bind((OneColumnItem) item, musicItemClickListener);
                    }
                    break;

                case ListItem.TYPE_TWO_COLUMN:
                    if (holder instanceof TwoColumnViewHolder && item instanceof TwoColumnItem) {
                        ((TwoColumnViewHolder) holder).bind((TwoColumnItem) item, musicItemClickListener);
                    }
                    break;

                default:
                    Log.w(TAG, "未处理的ViewHolder类型: " + holder.getClass().getSimpleName());
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "绑定ViewHolder时发生错误，position: " + position, e);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder被回收时的生命周期回调
     */
    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        try {
            if (holder instanceof BannerViewHolder) {
                ((BannerViewHolder) holder).onViewRecycled();
            }
            // 如果其他ViewHolder也需要回收处理，可以在这里添加
        } catch (Exception e) {
            Log.e(TAG, "ViewHolder回收时发生错误", e);
        }
    }

    /**
     * ViewHolder附加到窗口时的生命周期回调
     */
    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);

        try {
            if (holder instanceof BannerViewHolder) {
                ((BannerViewHolder) holder).onViewAttachedToWindow();
            }
        } catch (Exception e) {
            Log.e(TAG, "ViewHolder附加到窗口时发生错误", e);
        }
    }

    /**
     * ViewHolder从窗口分离时的生命周期回调
     */
    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);

        try {
            if (holder instanceof BannerViewHolder) {
                ((BannerViewHolder) holder).onViewDetachedFromWindow();
            }
        } catch (Exception e) {
            Log.e(TAG, "ViewHolder从窗口分离时发生错误", e);
        }
    }

    /**
     * Adapter与RecyclerView分离时的清理工作
     */
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // 清理资源，防止内存泄漏
        context = null;
        inflater = null;
    }
}
