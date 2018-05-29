package com.suneetsrivastava.mlkit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAPTURE_IMAGE =2;
    private String cameraId = "suneet";
    Bitmap image;
    private Button clickMe;
    private ImageView imageView;
    private static String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        clickMe = findViewById(R.id.clickMe);
        clickMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickImageFromGallery();
            }
        });



//        int rotation = getRotationCompensation(cameraId,this,this)

    }

    private void takePic(){
        Intent takePic = new Intent (MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePic.resolveActivity(getPackageManager())!=null){
            startActivityForResult(takePic,REQUEST_CAPTURE_IMAGE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_CAPTURE_IMAGE && resultCode == RESULT_OK){
            getCameraId();
            Bundle extras = data.getExtras();
//            imageObject = (Image) extras.get("data");
            image = (Bitmap) extras.get("data");
            imageView.setImageBitmap(image);
            try {
                int rotation = getRotationCompensation(cameraId,this,this);
                Log.e(TAG, "onCreate: "+cameraId+ " "+rotation );
                startImageProcesssing(cameraId,rotation);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        if(requestCode== 1 && resultCode == RESULT_OK && data!=null  ){
            Uri uri = data.getData();
            try {
                image = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
                imageView.setImageBitmap(image);
                startImageProcesssing(cameraId,0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int getRotationCompensation(String cameraId, Activity activity, Context context)
            throws CameraAccessException {

        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);


        CameraManager cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        int sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION);
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360;

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        int result;
        switch (rotationCompensation) {
            case 0:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                result = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                result = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                result = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                Log.e("TAG", "Bad rotation value: " + rotationCompensation);
        }
        return result;
    }

    private void getCameraId() {
        CameraManager cameraManager = (CameraManager) this.getSystemService(CAMERA_SERVICE);

        try {
            for(String s: cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(s);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_BACK){
                    cameraId = s;
                    break;
                }

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }
    private void startImageProcesssing(String cameraId, int rotation){

        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(image);
        FirebaseVisionTextDetector textDetector = FirebaseVision.getInstance().getVisionTextDetector();
        textDetector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                        List<FirebaseVisionText.Block> blocks = firebaseVisionText.getBlocks();
                        if (blocks.size() == 0) {
                            Toast.makeText(MainActivity.this, "No Text", Toast.LENGTH_SHORT).show();
                        }

                        for (int i = 0; i < blocks.size(); i++) {
                            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
                            Log.e(TAG, "onSuccess: "+lines.get(i) );
//
                        }
                    }
                })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure: " );
                e.printStackTrace();
            }
        })
        ;

    }

    void pickImageFromGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(intent,"Select Image"),1);
    }
}
