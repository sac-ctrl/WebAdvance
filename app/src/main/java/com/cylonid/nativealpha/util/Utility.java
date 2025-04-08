package com.cylonid.nativealpha.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import com.cylonid.nativealpha.BuildConfig;
import com.cylonid.nativealpha.R;
import com.cylonid.nativealpha.WebViewActivity;
import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.WebApp;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utility {

    public static void deleteShortcuts(List<Integer> removableWebAppIds) {
        ShortcutManager manager = App.getAppContext().getSystemService(ShortcutManager.class);
        for (ShortcutInfo info : manager.getPinnedShortcuts()) {
            int id = info.getIntent().getIntExtra(Const.INTENT_WEBAPPID, -1);
            if (removableWebAppIds.contains(id)) {
                manager.disableShortcuts(Arrays.asList(info.getId()), App.getAppContext().getString(R.string.webapp_already_deleted));
            }
        }
    }

    public static void setViewAndChildrenEnabled(View view, boolean enabled) {

        view.setClickable(enabled);
        if (enabled) {
            view.setAlpha(1.0f);
        }
        else {
            view.setAlpha(0.75f);
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                setViewAndChildrenEnabled(child, enabled);
            }
        }
    }



    public static void Assert(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static Integer getWidthFromIcon(String size_string) {
        int x_index = size_string.indexOf("x");
        if (x_index == -1)
            x_index = size_string.indexOf("×");

        if (x_index == -1)
            return 1;
        String width = size_string.substring(0, x_index);

        return Integer.parseInt(width);
    }

    public static String getFileNameFromDownload(String url, String content_disposition, String mime_type) {
        String file_name = null;
        if (content_disposition != null && !content_disposition.equals("")) {
            Pattern pattern = Pattern.compile("attachment; filename=\"(.*)\"; filename\\*=UTF-8''(.*)", Pattern.CASE_INSENSITIVE);
            Matcher m = pattern.matcher(content_disposition);
            file_name = m.matches() ? m.group(2) : null;
        }
        if (file_name == null) {
            file_name = URLUtil.guessFileName(url, content_disposition, mime_type);
        }

        return file_name;
    }

}
