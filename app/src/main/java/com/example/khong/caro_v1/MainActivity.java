package com.example.khong.caro_v1;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    Button btnThoat, btnBot, btnPlayer, btnBluetooth;
    public static Button btnSound;
    AlertDialog dialog;

    public static MediaPlayer song;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Anhxa();

        song = MediaPlayer.create(MainActivity.this, R.raw.music);
        song.start();

        btnSound.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (song.isPlaying()) {
                    btnSound.setBackgroundResource(R.drawable.sound_off);
                    song.pause();
                } else {
                    btnSound.setBackgroundResource(R.drawable.sound_on);
                    song.start();
                }
            }
        });

        AddEvent();

    }

    protected void Anhxa() {
        btnThoat = findViewById(R.id.btn_thoat);
        btnPlayer = findViewById(R.id.btn_player);
        btnBot = findViewById(R.id.btn_bot);
        btnBluetooth = findViewById(R.id.btn_bluetooth);
        btnSound = findViewById(R.id.btnSound);
    }

    protected void AddEvent() {
        btnThoat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowAlertDialog();
            }
        });

        btnPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnPlayer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.scale_btn));

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, Player.class);
                        startActivity(intent);
                    }
                }, 100);
            }
        });

        btnBot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnBot.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.scale_btn));
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, Bot.class);
                        startActivity(intent);
                    }
                }, 100);
            }
        });

        btnBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnBluetooth.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.scale_btn));
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, Bluetooth.class);
                        startActivity(intent);
                    }
                }, 100);
            }
        });
    }

    private void ShowAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Game Caro");
        builder.setMessage("Bạn chắc chắn muốn thoát không?");
        builder.setCancelable(false);
        builder.setPositiveButton("Ứ chịu", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                System.exit(0);
            }
        });

        dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        ShowAlertDialog();
    }
}
