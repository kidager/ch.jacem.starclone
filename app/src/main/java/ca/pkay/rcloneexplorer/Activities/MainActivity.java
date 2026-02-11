package ca.pkay.rcloneexplorer.Activities;

import static ca.pkay.rcloneexplorer.util.ActivityHelper.tryStartActivityForResult;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import ca.pkay.rcloneexplorer.AppShortcutsHelper;
import ca.pkay.rcloneexplorer.BuildConfig;
import ca.pkay.rcloneexplorer.Database.json.Importer;
import ca.pkay.rcloneexplorer.Database.json.SharedPreferencesBackup;
import ca.pkay.rcloneexplorer.Dialogs.Dialogs;
import ca.pkay.rcloneexplorer.Dialogs.InputDialog;
import ca.pkay.rcloneexplorer.Dialogs.LoadingDialog;
import ca.pkay.rcloneexplorer.Fragments.FileExplorerFragment;
import ca.pkay.rcloneexplorer.Items.RemoteItem;
import ca.pkay.rcloneexplorer.R;
import ca.pkay.rcloneexplorer.Rclone;
import ca.pkay.rcloneexplorer.RemoteConfig.RemoteConfigHelper;
import ca.pkay.rcloneexplorer.RuntimeConfiguration;
import ca.pkay.rcloneexplorer.Services.StreamingService;
import ca.pkay.rcloneexplorer.Services.TriggerService;
import ca.pkay.rcloneexplorer.Settings.FileAccessSettingsFragment;
import ca.pkay.rcloneexplorer.Settings.GeneralPreferencesFragment;
import ca.pkay.rcloneexplorer.Settings.LogPreferencesFragment;
import ca.pkay.rcloneexplorer.Settings.NotificationPreferencesFragment;
import ca.pkay.rcloneexplorer.Settings.SettingsFragment;
import ca.pkay.rcloneexplorer.Settings.ThemingPreferencesFragment;
import ca.pkay.rcloneexplorer.ui.navigation.BottomNavTab;
import ca.pkay.rcloneexplorer.ui.navigation.MainActivityNavKt;
import ca.pkay.rcloneexplorer.util.ActivityHelper;
import ca.pkay.rcloneexplorer.util.FLog;
import ca.pkay.rcloneexplorer.util.PermissionManager;
import de.felixnuesse.extract.updates.UpdateChecker;
import es.dmoral.toasty.Toasty;
import java9.util.stream.Stream;

