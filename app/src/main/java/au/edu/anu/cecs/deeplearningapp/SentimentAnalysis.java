package au.edu.anu.cecs.deeplearningapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class SentimentAnalysis extends AppCompatActivity {
    Button returnBut, analyzeBut;
    EditText newComment;
    TextView resultText;
    ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentiment_analysis);

        newComment = findViewById(R.id.new_comment);
        scrollView = findViewById(R.id.scroll_view);
        resultText = findViewById(R.id.result_text_view);

        returnBut = findViewById(R.id.return_button);
        returnBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SentimentAnalysis.this, MainActivity.class);
                startActivity(intent);
            }
        });

        analyzeBut = findViewById(R.id.analyze_button);
        analyzeBut.setOnClickListener((View v) -> {
            classify(newComment.getText().toString());
        });
    }

    /** Send input text to TextClassificationClient and get the classify messages. */
    private void classify(final String text) {
//        executorService.execute(
//                () -> {
//                    // TODO 7: Run sentiment analysis on the input text
//
//                    // TODO 8: Convert the result to a human-readable text
//                    String textToShow = "Dummy classification result.\n";
//
//                    // Show classification result on screen
//                    showResult(textToShow);
//                });
    }

    /** Show classification result on the screen. */
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
}