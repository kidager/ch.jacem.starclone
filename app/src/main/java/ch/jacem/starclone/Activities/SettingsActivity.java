package ch.jacem.starclone.Activities;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import ch.jacem.starclone.Settings.FileAccessPreferencesFragment;
import ch.jacem.starclone.Settings.FileAccessSettingsFragment;
import ch.jacem.starclone.Settings.LogPreferencesFragment;
import ch.jacem.starclone.Settings.NotificationPreferencesFragment;
import ch.jacem.starclone.Settings.SettingsFragment;
import ch.jacem.starclone.Settings.GeneralPreferencesFragment;
import ch.jacem.starclone.Settings.ThemingPreferencesFragment;
import ch.jacem.starclone.util.ActivityHelper;
import ch.jacem.starclone.R;
import ch.jacem.starclone.RuntimeConfiguration;

public class SettingsActivity extends AppCompatActivity implements SettingsFragment.OnSettingCategorySelectedListener {

    public final static String THEME_CHANGED = "ch.jacem.starclone.SettingsActivity.THEME_CHANGED";
    private final String SAVED_THEME_CHANGE = "ch.jacem.starclone.SettingsActivity.OUTSTATE_THEME_CHANGED";
    private final String SAVED_FRAGMENT = "ch.jacem.starclone.SettingsActivity.RESTORE_FRAGMENT";
    private boolean themeHasChanged;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(RuntimeConfiguration.attach(this, newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHelper.applyTheme(this);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
            fragmentManager.popBackStack();
        }

        startSettingsFragment();

        if (savedInstanceState != null) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(SAVED_FRAGMENT);
            if (fragment != null) {
                restoreFragment(fragment);
            }
        }

        themeHasChanged = savedInstanceState != null && savedInstanceState.getBoolean(SAVED_THEME_CHANGE, false);
        Intent returnData = new Intent();
        returnData.putExtra(THEME_CHANGED, themeHasChanged);
        setResult(RESULT_OK, returnData);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_THEME_CHANGE, themeHasChanged);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void restoreFragment(Fragment fragment) {
        if (fragment instanceof GeneralPreferencesFragment) {
            startGeneralSettingsFragment();
        } else if (fragment instanceof FileAccessSettingsFragment) {
            startFileAccessSettingsFragment();
        } else if (fragment instanceof ThemingPreferencesFragment) {
            startLookAndFeelSettingsFragment();
        } else if (fragment instanceof NotificationPreferencesFragment) {
            startNotificationSettingsFragment();
        } else if (fragment instanceof LogPreferencesFragment) {
            startLoggingSettingsActivity();
        }
    }

    private void startSettingsFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.flFragment, ch.jacem.starclone.ui.screens.settings.SettingsFragment.newInstance());
        transaction.commit();
    }

    private void startGeneralSettingsFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.flFragment, new ch.jacem.starclone.ui.screens.settings.general.GeneralSettingsFragment(), SAVED_FRAGMENT);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void startFileAccessSettingsFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.flFragment, new ch.jacem.starclone.ui.screens.settings.fileaccess.FileAccessFragment(), SAVED_FRAGMENT);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void startLookAndFeelSettingsFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.flFragment, new ch.jacem.starclone.ui.screens.settings.theming.ThemingFragment(), SAVED_FRAGMENT);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void startNotificationSettingsFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.flFragment, new ch.jacem.starclone.ui.screens.settings.notifications.NotificationSettingsFragment(), SAVED_FRAGMENT);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void startLoggingSettingsActivity() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.flFragment, new ch.jacem.starclone.ui.screens.settings.logging.LoggingSettingsFragment(), SAVED_FRAGMENT);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onSettingCategoryClicked(int category) {
        switch (category) {
            case SettingsFragment.GENERAL_SETTINGS:
                startGeneralSettingsFragment();
                break;
            case SettingsFragment.FILE_ACCESS_SETTINGS:
                startFileAccessSettingsFragment();
                break;
            case SettingsFragment.LOOK_AND_FEEL_SETTINGS:
                startLookAndFeelSettingsFragment();
                break;
            case SettingsFragment.LOGGING_SETTINGS:
                startLoggingSettingsActivity();
                break;
            case SettingsFragment.NOTIFICATION_SETTINGS:
                startNotificationSettingsFragment();
                break;
        }
    }
}