public class MainActivity extends AppCompatActivity
        implements InputDialog.OnPositive,
        InputDialog.OnNegative,
        SettingsFragment.OnSettingCategorySelectedListener {

    private static final String TAG = "MainActivity";
    public static final String MAIN_ACTIVITY_START_LOG = "MAIN_ACTIVITY_START_LOG";
    public static final String MAIN_ACTIVITY_START_IMPORT = "MAIN_ACTIVITY_START_IMPORT";
    public static final String MAIN_ACTIVITY_START_EXPORT = "MAIN_ACTIVITY_START_EXPORT";
    private static final int READ_REQUEST_CODE = 42;
    private static final int SETTINGS_CODE = 71;
    private static final int WRITE_REQUEST_CODE = 81;
    private final String FILE_EXPLORER_FRAGMENT_TAG = "ca.pkay.rcexplorer.MAIN_ACTIVITY_FILE_EXPLORER_TAG";
    private Rclone rclone;
    private Fragment fragment;
    private Context context;
    private BottomNavTab currentTab = BottomNavTab.REMOTES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityHelper.applyTheme(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean allPermissionsGranted = (new PermissionManager(this)).hasAllRequiredPermissions();
        boolean completedIntroOnce = OnboardingActivity.Companion.completedIntro(this);
        if (!allPermissionsGranted || !completedIntroOnce) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        }

        context = this;
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(false);
        }

        // Set up Compose bottom navigation
        ComposeView bottomNavCompose = findViewById(R.id.bottom_nav_compose);
        MainActivityNavKt.setupBottomNav(bottomNavCompose, this, tab -> {
            onTabSelected(tab);
            return null;
        });

        rclone = new Rclone(this);

        findViewById(R.id.locked_config_btn).setOnClickListener(v -> askForConfigPassword());

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        int lastVersionCode = sharedPreferences.getInt(getString(R.string.pref_key_version_code), -1);
        String lastVersionName = sharedPreferences.getString(getString(R.string.pref_key_version_name), "");
        int currentVersionCode = BuildConfig.VERSION_CODE / 10;
        String currentVersionName = BuildConfig.VERSION_NAME;

        if (lastVersionCode < currentVersionCode || !lastVersionName.equals(currentVersionName)) {
            if (lastVersionCode == 9) {
                AppShortcutsHelper.removeAllAppShortcuts(this);
                AppShortcutsHelper.populateAppShortcuts(this, rclone.getRemotes());
            }

            startRemotesFragment();

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(getString(R.string.pref_key_version_code), currentVersionCode);
            editor.putString(getString(R.string.pref_key_version_name), currentVersionName);
            editor.apply();
        } else if (rclone.isConfigEncrypted()) {
            askForConfigPassword();
        } else if (savedInstanceState != null) {
            fragment = getSupportFragmentManager().findFragmentByTag(FILE_EXPLORER_FRAGMENT_TAG);
            if (fragment instanceof FileExplorerFragment) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.flFragment, fragment, FILE_EXPLORER_FRAGMENT_TAG);
                transaction.commit();
            } else {
                String savedTab = savedInstanceState.getString("currentTab");
                if (savedTab != null) {
                    onTabSelected(BottomNavTab.valueOf(savedTab));
                } else {
                    startRemotesFragment();
                }
            }
        } else if (bundle != null && bundle.containsKey(AppShortcutsHelper.APP_SHORTCUT_REMOTE_NAME)) {
            String remoteName = bundle.getString(AppShortcutsHelper.APP_SHORTCUT_REMOTE_NAME);
            RemoteItem remoteItem = rclone.getRemoteItemFromName(remoteName);
            if (remoteItem != null) {
                AppShortcutsHelper.reportAppShortcutUsage(this, remoteItem.getName());
                startRemote(remoteItem, false);
            } else {
                Toasty.error(this, getString(R.string.remote_not_found), Toast.LENGTH_SHORT, true).show();
                finish();
            }
        } else {
            startRemotesFragment();
        }

        if (MAIN_ACTIVITY_START_LOG.equals(intent.getAction())) {
            startLogFragment();
        }

        if (MAIN_ACTIVITY_START_IMPORT.equals(intent.getAction())) {
            startConfigImportFlow();
        }

        if (MAIN_ACTIVITY_START_EXPORT.equals(intent.getAction())) {
            startConfigExportFlow();
        }

        TriggerService triggerService = new TriggerService(context);
        triggerService.queueTrigger();

        (new UpdateChecker(this)).schedule();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MAIN_ACTIVITY_START_LOG.equals(getIntent().getAction())) {
            startLogFragment();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentTab", currentTab.name());
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        requestPermissions();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(RuntimeConfiguration.attach(this, newBase));
    }

    private void onTabSelected(BottomNavTab tab) {
        currentTab = tab;
        MainActivityNavKt.syncSelectedTab(tab);
        switch (tab) {
            case REMOTES:
                startRemotesFragment();
                break;
            case TASKS:
                startTasksFragment();
                break;
            case LOGS:
                startLogFragment();
                break;
            case SETTINGS:
                startSettingsFragment();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (data != null) {
                uri = data.getData();
                new CopyConfigFile().execute(uri);
            }
        } else if (requestCode == SETTINGS_CODE && resultCode == RESULT_OK) {
            boolean themeChanged = data.getBooleanExtra(SettingsActivity.THEME_CHANGED, false);
            if (themeChanged) {
                recreate();
            }
        } else if (requestCode == WRITE_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri uri;
            if (data != null) {
                uri = data.getData();
                try {
                    rclone.exportConfigFile(uri);
                } catch (IOException e) {
                    FLog.e(TAG, "Could not export config file to %s", e, uri);
                    Toasty.error(this, getString(R.string.error_exporting_config_file), Toast.LENGTH_SHORT, true).show();
                }
            }
        } else if (requestCode == FileExplorerFragment.STREAMING_INTENT_RESULT) {
            Intent serveIntent = new Intent(this, StreamingService.class);
            context.stopService(serveIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            switch (permissions[i]) {
                case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                    RefreshLocalAliases refresh = new RefreshLocalAliases();
                    if (refresh.isRequired()) {
                        refresh.execute();
                    }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        File dir = getExternalCacheDir();
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                new File(dir, aChildren).delete();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (fragment instanceof FileExplorerFragment) {
            if (((FileExplorerFragment) fragment).onBackButtonPressed()) {
                return;
            } else {
                fragment = null;
            }
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        } else if (currentTab != BottomNavTab.REMOTES) {
            onTabSelected(BottomNavTab.REMOTES);
            return;
        }
        super.onBackPressed();
    }

    public void startConfigExportFlow() {
        if (rclone.isConfigFileCreated()) {
            exportConfigFile();
        } else {
            Toasty.info(this, getString(R.string.no_config_found), Toast.LENGTH_SHORT, true).show();
        }
    }

    public void startConfigImportFlow() {
        Uri configUri;
        if (rclone.isConfigFileCreated()) {
            warnUserAboutOverwritingConfiguration();
        } else if (null != (configUri = rclone.searchExternalConfig())) {
            askUseExternalConfig(configUri);
        } else {
            importConfigFile();
        }
    }

    private void startTasksFragment() {
        startFragment(ca.pkay.rcloneexplorer.ui.screens.tasks.TasksFragment.newInstance());
    }

    private void startLogFragment() {
        startFragment(ca.pkay.rcloneexplorer.ui.screens.logs.LogsFragment.newInstance());
    }

    private void startFragment(Fragment fragmentToStart) {
        View appBar = findViewById(R.id.appBar);
        if (appBar != null) appBar.setVisibility(View.VISIBLE);

        fragment = fragmentToStart;
        FragmentManager fragmentManager = getSupportFragmentManager();

        for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
            fragmentManager.popBackStack();
        }

        if (!isFinishing()) {
            fragmentManager.beginTransaction().replace(R.id.flFragment, fragment).commitAllowingStateLoss();
        }
    }

    public void startRemotesFragment() {
        startFragment(ca.pkay.rcloneexplorer.ui.screens.remotes.RemotesFragment.newInstance());
    }

    private void warnUserAboutOverwritingConfiguration() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.RoundedCornersDialog);
        builder.setTitle(R.string.replace_config_file_question);
        builder.setMessage(R.string.config_file_lost_statement);
        builder.setPositiveButton(R.string.continue_statement, (dialogInterface, i) -> {
            dialogInterface.cancel();
            Uri configUri;
            if (null != (configUri = rclone.searchExternalConfig())) {
                askUseExternalConfig(configUri);
            } else {
                importConfigFile();
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
        builder.show();
    }

    public void askUseExternalConfig(final Uri uri) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.RoundedCornersDialog);
        builder.setTitle(R.string.config_use_external_question);
        builder.setMessage(context.getString(R.string.config_import_external_explain, uri.toString()));
        builder.setPositiveButton(R.string.continue_statement, (dialogInterface, i) -> {
            dialogInterface.cancel();
            new CopyConfigFile().execute(uri);
        });
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
            dialogInterface.cancel();
            importConfigFile();
        });
        builder.show();
    }

    private void askForConfigPassword() {
        findViewById(R.id.locked_config).setVisibility(View.VISIBLE);
        InputDialog dialog = new InputDialog()
                .setTitle(R.string.config_password_protected)
                .setMessage(R.string.please_enter_password)
                .setNegativeButton(R.string.cancel)
                .setPositiveButton(R.string.okay_confirmation)
                .setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        dialog.setCancelable(false);
        dialog.show(getSupportFragmentManager(), "input dialog");
    }

    @Override
    public void onPositive(String tag, String input) {
        new DecryptConfig().execute(input);
    }

    @Override
    public void onNegative(String tag) {
        findViewById(R.id.locked_config).setVisibility(View.GONE);
        restoreConfigBackup();
        startRemotesFragment();
    }

    private void backupCurrentConfig() {
        File configFile = new File(getFilesDir(), "rclone.conf");
        File backupFile = new File(getFilesDir(), "rclone.conf-backup");
        if (configFile.exists()) {
            try {
                java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                FLog.e(TAG, "Failed to backup config", e);
            }
        }
    }

    private void restoreConfigBackup() {
        File configFile = new File(getFilesDir(), "rclone.conf");
        File backupFile = new File(getFilesDir(), "rclone.conf-backup");
        if (backupFile.exists()) {
            try {
                java.nio.file.Files.copy(backupFile.toPath(), configFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                backupFile.delete();
            } catch (IOException e) {
                FLog.e(TAG, "Failed to restore config backup", e);
            }
        }
    }

    private void deleteConfigBackup() {
        File backupFile = new File(getFilesDir(), "rclone.conf-backup");
        backupFile.delete();
    }

    public void importConfigFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        tryStartActivityForResult(this, intent, READ_REQUEST_CODE);
    }

    public void exportConfigFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String localTime = date.format(Calendar.getInstance().getTime());
        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.app_name) + "-backup-" + localTime + ".zip");
        tryStartActivityForResult(this, intent, WRITE_REQUEST_CODE);
    }

    public void requestPermissions() {
        boolean refreshLocalAliases = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(getString(R.string.pref_key_refresh_local_aliases), true);
        if (refreshLocalAliases) {
            FLog.d(TAG, "Reloading local path aliases");
            RefreshLocalAliases refresh = new RefreshLocalAliases();
            if (refresh.isRequired()) {
                refresh.execute();
            }
        }
    }

    public void openNavigationDrawer() {
        // No-op: drawer removed, using bottom navigation
    }

    public void onRemoteClick(RemoteItem remote) {
        startRemote(remote, true);
    }

    private void startSettingsFragment() {
        startFragment(ca.pkay.rcloneexplorer.ui.screens.settings.SettingsFragment.newInstance());
    }

    @Override
    public void onSettingCategoryClicked(int category) {
        Fragment settingsSubFragment = null;
        switch (category) {
            case SettingsFragment.GENERAL_SETTINGS:
                settingsSubFragment = new ca.pkay.rcloneexplorer.ui.screens.settings.general.GeneralSettingsFragment();
                break;
            case SettingsFragment.FILE_ACCESS_SETTINGS:
                settingsSubFragment = new ca.pkay.rcloneexplorer.ui.screens.settings.fileaccess.FileAccessFragment();
                break;
            case SettingsFragment.LOOK_AND_FEEL_SETTINGS:
                settingsSubFragment = new ca.pkay.rcloneexplorer.ui.screens.settings.theming.ThemingFragment();
                break;
            case SettingsFragment.LOGGING_SETTINGS:
                settingsSubFragment = new ca.pkay.rcloneexplorer.ui.screens.settings.logging.LoggingSettingsFragment();
                break;
            case SettingsFragment.NOTIFICATION_SETTINGS:
                settingsSubFragment = new ca.pkay.rcloneexplorer.ui.screens.settings.notifications.NotificationSettingsFragment();
                break;
        }
        if (settingsSubFragment != null) {
            fragment = settingsSubFragment;
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.flFragment, settingsSubFragment)
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        }
    }

    private void startRemote(RemoteItem remote, boolean addToBackStack) {
        View appBar = findViewById(R.id.appBar);
        if (appBar != null) appBar.setVisibility(View.VISIBLE);

        fragment = FileExplorerFragment.newInstance(remote);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.flFragment, fragment, FILE_EXPLORER_FRAGMENT_TAG);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();

        AppShortcutsHelper.reportAppShortcutUsage(this, remote.getName());
    }

    @SuppressLint("StaticFieldLeak")
    private class CopyConfigFile extends AsyncTask<Uri, Void, Boolean> {

        private final int SUCCESS_IMPORT = 0;
        private final int FAILURE_UNSPECIFIED = 1;
        private final int FAILURE_RCLONE_CONF_NOT_VALID = 2;
        private final int FAILURE_ZIP_NO_JSON = 3;
        private final int FAILURE_ZIP_INVALID_JSON = 4;
        private final int FAILURE_ZIP_INVALID_CONF = 5;
        private final int FAILURE_ZIP_MISSING_CONF = 6;

        private int statusCode = FAILURE_UNSPECIFIED;
        private LoadingDialog loadingDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            findViewById(R.id.locked_config).setVisibility(View.GONE);
            backupCurrentConfig();
            loadingDialog = new LoadingDialog()
                    .setTitle(R.string.copying_rclone_config)
                    .setCanCancel(false);
            loadingDialog.show(getSupportFragmentManager(), "loading dialog");
        }

        @Override
        protected Boolean doInBackground(Uri... uris) {
            ContentResolver resolver = context.getContentResolver();
            String mime = resolver.getType(uris[0]);

            if (mime.equals("application/zip")) {
                try {
                    boolean validRclone = rclone.copyConfigFileFromZip(uris[0]);
                    if (!validRclone) {
                        statusCode = FAILURE_ZIP_INVALID_CONF;
                        return false;
                    }
                    statusCode = SUCCESS_IMPORT;
                } catch (Exception e) {
                    statusCode = FAILURE_ZIP_MISSING_CONF;
                    return false;
                }

                try {
                    String json = rclone.readDatabaseJson(uris[0]);
                    Importer.importJson(json, context);
                    json = rclone.readSharedPrefs(uris[0]);
                    SharedPreferencesBackup.importJson(json, context);
                } catch (JSONException e) {
                    statusCode = FAILURE_ZIP_INVALID_JSON;
                    return false;
                } catch (Exception e) {
                    statusCode = FAILURE_ZIP_NO_JSON;
                    return false;
                }
                return true;
            }

            try {
                boolean validRclone = rclone.copyConfigFile(uris[0]);
                if (validRclone) {
                    statusCode = SUCCESS_IMPORT;
                    return true;
                }
                statusCode = FAILURE_RCLONE_CONF_NOT_VALID;
                return false;
            } catch (IOException e) {
                statusCode = FAILURE_RCLONE_CONF_NOT_VALID;
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            Dialogs.dismissSilently(loadingDialog);

            String errorMessage = getString(R.string.copying_rclone_config_fail);
            if (!success) {
                switch (statusCode) {
                    case FAILURE_RCLONE_CONF_NOT_VALID:
                    case FAILURE_ZIP_INVALID_CONF:
                        errorMessage = getString(R.string.import_configuration_fail_rclone);
                        break;
                    case FAILURE_ZIP_MISSING_CONF:
                        errorMessage = getString(R.string.import_configuration_fail_zip_conf_missing);
                        break;
                    case FAILURE_ZIP_NO_JSON:
                    case FAILURE_ZIP_INVALID_JSON:
                        errorMessage = getString(R.string.import_configuration_fail_zip_json_missing);
                        break;
                }
                Toasty.error(context, errorMessage, Toast.LENGTH_LONG, true).show();
                restoreConfigBackup();
                return;
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPreferences.getBoolean(getString(R.string.pref_key_enable_saf), false)) {
                RemoteConfigHelper.enableSaf(context);
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(getString(R.string.shared_preferences_pinned_remotes));
            editor.remove(getString(R.string.shared_preferences_drawer_pinned_remotes));
            editor.remove(getString(R.string.shared_preferences_hidden_remotes));
            editor.remove(getString(R.string.pref_key_accessible_storage_locations));
            editor.apply();

            if (rclone.isConfigEncrypted()) {
                askForConfigPassword();
            } else {
                deleteConfigBackup();
                AppShortcutsHelper.removeAllAppShortcuts(context);
                AppShortcutsHelper.populateAppShortcuts(context, rclone.getRemotes());
                startRemotesFragment();
            }
        }
    }

    private class RefreshLocalAliases extends AsyncTask<Void, Void, Boolean> {

        private String EMULATED = "5d44cd8d-397c-4107-b79b-17f2b6a071e8";
        private LoadingDialog loadingDialog;

        private boolean isPermissable(File file) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                return Environment.isExternalStorageLegacy(file);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return Environment.isExternalStorageManager(file);
            } else {
                return true;
            }
        }

        protected boolean isRequired() {
            String[] externalVolumes = null;
            String persisted = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(getString(R.string.pref_key_accessible_storage_locations), null);
            if (null != persisted) {
                externalVolumes = persisted.split("\\|");
            }
            String[] current = Stream.of(context.getExternalFilesDirs(null))
                    .filter(f -> f != null)
                    .map(this::getRootOrSelf)
                    .filter(this::isPermissable)
                    .map(File::getAbsolutePath)
                    .toArray(String[]::new);

            if (Arrays.deepEquals(externalVolumes, current)) {
                FLog.d(TAG, "Storage volumes not changed, no refresh required");
                return false;
            } else {
                FLog.d(TAG, "Storage volumes changed, refresh required");
                externalVolumes = current;
                persisted = TextUtils.join("|", current);
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                        .putString(getString(R.string.pref_key_accessible_storage_locations), persisted).apply();
                return true;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (isFinishing() || isDestroyed() || getSupportFragmentManager().isStateSaved()) {
                FLog.w(TAG, "Invalid state, stopping drive refresh");
                cancel(true);
                return;
            }
            View v = findViewById(R.id.locked_config);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
            loadingDialog = new LoadingDialog()
                    .setTitle(R.string.refreshing_local_alias_remotes)
                    .setCanCancel(false);
            loadingDialog.show(getSupportFragmentManager(), "loading dialog");
        }

        @Override
        protected Boolean doInBackground(Void... aVoid) {
            if (isCancelled()) {
                return null;
            }
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            Set<String> generated = pref.getStringSet(getString(R.string.pref_key_local_alias_remotes), new HashSet<>());
            Set<String> renamed = pref.getStringSet(getString(R.string.pref_key_renamed_remotes), new HashSet<>());
            SharedPreferences.Editor editor = pref.edit();
            for (String remote : generated) {
                rclone.deleteRemote(remote);
                renamed.remove(remote);
                editor.remove(getString(R.string.pref_key_renamed_remote_prefix, remote));
            }
            editor.putStringSet(getString(R.string.pref_key_renamed_remotes), renamed);
            editor.apply();
            File[] dirs = context.getExternalFilesDirs(null);
            for (File file : dirs) {
                if (null == file) {
                    continue;
                }
                if (file.getPath().contains(BuildConfig.APPLICATION_ID)) {
                    try {
                        File root = getVolumeRoot(file);
                        if (root.canRead()) {
                            addLocalRemote(root);
                        } else if (file.canRead()) {
                            addLocalRemote(file);
                        }
                    } catch (NullPointerException | IOException e) {
                        FLog.w(TAG, "doInBackground: could not read file", e);
                    }
                }
            }
            return null;
        }

        private File getRootOrSelf(File file) {
            File root = getVolumeRoot(file);
            if (root.canRead()) {
                return root;
            } else {
                return file;
            }
        }

        private File getVolumeRoot(File file) {
            String path = file.getAbsolutePath();
            int levelsUp = 0;
            int index = path.length();
            while (levelsUp++ <= 3) {
                index = path.lastIndexOf('/', index - 1);
            }
            return new File(path.substring(0, index));
        }

        private void addLocalRemote(File root) throws IOException {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            String name = root.getCanonicalPath();
            String id = UUID.randomUUID().toString();
            try {
                if (Environment.isExternalStorageEmulated(root)) {
                    id = EMULATED;
                }
            } catch (IllegalArgumentException e) {
                FLog.e(TAG, "RefreshLocalAliases/addLocalRemote: %s is not a valid path", e, name);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
                StorageVolume storageVolume = storageManager.getStorageVolume(root);
                if (null != storageVolume) {
                    name = storageVolume.getDescription(context);
                    if (null != storageVolume.getUuid()) {
                        id = storageVolume.getUuid();
                    }
                }
            }

            String path = root.getAbsolutePath();
            ArrayList<String> options = new ArrayList<>();
            options.add(id);
            options.add("alias");
            options.add("remote");
            options.add(path);
            FLog.d(TAG, "Adding local remote [%s] remote = %s", id, path);
            Process process = rclone.configCreate(options);
            if (null == process) {
                return;
            }
            try {
                process.waitFor();
                if (process.exitValue() != 0) {
                    FLog.w(TAG, "addLocalRemote: process error");
                    return;
                }
            } catch (InterruptedException e) {
                FLog.e(TAG, "addLocalRemote: process error", e);
                return;
            }
            Set<String> renamedRemotes = pref.getStringSet(getString(R.string.pref_key_renamed_remotes), new HashSet<>());
            Set<String> pinnedRemotes = pref.getStringSet(getString(R.string.shared_preferences_drawer_pinned_remotes), new HashSet<>());
            Set<String> generatedRemotes = pref.getStringSet(getString(R.string.pref_key_local_alias_remotes), new HashSet<>());
            renamedRemotes.add(id);
            pinnedRemotes.add(id);
            generatedRemotes.add(id);
            pref.edit()
                    .putStringSet(getString(R.string.pref_key_renamed_remotes), renamedRemotes)
                    .putString(getString(R.string.pref_key_renamed_remote_prefix, id), name)
                    .putStringSet(getString(R.string.shared_preferences_drawer_pinned_remotes), pinnedRemotes)
                    .putStringSet(getString(R.string.pref_key_local_alias_remotes), generatedRemotes)
                    .apply();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            Dialogs.dismissSilently(loadingDialog);
            AppShortcutsHelper.removeAllAppShortcuts(context);
            AppShortcutsHelper.populateAppShortcuts(context, rclone.getRemotes());

            startRemotesFragment();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class DecryptConfig extends AsyncTask<String, Void, Boolean> {

        private LoadingDialog loadingDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingDialog = new LoadingDialog()
                    .setTitle(R.string.working)
                    .setCanCancel(false);
            loadingDialog.show(getSupportFragmentManager(), "loading dialog");
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            return rclone.decryptConfig(strings[0]);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            Dialogs.dismissSilently(loadingDialog);
            if (!success) {
                Toasty.error(context, getString(R.string.error_unlocking_config), Toast.LENGTH_LONG, true).show();
                askForConfigPassword();
            } else {
                findViewById(R.id.locked_config).setVisibility(View.GONE);
                deleteConfigBackup();
                AppShortcutsHelper.removeAllAppShortcuts(context);
                AppShortcutsHelper.populateAppShortcuts(context, rclone.getRemotes());
            }
        }
    }
}
