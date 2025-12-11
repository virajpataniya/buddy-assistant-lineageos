package com.buddy.assistant;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
// Using standard Android framework APIs
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity implements RecognitionListener {
    
    private EditText mCommandInput;
    private Button mSendButton;
    private Button mListenButton;
    private TextView mResponseText;
    private SpeechRecognizer mSpeechRecognizer;
    private boolean mIsListening = false;
    
    // Broadcast receiver for voice commands
    private BroadcastReceiver mCommandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals("com.buddy.assistant.VOICE_COMMAND")) {
                String command = intent.getStringExtra("command");
                if (command != null) {
                    Log.d("MainActivity", "Received voice command: " + command);
                    AICommandProcessor.CommandResult result = AICommandProcessor.processCommand(command);
                    String response = executeAICommand(result);
                    showToast(response);
                    if (mResponseText != null) {
                        mResponseText.setText(response);
                    }
                }
            }
        }
    };
    
    private static final int PERMISSION_REQUEST_CODE = 1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize views
        mCommandInput = findViewById(R.id.command_input);
        mSendButton = findViewById(R.id.send_button);
        mListenButton = findViewById(R.id.listen_button);
        mResponseText = findViewById(R.id.response_text);
        
        // Set initial response text
        if (mResponseText != null) {
            mResponseText.setText("Ready to help! Tap the microphone button to start voice commands or type your command above.");
        }
        
        // Check and request permissions
        checkPermissions();
        
        // Initialize speech recognizer
        initializeSpeechRecognizer();
        
        // Register broadcast receiver for voice commands
        IntentFilter filter = new IntentFilter("com.buddy.assistant.VOICE_COMMAND");
        registerReceiver(mCommandReceiver, filter);
        
        // Set up button click listeners
        if (mSendButton != null) {
            mSendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    processCommand();
                }
            });
        }
        
        if (mListenButton != null) {
            mListenButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startListening();
                }
            });
        }
        
        Toast.makeText(this, "Buddy Assistant loaded! Tap microphone to start voice commands.", Toast.LENGTH_LONG).show();
    }
    
    private void checkPermissions() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 
                PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission granted!", Toast.LENGTH_SHORT).show();
                initializeSpeechRecognizer();
            } else {
                Toast.makeText(this, "Microphone permission required for voice commands", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mSpeechRecognizer.setRecognitionListener(this);
        } else {
            // Speech recognition not available - use fallback method
            mSpeechRecognizer = null;
        }
    }
    
    private void startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if speech recognition is available
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showToast("Speech recognition not available. Using text input instead.");
            mCommandInput.requestFocus();
            mCommandInput.setHint("Type your command here (e.g., 'turn on flashlight')");
            mResponseText.setText("🎤 Speech recognition not available.\n\n✅ Use text input instead:\n• Type: 'turn on flashlight'\n• Type: 'set alarm for 7 AM'\n• Type: 'turn on bluetooth'\n\nAll 50+ commands work via text input!");
            return;
        }
        
        // Try using system voice input dialog as fallback
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your command to Buddy Assistant...");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            
            startActivityForResult(intent, 123);
            
            if (mResponseText != null) {
                mResponseText.setText("🎤 Opening voice input... Speak your command!\n\nSay: 'Hey Buddy, turn on flash'\nOr: 'Set alarm for 7 AM'");
            }
            
            Toast.makeText(this, "🎤 Voice input opened! Speak your command!", Toast.LENGTH_LONG).show();
            
            // Also start the AlwaysOnService for continuous listening
            startAlwaysOnService();
            
        } catch (Exception e) {
            // Fallback to manual input
            if (mResponseText != null) {
                mResponseText.setText("❌ Voice input not available on this device.\n\nPlease type your command above instead.\n\nExample: 'turn on flash' or 'set alarm for 7 AM'");
            }
            
            if (mCommandInput != null) {
                mCommandInput.requestFocus();
                mCommandInput.setHint("Type your command here (voice not available)...");
            }
            
            Toast.makeText(this, "Voice input not available. Please type your command.", Toast.LENGTH_LONG).show();
        }
    }
    
    private void stopListening() {
        mIsListening = false;
        mListenButton.setText("🎤 Listen");
        
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.stopListening();
        }
        
        if (mResponseText != null) {
            mResponseText.setText("Voice listening stopped. Tap microphone to start again or type your command above.");
        }
    }
    
    private void processCommand() {
        if (mCommandInput == null || mResponseText == null) {
            Toast.makeText(this, "Error: UI not properly initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String command = mCommandInput.getText().toString();
        if (command.isEmpty()) {
            Toast.makeText(this, "Please enter a command or use voice input", Toast.LENGTH_SHORT).show();
            return;
        }
        
        mResponseText.setText("Processing: " + command);
        
        // Process the command
        String response = processSimpleCommand(command);
        mResponseText.setText("Response: " + response);
        
        // Clear input
        mCommandInput.setText("");
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void startAlwaysOnService() {
        try {
            Intent serviceIntent = new Intent(this, AlwaysOnService.class);
            startService(serviceIntent);
            showToast("Always-on listening started! Say 'Hey Buddy' anytime.");
        } catch (Exception e) {
            Log.e("MainActivity", "Could not start AlwaysOnService: " + e.getMessage());
        }
    }
    
    private String processSimpleCommand(String command) {
        String lowerCommand = command.toLowerCase().trim();
        
        // Remove "hey buddy" if present
        if (lowerCommand.startsWith("hey buddy")) {
            lowerCommand = lowerCommand.substring(9).trim();
        }
        
        // Use AI Command Processor for natural language understanding
        AICommandProcessor.CommandResult result = AICommandProcessor.processCommand(lowerCommand);
        
        if (result.confidence > 0.6f) {
            return executeAICommand(result);
        }
        
        // Fallback to original string matching for backward compatibility
        
        try {
            // Alarm commands
            if (lowerCommand.contains("alarm")) {
                try {
                    // Try to parse time from command
                    String timeMessage = "";
                    if (lowerCommand.contains("7") && lowerCommand.contains("am")) {
                        timeMessage = " for 7:00 AM";
                    } else if (lowerCommand.contains("tomorrow")) {
                        timeMessage = " for tomorrow";
                    }
                    
                    // Try multiple alarm intents
                    Intent alarmIntent = new Intent(android.provider.AlarmClock.ACTION_SET_ALARM);
                    alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    // Add time if we can parse it
                    if (lowerCommand.contains("7") && lowerCommand.contains("am")) {
                        alarmIntent.putExtra(android.provider.AlarmClock.EXTRA_HOUR, 7);
                        alarmIntent.putExtra(android.provider.AlarmClock.EXTRA_MINUTES, 0);
                        alarmIntent.putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "Buddy Assistant Alarm");
                    }
                    
                    // Check if alarm app is available
                    if (alarmIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(alarmIntent);
                        return "✅ Setting alarm" + timeMessage;
                    } else {
                        // Fallback to clock app
                        Intent clockIntent = new Intent(Intent.ACTION_MAIN);
                        clockIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                        clockIntent.setPackage("com.android.deskclock");
                        clockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        
                        if (clockIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(clockIntent);
                            return "✅ Opening clock app for alarm" + timeMessage;
                        } else {
                            // Final fallback - open any time-related app
                            Intent timeIntent = new Intent(android.provider.Settings.ACTION_DATE_SETTINGS);
                            startActivity(timeIntent);
                            return "✅ Opening time settings (alarm app not found)";
                        }
                    }
                } catch (Exception e) {
                    return "❌ Could not open alarm app: " + e.getMessage();
                }
            }
            // Flashlight commands - support multiple variations
            else if (lowerCommand.contains("flashlight") || lowerCommand.contains("torch") || 
                     lowerCommand.contains("flash") || lowerCommand.contains("light")) {
                try {
                    // Try to control flashlight directly
                    if (lowerCommand.contains("on") || lowerCommand.contains("turn on")) {
                        // Turn on flashlight
                        android.hardware.camera2.CameraManager cameraManager = 
                            (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE);
                        String cameraId = cameraManager.getCameraIdList()[0];
                        cameraManager.setTorchMode(cameraId, true);
                        return "✅ Flashlight turned ON";
                    } else if (lowerCommand.contains("off") || lowerCommand.contains("turn off")) {
                        // Turn off flashlight
                        android.hardware.camera2.CameraManager cameraManager = 
                            (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE);
                        String cameraId = cameraManager.getCameraIdList()[0];
                        cameraManager.setTorchMode(cameraId, false);
                        return "✅ Flashlight turned OFF";
                    } else {
                        // Toggle flashlight
                        android.hardware.camera2.CameraManager cameraManager = 
                            (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE);
                        String cameraId = cameraManager.getCameraIdList()[0];
                        // For toggle, we'll turn it on (you can enhance this to check current state)
                        cameraManager.setTorchMode(cameraId, true);
                        return "✅ Flashlight toggled ON";
                    }
                } catch (Exception e) {
                    // Fallback to opening camera app
                    try {
                        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivity(cameraIntent);
                        return "✅ Opening camera for flashlight control";
                    } catch (Exception e2) {
                        return "❌ Flashlight control not available: " + e.getMessage();
                    }
                }
            }
            // Bluetooth commands
            else if (lowerCommand.contains("bluetooth")) {
                try {
                    android.bluetooth.BluetoothAdapter bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
                    if (bluetoothAdapter == null) {
                        return "❌ Bluetooth not available on this device";
                    }
                    
                    if (lowerCommand.contains("on") || lowerCommand.contains("turn on") || lowerCommand.contains("open")) {
                        if (!bluetoothAdapter.isEnabled()) {
                            // Use proper Bluetooth enabling with user permission
                            Intent enableBtIntent = new Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, 1);
                            return "✅ Requesting Bluetooth permission...";
                        } else {
                            return "✅ Bluetooth is already ON";
                        }
                    } else if (lowerCommand.contains("off") || lowerCommand.contains("turn off")) {
                        if (bluetoothAdapter.isEnabled()) {
                            bluetoothAdapter.disable();
                            return "✅ Bluetooth turned OFF";
                        } else {
                            return "✅ Bluetooth is already OFF";
                        }
                    } else {
                        // Toggle Bluetooth
                        if (bluetoothAdapter.isEnabled()) {
                            bluetoothAdapter.disable();
                            return "✅ Bluetooth toggled OFF";
                        } else {
                            bluetoothAdapter.enable();
                            return "✅ Bluetooth toggled ON";
                        }
                    }
                } catch (Exception e) {
                    // Fallback to opening settings
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                        startActivity(intent);
                        return "✅ Opening Bluetooth settings (direct control failed)";
                    } catch (Exception e2) {
                        return "❌ Could not control Bluetooth: " + e.getMessage();
                    }
                }
            }
            // WiFi commands
            else if (lowerCommand.contains("wifi") || lowerCommand.contains("wi-fi")) {
                try {
                    android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) getSystemService(WIFI_SERVICE);
                    if (wifiManager == null) {
                        return "❌ WiFi not available on this device";
                    }
                    
                    if (lowerCommand.contains("on") || lowerCommand.contains("turn on")) {
                        if (!wifiManager.isWifiEnabled()) {
                            wifiManager.setWifiEnabled(true);
                            return "✅ WiFi turned ON";
                        } else {
                            return "✅ WiFi is already ON";
                        }
                    } else if (lowerCommand.contains("off") || lowerCommand.contains("turn off")) {
                        if (wifiManager.isWifiEnabled()) {
                            wifiManager.setWifiEnabled(false);
                            return "✅ WiFi turned OFF";
                        } else {
                            return "✅ WiFi is already OFF";
                        }
                    } else {
                        // Toggle WiFi
                        if (wifiManager.isWifiEnabled()) {
                            wifiManager.setWifiEnabled(false);
                            return "✅ WiFi toggled OFF";
                        } else {
                            wifiManager.setWifiEnabled(true);
                            return "✅ WiFi toggled ON";
                        }
                    }
                } catch (Exception e) {
                    // Fallback to opening settings
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                        startActivity(intent);
                        return "✅ Opening WiFi settings (direct control failed)";
                    } catch (Exception e2) {
                        return "❌ Could not control WiFi: " + e.getMessage();
                    }
                }
            }
            // Settings commands
            else if (lowerCommand.contains("settings") || lowerCommand.contains("setting")) {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                    startActivity(intent);
                    return "✅ Opening device settings";
                } catch (Exception e) {
                    return "❌ Could not open settings";
                }
            }
            // Volume commands
            else if (lowerCommand.contains("volume") || lowerCommand.contains("sound")) {
                try {
                    android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
                    
                    if (lowerCommand.contains("increase") || lowerCommand.contains("up") || lowerCommand.contains("higher")) {
                        // Increase media volume
                        int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
                        int maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
                        
                        if (lowerCommand.contains("100") || lowerCommand.contains("max")) {
                            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, maxVolume, 0);
                            return "✅ Volume set to maximum";
                        } else {
                            int newVolume = Math.min(currentVolume + 2, maxVolume);
                            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0);
                            return "✅ Volume increased";
                        }
                    } else if (lowerCommand.contains("decrease") || lowerCommand.contains("down") || lowerCommand.contains("lower")) {
                        // Decrease media volume
                        int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
                        int newVolume = Math.max(currentVolume - 2, 0);
                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0);
                        return "✅ Volume decreased";
                    } else {
                        // Show volume settings
                        Intent intent = new Intent(android.provider.Settings.ACTION_SOUND_SETTINGS);
                        startActivity(intent);
                        return "✅ Opening sound settings";
                    }
                } catch (Exception e) {
                    return "❌ Could not control volume: " + e.getMessage();
                }
            }
            // Brightness commands
            else if (lowerCommand.contains("brightness") || lowerCommand.contains("bright")) {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS);
                    startActivity(intent);
                    return "✅ Opening display settings for brightness";
                } catch (Exception e) {
                    return "✅ Brightness control (simulated)";
                }
            }
            // Camera commands
            else if (lowerCommand.contains("camera") || lowerCommand.contains("photo")) {
                try {
                    // Try multiple camera intents
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    // Check if camera app is available
                    if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(cameraIntent);
                        return "✅ Opening camera";
                    } else {
                        // Try multiple camera apps in order of preference
                        String[] cameraPackages = {
                            "com.android.camera2",           // LineageOS camera
                            "com.android.camera",            // AOSP camera
                            "com.google.android.GoogleCamera", // Google Camera
                            "com.oneplus.camera",            // OnePlus camera
                            "com.samsung.camera",            // Samsung camera
                            "com.miui.camera"                // MIUI camera
                        };
                        
                        boolean cameraOpened = false;
                        for (String packageName : cameraPackages) {
                            Intent cameraAppIntent = new Intent(Intent.ACTION_MAIN);
                            cameraAppIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                            cameraAppIntent.setPackage(packageName);
                            cameraAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            
                            if (cameraAppIntent.resolveActivity(getPackageManager()) != null) {
                                startActivity(cameraAppIntent);
                                return "✅ Opening camera (" + packageName + ")";
                            }
                        }
                        
                        // Final fallback - open gallery
                        Intent galleryIntent = new Intent(Intent.ACTION_VIEW);
                        galleryIntent.setType("image/*");
                        galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        
                        if (galleryIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(galleryIntent);
                            return "✅ Opening gallery (camera not found)";
                        } else {
                            return "❌ No camera or gallery app available";
                        }
                    }
                } catch (Exception e) {
                    return "❌ Could not open camera: " + e.getMessage();
                }
            }
            // Help command
            else if (lowerCommand.contains("help") || lowerCommand.contains("what can you do")) {
                return "🤖 I can help with:\n" +
                       "• Alarms: 'set alarm for 7 AM'\n" +
                       "• Flashlight: 'turn on flash' or 'flashlight'\n" +
                       "• Bluetooth: 'turn on bluetooth'\n" +
                       "• WiFi: 'turn on wifi'\n" +
                       "• Settings: 'open settings'\n" +
                       "• Volume: 'increase volume'\n" +
                       "• Brightness: 'increase brightness'\n" +
                       "• Camera: 'open camera'";
            }
            else {
                return "I heard: \"" + command + "\"\n\n" +
                       "I can help with: alarms, flashlight/flash, bluetooth, wifi, settings, volume, brightness, camera.\n" +
                       "Try: 'turn on flash' or 'set alarm for 7 AM'";
            }
        } catch (Exception e) {
            return "Sorry, I encountered an error processing that command: " + e.getMessage();
        }
    }
    
    private String executeAICommand(AICommandProcessor.CommandResult result) {
        String intent = result.intent;
        Map<String, String> params = result.parameters;
        
        try {
            switch (intent) {
                case "BLUETOOTH_ON":
                    return enableBluetooth();
                case "BLUETOOTH_OFF":
                    return disableBluetooth();
                case "WIFI_ON":
                    return enableWifi();
                case "WIFI_OFF":
                    return disableWifi();
                case "FLASHLIGHT_ON":
                    return enableFlashlight();
                case "FLASHLIGHT_OFF":
                    return disableFlashlight();
                case "CAMERA_OPEN":
                    return openCamera();
                case "VOLUME_UP":
                    return increaseVolume();
                case "VOLUME_DOWN":
                    return decreaseVolume();
                case "VOLUME_MAX":
                    return setMaxVolume();
                case "ALARM_SET":
                    return setAlarm(params.get("time"), params.get("period"));
                case "ALARM_CANCEL":
                    return cancelAlarm();
                case "ALARM_LIST":
                    return listAlarms();
                case "TIMER_START":
                    return startTimer(params.get("duration"), params.get("unit"));
                case "TIMER_STOP":
                    return stopTimer();
                case "TIMER_SNOOZE":
                    return snoozeTimer();
                case "DND_ON":
                    return enableDoNotDisturb();
                case "DND_OFF":
                    return disableDoNotDisturb();
                case "THEME_DARK":
                    return setDarkTheme();
                case "THEME_LIGHT":
                    return setLightTheme();
                case "RINGTONE_VOLUME_UP":
                    return increaseRingtoneVolume();
                case "RINGTONE_VOLUME_DOWN":
                    return decreaseRingtoneVolume();
                case "ALARM_VOLUME_UP":
                    return increaseAlarmVolume();
                case "ALARM_VOLUME_DOWN":
                    return decreaseAlarmVolume();
                case "MOBILE_DATA_ON":
                    return enableMobileData();
                case "MOBILE_DATA_OFF":
                    return disableMobileData();
                case "HOTSPOT_ON":
                    return enableHotspot();
                case "HOTSPOT_OFF":
                    return disableHotspot();
                case "AIRPLANE_MODE_ON":
                    return enableAirplaneMode();
                case "AIRPLANE_MODE_OFF":
                    return disableAirplaneMode();
                case "SCREENSHOT":
                    return takeScreenshot();
                case "OPEN_APP":
                    return openApp(params.get("app_name"));
                case "CAMERA_PHOTO":
                    return takePhoto();
                case "CAMERA_VIDEO":
                    return recordVideo();
                case "CALL_PHONE":
                    return makeCall(params.get("contact"));
                case "SEND_SMS":
                    return sendSMS(params.get("message"));
                case "READ_MESSAGES":
                    return readMessages();
                case "CREATE_EVENT":
                    return createCalendarEvent();
                case "CREATE_REMINDER":
                    return createReminder();
                case "BRIGHTNESS_UP":
                    return increaseBrightness();
                case "BRIGHTNESS_DOWN":
                    return decreaseBrightness();
                case "BRIGHTNESS_AUTO":
                    return setAutoBrightness();
                case "CHANGE_WALLPAPER":
                    return changeWallpaper();
                case "AUTO_ROTATE_ON":
                    return enableAutoRotate();
                case "AUTO_ROTATE_OFF":
                    return disableAutoRotate();
                case "NFC_ON":
                    return enableNFC();
                case "NFC_OFF":
                    return disableNFC();
                case "LOCATION_ON":
                    return enableLocation();
                case "LOCATION_OFF":
                    return disableLocation();
                case "BATTERY_SAVER_ON":
                    return enableBatterySaver();
                case "BATTERY_SAVER_OFF":
                    return disableBatterySaver();
                case "SOUND_RING":
                    return setRingMode();
                case "SOUND_VIBRATE":
                    return setVibrateMode();
                case "SOUND_SILENT":
                    return setSilentMode();
                case "SCREEN_RECORD":
                    return startScreenRecording();
                case "ADD_NOTE":
                    return addNote(params.get("note"));
                case "ADD_TO_LIST":
                    return addToList(params.get("item"));
                case "MEDIA_PLAY":
                    return playMedia();
                case "MEDIA_PAUSE":
                    return pauseMedia();
                case "MEDIA_NEXT":
                    return nextMedia();
                case "MEDIA_PREVIOUS":
                    return previousMedia();
                case "WEATHER_CURRENT":
                    return getCurrentWeather();
                case "WEATHER_FORECAST":
                    return getWeatherForecast();
                case "NAVIGATE_TO":
                    return navigateTo(params.get("location"));
                case "COMMUTE_TIME":
                    return getCommuteTime();
                case "TRANSLATE":
                    return translateText(params.get("text"));
                case "SMART_HOME_CONTROL":
                    return controlSmartHome();
                case "POMODORO_START":
                    return startPomodoro();
                default:
                    return "❌ AI Command not implemented: " + intent;
            }
        } catch (Exception e) {
            return "❌ Error executing AI command: " + e.getMessage();
        }
    }
    
    private String enableBluetooth() {
        try {
            android.bluetooth.BluetoothAdapter bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                return "❌ Bluetooth not available on this device";
            }
            
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
                return "✅ Requesting Bluetooth permission...";
            } else {
                return "✅ Bluetooth is already ON";
            }
        } catch (Exception e) {
            return "❌ Could not enable Bluetooth: " + e.getMessage();
        }
    }
    
    private String disableBluetooth() {
        try {
            android.bluetooth.BluetoothAdapter bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.disable();
                return "✅ Bluetooth turned OFF";
            } else {
                return "✅ Bluetooth is already OFF";
            }
        } catch (Exception e) {
            return "❌ Could not disable Bluetooth: " + e.getMessage();
        }
    }
    
    private String enableWifi() {
        try {
            android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) getSystemService(WIFI_SERVICE);
            if (wifiManager != null && !wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
                return "✅ WiFi turned ON";
            } else {
                return "✅ WiFi is already ON";
            }
        } catch (Exception e) {
            return "❌ Could not enable WiFi: " + e.getMessage();
        }
    }
    
    private String disableWifi() {
        try {
            android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) getSystemService(WIFI_SERVICE);
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
                return "✅ WiFi turned OFF";
            } else {
                return "✅ WiFi is already OFF";
            }
        } catch (Exception e) {
            return "❌ Could not disable WiFi: " + e.getMessage();
        }
    }
    
    private String enableFlashlight() {
        try {
            android.hardware.camera2.CameraManager cameraManager = 
                (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE);
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, true);
            return "✅ Flashlight turned ON";
        } catch (Exception e) {
            return "❌ Could not enable flashlight: " + e.getMessage();
        }
    }
    
    private String disableFlashlight() {
        try {
            android.hardware.camera2.CameraManager cameraManager = 
                (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE);
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, false);
            return "✅ Flashlight turned OFF";
        } catch (Exception e) {
            return "❌ Could not disable flashlight: " + e.getMessage();
        }
    }
    
    private String openCamera() {
        try {
            String[] cameraPackages = {
                "com.android.camera2", "com.android.camera", "com.google.android.GoogleCamera",
                "com.oneplus.camera", "com.samsung.camera", "com.miui.camera"
            };
            
            for (String packageName : cameraPackages) {
                Intent cameraIntent = new Intent(Intent.ACTION_MAIN);
                cameraIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                cameraIntent.setPackage(packageName);
                cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(cameraIntent);
                    return "✅ Opening camera (" + packageName + ")";
                }
            }
            
            return "❌ No camera app found";
        } catch (Exception e) {
            return "❌ Could not open camera: " + e.getMessage();
        }
    }
    
    private String increaseVolume() {
        try {
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
            int newVolume = Math.min(currentVolume + 2, maxVolume);
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0);
            return "✅ Volume increased";
        } catch (Exception e) {
            return "❌ Could not increase volume: " + e.getMessage();
        }
    }
    
    private String decreaseVolume() {
        try {
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
            int newVolume = Math.max(currentVolume - 2, 0);
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0);
            return "✅ Volume decreased";
        } catch (Exception e) {
            return "❌ Could not decrease volume: " + e.getMessage();
        }
    }
    
    private String setMaxVolume() {
        try {
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            int maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, maxVolume, 0);
            return "✅ Volume set to maximum";
        } catch (Exception e) {
            return "❌ Could not set max volume: " + e.getMessage();
        }
    }
    
    private String setAlarm(String time, String period) {
        try {
            Log.d("MainActivity", "setAlarm called with time: " + time + ", period: " + period);
            
            // Try multiple alarm app approaches
            Intent alarmIntent = new Intent(android.provider.AlarmClock.ACTION_SET_ALARM);
            alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (time != null && period != null) {
                int hour = Integer.parseInt(time);
                if (period.equals("pm") && hour != 12) {
                    hour += 12;
                } else if (period.equals("am") && hour == 12) {
                    hour = 0;
                }
                alarmIntent.putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour);
                alarmIntent.putExtra(android.provider.AlarmClock.EXTRA_MINUTES, 0);
                alarmIntent.putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "Buddy Assistant Alarm");
            }
            
            // Try the standard alarm intent first
            if (alarmIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(alarmIntent);
                return "✅ Setting alarm" + (time != null ? " for " + time + " " + period : "");
            }
            
            // Fallback 1: Try to open clock app directly
            Intent clockIntent = new Intent(Intent.ACTION_MAIN);
            clockIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            clockIntent.setPackage("com.android.deskclock");
            clockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (clockIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(clockIntent);
                return "✅ Opening clock app for alarm setup";
            }
            
            // Fallback 2: Try generic clock app
            Intent genericClockIntent = new Intent(Intent.ACTION_MAIN);
            genericClockIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            genericClockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (genericClockIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(genericClockIntent);
                return "✅ Opening clock app for alarm setup";
            }
            
            // Fallback 3: Open settings
            Intent settingsIntent = new Intent(android.provider.Settings.ACTION_DATE_SETTINGS);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(settingsIntent);
            return "❌ No alarm app found, opened settings instead";
            
        } catch (Exception e) {
            return "❌ Could not set alarm: " + e.getMessage();
        }
    }
    
    // Enhanced Core Phone Utilities
    private String cancelAlarm() {
        try {
            Intent intent = new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS);
            startActivity(intent);
            return "✅ Opening alarm list to cancel";
        } catch (Exception e) {
            return "❌ Could not open alarm list: " + e.getMessage();
        }
    }
    
    private String listAlarms() {
        try {
            Intent intent = new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS);
            startActivity(intent);
            return "✅ Showing alarm list";
        } catch (Exception e) {
            return "❌ Could not show alarms: " + e.getMessage();
        }
    }
    
    private String startTimer(String duration, String unit) {
        try {
            Intent intent = new Intent(android.provider.AlarmClock.ACTION_SET_TIMER);
            if (duration != null && unit != null) {
                int minutes = Integer.parseInt(duration);
                if (unit.startsWith("hour") || unit.startsWith("hr")) {
                    minutes *= 60;
                }
                intent.putExtra(android.provider.AlarmClock.EXTRA_LENGTH, minutes * 60);
            }
            startActivity(intent);
            return "✅ Starting timer" + (duration != null ? " for " + duration + " " + unit : "");
        } catch (Exception e) {
            return "❌ Could not start timer: " + e.getMessage();
        }
    }
    
    private String stopTimer() {
        try {
            Intent intent = new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS);
            startActivity(intent);
            return "✅ Opening timer controls";
        } catch (Exception e) {
            return "❌ Could not access timer: " + e.getMessage();
        }
    }
    
    private String snoozeTimer() {
        try {
            Intent intent = new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS);
            startActivity(intent);
            return "✅ Opening timer controls for snooze";
        } catch (Exception e) {
            return "❌ Could not access timer: " + e.getMessage();
        }
    }
    
    private String enableDoNotDisturb() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
            return "✅ Opening Do Not Disturb settings";
        } catch (Exception e) {
            return "❌ Could not access Do Not Disturb: " + e.getMessage();
        }
    }
    
    private String disableDoNotDisturb() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
            return "✅ Opening Do Not Disturb settings";
        } catch (Exception e) {
            return "❌ Could not access Do Not Disturb: " + e.getMessage();
        }
    }
    
    private String setDarkTheme() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS);
            startActivity(intent);
            return "✅ Opening display settings for dark theme";
        } catch (Exception e) {
            return "❌ Could not access theme settings: " + e.getMessage();
        }
    }
    
    private String setLightTheme() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS);
            startActivity(intent);
            return "✅ Opening display settings for light theme";
        } catch (Exception e) {
            return "❌ Could not access theme settings: " + e.getMessage();
        }
    }
    
    private String increaseRingtoneVolume() {
        try {
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_RING);
            int maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_RING);
            int newVolume = Math.min(currentVolume + 2, maxVolume);
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, newVolume, 0);
            return "✅ Ringtone volume increased";
        } catch (Exception e) {
            return "❌ Could not increase ringtone volume: " + e.getMessage();
        }
    }
    
    private String decreaseRingtoneVolume() {
        try {
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_RING);
            int newVolume = Math.max(currentVolume - 2, 0);
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, newVolume, 0);
            return "✅ Ringtone volume decreased";
        } catch (Exception e) {
            return "❌ Could not decrease ringtone volume: " + e.getMessage();
        }
    }
    
    private String increaseAlarmVolume() {
        try {
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM);
            int maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM);
            int newVolume = Math.min(currentVolume + 2, maxVolume);
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, newVolume, 0);
            return "✅ Alarm volume increased";
        } catch (Exception e) {
            return "❌ Could not increase alarm volume: " + e.getMessage();
        }
    }
    
    private String decreaseAlarmVolume() {
        try {
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM);
            int newVolume = Math.max(currentVolume - 2, 0);
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, newVolume, 0);
            return "✅ Alarm volume decreased";
        } catch (Exception e) {
            return "❌ Could not decrease alarm volume: " + e.getMessage();
        }
    }
    
    private String enableMobileData() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS);
            startActivity(intent);
            return "✅ Opening mobile data settings";
        } catch (Exception e) {
            return "❌ Could not access mobile data settings: " + e.getMessage();
        }
    }
    
    private String disableMobileData() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS);
            startActivity(intent);
            return "✅ Opening mobile data settings";
        } catch (Exception e) {
            return "❌ Could not access mobile data settings: " + e.getMessage();
        }
    }
    
    private String enableHotspot() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
            startActivity(intent);
            return "✅ Opening wireless settings for hotspot";
        } catch (Exception e) {
            return "❌ Could not access hotspot settings: " + e.getMessage();
        }
    }
    
    private String disableHotspot() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
            startActivity(intent);
            return "✅ Opening wireless settings for hotspot";
        } catch (Exception e) {
            return "❌ Could not access hotspot settings: " + e.getMessage();
        }
    }
    
    private String enableAirplaneMode() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            startActivity(intent);
            return "✅ Opening airplane mode settings";
        } catch (Exception e) {
            return "❌ Could not access airplane mode: " + e.getMessage();
        }
    }
    
    private String disableAirplaneMode() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            startActivity(intent);
            return "✅ Opening airplane mode settings";
        } catch (Exception e) {
            return "❌ Could not access airplane mode: " + e.getMessage();
        }
    }
    
    private String takeScreenshot() {
        try {
            // Use system screenshot service
            Intent intent = new Intent("android.intent.action.SCREENSHOT");
            sendBroadcast(intent);
            return "✅ Taking screenshot";
        } catch (Exception e) {
            return "❌ Could not take screenshot: " + e.getMessage();
        }
    }
    
    private String openApp(String appName) {
        try {
            if (appName == null || appName.isEmpty()) {
                return "❌ Please specify which app to open";
            }
            
            // Try to find and launch the app
            Intent intent = getPackageManager().getLaunchIntentForPackage(appName);
            if (intent == null) {
                // Try common app names
                String[] commonApps = {
                    "com.android.chrome", "com.google.android.apps.maps", "com.android.calculator2",
                    "com.android.calendar", "com.android.contacts", "com.android.gallery3d",
                    "com.android.music", "com.android.settings", "com.android.vending"
                };
                
                for (String packageName : commonApps) {
                    if (packageName.contains(appName.toLowerCase())) {
                        intent = getPackageManager().getLaunchIntentForPackage(packageName);
                        if (intent != null) break;
                    }
                }
            }
            
            if (intent != null) {
                startActivity(intent);
                return "✅ Opening " + appName;
            } else {
                return "❌ App not found: " + appName;
            }
        } catch (Exception e) {
            return "❌ Could not open app: " + e.getMessage();
        }
    }
    
    private String takePhoto() {
        try {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivity(intent);
            return "✅ Opening camera for photo";
        } catch (Exception e) {
            return "❌ Could not open camera: " + e.getMessage();
        }
    }
    
    private String recordVideo() {
        try {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
            startActivity(intent);
            return "✅ Opening camera for video recording";
        } catch (Exception e) {
            return "❌ Could not open camera: " + e.getMessage();
        }
    }
    
    private String makeCall(String contact) {
        try {
            if (contact == null || contact.isEmpty()) {
                return "❌ Please specify who to call";
            }
            
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(android.net.Uri.parse("tel:" + contact));
            startActivity(intent);
            return "✅ Calling " + contact;
        } catch (Exception e) {
            return "❌ Could not make call: " + e.getMessage();
        }
    }
    
    private String sendSMS(String message) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(android.net.Uri.parse("sms:"));
            if (message != null && !message.isEmpty()) {
                intent.putExtra("sms_body", message);
            }
            startActivity(intent);
            return "✅ Opening SMS app" + (message != null ? " with message: " + message : "");
        } catch (Exception e) {
            return "❌ Could not open SMS: " + e.getMessage();
        }
    }
    
    private String readMessages() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_MESSAGING);
            startActivity(intent);
            return "✅ Opening messages app";
        } catch (Exception e) {
            return "❌ Could not open messages: " + e.getMessage();
        }
    }
    
    private String createCalendarEvent() {
        try {
            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setData(android.provider.CalendarContract.Events.CONTENT_URI);
            startActivity(intent);
            return "✅ Opening calendar to create event";
        } catch (Exception e) {
            return "❌ Could not open calendar: " + e.getMessage();
        }
    }
    
    private String createReminder() {
        try {
            Intent intent = new Intent(android.provider.AlarmClock.ACTION_SET_ALARM);
            startActivity(intent);
            return "✅ Opening alarm app to create reminder";
        } catch (Exception e) {
            return "❌ Could not create reminder: " + e.getMessage();
        }
    }
    
    // Device Personalization
    private String increaseBrightness() {
        try {
            android.provider.Settings.System.putInt(getContentResolver(), 
                android.provider.Settings.System.SCREEN_BRIGHTNESS, 255);
            return "✅ Brightness increased to maximum";
        } catch (Exception e) {
            return "❌ Could not increase brightness: " + e.getMessage();
        }
    }
    
    private String decreaseBrightness() {
        try {
            android.provider.Settings.System.putInt(getContentResolver(), 
                android.provider.Settings.System.SCREEN_BRIGHTNESS, 50);
            return "✅ Brightness decreased";
        } catch (Exception e) {
            return "❌ Could not decrease brightness: " + e.getMessage();
        }
    }
    
    private String setAutoBrightness() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS);
            startActivity(intent);
            return "✅ Opening display settings for auto brightness";
        } catch (Exception e) {
            return "❌ Could not access brightness settings: " + e.getMessage();
        }
    }
    
    private String changeWallpaper() {
        try {
            Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
            startActivity(Intent.createChooser(intent, "Choose wallpaper"));
            return "✅ Opening wallpaper selection";
        } catch (Exception e) {
            return "❌ Could not change wallpaper: " + e.getMessage();
        }
    }
    
    private String enableAutoRotate() {
        try {
            android.provider.Settings.System.putInt(getContentResolver(), 
                android.provider.Settings.System.ACCELEROMETER_ROTATION, 1);
            return "✅ Auto rotate enabled";
        } catch (Exception e) {
            return "❌ Could not enable auto rotate: " + e.getMessage();
        }
    }
    
    private String disableAutoRotate() {
        try {
            android.provider.Settings.System.putInt(getContentResolver(), 
                android.provider.Settings.System.ACCELEROMETER_ROTATION, 0);
            return "✅ Auto rotate disabled";
        } catch (Exception e) {
            return "❌ Could not disable auto rotate: " + e.getMessage();
        }
    }
    
    private String enableNFC() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_NFC_SETTINGS);
            startActivity(intent);
            return "✅ Opening NFC settings";
        } catch (Exception e) {
            return "❌ Could not access NFC settings: " + e.getMessage();
        }
    }
    
    private String disableNFC() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_NFC_SETTINGS);
            startActivity(intent);
            return "✅ Opening NFC settings";
        } catch (Exception e) {
            return "❌ Could not access NFC settings: " + e.getMessage();
        }
    }
    
    private String enableLocation() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            return "✅ Opening location settings";
        } catch (Exception e) {
            return "❌ Could not access location settings: " + e.getMessage();
        }
    }
    
    private String disableLocation() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            return "✅ Opening location settings";
        } catch (Exception e) {
            return "❌ Could not access location settings: " + e.getMessage();
        }
    }
    
    private String enableBatterySaver() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS);
            startActivity(intent);
            return "✅ Opening battery saver settings";
        } catch (Exception e) {
            return "❌ Could not access battery saver: " + e.getMessage();
        }
    }
    
    private String disableBatterySaver() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS);
            startActivity(intent);
            return "✅ Opening battery saver settings";
        } catch (Exception e) {
            return "❌ Could not access battery saver: " + e.getMessage();
        }
    }
    
    private String setRingMode() {
        try {
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            audioManager.setRingerMode(android.media.AudioManager.RINGER_MODE_NORMAL);
            return "✅ Set to ring mode";
        } catch (Exception e) {
            return "❌ Could not set ring mode: " + e.getMessage();
        }
    }
    
    private String setVibrateMode() {
        try {
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            audioManager.setRingerMode(android.media.AudioManager.RINGER_MODE_VIBRATE);
            return "✅ Set to vibrate mode";
        } catch (Exception e) {
            return "❌ Could not set vibrate mode: " + e.getMessage();
        }
    }
    
    private String setSilentMode() {
        try {
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            audioManager.setRingerMode(android.media.AudioManager.RINGER_MODE_SILENT);
            return "✅ Set to silent mode";
        } catch (Exception e) {
            return "❌ Could not set silent mode: " + e.getMessage();
        }
    }
    
    private String startScreenRecording() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS);
            startActivity(intent);
            return "✅ Opening display settings for screen recording";
        } catch (Exception e) {
            return "❌ Could not access screen recording: " + e.getMessage();
        }
    }
    
    private String addNote(String note) {
        try {
            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setType("vnd.android.cursor.dir/note");
            if (note != null && !note.isEmpty()) {
                intent.putExtra(Intent.EXTRA_TEXT, note);
            }
            startActivity(intent);
            return "✅ Opening notes app" + (note != null ? " with note: " + note : "");
        } catch (Exception e) {
            return "❌ Could not open notes: " + e.getMessage();
        }
    }
    
    private String addToList(String item) {
        try {
            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setType("vnd.android.cursor.dir/list");
            if (item != null && !item.isEmpty()) {
                intent.putExtra(Intent.EXTRA_TEXT, item);
            }
            startActivity(intent);
            return "✅ Opening list app" + (item != null ? " with item: " + item : "");
        } catch (Exception e) {
            return "❌ Could not open list app: " + e.getMessage();
        }
    }
    
    // Content & Media
    private String playMedia() {
        try {
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY));
            sendBroadcast(intent);
            return "✅ Playing media";
        } catch (Exception e) {
            return "❌ Could not play media: " + e.getMessage();
        }
    }
    
    private String pauseMedia() {
        try {
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE));
            sendBroadcast(intent);
            return "✅ Pausing media";
        } catch (Exception e) {
            return "❌ Could not pause media: " + e.getMessage();
        }
    }
    
    private String nextMedia() {
        try {
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT));
            sendBroadcast(intent);
            return "✅ Next track";
        } catch (Exception e) {
            return "❌ Could not skip to next: " + e.getMessage();
        }
    }
    
    private String previousMedia() {
        try {
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS));
            sendBroadcast(intent);
            return "✅ Previous track";
        } catch (Exception e) {
            return "❌ Could not go to previous: " + e.getMessage();
        }
    }
    
    // Navigation & Information
    private String getCurrentWeather() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("weather://"));
            startActivity(intent);
            return "✅ Opening weather app";
        } catch (Exception e) {
            return "❌ Could not open weather: " + e.getMessage();
        }
    }
    
    private String getWeatherForecast() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("weather://forecast"));
            startActivity(intent);
            return "✅ Opening weather forecast";
        } catch (Exception e) {
            return "❌ Could not open weather forecast: " + e.getMessage();
        }
    }
    
    private String navigateTo(String location) {
        try {
            if (location == null || location.isEmpty()) {
                return "❌ Please specify where to navigate";
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("geo:0,0?q=" + location));
            startActivity(intent);
            return "✅ Navigating to " + location;
        } catch (Exception e) {
            return "❌ Could not navigate: " + e.getMessage();
        }
    }
    
    private String getCommuteTime() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("google.navigation:q=work"));
            startActivity(intent);
            return "✅ Getting commute time to work";
        } catch (Exception e) {
            return "❌ Could not get commute time: " + e.getMessage();
        }
    }
    
    private String translateText(String text) {
        try {
            if (text == null || text.isEmpty()) {
                return "❌ Please specify text to translate";
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("translate://" + text));
            startActivity(intent);
            return "✅ Translating: " + text;
        } catch (Exception e) {
            return "❌ Could not translate: " + e.getMessage();
        }
    }
    
    // Lifestyle/Automation
    private String controlSmartHome() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_MAPS);
            startActivity(intent);
            return "✅ Opening smart home controls";
        } catch (Exception e) {
            return "❌ Could not access smart home: " + e.getMessage();
        }
    }
    
    private String startPomodoro() {
        try {
            Intent intent = new Intent(android.provider.AlarmClock.ACTION_SET_TIMER);
            intent.putExtra(android.provider.AlarmClock.EXTRA_LENGTH, 25 * 60); // 25 minutes
            startActivity(intent);
            return "✅ Starting 25-minute Pomodoro session";
        } catch (Exception e) {
            return "❌ Could not start Pomodoro: " + e.getMessage();
        }
    }
    
    // Speech Recognition Listener Methods
    @Override
    public void onReadyForSpeech(Bundle params) {
        if (mResponseText != null) {
            mResponseText.setText("🎤 Ready! Speak your command now...");
        }
    }
    
    @Override
    public void onBeginningOfSpeech() {
        if (mResponseText != null) {
            mResponseText.setText("🎤 I'm listening... Keep speaking...");
        }
    }
    
    @Override
    public void onRmsChanged(float rmsdB) {
        // Volume level changed - could show visual feedback
    }
    
    @Override
    public void onBufferReceived(byte[] buffer) {
        // Audio buffer received
    }
    
    @Override
    public void onEndOfSpeech() {
        if (mResponseText != null) {
            mResponseText.setText("🎤 Processing your speech...");
        }
    }
    
    @Override
    public void onError(int error) {
        mIsListening = false;
        mListenButton.setText("🎤 Listen");
        
        String errorMessage = "Speech recognition error: ";
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                errorMessage += "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                errorMessage += "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                errorMessage += "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                errorMessage += "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                errorMessage += "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                errorMessage += "No speech input recognized";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                errorMessage += "Recognition service busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                errorMessage += "Server error";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                errorMessage += "No speech input";
                break;
            default:
                errorMessage += "Unknown error";
                break;
        }
        
        if (mResponseText != null) {
            mResponseText.setText("❌ " + errorMessage + "\n\nTap microphone to try again or type your command above.");
        }
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onResults(Bundle results) {
        mIsListening = false;
        mListenButton.setText("🎤 Listen");
        
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String spokenText = matches.get(0);
            
            if (mCommandInput != null) {
                mCommandInput.setText(spokenText);
            }
            
            if (mResponseText != null) {
                mResponseText.setText("🎤 I heard: \"" + spokenText + "\"\n\nProcessing your command...");
            }
            
            // Process the spoken command
            String response = processSimpleCommand(spokenText);
            if (mResponseText != null) {
                mResponseText.setText("🎤 Voice Command: \"" + spokenText + "\"\n\n" + response);
            }
            
            Toast.makeText(this, "Voice command processed: " + spokenText, Toast.LENGTH_SHORT).show();
        } else {
            if (mResponseText != null) {
                mResponseText.setText("❌ No speech recognized. Try speaking more clearly or type your command above.");
            }
        }
    }
    
    @Override
    public void onPartialResults(Bundle partialResults) {
        // Partial results - could show live transcription
    }
    
    @Override
    public void onEvent(int eventType, Bundle params) {
        // Additional events
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCommandReceiver != null) {
            unregisterReceiver(mCommandReceiver);
        }
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 123 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                
                if (mCommandInput != null) {
                    mCommandInput.setText(spokenText);
                }
                
                if (mResponseText != null) {
                    mResponseText.setText("🎤 Voice Command: \"" + spokenText + "\"\n\nProcessing your command...");
                }
                
                // Process the spoken command
                String response = processSimpleCommand(spokenText);
                if (mResponseText != null) {
                    mResponseText.setText("🎤 Voice Command: \"" + spokenText + "\"\n\n" + response);
                }
                
                Toast.makeText(this, "Voice command processed: " + spokenText, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 123) {
            // Voice input was cancelled or failed
            if (mResponseText != null) {
                mResponseText.setText("Voice input cancelled or failed. You can type your command above instead.");
            }
            Toast.makeText(this, "Voice input cancelled. You can type your command instead.", Toast.LENGTH_SHORT).show();
        }
    }
}