package com.example.ai;

import android.os.Bundle;

oneway interface IAiCallback {
    void onToken(String token);
    void onComplete(String fullResponse, in Bundle metadata);
    void onError(int errorCode, String errorMessage);
}
