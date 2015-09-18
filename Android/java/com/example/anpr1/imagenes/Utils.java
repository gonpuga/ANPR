package com.example.anpr1.imagenes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Gonzalo on 07/07/2015.
 * Clase con funciones de apoyo al picker de imágenes
 */
public class Utils {
    private Context contexto;

    // constructor
    public Utils(Context context) {
        this.contexto = context;
    }

    // Leemos los paths de las imágenes desde memoria
    public ArrayList<String> getFilePaths() {
        /*

        //Esto es para trabajar con un directorio de memoria
        ArrayList<String> filePaths = new ArrayList<String>();

        File directory = new File(
                android.os.Environment.getExternalStorageDirectory()
                        + File.separator + PickerConf.PHOTO_ALBUM);

        // Comprobamos que exista el directorio
        if (directory.isDirectory()) {
            // Obtenemos un listado con los paths
            File[] listFiles = directory.listFiles();

            // Vemos si hay imágenes contenidas en el directorio
            if (listFiles.length > 0) {

                // Recorremos todos los ficheros
                for (int i = 0; i < listFiles.length; i++) {

                    // Obtenemos su path
                    String filePath = listFiles[i].getAbsolutePath();

                    // Sólo los almacenamos si tiene la extensión correcta
                    if (IsSupportedFile(filePath)) {
                        filePaths.add(filePath);
                    }
                }
            } else {
                // El directorio de imágenes está vacío
                Toast.makeText(
                        contexto,
                        PickerConf.PHOTO_ALBUM
                                + " esta vacío. Por favor, cargue las imágenes en ese directorio!",
                        Toast.LENGTH_LONG).show();
            }

        } else {
            AlertDialog.Builder alert = new AlertDialog.Builder(contexto);
            alert.setTitle("Error!");
            alert.setMessage("El directorio " + PickerConf.PHOTO_ALBUM
                    +" no es correcto!\nPor favor, crea un directorio denominado "
                    + PickerConf.PHOTO_ALBUM + " en memoria");
            alert.setPositiveButton("OK", null);
            alert.show();
        }*/

        //trabajamos con assets
        ArrayList<String> filePaths = new ArrayList<String>();
        //accedemos al assets
        AssetManager assetManager = contexto.getAssets();
        //obtenemos el listado de ficheros del directorio indicado
        try {
            String[] files = assetManager.list(PickerConf.PHOTO_ALBUM);
            //guardamos la ruta a los ficheros
            for (String strImageName : files) {
                filePaths.add(PickerConf.PHOTO_ALBUM + File.separator + strImageName);
                Log.d("Carga", "Fichero cargado: "+PickerConf.PHOTO_ALBUM + File.separator + strImageName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return filePaths;
    }

    // Comprobamos la extensión del fichero se adapte a nuestras necesidades
    private boolean IsSupportedFile(String filePath) {
        String ext = filePath.substring((filePath.lastIndexOf(".") + 1),
                filePath.length());

        if (PickerConf.FILE_EXTN
                .contains(ext.toLowerCase(Locale.getDefault())))
            return true;
        else
            return false;

    }

    /*
     * Obtnemos el ancho de la pantalla para dividirla con el grid de imágenes
     */
    public int getScreenWidth() {
        int columnWidth;
        WindowManager wm = (WindowManager) contexto
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        final Point point = new Point();
        try {
            display.getSize(point);
        } catch (java.lang.NoSuchMethodError ignore) { // dispositivos con api inferior a 9
            point.x = display.getWidth();
            point.y = display.getHeight();
        }
        columnWidth = point.x;
        return columnWidth;
    }
}
