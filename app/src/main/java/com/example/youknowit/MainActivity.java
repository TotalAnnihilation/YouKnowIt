package com.example.youknowit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.navigation.NavigationView;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {


    private RecyclerView mRecycleViewPhotos;
    private PhotoAdapter mPhotoAdapter;
    private Handler mMainHandler;
    private SwipeRefreshLayout mRefreshLayout;
    private WebRequest dbRequest;
    private Handler dbRequestHandle;
    private int pageNo = 0;
    private int[] lastVisibleItems;
    private int lastVisibleItem;
    private boolean loading = false;
    private boolean loadmore = true;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private String mPhotoType;
    private boolean openPhotoDetail = true;
    private MyDBOpenHelper myDBOpenHelper;
    private SQLiteDatabase db;
    private Cursor cursor;
    private ProgressDialog mProgressDialog;
    private String DB_PATH = "/data/data/com.example.youknowit/databases/";
    private String DB_NAME = "my.db";
    private ImageButton menuButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer_layout);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mRefreshLayout = findViewById(R.id.refresh_layout);

        mPhotoType = getString(R.string.sishang_name);

        //NavigationView 内容点击事件
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                String title = (String) menuItem.getTitle();
                mPhotoType = title;
                dbRequestHandle.sendEmptyMessage(3);
                mRefreshLayout.setRefreshing(true);
                mDrawerLayout.closeDrawer(mNavigationView);
                return false;
            }
        });

        menuButton=findViewById(R.id.menu_button);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawerLayout.openDrawer(mNavigationView);
            }
        });

        //初始化图片列表界面
        mRecycleViewPhotos = findViewById(R.id.recycler_view_photos);
        mRecycleViewPhotos.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        //设置Adapter
        mPhotoAdapter = new PhotoAdapter();
        mRecycleViewPhotos.setAdapter(mPhotoAdapter);
        //增加上拉加载功能及预加载功能
        mRecycleViewPhotos.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                if (newState == RecyclerView.SCROLL_STATE_IDLE &&
                        lastVisibleItem + 1 == mPhotoAdapter.getItemCount() && !loading && loadmore) {
                    loading = true;
                    System.out.println("pageno:" + pageNo);
                    dbRequestHandle.sendEmptyMessage(0);
                }
            }

            //计算第一个及最后一个item位置
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                lastVisibleItems = staggeredGridLayoutManager.findLastCompletelyVisibleItemPositions(null);
                lastVisibleItem = findMax(lastVisibleItems);
            }
        });

        //主线程Handler
        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    //增量刷新界面
                    case 0: {
                        System.out.println("增量刷新：" + ((List<PhotoItem>) msg.obj).size());
                        //System.out.println("主线程的mPhotoAdapter"+mPhotoAdapter);
                        mPhotoAdapter.getPhotoItemList().addAll((List<PhotoItem>) msg.obj);
                        mPhotoAdapter.setImageScale();
                        System.out.println("PhotoItemList的Size" + mPhotoAdapter.getPhotoItemList().size());
                        mPhotoAdapter.notifyDataSetChanged();
                        loading = false;

                    }
                    break;
                    //刷新一个item
                    case 1: {
                        //System.out.println(msg.arg1);
                        mPhotoAdapter.notifyItemChanged(msg.arg1);
                    }
                    break;
                    //刷新界面
                    case 2: {
                        System.out.println("全量刷新：" + ((List<PhotoItem>) msg.obj).size());
                        //System.out.println("主线程的mPhotoAdapter"+mPhotoAdapter);
                        mPhotoAdapter.getPhotoItemList().clear();
                        mPhotoAdapter.getPhotoItemList().addAll((List<PhotoItem>) msg.obj);
                        mPhotoAdapter.setImageScale();
                        System.out.println("PhotoItemList的Size" + mPhotoAdapter.getPhotoItemList().size());
                        mPhotoAdapter.notifyDataSetChanged();
                        loading = false;
                        if (mRefreshLayout.isRefreshing())
                            mRefreshLayout.setRefreshing(false);

                    }
                    break;
                    //首次加载
                    case 3: {
                        mProgressDialog = new ProgressDialog(MainActivity.this);
                        mProgressDialog.setTitle("首次启动需要进行数据加载");
                        mProgressDialog.setMessage("请稍等...");
                        mProgressDialog.setCancelable(false);
                        mProgressDialog.show();
                        dbRequestHandle.sendEmptyMessage(1);

                    }
                    break;
                    //首次加载完毕
                    case 4: {
                        Toast.makeText(MainActivity.this, "数据加载完毕", Toast.LENGTH_SHORT).show();
                        mProgressDialog.cancel();
                        myDBOpenHelper = new MyDBOpenHelper(getApplicationContext(), "my.db", null, 1);
                        db = myDBOpenHelper.getWritableDatabase();
                        dbRequestHandle.sendEmptyMessage(0);

                    }
                    break;
                }


            }
        };


        //数据库专用线程
        dbRequest = new WebRequest();
        new Thread(dbRequest).start();
        dbRequestHandle = new Handler(dbRequest.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    //首次启动的时候将数据库文件移动到程序目录
                    case 1: {
                        try {
                            // 得到 assets 目录下我们实现准备好的 SQLite 数据库作为输入流
                            InputStream is = getBaseContext().getAssets().open(DB_NAME);
                            // 输出流
                            OutputStream os = new FileOutputStream(DB_PATH + DB_NAME);
                            // 文件写入
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = is.read(buffer)) > 0) {
                                os.write(buffer, 0, length);
                            }

                            // 关闭文件流
                            os.flush();
                            os.close();
                            is.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mMainHandler.sendEmptyMessage(4);

                    }
                    break;
                    //增加一页数据并进行页面刷新
                    case 0: {
                        loading = true;
//                    System.out.println("加载页数：" + pageNo);
                        int offset = pageNo * 20;
                        List<PhotoItem> albums = getAlbums(mPhotoType, offset, 20);
                        albums = getAlbumsCover(albums);
                        pageNo++;
                        loadmore = !(albums.size() == 0);
                        Message message = mMainHandler.obtainMessage();
                        message.what = 0;
                        message.obj = albums;
                        message.sendToTarget();
                    }
                    break;
                    //重新加载第一页
                    case 3: {
                        loading = true;
                        pageNo = 0;
//                    System.out.println("加载页数：" + pageNo);
                        int offset = pageNo * 20;
                        List<PhotoItem> albums = getAlbums(mPhotoType, offset, 20);
                        albums = getAlbumsCover(albums);
                        pageNo++;
                        loadmore = !(albums.size() == 0);
                        Message message = mMainHandler.obtainMessage();
                        message.what = 2;
                        message.obj = albums;
                        message.sendToTarget();
                    }
                    break;

                }

            }
        };

        //下拉刷新
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                dbRequestHandle.sendEmptyMessage(3);
            }
        });


        //初始化数据
        if ((new File(DB_PATH + DB_NAME)).exists() == false) {
            // 如 SQLite 数据库文件不存在，再检查一下 database 目录是否存在
            File f = new File(DB_PATH);
            // 如 database 目录不存在，新建该目录
            if (!f.exists()) {
                f.mkdir();
            }
            mMainHandler.sendEmptyMessage(3);
        } else {
            myDBOpenHelper = new MyDBOpenHelper(getApplicationContext(), "my.db", null, 1);
            db = myDBOpenHelper.getWritableDatabase();
            dbRequestHandle.sendEmptyMessage(0);
        }
