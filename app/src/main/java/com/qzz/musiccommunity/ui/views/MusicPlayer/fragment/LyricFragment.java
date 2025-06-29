package com.qzz.musiccommunity.ui.views.MusicPlayer.fragment;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.ui.views.MusicPlayer.iface.ColorAwareComponent;

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

public class LyricFragment extends Fragment implements ColorAwareComponent {

    private NestedScrollView scrollView;
    private LinearLayout lyricContainer;
    private List<LyricLine> lyricLines = new ArrayList<>();
    private List<TextView> lyricTextViews = new ArrayList<>();
    private int currentHighlightIndex = -1;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    // 记录背景色，默认为深色背景
    private int backgroundColor = Color.BLACK;

    // 添加缺失的变量：记录背景是否为深色
    private boolean isDarkBackground = true;
    // 字体相关变量
    private Typeface miSansTypeface;
    private final float normalTextSize = 18f;
    private final float highlightTextSize = 20f;

    private static class LyricLine {
        long timeMs;
        String text;

        LyricLine(long timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text;
        }
    }

    @Override
    public void setIsDarkBackground(boolean isDark) {
        this.isDarkBackground = isDark;
        updateLyricsColor(); // 立即更新颜色
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lyric, container, false);
        initViews(view);

        // 加载MiSans VF字体
        loadMiSansFont();

        // 获取背景颜色
        view.post(() -> {
            Drawable background = view.getBackground();
            if (background instanceof ColorDrawable) {
                backgroundColor = ((ColorDrawable) background).getColor();
            } else {
                // 如果无法直接获取，尝试从父容器获取
                ViewGroup parent = (ViewGroup) view.getParent();
                if (parent != null && parent.getBackground() instanceof ColorDrawable) {
                    backgroundColor = ((ColorDrawable) parent.getBackground()).getColor();
                }
            }
        });

