package com.example.anpr1.imagenes;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by Gonzalo on 07/07/2015.
 * Clase para generar el gridview pesonalizado
 */
public class GridViewImageAdapter extends BaseAdapter {
    private Activity actividad;
    private ArrayList<String> filePaths = new ArrayList<String>();
    private int imageWidth;

    /**
     * Constructor de la clase
     * @param activity
     * @param filePaths
     * @param imageWidth
     */
    public GridViewImageAdapter(Activity activity, ArrayList<String> filePaths,
                                int imageWidth) {
        this.actividad = activity;
        this.filePaths = filePaths;
        this.imageWidth = imageWidth;
    }

    @Override
    public int getCount() {
        return this.filePaths.size();
    }

    @Override
    public Object getItem(int position) {
        return this.filePaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(actividad);
        } else {
            imageView = (ImageView) convertView;
        }

        // Escalamos la imagen de acuerdo al tamaño de la pantalla
        //Bitmap image = decodeFile(filePaths.get(position), imageWidth,
        //        imageWidth);

        Bitmap image = getBitmapFromAssets(filePaths.get(position), imageWidth,
                imageWidth);

        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setLayoutParams(new GridView.LayoutParams(imageWidth,
                imageWidth));
        imageView.setImageBitmap(image);

        // Onclicklistener sobre la imagen
        imageView.setOnClickListener(new OnImageClickListener(position));

        return imageView;
    }

    class OnImageClickListener implements View.OnClickListener {

        int posicion;

        // constructor
        public OnImageClickListener(int position) {
            this.posicion = position;
        }

        @Override
        public void onClick(View v) {
            // on selecting grid view image
            // launch full screen activity

            /*Intent i = new Intent(actividad, FullScreenViewActivity.class);
            i.putExtra("position", posicion);
            actividad.startActivity(i);*/

            //En este punto debemos devolver la ruta de la imagen o la imagen misma
            //Toast.makeText(actividad.getApplicationContext(), "La imagen seleccionada es: " + filePaths.get(posicion), Toast.LENGTH_LONG).show();


            //devolvemos el valor y cerramos la actividad
            Intent intent=actividad.getIntent();
            intent.putExtra("resultado", filePaths.get(posicion));
            actividad.setResult(actividad.RESULT_OK, intent);
            actividad.finish();
        }

    }

    /*
     * Redimensionamos la imagen
     * Esta función trabaja sobre ficheros ubicados en un directorio del dispositivo
     * En nuestro caso no lo usamos al trabajar sobre assets
     */
    public static Bitmap decodeFile(String filePath, int WIDTH, int HIGHT) {
        try {

            File f = new File(filePath);

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            final int REQUIRED_WIDTH = WIDTH;
            final int REQUIRED_HIGHT = HIGHT;
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_WIDTH
                    && o.outHeight / scale / 2 >= REQUIRED_HIGHT)
                scale *= 2;

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    //prueba con assets
    public Bitmap getBitmapFromAssets(String filePath, int WIDTH, int HIGHT) {
        AssetManager assetManager = this.actividad.getApplicationContext().getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        try {
            //accedemos al fichero
            istr = assetManager.open(filePath);
            //escalamos
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(istr, null, o);

            final int REQUIRED_WIDTH = WIDTH;
            final int REQUIRED_HIGHT = HIGHT;
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_WIDTH
                    && o.outHeight / scale / 2 >= REQUIRED_HIGHT)
                scale *= 2;

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            //decodificamos
            bitmap = BitmapFactory.decodeStream(istr,null, o2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }
}
