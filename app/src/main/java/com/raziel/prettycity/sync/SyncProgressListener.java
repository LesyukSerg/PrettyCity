package com.raziel.prettycity.sync;

public interface SyncProgressListener {
    void onSyncStarted(String direction); // "up" або "down"
    void onProgress(int current, int total);
    void onSyncCompleted();
}
