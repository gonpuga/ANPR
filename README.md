# ANPR
Reconocedor Automático de Matrículas de Tráfico para Android

####Descripción
Sistema de reconocimiento de matrículas de tráfico para Android, basado en OpenCV, SVM y Tesseract.

####Prerrequisitos
* Android 2.3 o superior
* Un fichero de entrenamiento [trained data file][tessdata] para español. Se recomienda v.3.0.1. Estos ficheros se deben extraer en un subditectorio denominado `tessdata`.

####Dependencias
* http://opencv.org/
* https://github.com/rmtheis/tess-two

#### Configuración

Proyecto configurado para trabajar con Android SDK Tools r22.3+ 

Para generar la librería tess-two code, debe ejecutar la siguientes secuencia de comandos:

    git clone git://github.com/rmtheis/tess-two tess
    cd tess
    cd tess-two
    ndk-build
    android update project --path .
    ant release

## Licencia

    Copyright 2015 Gonzalo Puga Sabio

    Licensed under the Academic Free License version 3.0

        https://opensource.org/licenses/AFL-3.0


