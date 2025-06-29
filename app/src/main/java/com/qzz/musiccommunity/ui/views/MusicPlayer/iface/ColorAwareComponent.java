package com.qzz.musiccommunity.ui.views.MusicPlayer.iface;

/**
 * 使组件能够感知背景色是深色还是浅色
 */
public interface ColorAwareComponent {
    /**
     * 设置背景是否为深色
     * @param isDark true表示深色背景，false表示浅色背景
     */
    void setIsDarkBackground(boolean isDark);
}
