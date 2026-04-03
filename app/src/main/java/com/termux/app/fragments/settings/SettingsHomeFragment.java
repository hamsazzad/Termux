package com.termux.app.fragments.settings;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.termux.R;
import com.termux.app.activities.SettingsActivity;
import com.termux.app.models.UserAction;
import com.termux.shared.activities.ReportActivity;
import com.termux.shared.android.AndroidUtils;
import com.termux.shared.android.PackageUtils;
import com.termux.shared.file.FileUtils;
import com.termux.shared.interact.ShareUtils;
import com.termux.shared.models.ReportInfo;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

public class SettingsHomeFragment extends Fragment {

    private TermuxAppSharedPreferences mPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Navigation rows that don't need preferences
        setupNavigationRows(view);

        // Load preferences on a background thread, then bind UI on the main thread
        new Thread(() -> {
            if (getContext() == null) return;
            mPreferences = TermuxAppSharedPreferences.build(getContext(), true);
            if (mPreferences == null) return;

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                bindAppearanceCard(view);
                bindBehaviorCard(view);
                bindAboutCard(view);
            });
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPEARANCE
    // ─────────────────────────────────────────────────────────────────────────

    private void bindAppearanceCard(@NonNull View root) {
        // ── Font Size SeekBar ──
        SeekBar seekFontSize = root.findViewById(R.id.seekbar_font_size);
        TextView textFontSize = root.findViewById(R.id.text_font_size_value);

        int currentFontSize = mPreferences.getFontSize();
        seekFontSize.setProgress(currentFontSize);
        textFontSize.setText(String.valueOf(currentFontSize));

        seekFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textFontSize.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mPreferences.setFontSize(seekBar.getProgress());
            }
        });

        // ── Terminal Margin Switch ──
        SwitchMaterial switchMargin = root.findViewById(R.id.switch_margin_adjustment);
        switchMargin.setChecked(mPreferences.isTerminalMarginAdjustmentEnabled());
        root.findViewById(R.id.row_margin_adjustment).setOnClickListener(v -> {
            boolean newValue = !switchMargin.isChecked();
            switchMargin.setChecked(newValue);
            mPreferences.setTerminalMarginAdjustment(newValue);
        });

        // ── Show Extra Keys Toolbar Switch ──
        SwitchMaterial switchToolbar = root.findViewById(R.id.switch_show_toolbar);
        switchToolbar.setChecked(mPreferences.shouldShowTerminalToolbar());
        root.findViewById(R.id.row_show_toolbar).setOnClickListener(v -> {
            boolean newValue = !switchToolbar.isChecked();
            switchToolbar.setChecked(newValue);
            mPreferences.setShowTerminalToolbar(newValue);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BEHAVIOR
    // ─────────────────────────────────────────────────────────────────────────

    private void bindBehaviorCard(@NonNull View root) {
        // ── Soft Keyboard Enabled ──
        SwitchMaterial switchKb = root.findViewById(R.id.switch_soft_keyboard);
        switchKb.setChecked(mPreferences.isSoftKeyboardEnabled());
        root.findViewById(R.id.row_soft_keyboard).setOnClickListener(v -> {
            boolean newValue = !switchKb.isChecked();
            switchKb.setChecked(newValue);
            mPreferences.setSoftKeyboardEnabled(newValue);
        });

        // ── Soft Keyboard only if No Hardware ──
        SwitchMaterial switchKbNoHw = root.findViewById(R.id.switch_soft_keyboard_no_hw);
        switchKbNoHw.setChecked(mPreferences.isSoftKeyboardEnabledOnlyIfNoHardware());
        root.findViewById(R.id.row_soft_keyboard_no_hw).setOnClickListener(v -> {
            boolean newValue = !switchKbNoHw.isChecked();
            switchKbNoHw.setChecked(newValue);
            mPreferences.setSoftKeyboardEnabledOnlyIfNoHardware(newValue);
        });

        // ── Keep Screen On ──
        SwitchMaterial switchScreen = root.findViewById(R.id.switch_keep_screen_on);
        switchScreen.setChecked(mPreferences.shouldKeepScreenOn());
        root.findViewById(R.id.row_keep_screen_on).setOnClickListener(v -> {
            boolean newValue = !switchScreen.isChecked();
            switchScreen.setChecked(newValue);
            mPreferences.setKeepScreenOn(newValue);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ABOUT
    // ─────────────────────────────────────────────────────────────────────────

    private void bindAboutCard(@NonNull View root) {
        // App version
        TextView textVersion = root.findViewById(R.id.text_app_version);
        try {
            String versionName = requireContext()
                .getPackageManager()
                .getPackageInfo(requireContext().getPackageName(), 0)
                .versionName;
            textVersion.setText("Version " + versionName);
        } catch (Exception e) {
            textVersion.setText("Version —");
        }

        // Donate row — show only if not Google Play signed
        View donateRow = root.findViewById(R.id.row_donate);
        new Thread(() -> {
            boolean showDonate = false;
            try {
                String sha = PackageUtils.getSigningCertificateSHA256DigestForPackage(requireContext());
                if (sha != null) {
                    String release = TermuxUtils.getAPKRelease(sha);
                    showDonate = release != null &&
                        !release.equals(TermuxConstants.APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST);
                }
            } catch (Exception ignored) {}
            final boolean finalShow = showDonate;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                donateRow.setVisibility(finalShow ? View.VISIBLE : View.GONE);
            });
        }).start();

        donateRow.setOnClickListener(v ->
            ShareUtils.openUrl(requireContext(), TermuxConstants.TERMUX_DONATE_URL));

        root.findViewById(R.id.row_about).setOnClickListener(v -> {
            new Thread(() -> {
                String title = "About";
                StringBuilder sb = new StringBuilder();
                sb.append(TermuxUtils.getAppInfoMarkdownString(
                    requireContext(), TermuxUtils.AppInfoMode.TERMUX_AND_PLUGIN_PACKAGES));
                sb.append("\n\n").append(
                    AndroidUtils.getDeviceInfoMarkdownString(requireContext(), true));
                sb.append("\n\n").append(
                    TermuxUtils.getImportantLinksMarkdownString(requireContext()));

                String actionName = UserAction.ABOUT.getName();
                ReportInfo info = new ReportInfo(actionName,
                    TermuxConstants.TERMUX_APP.TERMUX_SETTINGS_ACTIVITY_NAME, title);
                info.setReportString(sb.toString());
                info.setReportSaveFileLabelAndPath(actionName,
                    Environment.getExternalStorageDirectory() + "/" +
                        FileUtils.sanitizeFileName(
                            TermuxConstants.TERMUX_APP_NAME + "-" + actionName + ".log",
                            true, true));
                ReportActivity.startReportActivity(requireContext(), info);
            }).start();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NAVIGATION to sub-screens
    // ─────────────────────────────────────────────────────────────────────────

    private void setupNavigationRows(@NonNull View root) {
        // Advanced Appearance → TerminalViewPreferencesFragment
        root.findViewById(R.id.row_advanced_appearance).setOnClickListener(v -> {
            if (getActivity() instanceof SettingsActivity) {
                ((SettingsActivity) getActivity()).navigateTo(
                    new com.termux.app.fragments.settings.termux.TerminalViewPreferencesFragment(),
                    "Advanced Appearance");
            }
        });

        // Advanced Behavior → TerminalIOPreferencesFragment
        root.findViewById(R.id.row_advanced_behavior).setOnClickListener(v -> {
            if (getActivity() instanceof SettingsActivity) {
                ((SettingsActivity) getActivity()).navigateTo(
                    new com.termux.app.fragments.settings.termux.TerminalIOPreferencesFragment(),
                    "Advanced Behavior");
            }
        });
    }
}
