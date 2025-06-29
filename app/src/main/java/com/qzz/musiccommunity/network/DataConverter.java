package com.qzz.musiccommunity.network;

import com.qzz.musiccommunity.model.BannerItem;
import com.qzz.musiccommunity.model.HorizontalCardItem;
import com.qzz.musiccommunity.model.OneColumnItem;
import com.qzz.musiccommunity.model.TwoColumnItem;
import com.qzz.musiccommunity.model.iface.ListItem;
import com.qzz.musiccommunity.network.dto.ModuleConfig;

import java.util.ArrayList;
import java.util.List;

public class DataConverter {

    /**
     * 将API返回的ModuleConfig列表转换为ListItem列表
     */
    public static List<ListItem> convertToListItems(List<ModuleConfig> moduleConfigs) {
        List<ListItem> listItems = new ArrayList<>();

        if (moduleConfigs == null || moduleConfigs.isEmpty()) {
            return listItems;
        }

        for (ModuleConfig moduleConfig : moduleConfigs) {
            switch (moduleConfig.getStyle()) {
                case ListItem.TYPE_BANNER:
                    listItems.add(new BannerItem(moduleConfig));
                    break;
                case ListItem.TYPE_HORIZONTAL_CARD:
                    listItems.add(new HorizontalCardItem(moduleConfig));
                    break;
                case ListItem.TYPE_ONE_COLUMN:
                    listItems.add(new OneColumnItem(moduleConfig));
                    break;
                case ListItem.TYPE_TWO_COLUMN:
                    listItems.add(new TwoColumnItem(moduleConfig));
                    break;
                default:
                    // 未知类型，可以添加默认处理或忽略
                    break;
            }
        }

        return listItems;
    }
}

