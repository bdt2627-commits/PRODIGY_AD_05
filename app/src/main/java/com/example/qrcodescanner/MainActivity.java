package com.example.qrcodescanner;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.*;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    PreviewView previewView;
    TextView resultText;
    Button openButton;
    String scannedData = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        resultText = findViewById(R.id.resultText);
        openButton = findViewById(R.id.openButton);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        }

        openButton.setOnClickListener(v -> {
            if (scannedData.startsWith("http")) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(scannedData)));
            }
        });
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                BarcodeScanner scanner =
                        BarcodeScanning.getClient(
                                new BarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                        .build());

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                        imageProxy -> {
                            if (imageProxy.getImage() != null) {
                                InputImage image = InputImage.fromMediaImage(
                                        imageProxy.getImage(),
                                        imageProxy.getImageInfo().getRotationDegrees());

                                scanner.process(image)
                                        .addOnSuccessListener(barcodes -> {
                                            for (Barcode barcode : barcodes) {
                                                scannedData = barcode.getRawValue();
                                                resultText.setText(scannedData);

                                                if (scannedData.startsWith("http")) {
                                                    openButton.setVisibility(Button.VISIBLE);
                                                }
                                            }
                                        })
                                        .addOnCompleteListener(task -> imageProxy.close());
                            }
                        });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.bindToLifecycle(this, cameraSelector,
                        preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }
}
