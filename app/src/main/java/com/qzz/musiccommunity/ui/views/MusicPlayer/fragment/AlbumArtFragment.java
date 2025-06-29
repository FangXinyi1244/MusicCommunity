package com.qzz.musiccommunity.ui.views.MusicPlayer.fragment;

import android.animation.ObjectAnimator;
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

public class AlbumArtFragment extends Fragment {

    private ImageView ivAlbumArt;
    private ObjectAnimator rotationAnimator;
    private boolean isRotating = false;

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

