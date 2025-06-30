package com.qzz.musiccommunity.ui.views.home.viewholder;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
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
import com.qzz.musiccommunity.model.TwoColumnItem;
import com.qzz.musiccommunity.database.dto.MusicInfo;
import com.qzz.musiccommunity.ui.views.MusicPlayer.iface.OnMusicItemClickListener;

public class TwoColumnViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "TwoColumnViewHolder";

    private View leftImageView;  // 左侧整个布局容器
    private View rightImageView; // 右侧整个布局容器

    // 左侧子View
    private TextView leftImageLeftContent;
    private ImageView leftImageRightContent;

    // 右侧子View
    private TextView rightImageLeftContent;
    private ImageView rightImageRightContent;

    // 添加监听器接口引用
    private OnMusicItemClickListener listener;

    public TwoColumnViewHolder(@NonNull View itemView) {
        super(itemView);
        initViews();
    }

    private void initViews() {
        try {

            // 获取左右两个include的布局容器
            leftImageView = itemView.findViewById(R.id.leftImageView);
            rightImageView = itemView.findViewById(R.id.rightImageView);

            // 初始化左侧子View
            if (leftImageView != null) {
                leftImageLeftContent = leftImageView.findViewById(R.id.leftContent);
                leftImageRightContent = leftImageView.findViewById(R.id.rightContent);
            }

            // 初始化右侧子View
            if (rightImageView != null) {
                rightImageLeftContent = rightImageView.findViewById(R.id.leftContent);
                rightImageRightContent = rightImageView.findViewById(R.id.rightContent);
            }

            logInitializationStatus();

        } catch (Exception e) {
            Log.e(TAG, "初始化视图时发生错误", e);
        }
    }

    /**
     * 递归查找titleText
     */
    private TextView findTitleTextView(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child instanceof TextView) {
                    String tag = (String) child.getTag();
                    if ("titleText".equals(tag)) {
                        return (TextView) child;
                    }
                } else if (child instanceof ViewGroup) {
                    TextView result = findTitleTextView(child);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 记录初始化状态
     */
    private void logInitializationStatus() {
        Log.d(TAG, "视图初始化状态:");
        Log.d(TAG, "leftImageView: " + (leftImageView != null ? "已找到" : "未找到"));
        Log.d(TAG, "rightImageView: " + (rightImageView != null ? "已找到" : "未找到"));
        Log.d(TAG, "leftImageLeftContent: " + (leftImageLeftContent != null ? "已找到" : "未找到"));
        Log.d(TAG, "leftImageRightContent: " + (leftImageRightContent != null ? "已找到" : "未找到"));
        Log.d(TAG, "rightImageLeftContent: " + (rightImageLeftContent != null ? "已找到" : "未找到"));
        Log.d(TAG, "rightImageRightContent: " + (rightImageRightContent != null ? "已找到" : "未找到"));
    }

    public void bind(TwoColumnItem item, OnMusicItemClickListener listener) {
        this.listener = listener; // 保存监听器引用

        if (item == null) {
            Log.w(TAG, "TwoColumnItem为空，无法绑定数据");
            clearAllContent();
            return;
        }
        try {
            // 根据音乐数据数量进行不同处理
            if (item.getMusicList() != null && item.getMusicList().size() >= 2) {
                bindTwoMusicItems(item.getMusicList().get(0), item.getMusicList().get(1));
            } else if (item.getMusicList() != null && item.getMusicList().size() == 1) {
                bindOneMusicItem(item.getMusicList().get(0));
            } else {
                bindNoMusicItems();
            }
        } catch (Exception e) {
            Log.e(TAG, "绑定TwoColumnItem数据时发生错误", e);
            clearAllContent();
        }
    }

    /**
     * 绑定两个音乐项
     */
    private void bindTwoMusicItems(MusicInfo leftMusic, MusicInfo rightMusic) {
        Log.d(TAG, "绑定两个音乐项");
        bindMusicToLeftView(leftMusic);
        bindMusicToRightView(rightMusic);

        // 显示两个View
        if (leftImageView != null) {
            leftImageView.setVisibility(View.VISIBLE);
        }
        if (rightImageView != null) {
            rightImageView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 绑定一个音乐项（只显示左侧）
     */
    private void bindOneMusicItem(MusicInfo music) {
        Log.d(TAG, "绑定一个音乐项");
        bindMusicToLeftView(music);
        clearRightView();

        // 显示左侧View，隐藏右侧View
        if (leftImageView != null) {
            leftImageView.setVisibility(View.VISIBLE);
        }
        if (rightImageView != null) {
            rightImageView.setVisibility(View.GONE);
        }
    }

    /**
     * 没有音乐项时的处理
     */
    private void bindNoMusicItems() {
        Log.d(TAG, "没有音乐项，清空所有内容");
        clearLeftView();
        clearRightView();

        // 隐藏两个View
        if (leftImageView != null) {
            leftImageView.setVisibility(View.GONE);
        }
        if (rightImageView != null) {
            rightImageView.setVisibility(View.GONE);
        }
    }

    /**
     * 绑定音乐信息到左侧View
     */
    private void bindMusicToLeftView(MusicInfo music) {
        if (music == null) {
            clearLeftView();
            return;
        }
        try {
            // 设置音乐名称
            if (leftImageLeftContent != null) {
                String musicName = music.getMusicName();
                leftImageLeftContent.setText(musicName != null ? musicName : "未知歌曲");
            }
            // 使用Glide加载封面图片作为背景
            if (leftImageView != null) {
                loadCoverAsBackground(leftImageView, music.getCoverUrl());
            }
            // 设置播放按钮点击事件
            if (leftImageRightContent != null) {
                leftImageRightContent.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onPlayButtonClick(music, 0); // 左侧位置为0
                    } else {
                        // 保留原有的Toast作为回退方案
                        Toast.makeText(v.getContext(),
                                "播放: " + music.getMusicName(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // 设置整体点击事件
            if (leftImageView != null) {
                leftImageView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemClick(music, 0); // 左侧位置为0
                    }
                });
            }
            Log.d(TAG, "成功绑定左侧音乐: " + music.getMusicName());
        } catch (Exception e) {
            Log.e(TAG, "绑定左侧音乐信息时发生错误", e);
        }
    }
    /**
     * 绑定音乐信息到右侧View
     */
    private void bindMusicToRightView(MusicInfo music) {
        if (music == null) {
            clearRightView();
            return;
        }
        try {
            // 设置音乐名称
            if (rightImageLeftContent != null) {
                String musicName = music.getMusicName();
                rightImageLeftContent.setText(musicName != null ? musicName : "未知歌曲");
            }
            // 使用Glide加载封面图片作为背景
            if (rightImageView != null) {
                loadCoverAsBackground(rightImageView, music.getCoverUrl());
            }
            // 设置播放按钮点击事件
            if (rightImageRightContent != null) {
                rightImageRightContent.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onPlayButtonClick(music, 1); // 右侧位置为1
                    } else {
                        // 保留原有的Toast作为回退方案
                        Toast.makeText(v.getContext(),
                                "播放: " + music.getMusicName(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // 设置整体点击事件
            if (rightImageView != null) {
                rightImageView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemClick(music, 1); // 右侧位置为1
                    }
                });
            }
            Log.d(TAG, "成功绑定右侧音乐: " + music.getMusicName());
        } catch (Exception e) {
            Log.e(TAG, "绑定右侧音乐信息时发生错误", e);
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
     * 创建圆角背景Drawable
     */
    private Drawable createRoundedBackground(Drawable originalDrawable) {
        try {
            // 创建圆角shape
            GradientDrawable roundedShape = new GradientDrawable();
            roundedShape.setCornerRadius(dpToPx(20));
            roundedShape.setColor(Color.TRANSPARENT);

            // 创建图层叠加
            LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{
                    originalDrawable,
                    roundedShape
            });

            // 设置圆角遮罩
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(
                        itemView.getContext().getResources(),
                        drawableToBitmap(originalDrawable)
                );
                roundedBitmapDrawable.setCornerRadius(dpToPx(20));
                return roundedBitmapDrawable;
            } else {
                return layerDrawable;
            }
        } catch (Exception e) {
            Log.e(TAG, "创建圆角背景时发生错误", e);
            return originalDrawable;
        }
    }

    /**
     * Drawable转Bitmap
     */
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 100,
                drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 100,
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * dp转px
     */
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                itemView.getContext().getResources().getDisplayMetrics()
        );
    }

    /**
     * 清空左侧View内容
     */
    private void clearLeftView() {
        try {
            if (leftImageLeftContent != null) {
                leftImageLeftContent.setText("");
            }
            if (leftImageView != null) {
                leftImageView.setBackgroundResource(R.drawable.round_20);
            }
            if (leftImageRightContent != null) {
                leftImageRightContent.setOnClickListener(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "清空左侧View时发生错误", e);
        }
    }

    /**
     * 清空右侧View内容
     */
    private void clearRightView() {
        try {
            if (rightImageLeftContent != null) {
                rightImageLeftContent.setText("");
            }
            if (rightImageView != null) {
                rightImageView.setBackgroundResource(R.drawable.round_20);
            }
            if (rightImageRightContent != null) {
                rightImageRightContent.setOnClickListener(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "清空右侧View时发生错误", e);
        }
    }

    /**
     * 清空所有内容
     */
    private void clearAllContent() {
        try {

            // 清空左右两侧内容
            clearLeftView();
            clearRightView();

            Log.d(TAG, "已清空所有内容");
        } catch (Exception e) {
            Log.e(TAG, "清空所有内容时发生错误", e);
        }
    }

    /**
     * 获取绑定的数据状态（用于调试）
     */
    public void logCurrentState() {
        Log.d(TAG, "=== TwoColumnViewHolder 当前状态 ===");
        Log.d(TAG, "左侧内容: " + (leftImageLeftContent != null ? leftImageLeftContent.getText().toString() : "null"));
        Log.d(TAG, "右侧内容: " + (rightImageLeftContent != null ? rightImageLeftContent.getText().toString() : "null"));
        Log.d(TAG, "左侧View可见性: " + (leftImageView != null ? leftImageView.getVisibility() : "null"));
        Log.d(TAG, "右侧View可见性: " + (rightImageView != null ? rightImageView.getVisibility() : "null"));
        Log.d(TAG, "==============================");
    }
}

