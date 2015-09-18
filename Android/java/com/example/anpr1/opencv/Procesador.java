package com.example.anpr1.opencv;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.example.anpr1.R;
import com.example.anpr1.svm.MatXML;
import com.example.anpr1.tesstool.TaskCompleted;
import com.example.anpr1.tesstool.TessAsyncEngine;
import com.example.anpr1.tesstool.TessEngine;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.CvSVM;
import org.opencv.ml.CvSVMParams;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Gonzalo on 08/07/2015.
 * Implementa la interfaz TaskCompleted para recoger el dato y dibujarlo
 */
public class Procesador implements TaskCompleted {
    static final String TAG = "DBG_" + Procesador.class.getName();
    private Activity padre;

    //reconocedor tesseract
    private TessAsyncEngine ocr;
    //private TessEngine ocr;
    public static String resultado;

    //reconocedor svm
    private MatXML lectorMat;
    private CvSVM reconocedor;
    final char caracteres[]={   '0','1','2','3','4','5','6','7','8'
            ,'9','A','B','C','D','E','F','G','H'
            ,'I','J','K','L','M','N','P','Q','R'
            ,'S','T','U','V','W','X','Y','Z'};


    public Procesador(Activity father) { //Constructor
        padre = father;
        resultado = "Procesando...";

        //cargamos el reconocedor svm
        //lectura de mat
        lectorMat=new MatXML();
        //accedemos al fichero de la carpeta raw
        InputStream f=father.getApplicationContext().getResources().openRawResource(R.raw.gsvm);
        //lo abrimos
        lectorMat.open(f);

        //leemos los datos
        Mat classes=lectorMat.readMat("classes");//matriz pequeña=>lector normal

        //la otra matriz es demasiado grande => cargamos desde imagen
        //debemos evitar el escalado de la imagen
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap trainingDataBitmap= BitmapFactory.decodeResource(father.getApplicationContext().getResources(), R.raw.gsvm_image, options);
        Mat trainingData=new Mat();//de flotantes
        Utils.bitmapToMat(trainingDataBitmap, trainingData);
        //AQUI LA MATRIZ ES DE TIPO CvType.CV_8UC4 => PASAR A BGR CV_8UC1)
        Imgproc.cvtColor(trainingData,trainingData,Imgproc.COLOR_BGRA2GRAY);
        //lo intento pasar a CV_32F
        trainingData.convertTo(trainingData, CvType.CV_32F);

        //definimos los parámetros de reconocedor
        CvSVMParams params = new CvSVMParams();
        params.set_svm_type(CvSVM.C_SVC);
        params.set_kernel_type(CvSVM.LINEAR);
        params.set_degree(0);
        params.set_gamma(1);
        params.set_coef0(0);
        params.set_C(1);
        params.set_nu(0);
        params.set_p(0);
        TermCriteria termCriteria=new TermCriteria(TermCriteria.MAX_ITER, 1000, 0.1);//ESTE ES EL ITER DE C++
        params.set_term_crit(termCriteria);

        //declaramos el reconocodecor de acuerdo a los parámetros anteriores
        //quizá todo esto se haga en sólo el reconocedor => probar
        reconocedor=new CvSVM();
        reconocedor.train(trainingData, classes, new Mat(), new Mat(), params);
        Log.d(TAG, "Reconocedor entrenado con éxito!!!");
    }

