package com.qzz.musiccommunity.ui.views.MusicPlayer.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.qzz.musiccommunity.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricFragment extends Fragment {

    private NestedScrollView scrollView;
    private LinearLayout lyricContainer;
    private List<LyricLine> lyricLines = new ArrayList<>();
    private List<TextView> lyricTextViews = new ArrayList<>();
    private int currentHighlightIndex = -1;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static class LyricLine {
        long timeMs;
        String text;

        LyricLine(long timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lyric, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view) {
        scrollView = view.findViewById(R.id.scrollView);
        lyricContainer = view.findViewById(R.id.lyricContainer);
    }

    public void updateLyric(String lyricUrl) {
        if (lyricUrl == null || lyricUrl.isEmpty()) {
            showDefaultLyric();
            return;
        }

        executorService.execute(() -> {
            try {
                String lyricContent = downloadLyric(lyricUrl);
                List<LyricLine> parsedLyrics = parseLyric(lyricContent);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        lyricLines = parsedLyrics;
                        displayLyrics();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(this::showDefaultLyric);
                }
            }
        });
    }

    private String downloadLyric(String lyricUrl) throws IOException {
        URL url = new URL(lyricUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        
        reader.close();
        inputStream.close();
        connection.disconnect();
        
        return content.toString();
    }

    private List<LyricLine> parseLyric(String lyricContent) {
        List<LyricLine> lyrics = new ArrayList<>();
        
        // LRC格式解析：[mm:ss.xx]歌词内容
        Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)");
        String[] lines = lyricContent.split("\n");
        
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.find()) {
                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));
                int centiseconds = Integer.parseInt(matcher.group(3));
                String text = matcher.group(4).trim();
                
                long timeMs = (minutes * 60 + seconds) * 1000 + centiseconds * 10;
                
                if (!text.isEmpty()) {
                    lyrics.add(new LyricLine(timeMs, text));
                }
            }
        }
        
        // 按时间排序
        Collections.sort(lyrics, (a, b) -> Long.compare(a.timeMs, b.timeMs));
        
        return lyrics;
    }

    private void displayLyrics() {
        lyricContainer.removeAllViews();
        lyricTextViews.clear();
        
        if (lyricLines.isEmpty()) {
            showDefaultLyric();
            return;
        }
        
        for (LyricLine lyricLine : lyricLines) {
            TextView textView = new TextView(getContext());
            textView.setText(lyricLine.text);
            textView.setTextSize(16);
            textView.setTextColor(Color.parseColor("#CCFFFFFF"));
            textView.setGravity(android.view.Gravity.CENTER);
            textView.setPadding(32, 16, 32, 16);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            textView.setLayoutParams(params);
            
            lyricContainer.addView(textView);
            lyricTextViews.add(textView);
        }
    }

    private void showDefaultLyric() {
        lyricContainer.removeAllViews();
        lyricTextViews.clear();
        
        TextView defaultText = new TextView(getContext());
        defaultText.setText("暂无歌词");
        defaultText.setTextSize(18);
        defaultText.setTextColor(Color.parseColor("#CCFFFFFF"));
        defaultText.setGravity(android.view.Gravity.CENTER);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        defaultText.setLayoutParams(params);
        
        lyricContainer.addView(defaultText);
    }

    public void updateProgress(int currentPositionMs) {
        if (lyricLines.isEmpty() || lyricTextViews.isEmpty()) {
            return;
        }
        
        int newHighlightIndex = -1;
        
        // 找到当前应该高亮的歌词行
        for (int i = 0; i < lyricLines.size(); i++) {
            if (currentPositionMs >= lyricLines.get(i).timeMs) {
                newHighlightIndex = i;
            } else {
                break;
            }
        }
        
        // 更新高亮状态
        if (newHighlightIndex != currentHighlightIndex) {
            // 取消之前的高亮
            if (currentHighlightIndex >= 0 && currentHighlightIndex < lyricTextViews.size()) {
                lyricTextViews.get(currentHighlightIndex).setTextColor(Color.parseColor("#CCFFFFFF"));
                lyricTextViews.get(currentHighlightIndex).setTextSize(16);
            }
            
            // 设置新的高亮
            if (newHighlightIndex >= 0 && newHighlightIndex < lyricTextViews.size()) {
                TextView highlightView = lyricTextViews.get(newHighlightIndex);
                highlightView.setTextColor(Color.WHITE);
                highlightView.setTextSize(18);
                
                // 滚动到当前歌词
                scrollToLyric(newHighlightIndex);
            }
            
            currentHighlightIndex = newHighlightIndex;
        }
    }

    private void scrollToLyric(int index) {
        if (index < 0 || index >= lyricTextViews.size()) {
            return;
        }
        
        TextView targetView = lyricTextViews.get(index);
        
        // 计算滚动位置，让当前歌词显示在屏幕中央
        int scrollY = targetView.getTop() - (scrollView.getHeight() / 2) + (targetView.getHeight() / 2);
        scrollView.smoothScrollTo(0, Math.max(0, scrollY));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