//        cursor = db.rawQuery("SELECT COUNT (*) FROM meizi", null);
//        cursor.moveToFirst();
//        long result = cursor.getLong(0);
//        cursor.close();
//        System.out.println(result);
//        if (result == 0) {
//
//        } else {
//
//        }

    }


    //查找最大值
    private int findMax(int[] intArray) {
        int max = intArray[0];
        for (int value : intArray) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    //查找最小值
    private int findMin(int[] intArray) {
        int min = intArray[0];
        for (int value : intArray) {
            if (value < min) {
                min = value;
            }
        }
        return min;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbRequest.getLooper().quitSafely();
    }


    //显示图片的ViewHolder
    private class PhotoHolder extends RecyclerView.ViewHolder {
        //        TextView mTextViewPhotoName;
        ImageView mImageViewPhoto;
        PhotoItem mPhotoItem;

        public PhotoHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_photo, parent, false));
//            mTextViewPhotoName=itemView.findViewById(R.id.text_view_photo_name);
            mImageViewPhoto = itemView.findViewById(R.id.image_view_photo);
        }

        public void bind(PhotoItem photoItem) {
            mPhotoItem = photoItem;
            mImageViewPhoto.setImageResource(R.drawable.loading);
            if (mPhotoItem.getScale() != 0) {
                StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) mImageViewPhoto.getLayoutParams();//获取你要填充图片的布局的layoutParam
                //因为是2列,所以宽度是屏幕的一半,高度是根据bitmap的高/宽*屏幕宽的一半
                layoutParams.width = getScreenWidth(MainActivity.this) / 2;//这个是布局的宽度
                layoutParams.height = (int) (layoutParams.width / mPhotoItem.getScale());
                mImageViewPhoto.setLayoutParams(layoutParams);
            }
            Glide.with(MainActivity.this).load(mPhotoItem.getUrl()).placeholder(R.drawable.loading).error(R.drawable.error).into(mImageViewPhoto);
            mImageViewPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (openPhotoDetail) {
                        openPhotoDetail = false;
                        ArrayList<String> imglist = getAlbumImgUrl(mPhotoItem);
                        Intent intent = new Intent(MainActivity.this, PictureDetail.class);
                        intent.putStringArrayListExtra("imglist", imglist);
                        startActivity(intent);
                        openPhotoDetail = true;
                    }
                }
            });

        }
    }

    //加载圈圈的ViewHolder
    private class FooterViewHolder extends RecyclerView.ViewHolder {
        private ProgressBar mFootViewProgress;

        public FooterViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.foot_view, parent, false));
