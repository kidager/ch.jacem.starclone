package ch.jacem.starclone.Activities;

import static ch.jacem.starclone.util.ActivityHelper.tryStartActivityForResult;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import ch.jacem.starclone.AppShortcutsHelper;
import ch.jacem.starclone.BuildConfig;
import ch.jacem.starclone.Database.json.Importer;
import ch.jacem.starclone.Database.json.SharedPreferencesBackup;

import ch.jacem.starclone.backup.AgeEncryption;
import ch.jacem.starclone.backup.BackupCrypto;
import ch.jacem.starclone.backup.EncryptedBackupAgent;
import ch.jacem.starclone.Items.RemoteItem;
import ch.jacem.starclone.R;
import ch.jacem.starclone.Rclone;
import ch.jacem.starclone.RemoteConfig.RemoteConfigHelper;
import ch.jacem.starclone.RuntimeConfiguration;
import ch.jacem.starclone.Services.TriggerService;
import ch.jacem.starclone.ui.navigation.BottomNavTab;
import ch.jacem.starclone.ui.navigation.MainActivityNavKt;
import ch.jacem.starclone.ui.navigation.MainPagerAdapter;
import ch.jacem.starclone.ui.screens.processes.ProcessesFragment;
import ch.jacem.starclone.ui.screens.remotes.RemotesContainerFragment;
import ch.jacem.starclone.ui.screens.settings.SettingsContainerFragment;
import ch.jacem.starclone.util.ActivityHelper;
import ch.jacem.starclone.util.FLog;
import ch.jacem.starclone.util.PermissionManager;
import ch.jacem.starclone.core.updates.UpdateChecker;
import es.dmoral.toasty.Toasty;
import java9.util.stream.Stream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static final String MAIN_ACTIVITY_START_LOG = "MAIN_ACTIVITY_START_LOG";
    public static final String MAIN_ACTIVITY_START_IMPORT = "MAIN_ACTIVITY_START_IMPORT";
    public static final String MAIN_ACTIVITY_START_EXPORT = "MAIN_ACTIVITY_START_EXPORT";
    public static final String MAIN_ACTIVITY_START_PROCESSES = "MAIN_ACTIVITY_START_PROCESSES";
    public static final String EXTRA_HIGHLIGHT_WORKER_ID = "EXTRA_HIGHLIGHT_WORKER_ID";
    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 81;
    private Rclone rclone;
    private Context context;
    private ViewPager2 viewPager;
    private MainPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Back gestures don't call onBackPressed from API 36 on
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (handleBack()) return;
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });

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

        // Set up ViewPager2
        viewPager = findViewById(R.id.viewPager);
        pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(BottomNavTab.values().length - 1);
        viewPager.setUserInputEnabled(true);

        // Restore saved page after recreation (e.g., theme change)
        if (savedInstanceState != null) {
            int savedPage = savedInstanceState.getInt("currentPage", 0);
            viewPager.setCurrentItem(savedPage, false);
        }

        // Sync ViewPager2 page changes → bottom nav
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                BottomNavTab tab = BottomNavTab.values()[position];
                MainActivityNavKt.syncSelectedTab(tab);
            }
        });

        // Set up Compose bottom navigation
        ComposeView bottomNavCompose = findViewById(R.id.bottom_nav_compose);
        MainActivityNavKt.setupBottomNav(bottomNavCompose, this,
            tab -> {
                // Tab selected → move ViewPager2
                viewPager.setCurrentItem(tab.ordinal(), false);
                return null;
            },
            tab -> {
                // Tab reselected → reset container
                onTabReselected(tab);
                return null;
            }
        );

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

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(getString(R.string.pref_key_version_code), currentVersionCode);
            editor.putString(getString(R.string.pref_key_version_name), currentVersionName);
            editor.apply();
        } else if (rclone.isConfigEncrypted()) {
            askForConfigPassword();
        } else if (bundle != null && bundle.containsKey(AppShortcutsHelper.APP_SHORTCUT_REMOTE_NAME)) {
            String remoteName = bundle.getString(AppShortcutsHelper.APP_SHORTCUT_REMOTE_NAME);
            RemoteItem remoteItem = rclone.getRemoteItemFromName(remoteName);
            if (remoteItem != null) {
                AppShortcutsHelper.reportAppShortcutUsage(this, remoteItem.getName());
                startRemote(remoteItem);
            } else {
                Toasty.error(this, getString(R.string.remote_not_found), Toast.LENGTH_SHORT, true).show();
                finish();
            }
        }

        checkPendingBackupRestore(sharedPreferences);

        if (MAIN_ACTIVITY_START_LOG.equals(intent.getAction())) {
            viewPager.setCurrentItem(BottomNavTab.LOGS.ordinal(), false);
        }

        if (MAIN_ACTIVITY_START_PROCESSES.equals(intent.getAction())) {
            navigateToProcesses(intent.getStringExtra(EXTRA_HIGHLIGHT_WORKER_ID));
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
            viewPager.setCurrentItem(BottomNavTab.LOGS.ordinal(), false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (MAIN_ACTIVITY_START_PROCESSES.equals(intent.getAction())) {
            navigateToProcesses(intent.getStringExtra(EXTRA_HIGHLIGHT_WORKER_ID));
        }
    }

    public void navigateToProcesses(String highlightWorkerId) {
        viewPager.setCurrentItem(BottomNavTab.PROCESSES.ordinal(), true);
        MainActivityNavKt.syncSelectedTab(BottomNavTab.PROCESSES);
        if (highlightWorkerId != null) {
            viewPager.post(() -> {
                Fragment page = getPageFragment(BottomNavTab.PROCESSES.ordinal());
                if (page instanceof ProcessesFragment) {
                    ((ProcessesFragment) page).highlightProcess(highlightWorkerId);
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentPage", viewPager.getCurrentItem());
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

    private void onTabReselected(BottomNavTab tab) {
        Fragment page = getPageFragment(tab.ordinal());
        if (page instanceof RemotesContainerFragment) {
            ((RemotesContainerFragment) page).resetToRemotesList();
        } else if (page instanceof SettingsContainerFragment) {
            ((SettingsContainerFragment) page).resetToSettingsList();
        }
    }

    private Fragment getPageFragment(int position) {
        return getSupportFragmentManager().findFragmentByTag("f" + position);
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

    // True when the back press was handled here
    private boolean handleBack() {
        int currentPage = viewPager.getCurrentItem();
        Fragment page = getPageFragment(currentPage);

        // Try delegating to the current page's container
        if (page instanceof RemotesContainerFragment) {
            if (((RemotesContainerFragment) page).onBackPressed()) return true;
        } else if (page instanceof SettingsContainerFragment) {
            if (((SettingsContainerFragment) page).onBackPressed()) return true;
        }

        // If not on Remotes, go to Remotes
        if (currentPage != BottomNavTab.REMOTES.ordinal()) {
            viewPager.setCurrentItem(BottomNavTab.REMOTES.ordinal(), true);
            return true;
        }

        return false;
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

    public void startRemotesFragment() {
        viewPager.setCurrentItem(BottomNavTab.REMOTES.ordinal(), false);
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
        android.widget.EditText editText = new android.widget.EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, 0, padding, 0);
        container.addView(editText);

        new MaterialAlertDialogBuilder(this, R.style.RoundedCornersDialog)
                .setTitle(R.string.config_password_protected)
                .setMessage(R.string.please_enter_password)
                .setView(container)
                .setCancelable(false)
                .setPositiveButton(R.string.okay_confirmation, (dialog, which) -> {
                    new DecryptConfig().execute(editText.getText().toString());
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    findViewById(R.id.locked_config).setVisibility(View.GONE);
                    restoreConfigBackup();
                    startRemotesFragment();
                })
                .show();
    }

    private void checkPendingBackupRestore(SharedPreferences prefs) {
        if (!prefs.getBoolean(EncryptedBackupAgent.PREF_PENDING_BACKUP_RESTORE, false)) {
            return;
        }
        File encFile = new File(getFilesDir(), "rclone.conf.enc");
        if (!encFile.exists()) {
            prefs.edit().remove(EncryptedBackupAgent.PREF_PENDING_BACKUP_RESTORE).apply();
            return;
        }
        String encryption = prefs.getString(getString(R.string.pref_key_cloud_backup_encryption), "none");
        boolean isAge = "age".equals(encryption);
        askForBackupRestoreKey(isAge);
    }

    private void askForBackupRestoreKey(boolean isAge) {
        android.widget.EditText editText = new android.widget.EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, 0, padding, 0);
        container.addView(editText);

        int promptRes = isAge ? R.string.backup_restore_age_prompt : R.string.backup_restore_password_prompt;
        new MaterialAlertDialogBuilder(this, R.style.RoundedCornersDialog)
                .setTitle(R.string.backup_restore_title)
                .setMessage(promptRes)
                .setView(container)
                .setCancelable(true)
                .setPositiveButton(R.string.okay_confirmation, (dialog, which) -> {
                    String key = editText.getText().toString();
                    new Thread(() -> {
                        try {
                            File encFile = new File(getFilesDir(), "rclone.conf.enc");
                            byte[] encrypted = java.nio.file.Files.readAllBytes(encFile.toPath());
                            byte[] decrypted;
                            if (isAge) {
                                decrypted = AgeEncryption.INSTANCE.decrypt(key, encrypted);
                            } else {
                                decrypted = BackupCrypto.INSTANCE.decrypt(key, encrypted);
                            }
                            File configFile = new File(getFilesDir(), "rclone.conf");
                            java.nio.file.Files.write(configFile.toPath(), decrypted);
                            encFile.delete();
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                            prefs.edit().remove(EncryptedBackupAgent.PREF_PENDING_BACKUP_RESTORE).apply();
                            runOnUiThread(() ->
                                AppShortcutsHelper.populateAppShortcuts(this, rclone.getRemotes())
                            );
                        } catch (Exception e) {
                            FLog.e(TAG, "Backup restore decryption failed", e);
                            runOnUiThread(() -> {
                                Toasty.error(this, getString(R.string.backup_restore_failed),
                                        Toast.LENGTH_LONG, true).show();
                                askForBackupRestoreKey(isAge);
                            });
                        }
                    }).start();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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
        startRemote(remote);
    }

    private void startRemote(RemoteItem remote) {
        // Switch to Remotes page and open the remote in the container
        viewPager.setCurrentItem(BottomNavTab.REMOTES.ordinal(), false);

        // Post to ensure the fragment is attached before opening
        viewPager.post(() -> {
            Fragment page = getPageFragment(BottomNavTab.REMOTES.ordinal());
            if (page instanceof RemotesContainerFragment) {
                ((RemotesContainerFragment) page).openRemote(remote);
            }
        });

        AppShortcutsHelper.reportAppShortcutUsage(this, remote.getName());
    }

    private android.app.Dialog createLoadingDialog(int titleResId) {
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        container.setPadding(padding, padding, padding, padding);
        container.addView(progressBar);

        return new MaterialAlertDialogBuilder(this, R.style.RoundedCornersDialog)
                .setTitle(titleResId)
                .setView(container)
                .setCancelable(false)
                .create();
    }

    private static void dismissLoadingDialog(android.app.Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
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
        private android.app.Dialog loadingDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            findViewById(R.id.locked_config).setVisibility(View.GONE);
            backupCurrentConfig();
            loadingDialog = createLoadingDialog(R.string.copying_rclone_config);
            loadingDialog.show();
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
            dismissLoadingDialog(loadingDialog);

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
        private android.app.Dialog loadingDialog;

        private boolean isPermissable(File file) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                return Environment.isExternalStorageLegacy(file);
            } else {
                return Environment.isExternalStorageManager(file);
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
            loadingDialog = createLoadingDialog(R.string.refreshing_local_alias_remotes);
            loadingDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... aVoid) {
            if (isCancelled()) {
                return null;
            }
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            Set<String> generated = pref.getStringSet(getString(R.string.pref_key_local_alias_remotes), new HashSet<>());
            Set<String> renamed = new HashSet<>(pref.getStringSet(getString(R.string.pref_key_renamed_remotes), new HashSet<>()));
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
            StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            StorageVolume storageVolume = storageManager.getStorageVolume(root);
            if (null != storageVolume) {
                name = storageVolume.getDescription(context);
                if (null != storageVolume.getUuid()) {
                    id = storageVolume.getUuid();
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
            Set<String> renamedRemotes = new HashSet<>(pref.getStringSet(getString(R.string.pref_key_renamed_remotes), new HashSet<>()));
            Set<String> drawerPinned = new HashSet<>(pref.getStringSet(getString(R.string.shared_preferences_drawer_pinned_remotes), new HashSet<>()));
            Set<String> topPinned = new HashSet<>(pref.getStringSet(getString(R.string.shared_preferences_pinned_remotes), new HashSet<>()));
            Set<String> generatedRemotes = new HashSet<>(pref.getStringSet(getString(R.string.pref_key_local_alias_remotes), new HashSet<>()));
            renamedRemotes.add(id);
            drawerPinned.add(id);
            topPinned.add(id);
            generatedRemotes.add(id);
            pref.edit()
                    .putStringSet(getString(R.string.pref_key_renamed_remotes), renamedRemotes)
                    .putString(getString(R.string.pref_key_renamed_remote_prefix, id), name)
                    .putStringSet(getString(R.string.shared_preferences_drawer_pinned_remotes), drawerPinned)
                    .putStringSet(getString(R.string.shared_preferences_pinned_remotes), topPinned)
                    .putStringSet(getString(R.string.pref_key_local_alias_remotes), generatedRemotes)
                    .apply();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            dismissLoadingDialog(loadingDialog);
            AppShortcutsHelper.removeAllAppShortcuts(context);
            AppShortcutsHelper.populateAppShortcuts(context, rclone.getRemotes());

            startRemotesFragment();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class DecryptConfig extends AsyncTask<String, Void, Boolean> {

        private android.app.Dialog loadingDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingDialog = createLoadingDialog(R.string.working);
            loadingDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            return rclone.decryptConfig(strings[0]);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            dismissLoadingDialog(loadingDialog);
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
