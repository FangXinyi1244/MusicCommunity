package com.qzz.musiccommunity.ui.views.MusicPlayer.iface;

import com.qzz.musiccommunity.database.dto.MusicInfo;

public interface OnMusicItemClickListener {
    void onPlayButtonClick(MusicInfo musicInfo, int position);
    void onItemClick(MusicInfo musicInfo, int position);
}

