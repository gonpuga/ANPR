package com.example.anpr1.tesstool;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.example.anpr1.imagenes.Tools;

/**
 * Created by Gonzalo on 06/07/2015.
 * Realiza el reconocimiento del texto pero en un hilo separado
 * Es adecuado para reconocer grandes cadenas de texto, ya que el proceso de reconocimiento
 * es lento.
 */
public class TessAsyncEngine extends AsyncTask<Object, Void, String> {
    //cadena para debug
    static final String TAG = "DBG_" + TessAsyncEngine.class.getName();

    private Bitmap bmp;

    private Activity context;
    //debemos devolver el resultado al llamador para dibujarlo en su lugar correcto
    private TaskCompleted mCallback;

    //necesario para devolver los datos
    public TessAsyncEngine(TaskCompleted task)
    {
        mCallback=task;
    }


    @Override
    protected String doInBackground(Object... params) {

        try {

            if(params.length < 2) {
                Log.e(TAG, "Error al llamar al reconocedor - faltan parámetros");
                return null;
            }

            if(!(params[0] instanceof Activity) || !(params[1] instanceof Bitmap)) {
                Log.e(TAG, "Error en la llamada, recuerde(context, bitmap)");
                return null;
            }

            context = (Activity)params[0];

            bmp = (Bitmap)params[1];

            if(context == null || bmp == null) {
                Log.e(TAG, "Error: los parámetros aportados son incorrectos(context, bitmap)");
                return null;
            }

            int rotate = 0;

            if(params.length == 3 && params[2]!= null && params[2] instanceof Integer){
                rotate = (Integer) params[2];
            }

            if(rotate >= -180 && rotate <= 180 && rotate != 0)
            {
                bmp = Tools.preRotateBitmap(bmp, rotate);
                Log.d(TAG, "Se ha rotado la imagen a reconocer " + rotate + " grados");
            }

            //configuramos el reconocedor de acuerdo a los parámetros indicados en Engine
            TessEngine tessEngine =  TessEngine.Generate(context);
            //transformamos la imagen al formato requerido para su reconocimiento
            bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
            //reconocemos la imagen
            String result = tessEngine.detectText(bmp);

            Log.d(TAG, result);

            return result;

        } catch (Exception ex) {
            Log.d(TAG, "Error: " + ex + "\n" + ex.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {

        if(s == null || bmp == null || context == null)
            return;

        //Mostramos el resultado en un fragmentDialog personalizado => no implementado aquí
        /*ImageDialog.New()
                .addBitmap(bmp)
                .addTitle(s)
                .show(context.getFragmentManager(), TAG);*/

        //mostramos el resultado con un toast
        //Toast.makeText(context, "Texto reconocido: " + s, Toast.LENGTH_SHORT).show();
        //Log.d(TAG, "Texto reconocido: " +s);

        //devolvemos el texto
        mCallback.onTaskComplete(s);

        super.onPostExecute(s);
    }
}
