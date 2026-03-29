package com.wifi.lib.baseui.delegate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class PermissionDelegate {
    public interface Callback {
        void granted();

        void denied(List<String> deniedList);

        default void cancel() {
        }
    }

    private final Context context;
    private boolean gotoSettingsWithRejected;
    private int requestCode;
    private Callback callback;
    private CharSequence title = "权限申请";
    private CharSequence content = "应用需要必要权限才能继续完成当前操作。";
    private CharSequence positiveText = "继续";
    private CharSequence negativeText = "取消";
    private Drawable icon;
    private AlertDialog currentDialog;

    public PermissionDelegate(@NonNull Context context) {
        this.context = context;
    }

    public boolean hasPermissions(@NonNull String[] permissions) {
        return !withoutPermissions(permissions);
    }

    public boolean withoutPermissions(@NonNull String[] permissions) {
        for (String permission : permissions) {
            if (withoutPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPermission(@NonNull String permission) {
        return !withoutPermission(permission);
    }

    public boolean withoutPermission(@NonNull String permission) {
        return ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED;
    }

    public PermissionDelegate setGotoSettingsWithRejected() {
        gotoSettingsWithRejected = true;
        return this;
    }

    public PermissionDelegate setHintTxt(@StringRes int titleResId, @StringRes int contentResId) {
        return setHintTxt(context.getString(titleResId), context.getString(contentResId));
    }

    public PermissionDelegate setHintTxt(@NonNull String title, @NonNull String content) {
        this.title = title;
        this.content = content;
        return this;
    }

    public PermissionDelegate setIcon(@DrawableRes int iconResId) {
        this.icon = ContextCompat.getDrawable(context, iconResId);
        return this;
    }

    public PermissionDelegate setIcon(Drawable icon) {
        this.icon = icon;
        return this;
    }

    public PermissionDelegate setBtnTxt(@StringRes int positiveResId, @StringRes int negativeResId) {
        return setBtnTxt(context.getString(positiveResId), context.getString(negativeResId));
    }

    public PermissionDelegate setBtnTxt(@NonNull String positiveText, @NonNull String negativeText) {
        this.positiveText = positiveText;
        this.negativeText = negativeText;
        return this;
    }

    public void requestPermissions(
            @NonNull Activity activity,
            @NonNull String[] permissions,
            int requestCode,
            @NonNull Callback callback
    ) {
        requestPermissions(activity, permissions, requestCode, false, callback);
    }

    public void requestPermissions(
            @NonNull Activity activity,
            @NonNull String[] permissions,
            int requestCode,
            boolean hideDialog,
            @NonNull Callback callback
    ) {
        requestPermissionsInternal(
                permissions,
                requestCode,
                hideDialog,
                callback,
                () -> ActivityCompat.requestPermissions(activity, permissions, requestCode)
        );
    }

    public void requestPermissions(
            @NonNull Fragment fragment,
            @NonNull String[] permissions,
            int requestCode,
            @NonNull Callback callback
    ) {
        requestPermissions(fragment, permissions, requestCode, false, callback);
    }

    public void requestPermissions(
            @NonNull Fragment fragment,
            @NonNull String[] permissions,
            int requestCode,
            boolean hideDialog,
            @NonNull Callback callback
    ) {
        requestPermissionsInternal(
                permissions,
                requestCode,
                hideDialog,
                callback,
                () -> fragment.requestPermissions(permissions, requestCode)
        );
    }

    private void requestPermissionsInternal(
            @NonNull String[] permissions,
            int requestCode,
            boolean hideDialog,
            @NonNull Callback callback,
            @NonNull Runnable requestAction
    ) {
        this.callback = callback;
        this.requestCode = requestCode;
        if (hasPermissions(permissions)) {
            callback.granted();
            return;
        }
        requestInternal(hideDialog, requestAction);
    }

    public void onResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != this.requestCode || callback == null) {
            return;
        }
        dismissDialog();

        List<String> deniedList = collectDeniedPermissions(permissions, grantResults);
        if (deniedList.isEmpty()) {
            callback.granted();
            return;
        }

        callback.denied(deniedList);
        if (gotoSettingsWithRejected) {
            gotoPermissionSettings();
        }
    }

    public void gotoPermissionSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @NonNull
    private List<String> collectDeniedPermissions(@NonNull String[] permissions, @NonNull int[] grantResults) {
        List<String> deniedList = new ArrayList<>();
        for (int index = 0; index < grantResults.length; index++) {
            if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                deniedList.add(permissions[index]);
            }
        }
        return deniedList;
    }

    private void requestInternal(boolean hideDialog, @NonNull Runnable requestAction) {
        if (hideDialog) {
            requestAction.run();
            return;
        }
        dismissDialog();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(content)
                .setCancelable(false)
                .setPositiveButton(positiveText, (dialog, which) -> requestAction.run())
                .setNegativeButton(negativeText, (dialog, which) -> {
                    if (callback != null) {
                        callback.cancel();
                    }
                });
        if (icon != null) {
            builder.setIcon(icon);
        }
        currentDialog = builder.show();
    }

    private void dismissDialog() {
        if (currentDialog != null) {
            currentDialog.dismiss();
            currentDialog = null;
        }
    }
}
