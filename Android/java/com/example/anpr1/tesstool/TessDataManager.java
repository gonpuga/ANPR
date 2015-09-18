package com.example.anpr1.tesstool;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Gonzalo on 06/07/2015.
 * Gestiona los ficheros de reconocimiento desde la memoria del dispositivo
 */
public class TessDataManager {
    //para depuración
    static final String TAG = "DBG_" + TessDataManager.class.getName();
    //directorio creado para almanecar los datos
    private static final String tessdir = "tesseract";
    //subdirectorio derivado desde assets
    private static final String subdir = "tessdata";
    //nombre el fichero de reconocimiento
    private static final String filename = "eng.traineddata";
    //dirección al al fichero
    private static String trainedDataPath;
    //dirección a la raiz
    private static String tesseractFolder;
    //bandera para realizar la incialización una única vez
    private static boolean initiated;

    /**
     * Obtenemos el directorio padre
     * @return
     */
    public static String getTesseractFolder() {
        return tesseractFolder;
    }

    /**
     * Devuelve la ruta al fichero de datos entrenado
     * @return
     */
    public static String getTrainedDataPath(){
        return initiated ? trainedDataPath : null;
    }


    /**
     * Carga los ficheros en la memoria del dispositivo => paso necesario
     * @param context
     */
    public static void initTessTrainedData(Context context){

        if(initiated)
            return;

        File appFolder = context.getFilesDir();
        File folder = new File(appFolder, tessdir);
        if(!folder.exists())
            folder.mkdir();
        tesseractFolder = folder.getAbsolutePath();

        File subfolder = new File(folder, subdir);
        if(!subfolder.exists())
            subfolder.mkdir();

        File file = new File(subfolder, filename);
        trainedDataPath = file.getAbsolutePath();
        Log.d(TAG, "Ruta al fichero de datos entrenados: " + trainedDataPath);

        if(!file.exists()) {

            try {
                FileOutputStream fileOutputStream;
                byte[] bytes = readTrainingData(context);
                if (bytes == null)
                    return;
                fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(bytes);
                fileOutputStream.close();
                initiated = true;
                Log.d(TAG, "Fichero de datos entrenados listo");
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Error al cargar el fichero de datos entrenado\n" + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "Error al cargar el fichero de datos entrenado\n" + e.getMessage());
            }
        }
        else{
            initiated = true;
        }
    }

    /**
     * Leemos los datos desde el fichero almacenado en nuestra app
     * @param context
     * @return
     */
    private static byte[] readTrainingData(Context context){

        try {
            //con esto trabajamos con raw (fichero directamente sobre raw)
            /*InputStream fileInputStream = context.getResources()
                    .openRawResource(R.raw.eng_traineddata);*/

            //Log.d(TAG, "Debo abrir el fichero: " + filename);

            //con esto trabajamos con assets
            InputStream fileInputStream = context.getAssets().open(subdir+"/"+filename);


            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            byte[] b = new byte[1024];

            int bytesRead;

            while (( bytesRead = fileInputStream.read(b))!=-1){
                bos.write(b, 0, bytesRead);
            }

            fileInputStream.close();

            return bos.toByteArray();

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Error leyendo el fichero de datos de entrenamiento\n" + e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Error leyendo el fichero de datos de entrenamiento\n" + e.getMessage());
        }

        return null;
    }
}