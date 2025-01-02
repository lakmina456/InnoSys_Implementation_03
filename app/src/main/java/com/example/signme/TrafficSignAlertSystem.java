package com.example.signme;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class TrafficSignAlertSystem {

    private TextToSpeech textToSpeech;
    private JSONObject contextualRules;
    private boolean isTtsInitialized = false;

    public TrafficSignAlertSystem(Context context) {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
                isTtsInitialized = true;
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });

        loadContextualRules(context);
    }

    public void loadContextualRules(Context context) {
        try {
            InputStream inputStream = context.getAssets().open("contextual_rules.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            contextualRules = new JSONObject(jsonString);
        } catch (Exception e) {
            Log.e("TrafficSignAlertSystem", "Error loading contextual rules", e);
        }
    }

    public void alertBasedOnContext(String sign1, String sign2) {
        try {
            if (contextualRules != null && contextualRules.has(sign1)) {
                JSONObject rulesForFirstSign = contextualRules.getJSONObject(sign1);
                if (rulesForFirstSign.has(sign2)) {
                    String message = rulesForFirstSign.getString(sign2);
                    speak(message);
                }
            }
        } catch (Exception e) {
            Log.e("TrafficSignAlertSystem", "Error checking contextual rules", e);
        }
    }

    void speak(String message) {
        if (isTtsInitialized) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
