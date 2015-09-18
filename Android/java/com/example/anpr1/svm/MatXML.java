package com.example.anpr1.svm;

import android.util.Log;

import com.example.anpr1.opencv.Procesador;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.InputStream;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by Gonzalo on 16/07/2015.
 *
 * Dado que el API openCV de Android carece de la funcionalidad FileStorage
 * debemos utilizarlo mediante llamadas al codigo nativo, o en nuestro caso,
 * mediante la implementación de una clase que realice dichas funciones
 */
public class MatXML {
    // static
    public static final int READ = 0;
    public static final int WRITE = 1;

    //TAG para errores
    private static final String TAG="DBG_" + MatXML.class.getName();

    // varaible
    private File file;
    private boolean isWrite;
    private Document doc;
    private Element rootElement;

    /**
     * Constructor de la clase
     */
    public MatXML() {
        file = null;
        isWrite = false;
        doc = null;
        rootElement = null;
    }


    /**
     * Indica el modo de apertura del fichero xml
     * @param filePath Ruta del fichero a tratar
     * @param flags Modo de apertura
     */
    public void open(String filePath, int flags ) {
        try {
            if( flags == READ ) {
                open(filePath);
            }
            else {
                create(filePath);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Apertura del fichero indicado en modo sólo lectura
     * @param filePath Ruta al fichero a leer
     */
    public void open(String filePath) {
        try {
            file = new File(filePath);
            if( file == null || file.isFile() == false ) {
                Log.e(TAG, "Imposible abrir el archivo: " + filePath);
            }
            else {
                isWrite = false;
                doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
                doc.getDocumentElement().normalize();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Apertura para lectura
     * @param file InputStream, por ejemplo fichero desde raw
     */
    public void open(InputStream file) {
        try {
            isWrite = false;
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
            doc.getDocumentElement().normalize();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Apertura del fichero en modo de sólo escritura
     * @param filePath Ruta del fichero a escribir
     */
    public void create(String filePath) {
        try {
            file = new File(filePath);
            if( file == null ) {
                Log.e(TAG, "Imposible escribir fichero: " + filePath);
            }
            else {
                isWrite = true;
                doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

                rootElement = doc.createElement("opencv_storage");
                doc.appendChild(rootElement);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Lee el contenido del fichero xml y lo transforma en una mat
     * @param tag Etiqueta del fichero que queremos extraer
     * @return Mat con el contenido de la etiqueta tag del fichero xml
     */
    public Mat readMat(String tag) {
        if( isWrite ) {
            Log.e(TAG, "Intentando leer un fichero en modo escritura");
            return null;
        }

        NodeList nodelist = doc.getElementsByTagName(tag);
        Mat readMat = null;

        for( int i = 0 ; i<nodelist.getLength() ; i++ ) {
            Node node = nodelist.item(i);

            if( node.getNodeType() == Node.ELEMENT_NODE ) {
                Element element = (Element)node;

                String type_id = element.getAttribute("type_id");
                if( "opencv-matrix".equals(type_id) == false) {
                    Log.e(TAG, "Error localizando la etiqueta type_id ");
                }

                String rowsStr = element.getElementsByTagName("rows").item(0).getTextContent();
                String colsStr = element.getElementsByTagName("cols").item(0).getTextContent();
                String dtStr = element.getElementsByTagName("dt").item(0).getTextContent();
                String dataStr = element.getElementsByTagName("data").item(0).getTextContent();

                int rows = Integer.parseInt(rowsStr);
                int cols = Integer.parseInt(colsStr);
                int type = CvType.CV_8U;

                Scanner s = new Scanner(dataStr);

                //matriz de float
                if( "f".equals(dtStr) ) {
                    type = CvType.CV_32F;
                    readMat = new Mat( rows, cols, type );
                    /*float fs[] = new float[1];
                    for( int r=0 ; r<rows ; r++ ) {
                        for( int c=0 ; c<cols ; c++ ) {
                            if( s.hasNextFloat() ) {
                                fs[0] = s.nextFloat();
                            }
                            else {
                                fs[0] = 0;
                                Log.e(TAG, "Error de lectura del float en fila=" + r + " columna=" + c);
                            }
                            readMat.put(r, c, fs);
                        }
                    }*/
                    String valores[]=dataStr.split("\\s+");
                    int indice=1;
                    float valor;
                    Log.e(TAG, "Tengo que procesar: " + valores.length);
                    //Log.e(TAG, "Valores[0]: " + Float.parseFloat(valores[0]));
                    //Log.e(TAG, "Valores[1]: " + Float.parseFloat(valores[1]));
                    //Log.e(TAG, "Valores[154950]: " + Float.parseFloat(valores[154950]));
                    for( int r=0 ; r<rows ; r++ ) {
                        for (int c = 0; c < cols; c++) {
                            valor=Float.parseFloat(valores[indice]);
                            indice++;
                            readMat.put(r, c, valor);
                            Log.e(TAG, "Guardo el valor: " + indice);
                        }
                    }
                }
                else if( "i".equals(dtStr) ) {//matriz de enteros
                    type = CvType.CV_32S;
                    readMat = new Mat( rows, cols, type );
                    int is[] = new int[1];
                    for( int r=0 ; r<rows ; r++ ) {
                        for( int c=0 ; c<cols ; c++ ) {
                            if( s.hasNextInt() ) {
                                is[0] = s.nextInt();
                            }
                            else {
                                is[0] = 0;
                                Log.e(TAG, "Error de lectura del int en fila=" + r + " columna=" + c);
                            }
                            readMat.put(r, c, is);
                        }
                    }
                }
                else if( "s".equals(dtStr) ) {//short
                    type = CvType.CV_16S;
                    readMat = new Mat( rows, cols, type );
                    short ss[] = new short[1];
                    for( int r=0 ; r<rows ; r++ ) {
                        for( int c=0 ; c<cols ; c++ ) {
                            if( s.hasNextShort() ) {
                                ss[0] = s.nextShort();
                            }
                            else {
                                ss[0] = 0;
                                Log.e(TAG, "Error de lectura del short en fila="+r + " columna="+c);
                            }
                            readMat.put(r, c, ss);
                        }
                    }
                }
                else if( "b".equals(dtStr) ) {//byte
                    readMat = new Mat( rows, cols, type );
                    byte bs[] = new byte[1];
                    for( int r=0 ; r<rows ; r++ ) {
                        for( int c=0 ; c<cols ; c++ ) {
                            if( s.hasNextByte() ) {
                                bs[0] = s.nextByte();
                            }
                            else {
                                bs[0] = 0;
                                Log.e(TAG, "Error de lectura del byte en fila="+r + " columna="+c);
                            }
                            readMat.put(r, c, bs);
                        }
                    }
                }
            }
        }
        return readMat;
    }


    /**
     * Escribe una mat en un fichero xml, dentro de la etiqueta tag indicada
     * @param tag Etiqueta con el tipo de dato a escribir
     * @param mat Mat con el contenido a escribir en el fichero
     */
    public void writeMat(String tag, Mat mat) {
        try {
            if( isWrite == false) {
                Log.e(TAG, "Intentando escribir en un fichero con permisos de lectura");
                return;
            }

            Element matrix = doc.createElement(tag);
            matrix.setAttribute("type_id", "opencv-matrix");
            rootElement.appendChild(matrix);

            Element rows = doc.createElement("rows");
            rows.appendChild( doc.createTextNode( String.valueOf(mat.rows()) ));

            Element cols = doc.createElement("cols");
            cols.appendChild( doc.createTextNode( String.valueOf(mat.cols()) ));

            Element dt = doc.createElement("dt");
            String dtStr;
            int type = mat.type();
            if(type == CvType.CV_32F ) { // type == CvType.CV_32FC1
                dtStr = "f";
            }
            else if( type == CvType.CV_32S ) { // type == CvType.CV_32SC1
                dtStr = "i";
            }
            else if( type == CvType.CV_16S  ) { // type == CvType.CV_16SC1
                dtStr = "s";
            }
            else if( type == CvType.CV_8U ){ // type == CvType.CV_8UC1
                dtStr = "b";
            }
            else {
                dtStr = "unknown";
            }
            dt.appendChild( doc.createTextNode( dtStr ));

            Element data = doc.createElement("data");
            String dataStr = dataStringBuilder( mat );
            data.appendChild( doc.createTextNode( dataStr ));

            // append all to matrix
            matrix.appendChild( rows );
            matrix.appendChild( cols );
            matrix.appendChild( dt );
            matrix.appendChild( data );

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Transforma la matriz mat en una cadena de texto
     * @param mat Matriz a transformar en cadena de texto
     * @return Cadena de texto con el contenido de la matriz
     */
    private String dataStringBuilder(Mat mat) {
        StringBuilder sb = new StringBuilder();
        int rows = mat.rows();
        int cols = mat.cols();
        int type = mat.type();

        if( type == CvType.CV_32F ) {
            float fs[] = new float[1];
            for( int r=0 ; r<rows ; r++ ) {
                for( int c=0 ; c<cols ; c++ ) {
                    mat.get(r, c, fs);
                    sb.append( String.valueOf(fs[0]));
                    sb.append( ' ' );
                }
                sb.append( '\n' );
            }
        }
        else if( type == CvType.CV_32S ) {
            int is[] = new int[1];
            for( int r=0 ; r<rows ; r++ ) {
                for( int c=0 ; c<cols ; c++ ) {
                    mat.get(r, c, is);
                    sb.append( String.valueOf(is[0]));
                    sb.append( ' ' );
                }
                sb.append( '\n' );
            }
        }
        else if( type == CvType.CV_16S ) {
            short ss[] = new short[1];
            for( int r=0 ; r<rows ; r++ ) {
                for( int c=0 ; c<cols ; c++ ) {
                    mat.get(r, c, ss);
                    sb.append( String.valueOf(ss[0]));
                    sb.append( ' ' );
                }
                sb.append( '\n' );
            }
        }
        else if( type == CvType.CV_8U ) {
            byte bs[] = new byte[1];
            for( int r=0 ; r<rows ; r++ ) {
                for( int c=0 ; c<cols ; c++ ) {
                    mat.get(r, c, bs);
                    sb.append( String.valueOf(bs[0]));
                    sb.append( ' ' );
                }
                sb.append( '\n' );
            }
        }
        else {
            sb.append("unknown type\n");
        }

        return sb.toString();
    }

    /**
     * Realiza la escritura efectiva del fichero xml
     */
    public void release() {
        try {
            if( isWrite == false) {
                Log.e(TAG, "El fichero no tiene permisos de escritura para el release");
                return;
            }

            DOMSource source = new DOMSource(doc);

            StreamResult result = new StreamResult(file);

            // write to xml file
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            // do it
            transformer.transform(source, result);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
