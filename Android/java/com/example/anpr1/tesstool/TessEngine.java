package com.example.anpr1.tesstool;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

/**
 * Created by Gonzalo on 06/07/2015.
 * Clase para el reconocimiento ocr con tesseract
 */
public class TessEngine {
    static final String TAG = "DBG_" + TessEngine.class.getName();

    private Context context;

    /**
     * Construstor de la clase
     * @param context
     */
    private TessEngine(Context context){
        this.context = context;
    }

    /**
     * Crear el reconocedor
     * @param context
     * @return
     */
    public static TessEngine Generate(Context context) {
        return new TessEngine(context);
    }

    /**
     * Detecta el texto contenido en una imagen.
     * @param bitmap Imagen con el texto a reconocer
     * @return Texto reconocido
     */
    public String detectText(Bitmap bitmap) {
        Log.d(TAG, "Inicializando TessBaseApi");
        TessDataManager.initTessTrainedData(context);
        TessBaseAPI tessBaseAPI = new TessBaseAPI();
        String path = TessDataManager.getTesseractFolder();
        Log.d(TAG, "Directorio con el diccionario de reconocimiento: " + path);
        tessBaseAPI.setDebug(true);
        tessBaseAPI.init(path, "eng");
        //Números y mayúsculas => contenido de matrículas
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "1234567890BCDEFGHYJKLMNPQRSTVWXYZ");
        //el resto de caracteres los obviamos
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "AIOU!@#$%^&*()_+=-qwertyuiop[]}{" +
                "asdfghjkll;L:'\"\\|~`xcvbnm,./<>?");
        //usamos los dos algoritmos disponibles y los combina
        tessBaseAPI.setPageSegMode(TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED);
        //mejoras segun la red => 17/05/2015
        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);//???
        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);//detección de línea => lo mejora algo

        Log.d(TAG, "Fin de la configuración de TessEngine");
        Log.d(TAG, "Analizando el bitmap");
        //supuestamente convirtiendo a escala de grises da mejores resultados => 17/07/2015
        tessBaseAPI.setImage(toGrayscale(bitmap));
        String inspection = tessBaseAPI.getUTF8Text();
        Log.d(TAG, "Resultado: " + inspection);
        //detenemos el reconocedor
        tessBaseAPI.end();
        //recogemos memoria
        System.gc();
        return inspection;
    }

    //prueba
    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    //esto supuestamente es un algoritmo de treshold muy lento
    public static Bitmap createBlackAndWhite(Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();
        // create output bitmap
        Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
        // color information
        int A, R, G, B;
        int pixel;

        // scan through all pixels
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                // get pixel color
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);
                int gray = (int) (0.2989 * R + 0.5870 * G + 0.1140 * B);

                // use 128 as threshold, above -> white, below -> black
                if (gray > 128)
                    gray = 255;
                else
                    gray = 0;
                // set new pixel color to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, gray, gray, gray));
            }
        }
        return bmOut;
    }
}
