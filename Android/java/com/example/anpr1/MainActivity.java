package com.example.anpr1;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.Toast;

import com.example.anpr1.imagenes.GridViewActivity;
import com.example.anpr1.imagenes.GridViewImageAdapter;
import com.example.anpr1.opencv.Procesador;
import com.example.anpr1.tesstool.TessAsyncEngine;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    //etiqueta para depuración
    private static final String TAG = "DBG_" + MainActivity.class.getName();
    private CameraBridgeViewBase cameraView;

    //gestión de la cámara del dispositivo
    private int indiceCamara; // 0-> camara trasera; 1-> camara frontal
    private int cam_anchura = 320;// resolucion deseada de la imagen
    private int cam_altura = 240;
    private static final String STATE_CAMERA_INDEX = "cameraIndex";

    //procesar desde cámara o desde ficheros
    private int tipoEntrada = 0;  // 0 -> cámara  1 -> ficheros
    Mat imagenRecurso_;
    boolean recargarRecurso = false;
    private static final int REQUEST_CODE_IMAGE = 1234;
    private static final int REQUEST_CODE_IMAGE_GALERY = 2345;
    private String procesarImagen;

    //para guardar las imagenes resultantes
    private boolean guardarSiguienteImagen = false;

    //procesamiento de la imagen
    private Procesador procesador;
    private int algoritmo=0;

    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV se cargo correctamente");
                    // fijamos la resolución una vez la libreria openCV se ha cargado
                    cameraView.setMaxFrameSize(cam_anchura, cam_altura);
                    cameraView.enableView();
                    break;
                default:
                    Log.e(TAG, "OpenCV no se cargo");
                    Toast.makeText(MainActivity.this, "OpenCV no se cargo",
                            Toast.LENGTH_LONG).show();
                    finish();
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        cameraView = (CameraBridgeViewBase) findViewById(R.id.vista_camara);
        cameraView.setCvCameraViewListener(this);

        // comprobamos si tenemos almacenado un índice de camara
        // por si tenemos que recrear la actividad
        if (savedInstanceState != null) {
            indiceCamara = savedInstanceState.getInt(STATE_CAMERA_INDEX, 0);
        } else {
            indiceCamara = 0;
        }
        cameraView.setCameraIndex(indiceCamara);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraView != null)
            cameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, loaderCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraView != null)
            cameraView.disableView();
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the current camera index.
        savedInstanceState.putInt(STATE_CAMERA_INDEX, indiceCamara);
        super.onSaveInstanceState(savedInstanceState);
    }

    //Interface CvCameraViewListener2
    public void onCameraViewStarted(int width, int height) {
        // No todas las resoluciones son posibles. Nosotros solicitamos una
        // y posteriormente android nos asigna la más similar de entre las
        // posibles
        cam_altura = height; //Estas son las que se usan de verdad
        cam_anchura = width;

        // Inicializamos el procesador de la imagen
        // Esto debe realizarse en esta función porque es en esa función cuando
        // tenemos cargada la librería OpenCV. Realizar la inicialización en el
        // constructor de la actividad daría un error si en dicho constructor se
        // intenta hacer cualquier cosa de OpenCV.

        //Le pasamos una referencia a la actividad para mostrar la salida
        //al menos de momento
        if(procesador==null)
            procesador = new Procesador(this);

    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat entrada;
        if (tipoEntrada == 0) {
            //estamos trabajando con la cámara
            entrada = inputFrame.rgba();
        } else {
            //trabajamos con ficheros

            //si no estamos mostrando/trabajando sobre la imagen
            //creamos el intent para su selección y lo lanzamos esperando respuesta
            if (procesarImagen == null) {
                if (tipoEntrada == 1) {
                    Intent i = new Intent(this, GridViewActivity.class);
                    startActivityForResult(i, REQUEST_CODE_IMAGE);
                } else {
                    //tipo entrada=2 => galeria
                    //leemos la imagen desde las galerias con un intent
                    Intent intent = new Intent();
                    // mostramos sólo imágenes, nada de videos
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    // permitimos elegir entre las distintas opciones
                    startActivityForResult(Intent.createChooser(intent, "Seleccione Imagen"), REQUEST_CODE_IMAGE_GALERY);
                }
            }

            //debemos recargar la imagen siempre que sea necesario
            if (recargarRecurso == true && procesarImagen != null) {
                imagenRecurso_ = new Mat();
                if (tipoEntrada == 1) {
                    //leemos la imagen desde el directorio de assets
                    Bitmap bitmap = getBitmapFromAssets(procesarImagen, cam_anchura, cam_altura);
                    //Convierte el recurso a una Mat de OpenCV
                    Utils.bitmapToMat(bitmap, imagenRecurso_);
                } else {
                    Bitmap bitmap = getBitmapFromGalery(procesarImagen, cam_anchura, cam_altura);
                    //Convierte el recurso a una Mat de OpenCV
                    Utils.bitmapToMat(bitmap, imagenRecurso_);
                }
                recargarRecurso = false;
            }

            //si no tenemos la imagen, ya sea porque la estamos seleccionando del gridview
            //o por otra causa, usamos como entrada la de cámara, para evitar detenciones
            //y darle más estabilidad al programa
            if (imagenRecurso_ != null)
                entrada = imagenRecurso_;
            else
                entrada = inputFrame.rgba();//para curarnos en salud
        }

        // Dibujamos una marca en la parte superior izquierda
        // para ubicar en dicha orientación el teléfono y evitar
        // problemas por imágenes invertidas

        // Independientemente de la orientación de la CÁMARA,
        // (siempre que este en landscape) podremos procesar
        //  Para ello debemos rotar la imagen convenientemente
        if (tipoEntrada == 0) {
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            switch (rotation) {
                case Surface.ROTATION_270:
                    // "SCREEN_ORIENTATION_REVERSE_LANDSCAPE" => modo normal
                    Log.d(TAG, "Apaisado invertido");
                    // rotacion de 180º en sentido horario
                    Core.flip(entrada.clone(), entrada, -1);
                    break;
            }
        }

        Mat esquina = entrada.submat(0, 10, 0, 10); // Arriba-izquierda
        esquina.setTo(new Scalar(255, 255, 255));

        // mandamos la imagen al procesador => dependiendo del tipo de algoritmo
        Mat salida=new Mat();
        switch (algoritmo)
        {
            case 0://svm
                salida=procesador.procesarSVM(entrada);
                break;
            case 1://tesseract sin segmentar
                salida=procesador.procesarTesseract(entrada);
                break;
            case 2://tesseract segmentado
                salida=procesador.procesarSVM(entrada);
                break;
            default://combinados
                salida=procesador.procesarSVM(entrada);
                break;
        }
        //Mat salida = procesador.procesa(entrada);


        //si nos piden guardar la imagen resultante lo hacemos
        if (guardarSiguienteImagen) {// Para foto salida debe ser rgba
            takePhoto(entrada, salida);
            guardarSiguienteImagen = false;
        }


        //Las imágenes que tenemos en recursos pueden tener un tamaño que no coincida con el
        //de la captura de la cámara. En este caso, cuando devolvamos el resultado es necesario
        //que éste tenga el mismo tamaño que el real de la captura de la cámara
        //con lo siquiente realizamos el escalado neceario
        if (tipoEntrada > 0)
            Imgproc.resize(salida, salida, new Size(cam_anchura, cam_altura));

        // liberamos la memoria
        System.gc();

        //devolvemos la imagen
        return salida;
    }

    /**
     * Algunos dispositivos pueden no tener tecla para mostrar el menú.
     * Lo arreglamos con est.
     *
     * @param event
     * @return
     */
    public boolean onTouchEvent(MotionEvent event) {
        openOptionsMenu();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*case R.id.cambiarCamara:
                //cambiamos entre la cámara frontal y la trasera
                indiceCamara++;
                if (indiceCamara == Camera.getNumberOfCameras()) {
                    indiceCamara = 0;
                }
                recreate();// requiere un ApiMin=11
                break;*/
            case R.id.resolucion_800x600:
                cam_anchura = 800;
                cam_altura = 600;
                reiniciarResolucion();
                break;
            case R.id.resolucion_640x480:
                cam_anchura = 640;
                cam_altura = 480;
                reiniciarResolucion();
                break;
            case R.id.resolucion_320x240:
                cam_anchura = 320;
                cam_altura = 240;
                reiniciarResolucion();
                break;
            case R.id.entrada_camara:
                tipoEntrada = 0;
                reiniciarResolucion();
                //descargamos la imagen de memoria
                procesarImagen = null;
                break;
            case R.id.entrada_ficheros:
                tipoEntrada = 1;
                recargarRecurso = true;
                //descargamos la imagen de memoria
                procesarImagen = null;
                //detenemos el ocr (para no mezclar resultados con un asyntask pendiente)
                procesador.detenerReconocedor();
                break;
            case R.id.entrada_galeria:
                tipoEntrada = 2;
                recargarRecurso = true;
                //descargamos la imagen de memoria
                procesarImagen = null;
                //detenemos el ocr
                procesador.detenerReconocedor();
                break;
            case R.id.guardar_imagenes:
                guardarSiguienteImagen = true;
                break;
            case R.id.entrada_svm:
                algoritmo=0;
                break;
            case R.id.entrada_tesseract:
                algoritmo=1;
                break;
            /*case R.id.entrada_tesseract_segmentado:
                algoritmo=2;
                break;
            case R.id.entrada_combinados:
                algoritmo=3;
                break;
            /*case R.id.preferencias:
                Intent i = new Intent(this, Preferencias.class);
                startActivity(i);
                break;*/
        }
        String msg = "W=" + Integer.toString(cam_anchura) + " H= "
                + Integer.toString(cam_altura) + " Cam= "
                + Integer.toBinaryString(indiceCamara);
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
        return true;
    }

    /**
     * Reinicia la resolución de la cámara
     */
    public void reiniciarResolucion() {
        cameraView.disableView();
        cameraView.setMaxFrameSize(cam_anchura, cam_altura);
        cameraView.enableView();
    }

    /**
     * Guarda la imágenes origen y salida del procesado
     *
     * @param input  Imagen original
     * @param output Imagen procesada
     */
    private void takePhoto(final Mat input, final Mat output) {
        // Determina la ruta para crear los archivos
        final long currentTimeMillis = System.currentTimeMillis();
        final String appName = getString(R.string.app_name);
        final String galleryPath = Environment
                .getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString();
        final String albumPath = galleryPath + "/" + appName;
        final String photoPathIn = albumPath + "/In_" + currentTimeMillis
                + ".png";
        final String photoPathOut = albumPath + "/Out_" + currentTimeMillis
                + ".png";
        // Asegurarse que el directorio existe
        File album = new File(albumPath);
        if (!album.isDirectory() && !album.mkdirs()) {
            Log.e(TAG, "Error al crear el directorio " + albumPath);
            return;
        }
        // Intenta crear los archivos
        Mat mBgr = new Mat();
        if (output.channels() == 1)
            Imgproc.cvtColor(output, mBgr, Imgproc.COLOR_GRAY2BGR, 3);
        else
            Imgproc.cvtColor(output, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
        if (!Highgui.imwrite(photoPathOut, mBgr)) {
            Log.e(TAG, "Fallo al guardar " + photoPathOut);
        }
        if (input.channels() == 1)
            Imgproc.cvtColor(input, mBgr, Imgproc.COLOR_GRAY2BGR, 3);
        else
            Imgproc.cvtColor(input, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
        if (!Highgui.imwrite(photoPathIn, mBgr))
            Log.e(TAG, "Fallo al guardar " + photoPathIn);
        mBgr.release();
        return;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_IMAGE) {
                Bundle res = data.getExtras();
                String result = res.getString("resultado");
                Log.d(TAG, "result:" + result);
                //almaceamos esta imagen para su procesado
                procesarImagen = result;
            } else if (requestCode == REQUEST_CODE_IMAGE_GALERY && data != null && data.getData() != null) {
                //imagen desde la galeria
                Uri uri = data.getData();

                //nos vamos a quedar con el path => este nuevo método funciona con kitkat
                String[] projection = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
                cursor.moveToFirst();

                Log.d(TAG, DatabaseUtils.dumpCursorToString(cursor));

                int columnIndex = cursor.getColumnIndex(projection[0]);
                procesarImagen = cursor.getString(columnIndex); // returns null
                cursor.close();
            }
        } else {
            Log.d(TAG, "El usuario ha cancelado la selección de la imagen");
            procesarImagen = null;
            tipoEntrada = 0;//volvemos a usar la cámara
        }
    }

    /*
    Obtiene una imagen desde la carpeta de assets para su procesado
     */
    public Bitmap getBitmapFromAssets(String filePath, int WIDTH, int HIGHT) {
        AssetManager assetManager = getAssets();

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
            bitmap = BitmapFactory.decodeStream(istr, null, o2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    /*
    Obtiene una imagen desde la galería del dispositivo para su procesado
     */
    public Bitmap getBitmapFromGalery(String filePath, int WIDTH, int HIGHT) {

        Bitmap bitmap = null;

        //escalamos
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, o);

        final int REQUIRED_WIDTH = WIDTH;
        final int REQUIRED_HIGHT = HIGHT;
        int scale = 1;
        while (o.outWidth / scale / 2 >= REQUIRED_WIDTH
                && o.outHeight / scale / 2 >= REQUIRED_HIGHT)
            scale *= 2;

        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        //decodificamos
        bitmap = BitmapFactory.decodeFile(filePath, o2);


        return bitmap;
    }



}
