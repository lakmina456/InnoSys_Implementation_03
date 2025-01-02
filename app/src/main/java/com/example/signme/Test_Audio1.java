package com.example.signme;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import android.media.AudioManager;
import android.widget.ImageButton;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;

public class Test_Audio1 extends AppCompatActivity {

    private static final String TAG = "Test_Audio1";
    private TrafficSignAlertSystem alertSystem;
    private String firstSignDetected = null;
    private String secondSignDetected = null;
    private String previousSign = "";
    private String currentSign = "";

    private VideoCapture videoCapture;
    private boolean isPlaying = false;
    private Handler handler;
    private Runnable videoRunnable;
    private ImageView videoView, signView,image_view1,image_view2;
    private TextView labelView;
    private final double alpha = 1.20; // Fixed contrast
    private final double beta = 20.0;  // Fixed brightness
    private Map<Integer, JSONArray> frameDetections;
    private TextToSpeech textToSpeech; // TextToSpeech instance
    private TextView dateTimeView;
    private Handler handler2 = new Handler();
    private ImageButton muteUnmuteButton;
    private boolean isMuted = false; // Track mute state
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test1);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);


        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed");
        } else {
            Log.d(TAG, "OpenCV initialized successfully");
        }
        alertSystem = new TrafficSignAlertSystem(this);

        videoView = findViewById(R.id.ImageView1);
        signView = findViewById(R.id.sign_view);
        labelView = findViewById(R.id.label_view);
        ConstraintLayout mainLayout = findViewById(R.id.root_layout);
        dateTimeView = findViewById(R.id.date_time_view);
        muteUnmuteButton = findViewById(R.id.mute_unmute1);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        image_view1 = findViewById(R.id.image_view1);
        image_view2 = findViewById(R.id.image_view2);


        muteUnmuteButton.setOnClickListener(v -> toggleMute());

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TextToSpeech: Language not supported");
                } else {
                    Log.d(TAG, "TextToSpeech initialized successfully");
                }
            } else {
                Log.e(TAG, "TextToSpeech initialization failed");
            }
        });

        String videoPath = copyResourceToInternalStorage(R.raw.v1, "v1.mp4");
        String jsonPath = copyResourceToInternalStorage(R.raw.detection_results1, "detection_results1.json");

        parseDetectionJSON(jsonPath);

        videoCapture = new VideoCapture();
        if (!videoCapture.open(videoPath)) {
            Log.e(TAG, "Cannot open video: " + videoPath);
            return;
        }

        handler = new Handler();

        videoRunnable = new Runnable() {
            int frameCount = 0;

            @Override
            public void run() {
                Mat frame = new Mat();
                if (videoCapture.read(frame)) {
                    frameCount++;

                    Mat adjustedFrame = new Mat();
                    frame.convertTo(adjustedFrame, -1, alpha, beta);

                    // Annotate frame and update views
                    annotateFrame(adjustedFrame, frameCount);

                    Bitmap bitmap = Bitmap.createBitmap(adjustedFrame.cols(), adjustedFrame.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(adjustedFrame, bitmap);

                    runOnUiThread(() -> videoView.setImageBitmap(bitmap));

                    handler.postDelayed(this, 33); // ~30fps
                } else {
                    isPlaying = false;
                }
            }
        };

        isPlaying = true;
        handler.post(videoRunnable);



        // Start updating the date and time
        updateDateTime();




        // Fade animation for layout transparency
        ObjectAnimator fadeLayout = ObjectAnimator.ofFloat(mainLayout, "alpha", 1f, 0.1f);
        fadeLayout.setDuration(3000);

        // Combine animations
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(fadeLayout);
        animatorSet.start();

    }


    private void toggleMute() {
        if (isMuted) {
            // Unmute
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            isMuted = false;
        } else {
            // Mute
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            isMuted = true;
        }
        updateButtonImage();
    }
    private void updateButtonImage() {
        if (isMuted) {
            muteUnmuteButton.setImageResource(R.drawable.volume_off); // Replace with your mute icon
        } else {
            muteUnmuteButton.setImageResource(R.drawable.volume); // Replace with your unmute icon
        }
    }

    private void updateDateTime() {
        // Create a Runnable to update the TextView every second
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // Get the current date and time
                LocalDateTime now = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    now = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MMM d hh:mm:ss a", Locale.getDefault());
                    String currentDateTime = now.format(formatter);
                    dateTimeView.setText(currentDateTime);
                    handler2.postDelayed(this, 1000);
                }

            }
        };

        // Start the first update
        handler2.post(runnable);
    }

    private void annotateFrame(Mat frame, int frameNumber) {
        JSONArray detections = frameDetections.get(frameNumber);
        if (detections == null) return;

        for (int i = 0; i < detections.length(); i++) {
            try {
                JSONObject detection = detections.getJSONObject(i);
                JSONArray bboxArray = detection.getJSONArray("bbox");
                String className = detection.getString("class_name");

                int xMin = bboxArray.getInt(0);
                int yMin = bboxArray.getInt(1);
                int xMax = bboxArray.getInt(2);
                int yMax = bboxArray.getInt(3);

                Rect rect = new Rect(xMin, yMin, xMax - xMin, yMax - yMin);
                org.opencv.imgproc.Imgproc.rectangle(frame, rect.tl(), rect.br(), new Scalar(0, 255, 0), 2);

                updatePreviousAndCurrentSign1(className);

                // Update sign image and label
                updateSignAndLabel(className);
            } catch (Exception e) {
                Log.e(TAG, "Error annotating frame: " + e.getMessage());
            }
        }
    }

    private void updateSignAndLabel(String className) {
        runOnUiThread(() -> {
            // Convert the detected sign name to a drawable resource name

            String drawableName = formatToDrawableName(className);
            labelView.setText(className);
            // Load sign image dynamically
            int resId = getResources().getIdentifier(drawableName, "drawable", getPackageName());

            if (resId != 0) {
                signView.setImageResource(resId);

            } else {
                //labelView.setText("");
            }

            // Speak the detected sign name
            //speakSignName(className);

            // Update label text
            //labelView.setText(className);
        });
    }
    private void updatePreviousAndCurrentSign1(String className) {
        if (firstSignDetected == null) {
            firstSignDetected = className;
            alertSystem.speak(className);
            image_view1.setImageResource(getResources().getIdentifier(formatToDrawableName(className), "drawable", getPackageName()));
        } else if (!className.equals(firstSignDetected) && secondSignDetected == null) {
            secondSignDetected = className;
            alertSystem.speak(className);
            alertSystem.alertBasedOnContext(firstSignDetected, secondSignDetected);
            image_view2.setImageResource(getResources().getIdentifier(formatToDrawableName(firstSignDetected), "drawable", getPackageName()));
            image_view1.setImageResource(getResources().getIdentifier(formatToDrawableName(secondSignDetected), "drawable", getPackageName()));
        } else if (!className.equals(firstSignDetected) && !className.equals(secondSignDetected)) {
            previousSign = firstSignDetected;
            firstSignDetected = secondSignDetected;
            secondSignDetected = className;
            alertSystem.alertBasedOnContext(previousSign, secondSignDetected);
            previousSign = secondSignDetected;
            secondSignDetected = className;
            alertSystem.alertBasedOnContext(previousSign, secondSignDetected);
            image_view2.setImageResource(getResources().getIdentifier(formatToDrawableName(firstSignDetected), "drawable", getPackageName()));
            image_view1.setImageResource(getResources().getIdentifier(formatToDrawableName(secondSignDetected), "drawable", getPackageName()));
        }
    }
    private void updatePreviousAndCurrentSign2(String className) {
        if (firstSignDetected == null) {
            firstSignDetected = className;
            alertSystem.alertBasedOnContext(firstSignDetected, firstSignDetected);
        } else if (!className.equals(firstSignDetected) && secondSignDetected == null) {
            secondSignDetected = className;
            alertSystem.speak(className);
            alertSystem.alertBasedOnContext(firstSignDetected, secondSignDetected);
        } else if (!className.equals(firstSignDetected) && !className.equals(secondSignDetected)) {
            previousSign = firstSignDetected;
            firstSignDetected = secondSignDetected;
            secondSignDetected = className;
            alertSystem.alertBasedOnContext(previousSign, secondSignDetected);
            previousSign = secondSignDetected;
            secondSignDetected = className;
            alertSystem.alertBasedOnContext(previousSign, secondSignDetected);
        }
    }



    private void speakSignName(String className) {
        if (textToSpeech != null && !textToSpeech.isSpeaking()) {
            textToSpeech.speak(className, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private String formatToDrawableName(String className) {
        // Convert to lowercase and replace spaces with underscores
        String formattedName = className.toLowerCase().replace(" ", "_");

        // Replace slashes with an empty string (to remove /) and remove all non-alphanumeric characters except underscore and digits
        formattedName = formattedName.replace("/", "").replaceAll("[^a-z0-9_]", "");

        // Specifically handle "km/h" to become "kmh"
        formattedName = formattedName.replace("km_h", "kmh");

        return formattedName;
    }


    private String copyResourceToInternalStorage(int resourceId, String outputFileName) {
        try {
            Resources resources = getResources();
            InputStream inputStream = resources.openRawResource(resourceId);
            File outputFile = new File(getFilesDir(), outputFileName);
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            Log.d(TAG, "Resource copied to: " + outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error copying resource: " + e.getMessage());
            return null;
        }
    }

    private void parseDetectionJSON(String jsonPath) {
        frameDetections = new HashMap<>();
        try {
            InputStream inputStream = new FileInputStream(jsonPath);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            JSONArray framesArray = new JSONArray(jsonString);

            for (int i = 0; i < framesArray.length(); i++) {
                JSONObject frameData = framesArray.getJSONObject(i);
                int frameNumber = frameData.getInt("frame");
                JSONArray detections = frameData.getJSONArray("detections");
                frameDetections.put(frameNumber, detections);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing detection JSON: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoCapture != null) {
            videoCapture.release();
        }
        if (handler != null) {
            handler.removeCallbacks(videoRunnable);
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        handler2.removeCallbacksAndMessages(null);
    }
}
