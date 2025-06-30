
package com.qzz.musiccommunity.ui.common.musicList.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.database.dto.MusicInfo;

import java.util.List;

public class MusicPlaylistAdapter extends RecyclerView.Adapter<MusicPlaylistAdapter.MusicViewHolder> {

    private List<MusicInfo> playlist;
    private MusicInfo currentPlayingMusic; // 当前正在播放的音乐
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onMusicItemClick(MusicInfo musicInfo, int position);
        void onDeleteItemClick(MusicInfo musicInfo, int position);
    }

    public MusicPlaylistAdapter(List<MusicInfo> playlist, MusicInfo currentPlayingMusic, OnItemClickListener listener) {
        this.playlist = playlist;
        this.currentPlayingMusic = currentPlayingMusic;
        this.listener = listener;
    }

    public void updatePlaylist(List<MusicInfo> newPlaylist, MusicInfo newCurrentPlayingMusic) {
        this.playlist = newPlaylist;
        this.currentPlayingMusic = newCurrentPlayingMusic;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music_playlist, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        MusicInfo music = playlist.get(position);
        holder.tvMusicName.setText(music.getMusicName());
        holder.tvArtistName.setText(music.getAuthor());

        // 判断是否是当前播放的歌曲
        if (currentPlayingMusic != null && music.getMusicUrl().equals(currentPlayingMusic.getMusicUrl())) {
            holder.tvMusicName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorAccent, null));
            holder.tvArtistName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorAccent, null));
        } else {
            holder.tvMusicName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white, null));
            holder.tvArtistName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white_alpha_50, null));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMusicItemClick(music, position);
            }
        });

        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteItemClick(music, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlist.size();
    }

    static class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView tvMusicName;
        TextView tvArtistName;
        ImageView ivDelete;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMusicName = itemView.findViewById(R.id.tvMusicName);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
    }
}