    //procesador con tesseract
    /*
    public Mat procesa(Mat entrada) {
        //copia temporal
        Mat salida = entrada.clone();
        //Obtenemos las áreas de imagen candidatos a matrícula
        List<Rect> candidatos=getCandidatosMatricula(entrada.clone());
        if(candidatos.size()==0)
            resultado="Procesando...";//rectificacion
        //es posible que tengamos varios candidatos en cada imagen
        //debemos discriminarlos => segmentacion interior
        List<Rect>candidatosReales=new ArrayList<Rect>();
        List<String>salidaReconocida=new ArrayList<String>();
        //reconocemos
        for(Rect candidato:candidatos)
        {
            Mat procesar=new Mat();
            procesar = entrada.submat(candidato);
            List<Rect> digitos=new ArrayList<Rect>();
            digitos=segmentacionInterior(procesar);
            //Log.d(TAG, "Candidatos dígito: " + digitos.size());
            salida = dibujarResultado(entrada, candidato, resultado);
            if(digitos.size()>=4 && digitos.size()<10) {
                //dígito por dígito => tesseract
                /*for(Rect digito:digitos)
                {
                    Mat zona=procesar.submat(digito);
                    Bitmap resultBitmap = Bitmap.createBitmap(zona.cols(), zona.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(zona, resultBitmap);
                    String matricula=ocr.detectText(resultBitmap);
                    if(matricula.length()==1)
                        resultado+=matricula;
                    Log.d(TAG, "Caracter reconocido: " + matricula);
                }*/

                /*
                //a esto le pasamos el tesseract => matricula completa => asyntask

                //el ocr funciona a color =>!!!!!!!!!!!!!!!!!!!!!!!
                //conversión a bitmap => el bitmap es a color y la salida en byn (según arriba)
                //Esto en realidad se hará sobre la submatriz una vez reconocida la matrícula!!!!!!!!!!!!!!!!!!!!!
                Bitmap resultBitmap = Bitmap.createBitmap(procesar.cols(), procesar.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(procesar, resultBitmap);
                //lo cancelamos de momento
                if (ocr == null || ocr.getStatus().equals(AsyncTask.Status.FINISHED))
                    ocr = new TessAsyncEngine(this);//nos encargamos de recibir los datos

                    //sólo ejecutamos una vez (está pendiente de resultado)
                else if (ocr.getStatus().equals(AsyncTask.Status.PENDING))
                    ocr.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, padre, resultBitmap);
                else
                    Log.d(TAG, "Debo esperar a que acabe otro reconocedor");*//*
            }
        }
        //Dependiendo de los candidatos mostramos el resultado
        /*if(candidatosReales.size()==1)
        {
            Rect zona=candidatosReales.get(0);
            salida=dibujarResultado(entrada, zona, resultado);
        }
        else {
            if(candidatosReales.size()>1)//demasiados candidatos
                dibujarCandidatos(salida, candidatosReales);
            else
            {
                //no se ha reconocido nada => mostramos al menos el área analizada
                dibujarCandidatos(salida, candidatos);
            }
        }*/

/*
        return salida;


        /*
        //obtenemos los candidatos a matricula => de este modo podemos probar distintos algoritmos
        //cambiando esta función (15/07/2015)
        List<Rect> candidatos = getCandidatosMatricula(entrada);
        //los discriminamos
        List<Rect> candidatosSegmentados = discriminarCandidatos(entrada, candidatos);

        //pruebas de segmentación de la matrícula
        /*if(candidatosSegmentados.size()==1){
            Mat procesar = entrada.submat(candidatosSegmentados.get(0));
            //salida=candidatosDigitos(procesar.clone());
            salida=segmentarInteriorDisco(entrada, candidatosSegmentados.get(0));
        }*/
        /*
        //sólo actuamos ante un candidato
        if (candidatosSegmentados.size() == 1) {
            Log.d(TAG, "OCR a la imagen");
            Mat procesar = entrada.submat(candidatosSegmentados.get(0));
            //salida=procesar.clone();//Con esto se muestra sólo la matrícula
            List<Rect> digitos=getCandidatosDigitos(procesar.clone());
            //reconocemos
            StringBuilder resultado=new StringBuilder();
            for(Rect digito:digitos)
            {
                Mat candidato=procesar.submat(digito);
                //la convertimos a escala de grises
                Imgproc.cvtColor(candidato,candidato,Imgproc.COLOR_BGRA2GRAY);
                //lo reconocemos
                resultado.append(leerImagen(candidato));
            }
            //ya podemos dibujar el resultado (x lo que sea se reconoce al revés)
            String matriculaReconocida=resultado.reverse().toString();
            //aquí habría que analizarla y rotarla o no de acuerdo al resultado
            //al igual que eliminar caracteres o indicar de algún modo que no está reconocida
            Log.d(TAG, "Matricula reconocida: " + matriculaReconocida);
            Rect zona=candidatosSegmentados.get(0);
            salida=dibujarResultado(entrada, zona, matriculaReconocida);

            //mejoramos la matricula => no funciona
            //procesar=prepararMatricula(procesar.clone());
            //salida=candidatosDigitos(procesar.clone());//=>dibuja la mat con los candidatos
            //modo 2 => muestra todo el dibujo
            //salida=segmentarInteriorMatricula(entrada, candidatosSegmentados.get(0));
            //modo 3 => muestra sólo la matrícula
            //salida=segmentarInteriorMatricula(procesar);

            /*
            //en nuestro caso mostramos el candidato y el posible resultado
            salida = dibujarResultado(entrada, candidatosSegmentados.get(0), resultado);
            //el ocr funciona a color =>!!!!!!!!!!!!!!!!!!!!!!!
            //conversión a bitmap => el bitmap es a color y la salida en byn (según arriba)
            //Esto en realidad se hará sobre la submatriz una vez reconocida la matrícula!!!!!!!!!!!!!!!!!!!!!
            Bitmap resultBitmap = Bitmap.createBitmap(procesar.cols(), procesar.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(procesar, resultBitmap);

            //probamos a hacerlo de modo individual
            //Debemos tener en ejecución un único reconocedor cada vez
            //Entonces, si no existe o el anterior ya ha terminado => crear una nuevo

            //lo cancelamos de momento
            if (ocr == null || ocr.getStatus().equals(AsyncTask.Status.FINISHED))
                ocr = new TessAsyncEngine(this);//nos encargamos de recibir los datos

            //sólo ejecutamos una vez (está pendiente de resultado)
            else if (ocr.getStatus().equals(AsyncTask.Status.PENDING))
                ocr.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, padre, resultBitmap);
            else
                Log.d(TAG, "Debo esperar a que acabe otro reconocedor");


            //no asyntask
            /*TessEngine ocr=TessEngine.Generate(padre);
            Rect zona=candidatosSegmentados.get(0);
            Mat procesar=entrada.submat(zona);
            //salida=procesar.clone();
            //el ocr trabaja a color
            Bitmap resultBitmap = Bitmap.createBitmap(procesar.cols(), procesar.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(procesar, resultBitmap);
            //analizamos el texto
            String matricula=ocr.detectText(resultBitmap);
            if(matricula.length()!=0)
                salida=dibujarResultado(entrada,zona,matricula);//reconocido
            else
                salida=dibujarResultado(entrada,zona,"???");*/
        /*} else {
            Log.d(TAG, "Demasiados candidatos en la imagen");
            //los dibujamos a título informativo (verde)
            dibujarCandidatos(salida, candidatos);
        }


        candidatos.clear();
        candidatosSegmentados.clear();

        return salida;*/


