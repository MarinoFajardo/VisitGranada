package com.example.visitgranada

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import com.google.gson.Gson
import okhttp3.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException

/**
 * Interfaz para definir el listener del fragmento que contiene el filtro.
 */
interface DialogFragmentListener {
    /**
     * Función que se llama al hacer click en el botón de Aplicar Filtro.
     * @param categorias Lista con las categorias filtradas.
     */
    fun onPositiveButtonClicked(categorias: List<String>)
}

/**
 * Actividad principal de la aplicación. En ella se muestra el mapa con los lugares de interés y la ubicación actual del usuario.
 * @property apiKey Clave de uso para la API de OpenTripMap.
 * @property apiUrl Dirección de la API.
 * @property numPeticiones Número de lugares que mostramos en el mapa.
 * @property myLoc Overlay para mostrar nuestra posición actual.
 * @property mapa Mapa en el cual se van a mostrar los lugares.
 * @property fusedLocationClient Variable usada para calcular la ubicación actual del usuario.
 * @property myLat Variable que representa la latitud de la ubicación actual del usuario.
 * @property myLong Variable que representa la longitud de la ubicación actual de un usuario.
 * @property lat Variable que representa a latitud de la ubicación de un lugar.
 * @property long Variable que representa la longitud de la ubicación de un lugar.
 * @property categoriasList Lista con las categorías seleccionadas en el filtro.
 *
 */
class MainActivity : AppCompatActivity(), DialogFragmentListener {

    companion object {
        const val apiKey = "5ae2e3f221c38a28845f05b687642b306327601507b64f8893f6b77f"
        const val apiUrl = "https://api.opentripmap.com/0.1/en/places/radius?radius=20000"
        const val numPeticiones = 20
    }

    private lateinit var myLoc:MyLocationNewOverlay
    private lateinit var mapa: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var myLat: Double = 0.0
    var myLong: Double = 0.0
    private var lat: Double = 0.0
    private var long: Double = 0.0
    private var categoriasList: List<String> = listOf()

