package com.buddy.assistant;

import android.content.Context;
import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceRecognitionHelper {
    
    private static final String TAG = "VoiceRecognitionHelper";
    private SpeechRecognizer mSpeechRecognizer;
    private VoiceRecognitionCallback mCallback;
    private Context mContext;
    
    public interface VoiceRecognitionCallback {
        void onVoiceResult(String spokenText);
        void onVoiceError(String error);
        void onVoiceStart();
        void onVoiceEnd();
    }
    
    public VoiceRecognitionHelper(Context context) {
        mContext = context;
    }
    
    public void setCallback(VoiceRecognitionCallback callback) {
        mCallback = callback;
    }
    
    public boolean isAvailable() {
        return SpeechRecognizer.isRecognitionAvailable(mContext);
    }
    
    public void startListening() {
        if (!isAvailable()) {
            if (mCallback != null) {
                mCallback.onVoiceError("Speech recognition not available on this device");
            }
            return;
        }
        
        if (mSpeechRecognizer == null) {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
            mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "Ready for speech");
                    if (mCallback != null) {
                        mCallback.onVoiceStart();
                    }
                }
                
                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "Beginning of speech");
                }
                
                @Override
                public void onRmsChanged(float rmsdB) {
                    // Volume level changed
                }
                
                @Override
                public void onBufferReceived(byte[] buffer) {
                    // Audio buffer received
                }
                
                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "End of speech");
                    if (mCallback != null) {
                        mCallback.onVoiceEnd();
                    }
                }
                
                @Override
                public void onError(int error) {
                    String errorMessage = getErrorMessage(error);
                    Log.e(TAG, "Speech recognition error: " + errorMessage);
                    if (mCallback != null) {
                        mCallback.onVoiceError(errorMessage);
                    }
                }
                
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        Log.d(TAG, "Speech result: " + spokenText);
                        if (mCallback != null) {
                            mCallback.onVoiceResult(spokenText);
                        }
                    } else {
                        if (mCallback != null) {
                            mCallback.onVoiceError("No speech recognized");
                        }
                    }
                }
                
                @Override
                public void onPartialResults(Bundle partialResults) {
                    // Partial results
                }
                
                @Override
                public void onEvent(int eventType, Bundle params) {
                    // Additional events
                }
            });
        }
        
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your command to Buddy Assistant...");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        
        mSpeechRecognizer.startListening(intent);
    }
    
    public void stopListening() {
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.stopListening();
        }
    }
    
    public void destroy() {
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
        }
    }
    
    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech input recognized";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error";
        }
    }
}
