package com.qzz.musiccommunity.ui.views.home.viewholder;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.model.HorizontalCardItem;
import com.qzz.musiccommunity.ui.views.home.adapter.HorizontalMusicAdapter;

public class HorizontalCardViewHolder extends RecyclerView.ViewHolder {
    private RecyclerView horizontalRecyclerView;

    public HorizontalCardViewHolder(@NonNull View itemView) {
        super(itemView);
        horizontalRecyclerView = itemView.findViewById(R.id.horizontalRecyclerView);
    }

    public void bind(HorizontalCardItem item) {
        // 设置横向RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                itemView.getContext(), LinearLayoutManager.HORIZONTAL, false);
        horizontalRecyclerView.setLayoutManager(layoutManager);


        // 创建适配器并设置数据
        HorizontalMusicAdapter adapter = new HorizontalMusicAdapter(item.getMusicList());
        horizontalRecyclerView.setAdapter(adapter);
    }
}



