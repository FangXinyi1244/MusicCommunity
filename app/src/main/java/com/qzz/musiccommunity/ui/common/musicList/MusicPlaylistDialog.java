package com.qzz.musiccommunity.ui.common.musicList;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.Service.MusicPlayerService;
import com.qzz.musiccommunity.instance.MusicManager;
import com.qzz.musiccommunity.database.dto.MusicInfo;
import com.qzz.musiccommunity.ui.common.musicList.adapter.MusicPlaylistAdapter;

import java.util.List;

public class MusicPlaylistDialog extends DialogFragment implements MusicPlaylistAdapter.OnItemClickListener {

    private static final String TAG = "MusicPlaylistDialog";

    private TextView tvPlaylistTitle;
    private LinearLayout llPlayMode;
    private ImageView ivPlayModeIcon;
    private TextView tvPlayModeText;
    private RecyclerView rvMusicList;
    private MusicPlaylistAdapter adapter;
    private MusicManager musicManager;
    private MusicPlayerService.PlayMode currentPlayMode = MusicPlayerService.PlayMode.SEQUENCE;

    // 播放列表变化的监听器接口
    public interface OnPlaylistActionListener {
        void onPlayMusicFromPlaylist(int position);
        void onPlaylistChanged();
        void onPlayModeChanged(MusicPlayerService.PlayMode playMode);
        MusicPlayerService.PlayMode getCurrentPlayMode();
    }

    private OnPlaylistActionListener playlistActionListener;

    public static MusicPlaylistDialog newInstance() {
        return new MusicPlaylistDialog();
    }

    public MusicPlaylistDialog() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置底部弹窗样式
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogStyle);
        musicManager = MusicManager.getInstance();

        // 获取监听器
        if (getActivity() instanceof OnPlaylistActionListener) {
            playlistActionListener = (OnPlaylistActionListener) getActivity();
            // 同步当前播放模式
            currentPlayMode = playlistActionListener.getCurrentPlayMode();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();
        if (window != null) {
            // 设置窗口属性
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            // 设置进入和退出动画
            window.setWindowAnimations(R.style.BottomDialogAnimation);

            // 设置背景透明
            window.setBackgroundDrawableResource(android.R.color.transparent);

            // 设置状态栏和导航栏样式
            window.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_music_playlist, container, false);
        initViews(view);
        setupRecyclerView();
        setupPlayModeToggle();
        updatePlaylistTitle();
        return view;
    }

    private void initViews(View view) {
        tvPlaylistTitle = view.findViewById(R.id.tvPlaylistTitle);
        llPlayMode = view.findViewById(R.id.llPlayMode);
        ivPlayModeIcon = view.findViewById(R.id.ivPlayModeIcon);
        tvPlayModeText = view.findViewById(R.id.tvPlayModeText);
        rvMusicList = view.findViewById(R.id.rvMusicList);
    }

    private void setupRecyclerView() {
        List<MusicInfo> playlist = musicManager.getPlaylist();
        adapter = new MusicPlaylistAdapter(playlist, musicManager.getCurrentMusic(), this);
        rvMusicList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMusicList.setAdapter(adapter);
    }

    private void setupPlayModeToggle() {
        llPlayMode.setOnClickListener(v -> togglePlayMode());
        updatePlayModeDisplay();
    }

    private void togglePlayMode() {
        // 循环切换播放模式
        MusicPlayerService.PlayMode[] modes = MusicPlayerService.PlayMode.values();
        int currentIndex = -1;

        // 找到当前模式的索引
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == currentPlayMode) {
                currentIndex = i;
                break;
            }
        }

        // 切换到下一个模式
        int nextIndex = (currentIndex + 1) % modes.length;
        currentPlayMode = modes[nextIndex];

        updatePlayModeDisplay();

        // 通知Activity播放模式已改变
        if (playlistActionListener != null) {
            playlistActionListener.onPlayModeChanged(currentPlayMode);
        }

        Log.d(TAG, "播放模式已切换为: " + currentPlayMode);
    }

    private void updatePlayModeDisplay() {
        switch (currentPlayMode) {
            case SEQUENCE:
                ivPlayModeIcon.setImageResource(R.drawable.ic_repeat);
                tvPlayModeText.setText(R.string.play_mode_sequence);
                break;
            case RANDOM:
                ivPlayModeIcon.setImageResource(R.drawable.ic_shuffle);
                tvPlayModeText.setText(R.string.play_mode_shuffle);
                break;
            case REPEAT_ONE:
                ivPlayModeIcon.setImageResource(R.drawable.ic_repeat_one);
                tvPlayModeText.setText(R.string.play_mode_repeat_one);
                break;
        }
    }

    private void updatePlaylistTitle() {
        String title = getString(R.string.current_playing_count, musicManager.getPlaylistSize());
        tvPlaylistTitle.setText(title);
        if (adapter != null) {
            adapter.updatePlaylist(musicManager.getPlaylist(), musicManager.getCurrentMusic());
        }
    }

    @Override
    public void onMusicItemClick(MusicInfo musicInfo, int position) {
        Log.d(TAG, "点击播放歌曲: " + musicInfo.getMusicName() + ", 位置: " + position);
        musicManager.setCurrentPosition(position);
        if (playlistActionListener != null) {
            playlistActionListener.onPlayMusicFromPlaylist(position);
        }
        dismiss();
    }

    @Override
    public void onDeleteItemClick(MusicInfo musicInfo, int position) {
        Log.d(TAG, "点击删除歌曲: " + musicInfo.getMusicName() + ", 位置: " + position);

        // 使用位置删除，这样更精确
        boolean removed = musicManager.removeSong(position);

        if (removed) {
            // 通知Activity播放列表已变化
            if (playlistActionListener != null) {
                playlistActionListener.onPlaylistChanged();
            }

            // 更新对话框显示
            updatePlaylistTitle();

            // 如果播放列表为空，关闭整个应用
            if (musicManager.isPlaylistEmpty()) {
                if (getActivity() != null) {
                    getActivity().finish();
                }
                return;
            }
        } else {
            Log.e(TAG, "删除歌曲失败: " + musicInfo.getMusicName());
        }
    }

    /**
     * 外部调用此方法通知播放列表发生变化
     */
    public void notifyPlaylistChanged() {
        if (adapter != null) {
            updatePlaylistTitle();
        }
    }

    /**
     * 外部调用此方法更新播放模式显示
     */
    public void updatePlayMode(MusicPlayerService.PlayMode newPlayMode) {
        if (newPlayMode != currentPlayMode) {
            currentPlayMode = newPlayMode;
            updatePlayModeDisplay();
            Log.d(TAG, "播放模式已同步更新为: " + currentPlayMode);
        }
    }
}
