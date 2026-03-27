package com.wifi.lib.command;

import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.utils.Toasty;

public final class CommandViewHelper {
    private CommandViewHelper() {
    }

    public static void attachLongPressCodeHint(@NonNull View view, @NonNull CommandReservation reservation) {
        attachLongPressCodeHint(view, reservation.getCodeValue(), reservation.getCodeExplanation());
    }

    public static void attachLongPressCodeHint(
            @NonNull View view,
            @NonNull String code,
            @Nullable String codeExplanation
    ) {
        view.setOnLongClickListener(v -> {
            StringBuilder builder = new StringBuilder();
            builder.append("编码：").append(CommandCode.of(code).getValue());
            if (!TextUtils.isEmpty(codeExplanation)) {
                builder.append("\n解释：").append(codeExplanation.trim());
            }
            Toasty.showLong(builder.toString());
            return true;
        });
    }

    public static void bindClickWithCodeHint(
            @NonNull View view,
            @NonNull CommandReservation reservation,
            @NonNull View.OnClickListener clickListener
    ) {
        bindClickWithCodeHint(view, reservation.getCodeValue(), reservation.getCodeExplanation(), clickListener);
    }

    public static void bindClickWithCodeHint(
            @NonNull View view,
            @NonNull String code,
            @Nullable String codeExplanation,
            @NonNull View.OnClickListener clickListener
    ) {
        view.setOnClickListener(clickListener);
        attachLongPressCodeHint(view, code, codeExplanation);
    }
}