//            mTextViewPhotoName=itemView.findViewById(R.id.text_view_photo_name);
            mFootViewProgress = itemView.findViewById(R.id.foot_view_progress);
            mFootViewProgress.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    dbRequestHandle.sendEmptyMessage(0);

                }
            });
        }

        public ProgressBar getmFootViewProgress() {
            return mFootViewProgress;
        }
    }


    //适配器
    private class PhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_ITEM = 0;
        private static final int TYPE_FOOTER = 1;


        private List<PhotoItem> mPhotoItemList = new ArrayList<>();

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
            if (viewType == TYPE_ITEM) {
                return new PhotoHolder(layoutInflater, parent);
            } else if (viewType == TYPE_FOOTER) {
                return new FooterViewHolder(layoutInflater, parent);
            }
            return null;
//            return new PhotoHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof PhotoHolder) {
//                System.out.println("PhotoHolder:"+position);
                PhotoItem photoItem = mPhotoItemList.get(position);
                ((PhotoHolder) holder).bind(photoItem);
                holder.itemView.setTag(position);
            } else if (holder instanceof FooterViewHolder) {
//               System.out.println("FooterViewHolder:"+position);
                FooterViewHolder footerViewHolder = (FooterViewHolder) holder;

                if (loadmore)
                    footerViewHolder.getmFootViewProgress().setVisibility(View.VISIBLE);
                else
                    footerViewHolder.getmFootViewProgress().setVisibility(View.GONE);

            }

        }

        @Override
        public int getItemCount() {
            return mPhotoItemList.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position + 1 == getItemCount()) {
                //最后一个item设置为footerView
//                System.out.println("TYPE_FOOTER");
                return TYPE_FOOTER;
            } else {
//                System.out.println("TYPE_ITEM");
                return TYPE_ITEM;
            }
        }


        public List<PhotoItem> getPhotoItemList() {
            return mPhotoItemList;
        }


        private void setImageScale() {
            for (final PhotoItem PhotoItem : mPhotoItemList) {
                if (PhotoItem.getScale() == 0) {
                    Glide.with(MainActivity.this).load(PhotoItem.getUrl()).placeholder(R.drawable.loading).error(R.drawable.error).into(new SimpleTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            float scale = resource.getIntrinsicWidth() / (float) resource.getIntrinsicHeight();
                            PhotoItem.setScale(scale);
                            notifyDataSetChanged();
                        }
                    });
                } else {
                    notifyDataSetChanged();
                }
            }
        }

    }

    //旋转屏幕之后重新适配
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mPhotoAdapter.notifyDataSetChanged();
    }


    //获取屏幕宽度的方法
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }


    //数据库-分页输出相册
    public List<PhotoItem> getAlbums(String photoType, int offset, int maxResult) {
        List<PhotoItem> photoItems = new ArrayList<PhotoItem>();
        SQLiteDatabase db = myDBOpenHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT distinct(album) FROM meizi where phototype=? LIMIT ? offset ?",
                new String[]{photoType, String.valueOf(maxResult), String.valueOf(offset)});
        while (cursor.moveToNext()) {
            String album = cursor.getString(cursor.getColumnIndex("album"));
            photoItems.add(new PhotoItem(photoType, album));
        }
        cursor.close();
        return photoItems;
    }


    //数据库-获取相册封面
    public List<PhotoItem> getAlbumsCover(List<PhotoItem> albums) {
        List<PhotoItem> photoItems = new ArrayList<PhotoItem>();
        SQLiteDatabase db = myDBOpenHelper.getReadableDatabase();
        for (PhotoItem photoItem : albums) {
            Cursor cursor = db.rawQuery("SELECT * FROM meizi where phototype=? and album= ? limit 1",
                    new String[]{photoItem.getPhotoType(), photoItem.getAlbum()});
            while (cursor.moveToNext()) {
                String photoid = cursor.getString(cursor.getColumnIndex("photoid"));
                String url = cursor.getString(cursor.getColumnIndex("url"));
                photoItem.setPhotoid(photoid);
                photoItem.setUrl(url);
                photoItems.add(photoItem);
            }
        }

        if (cursor != null)
            cursor.close();
        return photoItems;
    }

    //数据库-获取相册中所有图片的连接
    public ArrayList<String> getAlbumImgUrl(PhotoItem photoItem) {
        ArrayList<String> photoItems = new ArrayList<String>();
        SQLiteDatabase db = myDBOpenHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT url FROM meizi where phototype=? and album= ? ",
                new String[]{photoItem.getPhotoType(), photoItem.getAlbum()});
        while (cursor.moveToNext()) {
            String url = cursor.getString(cursor.getColumnIndex("url"));
            ;
            photoItems.add(url);
        }

        cursor.close();
        return photoItems;
    }
}

