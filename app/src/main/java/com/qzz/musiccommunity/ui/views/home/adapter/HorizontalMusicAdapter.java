package com.qzz.musiccommunity.ui.views.home.adapter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.network.dto.MusicInfo;

import java.util.ArrayList;
import java.util.List;

public class HorizontalMusicAdapter extends RecyclerView.Adapter<HorizontalMusicAdapter.MusicViewHolder> {
    private static final String TAG = "HorizontalMusicAdapter";

    private List<MusicInfo> musicList;
    private OnItemClickListener listener;
    private int itemMarginResId; // 使用资源ID而非具体数值

    public interface OnItemClickListener {
        void onItemClick(MusicInfo musicInfo, int position);
    }

    // 构造函数改为使用资源ID
    public HorizontalMusicAdapter(List<MusicInfo> musicList, int itemMarginResId) {
        this.musicList = musicList != null ? musicList : new ArrayList<>();
        this.itemMarginResId = itemMarginResId; // 例如：R.dimen.item_horizontal_margin
    }

    // 重载构造函数，提供默认间距
    public HorizontalMusicAdapter(List<MusicInfo> musicList) {
        this(musicList, R.dimen.item_horizontal_margin); // 使用默认间距资源
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);

        // 使用资源文件获取间距值
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            int marginPx = parent.getContext().getResources()
                    .getDimensionPixelSize(itemMarginResId);
            layoutParams.setMarginStart(marginPx);
            layoutParams.setMarginEnd(marginPx);
            view.setLayoutParams(layoutParams);
        }

        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        if (position >= 0 && position < musicList.size()) {
            holder.bind(musicList.get(position), position);
        }
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    public void updateData(List<MusicInfo> newMusicList) {
        this.musicList = newMusicList != null ? newMusicList : new ArrayList<>();
        notifyDataSetChanged();
    }

    class MusicViewHolder extends RecyclerView.ViewHolder {
        private TextView leftContent;
        private ImageView rightContent;

        MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            leftContent = itemView.findViewById(R.id.leftContent);
            rightContent = itemView.findViewById(R.id.rightContent);
        }

        void bind(MusicInfo music, int position) {
            if (music == null) {
                Log.w(TAG, "MusicInfo为空，位置: " + position);
                clearContent();
                return;
            }

            try {
                // 设置音乐名称
                if (leftContent != null) {
                    String musicName = music.getMusicName();
                    leftContent.setText(musicName != null ? musicName : "未知歌曲");
                }

                // 使用优化后的方法加载封面图片
                loadCoverAsBackground(itemView, music.getCoverUrl());

                // 设置播放按钮点击事件
                if (rightContent != null) {
                    rightContent.setOnClickListener(v -> {
                        if (music.getMusicName() != null) {
                            Toast.makeText(v.getContext(),
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

                Log.d(TAG, "成功绑定音乐数据: " + music.getMusicName() + ", 位置: " + position);

            } catch (Exception e) {
                Log.e(TAG, "绑定音乐数据时发生错误，位置: " + position, e);
                clearContent();
            }
        }

        /**
         * 参照您的优化方案：使用资源文件管理圆角半径，增强健壮性
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
                                    if (layout != null) { // 增加二次检查
                                        layout.setBackground(resource);
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                    if (layout != null) {
                                        layout.setBackgroundResource(R.drawable.round_20);
                                    }
                                }
                            });

                    Log.d(TAG, "开始加载封面背景图片: " + coverUrl);
                }
            } catch (Exception e) {
                Log.e(TAG, "加载封面背景图片时发生错误: " + coverUrl, e);
                if (layout != null) {
                    layout.setBackgroundResource(R.drawable.round_20);
                }
            }
        }

        private void clearContent() {
            try {
                if (leftContent != null) {
                    leftContent.setText("");
                }
                if (itemView != null) {
                    itemView.setBackgroundResource(R.drawable.round_20);
                    itemView.setOnClickListener(null);
                }
                if (rightContent != null) {
                    rightContent.setOnClickListener(null);
                }
            } catch (Exception e) {
                Log.e(TAG, "清空内容时发生错误", e);
            }
        }
    }
}