    /**
     * Acción que se ejecuta al crear la actividad. En ella se crea la configuración para usar el mapa de OpenStreetMao(osm).
     * Tras esto se solicitan los diferentes permisos necesarios para acceder a la ubicación del dispositivo y se hace una llamada a la API para obtener los lugares de interés.
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * Configuración necesaria para usar OSM.
         */
        val config = Configuration.getInstance()
        config.userAgentValue = packageName
        config.load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))
        setContentView(R.layout.activity_main)

        /**
         * Configuración del Mapa.
         */
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mapa = findViewById(R.id.mapView)
        mapa.setMultiTouchControls(true)

        /**
         * Comprobamos los permisos necesarios.
         * Si se tienen los permisos, se obtienen los lugares de interés, en caso contrario, se solicitan.
         */
        if((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        } else {
            obtenerUbicacionActual()
        }
    }

    /**
     * Función usada para obtener la ubicación actual del dispositivo.
     */
    private fun obtenerUbicacionActual() {
        /**
         * Primero se hace una verificación de los permisos.
         */
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        }
        /**
         * Comprobamos si existe una ubicación anterior. Si existe usamos esa latitud y longitud para obtener los lugares.
         */
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    myLat = location.latitude
                    myLong = location.longitude
                    setupMap()
                } else {
                    /**
                     * Si no hay una ubicación anterior, se hace una petición para obtener la ubicación.
                     */
                    val locationRequest = LocationRequest()
                    locationRequest.interval = 10000
                    locationRequest.fastestInterval = 5000
                    locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            super.onLocationResult(locationResult)
                            val location = locationResult.lastLocation
                            if (location != null) {
                                myLat = location.latitude
                                myLong = location.longitude
                                setupMap()
                            }
                        }
                    }
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Error de ubicación", exception.message.toString())
                Toast.makeText(this, "Error al obtener la ubicación actual", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Función usada para crear el menú de opciones.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_filtro, menu)
        return true
    }

    /**
     * Función para manejar la selección de una opción del menú.
     * @param item representa el objeto del menu, en este caso el único existente es el filtro.
     */

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                mostrarFiltro()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Función usada para mostrar el filtro. Se crea el fragmento deonde se encuentra y se le pasa la lista de categorías seleccionadas.
     */
    private fun mostrarFiltro() {
        val filter = FilterFragment()
        val args = Bundle().apply {
            putStringArrayList("cate", ArrayList(categoriasList))
        }
        filter.arguments = args
        filter.show(supportFragmentManager, "FilterDialog")
    }


    /**
     * Función que se ejecuta tras la comprobación de permisos.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionActual()

        } else {
            Toast.makeText(this, "Se requieren permisos de ubicación para mostrar el mapa.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Función para ajustar el mapa. Situamos el centro del mapa en la ubicación actual del dispositivo, añadimos un marcador para conocer esa ubicación y se muestran los lugares.
     */
    fun setupMap() {
        val point = GeoPoint(myLat, myLong) // Lugar donde nos encontramos
        // Centramos el mapa y le damos zoom
        mapa.controller.setCenter(point)
        mapa.controller.setZoom(10.0)

        // Calculamos nuestra ubicación en el mapa y la marcamos
        myLoc = MyLocationNewOverlay(GpsMyLocationProvider(this), mapa)
        myLoc.enableMyLocation()
        mapa.overlays.add(myLoc)

        mostrarLugares()
    }

    /**
     * Función usada para obtener los lugares de interés.
     * Se realiza la petición a la API. Si tiene éxito, se muestran los lugares, en caso contrario, se notifica del error.
     */
    private fun mostrarLugares() {
        val client = OkHttpClient()

        // Creamos la petición a la API de openTripMap
        val request = Request.Builder()
            .url("$apiUrl&lon=$myLong&lat=$myLat&kinds=interesting_places&limit=$numPeticiones&apikey=$apiKey")
            .build()

        // Realizamos la petición
        client.newCall(request).enqueue(object : Callback {
            // Manejo de errores de conexión
            override fun onFailure(call: Call, e: IOException) {
                Log.d("Error de conexión", e.message.toString())
            }

            // Respuesta
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val lugares = gson.fromJson(responseBody, RespuestaLugares::class.java)

                // Mostrar los lugares en el mapa
                runOnUiThread {
                    mostrarMarcadores(lugares)
                }
            }
        })
    }
    /**
     * Función usada para obtener los lugares de interés filtrados por categorías.
     * Se realiza la petición a la API. Si tiene éxito, se muestran los lugares, en caso contrario, se notifica del error.
     * @param categorias Lista de categorías a filtrar.
     */
    private fun mostrarCategoria(categorias:List<String>) {
        val client = OkHttpClient()
        val cats = categorias.joinToString(",")
        Log.d("Cats: ",cats)
        // Creamos la petición a la API de openTripMap
        val request = Request.Builder()
            .url("$apiUrl&lon=$myLong&lat=$myLat&kinds=$cats&limit=$numPeticiones&apikey=$apiKey")
            .build()

        // Realizamos la petición
        client.newCall(request).enqueue(object : Callback {
            // Manejo de errores de conexión
            override fun onFailure(call: Call, e: IOException) {
                Log.d("Error de conexión", e.message.toString())
            }

            // Respuesta
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val lugares = gson.fromJson(responseBody, RespuestaLugares::class.java)

                // Mostrar todos los lugares en el mapa
                runOnUiThread {
                    mostrarMarcadores(lugares)
                }
            }
        })
    }

    /**
     * Función usada para añadir los marcdores a los lugares proporcionados por la API.
     * @param lugares Los lugares obtenidos tras la respuesta de la API.
     */
    fun mostrarMarcadores(lugares: RespuestaLugares) {
        for (feature in lugares.features) {
            val coordinates = feature.geometry.coordinates
            val name = feature.properties.name
            val xid = feature.properties.xid
            val kinds = feature.properties.kinds
            val marker = Marker(mapa)
            marker.position = GeoPoint(coordinates[1], coordinates[0])
            lat = coordinates[1]
            long = coordinates[0]
            marker.title = name
            if (kinds.contains("industrial_facilities")) {//architecture
                marker.icon = ContextCompat.getDrawable(this, R.drawable.factory)
            } else if (kinds.contains("cultural")) {
                marker.icon = ContextCompat.getDrawable(this, R.drawable.library)
            } else if (kinds.contains("religion")) {
                marker.icon = ContextCompat.getDrawable(this, R.drawable.church)
            }else if(kinds.contains("natural")) {
                marker.icon = ContextCompat.getDrawable(this, R.drawable.pine_tree)
            }else if(kinds.contains("historic")) {
                marker.icon = ContextCompat.getDrawable(this, R.drawable.temple_buddhist)
            }else if (kinds.contains("architecture")){
                marker.icon = ContextCompat.getDrawable(this, R.drawable.castle)
            } else {
                // Otros casos
                marker.icon = ContextCompat.getDrawable(this, R.drawable.city)
            }
            marker.setOnMarkerClickListener { _, _ ->
                if(marker.isInfoWindowOpen){
                    marker.closeInfoWindow()
                    true
                }else{
                    mostrarInfoLugar(marker, name, xid)
                    true
                }
            }
            mapa.overlays.add(marker)
        }
        mapa.overlays.add(myLoc)
        mapa.invalidate()
    }

    /**
     * Función usada para mostrar la información del lugar cuando se pincha en un marcador.
     * @param marker Marcador seleccionado.
     * @param title Nombre del lugar.
     * @param xid ID del lugar dentro de la API para realizar la petición que proporciona más detalles sobre el lugar.
     */
    private fun mostrarInfoLugar(marker: Marker, title: String, xid: String) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.info_window_layout, null)

        val titulo = view.findViewById<TextView>(R.id.bubble_title)
        titulo.text = title
        val client = OkHttpClient()
        val url = "https://api.opentripmap.com/0.1/en/places/xid/$xid?apikey=${apiKey}"
        val request = Request.Builder()
            .url(url)
            .build()
        client.newCall(request).enqueue(object : Callback {
            // Manejo de errores de conexión
            override fun onFailure(call: Call, e: IOException) {
                Log.d("Error de conexión", e.message.toString())
            }

            // Respuesta
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val respuestaJson = JSONObject(responseBody.toString())

                //Imagen
                val imageView = view.findViewById<ImageView>(R.id.bubble_image)
                val imagenString = respuestaJson.optString("preview")
                val foto: String
                if(imagenString != ""){
                    val imagenObect = JSONObject(imagenString)
                    foto = imagenObect.optString("source", "")
                    cargarImagen(foto, imageView)
                }else{
                    foto = "https://media.istockphoto.com/id/1328286878/es/vector/estatua-gen%C3%A9rica-con-dibujo-rel%C3%A1mpago.jpg?s=612x612&w=0&k=20&c=WeeVDQbOr61yAJJEUox4eh7BrAu6uw6TgNOsgGvEj8o="
                    cargarImagen(foto, imageView)
                }
                //web
                view.findViewById<Button>(R.id.wikipediaButton).setOnClickListener {
                    val web = respuestaJson.optString("otm","")
                    abrirURL(web)
                }

                // Navegación
                view.findViewById<Button>(R.id.navigationButton).setOnClickListener {
                    val gmmIntentUri = Uri.parse("geo:${marker.position.latitude},${marker.position.longitude}?q=${marker.position.latitude},${marker.position.longitude}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    startActivity(mapIntent)
                }

                // Compartir
                view.findViewById<Button>(R.id.shareButton).setOnClickListener {
                    compartirUbicacion()
                }

            }
        })
        val markerInfoWindow = CustomMarkerInfoWindow(view,mapa)
        marker.infoWindow = markerInfoWindow
        marker.showInfoWindow()

    }

    /**
     * Función usada para cargar una imagen dentro del ImageView que contiene la imagen del lugar.
     * @param url URL de la imagen.
     * @param imageView ImageView donde se visualizará la imagen.
     */
    fun cargarImagen(url: String, imageView: ImageView) {
        runOnUiThread {
            Glide.with(this)
                .load(url)
                .into(imageView)
        }
    }

    /**
     * Función usada para manejar el click al botón de Web de un lugar. Abre una nueva actividad que abre la URL en el navegador.
     * @param url URL de la web del lugar.
     */
    fun abrirURL(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    /**
     * Función que se ejecuta al presonar el botón de aplicar filro.
     * @param categorias categorías que se desean filtrar.
     */
    override fun onPositiveButtonClicked(categorias: List<String>) {
        categoriasList = categorias
        Log.d("Categorías: ",categorias.toString())
        if(categorias.isNotEmpty()){
            mapa.overlays.clear()
            mostrarCategoria(categoriasList)
        }else{
            mapa.overlays.clear()
            mostrarLugares()
        }
    }

    /**
     * Función que se ejecuta al pulsar el botón para compratir una ubicación.
     * Crea un intent para poder compartir la ubicación via WhatsUp.
     */
    fun compartirUbicacion(){
        val whatsAppMessage = "https://maps.google.com/maps?q=$lat,$long"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, whatsAppMessage)
            type = "text/plain"
            setPackage("com.whatsapp")
        }
        startActivity(sendIntent)
    }


}

/**
 * Clase que representa la respuesta de la API, ya que esta retorna una lista de features.
 * @property features Lista de features.
 */
data class RespuestaLugares(val features: List<Feature>)

/**
 * Clase que representa cada item dentro de la lista de features, en este caso, las coordenadas y las propiedades.
 * @property geometry Representa la geometría del lugar como una lista de coordenadas.
 * @property properties Propiedades del lugar.
 */
data class Feature(val geometry: Geometry, val properties: Properties)

/**
 * Clase que representa las coordenadas dentro de la respuesta.
 * @property coordinates Lista de coordenadas.
 */
data class Geometry(val coordinates: List<Double>)

/**
 * Clase que representa las propiedades de un objeto.
 * @property name Nombre del lugar.
 * @property xid ID del lugar.
 * @property kinds Categoría a la que pertenece el lugar.
 */
data class Properties(val name: String, val xid: String, val kinds: String)