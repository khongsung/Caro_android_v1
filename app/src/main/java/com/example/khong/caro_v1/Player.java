package com.example.khong.caro_v1;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

public class Player extends AppCompatActivity {

    private Context context;
    Button btnPlayGame,btnSound;
    TextView tvTurn, tvNumberX, tvNumberO, tvTimePlay;
    final static int maxN = 15;
    private ImageView[][] ivCell = new ImageView[maxN][maxN];
    private Drawable[] drawCell = new Drawable[4];
    private int[][] valueCell = new int[maxN][maxN];
    private int turnPlay;
    private boolean firstMove = false, isClicked;
    private int xMove, yMove;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        context = this;

        btnPlayGame = findViewById(R.id.btnPlay);
        tvTurn = findViewById(R.id.tvTurn);
        tvNumberX = findViewById(R.id.tvNumberX);
        tvNumberO = findViewById(R.id.tvNumberO);
        tvTimePlay = findViewById(R.id.tvTimePlay);
        btnSound = findViewById(R.id.btnSound);

        if (MainActivity.song.isPlaying()) {
            btnSound.setBackgroundResource(R.drawable.sound_on);
            MainActivity.btnSound.setBackgroundResource(R.drawable.sound_on);
        }else {
            btnSound.setBackgroundResource(R.drawable.sound_off);
            MainActivity.btnSound.setBackgroundResource(R.drawable.sound_off);
        }

        btnSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.song.isPlaying()) {
                    MainActivity.song.pause();
                    btnSound.setBackgroundResource(R.drawable.sound_off);
                } else {
                    MainActivity.song.start();
                    btnSound.setBackgroundResource(R.drawable.sound_on);
                }
            }
        });

        setListen();
        loadResouce();
        designBoard();
    }

    long lStartTime, lPauseTime, lSystemTime = 0L;
    Handler handler = new Handler();
    boolean isRun;

    @Override
    public void onBackPressed() {
        timeStop();
        super.onBackPressed();
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            lSystemTime = SystemClock.uptimeMillis() - lStartTime;
            long lUpdateTime = lPauseTime + lSystemTime;
            long secs = (long)(lUpdateTime/1000);
            secs = 10-secs %60;
            tvTimePlay.setText(String.format("%02d",secs));
            if (secs==3 || secs ==2 || secs ==1 || secs == 0) {
                tvTimePlay.setTextColor(Color.RED);
                tvTimePlay.setAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.flash_tv));
            } else
            {
                tvTimePlay.setTextColor(Color.WHITE);
                tvTimePlay.setAnimation(null);
            }
            if(secs==0) {
                Toast.makeText(context, "Hết giờ", Toast.LENGTH_SHORT).show();
                if(turnPlay==1) {
                    tvTurn.setText("B là người thắng");
                } else {
                    tvTurn.setText("A là người thắng");
                }
                tvTimePlay.setText("00");
                isClicked = true;
                return;
            }
            handler.postDelayed(this,1000);
        }
    };

    void timeStart() {
        if(isRun)
            return;
        isRun = true;
        lStartTime = SystemClock.uptimeMillis();
        handler.postDelayed(runnable, 0);
    }

    void timeStop() {
        if(!isRun)
            return;
        isRun = false;
        lPauseTime = 0;
        handler.removeCallbacks(runnable);
    }

    private void setListen() {
        btnPlayGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timeStop();
                tvTimePlay.setText("10");
                tvTimePlay.setTextColor(Color.WHITE);
                tvTimePlay.setAnimation(null);
                initGame();
                playGame();
            }
        });
    }

    private void loadResouce() {
        drawCell[0] = null; // empty cell
        drawCell[1] = context.getResources().getDrawable(R.drawable.x);
        drawCell[2] = context.getResources().getDrawable(R.drawable.o);
        drawCell[3] = context.getResources().getDrawable(R.drawable.block); //background
    }

    private void initGame() {
        arrO.clear(); arrX.clear();
        tvNumberO.setText("0"); tvNumberX.setText("0");
        firstMove = true;
        winner_play = 0;
        for(int i=0; i<maxN; i++) {
            for(int j=0; j<maxN; j++) {
                ivCell[i][j].setImageDrawable(drawCell[0]);
                valueCell[i][j] = 0;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void designBoard() {
        int sizeofCell = Math.round(screenWith()/maxN);
        LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(sizeofCell*maxN, sizeofCell);
        LinearLayout.LayoutParams lpCell = new LinearLayout.LayoutParams(sizeofCell,sizeofCell);

        LinearLayout linBoardGame = (LinearLayout) findViewById(R.id.linBoardGame);

        //create cells
        for(int i=0; i<maxN; i++) {
            LinearLayout linRow = new LinearLayout(context);
            //make a rows
            for(int j=0; j<maxN; j++) {
                ivCell[i][j] = new ImageView(context);
                ivCell[i][j].setBackground(drawCell[3]);
                final int x = i;
                final int y = j;
                ivCell[i][j].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(valueCell[x][y] == 0) {
                            if(!isClicked) {
                                isClicked = true;
                                xMove = x;
                                yMove = y;
                                makeMove();
                            }
                        }
                    }
                });
                linRow.addView(ivCell[i][j],lpCell);
            }
            linBoardGame.addView(linRow, lpRow);
        }
    }

    private float screenWith() {
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        return dm.widthPixels;
    }

    private void playGame() {
        Random random = new Random();
        turnPlay = random.nextInt(2) + 1;
        if (turnPlay == 1) {
            Toast.makeText(context, "người A chơi", Toast.LENGTH_SHORT).show();
            playerTurn();
        } else {
            Toast.makeText(context,"Người B chơi", Toast.LENGTH_SHORT).show();
            botTurn();
        }
    }

    private void playerTurn() {
        tvTurn.setText("Người A");
        isClicked = false;
    }

    private void botTurn() {
        tvTurn.setText("Người B");
        isClicked = false;
    }

    ArrayList<Integer> arrX = new ArrayList<Integer>();
    ArrayList<Integer> arrO = new ArrayList<Integer>();

    private void makeMove() {
        ivCell[xMove][yMove].setImageDrawable(drawCell[turnPlay]);
        valueCell[xMove][yMove] = turnPlay;

        if (firstMove) {
            timeStop();
            timeStart();
            if(turnPlay == 1) {
                arrX.add(turnPlay);
                tvNumberX.setText(String.valueOf(arrX.size()));
            } else {
                arrO.add(turnPlay);
                tvNumberO.setText(String.valueOf(arrO.size()));
            }
        }

        if (noEmptycell()) {
            Toast.makeText(context, "Draw!!!", Toast.LENGTH_SHORT).show();
            return;
        } else if (CheckWinner()) {
            timeStop();
            if (winner_play == 1) {
                Toast.makeText(context, "A là người thắng", Toast.LENGTH_SHORT).show();//
                tvTurn.setText("A là người thắng");
            } else {
                Toast.makeText(context, "B là người thắng", Toast.LENGTH_SHORT).show();//
                tvTurn.setText("B là người thắng");
            }


            return;
        }

        if(turnPlay == 1) {
            turnPlay = (1+2) - turnPlay;
            botTurn();
        } else {
            turnPlay = 3 - turnPlay;
            playerTurn();
        }
    }

    private boolean noEmptycell() {
        for(int i=0;i<maxN;i++){
            for(int j=0;j<maxN;j++)
                if(valueCell[i][j]==0) return false;
        }
        return true;
    }

    private int winner_play;
    private boolean CheckWinner() {
        //we only need to check the recent move xMove,yMove can create 5 cells in a row or not
        if(winner_play!=0) return true;
        //check in row =( i forget that :D
        VectorEnd(xMove,0,0,1,xMove,yMove);
        //check column
        VectorEnd(0,yMove,1,0,xMove,yMove);
        //check left to right
        if(xMove+yMove>=maxN-1){
            VectorEnd(maxN-1,xMove+yMove-maxN+1,-1,1,xMove,yMove);
        } else{
            VectorEnd(xMove+yMove,0,-1,1,xMove,yMove);
        }
        //check right to left
        if(xMove<=yMove){
            VectorEnd(xMove-yMove+maxN-1,maxN-1,-1,-1,xMove,yMove);
        }else{
            VectorEnd(maxN-1,maxN-1-(xMove-yMove),-1,-1,xMove,yMove);
        }
        if(winner_play!=0) return true; else return false;
    }

    private void VectorEnd(int xx, int yy, int vx, int vy, int rx, int ry) {
        //this void will check the row base on vector(vx,vy) in range (rx,ry)-4*(vx,vy) -> (rx,ry)+4*(vx,vy)
        //ok i will explain this :) hope you understand :D if not yet feel free to comment below i will help you
        if(winner_play!=0) return;
        final int range=4;
        int i,j;
        int xbelow=rx-range*vx;
        int ybelow=ry-range*vy;
        int xabove=rx+range*vx;
        int yabove=ry+range*vy;
        String st="";
        i=xx;j=yy;
        while(!inside(i,xbelow,xabove)||!inside(j,ybelow,yabove)){
            i+=vx;j+=vy;
        }
        while(true){
            st=st+String.valueOf(valueCell[i][j]);
            if(st.length()==5){
                EvalEnd(st);
                st=st.substring(1,5);//substring of st from index 1->5;=> delete first character
            }
            i+=vx;j+=vy;
            if(!inBoard(i,j) || !inside(i,xbelow,xabove)|| !inside(j,ybelow,yabove) || winner_play!=0){
                break;
            }
        }
    }

    private boolean inside(int i, int xbelow, int xabove) {//this check i in [xbelow,xabove] or not
        return (i-xbelow)*(i-xabove)<=0;
    }

    private boolean inBoard(int i, int j) {
        //check i,j in board or not
        if(i<0||i>maxN-1||j<0||j>maxN-1) return false;
        return true;
    }

    private void EvalEnd(String st) {
        switch (st){
            case "11111": winner_play=1;break;
            case "22222": winner_play=2;break;
            default:break;
        }
    }
}
