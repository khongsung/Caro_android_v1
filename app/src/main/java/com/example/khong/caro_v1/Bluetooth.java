package com.example.khong.caro_v1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

public class Bluetooth extends AppCompatActivity {

    private Context context;
    Button btnPlayAgain, btnConnect, btnSound;
    TextView tvTurn,tvNumberX,tvNumberO, tvTimePlay;
    final static int maxN = 15;
    private ImageView[][] ivCell = new ImageView[maxN][maxN];
    private Drawable[] drawCell = new Drawable[4];
    private int[][] valueCell = new int[maxN][maxN];
    private int turnPlay;
    private boolean firstMove = false, isClicked, arrNumber = false;
    private int xMove, yMove;

    private TextView status;
    private ListView listView;
    private Dialog dialog;
    private ArrayAdapter<String> chatAdapter;
    private ArrayList<String> chatMessages;
    private BluetoothAdapter bluetoothAdapter;

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private ChatController chatController;
    private BluetoothDevice connectingDevice;
    private ArrayAdapter<String> discoveredDevicesAdapter;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_OBJECT = "device_name";

    public Bluetooth() {
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        context = this;
        Anhxa();

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

        playAgaint();
        loadResouce();
        designBoard();

        //bluetooth
        //check device support bluetooth or not
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
            finish();
        }

