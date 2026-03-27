package com.wifi.lib.mvvm;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public abstract class BaseViewModel extends AndroidViewModel {
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> messageEvent = new MutableLiveData<>();

    protected BaseViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public LiveData<Event<String>> getMessageEvent() {
        return messageEvent;
    }

    protected void showLoading() {
        loadingLiveData.setValue(true);
    }

    protected void hideLoading() {
        loadingLiveData.setValue(false);
    }

    protected void dispatchMessage(@NonNull String message) {
        messageEvent.setValue(new Event<>(message));
    }
}
