package com.qzz.musiccommunity.ui.views.MusicPlayer.iface;

public interface ColorAwareComponent {
    /**
     * 更新组件颜色
     * @param backgroundColor 背景色
     * @param textColor 文本色
     */
    void updateColors(int backgroundColor, int textColor);

    /**
     * 设置是否为深色背景
     * @param isDark 是否为深色背景
     */
    void setIsDarkBackground(boolean isDark);
}
