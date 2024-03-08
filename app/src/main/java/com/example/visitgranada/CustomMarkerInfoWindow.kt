package com.example.visitgranada

import android.view.View
import android.widget.Button
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.InfoWindow

/**
 * Clase que extiende [InfoWindow] para poder asignar una layout a un marcador del mapa.
 * @property closeButton Botón usado para cerrar la vista.
 */
class CustomMarkerInfoWindow(view: View, mapView: MapView) : InfoWindow(view, mapView) {
    private val closeButton: Button = view.findViewById(R.id.closeButton)

    init {
        // Configurar el OnClickListener para el botón de cierre
        closeButton.setOnClickListener {
            close()
        }
    }

    /**
     * Función que se ejecuta cuando se abre el InfoWindow.
     */
    override fun onOpen(item: Any?) {
    }

    /**
     * Función que se ejecuta cuando se cierra el InfoWindow
     */
    override fun onClose() {
    }
}