package com.example.rentenv;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // Tempo em milissegundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_screen);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Iniciar a atividade EmptyActivity após o tempo de SPLASH_DURATION
                Intent intent = new Intent(SplashScreen.this, Mapa.class);
                startActivity(intent);
                finish(); // Encerrar a atividade da splash para que o usuário não possa voltar a ela
            }
        }, SPLASH_DURATION);
    }
}


