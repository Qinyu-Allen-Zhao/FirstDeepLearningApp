package au.edu.anu.cecs.deeplearningapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.modeldownloader.CustomModel;
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions;
import com.google.firebase.ml.modeldownloader.DownloadType;
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class SentimentAnalysis extends AppCompatActivity {
    private Button returnBut, analyzeBut;
    private EditText newComment;
    private TextView resultText;
    private ExecutorService executorService;
    private ScrollView scrollView;
    private NLClassifier textClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentiment);

        // Call the method to download TFLite model
        downloadModel("SentimentAnalysis");

        newComment = findViewById(R.id.new_comment);
        scrollView = findViewById(R.id.sa_scroll_view);
        resultText = findViewById(R.id.sa_result_text);

        // Return the main activity
        returnBut = findViewById(R.id.sa_return_button);
        returnBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SentimentAnalysis.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // Analyze the current comment
        analyzeBut = findViewById(R.id.sa_analyze_button);
        analyzeBut.setOnClickListener((View v) -> {
            classify(newComment.getText().toString());
        });
    }

    /**
     * Send input text to TextClassificationClient and get the classify messages.
     */
    private void classify(final String text) {
        executorService.execute(
                () -> {
                    // Run sentiment analysis on the input text
                    List<Category> results = textClassifier.classify(text);

                    // TODO 8: Convert the result to a human-readable text
                    String textToShow = "Input: " + text + "\nOutput:\n";
                    for (int i = 0; i < results.size(); i++) {
                        Category result = results.get(i);
                        textToShow += String.format("    %s: %s\n", result.getLabel(),
                                result.getScore());
                    }
                    textToShow += "---------\n";

                    // Show classification result on screen
                    showResult(textToShow);
                });
    }

    /**
     * Show classification result on the screen.
     */
    private void showResult(final String textToShow) {
        // Run on UI thread as we'll updating our app UI
        runOnUiThread(
                () -> {
                    // Append the result to the UI.
                    newComment.append(textToShow);

                    // Clear the input text.
                    newComment.getText().clear();

                    // Scroll to the bottom to show latest entry's classification result.
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                });
    }

    /**
     * Download model from Firebase ML.
     */
    private synchronized void downloadModel(String modelName) {
        CustomModelDownloadConditions conditions = new CustomModelDownloadConditions.Builder()
                .requireWifi()  // Also possible: .requireCharging() and .requireDeviceIdle()
                .build();
        FirebaseModelDownloader.getInstance()
                .getModel("SentimentAnalysis", DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions)
                .addOnSuccessListener(new OnSuccessListener<CustomModel>() {
                    @Override
                    public void onSuccess(CustomModel model) {
                        // Download complete. Depending on your app, you could enable the ML
                        // feature, or switch from the local model to the remote model, etc.

                        // The CustomModel object contains the local path of the model file,
                        // which you can use to instantiate a TensorFlow Lite interpreter.
                        File modelFile = model.getFile();
                        if (modelFile != null) {
                            // Initialize a TextClassifier with the downloaded model
                            try {
                                textClassifier = NLClassifier.createFromFile(modelFile);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // Enable predict button
                            analyzeBut.setEnabled(true);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SentimentAnalysis", "Failed to download and initialize the model. ", e);
                    Toast.makeText(
                            getApplicationContext(),
                            "Model download failed, please check your connection.",
                            Toast.LENGTH_LONG)
                            .show();
                    analyzeBut.setEnabled(false);
                });
    }
}