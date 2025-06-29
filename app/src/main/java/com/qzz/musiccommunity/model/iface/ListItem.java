package com.qzz.musiccommunity.model.iface;

public interface ListItem {
    int getItemType();
    // 定义列表项类型常量
    int TYPE_BANNER = 1;
    int TYPE_HORIZONTAL_CARD = 2;
    int TYPE_ONE_COLUMN = 3;
    int TYPE_TWO_COLUMN = 4;
}
