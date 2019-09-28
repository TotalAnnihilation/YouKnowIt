package com.example.youknowit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import androidx.appcompat.app.AppCompatActivity;

public class MyDBOpenHelper extends SQLiteOpenHelper {
    private Context context;
    public MyDBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                          int version){
        super(context, "my.db", null, 1);
        this.context=context;

    }
    @Override
    //数据库第一次创建时被调用
    public void onCreate(SQLiteDatabase db) {
    }
    //软件版本号发生改变时调用
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }




}