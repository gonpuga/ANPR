package com.example.anpr1.tesstool;

/**
 * Created by Gonzalo on 13/07/2015.
 * Interfaz para devolver los datos desde el asyntask a la clase llamadaora
 */
public interface TaskCompleted {
    // Definimos los datos que queramos devolver, en este caso una cadena de texto
    public void onTaskComplete(String result);
}
