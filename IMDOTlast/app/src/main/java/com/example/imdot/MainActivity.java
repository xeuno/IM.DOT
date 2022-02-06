package com.example.imdot;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;

public class MainActivity extends AppCompatActivity {

    private SurfaceView surfaceView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private TextToSpeech tts;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private String  productName,productSellByDate,productPrice, barcodeData_text;
    public static final String TAG = "MainActivity";
    private static String DATABASE_NAME = null;
    private static String TABLE_NAME = "employee";
    private static int DATABASE_VERSION = 1;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private String editTextAddress="Input New ip address", editTextPort="8888";
    static int count=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sf = getSharedPreferences("mode_File",MODE_PRIVATE);
        count = sf.getInt("mode", 0); //key, value => 키값없으면 디폴트값

        barcodeData_text="";
        productName="";
        surfaceView = findViewById(R.id.surface_view);

        initialiseDetectorsAndSources();

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR){
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });

        //intent 넘겨주는 부분
        Intent intent1 = getIntent();
        editTextAddress=intent1.getStringExtra("ip_address");
    }

    //추가한 부분 프로그램 종료시 실행
    @Override
    protected void onStop() {
        super.onStop();

        // Activity가 종료되기 전에 저장
        //SharedPreferences를 mode_File이름, 기본모드로 설정
        SharedPreferences sharedPreferences = getSharedPreferences("mode_File",MODE_PRIVATE);

        //저장을 하기위해 editor를 이용하여 값을 저장시켜준다.
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("mode",count); // key, value를 이용하여 저장하는 형태

        //최종 커밋
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d("test", "onPrepareOptionsMenu - 옵션메뉴가 " +
                "화면에 보여질때 마다 호출됨");
        if(count==1){ //직원모드일 때
            menu.getItem(1).setIcon(R.drawable.staff);
        }else{ //시각장애인 모드일 때
            menu.getItem(1).setIcon(R.drawable.sound);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if (id == R.id.sound) {
            count+=1;
            count=count%2;
            if(count==1) {
                item.setIcon(R.drawable.staff);
                final Toast toast = Toast.makeText(this, "직원모드 입니다", Toast.LENGTH_SHORT);
                toast.show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() { @Override public void run() { toast.cancel(); } }, 900);
            }
            else if(count==0) {
                item.setIcon(R.drawable.sound);
                final Toast toast = Toast.makeText(this, "시각 장애인모드 입니다", Toast.LENGTH_SHORT);
                toast.show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() { @Override public void run() { toast.cancel(); } }, 900);
            }
            return true;
        }
        if (id == R.id.text_input) {
            Intent intent=new Intent(getApplicationContext(), textmemo.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("ip_address",editTextAddress);
            startActivity(intent);
            return true;
        }
        if (id == R.id.ip) {
            Intent intent = new Intent(getApplicationContext(), setip.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);//뒤로가기 했을 때 액티비티 다시 뜨지 않도록
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class MyClientTask extends AsyncTask<Void, Void, Void> {
        String dstAddress;
        int dstPort;
        String response = "";
        String myMessage = "";

        //constructor
        MyClientTask(String addr, int port, String message){
            dstAddress = addr;
            dstPort = port;
            myMessage = message;
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            Socket socket = null;
            myMessage = myMessage.toString();
            try {
                socket = new Socket(dstAddress, dstPort);
                //송신
                OutputStream out = socket.getOutputStream();
                out.write(myMessage.getBytes());

                //수신
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
                byte[] buffer = new byte[1024];
                int bytesRead;
                InputStream inputStream = socket.getInputStream();

                while ((bytesRead = inputStream.read(buffer)) != -1){
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                    response += byteArrayOutputStream.toString("UTF-8");
                }
                response = "서버의 응답: " + response;

            } catch (UnknownHostException e) {
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                e.printStackTrace();
                response = "IOException: " + e.toString();
            }finally{
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        super.onTouchEvent(event);
        boolean qr_code;
        Vibrator vibrator=(Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        if(count==0){
            if(event.getAction()==MotionEvent.ACTION_DOWN)
            {
                DATABASE_NAME = "database_barcode.db";
                boolean isOpen = openDatabase();
                if (isOpen) {
                    executeRawQuery();
                    executeRawQueryParam();
                }
                qr_code = Check_code(barcodeData_text);
                tts.setSpeechRate(0.9f);
                if (productName != ""&& qr_code == false)
                {
                    tts.speak("상품",TextToSpeech.QUEUE_FLUSH,null);
                    tts.speak(productName,TextToSpeech.QUEUE_ADD,null);
                    tts.speak("가격",TextToSpeech.QUEUE_ADD,null);
                    tts.speak(productPrice,TextToSpeech.QUEUE_ADD,null);
                    tts.speak("유통기한",TextToSpeech.QUEUE_ADD,null);
                    tts.speak(productSellByDate,TextToSpeech.QUEUE_ADD,null);
                }
                else if (qr_code == true && barcodeData_text!="") {
                    tts.speak(barcodeData_text,TextToSpeech.QUEUE_FLUSH,null);
                }
                else if(productName==""){
                    tts.speak("바코드가 없거나 등록되지 않았습니다",TextToSpeech.QUEUE_FLUSH,null);
                }
                productName="";
                return true;
            }
        }
        else if(count==1) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                DATABASE_NAME = "database_barcode.db";
                boolean isOpen = openDatabase();
                if (isOpen) {
                    executeRawQuery();
                    executeRawQueryParam();
                }
                if(barcodeData_text!="")
                {
                    vibrator.vibrate(200);
                    MyClientTask myClientTask = new MyClientTask(editTextAddress, Integer.parseInt(editTextPort), "abc");
                    myClientTask.execute();
                }
                else
                {
                    final Toast toast = Toast.makeText(this, "바코드가 인식되지 않았습니다.", Toast.LENGTH_SHORT);
                    toast.show();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() { @Override public void run() { toast.cancel(); } }, 900);

                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected  void onDestroy(){
        super.onDestroy();
        if(tts!=null)
        {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    private void initialiseDetectorsAndSources(){
        barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build();
        cameraSource = new CameraSource.Builder(this,barcodeDetector).setRequestedPreviewSize(1920,1080).setAutoFocusEnabled(true).build();
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                try
                {
                    if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    {
                        cameraSource.start(surfaceView.getHolder());
                    }
                    else
                    {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA_PERMISSION);
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() { }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                // 바코드가 인식되었을 때 무슨 일을 할지
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if(barcodes.size() != 0)
                {
                    barcodeData_text = barcodes.valueAt(0).displayValue; //바코드 인식 결과물
                    if(count==0)
                    {
                        Vibrator vibrator=(Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                        vibrator.vibrate(200);
                    }
                    Log.d("Detection", barcodeData_text);
                }
                else
                {
                    barcodeData_text="";
                }
            }
        });
    }

    //문자열 길이와 한글 포함여부로 바코드인지 QR코드인지 구분하기
    public static boolean Check_code(String word) {
        boolean korean = false;
        for ( int i = 0 ; i < word.length(); i++ )
        {
            char ch = word.charAt( i );
            Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of( ch );
            if ( Character.UnicodeBlock.HANGUL_SYLLABLES.equals( unicodeBlock ) ||
                    Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO.equals( unicodeBlock )
                    || Character.UnicodeBlock.HANGUL_JAMO.equals( unicodeBlock ) )
            {
                korean = true;
            }
        }
        if ((word.length() == 13 || word.length() == 8) && korean == false) {
            return false;
        }
        else
            return true;
    }

    private boolean openDatabase() {
        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        return true;
    }

    private void executeRawQuery() {
        Cursor c1 = db.rawQuery("select count(*) as Total from " + TABLE_NAME, null);
        c1.moveToNext();
        c1.close();

    }

    private void executeRawQueryParam() {
        String SQL = "select name, price, date, code "
                + " from " + TABLE_NAME
                + " where code  = ?";
        Cursor c1 = db.rawQuery(SQL, new String[]{barcodeData_text}); //데이터 조회하기
        int recordCount = c1.getCount();
        if(c1 != null && recordCount !=0) //데이터베이스 값이 있을 때만 작동
        {
            c1.moveToNext();
            String name = c1.getString(0);
            String price = c1.getString(1);
            String date = c1.getString(2);
            String code = c1.getString(3);
            productName=name;
            productPrice=price;
            productSellByDate=date;
        }

        c1.close();
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {

            try {
                String DROP_SQL = "drop table if exists " + TABLE_NAME;
                db.execSQL(DROP_SQL);
            } catch(Exception ex) {
                Log.e(TAG, "Exception in DROP_SQL", ex);
            }

            String CREATE_SQL = "create table " + TABLE_NAME + "("
                    + " _id integer PRIMARY KEY autoincrement, "
                    + " name text, "
                    + " price integer, "
                    + " date text, "
                    + " code text)";

            try {
                db.execSQL(CREATE_SQL);
            } catch(Exception ex) {
                Log.e(TAG, "Exception in CREATE_SQL", ex);
            }

            try {
                db.execSQL( "insert into " + TABLE_NAME + "(name, price, date, code) values ('note', '2000원', '2023년8월8일','8804722103887');");
                db.execSQL( "insert into " + TABLE_NAME + "(name, price, date, code) values ('물티슈', '1500원', '2023년8월5일','8809372753765');");
                db.execSQL( "insert into " + TABLE_NAME + "(name, price, date, code) values ('cola', '1000원', '2023년8월5일','8809344661852');");
            } catch(Exception ex) {
                Log.e(TAG, "Exception in insert SQL", ex);
            }
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ".");
        }
    }
}