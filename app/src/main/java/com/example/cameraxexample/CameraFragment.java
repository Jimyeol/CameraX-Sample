package com.example.cameraxexample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {

    NavController nav = null;
    ImageButton btnCapture = null;
    PreviewView viewFinder = null;
    ImageView snapShot = null;

    private static final int REQUEST_CODE_CAMERA_PERMISSION = 0x0101;
    private ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getContext()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public CameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, getContext(), mLoaderCallback);
        }
        else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnCapture = (ImageButton)view.findViewById(R.id.btnCapture);
        viewFinder = (PreviewView)view.findViewById(R.id.view_finder);
        snapShot = (ImageView)view.findViewById(R.id.snapShot);



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[] { Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA_PERMISSION);
                return;
            }
        }

        viewFinder.post(this::init);

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.btnCapture:
//                        nav = Navigation.findNavController(view);
//                        NavDirections directions = CameraFragmentDirections.actionCameraFragmentToImageFragment().setMyName("finish");
//                        nav.navigate(directions);
                        break;
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewFinder.post(this::init);
            }
        }
    }

    private void init() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this.getContext());
        cameraProviderListenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();

                DisplayMetrics metrics = new DisplayMetrics();
                viewFinder.getDisplay().getRealMetrics(metrics);

                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();


                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                        .setTargetResolution(new android.util.Size(1080, 1920))
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, new imageAnalyzer());

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(this.getActivity(), cameraSelector, imageAnalysis, preview);



            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    private class imageAnalyzer implements ImageAnalysis.Analyzer {
        @SuppressLint({"UnsafeExperimentalUsageError", "RestrictedApi"})
        @Override
        public void analyze(@NonNull ImageProxy image) {
            if(image.getImage() == null) {
                image.close();
                return;
            }

            try{
                Mat src = ExtendMat.toRGB(image);

                // rotate image
                int rotateDegree = image.getImageInfo().getRotationDegrees();
                if (rotateDegree == 90) {
                    Core.rotate(src, src, Core.ROTATE_90_CLOCKWISE);
                }
                else if (rotateDegree == 180) {
                    Core.rotate(src, src, Core.ROTATE_180);
                }
                else if (rotateDegree == 270 || rotateDegree == -90) {
                    Core.rotate(src, src, Core.ROTATE_90_COUNTERCLOCKWISE);
                }

                // grayscale image
                Mat gray = new Mat();
                Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY);

                // 원본영상 brightness 조정

                CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
                clahe.apply(gray, gray);

                // 테스트로 edge 표시한 영상 화면에 표시
                setImage(snapShot, gray);

                nav = Navigation.findNavController(getView());
                NavDirections directions = CameraFragmentDirections.actionCameraFragmentToImageFragment(createBitmap(gray)).setMyImage(createBitmap(gray));
                nav.navigate(directions);



                //snapShot.setImageURI();



            }catch (Exception e) {
            }

            image.close();
        }
    }

    private Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private Bitmap createBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }

    private void setImage(ImageView target, Mat mat) {
        Bitmap bitmap = createBitmap(mat);
        getActivity().runOnUiThread(() -> target.setImageBitmap(bitmap));
    }

}