        return view;
    }

    private void loadMiSansFont() {
        try {
            // 尝试使用字体资源（Android 8.0及以上推荐）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                miSansTypeface = getResources().getFont(R.font.mi_sans_vf);
            } else {
                // 对于较旧的设备，从assets加载
                if (getActivity() != null) {
                    miSansTypeface = Typeface.createFromAsset(getActivity().getAssets(), "font/mi_sans_vf/mi_sans_vf.ttf");
                } else {
                    miSansTypeface = Typeface.DEFAULT;
                }
            }
        } catch (Exception e) {
            Log.e("LyricFragment", "加载MiSans字体失败", e);
            // 如果加载失败，使用系统默认字体
            miSansTypeface = Typeface.DEFAULT;
        }
    }


    private void initViews(View view) {
        scrollView = view.findViewById(R.id.scrollView);
        lyricContainer = view.findViewById(R.id.lyricContainer);
    }

    // 设置背景颜色方法，可以从外部调用更新背景色
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;

        // 添加空值检查和UI线程保证
        if (getActivity() != null && !getActivity().isFinishing()) {
            getActivity().runOnUiThread(() -> {
                // 如果已经有歌词显示，刷新它们的颜色
                if (!lyricTextViews.isEmpty()) {
                    updateLyricsColor();
                }
            });
        }
    }


    // 更新所有歌词的颜色
    private void updateLyricsColor() {
        for (int i = 0; i < lyricTextViews.size(); i++) {
            TextView textView = lyricTextViews.get(i);
            if (i == currentHighlightIndex) {
                // 高亮歌词使用完全相反的颜色
                textView.setTextColor(getContrastColor(backgroundColor, 1.0f));
            } else {
                // 普通歌词使用半透明的相反颜色
                textView.setTextColor(getContrastColor(backgroundColor, 0.8f));
            }
        }
    }

    private int getContrastColor(int color, float alpha) {
        // 分析背景色亮度
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);
        boolean isDarkBg = hsl[2] < 0.5f;

        // 修复：正确的变量名
        this.isDarkBackground = isDarkBg; // 更新深色背景状态

        // 基于背景色决定文字颜色
        int textColor;

        if (isDarkBg) {
            // 深色背景使用高亮度文字
            textColor = Color.rgb(
                    Math.min(255, 255 - Color.red(color) + 40),
                    Math.min(255, 255 - Color.green(color) + 40),
                    Math.min(255, 255 - Color.blue(color) + 40)
            );
        } else {
            // 浅色背景使用低亮度文字
            textColor = Color.rgb(
                    Math.max(0, 255 - Color.red(color) - 40),
                    Math.max(0, 255 - Color.green(color) - 40),
                    Math.max(0, 255 - Color.blue(color) - 40)
            );
        }

        // 确保WCAG AA级别对比度 (4.5:1)
        double contrast = ColorUtils.calculateContrast(textColor, color);
        if (contrast < 4.5) {
            if (isDarkBg) {
                // 深色背景，提高文字亮度
                textColor = Color.rgb(
                        Math.min(255, Color.red(textColor) + 30),
                        Math.min(255, Color.green(textColor) + 30),
                        Math.min(255, Color.blue(textColor) + 30)
                );
            } else {
                // 浅色背景，降低文字亮度
                textColor = Color.rgb(
                        Math.max(0, Color.red(textColor) - 30),
                        Math.max(0, Color.green(textColor) - 30),
                        Math.max(0, Color.blue(textColor) - 30)
                );
            }
        }

        // 应用透明度
        return ColorUtils.setAlphaComponent(textColor, (int)(alpha * 255));
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
        // 保持不变
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
        // 保持不变
        List<LyricLine> lyrics = new ArrayList<>();

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

            // 应用字体样式
            applyTextStyle(textView, false);

            // 使用与背景相反的半透明颜色
            textView.setTextColor(getContrastColor(backgroundColor, 0.8f));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            textView.setLayoutParams(params);

            lyricContainer.addView(textView);
            lyricTextViews.add(textView);
        }
    }

    // 应用文本样式的辅助方法
    private void applyTextStyle(TextView textView, boolean isHighlighted) {
        // 设置MiSans字体
        textView.setTypeface(miSansTypeface);

        // 设置字体粗细为305（在TextView中通常使用Typeface.create的第二个参数）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android 9.0及以上支持精确的字体粗细
            textView.setTypeface(Typeface.create(miSansTypeface, 305, false));
        }

        // 设置字体大小
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, isHighlighted ? highlightTextSize : normalTextSize);

        // 设置行高
        // 注意：Android中无法直接设置具体的行高名称如"xlarge_32"
        // 假设xlarge_32对应32dp的行高
        float lineHeight = getResources().getDimensionPixelSize(R.dimen.xlarge_32);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            textView.setLineHeight((int)lineHeight);
        } else {
            // 对于旧版本，使用setLineSpacing
            textView.setLineSpacing(lineHeight - textView.getTextSize(), 1);
        }

        // 设置字母间距
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textView.setLetterSpacing(0f);  // 0表示无额外间距
        }

        // 设置文本对齐
        textView.setGravity(Gravity.CENTER);

        // 设置内边距
        textView.setPadding(32, 16, 32, 16);
    }

    private void showDefaultLyric() {
        lyricContainer.removeAllViews();
        lyricTextViews.clear();

        TextView defaultText = new TextView(getContext());
        defaultText.setText("暂无歌词");

        // 应用字体样式
        applyTextStyle(defaultText, false);

        // 使用与背景相反的颜色
        defaultText.setTextColor(getContrastColor(backgroundColor, 0.8f));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, getResources().getDimensionPixelSize(R.dimen.xlarge_32), 0, 0);
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
                TextView prevHighlightView = lyricTextViews.get(currentHighlightIndex);
                // 使用与背景相反的半透明颜色
                prevHighlightView.setTextColor(getContrastColor(backgroundColor, 0.8f));

                // 还原字体样式
                applyTextStyle(prevHighlightView, false);
            }

            // 设置新的高亮
            if (newHighlightIndex >= 0 && newHighlightIndex < lyricTextViews.size()) {
                TextView highlightView = lyricTextViews.get(newHighlightIndex);
                // 使用与背景完全相反的颜色（不透明）
                highlightView.setTextColor(getContrastColor(backgroundColor, 1.0f));

                // 应用高亮字体样式
                applyTextStyle(highlightView, true);

                // 滚动到当前歌词
                scrollToLyric(newHighlightIndex);
            }

            currentHighlightIndex = newHighlightIndex;
        }
    }

    private void scrollToLyric(int index) {
        // 保持不变
        if (index < 0 || index >= lyricTextViews.size()) {
            return;
        }

        TextView targetView = lyricTextViews.get(index);

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


