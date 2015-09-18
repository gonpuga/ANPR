package com.example.anpr1.imagenes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;

/**
 * Created by Gonzalo on 06/07/2015.
 * Clase con herramientas para trabajar con ficheros bmp
 */
public class Tools {

    /**
     * Rota un bitmap un ángulo dado
     * @param source Imagen de origen
     * @param angle Ángulo de rotación
     * @return Imagen rotada
     */
    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * Rota una imagen un ángulo dado (la imagen de origen no se ve afectada)
     * @param source Imagen a rotar
     * @param angle Ángulo de rotación
     * @return Imagen rotada
     */
    public static Bitmap preRotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.preRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
    }

    /**
     * Tipo enumerado para definir distintos escalados sobre la imagen
     * CROP: Recortar
     * FIT: Ajustar
     */
    public static enum ScalingLogic {
        CROP, FIT
    }

    /**
     * Calcula el porcentaje de escalado de la imagen de acuerdo el tipo de escalado seleccionado
     * @param srcWidth
     * @param srcHeight
     * @param dstWidth
     * @param dstHeight
     * @param scalingLogic
     * @return
     */
    public static int calculateSampleSize(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
                                          ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.FIT) {
            final float srcAspect = (float) srcWidth / (float) srcHeight;
            final float dstAspect = (float) dstWidth / (float) dstHeight;

            if (srcAspect > dstAspect) {
                return srcWidth / dstWidth;
            } else {
                return srcHeight / dstHeight;
            }
        } else {
            final float srcAspect = (float) srcWidth / (float) srcHeight;
            final float dstAspect = (float) dstWidth / (float) dstHeight;

            if (srcAspect > dstAspect) {
                return srcHeight / dstHeight;
            } else {
                return srcWidth / dstWidth;
            }
        }
    }

    /**
     * Obtiene un bitmap desde un conjunto de datos devuelto por la cámara
     * @param bytes
     * @param dstWidth
     * @param dstHeight
     * @param scalingLogic
     * @return
     */
    public static Bitmap decodeByteArray(byte[] bytes, int dstWidth, int dstHeight,
                                         ScalingLogic scalingLogic) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        options.inJustDecodeBounds = false;
        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, dstWidth,
                dstHeight, scalingLogic);
        Bitmap unscaledBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        return unscaledBitmap;
    }

    /**
     * Devuelve un rectángulo que abarca la imagen original, de acuero a su tamaño y el factor
     * de escalado aplicado
     * @param srcWidth
     * @param srcHeight
     * @param dstWidth
     * @param dstHeight
     * @param scalingLogic
     * @return
     */
    public static Rect calculateSrcRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
                                        ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.CROP) {
            final float srcAspect = (float) srcWidth / (float) srcHeight;
            final float dstAspect = (float) dstWidth / (float) dstHeight;

            if (srcAspect > dstAspect) {
                final int srcRectWidth = (int) (srcHeight * dstAspect);
                final int srcRectLeft = (srcWidth - srcRectWidth) / 2;
                return new Rect(srcRectLeft, 0, srcRectLeft + srcRectWidth, srcHeight);
            } else {
                final int srcRectHeight = (int) (srcWidth / dstAspect);
                final int scrRectTop = (int) (srcHeight - srcRectHeight) / 2;
                return new Rect(0, scrRectTop, srcWidth, scrRectTop + srcRectHeight);
            }
        } else {
            return new Rect(0, 0, srcWidth, srcHeight);
        }
    }

    /**
     * Calcula el rectángulo que abarcará la imagen resultante, de acuerdo a los tamaños y el
     * factor de escalado a aplicar.
     * @param srcWidth
     * @param srcHeight
     * @param dstWidth
     * @param dstHeight
     * @param scalingLogic
     * @return
     */
    public static Rect calculateDstRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
                                        ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.FIT) {
            final float srcAspect = (float) srcWidth / (float) srcHeight;
            final float dstAspect = (float) dstWidth / (float) dstHeight;

            if (srcAspect > dstAspect) {
                return new Rect(0, 0, dstWidth, (int) (dstWidth / srcAspect));
            } else {
                return new Rect(0, 0, (int) (dstHeight * srcAspect), dstHeight);
            }
        } else {
            return new Rect(0, 0, dstWidth, dstHeight);
        }
    }

    /**
     * Obtiene un bitmap escalado a partir de uno sin escalar.
     * @param unscaledBitmap
     * @param dstWidth
     * @param dstHeight
     * @param scalingLogic
     * @return
     */
    public static Bitmap createScaledBitmap(Bitmap unscaledBitmap, int dstWidth, int dstHeight,
                                            ScalingLogic scalingLogic) {
        Rect srcRect = calculateSrcRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(),
                dstWidth, dstHeight, scalingLogic);
        Rect dstRect = calculateDstRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(),
                dstWidth, dstHeight, scalingLogic);
        Bitmap scaledBitmap = Bitmap.createBitmap(dstRect.width(), dstRect.height(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.drawBitmap(unscaledBitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }
}
