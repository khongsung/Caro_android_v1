package com.example.khong.caro_v1;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Bot extends AppCompatActivity {

    private Context context;
    Button btnPlayGame, btnSound;
    TextView tvTurn, tvNumberX, tvNumberO, tvTimePlay;
    final static int maxN = 15;
    private ImageView[][] ivCell = new ImageView[maxN][maxN];
    private Drawable[] drawCell = new Drawable[4];
    private int[][] valueCell = new int[maxN][maxN];
    private int turnPlay;
    private boolean firstMove = false, isClicked, arrNumber = false;
    private int xMove, yMove;
    private int winner_play;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot);
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
                tvTurn.setText("Máy thắng");
                tvTimePlay.setText("00");
                timeStop();
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
        btnPlayGame = (Button) findViewById(R.id.btnPlay);
        tvTurn = (TextView) findViewById(R.id.tvTurn);

        btnPlayGame.setText("Chơi Game");
        tvTurn.setText("Nhấn chơi game để chơi");

        btnPlayGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                init_game();
                play_game();
                tvTimePlay.setText("10");
                tvTimePlay.setTextColor(Color.WHITE);
                tvTimePlay.setAnimation(null);
            }
        });
    }

    private void init_game() {
        arrO.clear(); arrX.clear();
        tvNumberO.setText("0"); tvNumberX.setText("0");
        firstMove = true;
        arrNumber = true;
        winner_play = 0;
        for (int i = 0; i < maxN; i++) {
            for (int j = 0; j < maxN; j++) {
                ivCell[i][j].setImageDrawable(drawCell[0]);//default or Empty cell
                valueCell[i][j] = 0;
            }
        }
    }

    private void play_game() {
        Random r = new Random();
        turnPlay = r.nextInt(2) + 1;//r.nextint(2) return value in [0,1]

        if (turnPlay == 1) {//player play first
            //inform => make a toast
            Toast.makeText(context, "Người chơi!", Toast.LENGTH_SHORT).show();//dont forget show(); :D
            playerTurn();
        } else {//bot turn
            Toast.makeText(context, "Máy chơi!", Toast.LENGTH_SHORT).show();//dont forget show(); :D
            botTurn();
        }

    }

    private void botTurn() {
        tvTurn.setText("Bot");
        //if this is first move bot always choose center cell (7,7)
        if (firstMove) {
            firstMove = false;
            xMove = 7;
            yMove = 7;
            makeMove();
        } else {
            //try to find best xMove,yMove
            findBotMove();
            makeMove();
        }
    }

    private final int[] iRow={-1,-1,-1,0,1,1,1,0};
    private final int[] iCol={-1,0,1,1,1,0,-1,-1};
    private void findBotMove() {
        List<Integer> listX = new ArrayList<Integer>();
        List<Integer> listY= new ArrayList<Integer>();
        //find empty cell can move, and we we only move two cell in range 2
        final int range=2;
        for(int i=0;i<maxN;i++){
            for(int j=0;j<maxN;j++)
                if(valueCell[i][j]!=0){//not empty
                    for(int t=1;t<=range;t++){
                        for(int k=0;k<8;k++){
                            int x=i+iRow[k]*t;
                            int y=j+iCol[k]*t;
                            if(inBoard(x,y) && valueCell[x][y]==0){
                                listX.add(x);
                                listY.add(y);
                            }
                        }
                    }
                }
        }
        int lx=listX.get(0);
        int ly=listY.get(0);
        //bot always find min board_position_value
        int res= Integer.MAX_VALUE-10;
        for(int i=0;i<listX.size();i++){
            int x=listX.get(i);
            int y=listY.get(i);
            valueCell[x][y]=2;
            int rr=getValue_Position();
            if(rr<res){
                res=rr;lx=x;ly=y;
            }
            valueCell[x][y]=0;
        }
        xMove=lx;yMove=ly;
    }

    private int getValue_Position() {
        //this function will find the board_position_value
        int rr=0;
        int pl=turnPlay;
        //row
        for(int i=0;i<maxN;i++){
            rr+=CheckValue(maxN-1,i,-1,0,pl);
        }
        //column
        for(int i=0;i<maxN;i++){
            rr+=CheckValue(i, maxN - 1, 0, -1, pl);
        }
        //cross right to left
        for(int i=maxN-1;i>=0;i--){
            rr+=CheckValue(i,maxN-1,-1,-1,pl);
        }
        for(int i=maxN-2;i>=0;i--){
            rr+=CheckValue(maxN-1,i,-1,-1,pl);
        }
        //cross left to right
        for(int i=maxN-1;i>=0;i--){
            rr+=CheckValue(i,0,-1,1,pl);
        }
        for(int i=maxN-1;i>=1;i--){
            rr+=CheckValue(maxN-1,i,-1,1,pl);
        }
        return rr;
    }

    private int CheckValue(int xd, int yd, int vx, int vy, int pl) {
        //comback with check value
        int i,j;
        int rr=0;
        i=xd;j=yd;
        String st=String.valueOf(valueCell[i][j]);
        while(true){
            i+=vx;j+=vy;
            if(inBoard(i,j)){
                st=st+String.valueOf(valueCell[i][j]);
                if(st.length()==6){
                    rr+=Eval(st,pl);
                    st=st.substring(1,6);
                }
            } else break;
        }
        return rr;

    }

    //////////////funtion evaluate
    private int Eval(String st, int pl) {
        //this function is put score for 6 cells in a row
        //pl is player turn => you will get a bonus point if it's your turn
        //I will show you and explain how i can make it and what it mean in part improve bot move
        int b1 = 1, b2 = 1;
        if (pl == 1) {
            b1 = 2;
            b2 = 1;
        } else {
            b1 = 1;
            b2 = 2;
        }
        switch (st) {
            case "111110":return b1* 100000000;
            case "011111":return b1* 100000000;
            case "211111":return b1* 100000000;
            case "111112":return b1* 100000000;
            case "011110":return b1* 10000000;
            case "101110":return b1* 1002;
            case "011101":return b1* 1002;
            case "011112":return b1* 1000;
            case "011100":return b1* 102;
            case "001110":return b1* 102;
            case "210111":return b1* 100;
            case "211110":return b1* 100;
            case "211011":return b1* 100;
            case "211101":return b1* 100;
            case "010100":return b1* 10;
            case "011000":return b1* 10;
            case "001100":return b1* 10;
            case "000110":return b1* 10;
            case "211000":return b1* 1;
            case "201100":return b1* 1;
            case "200110":return b1* 1;
            case "200011":return b1* 1;
            case "222220":return b2* -100000000;
            case "022222":return b2* -100000000;
            case "122222":return b2* -100000000;
            case "222221":return b2* -100000000;
            case "022220":return b2* -10000000;
            case "202220":return b2* -1002;
            case "022202":return b2* -1002;
            case "022221":return b2* -1000;
            case "022200":return b2* -102;
            case "002220":return b2* -102;
            case "120222":return b2* -100;
            case "122220":return b2* -100;
            case "122022":return b2* -100;
            case "122202":return b2* -100;
            case "020200":return b2* -10;
            case "022000":return b2* -10;
            case "002200":return b2* -10;
            case "000220":return b2* -10;
            case "122000":return b2* -1;
            case "102200":return b2* -1;
            case "100220":return b2* -1;
            case "100022":return b2* -1;
            default:
                break;
        }
        return 0;
    }

    private void playerTurn() {
        tvTurn.setText("Player");
        firstMove=false;
        isClicked = false;
        /// we get xMove,yMove of player by the way listen click on cell so turn listen on
    }

    private void loadResouce() {
        drawCell[0] = null; // empty cell
        drawCell[1] = context.getResources().getDrawable(R.drawable.x);
        drawCell[2] = context.getResources().getDrawable(R.drawable.o);
        drawCell[3] = context.getResources().getDrawable(R.drawable.block); //background
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

    ArrayList<Integer> arrX = new ArrayList<Integer>();
    ArrayList<Integer> arrO = new ArrayList<Integer>();

    private void makeMove() {
            if(arrNumber) {
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

        ivCell[xMove][yMove].setImageDrawable(drawCell[turnPlay]);
        valueCell[xMove][yMove] = turnPlay;
        if (noEmptycell()) {
            Toast.makeText(context, "Draw!!!", Toast.LENGTH_SHORT).show();
            return;
        } else if (CheckWinner()) {
            timeStop();

            if (winner_play == 1) {
                Toast.makeText(context, "Người chơi thắng", Toast.LENGTH_SHORT).show();//
                tvTurn.setText("Người chơi đã thắng!");
            } else {
                Toast.makeText(context, "Máy thắng", Toast.LENGTH_SHORT).show();//
                tvTurn.setText("Máy đã thắng");
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
