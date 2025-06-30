package com.qzz.musiccommunity.model;

import com.qzz.musiccommunity.model.iface.ListItem;
import com.qzz.musiccommunity.network.dto.ModuleConfig;
import com.qzz.musiccommunity.database.dto.MusicInfo;

import java.util.List;

public class HorizontalCardItem implements ListItem {
    private int moduleId;
    private String title;
    private List<MusicInfo> musicList;

    public HorizontalCardItem(ModuleConfig moduleConfig) {
        this.moduleId = moduleConfig.getModuleConfigId();
        this.title = moduleConfig.getModuleName();
        this.musicList = moduleConfig.getMusicInfoList();
    }

    @Override
    public int getItemType() {
        return TYPE_HORIZONTAL_CARD;
    }

    public int getModuleId() {
        return moduleId;
    }

    public String getTitle() {
        return title;
    }

    public List<MusicInfo> getMusicList() {
        return musicList;
    }
}
