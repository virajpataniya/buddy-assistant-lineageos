package com.buddy.assistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import java.util.ArrayList;

public class AlwaysOnService extends Service implements RecognitionListener {
    private static final String TAG = "AlwaysOnService";
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Log.d(TAG, "AlwaysOnService onCreate");
            createNotificationChannel();
            initializeSpeechRecognizer();
            startContinuousListening();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "AlwaysOnService onStartCommand");
        try {
            createNotificationChannel();
            startForeground(1, createNotification());
            initializeSpeechRecognizer();
            startContinuousListening();
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand: " + e.getMessage());
        }
        return START_STICKY; // Restart if killed
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                    "buddy_assistant_channel",
                    "Buddy Assistant Always-On",
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Always-on voice detection for Buddy Assistant");
                
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel: " + e.getMessage());
            }
        }
    }
    
    private Notification createNotification() {
        try {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
            );
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return new Notification.Builder(this, "buddy_assistant_channel")
                    .setContentTitle("Buddy Assistant")
                    .setContentText("Always-on listening active - Say 'Hey Buddy'")
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();
            } else {
                return new Notification.Builder(this)
                    .setContentTitle("Buddy Assistant")
                    .setContentText("Always-on listening active - Say 'Hey Buddy'")
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification: " + e.getMessage());
            // Return a simple notification as fallback
            return new Notification.Builder(this)
                .setContentTitle("Buddy Assistant")
                .setContentText("Always-on listening active")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build();
        }
    }
    
    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(this);
        }
    }
    
    private void startContinuousListening() {
        if (speechRecognizer != null) {
            isListening = false; // Reset listening state
            
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            
            speechRecognizer.startListening(intent);
            isListening = true;
            Log.d(TAG, "Started continuous listening...");
        }
    }
    
    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String command = matches.get(0).toLowerCase();
            
            // Check for "hey buddy" wake word
            if (command.contains("hey buddy")) {
                Log.d(TAG, "Wake word detected: " + command);
                // Process the command after wake word
                processCommand(command);
            }
        }
        
        // Restart listening for continuous detection
        startContinuousListening();
    }
    
    private void processCommand(String command) {
        // Remove "hey buddy" and process the actual command
        String actualCommand = command.replace("hey buddy", "").trim();
        
        // Send command to MainActivity for processing using the same action
        Intent intent = new Intent("com.buddy.assistant.VOICE_COMMAND");
        intent.putExtra("command", actualCommand);
        sendBroadcast(intent);
        
        Log.d(TAG, "Sent command: " + actualCommand);
    }
    
    @Override
    public void onError(int error) {
        Log.e(TAG, "Speech recognition error: " + error);
        // Restart listening after error
        startContinuousListening();
    }
    
    // Other RecognitionListener methods...
    @Override public void onReadyForSpeech(Bundle params) {}
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float rmsdB) {}
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEndOfSpeech() {}
    @Override public void onPartialResults(Bundle partialResults) {}
    @Override public void onEvent(int eventType, Bundle params) {}
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