        //modo nuevo

   // }

    //procesasador con svm
    public Mat procesarSVM(Mat entrada) {
        //copia temporal
        Mat salida = entrada.clone();

        //Obtenemos las áreas de imagen candidatos a matrícula
        List<Rect> candidatos=getCandidatosMatricula(entrada.clone());
        //es posible que tengamos varios candidatos en cada imagen
        //debemos discriminarlos => segmentacion interior
        List<Rect>candidatosReales=new ArrayList<Rect>();
        List<String>salidaReconocida=new ArrayList<String>();
        for(Rect candidato:candidatos)
        {
            Mat procesar=new Mat();
            procesar = entrada.submat(candidato);
            List<Rect> digitos=new ArrayList<Rect>();
            digitos=segmentacionInterior(procesar);
            //ordenamos los rectángulos de acuerdo a su coordenada x => 29/07/2015
            //de este modo nos aseguramos el orden correcto en el reconocimiento
            Collections.sort(digitos, new OrdenarRectangulos());
            //Log.d(TAG, "Candidatos dígito: " + digitos.size());
            if(digitos.size()>=4 && digitos.size()<10) {
                //le pasamos el ocr
                StringBuilder resultado=new StringBuilder();
                for(Rect digito:digitos)
                {
                    Mat zona=procesar.submat(digito);
                    //la convertimos a escala de grises
                    Imgproc.cvtColor(zona,zona,Imgproc.COLOR_BGRA2GRAY);
                    //lo reconocemos
                    resultado.append(leerImagen(zona));
                }
                //ya podemos dibujar el resultado (x lo que sea se reconoce al revés)
                String matriculaReconocida=resultado.toString();
                //la debemos procesar (quitamos el caracter de no reconocido por espacio
                matriculaReconocida=matriculaReconocida.replace((char)0,' ');
                //ahora le quitamos los espacios
                matriculaReconocida=matriculaReconocida.replaceAll("\\s+","");
                //vemos si es europea
                //corregirMatricula(matriculaReconocida);
                //a partir de 3 caracteres reconocidos suponemos que es una matrícula
                if(matriculaReconocida.length()>3) {
                    candidatosReales.add(candidato);
                    salidaReconocida.add(matriculaReconocida);
                }
                //Log.d(TAG, "Matricula reconocida: " + matriculaReconocida);
            }
        }
        //Dependiendo de los candidatos mostramos el resultado
        if(candidatosReales.size()==1)
        {
            Rect zona = candidatosReales.get(0);
            salida = dibujarResultado(entrada, zona, salidaReconocida.get(0));
        }
        else {
            if(candidatosReales.size()>1)//demasiados candidatos
                dibujarCandidatos(salida, candidatosReales);
            else
            {
                //no se ha reconocido nada => mostramos al menos el área analizada
                dibujarCandidatos(salida, candidatos);
            }
        }
        return salida;
    }

