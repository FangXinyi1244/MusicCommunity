package com.qzz.musiccommunity.ui.views.home.adapter;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.database.dto.MusicInfo;
import com.youth.banner.adapter.BannerAdapter;

import java.util.List;

public class ImageAdapter extends BannerAdapter<MusicInfo, ImageAdapter.BannerViewHolder> {
    private static final String TAG = "ImageAdapter";

    public ImageAdapter(List<MusicInfo> dataList) {
        super(dataList);
    }

    /**
     * 更新数据并通知刷新
     */
    public void updateData(List<MusicInfo> newDataList) {
        if (newDataList != null) {
            mDatas.clear();
            mDatas.addAll(newDataList);
            notifyDataSetChanged();
        }
    }

    /**
     * 添加数据
     */
    public void addData(List<MusicInfo> newDataList) {
        if (newDataList != null && !newDataList.isEmpty()) {
            int startPosition = mDatas.size();
            mDatas.addAll(newDataList);
            notifyItemRangeInserted(startPosition, newDataList.size());
        }
    }

    @Override
    public BannerViewHolder onCreateHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner2, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindView(BannerViewHolder holder, MusicInfo data, int position, int size) {
        // 清除之前的加载任务
        Glide.with(holder.itemView.getContext()).clear(holder.imageView);

        if (data == null) {
            // 数据为空时加载默认图片
            loadCoverAsBackground(holder.imageView, null, position);
            return;
        }

        // 加载封面图片
        String coverUrl = data.getCoverUrl();
        loadCoverAsBackground(holder.imageView, coverUrl, position);

        Log.d(TAG, "绑定Banner数据 - 位置: " + position + ", 封面URL: " + coverUrl);
    }

    /**
     * 参照您的优化方案：使用Glide加载封面图片作为ImageView背景
     * 使用CustomTarget实现更精确的控制和错误处理
     */
    private void loadCoverAsBackground(ImageView imageView, String coverUrl, int position) {
        try {
            if (imageView == null) {
                Log.w(TAG, "ImageView为空，位置: " + position);
                return;
            }

            // 直接从资源获取 px 值，系统会自动处理 dp 转换
            int radiusPx = imageView.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.cover_corner_radius);

            // 构建Glide请求
            Glide.with(imageView.getContext())
                    .load(coverUrl != null && !coverUrl.trim().isEmpty() ?
                            coverUrl : R.drawable.default_music_cover)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .transform(new RoundedCorners(radiusPx))
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource,
                                                    @Nullable Transition<? super Drawable> transition) {
                            if (imageView != null) { // 二次检查防止内存泄漏
                                // 方式一：设置为背景（推荐用于需要在图片上叠加其他内容的情况）
                                imageView.setBackground(resource);
                                // 清除ImageView的src，避免重复显示
                                imageView.setImageDrawable(null);

                                // 方式二：如果您希望使用ImageView的src属性，可以用这个
                                // imageView.setImageDrawable(resource);

                                Log.d(TAG, "成功加载封面背景图片，位置: " + position);
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            if (imageView != null) {
                                // 使用您现有的默认背景
                                imageView.setBackgroundResource(R.drawable.round_20);
                                imageView.setImageDrawable(null);
                                Log.d(TAG, "清除图片加载，使用默认背景，位置: " + position);
                            }
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            if (imageView != null) {
                                // 加载失败时设置默认图片
                                imageView.setBackgroundResource(R.drawable.default_music_cover);
                                imageView.setImageDrawable(null);
                                Log.w(TAG, "图片加载失败，使用默认图片，位置: " + position);
                            }
                        }
                    });

            Log.d(TAG, "开始加载Banner封面图片: " + coverUrl + ", 位置: " + position);

        } catch (Exception e) {
            Log.e(TAG, "加载Banner封面图片时发生错误，位置: " + position + ", URL: " + coverUrl, e);
            if (imageView != null) {
                imageView.setBackgroundResource(R.drawable.round_20);
                imageView.setImageDrawable(null);
            }
        }
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            this.imageView = itemView.findViewById(R.id.banner_image);
        }

        /**
         * ViewHolder被回收时清理资源
         */
        public void onViewRecycled() {
            if (imageView != null) {
                // 清除Glide加载任务
                Glide.with(imageView.getContext()).clear(imageView);
                // 清除背景和图片，释放内存
                imageView.setBackground(null);
                imageView.setImageDrawable(null);
                Log.d("BannerViewHolder", "ViewHolder回收，清理图片资源");
            }
        }
    }
}
