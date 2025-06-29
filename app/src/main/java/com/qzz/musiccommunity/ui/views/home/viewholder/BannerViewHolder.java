package com.qzz.musiccommunity.ui.views.home.viewholder;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.model.BannerItem;
import com.qzz.musiccommunity.network.dto.MusicInfo;
import com.qzz.musiccommunity.ui.views.home.adapter.ImageAdapter;
import com.youth.banner.Banner;
import com.youth.banner.config.IndicatorConfig;
import com.youth.banner.indicator.CircleIndicator;
import com.youth.banner.listener.OnBannerListener;

public class BannerViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "BannerViewHolder";

    private Banner<MusicInfo, ImageAdapter> banner;
    private ImageAdapter imageAdapter;
    private Context context;

    public BannerViewHolder(@NonNull View itemView) {
        super(itemView);
        this.context = itemView.getContext();
        initViews();
        initBanner();
    }

    private void initViews() {
        banner = itemView.findViewById(R.id.banner);
    }

    private void initBanner() {
        if (banner == null) {
            Log.e(TAG, "Banner view not found in layout");
            return;
        }

        try {
            // 设置指示器
            banner.setIndicator(new CircleIndicator(context))
                    .setIndicatorGravity(IndicatorConfig.Direction.CENTER)
                    .setIndicatorSpace(8)
                    .setIndicatorRadius(4)
                    .setIndicatorHeight(8);

            // 设置轮播配置
            banner.isAutoLoop(true)
                    .setLoopTime(3000)
                    .setScrollTime(800)
                    .setUserInputEnabled(true);

            // 设置Banner的点击监听器
            banner.setOnBannerListener(new OnBannerListener<MusicInfo>() {
                @Override
                public void OnBannerClick(MusicInfo data, int position) {
                    handleBannerClick(data, position);
                }
            });

            // 添加生命周期观察者（如果Activity/Fragment实现了LifecycleOwner）
            if (context instanceof androidx.lifecycle.LifecycleOwner) {
                banner.addBannerLifecycleObserver((androidx.lifecycle.LifecycleOwner) context);
            }

        } catch (Exception e) {
            Log.e(TAG, "初始化Banner时发生错误", e);
        }
    }

    /**
     * 处理Banner点击事件
     */
    private void handleBannerClick(MusicInfo data, int position) {
        if (data == null) {
            Log.w(TAG, "Banner点击事件：数据为空");
            return;
        }

        try {
            // 显示点击反馈
            Toast.makeText(context,
                    "点击了音乐: " + (data.getMusicName() != null ? data.getMusicName() : "未知"),
                    Toast.LENGTH_SHORT).show();

            // 这里可以根据实际需求跳转到详情页或播放音乐
            // Intent intent = new Intent(context, MusicDetailActivity.class);
            // intent.putExtra("music_id", data.getId());
            // intent.putExtra("music_info", data);
            // context.startActivity(intent);

            Log.d(TAG, "Banner点击 - 位置: " + position + ", 音乐ID: " + data.getId());
        } catch (Exception e) {
            Log.e(TAG, "处理Banner点击事件时发生错误", e);
        }
    }

    /**
     * 绑定数据到ViewHolder
     */
    public void bind(BannerItem item) {
        if (banner == null) {
            Log.e(TAG, "Banner为空，无法绑定数据");
            return;
        }

        if (item == null || item.getMusicList() == null || item.getMusicList().isEmpty()) {
            // 数据为空时隐藏Banner
            banner.setVisibility(View.GONE);
            banner.stop();
            Log.d(TAG, "Banner数据为空，已隐藏");
            return;
        }

        try {
            // 确保Banner可见
            banner.setVisibility(View.VISIBLE);

            // 创建或更新适配器
            if (imageAdapter == null) {
                imageAdapter = new ImageAdapter(item.getMusicList());
                banner.setAdapter(imageAdapter);
            } else {
                // 使用新的API更新数据
                banner.setDatas(item.getMusicList());
            }

            // 设置标题（如果BannerItem有标题的话）
            if (item.getTitle() != null) {
                // 这里可以设置Banner标题，具体实现取决于你的BannerItem结构
                Log.d(TAG, "Banner标题: " + item.getTitle());
            }

            Log.d(TAG, "Banner数据绑定成功，项目数量: " + item.getMusicList().size());

        } catch (Exception e) {
            Log.e(TAG, "绑定Banner数据时发生错误", e);
            banner.setVisibility(View.GONE);
        }
    }

    /**
     * ViewHolder被回收时调用
     */
    public void onViewRecycled() {
        if (banner != null) {
            try {
                banner.stop();
                Log.d(TAG, "Banner已停止轮播（ViewHolder回收）");
            } catch (Exception e) {
                Log.e(TAG, "停止Banner轮播时发生错误", e);
            }
        }
    }

    /**
     * ViewHolder附加到窗口时调用
     */
    public void onViewAttachedToWindow() {
        if (banner != null && banner.getVisibility() == View.VISIBLE) {
            try {
                banner.start();
                Log.d(TAG, "Banner开始轮播（ViewHolder附加到窗口）");
            } catch (Exception e) {
                Log.e(TAG, "启动Banner轮播时发生错误", e);
            }
        }
    }

    /**
     * ViewHolder从窗口分离时调用
     */
    public void onViewDetachedFromWindow() {
        if (banner != null) {
            try {
                banner.stop();
                Log.d(TAG, "Banner停止轮播（ViewHolder从窗口分离）");
            } catch (Exception e) {
                Log.e(TAG, "停止Banner轮播时发生错误", e);
            }
        }
    }

    /**
     * 手动开始轮播
     */
    public void startBanner() {
        if (banner != null && banner.getVisibility() == View.VISIBLE) {
            banner.start();
        }
    }

    /**
     * 手动停止轮播
     */
    public void stopBanner() {
        if (banner != null) {
            banner.stop();
        }
    }

    /**
     * 设置Banner是否可以手动滑动
     */
    public void setUserInputEnabled(boolean enabled) {
        if (banner != null) {
            banner.setUserInputEnabled(enabled);
        }
    }

    /**
     * 获取Banner实例（用于外部操作）
     */
    public Banner<MusicInfo, ImageAdapter> getBanner() {
        return banner;
    }
}