        //show bluetooth devices dialog when click connect button
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPrinterPickDialog();
                initGame();
                playGame();
            }
        });

        //set chat adapter
        chatMessages = new ArrayList<>();
        chatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chatMessages);
        //listView.setAdapter(chatAdapter);
    }

    void Anhxa() {
        btnPlayAgain = findViewById(R.id.btnPlayAgain);
        tvTurn = findViewById(R.id.tvTurn);
        btnConnect= findViewById(R.id.btn_connect);
        status = (TextView) findViewById(R.id.status);
        tvNumberO = findViewById(R.id.tvNumberO);
        tvNumberX = findViewById(R.id.tvNumberX);
        btnSound = findViewById(R.id.btnSound);
        tvTimePlay = findViewById(R.id.tvTimePlay);
    }

    long lStartTime, lPauseTime, lSystemTime = 0L;
    Handler timerPlay = new Handler();
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
                sendMessage("hethoigianchoibandathua");
                tvTurn.setText("Hết giờ, Mình thua");
                isClicked = true;
                return;
            }
            timerPlay.postDelayed(this,1000);
        }
    };

    void timeStart() {
        if(isRun)
            return;
        isRun = true;
        lStartTime = SystemClock.uptimeMillis();
        timerPlay.postDelayed(runnable, 0);
    }

    void timeStop() {
        if(!isRun)
            return;
        isRun = false;
        lPauseTime = 0;
        timerPlay.removeCallbacks(runnable);
    }

    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ChatController.STATE_CONNECTED:
                            setStatus("Connected to: " + connectingDevice.getName());
                            btnConnect.setEnabled(false);
                            initGame();
                            playGame();
                            break;
                        case ChatController.STATE_CONNECTING:
                            setStatus("Connecting...");
                            btnConnect.setEnabled(false);
                            break;
                        case ChatController.STATE_LISTEN:
                        case ChatController.STATE_NONE:
                            setStatus("Not connected");
                            btnConnect.setEnabled(true);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                    String writeMessage = new String(writeBuf);
                    chatMessages.add(writeMessage);

                    if (writeMessage.length() == 13) {
                        tvTurn.setText("Mình thắng");
                        isClicked = true;
                    } else if (writeMessage.length() == 9) {
                        tvTurn.setText("Mình");
                    } else if(writeMessage.length() == 23) {
                        timeStop();
                        tvTurn.setText("Hết thời gian, mình thua");
                        isClicked = true;
                    } else {
                        tvTurn.setText("Khách");
                    }
                    //chatAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;

                    String readMessage = new String(readBuf, 0, msg.arg1);
                    chatMessages.add(readMessage);
                    //create a move
                    String point = String.valueOf(chatMessages.get(chatMessages.size()-1));
                    if (readMessage.length() == 8) {
                        //if partner say yes also play game again, unless game will be continute
                        initGame();
                        playGame();
                    } else if (readMessage.length() == 13) {
                        tvTurn.setText("Mình thua!");
                        isClicked = true;
                        timeStop();
                    } else if(9 == readMessage.length()) {
                        //play again
                        clickAgain();
                        tvTimePlay.setText("10");
                        tvTimePlay.setTextColor(Color.WHITE);
                        tvTimePlay.setAnimation(null);
                    } else if (readMessage.length() == 23) {
                        tvTurn.setText("Hết thời gian");
                        timeStop();
                        isClicked = true;
                    }
                    else {
                        //put out xMove, yMove
                        final String[] arr = point.split("\\s");
                        tvTurn.setText("Mình");
                        makeMove(Integer.valueOf(arr[0]), Integer.valueOf(arr[1]));
                        //chatAdapter.notifyDataSetChanged();
                    }

                    break;
                case MESSAGE_DEVICE_OBJECT:
                    connectingDevice = msg.getData().getParcelable(DEVICE_OBJECT);
                    Toast.makeText(getApplicationContext(), "Connected to " + connectingDevice.getName(),
                            Toast.LENGTH_SHORT).show();
                    //play
                    initGame();
                    playGame();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    public void clickAgain() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        // set title
        alertDialogBuilder.setTitle("Đề xuất");
        // set dialog message
        alertDialogBuilder
                .setMessage("Đối phương muốn chơi lại! Bạn có đồng ý?")
                .setCancelable(false)
                .setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        initGame();
                        playGame();
                        sendMessage("okcancel");
                    }
                })
                .setNegativeButton("Đíu nghe",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }

    private void setStatus(String s) {
        status.setText(s);
    }

    private void showPrinterPickDialog() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_bluetooth);
        dialog.setTitle("Bluetooth Devices");

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        //Initializing bluetooth adapters
        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        //locate listviews and attatch the adapters
        ListView listView = (ListView) dialog.findViewById(R.id.pairedDeviceList);
        ListView listView2 = (ListView) dialog.findViewById(R.id.discoveredDeviceList);
        listView.setAdapter(pairedDevicesAdapter);
        listView2.setAdapter(discoveredDevicesAdapter);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryFinishReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryFinishReceiver, filter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDevicesAdapter.add(getString(R.string.none_paired));
        }

        //Handling listview item click event
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                dialog.dismiss();
            }

        });

        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                    chatController = new ChatController(this, handler);
                } else {
                    Toast.makeText(this, "Bluetooth still disabled, turn off application!", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void sendMessage(String message) {
        if (chatController.getState() != ChatController.STATE_CONNECTED) {
            Toast.makeText(this, "Connection was lost!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            chatController.write(send);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            chatController = new ChatController(this, handler);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (chatController != null) {
            if (chatController.getState() == ChatController.STATE_NONE) {
                chatController.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatController != null)
            chatController.stop();
    }

    private final BroadcastReceiver discoveryFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    discoveredDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (discoveredDevicesAdapter.getCount() == 0) {
                    discoveredDevicesAdapter.add(getString(R.string.none_found));
                }
            }
        }
    };

    private void connectToDevice(String deviceAddress) {
        bluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        chatController.connect(device);
    }

    private void playAgaint() {
        btnPlayAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timeStop();
                tvTimePlay.setText("10");
                tvTimePlay.setTextColor(Color.WHITE);
                tvTimePlay.setAnimation(null);
                if (tvTurn.getText() == "Khách") {
                    Toast.makeText(context,"Chưa đến lượt", Toast.LENGTH_LONG).show();
                } else  {
                    sendMessage("playagain");
                }
            }
        });
    }

    private void loadResouce() {
        drawCell[0] = null; // empty cell
        drawCell[1] = context.getResources().getDrawable(R.drawable.x);
        drawCell[2] = context.getResources().getDrawable(R.drawable.o);
        drawCell[3] = context.getResources().getDrawable(R.drawable.block); //background
    }

    private int winner_play;
    private void initGame() {
        arrX.clear();
        arrO.clear();
        tvNumberO.setText("0");
        tvNumberX.setText("0");
        firstMove = true;
        arrNumber = true;
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
                        if(tvTurn.getText().toString() == "Khách") {
                            isClicked = true;
                        } else {
                            if (valueCell[x][y] == 0) {
                                if(!isClicked) {
                                    isClicked = true;
                                    xMove = x;
                                    yMove = y;
                                    if(winner_play == 0) {
                                        sendMessage(String.valueOf(x) + " " + String.valueOf(y));
                                        makeMove(xMove, yMove);
                                    }

                                    if(chatMessages.size()==225) {
                                        Toast.makeText(context, "Hòa cờ", Toast.LENGTH_LONG).show();
                                    }
                                }
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
        //turnPlay = random.nextInt(2) + 1;
        turnPlay = 1;
        playerTurn();
    }

    private void playerTurn() {
        firstMove = true;
        isClicked = false;
    }

    ArrayList<Integer> arrX = new ArrayList<Integer>();
    ArrayList<Integer> arrO = new ArrayList<Integer>();

    private void makeMove(int x, int y) {
        ivCell[x][y].setImageDrawable(drawCell[turnPlay]);
        valueCell[x][y] = turnPlay;

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
        //ckeck win
        if (CheckWinner()) {
            timeStop();
            if (winner_play != 0) {
                sendMessage("partnerwinner");
                timeStop();
            }
            return;
        }
        if(turnPlay == 1) {
            turnPlay = (1+2) - turnPlay;
        } else {
            turnPlay = 3 - turnPlay;
        }
        playerTurn();
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
