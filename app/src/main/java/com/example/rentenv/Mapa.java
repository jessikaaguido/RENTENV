package com.example.rentenv;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;


public class Mapa extends AppCompatActivity {
    private static final int REQUEST_PERMISSIONS_CODE = 1;
    TextView distanceTextView;
    Button confirmButton;
    private MapView mapView;
    IMapController mapController;
    MyLocationNewOverlay myLocationOverlay;
    private RoadManager roadManager;
    private GeoPoint startPoint;
    private GeoPoint endPoint;
    Marker startMarker;
    Marker endMarker;
    private Polyline roadOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa a configuração do OSMDroid
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue("com.example.rentenv");

        // Inicializa o mapa
        mapView = findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        //mapView.setBuiltInZoomControls(true); // DEPRECADA
        mapView.getController().setZoom(18.0);
        mapView.setMultiTouchControls(true);

        // Inicialize o controlador do mapa para setar posições
        mapController = mapView.getController();
        mapController.setCenter(startPoint);
        mapController.setZoom(18.0);

        // Solicita permissões de usuário (caso ainda não tenha sido concedido)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions(new String[] {
                    android.Manifest.permission.INTERNET,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }

        // Inicializa o RoadManager
        roadManager = new OSRMRoadManager(this, "com.example.rentenv");

        // Inicializa a sobreposição de localização
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Obtém a localização atual do usuário
            GeoPoint userLocation = myLocationOverlay.getMyLocation();
            if (userLocation != null) {
                // Define o centro do mapa como a localização atual do usuário
                mapController.setCenter(userLocation);
                // Define um nível de zoom adequado para visualizar a localização do usuário
                mapController.setZoom(30.0); // Você pode ajustar esse valor conforme necessário
            }
        }

        // Adiciona pontos pré-determinados (Minicampus da UFAM e Amazonas Shopping) como marcadores
        startPoint = new GeoPoint(-3.090675, -59.963054); // Minicampus da UFAM
        endPoint = new GeoPoint(-3.093767, -60.022682); // Amazonas Shopping

        // Cria um marcador no mapa: Posição 1
        startMarker = new Marker(mapView);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle("UFAM Setor-Norte");

        // Cria um marcador no mapa: Posição 2
        endMarker = new Marker(mapView);
        endMarker.setPosition(endPoint);
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        endMarker.setTitle("Amazonas Shopping");

        // adiciona marcadores (markers) ao mapa
        mapView.getOverlays().add(startMarker);
        mapView.getOverlays().add(endMarker);


        //cria uma variável pra receber eventos no mapa
        MapEventsReceiver mReceive = new MapEventsReceiver(){
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
                // Verifica qual marcador foi clicado
                if (startPoint.distanceToAsDouble(geoPoint) < 500) { // Exemplo de distância de clique
                    // Traça a rota até o Minicampus da UFAM
                    calculateRoad(myLocationOverlay.getMyLocation(), startMarker.getPosition());
                    return true;
                } else if (endPoint.distanceToAsDouble(geoPoint) < 500) { // Exemplo de distância de clique
                    // Traça a rota até o Amazonas Shopping
                    calculateRoad(myLocationOverlay.getMyLocation(), endMarker.getPosition());
                    return true;
                }
                return false;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false; // não realiza qualquer ação
            }
        };

        // Configura o evento de clique no mapa para traçar a rota: adiciona a callback de eventos ao mapa
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mReceive);
        mapView.getOverlays().add(0, mapEventsOverlay);

        // Inicializa os controles de distância
        distanceTextView = findViewById(R.id.distanceTextView);

        //eventos do botão "confirmar"
        confirmButton = findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (roadOverlay != null) {
                    double distance = roadOverlay.getDistance();
                    distanceTextView.setText("Distância da rota: " + String.format("%.2f", distance));
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    private void calculateRoad(GeoPoint start, GeoPoint end) {
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        waypoints.add(start);
        waypoints.add(end);

        Road road = roadManager.getRoad(waypoints);
        if (road.mStatus == Road.STATUS_OK) {
            if (roadOverlay != null) {
                mapView.getOverlays().remove(roadOverlay);
            }
            //roadOverlay = RoadManager.buildRoadOverlay(road, this);
            roadOverlay = RoadManager.buildRoadOverlay(road);
            mapView.getOverlays().add(roadOverlay);
            mapView.invalidate();
        } else {
            Toast.makeText(this, "Não foi possível calcular a rota", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para solicitar permissões de localização e armazenamento
    private void checkPermissions(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSIONS_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    // Permissão negada
                    Toast.makeText(this, "Permissão negada: " + permissions[i], Toast.LENGTH_SHORT).show();
                }
                else {
                    // Permissão cedida, recria a activity para carregar o mapa, só será executado uma vez
                    this.recreate();
                }
            }
        }
    }
}

