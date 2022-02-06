package com.example.imdot;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class MemoDbHelper extends SQLiteOpenHelper {
    private static MemoDbHelper sInstance;
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "Memo.db";
    private static final String SQL_CREATE_ENTRIES =
            String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s TEXT)",
                    MemoContract.MemoEntry.TABLE_NAME,
                    MemoContract.MemoEntry._ID,
                    MemoContract.MemoEntry.COLUME_NAME_TITLE,
                    MemoContract.MemoEntry.COLUME_NAME_CONTENTS);
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS "+ MemoContract.MemoEntry.TABLE_NAME;

    public static MemoDbHelper getInstance(Context context){
        if(sInstance == null){
            sInstance = new MemoDbHelper(context);
        }
        return sInstance;
    }
    public MemoDbHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) { //최초의 DB를 생성하는 부분
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) { //이전 테이블과 현재테이블의 변경점을 대응해주어야함.
        sqLiteDatabase.execSQL(SQL_DELETE_ENTRIES);
        onCreate(sqLiteDatabase);
    }
}
