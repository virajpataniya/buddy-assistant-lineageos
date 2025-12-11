package com.buddy.assistant;

import android.content.Context;
import android.util.Log;
import java.util.*;
import java.util.regex.Pattern;

public class AICommandProcessor {
    private static final String TAG = "AICommandProcessor";
    
    // Intent classification patterns
    private static final Map<String, List<String>> INTENT_PATTERNS = new HashMap<>();
    
    static {
        // Bluetooth intents
        INTENT_PATTERNS.put("BLUETOOTH_ON", Arrays.asList(
            "turn on bluetooth", "enable bluetooth", "activate bluetooth", 
            "switch on bluetooth", "bluetooth on", "open bluetooth",
            "start bluetooth", "connect bluetooth"
        ));
        
        INTENT_PATTERNS.put("BLUETOOTH_OFF", Arrays.asList(
            "turn off bluetooth", "disable bluetooth", "deactivate bluetooth",
            "switch off bluetooth", "bluetooth off", "close bluetooth",
            "stop bluetooth", "disconnect bluetooth"
        ));
        
        // WiFi intents
        INTENT_PATTERNS.put("WIFI_ON", Arrays.asList(
            "turn on wifi", "enable wifi", "activate wifi", "switch on wifi",
            "wifi on", "open wifi", "start wifi", "connect wifi"
        ));
        
        INTENT_PATTERNS.put("WIFI_OFF", Arrays.asList(
            "turn off wifi", "disable wifi", "deactivate wifi", "switch off wifi",
            "wifi off", "close wifi", "stop wifi", "disconnect wifi"
        ));
        
        // Flashlight intents
        INTENT_PATTERNS.put("FLASHLIGHT_ON", Arrays.asList(
            "turn on flashlight", "turn on torch", "turn on flash", "turn on light",
            "enable flashlight", "activate flashlight", "switch on flashlight",
            "flashlight on", "torch on", "flash on", "light on"
        ));
        
        INTENT_PATTERNS.put("FLASHLIGHT_OFF", Arrays.asList(
            "turn off flashlight", "turn off torch", "turn off flash", "turn off light",
            "disable flashlight", "deactivate flashlight", "switch off flashlight",
            "flashlight off", "torch off", "flash off", "light off"
        ));
        
        // Camera intents
        INTENT_PATTERNS.put("CAMERA_OPEN", Arrays.asList(
            "open camera", "take photo", "take picture", "open camera app",
            "launch camera", "start camera", "camera", "photo", "picture"
        ));
        
        // Volume intents
        INTENT_PATTERNS.put("VOLUME_UP", Arrays.asList(
            "increase volume", "turn up volume", "volume up", "louder",
            "make it louder", "volume higher", "increase sound", "turn up sound"
        ));
        
        INTENT_PATTERNS.put("VOLUME_DOWN", Arrays.asList(
            "decrease volume", "turn down volume", "volume down", "quieter",
            "make it quieter", "volume lower", "decrease sound", "turn down sound"
        ));
        
        INTENT_PATTERNS.put("VOLUME_MAX", Arrays.asList(
            "volume maximum", "volume 100", "volume max", "full volume",
            "maximum volume", "volume to max", "volume to 100%"
        ));
        
        // Alarm intents
        INTENT_PATTERNS.put("ALARM_SET", Arrays.asList(
            "set alarm", "create alarm", "add alarm", "schedule alarm",
            "wake me up", "remind me", "alarm for", "set reminder",
            "set alarm for", "create alarm for", "add alarm for",
            "schedule alarm for", "wake me up at", "remind me at"
        ));
        
        INTENT_PATTERNS.put("ALARM_CANCEL", Arrays.asList(
            "cancel alarm", "delete alarm", "remove alarm", "stop alarm",
            "turn off alarm", "disable alarm"
        ));
        
        INTENT_PATTERNS.put("ALARM_LIST", Arrays.asList(
            "list alarms", "show alarms", "what alarms", "alarm list",
            "upcoming alarms", "scheduled alarms"
        ));
        
        // Timer intents
        INTENT_PATTERNS.put("TIMER_START", Arrays.asList(
            "start timer", "set timer", "create timer", "begin timer",
            "timer for", "countdown for"
        ));
        
        INTENT_PATTERNS.put("TIMER_STOP", Arrays.asList(
            "stop timer", "cancel timer", "end timer", "pause timer"
        ));
        
        INTENT_PATTERNS.put("TIMER_SNOOZE", Arrays.asList(
            "snooze timer", "extend timer", "add time to timer"
        ));
        
        // Do Not Disturb intents
        INTENT_PATTERNS.put("DND_ON", Arrays.asList(
            "turn on do not disturb", "enable do not disturb", "activate do not disturb",
            "silent mode", "quiet mode", "do not disturb on"
        ));
        
        INTENT_PATTERNS.put("DND_OFF", Arrays.asList(
            "turn off do not disturb", "disable do not disturb", "deactivate do not disturb",
            "normal mode", "do not disturb off"
        ));
        
        // Theme intents
        INTENT_PATTERNS.put("THEME_DARK", Arrays.asList(
            "dark theme", "dark mode", "switch to dark", "enable dark theme",
            "turn on dark mode", "dark appearance"
        ));
        
        INTENT_PATTERNS.put("THEME_LIGHT", Arrays.asList(
            "light theme", "light mode", "switch to light", "enable light theme",
            "turn on light mode", "light appearance"
        ));
        
        // Volume intents (enhanced)
        INTENT_PATTERNS.put("RINGTONE_VOLUME_UP", Arrays.asList(
            "increase ringtone volume", "turn up ringtone", "ringtone louder",
            "increase call volume", "turn up call volume"
        ));
        
        INTENT_PATTERNS.put("RINGTONE_VOLUME_DOWN", Arrays.asList(
            "decrease ringtone volume", "turn down ringtone", "ringtone quieter",
            "decrease call volume", "turn down call volume"
        ));
        
        INTENT_PATTERNS.put("ALARM_VOLUME_UP", Arrays.asList(
            "increase alarm volume", "turn up alarm", "alarm louder",
            "increase alarm sound", "turn up alarm sound"
        ));
        
        INTENT_PATTERNS.put("ALARM_VOLUME_DOWN", Arrays.asList(
            "decrease alarm volume", "turn down alarm", "alarm quieter",
            "decrease alarm sound", "turn down alarm sound"
        ));
        
        // Mobile data and hotspot intents
        INTENT_PATTERNS.put("MOBILE_DATA_ON", Arrays.asList(
            "turn on mobile data", "enable mobile data", "activate mobile data",
            "turn on cellular data", "enable cellular data"
        ));
        
        INTENT_PATTERNS.put("MOBILE_DATA_OFF", Arrays.asList(
            "turn off mobile data", "disable mobile data", "deactivate mobile data",
            "turn off cellular data", "disable cellular data"
        ));
        
        INTENT_PATTERNS.put("HOTSPOT_ON", Arrays.asList(
            "turn on hotspot", "enable hotspot", "activate hotspot",
            "turn on wifi hotspot", "enable wifi sharing"
        ));
        
        INTENT_PATTERNS.put("HOTSPOT_OFF", Arrays.asList(
            "turn off hotspot", "disable hotspot", "deactivate hotspot",
            "turn off wifi hotspot", "disable wifi sharing"
        ));
        
        // Airplane mode intents
        INTENT_PATTERNS.put("AIRPLANE_MODE_ON", Arrays.asList(
            "turn on airplane mode", "enable airplane mode", "activate airplane mode",
            "flight mode on", "airplane mode"
        ));
        
        INTENT_PATTERNS.put("AIRPLANE_MODE_OFF", Arrays.asList(
            "turn off airplane mode", "disable airplane mode", "deactivate airplane mode",
            "flight mode off", "normal mode"
        ));
        
        // Screenshot intents
        INTENT_PATTERNS.put("SCREENSHOT", Arrays.asList(
            "take screenshot", "capture screen", "screenshot", "screen capture",
            "take screen shot", "capture screenshot"
        ));
        
        // App launching intents
        INTENT_PATTERNS.put("OPEN_APP", Arrays.asList(
            "open app", "launch app", "start app", "run app",
            "open", "launch", "start"
        ));
        
        // Camera capture intents
        INTENT_PATTERNS.put("CAMERA_PHOTO", Arrays.asList(
            "take photo", "take picture", "capture photo", "snap photo",
            "take a photo", "take a picture"
        ));
        
        INTENT_PATTERNS.put("CAMERA_VIDEO", Arrays.asList(
            "take video", "record video", "start recording", "video recording",
            "take a video", "record a video"
        ));
        
        // Phone and SMS intents
        INTENT_PATTERNS.put("CALL_PHONE", Arrays.asList(
            "call", "phone call", "make a call", "dial",
            "ring", "contact"
        ));
        
        INTENT_PATTERNS.put("SEND_SMS", Arrays.asList(
            "send message", "send sms", "text", "send text",
            "message", "sms"
        ));
        
        INTENT_PATTERNS.put("READ_MESSAGES", Arrays.asList(
            "read messages", "show messages", "unread messages",
            "check messages", "message list"
        ));
        
        // Calendar intents
        INTENT_PATTERNS.put("CREATE_EVENT", Arrays.asList(
            "create event", "add event", "schedule event", "calendar event",
            "add to calendar", "schedule meeting"
        ));
        
        INTENT_PATTERNS.put("CREATE_REMINDER", Arrays.asList(
            "create reminder", "add reminder", "set reminder", "remind me",
            "add reminder", "schedule reminder"
        ));
        
        // Display and personalization intents
        INTENT_PATTERNS.put("BRIGHTNESS_UP", Arrays.asList(
            "increase brightness", "turn up brightness", "brighter", "brighten screen",
            "increase screen brightness", "make it brighter"
        ));
        
        INTENT_PATTERNS.put("BRIGHTNESS_DOWN", Arrays.asList(
            "decrease brightness", "turn down brightness", "dimmer", "dim screen",
            "decrease screen brightness", "make it dimmer"
        ));
        
        INTENT_PATTERNS.put("BRIGHTNESS_AUTO", Arrays.asList(
            "auto brightness", "automatic brightness", "adaptive brightness",
            "turn on auto brightness", "enable auto brightness"
        ));
        
        INTENT_PATTERNS.put("CHANGE_WALLPAPER", Arrays.asList(
            "change wallpaper", "set wallpaper", "new wallpaper", "wallpaper",
            "change background", "set background"
        ));
        
        // System toggles
        INTENT_PATTERNS.put("AUTO_ROTATE_ON", Arrays.asList(
            "turn on auto rotate", "enable auto rotate", "activate auto rotate",
            "auto rotation on", "screen rotation on"
        ));
        
        INTENT_PATTERNS.put("AUTO_ROTATE_OFF", Arrays.asList(
            "turn off auto rotate", "disable auto rotate", "deactivate auto rotate",
            "auto rotation off", "screen rotation off"
        ));
        
        INTENT_PATTERNS.put("NFC_ON", Arrays.asList(
            "turn on nfc", "enable nfc", "activate nfc", "nfc on"
        ));
        
        INTENT_PATTERNS.put("NFC_OFF", Arrays.asList(
            "turn off nfc", "disable nfc", "deactivate nfc", "nfc off"
        ));
        
        INTENT_PATTERNS.put("LOCATION_ON", Arrays.asList(
            "turn on location", "enable location", "activate location", "gps on",
            "location services on", "turn on gps"
        ));
        
        INTENT_PATTERNS.put("LOCATION_OFF", Arrays.asList(
            "turn off location", "disable location", "deactivate location", "gps off",
            "location services off", "turn off gps"
        ));
        
        INTENT_PATTERNS.put("BATTERY_SAVER_ON", Arrays.asList(
            "turn on battery saver", "enable battery saver", "activate battery saver",
            "power saving mode", "battery saver on"
        ));
        
        INTENT_PATTERNS.put("BATTERY_SAVER_OFF", Arrays.asList(
            "turn off battery saver", "disable battery saver", "deactivate battery saver",
            "normal power mode", "battery saver off"
        ));
        
        // Sound profiles
        INTENT_PATTERNS.put("SOUND_RING", Arrays.asList(
            "ring mode", "normal mode", "sound on", "ringtone on",
            "turn on sound", "enable sound"
        ));
        
        INTENT_PATTERNS.put("SOUND_VIBRATE", Arrays.asList(
            "vibrate mode", "vibration mode", "vibrate only", "vibrate",
            "turn on vibrate", "enable vibrate"
        ));
        
        INTENT_PATTERNS.put("SOUND_SILENT", Arrays.asList(
            "silent mode", "mute mode", "silent", "mute",
            "turn off sound", "disable sound"
        ));
        
        // Screen recording
        INTENT_PATTERNS.put("SCREEN_RECORD", Arrays.asList(
            "start screen recording", "record screen", "screen record",
            "begin recording", "start recording"
        ));
        
        // Notes and lists
        INTENT_PATTERNS.put("ADD_NOTE", Arrays.asList(
            "add note", "create note", "write note", "note",
            "take note", "make note"
        ));
        
        INTENT_PATTERNS.put("ADD_TO_LIST", Arrays.asList(
            "add to list", "shopping list", "add item", "list",
            "add to shopping list", "grocery list"
        ));
        
        // Media control
        INTENT_PATTERNS.put("MEDIA_PLAY", Arrays.asList(
            "play music", "start music", "play song", "resume music",
            "play", "start playing"
        ));
        
        INTENT_PATTERNS.put("MEDIA_PAUSE", Arrays.asList(
            "pause music", "stop music", "pause song", "pause",
            "stop playing", "pause playing"
        ));
        
        INTENT_PATTERNS.put("MEDIA_NEXT", Arrays.asList(
            "next song", "next track", "skip song", "next",
            "skip track", "next music"
        ));
        
        INTENT_PATTERNS.put("MEDIA_PREVIOUS", Arrays.asList(
            "previous song", "previous track", "back song", "previous",
            "go back", "previous music"
        ));
        
        // Weather and information
        INTENT_PATTERNS.put("WEATHER_CURRENT", Arrays.asList(
            "current weather", "weather now", "what's the weather",
            "weather today", "how's the weather"
        ));
        
        INTENT_PATTERNS.put("WEATHER_FORECAST", Arrays.asList(
            "weather forecast", "tomorrow's weather", "weather tomorrow",
            "forecast", "weather prediction"
        ));
        
        // Navigation
        INTENT_PATTERNS.put("NAVIGATE_TO", Arrays.asList(
            "navigate to", "directions to", "go to", "route to",
            "how to get to", "drive to"
        ));
        
        INTENT_PATTERNS.put("COMMUTE_TIME", Arrays.asList(
            "commute time", "travel time", "how long to get to",
            "time to work", "time to home"
        ));
        
        // Translation
        INTENT_PATTERNS.put("TRANSLATE", Arrays.asList(
            "translate", "what does this mean", "how do you say",
            "translate to", "language translation"
        ));
        
        // Smart home control
        INTENT_PATTERNS.put("SMART_HOME_CONTROL", Arrays.asList(
            "turn on lights", "turn off lights", "smart home",
            "control lights", "home automation"
        ));
        
        // Pomodoro timer
        INTENT_PATTERNS.put("POMODORO_START", Arrays.asList(
            "start pomodoro", "begin pomodoro", "pomodoro timer",
            "focus session", "work session"
        ));
    }
    
