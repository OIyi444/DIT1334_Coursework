package com.name.ccf.UI.Second;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

import java.util.Locale;

public class LocaleHelper {

    private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";

    // 保存用户选的语言
    public static void saveLanguage(Context context, String language) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(SELECTED_LANGUAGE, language).apply();
    }

    // 从 SharedPreferences 取出语言
    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(SELECTED_LANGUAGE, Locale.getDefault().getLanguage());
    }

    // 设置语言
    public static Context setLocale(Context context, String language) {
        saveLanguage(context, language);
        return updateResources(context, language);
    }

    // 应用保存的语言（例如在 Activity 启动时调用）
    public static Context applySavedLocale(Context context) {
        String lang = getSavedLanguage(context);
        return updateResources(context, lang);
    }

    // 真正切换语言的地方
    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        }
    }
}
