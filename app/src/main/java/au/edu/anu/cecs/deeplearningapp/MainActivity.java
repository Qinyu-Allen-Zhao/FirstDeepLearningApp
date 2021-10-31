package au.edu.anu.cecs.deeplearningapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button senAna, landmark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Go to the sentiment analysis module
        senAna = findViewById(R.id.senAna);
        senAna.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SentimentAnalysis.class);
                startActivity(intent);
            }
        });

        landmark = findViewById(R.id.lr_button);
        landmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, LandmarkActivity.class);
                startActivity(intent);
            }
        });
    }
}