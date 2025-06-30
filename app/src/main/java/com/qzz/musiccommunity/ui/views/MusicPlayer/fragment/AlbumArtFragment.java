package com.qzz.musiccommunity.ui.views.MusicPlayer.fragment;

import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.ui.views.MusicPlayer.iface.ColorAwareComponent;

public class AlbumArtFragment extends Fragment implements ColorAwareComponent {

    private ImageView ivAlbumArt;
    private ObjectAnimator rotationAnimator;
    private boolean isRotating = false;

    private int backgroundColor;
    private int textColor;
    private boolean isDarkBackground;


    public boolean isRotating() {
        return isRotating;
    }

    @Override
    public void updateColors(int backgroundColor, int textColor) {
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;

        // 可以通过设置外层容器的背景色或添加动态效果
        View container = getView();
        if (container != null) {
            // 例如设置背景渐变或添加特效
            // container.setBackgroundColor(backgroundColor);
        }

        // 也可以设置专辑封面的边框颜色
        if (ivAlbumArt != null) {
            // 示例：设置专辑封面的ColorFilter
            // ivAlbumArt.setColorFilter(textColor, PorterDuff.Mode.MULTIPLY);
        }
    }

    @Override
    public void setIsDarkBackground(boolean isDark) {
        this.isDarkBackground = isDark;
        // 根据背景明暗调整UI元素
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album_art, container, false);
        initViews(view);
        setupRotationAnimation();
        return view;
    }

    private void initViews(View view) {
        ivAlbumArt = view.findViewById(R.id.ivAlbumArt);
    }

    private void setupRotationAnimation() {
        rotationAnimator = ObjectAnimator.ofFloat(ivAlbumArt, "rotation", 0f, 360f);
        rotationAnimator.setDuration(20000); // 20秒转一圈
        rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotationAnimator.setRepeatMode(ObjectAnimator.RESTART);
        rotationAnimator.setInterpolator(new LinearInterpolator());
    }

    public void updateAlbumArt(String coverUrl) {
        if (ivAlbumArt != null && getContext() != null) {
            Glide.with(getContext())
                    .load(coverUrl)
                    .placeholder(R.drawable.default_album_art)
                    .error(R.drawable.default_album_art)
                    .circleCrop()  // 这一行将图片裁剪为圆形
                    .into(ivAlbumArt);
        }
    }

    // 添加设置专辑封面的方法
    public void setAlbumArt(Bitmap bitmap) {
        if (ivAlbumArt != null && getContext() != null) {
            if (bitmap != null) {
                Glide.with(getContext())
                        .load(bitmap)
                        .circleCrop()
                        .into(ivAlbumArt);
            } else {
                // 设置默认封面
                Glide.with(getContext())
                        .load(R.drawable.default_album_art)
                        .circleCrop()
                        .into(ivAlbumArt);
            }
        }
    }

    public void setRotationAnimation(boolean shouldRotate) {
        if (rotationAnimator == null) return;

        if (shouldRotate && !isRotating) {
            rotationAnimator.start();
            isRotating = true;
        } else if (!shouldRotate && isRotating) {
            rotationAnimator.pause();
            isRotating = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
        }
    }
}

