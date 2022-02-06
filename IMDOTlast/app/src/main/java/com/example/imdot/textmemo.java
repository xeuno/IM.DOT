package com.example.imdot;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class textmemo extends AppCompatActivity {

    public static final int REQUEST_CODE_INSERT = 1000;
    private MemoAdapter mAdapter;
    private String editTextAddress;
    private EditText mContentsEditText;
    private long mMemoId = -1;
    private Button btn_reset;
    private Button btn_motor;
    private String host_port="8888";
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textmemo);

        mContentsEditText = findViewById(R.id.contents_edit);
        btn_reset = findViewById(R.id.btn_reset);
        btn_motor = findViewById(R.id.btn_motor);

        Intent intent2 = getIntent();
        editTextAddress=intent2.getStringExtra("ip_address");

        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContentsEditText.setText("");
            }
        });

        //모터 출력버튼을 눌렀을 때 실행되는 코드
        btn_motor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyClientTask myClientTask = new MyClientTask(editTextAddress, Integer.parseInt(host_port), mContentsEditText.getText().toString());
                myClientTask.execute();
                save_values();
            }
        });

        listView = findViewById(R.id.memo_list);


        Cursor cursor = getMemoCursor();
        mAdapter = new MemoAdapter(this, cursor);
        listView.setAdapter(mAdapter);

        //리스트를 눌렀을 때
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Cursor cursor = (Cursor) mAdapter.getItem(position);
                String contents = cursor.getString(cursor.getColumnIndexOrThrow(MemoContract.MemoEntry.COLUME_NAME_CONTENTS));
                mContentsEditText.setText(contents);

                final long memeoid = id;
                SQLiteDatabase db = MemoDbHelper.getInstance(textmemo.this).getWritableDatabase();
                int deletedCount = db.delete(MemoContract.MemoEntry.TABLE_NAME,
                        MemoContract.MemoEntry._ID + " = "+ memeoid, null);
            }
        });

        //list를 길게 눌렀을 때
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long id) {
                final long deletedId = id;

                AlertDialog.Builder builder = new AlertDialog.Builder(textmemo.this);
                builder.setTitle("메모 삭제");
                builder.setMessage("메모를 삭제하시겠습니까?");
                builder.setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SQLiteDatabase db = MemoDbHelper.getInstance(textmemo.this).getWritableDatabase();
                        int deletedCount = db.delete(MemoContract.MemoEntry.TABLE_NAME,
                                MemoContract.MemoEntry._ID + " = "+ deletedId, null);
                        if(deletedCount == 0){
                            Toast.makeText(textmemo.this, "삭제에 문제가 발생하였습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            mAdapter.swapCursor(getMemoCursor());
                            final Toast toast = Toast.makeText(textmemo.this, "메모가 삭제되었습니다.", Toast.LENGTH_SHORT);
                            toast.show();
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() { @Override public void run() { toast.cancel(); } }, 900);
                        }
                    }
                });

                builder.setNegativeButton("취소", null);
                builder.show();
                return true;
            }
        });
    }

    //socket communication
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

    private void save_values(){
        String contents = mContentsEditText.getText().toString();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MemoContract.MemoEntry.COLUME_NAME_TITLE, contents);
        contentValues.put(MemoContract.MemoEntry.COLUME_NAME_CONTENTS, contents);

        SQLiteDatabase db = MemoDbHelper.getInstance(this).getWritableDatabase();

        if(mMemoId == -1){
            long newRowId = db.insert(MemoContract.MemoEntry.TABLE_NAME, null, contentValues);

            if(newRowId == -1){
                Toast.makeText(this, "저장에 문제가 발생하였습니다.", Toast.LENGTH_SHORT).show();
            }else {
                final Toast toast = Toast.makeText(this, "메모가 저장되었습니다.", Toast.LENGTH_SHORT);
                toast.show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() { @Override public void run() { toast.cancel(); } }, 900);
                setResult(RESULT_OK);
            }
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }else {
            int count = db.update(MemoContract.MemoEntry.TABLE_NAME, contentValues,
                    MemoContract.MemoEntry._ID+" = "+mMemoId, null);
            if(count ==0){
                Toast.makeText(this,"수정에 문제가 발생하였습니다.", Toast.LENGTH_SHORT).show();
            }else{
                final Toast toast = Toast.makeText(this, "메모가 수정되었습니다.", Toast.LENGTH_SHORT);
                toast.show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() { @Override public void run() { toast.cancel(); } }, 900);
                setResult(RESULT_OK);
            }
        }
        //super.onBackPressed();
    }

    //뒤로가기 눌렀을 경우 저장되게 하는 코드
    @Override
    public void onBackPressed() {
        //save_values();
        super.onBackPressed();
    }

    private Cursor getMemoCursor() {
        MemoDbHelper dbHelper = MemoDbHelper.getInstance(this);

        return dbHelper.getReadableDatabase()
                .query(MemoContract.MemoEntry.TABLE_NAME,
                        null,null,null,null,null,MemoContract.MemoEntry._ID + " DESC");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_INSERT && resultCode == RESULT_OK){
            mAdapter.swapCursor(getMemoCursor());
        }
    }

    private static class MemoAdapter extends CursorAdapter{
        public MemoAdapter(Context context, Cursor c) {
            super(context, c, false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, viewGroup, false);
        }
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView titleText = view.findViewById(android.R.id.text1);
            titleText.setText(cursor.getString(cursor.getColumnIndexOrThrow(MemoContract.MemoEntry.COLUME_NAME_TITLE)));
        }
    }
}