    public static class CommandResult {
        public String intent;
        public Map<String, String> parameters;
        public float confidence;
        
        public CommandResult(String intent, Map<String, String> parameters, float confidence) {
            this.intent = intent;
            this.parameters = parameters;
            this.confidence = confidence;
        }
    }
    
    public static CommandResult processCommand(String userInput) {
        String normalizedInput = normalizeInput(userInput);
        Log.d(TAG, "Processing command: " + normalizedInput);
        
        // Intent classification using pattern matching (simplified DistilBERT approach)
        CommandResult bestMatch = null;
        float bestConfidence = 0.0f;
        
        for (Map.Entry<String, List<String>> entry : INTENT_PATTERNS.entrySet()) {
            String intent = entry.getKey();
            List<String> patterns = entry.getValue();
            
            for (String pattern : patterns) {
                float similarity = calculateSimilarity(normalizedInput, pattern);
                if (similarity > bestConfidence) {
                    bestConfidence = similarity;
                    bestMatch = new CommandResult(intent, extractParameters(normalizedInput, intent), similarity);
                    Log.d(TAG, "New best match: " + intent + " with pattern: " + pattern + " score: " + similarity);
                }
            }
        }
        
        // If confidence is too low, try fuzzy matching
        if (bestConfidence < 0.6f) {
            bestMatch = fuzzyMatch(normalizedInput);
        }
        
        Log.d(TAG, "Final result: " + (bestMatch != null ? bestMatch.intent : "UNKNOWN") + " with confidence: " + (bestMatch != null ? bestMatch.confidence : 0.0f));
        
        return bestMatch != null ? bestMatch : new CommandResult("UNKNOWN", new HashMap<>(), 0.0f);
    }
    
