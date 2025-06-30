package com.qzz.musiccommunity.ui.views.home.viewholder;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.qzz.musiccommunity.model.OneColumnItem;
import com.qzz.musiccommunity.database.dto.MusicInfo;
import com.qzz.musiccommunity.ui.views.MusicPlayer.iface.OnMusicItemClickListener;

public class OneColumnViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "OneColumnViewHolder";
    private View musicItemLayout;
    private TextView leftContent;
    private ImageView rightContent;
    private OnMusicItemClickListener listener; // 添加监听器接口引用
    public OneColumnViewHolder(@NonNull View itemView) {
        super(itemView);
        musicItemLayout = itemView.findViewById(R.id.oneImageView);
        if (musicItemLayout != null) {
            leftContent = musicItemLayout.findViewById(R.id.leftContent);
            rightContent = musicItemLayout.findViewById(R.id.rightContent);
        }
    }
    public void bind(OneColumnItem item, OnMusicItemClickListener listener) {
        this.listener = listener; // 保存监听器引用

        if (item == null) {
            Log.w(TAG, "OneColumnItem为空，无法绑定数据");
            clearContent();
            return;
        }
        try {
            // 绑定音乐数据
            if (item.getMusicList() != null && !item.getMusicList().isEmpty()) {
                MusicInfo music = item.getMusicList().get(0);
                bindMusicData(music, 0); // 传递位置参数
            } else {
                clearContent();
            }
        } catch (Exception e) {
            Log.e(TAG, "绑定OneColumnItem数据时发生错误", e);
            clearContent();
        }
    }
    /**
     * 绑定音乐数据
     */
    private void bindMusicData(MusicInfo music, int position) {
        if (music == null) {
            clearContent();
            return;
        }
        try {
            // 设置音乐名称
            if (leftContent != null) {
                String musicName = music.getMusicName();
                leftContent.setText(musicName != null ? musicName : "未知歌曲");
            }
            // 加载封面图片到背景
            if (musicItemLayout != null) {
                loadCoverAsBackground(musicItemLayout, music.getCoverUrl());
            }
            // 设置播放按钮点击事件
            if (rightContent != null) {
                rightContent.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onPlayButtonClick(music, position);
                    } else {
                        // 保留原有的Toast作为回退方案
                        Toast.makeText(itemView.getContext(),
                                "播放: " + music.getMusicName(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // 设置整体点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(music, position);
                }
            });
            Log.d(TAG, "成功绑定音乐数据: " + music.getMusicName());
        } catch (Exception e) {
            Log.e(TAG, "绑定音乐数据时发生错误", e);
            clearContent();
        }
    }

    /**
     * 使用Glide加载封面图片作为布局背景
     */
    private void loadCoverAsBackground(View layout, String coverUrl) {
        try {
            if (layout != null) {
                // 直接从资源获取 px 值，系统会自动处理 dp 转换
                int radiusPx = itemView.getContext().getResources()
                        .getDimensionPixelSize(R.dimen.cover_corner_radius);

                Glide.with(itemView.getContext())
                        .load(coverUrl)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .transform(new RoundedCorners(radiusPx))
                        .into(new CustomTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource,
                                                        @Nullable Transition<? super Drawable> transition) {
                                layout.setBackground(resource);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                layout.setBackgroundResource(R.drawable.round_20);
                            }
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "加载封面背景图片时发生错误: " + coverUrl, e);
            if (layout != null) {
                layout.setBackgroundResource(R.drawable.round_20);
            }
        }
    }



    /**
     * 清空内容
     */
    private void clearContent() {
        try {
            if (leftContent != null) {
                leftContent.setText("");
            }
            if (musicItemLayout != null) {
                musicItemLayout.setBackgroundResource(R.drawable.round_20);
            }
            if (rightContent != null) {
                rightContent.setOnClickListener(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "清空内容时发生错误", e);
        }
    }
}
