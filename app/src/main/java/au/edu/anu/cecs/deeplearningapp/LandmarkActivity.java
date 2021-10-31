package au.edu.anu.cecs.deeplearningapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class LandmarkActivity extends AppCompatActivity {
    private Button returnBut, analyzeBut, selectBut;
    private TextView resultText, imgUri;
    private ScrollView scrollView;
    private ImageView imageView;
    private FirebaseFunctions mFunctions;
    private Bitmap bitmap;
    private Uri filePath;

    private final int PICK_IMAGE_REQUEST = 21;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landmark);

        scrollView = findViewById(R.id.lr_scroll_view);
        resultText = findViewById(R.id.lr_result_text);
        imageView = findViewById(R.id.uploaded_img);

        // Initialize an instance of Cloud Functions:
        mFunctions = FirebaseFunctions.getInstance();

        // Select an image to analyze
        selectBut = findViewById(R.id.select_image);
        imgUri = findViewById(R.id.image_uri);
        selectBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });


        // Return the main activity
        returnBut = findViewById(R.id.lr_return_button);
        returnBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LandmarkActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // Recognize landmarks in the provided image
        analyzeBut = findViewById(R.id.lr_analyze_button);
        analyzeBut.setOnClickListener((View v) -> {
            try {
                recognize();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Select Image from user's Phone.
     */
    private void selectImage() {
        // Defining Implicit Intent to mobile gallery
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(
                Intent.createChooser(
                        intent,
                        "Select Image from here..."),
                PICK_IMAGE_REQUEST);
    }

    /**
     * When the image is selected, show the image in a image view to the user.
     */
    @Override
    public void onActivityResult(int requestCode,
                                 int resultCode,
                                 Intent data) {

        super.onActivityResult(requestCode,
                resultCode,
                data);

        // checking request code and result code
        // if request code is PICK_IMAGE_REQUEST and
        // resultCode is RESULT_OK
        // then set image in the image view
        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            // Get the Uri of data
            filePath = data.getData();
            imgUri.setText(filePath.toString());

            try {
                // Get the image as a Bitmap object
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void recognize() throws IOException {
        if (bitmap == null) {
            return;
        }
        // Scale down bitmap size
        bitmap = scaleBitmapDown(bitmap, 640);

        // Convert bitmap to base64 encoded string
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        String base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        // Create json request to cloud vision
        JsonObject request = new JsonObject();
        // Add image to request
        JsonObject image = new JsonObject();
        image.add("content", new JsonPrimitive(base64encoded));
        request.add("image", image);
        //Add features to the request
        JsonObject feature = new JsonObject();
        feature.add("maxResults", new JsonPrimitive(5));
        feature.add("type", new JsonPrimitive("LANDMARK_DETECTION"));
        JsonArray features = new JsonArray();
        features.add(feature);
        request.add("features", features);

        annotateImage(request.toString())
                .addOnCompleteListener(new OnCompleteListener<JsonElement>() {
                    @Override
                    public void onComplete(@NonNull Task<JsonElement> task) {
                        if (!task.isSuccessful()) {
                            // Task failed with an exception
                            Log.d("Recognize", "Failure", task.getException());
                        } else {
                            // Task completed successfully
                            Log.d("Recognize", "Success");
                            StringBuilder result = new StringBuilder();

                            for (JsonElement label : task.getResult().getAsJsonArray().get(0).getAsJsonObject().get("landmarkAnnotations").getAsJsonArray()) {
                                JsonObject labelObj = label.getAsJsonObject();
                                String landmarkName = labelObj.get("description").getAsString();
                                String entityId = labelObj.get("mid").getAsString();
                                float score = labelObj.get("score").getAsFloat();

                                result.append("Prediction ---\n")
                                        .append("Description: " + landmarkName + "\n")
                                        .append("Entity ID: " + entityId + "\n")
                                        .append("Prediction Score: " + score + "\n");

                                JsonObject bounds = labelObj.get("boundingPoly").getAsJsonObject();
                                // Multiple locations are possible, e.g., the location of the depicted
                                // landmark and the location the picture was taken.
                                JsonElement loc = labelObj.get("locations").getAsJsonArray().get(0);
                                JsonObject latLng = loc.getAsJsonObject().get("latLng").getAsJsonObject();
                                double latitude = latLng.get("latitude").getAsDouble();
                                double longitude = latLng.get("longitude").getAsDouble();

                                result.append("Latitude: " + latitude + "\n")
                                        .append("Longitude: " + longitude + "\n");
                                result.append("\n");
                                // Only use the top prediction
                                break;
                            }

                            resultText.setText(result.toString());
                        }
                    }
                });
    }

    /** scale down the image to save on bandwidth **/
    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private Task<JsonElement> annotateImage(String requestJson) {
        return mFunctions
                .getHttpsCallable("annotateImage")
                .call(requestJson)
                .continueWith(new Continuation<HttpsCallableResult, JsonElement>() {
                    @Override
                    public JsonElement then(@NonNull Task<HttpsCallableResult> task) {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        return JsonParser.parseString(new Gson().toJson(task.getResult().getData()));
                    }
                });
    }
}