    //procesar con tesseract sin segmentar
    public Mat procesarTesseract(Mat entrada)
    {
        //copia temporal
        Mat salida = entrada.clone();

        //Obtenemos las áreas de imagen candidatos a matrícula
        List<Rect> candidatos=getCandidatosMatricula(entrada.clone());
        if(candidatos.size()==0)
            resultado="Procesando...";//rectificacion

        //es posible que tengamos varios candidatos en cada imagen
        //debemos discriminarlos => segmentacion interior
        List<Rect>candidatosReales=new ArrayList<Rect>();
        for(Rect candidato:candidatos)
        {
            Mat procesar=new Mat();
            procesar = entrada.submat(candidato);
            List<Rect> digitos=new ArrayList<Rect>();
            digitos=segmentacionInterior(procesar);
            //Log.d(TAG, "Candidatos dígito: " + digitos.size());
            if(digitos.size()>=4 && digitos.size()<10) {
                candidatosReales.add(candidato);
            }
        }

        //sólo actuamos ante un candidato
        if (candidatosReales.size() == 1) {
            Log.d(TAG, "OCR a la imagen");
            Mat procesar = entrada.submat(candidatosReales.get(0));
            //salida=procesar.clone();//Con esto se muestra sólo la matrícula
            //en nuestro caso mostramos el candidato y el posible resultado
            salida = dibujarResultado(entrada, candidatosReales.get(0), resultado);
            //el ocr funciona a color =>!!!!!!!!!!!!!!!!!!!!!!!
            //conversión a bitmap => el bitmap es a color y la salida en byn (según arriba)
            //Esto en realidad se hará sobre la submatriz una vez reconocida la matrícula!!!!!!!!!!!!!!!!!!!!!
            Bitmap resultBitmap = Bitmap.createBitmap(procesar.cols(), procesar.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(procesar, resultBitmap);

            //probamos a hacerlo de modo individual
            //Debemos tener en ejecución un único reconocedor cada vez
            //Entonces, si no existe o el anterior ya ha terminado => crear una nuevo

            //lo cancelamos de momento
            if (ocr == null || ocr.getStatus().equals(AsyncTask.Status.FINISHED))
                ocr = new TessAsyncEngine(this);//nos encargamos de recibir los datos

                //sólo ejecutamos una vez (está pendiente de resultado)
            else if (ocr.getStatus().equals(AsyncTask.Status.PENDING))
                ocr.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, padre, resultBitmap);
            else
                Log.d(TAG, "Debo esperar a que acabe otro reconocedor");



        } else {
            Log.d(TAG, "Demasiados candidatos en la imagen");
            //los dibujamos a título informativo (verde)
            dibujarCandidatos(salida,candidatos);
        }


        //liberamos memoria
        candidatos.clear();
        candidatosReales.clear();

        return salida;

    }




