package com.example.youknowit;


import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;


import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import androidx.viewpager.widget.ViewPager;

public class PictureDetail extends AppCompatActivity {

    private ViewPager viewPager;
    private MyViewPagerAdapter myViewPagerAdapter;

    private List<String> imglist ;
    private LinearLayout points;
    private int prePosition;
    private ImageButton goBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        setStatusBarFullTransparent();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_detail);
        imglist=getIntent().getStringArrayListExtra("imglist");
        initView();
        initData();
    }

    private void initData() {
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float v, int i1) {
                position = position % imglist.size();
                //把前一个白变为黑
                points.getChildAt(prePosition).setBackgroundResource(R.drawable.point_back);
                //把当前白点变为黑点
                points.getChildAt(position).setBackgroundResource(R.drawable.point_white);
                //记录下当前位置(当前位置变白后，赋值给前一个点)
                prePosition = position;
            }

            @Override
            public void onPageSelected(int i) {

            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }

    private void initView() {
        viewPager = findViewById(R.id.viewpager);
        goBack= findViewById(R.id.goback);
        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        myViewPagerAdapter = new MyViewPagerAdapter(this,imglist);
        viewPager.setAdapter(myViewPagerAdapter);
        points = findViewById(R.id.points);
        for(int i = 0;i<imglist.size();i++) {
            //白点
            //根据viewPager的数量，添加白点指示器
            ImageView view = new ImageView(this);
            view.setBackgroundResource(R.drawable.point_back);
            //给点设置宽高
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(20, 20);
            //给控件设置边距
            params.leftMargin = 10;
            //给view设置参数
            view.setLayoutParams(params);
            //将图片添加到线性布局中
            points.addView(view);
        }

        points.getChildAt(0).setBackgroundResource(R.drawable.point_white);
        viewPager.setCurrentItem(0);
    }


}
