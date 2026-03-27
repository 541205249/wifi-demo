package com.wifi.lib.command;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.command.profile.CommandProfile;
import com.wifi.lib.log.DLog;
import com.wifi.lib.mvvm.BaseRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandSettingsRepository extends BaseRepository {
    private static final String TAG = "CommandSettingsRepo";
    private static final String PREF_NAME_PREFIX = "wifi_lib_command_settings";
    private static final String KEY_LAST_URI = "last_uri";

    private static final Map<String, CommandSettingsRepository> INSTANCES = new ConcurrentHashMap<>();

    private final Context appContext;
    private final SharedPreferences preferences;
    private final CommandProfile commandProfile;
    private final CommandCatalog catalog;
    private final CommandEngine commandEngine = new CommandEngine();
    private final CommandTableLoader commandTableLoader = new CommandTableLoader();

    @NonNull
    private CommandTable currentTable = CommandTable.empty();
    @Nullable
    private CommandCatalog.ValidationResult lastValidationResult;
    @Nullable
    private Uri lastLoadedUri;

    private CommandSettingsRepository(@NonNull Context context, @NonNull CommandProfile commandProfile) {
        this.appContext = context.getApplicationContext();
        this.commandProfile = commandProfile;
        this.catalog = commandProfile.getCatalog();
        this.preferences = appContext.getSharedPreferences(
                PREF_NAME_PREFIX + "_" + commandProfile.getProfileId(),
                Context.MODE_PRIVATE
        );
        restoreLastUri();
    }

    @NonNull
    public static CommandSettingsRepository getInstance(
            @NonNull Context context,
            @NonNull CommandProfile commandProfile
    ) {
        String instanceKey = context.getPackageName() + "#" + commandProfile.getProfileId();
        CommandSettingsRepository repository = INSTANCES.get(instanceKey);
        if (repository != null) {
            return repository;
        }
        synchronized (INSTANCES) {
            repository = INSTANCES.get(instanceKey);
            if (repository == null) {
                repository = new CommandSettingsRepository(context, commandProfile);
                INSTANCES.put(instanceKey, repository);
            }
        }
        return repository;
    }

    @NonNull
    public CommandCatalog getCatalog() {
        return catalog;
    }

    @NonNull
    public CommandEngine getCommandEngine() {
        return commandEngine;
    }

    @NonNull
    public Snapshot snapshot() {
        return new Snapshot(currentTable, lastValidationResult, lastLoadedUri);
    }

    @Nullable
    public Uri getLastLoadedUri() {
        return lastLoadedUri;
    }

    @NonNull
    public LoadResult loadFromUri(@NonNull Uri uri) throws IOException {
        CommandTable table = commandTableLoader.loadFromUri(appContext, uri);
        CommandCatalog.ValidationResult validationResult = catalog.validate(table);
        commandEngine.replaceCommandTable(table);
        currentTable = table;
        lastValidationResult = validationResult;
        lastLoadedUri = uri;
        preferences.edit().putString(KEY_LAST_URI, uri.toString()).apply();
        DLog.i(TAG, "命令编码表加载成功，uri=" + uri + ", count=" + table.size());
        return new LoadResult(table, validationResult, uri, uri.toString(), false);
    }

    @Nullable
    public LoadResult reloadLast() throws IOException {
        if (lastLoadedUri == null) {
            return null;
        }
        return loadFromUri(lastLoadedUri);
    }

    @NonNull
    public LoadResult loadBuiltInSample() throws IOException {
        try (InputStream inputStream = appContext.getResources().openRawResource(commandProfile.getBuiltInTableResId())) {
            CommandTable table = commandTableLoader.load(inputStream, commandProfile.getBuiltInSourceLabel());
            CommandCatalog.ValidationResult validationResult = catalog.validate(table);
            commandEngine.replaceCommandTable(table);
            currentTable = table;
            lastValidationResult = validationResult;
            DLog.i(TAG, "已加载内置示例编码表，profile=" + commandProfile.getProfileId() + ", count=" + table.size());
            return new LoadResult(table, validationResult, null, commandProfile.getBuiltInSourceLabel(), true);
        }
    }

    private void restoreLastUri() {
        String rawUri = preferences.getString(KEY_LAST_URI, "");
        if (TextUtils.isEmpty(rawUri)) {
            return;
        }
        try {
            lastLoadedUri = Uri.parse(rawUri);
        } catch (Exception exception) {
            DLog.w(TAG, "恢复上次编码表 Uri 失败", exception);
            lastLoadedUri = null;
        }
    }

    public static final class Snapshot {
        @NonNull
        private final CommandTable commandTable;
        @Nullable
        private final CommandCatalog.ValidationResult validationResult;
        @Nullable
        private final Uri sourceUri;

        private Snapshot(
                @NonNull CommandTable commandTable,
                @Nullable CommandCatalog.ValidationResult validationResult,
                @Nullable Uri sourceUri
        ) {
            this.commandTable = commandTable;
            this.validationResult = validationResult;
            this.sourceUri = sourceUri;
        }

        @NonNull
        public CommandTable getCommandTable() {
            return commandTable;
        }

        @Nullable
        public CommandCatalog.ValidationResult getValidationResult() {
            return validationResult;
        }

        @Nullable
        public Uri getSourceUri() {
            return sourceUri;
        }
    }

    public static final class LoadResult {
        @NonNull
        private final CommandTable commandTable;
        @NonNull
        private final CommandCatalog.ValidationResult validationResult;
        @Nullable
        private final Uri sourceUri;
        @NonNull
        private final String sourceLabel;
        private final boolean builtInSample;

        private LoadResult(
                @NonNull CommandTable commandTable,
                @NonNull CommandCatalog.ValidationResult validationResult,
                @Nullable Uri sourceUri,
                @NonNull String sourceLabel,
                boolean builtInSample
        ) {
            this.commandTable = commandTable;
            this.validationResult = validationResult;
            this.sourceUri = sourceUri;
            this.sourceLabel = sourceLabel;
            this.builtInSample = builtInSample;
        }

        @NonNull
        public CommandTable getCommandTable() {
            return commandTable;
        }

        @NonNull
        public CommandCatalog.ValidationResult getValidationResult() {
            return validationResult;
        }

        @Nullable
        public Uri getSourceUri() {
            return sourceUri;
        }

        @NonNull
        public String getSourceLabel() {
            return sourceLabel;
        }

        public boolean isBuiltInSample() {
            return builtInSample;
        }
    }
}
