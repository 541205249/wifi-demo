package com.wifi.lib.mvvm;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;

public class EventObserver<T> implements Observer<Event<T>> {
    public interface OnEventUnhandledContent<T> {
        void onEventUnhandledContent(T content);
    }

    private final OnEventUnhandledContent<T> onEventUnhandledContent;

    public EventObserver(@NonNull OnEventUnhandledContent<T> onEventUnhandledContent) {
        this.onEventUnhandledContent = onEventUnhandledContent;
    }

    @Override
    public void onChanged(Event<T> event) {
        if (event == null) {
            return;
        }
        T content = event.getContentIfNotHandled();
        if (content != null) {
            onEventUnhandledContent.onEventUnhandledContent(content);
        }
    }
}
