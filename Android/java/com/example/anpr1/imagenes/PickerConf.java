package com.example.anpr1.imagenes;

import android.os.Environment;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Gonzalo on 07/07/2015.
 * Clase con variables de configuración para el picker de imágenes
 */
public class PickerConf {
    // Número de columnas del gridview
    public static final int NUM_OF_COLUMNS = 3;

    // Padding del gridview
    public static final int GRID_PADDING = 8; // dp

    // Directorio de memoria con las imágenes a mostrar
    // en nuestro caso es un directorio dentro de assets
    public static final String PHOTO_ALBUM = "matriculas";

    // Formatos de imagen soportados
    public static final List<String> FILE_EXTN = Arrays.asList("jpg", "jpeg",
            "png");
}