    private static String normalizeInput(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    private static float calculateSimilarity(String input, String pattern) {
        // Simple word-based similarity calculation
        String[] inputWords = input.split("\\s+");
        String[] patternWords = pattern.split("\\s+");
        
        int matches = 0;
        for (String inputWord : inputWords) {
            for (String patternWord : patternWords) {
                if (inputWord.equals(patternWord) || 
                    inputWord.contains(patternWord) || 
                    patternWord.contains(inputWord)) {
                    matches++;
                    break;
                }
            }
        }
        
        return (float) matches / Math.max(inputWords.length, patternWords.length);
    }
    
    private static Map<String, String> extractParameters(String input, String intent) {
        Map<String, String> params = new HashMap<>();
        
        // Extract time for alarms and timers
        Log.d(TAG, "Checking intent: " + intent + " for time extraction");
        if (intent.equals("ALARM_SET") || intent.equals("TIMER_START")) {
            Log.d(TAG, "Intent matches ALARM_SET or TIMER_START, extracting time from: " + input);
            Pattern timePattern = Pattern.compile("(\\d{1,2})\\s*(am|pm|AM|PM)");
            java.util.regex.Matcher matcher = timePattern.matcher(input);
            if (matcher.find()) {
                params.put("time", matcher.group(1));
                params.put("period", matcher.group(2).toLowerCase());
                Log.d(TAG, "Extracted alarm time: " + matcher.group(1) + " " + matcher.group(2).toLowerCase());
            } else {
                Log.d(TAG, "No time pattern found in: " + input);
            }
            
            // Extract duration for timers
            Pattern durationPattern = Pattern.compile("(\\d+)\\s*(minute|min|hour|hr|second|sec)");
            matcher = durationPattern.matcher(input);
            if (matcher.find()) {
                params.put("duration", matcher.group(1));
                params.put("unit", matcher.group(2).toLowerCase());
            }
        }
        
        // Extract volume level
        if (intent.startsWith("VOLUME_")) {
            Pattern volumePattern = Pattern.compile("(\\d+)%?");
            java.util.regex.Matcher matcher = volumePattern.matcher(input);
            if (matcher.find()) {
                params.put("level", matcher.group(1));
            }
        }
        
        // Extract app name for OPEN_APP
        if (intent.equals("OPEN_APP")) {
            String[] words = input.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                if (words[i].equals("open") || words[i].equals("launch") || words[i].equals("start")) {
                    if (i + 1 < words.length) {
                        params.put("app_name", words[i + 1]);
                    }
                    break;
                }
            }
        }
        
        // Extract contact name for CALL_PHONE
        if (intent.equals("CALL_PHONE")) {
            String[] words = input.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                if (words[i].equals("call") || words[i].equals("dial")) {
                    if (i + 1 < words.length) {
                        params.put("contact", words[i + 1]);
                    }
                    break;
                }
            }
        }
        
