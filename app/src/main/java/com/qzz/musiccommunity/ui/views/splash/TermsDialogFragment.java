package com.qzz.musiccommunity.ui.views.splash;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.qzz.musiccommunity.R;

public class TermsDialogFragment extends DialogFragment {

    private static final String TAG = "TermsDialogFragment";

    private TermsDialogListener termsDialogListener;

    public interface TermsDialogListener {
        void onTermsAccepted();
    }

    public TermsDialogFragment() {
        // Required empty public constructor
    }

    public static TermsDialogFragment newInstance() {
        return new TermsDialogFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof TermsDialogListener) {
            termsDialogListener = (TermsDialogListener) context;
        } else {
            throw new RuntimeException("Parent fragment or activity must implement TermsDialogListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 使用系统默认的DialogFragment样式，不使用自定义样式
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 不设置透明背景，使用布局的背景来控制
        return inflater.inflate(R.layout.fragment_terms_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
        setupRichText(view);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();

            // 只设置一次背景透明
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // 保持对话框尺寸设置
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);

            // 重要：只处理阴影问题，不使用dimAmount=0
            // 这是关键点：不将dimAmount设为0，而是将其设为一个合适的值
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.5f; // 使用适当的暗度，避免完全透明
            window.setAttributes(params);
        }
    }

    private void setupViews(View view) {
        // Rectangle 803 - 点击遮罩关闭
        View rectangle803 = view.findViewById(R.id.rectangle_803);
        if (rectangle803 != null) {
            rectangle803.setOnClickListener(v -> dismiss());
        }

        // 点击接受按钮
        View acceptButton = view.findViewById(R.id.button);
        if (acceptButton != null) {
            acceptButton.setOnClickListener(v -> {
                // 处理接受按钮点击事件
                dismiss();
                // 在这里添加继续操作的代码
                if (termsDialogListener != null) {
                    termsDialogListener.onTermsAccepted();
                } else {
                    Log.w(TAG, "TermsDialogListener is not set, cannot notify acceptance.");
                }
            });
        }
    }

    private void setupRichText(View view) {
        // 获取条款内容的TextView
        TextView termsTextView = view.findViewById(R.id.textView2);
        if (termsTextView == null) return;

        // 获取字符串资源
        String fullText = getString(R.string.terms_and_conditions_text);

        // 创建SpannableString对象
        SpannableString spannableString = new SpannableString(fullText);

        // 设置"《用户协议》"的样式和点击事件
        int userAgreementStart = fullText.indexOf("《用户协议》");
        int userAgreementEnd = userAgreementStart + "《用户协议》".length();

        // 添加点击事件
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // 处理用户协议点击
                Toast.makeText(getContext(), "查看用户协议详情", Toast.LENGTH_SHORT).show();
                // 这里可以添加跳转到用户协议页面的代码
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#3482FF"));
                ds.setUnderlineText(true);
            }
        }, userAgreementStart, userAgreementEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 设置文本颜色
        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#3482FF")),
                userAgreementStart, userAgreementEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 设置文本样式为粗体
        spannableString.setSpan(new StyleSpan(Typeface.BOLD),
                userAgreementStart, userAgreementEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 设置"《隐私政策》"的样式和点击事件
        int privacyPolicyStart = fullText.indexOf("《隐私政策》");
        int privacyPolicyEnd = privacyPolicyStart + "《隐私政策》".length();

        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // 处理隐私政策点击
                Toast.makeText(getContext(), "查看隐私政策详情", Toast.LENGTH_SHORT).show();
                // 这里可以添加跳转到隐私政策页面的代码
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#3482FF"));
                ds.setUnderlineText(true);
            }
        }, privacyPolicyStart, privacyPolicyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 设置文本颜色
        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#3482FF")),
                privacyPolicyStart, privacyPolicyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 设置文本样式为粗体
        spannableString.setSpan(new StyleSpan(Typeface.BOLD),
                privacyPolicyStart, privacyPolicyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 应用到TextView
        termsTextView.setText(spannableString);

        // 激活链接点击功能
        termsTextView.setMovementMethod(LinkMovementMethod.getInstance());

        // 可选：设置链接点击时的高亮颜色
        termsTextView.setHighlightColor(Color.parseColor("#33999999"));
    }


    // 帮助方法：为指定文本添加样式
    private void addStyleToText(SpannableString spannableString, String targetText, Object... styles) {
        String fullText = spannableString.toString();
        int start = fullText.indexOf(targetText);
        if (start >= 0) {
            int end = start + targetText.length();
            for (Object style : styles) {
                spannableString.setSpan(style, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }
}
