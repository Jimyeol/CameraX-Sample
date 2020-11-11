package com.example.cameraxexample;

import android.graphics.ImageFormat;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

public class ExtendMat extends Mat {
    public static Mat toRGB(ImageProxy imageProxy) {
        Mat rgbaMat = new Mat();

        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        if (imageProxy.getFormat() == ImageFormat.YUV_420_888 && planes.length == 3) {
            int chromaPixelStride = planes[1].getPixelStride();
            if (chromaPixelStride == 2) { // Chroma channels are interleaved
                if (planes[0].getPixelStride() != 1) {
                    throw new IllegalArgumentException("panels[0].pixelStride not equals 1");
                }
                if (planes[1].getPixelStride() != 2) {
                    throw new IllegalArgumentException("panels[1].pixelStride not equals 2");
                }

                ByteBuffer yPlane = planes[0].getBuffer();
                ByteBuffer uvPlane1 = planes[1].getBuffer();
                ByteBuffer uvPlane2 = planes[2].getBuffer();
                Mat yMat = new Mat(height, width, CvType.CV_8UC1, yPlane);
                Mat uvMat1 = new Mat(height / 2, width / 2, CvType.CV_8UC2, uvPlane1);
                Mat uvMat2 = new Mat(height / 2, width / 2, CvType.CV_8UC2, uvPlane2);
                long addrDiff = uvMat2.dataAddr() - uvMat1.dataAddr();
                if (addrDiff > 0) {
                    if (addrDiff != 1L) {
                        throw new IllegalArgumentException("addrDiff not equals 1");
                    }
                    Imgproc.cvtColorTwoPlane(yMat, uvMat1, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV12);
                    Imgproc.cvtColor(rgbaMat, rgbaMat, Imgproc.COLOR_RGBA2RGB);
                }
                else {
                    if (addrDiff != -1L) {
                        throw new IllegalArgumentException("addrDiff not equals -1");
                    }
                    Imgproc.cvtColorTwoPlane(yMat, uvMat2, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV21);
                    Imgproc.cvtColor(rgbaMat, rgbaMat, Imgproc.COLOR_RGBA2RGB);
                }
            }
            else {
                byte[] yuvBytes = new byte[width * (height + height / 2)];
                ByteBuffer yPlane = planes[0].getBuffer();
                ByteBuffer uPlane = planes[1].getBuffer();
                ByteBuffer vPlane = planes[2].getBuffer();

                yPlane.get(yuvBytes, 0, width * height);

                int chromaRowStride = planes[1].getRowStride();
                int chromaRowPadding = chromaRowStride - width / 2;

                int offset = width * height;

                if (chromaRowPadding == 0) {
                    // When the row stride of the chroma channels equals their width, we can copy
                    // the entire channels in one go
                    uPlane.get(yuvBytes, offset, width * height / 4);
                    offset += width * height / 4;
                    vPlane.get(yuvBytes, offset, width * height / 4);
                } else {
                    // When not equal, we need to copy the channels row by row
                    for (int i = 0; i <height / 2; i++) {
                        uPlane.get(yuvBytes, offset, width / 2);
                        offset += width / 2;
                        if (i < height / 2 - 1) {
                            uPlane.position(uPlane.position() + chromaRowPadding);
                        }
                    }
                    for (int i = 0; i <height / 2; i++) {
                        vPlane.get(yuvBytes, offset, width / 2);
                        offset += width / 2;
                        if (i < height / 2 - 1) {
                            vPlane.position(vPlane.position() + chromaRowPadding);
                        }
                    }
                }

                Mat yuvMat = new Mat(height + height / 2, width, CvType.CV_8UC1);
                yuvMat.put(0, 0, yuvBytes);
                Imgproc.cvtColor(yuvMat, rgbaMat, Imgproc.COLOR_YUV2RGBA_I420, 4);
                Imgproc.cvtColor(rgbaMat, rgbaMat, Imgproc.COLOR_RGBA2RGB);
            }
        }
        else {
            throw new IllegalArgumentException("src is not YUV_420_888 or planes size is not 3");
        }


        return rgbaMat;
    }

    public static void autoBrightnessAndContrast(Mat src, Mat dst) {
        Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(src);
        double inputRange = minMaxLocResult.maxVal-minMaxLocResult.minVal;
        int histSize = 256;
        double alpha = (histSize-1) / inputRange;
        double beta = -minMaxLocResult.minVal*alpha;
        Core.convertScaleAbs(src, dst, alpha, beta);
    }
}
