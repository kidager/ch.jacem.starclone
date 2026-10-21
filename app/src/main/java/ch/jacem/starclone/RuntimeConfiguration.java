package ch.jacem.starclone;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.LocaleList;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceManager;

import java.util.Locale;

/**
 * Design goal: Manage runtime resource configuration (locale, theme)
 */
public class RuntimeConfiguration {

    private static final String TAG = "RuntimeConfiguration";

    @NonNull
    public static Context attach(@Nullable Activity host, @NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        Locale locale = Locale.getDefault();
        if (preferences.contains(context.getString(R.string.pref_key_locale))) {
            String localeTag = preferences.getString(context.getString(R.string.pref_key_locale), "en-US");
            locale = Locale.forLanguageTag(localeTag);
        }

        context = applyLocale(context, locale, host);

        //ThemeHelper.applyDarkMode(host.getApplicationContext());
        return new ContextThemeWrapper(context, R.style.AppTheme);
    }

    @NonNull
    public static Context attach(@NonNull Context context) {
        return attach(null, context);
    }

    @NonNull
    public static Context applyLocale(@NonNull Context context, @NonNull Locale locale, @Nullable Activity host) {
        Configuration conf = context.getResources().getConfiguration();
        Locale.setDefault(locale);
        conf.setLocale(locale);
        return context.createConfigurationContext(conf);
    }

    public static Context applyTheme(@NonNull Context context, boolean darkTheme) {
        int themeRes = R.style.AppTheme;

        return new ContextThemeWrapper(context, themeRes);
    }

    @VisibleForTesting
    static class ContextConfigurationWrapper extends ContextThemeWrapper {

        public static ContextThemeWrapper wrap (Context context, int themeResId, Locale locale) {
            Configuration conf = context.getResources().getConfiguration();
            conf.setLocale(locale);
            // API 24+
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            conf.setLocales(localeList);
            context = context.createConfigurationContext(conf);
            return new ContextThemeWrapper(context, themeResId);
        }
    }
}
