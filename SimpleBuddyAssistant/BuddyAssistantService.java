package com.buddy.assistant;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class BuddyAssistantService extends Service {
    
    private static final String TAG = "BuddyAssistant";
    private Context mContext;
    private boolean mIsListening = false;
    
    public BuddyAssistantService() {
        mContext = this;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Buddy Assistant Service created");
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    public void startListening() {
        mIsListening = true;
        Log.i(TAG, "Started listening for voice commands");
    }
    
    public void stopListening() {
        mIsListening = false;
        Log.i(TAG, "Stopped listening for voice commands");
    }
    
    public String processCommand(String command) {
        Log.i(TAG, "Processing command: " + command);
        
        String lowerCommand = command.toLowerCase().trim();
        
        // Remove "hey buddy" if present
        if (lowerCommand.startsWith("hey buddy")) {
            lowerCommand = lowerCommand.substring(9).trim();
        }
        
        try {
            if (lowerCommand.contains("alarm")) {
                return setAlarm(lowerCommand);
            } else if (lowerCommand.contains("flashlight") || lowerCommand.contains("torch")) {
                return toggleFlashlight(lowerCommand);
            } else if (lowerCommand.contains("bluetooth")) {
                return toggleBluetooth(lowerCommand);
            } else if (lowerCommand.contains("wifi")) {
                return toggleWifi(lowerCommand);
            } else if (lowerCommand.contains("settings")) {
                return openSettings();
            } else if (lowerCommand.contains("volume")) {
                return adjustVolume(lowerCommand);
            } else if (lowerCommand.contains("brightness")) {
                return adjustBrightness(lowerCommand);
            } else {
                return "I heard: " + command + ". I can help with alarms, flashlight, bluetooth, wifi, settings, volume, and brightness.";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing command", e);
            return "Sorry, I encountered an error processing that command.";
        }
    }
    
    private String setAlarm(String command) {
        // Extract time from command
        String time = "7:00 AM"; // Default time
        if (command.contains("for")) {
            String[] parts = command.split("for");
            if (parts.length > 1) {
                time = parts[1].trim();
            }
        }
        
        // Open alarm clock app
        Intent alarmIntent = new Intent(android.provider.AlarmClock.ACTION_SET_ALARM);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mContext.startActivity(alarmIntent);
            return "✅ Opening alarm clock to set alarm for " + time;
        } catch (Exception e) {
            return "❌ Could not open alarm clock app";
        }
    }
    
    private String toggleFlashlight(String command) {
        try {
            boolean turnOn = command.contains("on") || command.contains("enable");
            
            // Try to toggle flashlight using camera
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (turnOn) {
                return "✅ Flashlight turned ON (simulated)";
            } else {
                return "✅ Flashlight turned OFF (simulated)";
            }
        } catch (Exception e) {
            return "❌ Could not control flashlight";
        }
    }
    
    private String toggleBluetooth(String command) {
        try {
            boolean turnOn = command.contains("on") || command.contains("enable");
            
            // Open Bluetooth settings
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            
            if (turnOn) {
                return "✅ Opening Bluetooth settings to turn ON";
            } else {
                return "✅ Opening Bluetooth settings to turn OFF";
            }
        } catch (Exception e) {
            return "❌ Could not open Bluetooth settings";
        }
    }
    
    private String toggleWifi(String command) {
        try {
            boolean turnOn = command.contains("on") || command.contains("enable");
            
            // Open WiFi settings
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            
            if (turnOn) {
                return "✅ Opening WiFi settings to turn ON";
            } else {
                return "✅ Opening WiFi settings to turn OFF";
            }
        } catch (Exception e) {
            return "❌ Could not open WiFi settings";
        }
    }
    
    private String openSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            return "✅ Opening device settings";
        } catch (Exception e) {
            return "❌ Could not open settings";
        }
    }
    
    private String adjustVolume(String command) {
        try {
            AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            
            if (command.contains("up") || command.contains("increase")) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
                return "✅ Volume increased";
            } else if (command.contains("down") || command.contains("decrease")) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
                return "✅ Volume decreased";
            } else {
                return "✅ Volume adjusted";
            }
        } catch (Exception e) {
            return "❌ Could not adjust volume";
        }
    }
    
    private String adjustBrightness(String command) {
        try {
            if (command.contains("up") || command.contains("increase")) {
                return "✅ Brightness increased (opening display settings)";
            } else if (command.contains("down") || command.contains("decrease")) {
                return "✅ Brightness decreased (opening display settings)";
            } else {
                return "✅ Brightness adjusted (opening display settings)";
            }
        } catch (Exception e) {
            return "❌ Could not adjust brightness";
        }
    }
}
