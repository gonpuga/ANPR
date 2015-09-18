package com.example.anpr1.imagenes;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.GridView;

import com.example.anpr1.R;

import java.util.ArrayList;

/**
 * Created by Gonzalo on 07/07/2015.
 * Actividad para seleccionar la imagen a analizar desde memoria
 */
public class GridViewActivity extends Activity {
    private Utils utils;
    private ArrayList<String> imagePaths = new ArrayList<String>();
    private GridViewImageAdapter adapter;
    private GridView gridView;
    private int columnWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_gridview);
        //accedemos al grid
        gridView = (GridView) findViewById(R.id.grid_view);
        //cargamos las variables de configuración
        utils = new Utils(this);

        // inicializamos el gridView
        initilizeGridLayout();

        // cargamos las rutas de las imagenes
        imagePaths = utils.getFilePaths();

        // Creamos el adaptador
        adapter = new GridViewImageAdapter(GridViewActivity.this, imagePaths,
                columnWidth);

        // Asignamos el adaptador a la vista
        gridView.setAdapter(adapter);
    }

    private void initilizeGridLayout() {
        Resources r = getResources();
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                PickerConf.GRID_PADDING, r.getDisplayMetrics());

        columnWidth = (int) ((utils.getScreenWidth() - ((PickerConf.NUM_OF_COLUMNS + 1) * padding)) / PickerConf.NUM_OF_COLUMNS);

        gridView.setNumColumns(PickerConf.NUM_OF_COLUMNS);
        gridView.setColumnWidth(columnWidth);
        gridView.setStretchMode(GridView.NO_STRETCH);
        gridView.setPadding((int) padding, (int) padding, (int) padding,
                (int) padding);
        gridView.setHorizontalSpacing((int) padding);
        gridView.setVerticalSpacing((int) padding);
    }
}