    @Override
    public void onTaskComplete(String result) {
        //ahora le quitamos los espacios
        result=result.replaceAll("\\s+","");
        Log.d(TAG, "Resultado devuelto por aysntask: " + result);
        if (result.length() != 0)
            resultado = result;
        else
            resultado = "???";
    }

    /**
     * Detiene el asyntask con el tesseract en curso
     */
    public void detenerReconocedor()
    {
        if(ocr!=null)
            ocr.cancel(true);
        resultado="Procesando...";
    }


    //no funciona
   private void aplicarOCR(Mat entrada, List<Rect> digitos)
    {
        StringBuilder resultado=new StringBuilder();
        for(Rect digito:digitos)
        {
            Mat procesar=entrada.submat(digito);
            //aplicamos el ocr
            TessEngine ocr=TessEngine.Generate(padre);
            //salida=procesar.clone();
            //el ocr trabaja a color
            Bitmap resultBitmap = Bitmap.createBitmap(procesar.cols(), procesar.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(procesar, resultBitmap);
            //analizamos el texto
            /*String matricula=ocr.detectText(resultBitmap);
            if(matricula.length()!=0)
                Log.d(TAG, "Caracter reconocido: " + matricula);
            else
                Log.d(TAG, "Caracter no reconocido");*/
            resultado.append(ocr.detectText(resultBitmap));
        }
        Log.d(TAG, "Matricula reconocido: " + resultado);
    }

    /**
     * Binariza la imagen mediante otsu
     * @param entrada Mat a binarizar
     * @return Mat binarizada
     */
    public Mat otsu(Mat entrada) {
        Mat salida = new Mat();
        Imgproc.threshold(entrada, salida, 60, 255, Imgproc.THRESH_OTSU
                | Imgproc.THRESH_BINARY_INV);
        return salida;
    }


    /**
     * Marca en la imagen las zonas candidatas a matrícula (cumplen con las medidas)
     * @param salida Imagen con las zonas candidatas a matrícula marcadas.
     * @param candidatos Zonas a marcar en la imagen
     */
    private void dibujarCandidatos(Mat salida, List<Rect> candidatos) {
        for (Rect candidato : candidatos) {
            final Point p1 = new Point(candidato.x, candidato.y);
            final Point p2 = new Point(candidato.x + candidato.width, candidato.y + candidato.height);
            Core.rectangle(salida, p1, p2, new Scalar(0, 255, 0));//estos en verde
        }
    }

    /**
     * Muestra el texto reconocido en la imagen
     * @param imagen Imagen sobre la que dibujar
     * @param digit_rect Zona sobre la que dibujar
     * @param matricula Texto a mostrar
     * @return Imagen Imagen con los resultados dibujados
     */
    private Mat dibujarResultado(Mat imagen, Rect digit_rect, String matricula) {
        Mat salida = imagen.clone();
        Point P1 = new Point(digit_rect.x, digit_rect.y);
        Point P2 = new Point(digit_rect.x + digit_rect.width, digit_rect.y
                + digit_rect.height);

        Core.rectangle(salida, P1, P2, new Scalar(255, 0, 0));//resultado en rojo

        // Escribir numero
        int fontFace = Core.FONT_HERSHEY_PLAIN;
        double fontScale = 2;
        int thickness = 2;
        //texto
        Core.putText(salida, matricula, P1, fontFace, fontScale,
                new Scalar(233, 0, 0), thickness, 8, false);
        //sombreado
        /*Core.putText(salida, matricula, P1, fontFace, fontScale,
                new Scalar(255, 255, 255), thickness / 2, 8, false);*/
        return salida;
    }

    /**
     * Reconocedor de la imagen mediante patrones svm
     * @param imagen Segmento de imagen con el carácter a reconocer
     *               Debe ser escala de grises, ya que disponse de más informacion que en binario
     * @return Caracter reconocido
     */
    private char leerImagen(Mat imagen)
    {
        //dado que la matriz de reconocimiento esta compuesta por
        //imágenes 10x15, redimensionamos la entrada a ese
        //mismo tamaño
        Imgproc.resize(imagen, imagen, new Size(10, 15), 0, 0, Imgproc.INTER_CUBIC);
        //creamos copia de esta imagen
        Mat imagenComparar=imagen.clone();
        //creamos una imagen de puntos
        Mat p=new Mat(1, imagen.cols()*imagen.rows(), imagen.type(), new Scalar(255));
        //comparamos
        for(int y=0; y<imagen.rows(); y++)
            for(int x=0; x<imagen.cols(); x++)
                p.put(0,x+(y*imagen.cols()),imagen.get(y,x));
        //convertimos la imagen
        p.convertTo(p, CvType.CV_32F);
        //lanzamos la predicción
        int res=(int)reconocedor.predict(p);
        if(res<35)
            return caracteres[res];
        else
            return 0;
    }

    /**
     * Guarda la imagen en el dispositivo
     * @param input Imagen a guardar.
     * @param name Nombre a asignar a la imagen
     */
    private void saveImage(final Mat input, String name) {
        // Determina la ruta para crear los archivos
        final long currentTimeMillis = System.currentTimeMillis();
        final String appName = padre.getString(R.string.app_name);
        final String galleryPath = Environment
                .getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString();
        final String albumPath = galleryPath + "/" + appName;
        final String photoPathIn = albumPath + "/" + name + "_" + currentTimeMillis
                + ".png";

        // Asegurarse que el directorio existe
        File album = new File(albumPath);
        if (!album.isDirectory() && !album.mkdirs()) {
            Log.e(TAG, "Error al crear el directorio " + albumPath);
            return;
        }
        // Intenta crear los archivos
        Mat mBgr = new Mat();

        if (input.channels() == 1)
            Imgproc.cvtColor(input, mBgr, Imgproc.COLOR_GRAY2BGR, 3);
        else
            Imgproc.cvtColor(input, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
        if (!Highgui.imwrite(photoPathIn, mBgr))
            Log.e(TAG, "Fallo al guardar " + photoPathIn);
        mBgr.release();
        return;
    }


    //ALGORITMOS EMPLEADOS

    /**
     * Comprueba que el tamaño de la zona a analizar cumpla con las medidas de
     * una matrícula española.
     * @param rect RotateRect con la zona a analizar
     * @return True si cumple con las medidas y false en caso contrario
     */
    private boolean verifySize(RotatedRect rect)
    {
        float error=0.4f;
        //El tamaño de la matrícula española es: 52x11 => aspect0 4.7272
        float aspect=4.7272f;
        //Establecemos un área mínima y máxima. Todo lo que se salga de ahí se descarta.
        int min= (int) (15*aspect*15); // área mínima
        int max= (int) (125*aspect*125); // área máxima
        //Nos quedamos sólo con las partes que cumplan con el ratio de aspecto
        float rmin= aspect-aspect*error;
        float rmax= aspect+aspect*error;

        int area= (int) (rect.size.height * rect.size.width);
        float r= (float)rect.size.width / (float)rect.size.height;
        if(r<1)
            r= (float)rect.size.height / (float)rect.size.width;

        if(( area < min || area > max ) || ( r < rmin || r > rmax )){
            return false;
        }else{
            return true;
        }
    }

    /**
     * Comprueba si el el área a analizar (RotatedRect) tiene un angulo determinado.
     * Se realiza una conversión de ángulos a un modo más entendible (humanos)
     * La matrícula estará en horizontal con un margen de 15º
     * @param rect Zona a analizar
     * @return true si cumple el requisito de ángulo y false en caso contrario
     */
    private boolean verifyAngle(RotatedRect rect)
    {
        //medimos el ángulo siempre de acuerdo al lado ancho
        //hacemos correción para que sea más legible
        // | 0ª
        // / 45º
        // -- 90º
        // \ 135º

        double angulo=rect.angle;
        if(rect.size.width < rect.size.height)
            angulo+=180;
        else
            angulo+=90;

        //dejamos 15º de margen
        if(angulo<85 || angulo>105)
            return false;
        else
            return true;
    }

    //buscamos candidatos de matrícula => segment
    //Como la matrícula puede estar torcida => rotated rect
    //mirar ejemplo en : http://study.marearts.com/2013/08/opencv-rotatedrect-draw-example-source.html
    //FUNCIONA DE PUTA MADRE

    /**
     * Devuelve RotateRect con zonas que cumplen con el aspecto de matrícula
     * @param entrada Imagen a analizar
     * @return Zonas con candidatos a matrícula
     * @deprecated Sólo se usa para crear imágenes de pasos intermedios
     */
    private List<RotatedRect> getCandidatosMatriculaRotacion(Mat entrada)
    {
        List<RotatedRect> candidatos=new ArrayList<RotatedRect>();

        //convertimos la imagen a escala de grises
        Mat img_gray=new Mat();
        Imgproc.cvtColor(entrada, img_gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.blur(img_gray, img_gray, new Size(5, 5));

        //Dado que las matriculas suelen tener una alta densidad de líneas verticales,
        //las localizamos mediante sobel
        Mat img_sobel=new Mat();
        Imgproc.Sobel(img_gray, img_sobel, CvType.CV_8U, 1, 0, 3, 1, 0, Imgproc.BORDER_DEFAULT);

        //Binarizamos mediante otsu
        Mat img_threshold=new Mat();
        Imgproc.threshold(img_sobel, img_threshold, 0, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);

        //operacion morfologica
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(17, 3));
        Imgproc.morphologyEx(img_threshold, img_threshold, Imgproc.MORPH_CLOSE, element);

        //buscamos los posibles candidatos a matrícula
        Mat copia=img_threshold.clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        //como no necesitamos la jerarquia => ponemos new mat en la llamada
        Imgproc.findContours(copia, contours, new Mat(), Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_NONE);

        //debemos recorrer => nos quedamos sólo con lo que nos interesa
        List<MatOfPoint> contornosOK = new ArrayList<MatOfPoint>();
        for(MatOfPoint contorno:contours)
        {
            //Convertimos contornos(i) de MatOfPoint a MatOfPoint2f
            RotatedRect rect=Imgproc.minAreaRect(new MatOfPoint2f(contorno.toArray()));
            if(verifySize(rect) && verifyAngle(rect)) {
                candidatos.add(rect);
                contornosOK.add(contorno);//para dibujo posterior =>quitar si no lo usamos
            }
        }

        //liberar memoria
        img_gray.release();
        img_sobel.release();
        copia.release();

        return candidatos;
    }


    /**
     * Devuelve zonas rectas que contienen candidatos a matrícula
     * Esto es debido a que el API de openCV de java no implementa las funciones de correción
     * de perspectiva, y los resultados son satisfactorios sin la misma.
     * @param entrada Imagen a analizar
     * @return Zonas candidatas a contener una matrícula en su interior
     */
    private List<Rect> getCandidatosMatricula(Mat entrada)
    {
        List<Rect> candidatos=new ArrayList<Rect>();

        //convertimos la imagen a escala de grises
        Mat img_gray=new Mat();
        Imgproc.cvtColor(entrada, img_gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.blur(img_gray, img_gray, new Size(5, 5));

        //Dado que las matriculas suelen tener una alta densidad de líneas verticales,
        //las localizamos mediante sobel
        Mat img_sobel=new Mat();
        Imgproc.Sobel(img_gray, img_sobel, CvType.CV_8U, 1, 0, 3, 1, 0, Imgproc.BORDER_DEFAULT);

        //Binarizamos mediante otsu
        Mat img_threshold=new Mat();
        Imgproc.threshold(img_sobel, img_threshold, 0, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);

        //operacion morfologica
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(17, 3));
        Imgproc.morphologyEx(img_threshold, img_threshold, Imgproc.MORPH_CLOSE, element);

        //buscamos los posibles candidatos a matrícula
        Mat copia=img_threshold.clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        //como no necesitamos la jerarquia => ponemos new mat en la llamada
        Imgproc.findContours(copia, contours, new Mat(), Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_NONE);

        //debemos recorrer => nos quedamos sólo con lo que nos interesa
        //List<MatOfPoint> contornosOK = new ArrayList<MatOfPoint>();
        for(MatOfPoint contorno:contours)
        {
            //Convert contours(i) from MatOfPoint to MatOfPoint2f
            RotatedRect rect=Imgproc.minAreaRect(new MatOfPoint2f( contorno.toArray() ));
            Rect BB=Imgproc.boundingRect(contorno);
            //corregimos por ambos lados
            if(verifySize(rect) && verifyAngle(rect)) {
                candidatos.add(Imgproc.boundingRect(contorno));//tambien es válido rect.boundingRect()
                //contornosOK.add(contorno);//para dibujo posterior =>quitar si no lo usamos
            }
        }

        //liberar memoria
        img_gray.release();
        img_sobel.release();
        img_threshold.release();
        copia.release();

        return candidatos;
    }


    /**
     * Segmenta el interior de la matrícula
     * @param entrada Imagen a segmentar
     * @return Listado con los candidatos a dígito
     */
    private List<Rect> segmentacionInterior(Mat entrada)
    {
        Mat procesar=new Mat();
        procesar = entrada.clone();
        //la pasamos a escala de grises
        Mat zonaGris=new Mat();
        Imgproc.cvtColor(procesar, zonaGris, Imgproc.COLOR_RGBA2GRAY);
        //ecualizamos el histograma
        Mat histograma=new Mat();
        Imgproc.equalizeHist(zonaGris, histograma);
        //lo pasamos a binario
        Mat treshold=new Mat();
        //Imgproc.threshold(histograma, treshold, 60,255, Imgproc.THRESH_BINARY_INV);
        treshold=otsu(histograma);
        //buscamos los contornos
        List<MatOfPoint> contornos = new ArrayList<MatOfPoint>();
        List<Rect> digitos=new ArrayList<Rect>();
        // buscamos los contornos (jerarquía de 2 niveles y devolvemos todos los
        // puntos)
        Imgproc.findContours(treshold, contornos, new Mat(), Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_NONE);
        //los recorremos y los dibujamos
        for(MatOfPoint contorno:contornos)
        {
            Rect BB=Imgproc.boundingRect(contorno);
            if(BB.width>=BB.height)
                continue;

            digitos.add(BB);
        }

        //liberar memoria
        zonaGris.release();
        histograma.release();
        treshold.release();
        procesar.release();

        return digitos;
    }

    private String corregirMatricula(String matricula)
    {
        //nos quedamos los 3 últimos caracteres
        String fin =matricula.substring(matricula.length()-3, matricula.length());
        //vemos que todos sean letras borrando todo lo que no sean dígitos
        fin = fin.replaceAll("\\D+","");
        if(fin.length()==0)
        {
            //estamos ante una matrícula europea

        }

        return matricula;

    }


    /**
     * Clase para la ordenación de los Rect
     */
    class OrdenarRectangulos implements Comparator<Rect> {
        @Override
        public int compare(Rect r1, Rect r2) {
            return (r1.x>r2.x) ? 1 : -1;
        }
    }


}