        // Extract message content for SEND_SMS
        if (intent.equals("SEND_SMS")) {
            String[] words = input.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                if (words[i].equals("send") || words[i].equals("text")) {
                    if (i + 1 < words.length) {
                        StringBuilder message = new StringBuilder();
                        for (int j = i + 1; j < words.length; j++) {
                            message.append(words[j]).append(" ");
                        }
                        params.put("message", message.toString().trim());
                    }
                    break;
                }
            }
        }
        
        // Extract location for navigation
        if (intent.equals("NAVIGATE_TO")) {
            String[] words = input.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                if (words[i].equals("to") || words[i].equals("navigate")) {
                    if (i + 1 < words.length) {
                        StringBuilder location = new StringBuilder();
                        for (int j = i + 1; j < words.length; j++) {
                            location.append(words[j]).append(" ");
                        }
                        params.put("location", location.toString().trim());
                    }
                    break;
                }
            }
        }
        
        // Extract text for translation
        if (intent.equals("TRANSLATE")) {
            String[] words = input.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                if (words[i].equals("translate")) {
                    if (i + 1 < words.length) {
                        StringBuilder text = new StringBuilder();
                        for (int j = i + 1; j < words.length; j++) {
                            text.append(words[j]).append(" ");
                        }
                        params.put("text", text.toString().trim());
                    }
                    break;
                }
            }
        }
        
        // Extract note content
        if (intent.equals("ADD_NOTE")) {
            String[] words = input.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                if (words[i].equals("note") || words[i].equals("add")) {
                    if (i + 1 < words.length) {
                        StringBuilder note = new StringBuilder();
                        for (int j = i + 1; j < words.length; j++) {
                            note.append(words[j]).append(" ");
                        }
                        params.put("note", note.toString().trim());
                    }
                    break;
                }
            }
        }
        
        // Extract list item
        if (intent.equals("ADD_TO_LIST")) {
            String[] words = input.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                if (words[i].equals("add") || words[i].equals("list")) {
                    if (i + 1 < words.length) {
                        StringBuilder item = new StringBuilder();
                        for (int j = i + 1; j < words.length; j++) {
                            item.append(words[j]).append(" ");
                        }
                        params.put("item", item.toString().trim());
                    }
                    break;
                }
            }
        }
        
        return params;
    }
    
    private static CommandResult fuzzyMatch(String input) {
        // Advanced fuzzy matching for natural language
        String[] words = input.split("\\s+");
        
        // Check for action words
        boolean hasAction = false;
        String action = "";
        for (String word : words) {
            if (word.matches("(turn|switch|enable|disable|open|close|start|stop|increase|decrease|set|create)")) {
                hasAction = true;
                action = word;
                break;
            }
        }
        
        // Check for target words
        String target = "";
        for (String word : words) {
            if (word.matches("(bluetooth|wifi|flashlight|torch|flash|light|camera|volume|sound|alarm)")) {
                target = word;
                break;
            }
        }
        
        if (hasAction && !target.isEmpty()) {
            String intent = generateIntent(action, target, words);
            return new CommandResult(intent, new HashMap<>(), 0.7f);
        }
        
        return null;
    }
    
    private static String generateIntent(String action, String target, String[] words) {
        // Generate intent based on action and target
        if (target.equals("bluetooth")) {
            return action.matches("(turn|switch|enable|open|start)") ? "BLUETOOTH_ON" : "BLUETOOTH_OFF";
        } else if (target.equals("wifi")) {
            return action.matches("(turn|switch|enable|open|start)") ? "WIFI_ON" : "WIFI_OFF";
        } else if (target.matches("(flashlight|torch|flash|light)")) {
            return action.matches("(turn|switch|enable|open|start)") ? "FLASHLIGHT_ON" : "FLASHLIGHT_OFF";
        } else if (target.equals("camera")) {
            return "CAMERA_OPEN";
        } else if (target.matches("(volume|sound)")) {
            if (action.matches("(increase|turn up|higher)")) {
                return "VOLUME_UP";
            } else if (action.matches("(decrease|turn down|lower)")) {
                return "VOLUME_DOWN";
            } else if (action.matches("(set|make)")) {
                return "VOLUME_MAX";
            }
        } else if (target.equals("alarm")) {
            return "ALARM_SET";
        }
        
        return "UNKNOWN";
    }
}
