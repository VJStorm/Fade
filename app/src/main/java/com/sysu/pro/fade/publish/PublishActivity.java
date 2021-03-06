package com.sysu.pro.fade.publish;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.google.gson.Gson;
import com.sysu.pro.fade.Const;
import com.sysu.pro.fade.R;
import com.sysu.pro.fade.beans.Image;
import com.sysu.pro.fade.beans.Note;
import com.sysu.pro.fade.beans.SimpleResponse;
import com.sysu.pro.fade.beans.User;
import com.sysu.pro.fade.home.view.imageAdaptiveIndicativeItemLayout;
import com.sysu.pro.fade.publish.Event.ClickToCropEvent;
import com.sysu.pro.fade.publish.Event.CropToClickEvent;
import com.sysu.pro.fade.publish.Event.ImageSelectorToPublish;
import com.sysu.pro.fade.publish.Event.MapToPublish;
import com.sysu.pro.fade.publish.Event.SearchToPublish;
import com.sysu.pro.fade.publish.adapter.PostArticleImgAdapter;
import com.sysu.pro.fade.publish.crop.CropActivity;
import com.sysu.pro.fade.publish.imageselector.ImageSelectorActivity;
import com.sysu.pro.fade.publish.imageselector.constant.Constants;
import com.sysu.pro.fade.publish.imageselector.utils.BitmapUtils;
import com.sysu.pro.fade.publish.imageselector.utils.ImageSelectorUtils;
import com.sysu.pro.fade.publish.map.Activity.MapActivity;
import com.sysu.pro.fade.service.NoteService;
import com.sysu.pro.fade.utils.RetrofitUtil;
import com.sysu.pro.fade.utils.UserUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class PublishActivity extends AppCompatActivity {

    public static PublishActivity publishActivity;
    private static final int REQUEST_CODE = 0x00000011;

    private EditText edit_temp = null;
    private List<Bitmap> bitmaps = new ArrayList<Bitmap>();

    private String path = null;

    private RelativeLayout rl_editbar_bg;
    private View activityRootView;
    private int newCount = 9;
    private ArrayList<String> images = new ArrayList<String>();
    private ArrayList<String> newDataList = new ArrayList<String>();
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private final int maxCount = 9;
    private EditText et_emotion; //编辑器
//    private EmotionMainFragment emotionMainFragment;

    private boolean isHidden = true;

    private FrameLayout frameLayout;

    private ImageButton imageButton;

    private imageAdaptiveIndicativeItemLayout pager;

    private RelativeLayout pagerContainer;

    private boolean flag = true;

    private LinearLayout choose_view;
    public PostArticleImgAdapter postArticleImgAdapter;
    private TextView tv;//删除区域提示
    private Context mContext;
    ImageButton icon_add_pic;
    ImageButton icon_sub_pic;
    private int show;
    private static int CHOOSE = 0;
    private static int PAGER = 1;

    //2017.9.29
    public static float[] imageX;
    public static float[] imageY;
    private int crop_size = 1;

    //2017.9.13 hl
    public Integer have_compress_num = 0;
    //public String image_size_list;
    public Integer note_id; //发送文本成功后得到的
    //add by hl
    private User user;
    private TextView publishTextView;
    private ProgressDialog progressDialog;
    private static List<File> images_files;
    /*add By huanglu 2017.2.30，统一到java bean里处理*/
    private Note note;
    private List<Image>imageArray;
    //rxjava接入
    private Retrofit retrofit;
    private NoteService noteService;

    int cutSize; // 比例类型，1表示4:5， 0表示15:8
    int cut_size;   //1表示4:5,2表示15:8
    int curShowPosition = 0;

    //地图相关
    private static String city;
    private String address;
    public AMapLocationClientOption mLocationOption = null;
    private static double latitude;
    private static double longitude;
    private boolean mapStatus = false;
    private boolean isShowMap = false;
    //声明AMapLocationClientOption对象
    public AMapLocationClient mLocationClient = null;

    private TextView tv_count;
    private void dealWithImagesToSend(final List<String>images){

        //发送帖子的最后操作在这里
        //收集要发送的图片数据，包装一下,压缩好图片后，发送帖子
        if(images_files == null) images_files = new ArrayList<>();
        else images_files.clear();

        for(int i = 0; i < images.size(); i++){
            final Image image = new Image();
            //获得坐标
            int x = (int) (imageX[i] * 1000);
            String xStr = "" + x;
            int y = (int) (imageY[i] * 1000);
            String yStr = "" + y;
            image.setImage_coordinate(xStr + ":" + yStr);
            image.setImage_cut_size(cut_size + "");
            String image_path = images.get(i);
            //然后压缩图片
            Luban.with(this)
                    .load(new File(image_path))
                    .setCompressListener(new OnCompressListener() {
                @Override
                public void onStart() {
                    Log.i("压缩图片","开始");
                }
                @Override
                public void onSuccess(File file) {

                    //改变获取图片宽高的方式，只读取头信息，不整个解码，用以解决内存溢出 ---by 赖贤城
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    /**
                     * 最关键在此，把options.inJustDecodeBounds = true;
                     * 这里再decodeFile()，返回的bitmap为空，但此时调用options.outHeight时，已经包含了图片的高了
                     */
                    Bitmap bitmap_temp = BitmapFactory.decodeFile(file.getPath(), options); // 此时返回的bitmap为null
                    //获得宽高比
                    final Double size = Integer.valueOf(options.outWidth).doubleValue()/ Integer.valueOf(options.outHeight).doubleValue();


                    image.setImage_size(size.toString());
                    //添加到图片队列
                    imageArray.add(image);
                    images_files.add(new File(file.getPath()));
                    have_compress_num++;
                    Log.i("压缩图片","成功");
                    if(have_compress_num == images.size()){
                        //直到这里，所有图片才生成本地的压缩文件，才能发送图片
                        //TODO : 后面两个参数为 coordinate_list, cut_size_list
                        //cut_size为裁剪比例，1代表宽图4:5, 2代表长图15:8
                        //全部图片压缩完毕，发送帖子
                        note.setImages(imageArray);
                        MultipartBody.Builder builder= new MultipartBody.Builder().setType(MultipartBody.FORM)
                                .addFormDataPart("note", new Gson().toJson(note));
                        for(File temp : images_files){
                            builder.addFormDataPart("file", temp.getName(), RequestBody.create(MediaType.parse("image/*"), temp));
                        }
                        RequestBody body = builder.build();
                        noteService.addNote(body)
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Subscriber<SimpleResponse>() {
                                    @Override
                                    public void onCompleted() {
                                    }
                                    @Override
                                    public void onError(Throwable e) {
                                        Log.e("发送帖子","出错");
                                        Toast.makeText(PublishActivity.this,"发送失败",Toast.LENGTH_SHORT).show();
                                        progressDialog.dismiss();
                                        finish();
                                    }

                                    @Override
                                    public void onNext(SimpleResponse simpleResponse) {
                                        if(simpleResponse.getErr() == null){
                                            Toast.makeText(PublishActivity.this,"发送成功",Toast.LENGTH_SHORT).show();
                                            progressDialog.dismiss();
                                            //添加一些服务器返回来的参数
                                            Map<String,Object> extra = simpleResponse.getExtra();
                                            List<String>imageUrls = (List<String>) extra.get("imageUrls");
                                            if(imageUrls != null){
                                                for(int k = 0; k < imageUrls.size(); k++ ){
                                                    imageArray.get(k).setImage_url(imageUrls.get(k));
                                                }
                                            }
                                            note.setImages(imageArray);
                                            Integer note_id = (Integer) extra.get("note_id");
                                            String post_time = (String) extra.get("post_time");
                                            note.setNote_id(note_id);
                                            note.setPost_time(post_time);
                                            //通知主界面（ContentHome）更新
                                            EventBus.getDefault().post(note);
                                            Intent intent = new Intent();
                                            intent.putExtra("NotEdit", false);
                                            setResult(RESULT_OK, intent);
                                            finish();
                                        }else {
                                            Toast.makeText(PublishActivity.this,"发送失败",Toast.LENGTH_SHORT).show();
                                        }
                                        //最后要将所有缓存图片删除
/*                                        for(File chache_file : images_files){
                                            if(chache_file.exists()){
                                                chache_file.delete();
                                            }
                                        }*/
                                    }
                                });
                    }
                }
                @Override
                public void onError(Throwable e) {
                    Log.i("压缩图片","失败");
                    //Toast.makeText(PublishActivity.this,e.getMessage().toString(),Toast.LENGTH_SHORT).show();
                }
            }).launch();
           // File cache_file = ImageUtils.saveBitmapFileByCompress(cache_path_root,bitmap_temp,50);
        }
        if(images.size() == 0){
            //说明是纯文字帖，另外处理
            MultipartBody.Builder builder= new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("note", new Gson().toJson(note));
            RequestBody body = builder.build();
            noteService.addNote(body)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<SimpleResponse>() {
                        @Override
                        public void onCompleted() {
                        }
                        @Override
                        public void onError(Throwable e) {
                            Log.e("发送帖子","出错");
                            Toast.makeText(PublishActivity.this,"发送失败",Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                            finish();
                        }
                        @Override
                        public void onNext(SimpleResponse simpleResponse) {
                            if(simpleResponse.getErr() == null){
                                Toast.makeText(PublishActivity.this,"发送成功",Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                                //添加一些服务器返回来的参数
                                Map<String,Object> extra = simpleResponse.getExtra();
                                Integer note_id = (Integer) extra.get("note_id");
                                String post_time = (String) extra.get("post_time");
                                note.setNote_id(note_id);
                                note.setPost_time(post_time);
                                //通知主界面（ContentHome）更新
                                EventBus.getDefault().post(note);
                                Intent intent = new Intent();
                                intent.putExtra("NotEdit", false);
                                setResult(RESULT_OK, intent);
                                finish();
                            }else {
                                Toast.makeText(PublishActivity.this,"发送失败",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_publish);
        imageX = new float[10];
        imageY = new float[10];
        //注册
        EventBus.getDefault().register(this);
        user = new UserUtil(PublishActivity.this).getUer();//从本地存储初始化用户信息
        note = new Note();//本页面的note对象
        retrofit = RetrofitUtil.createRetrofit(Const.BASE_IP,user.getTokenModel());
        noteService = retrofit.create(NoteService.class);
        imageArray = new ArrayList<>();//图片对象数组
        progressDialog = new ProgressDialog(PublishActivity.this);
        progressDialog.setIndeterminate(false);
        progressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.dialog_style_xml_color));
        show = CHOOSE;
        InitView();
        requestLocation();
        if (flag) {
            et_emotion= (EditText) findViewById(R.id.my_et_emotion);
            //设置焦点，可被操作
//            et_emotion.setFocusable(true);
//            et_emotion.setFocusableInTouchMode(true);
//            et_emotion.requestFocus();
//            InputMethodManager im = ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE));
//            im.showSoftInput(et_emotion, 0);
            InitListener();
//            initEmotionMainFragment();
        }

    }

    private String getCoordinateString() {
        String str = "";
        for (int i = 0; i < images.size(); i++) {
            int x = (int) (imageX[i] * 1000);
            String xStr = "" + x;
            int y = (int) (imageY[i] * 1000);
            String yStr = "" + y;
            str += xStr + ":" + yStr;
            if (i < images.size() - 1)
                str += ",";
        }
        Log.d("upload_size", str);
        return str;
    }

    private void changeRatio(int pos) {
        //将后面的图片比例改到前面
        for (int i = pos; i < images.size(); i++) {
            imageX[i] = imageX[i + 1];
            imageY[i] = imageY[i + 1];
        }
    }

    private void clearRatio() {
        for (int i = 0; i < 9; i++) {
            imageX[i] = imageY[i] = 0;
        }
    }
    private void InitListener() {
        publishTextView = (TextView) findViewById(R.id.tv_confirm);
        publishTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit_temp.getText().toString().isEmpty() && images.size() == 0) {
                    Toast.makeText(PublishActivity.this,"输入不能为空",Toast.LENGTH_SHORT).show();
                    return;
                }
                else if (edit_temp.getText().toString().length() > 255) {
                    Toast.makeText(PublishActivity.this,"输入不能超过255个字",Toast.LENGTH_SHORT).show();
                    return;
                }
                //发送帖子
                progressDialog.show();
                //设置Note对象的一些属性
                note.setUser_id(user.getUser_id());
                note.setNickname(user.getNickname());
                note.setNote_content(edit_temp.getText().toString());
                note.setHead_image_url(user.getHead_image_url());
                if (mapStatus)
                    note.setNote_area(city + "·" + address);
                //处理图片，发送后的回调处理
                dealWithImagesToSend(images);
            }
        });

        findViewById(R.id.choose_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showListDialog();
            }
        });

        icon_add_pic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showListDialog();
            }
        });

        icon_sub_pic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PublishActivity.this);
                builder.setTitle("提示");
                builder.setMessage("确定要删除这张照片吗?");
                builder.setCancelable(false);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        if (images.size() == 2) {
                            clearRatio();
                            Log.d("Yellow", "in!");
                        }
                        if (images.size() == 1) {
                            images.clear();
                            newCount = maxCount;
                            choose_view.setVisibility(View.VISIBLE);
                            setHiddenPager(true);
                            show = CHOOSE;
                            clearRatio();
                        }
                        else {
                            int currentItem = pager.getPosition();
                            removeImg(currentItem);
                            changeRatio(currentItem);
                            for (int i = 0; i < imageX.length; i++) {
                                Log.d("Yellow", "i: " + i);
                                Log.d("Yellow", "imageX: " + imageX[i]);
                            }
                            for (int i = 0; i < imageX.length; i++) {
                                Log.d("Yellow", "i: " + i);
                                Log.d("Yellow", "imageY:: " + imageY[i]);
                            }
                            newCount = maxCount - images.size();
                            pager.setPaths(images,currentItem);
//                                updatePosition();
//                                pager.removeViewAt(currentItem);
                            pager.notifyChanged();
                            ShowViewPager();
                        }


                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.GRAY);
            }
        });


        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog();
            }
        });

        findViewById(R.id.not_choose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (findViewById(R.id.notation).getVisibility() == View.GONE) {
                    //可以点了
                    mapStatus = true;
                    findViewById(R.id.not_choose).setVisibility(View.GONE);
                    findViewById(R.id.is_choose).setVisibility(View.VISIBLE);
                }
            }
        });
        findViewById(R.id.is_choose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapStatus = false;
                findViewById(R.id.not_choose).setVisibility(View.VISIBLE);
                findViewById(R.id.is_choose).setVisibility(View.GONE);
            }
        });
        findViewById(R.id.map_information).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (findViewById(R.id.notation).getVisibility() == View.GONE) {
                    //可以点了
                    mapStatus = true;
                    findViewById(R.id.not_choose).setVisibility(View.GONE);
                    findViewById(R.id.is_choose).setVisibility(View.VISIBLE);
                    Intent intent = new Intent(PublishActivity.this, MapActivity.class);
                    intent.putExtra("longitude", longitude);
                    intent.putExtra("latitude", latitude);
                    intent.putExtra("city", city);
                    startActivityForResult(intent, Constants.MAP_RESULT_CODE);
                }
            }
        });
        activityRootView = findViewById(R.id.activity_publish);
//        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
//                if (heightDiff > dpToPx(PublishActivity.this, 200)) {
//                    findViewById(R.id.rl_editbar_bg).setVisibility(View.VISIBLE);
//                    findViewById(R.id.emotion_button).setVisibility(View.VISIBLE);
//                    findViewById(R.id.keyboard_button).setVisibility(View.GONE);
//                    choose_view.setVisibility(View.GONE);
//                    setHiddenPager(true);
//                }
//                else{
//                    findViewById(R.id.emotion_button).setVisibility(View.GONE);
//                    findViewById(R.id.keyboard_button).setVisibility(View.VISIBLE);
//                    if (frameLayout.getVisibility() == View.GONE)
//                     findViewById(R.id.rl_editbar_bg).setVisibility(View.GONE);
//                    if (show == CHOOSE)
//                        choose_view.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (findViewById(R.id.rl_editbar_bg).getVisibility() == View.GONE)
//                             choose_view.setVisibility(View.VISIBLE);
//                        }
//                    }, 50);
//                    if (show == PAGER)
//                        pager.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
////                                if (findViewById(R.id.rl_editbar_bg).getVisibility() == View.GONE)
//                                    setHiddenPager(false);
//                            }
//                        }, 200);
//                }
//
//            }
//        });
//
        edit_temp = (EditText) findViewById(R.id.my_et_emotion);
        tv_count = findViewById(R.id.tv_count);
        edit_temp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String content = edit_temp.getText().toString();
                if (content.length() > 255) {
                    tv_count.setVisibility(View.VISIBLE);
                    int text = 255 - content.length();
                    tv_count.setText(String.valueOf(text));
                }
                else {
                    tv_count.setVisibility(View.GONE);
                }
            }
        });
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(PublishActivity.this);
        builder.setTitle("退出此次编辑?");
        builder.setPositiveButton("确定",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent2 = new Intent();
                intent2.putExtra("NotEdit", true);
                setResult(RESULT_OK, intent2);
                finish();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.GRAY);
    }

    private void updatePosition() {
        for (int i = curShowPosition;i < images.size(); i++) {
            imageX[i] = imageX[i+1];
            imageY[i] = imageY[i+1];
        }
    }



//    public static float dpToPx(Context context, float valueInDp) {
//        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
//        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
//    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK )
            showAlertDialog();
        return false;
    }

    private void InitView() {
//        SharedPreferences sharedPreferences = getSharedPreferences("MY_PREFERENCE",
//                Context.MODE_PRIVATE);
//        isShowMap = sharedPreferences.getBoolean("isShowMap", false);
//        longitude = sharedPreferences.getInt("longitude", 0);
//        latitude = sharedPreferences.getInt("latitude", 0);
        if (isShowMap) {
            findViewById(R.id.map_information).setVisibility(View.VISIBLE);
            findViewById(R.id.notation).setVisibility(View.GONE);
        }

        icon_sub_pic = (ImageButton) findViewById(R.id.icon_sub_pic);
        icon_add_pic = (ImageButton) findViewById(R.id.icon_add_pic);
        pager = (imageAdaptiveIndicativeItemLayout) findViewById(R.id.image_layout);
        choose_view = (LinearLayout) findViewById(R.id.choose_view);
        tv = (TextView) findViewById(R.id.tv);
        pagerContainer = (RelativeLayout) findViewById(R.id.pager_container);
        setHiddenPager(true);
    }

    /**
     * 隐藏或是显示“Pager和增加和删除按钮”
     * @param isHidden true为隐藏 ，false为显示
     */
    private void setHiddenPager(boolean isHidden){
        if (isHidden)
            pagerContainer.setVisibility(View.GONE);
        else
            pagerContainer.setVisibility(View.VISIBLE);
    }

//    private void initEmotionMainFragment() {
//        //构建传递参数
//        Bundle bundle = new Bundle();
//        //绑定主内容编辑框
//        bundle.putBoolean(EmotionMainFragment.BIND_TO_EDITTEXT,false);
//        //隐藏控件
//        bundle.putBoolean(EmotionMainFragment.HIDE_BAR_EDITTEXT_AND_BTN,true);
//
//        bundle.putBoolean(EmotionMainFragment.EMOTION_HIDE,isHidden);
        //替换fragment
        //创建修改实例
//        frameLayout = (FrameLayout) findViewById(R.id.fl_memotionview_main);
//        rl_editbar_bg = (RelativeLayout) findViewById(R.id.rl_editbar_bg);
//        emotionMainFragment = EmotionMainFragment.newInstance(EmotionMainFragment.class,bundle);
//        emotionMainFragment.bindToContentView(et_emotion);
//        emotionMainFragment.bindToFramelayout(frameLayout);
//        emotionMainFragment.bindToRl_editbar_bg(rl_editbar_bg);
//        emotionMainFragment.bindToEmotion((ImageView)findViewById(R.id.emotion_button));
//        emotionMainFragment.bindToKeyboardEmotion((ImageView)findViewById(R.id.keyboard_button));
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        // Replace whatever is in thefragment_container view with this fragment,
//        // and add the transaction to the backstack
//        transaction.replace(R.id.fl_memotionview_main,emotionMainFragment);
        //返回栈
//        transaction.addToBackStack(null);
//        //提交修改
//        transaction.commit();
//    }

//    @Override
//    public void onBackPressed() {
//        /**
//         * 判断是否拦截返回键操作
//         */
//        if (!emotionMainFragment.isInterceptBackPress()) {
//            super.onBackPressed();
//        }
//    }


    @Override
    protected void onResume() {
        super.onResume();
//        ShowThumbnail();
//        ShowViewPager();
        }

    private void ShowViewPager() {
        if (images != null) {
            choose_view.setVisibility(View.GONE);
            setHiddenPager(false);
            show = PAGER;
            float maxRatio = 0;
            pager.setViewPagerMaxHeight(1800);
            double ratio = 1.0;
            if (images.size() > 1){
                ratio = 1.0;
            }
            else if (images.size() == 1){
                double imgRadio = determineSize(images.get(0));
                if (imgRadio > 2)
                    ratio = 2.0;
                else if (imgRadio < 0.75)
                    ratio = 0.75;
                else
                    ratio = imgRadio;
            }
            final double inRatio = ratio;
            Log.d("yellowsss", "ratio: " + ratio);
            pager.setClickMode(imageAdaptiveIndicativeItemLayout.ClickMode.EDIT);
            String coordinateString = getCoordinateString();
            String[] coordinates = coordinateString.split(",");
            pager.setImgCoordinates(Arrays.asList(coordinates));
            pager.setHeightByRatio((float)(1.0/ratio));
            Log.d("yellowsss", "coordinates: " + coordinateString);
            pager.setPaths(images,curShowPosition);

            //pager.invalidate();
            //pager.forceLayout();
            pager.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                boolean isLayouted = false;
                @Override
                public void onGlobalLayout() {
                    if (isLayouted)
                        return;
                    isLayouted = true;
                    ViewGroup.LayoutParams layoutParams = pagerContainer.getLayoutParams();
                    layoutParams.height = pager.getPagerRootHeight();
                    layoutParams.width = pager.getMeasuredWidth();
                    Log.d("yellowsss", "layoutParams: " + layoutParams.height + " " + layoutParams.width);
                    pagerContainer.setLayoutParams(layoutParams);
                    pagerContainer.invalidate();
                    icon_add_pic.invalidate();
                    icon_sub_pic.invalidate();
                    pager.setHeightByRatio((float)(1.0/inRatio));
                    pager.imgAdapter.notifyDataSetChanged();
                }
            });



            //ViewGroup.LayoutParams layoutParams = pagerContainer.getLayoutParams();
            //layoutParams.height = pager.getPagerRootHeight();
            //layoutParams.width = pager.getPagerRootWidth();
            //pagerContainer.setLayoutParams(layoutParams);
            //pagerContainer.invalidate();
            //icon_add_pic.invalidate();
            //icon_sub_pic.invalidate();



        }
    }

    private void ShowThumbnail() {
        if (images != null) {
            Bitmap bp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_addpic);
            bitmaps.clear();
            for (String image : images) {
                Bitmap newBp = BitmapUtils.decodeSampledBitmapFromFd(image, 200, 200);
                bitmaps.add(newBp);
            }
        }
    }

    private static float determineSize(String image) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        /**
         * 最关键在此，把options.inJustDecodeBounds = true;
         * 这里再decodeFile()，返回的bitmap为空，但此时调用options.outHeight时，已经包含了图片的高了
         */
        Bitmap bitmap = BitmapFactory.decodeFile(image, options); // 此时返回的bitmap为null
        /**
         *options.outHeight为原始图片的高
         */
        float currentRatio = (float)options.outWidth / (float)options.outHeight;
        return currentRatio;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.TAKE_PICTURE  && resultCode == RESULT_OK)
        {
//            Log.d("yellow", "path3: " + path);
//            Log.d("yellow", "images: " + images.get(0));
//            Log.d("yellow", "getExternalStorageDirectory: " + Environment.getExternalStorageDirectory());

            path = vFile.toString();
            if (path != null) {
//                path = Environment.getExternalStorageDirectory() + path;
                Log.d("yellow", "path2: " + path);
                images.add(path);
                newCount--;
                curShowPosition = images.size() - 1;
                ShowViewPager();
             }
        }
    }

    private void showListDialog() {
        final String[] items = { "拍摄","从相册选择"};
        AlertDialog.Builder listDialog =
                new AlertDialog.Builder(PublishActivity.this);
//        listDialog.setTitle("我是一个列表Dialog");
        listDialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
//                        Toast.makeText(PublishActivity.this,
//                                "你点击了拍照", Toast.LENGTH_SHORT).show();
                        takePhoto(PublishActivity.this);
                        break;
                    case 1:
//                        Toast.makeText(PublishActivity.this,
//                                "你点击了相册", Toast.LENGTH_SHORT).show();
//                        ImageSelectorUtils.openPhoto(PublishActivity.this, REQUEST_CODE, 9, images, newCount);
//                        emotionMainFragment.bindToContentView(findViewById(R.id.picker_04_horizontal));
//                        ImageSelectorActivity.openActivity(PublishActivity.this, REQUEST_CODE, 9, images, newCount);
                        Intent intent = new Intent(PublishActivity.this, ImageSelectorActivity.class);
                        intent.putExtra(Constants.MAX_SELECT_COUNT, 9);
                        intent.putStringArrayListExtra(ImageSelectorUtils.SELECT_LAST, images);
                        intent.putExtra(Constants.NEW_COUNT, newCount);
                        startActivity(intent);
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                EventBus.getDefault().post(new PublishToImageSelector(9, images, newCount));
//                                Log.d("Yellow", "PublishNewCount: " + newCount);
//                            }
//                        }).start();
                        break;
                }
                // which 下标从0开始
                // ...To-do

            }
        });
        listDialog.show();
    }

    private File vFile;
    //适配7.0的拍照方法
    private void takePhoto(Activity activity)
    {

        Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        openCameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Log.d("yellow", "getExternalCacheDir()" + getExternalCacheDir());
        Log.d("yellow", "OldEnvironment.getExternalStorageDirectory()" + Environment.getExternalStorageDirectory());
        vFile = new File(Environment.getExternalStorageDirectory()
                + "/Fade", String.valueOf(System.currentTimeMillis())
                + ".jpg");
        if (vFile.exists())
        {
            vFile.delete();
        }

//        Log.d("vapth", vFile.toString());
        Uri uri;
//        FileProvider.getUriForFile(mContext, BuildConfig.APP_PROVIDER, file)
        if (Build.VERSION.SDK_INT >= 24)
            uri = FileProvider.getUriForFile(getApplicationContext(),
                    activity.getApplicationContext().getPackageName() +
                            ".FileProvider", vFile);
        else uri = Uri.fromFile(vFile);
        openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        openCameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            // 7.0
//            //添加这一句表示对目标应用临时授权该Uri所代表的文件
//
//        }
        startActivityForResult(openCameraIntent, Constants.TAKE_PICTURE);
    }

    private void removeImg(int location)
    {
        if (location + 1 <= images.size())
        {
            images.remove(location);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ImageSelectorToPublish event) {
        images = event.getImages();
        newCount = event.getNewCount();
//        curShowPosition = images.size() - 1;
        curShowPosition = 0;
        ShowViewPager();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MapToPublish event) {
        address = event.getAddress();
        Log.d("Yellow", "MapToPublish");
        ((TextView) findViewById(R.id.address)).setText(address);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SearchToPublish event) {
        Log.d("Yellow", "SearchToPublish");
        address = event.getAddress();
        ((TextView) findViewById(R.id.address)).setText(address);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CropToClickEvent event) {
        String message = event.getMessage();
        curShowPosition = event.getPosition();
        for (int i = 0; i < imageX.length; i++) {
            Log.d("Yellow", "i: " + i);
            Log.d("Yellow", "imageX: " + imageX[i]);
        }
        for (int i = 0; i < imageX.length; i++) {
            Log.d("Yellow", "i: " + i);
            Log.d("Yellow", "imageY:: " + imageY[i]);
        }
        Log.d("yellowsss", message);
        ShowViewPager();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ClickToCropEvent event) {
        int curPosition = event.getCurPosition();
        Intent intent = new Intent(PublishActivity.this, CropActivity.class);
        intent.putStringArrayListExtra(ImageSelectorUtils.CROP_LAST, images);
        intent.putExtra(Constants.FLAG,cutSize);
        intent.putExtra(Constants.CURRENT_CROP_POSITION, curPosition);
        intent.putExtra(Constants.CROP_COUNT, newCount);
        //TODO:跳转裁剪
        startActivity(intent);
    }

    @Override
    protected void onDestroy(){
        if (mLocationClient != null) {
            mLocationClient.stopLocation();//停止定位后，本地定位服务并不会被销毁
            mLocationClient.onDestroy();//销毁定位客户端，同时销毁本地定位服务。
        }
        EventBus.getDefault().unregister(this);//反注册EventBus

        pager.release();//释放图片资源，试图解决OOM ---by 赖贤城
        super.onDestroy();
    }



    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public MyLocationListener myListener = new MyLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if (amapLocation != null) {
                if (amapLocation.getErrorCode() == 0) {
                    findViewById(R.id.map_information).setVisibility(View.VISIBLE);
                    findViewById(R.id.notation).setVisibility(View.GONE);
                    //可在其中解析amapLocation获取相应内容。
//                    amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                    latitude = amapLocation.getLatitude();//获取纬度
                    longitude = amapLocation.getLongitude();//获取经度
                    Log.d("haa", "latitude" + latitude);
                    Log.d("haa", "longitude" + longitude);
//                    amapLocation.getAccuracy();//获取精度信息
                    address = amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
//                    amapLocation.getCountry();//国家信息
//                    amapLocation.getProvince();//省信息
                    city = amapLocation.getCity();//城市信息
//                    SharedPreferences sharedPreferences = getSharedPreferences("MY_PREFERENCE",
//                            Context.MODE_PRIVATE);
//                    SharedPreferences.Editor editor = sharedPreferences.edit();
//                    editor.putFloat("latitude",(float) latitude);
//                    editor.putFloat("longitude", (float)longitude);
//                    editor.apply();
                    ((TextView) findViewById(R.id.city)).setText(city);
                    ((TextView) findViewById(R.id.address)).setText(address);
//                    amapLocation.getDistrict();//城区信息
//                    amapLocation.getStreet();//街道信息
//                    amapLocation.getStreetNum();//街道门牌号信息
//                    amapLocation.getCityCode();//城市编码
//                    amapLocation.getAdCode();//地区编码
//                    amapLocation.getAoiName();//获取当前定位点的AOI信息
                }else {
                    findViewById(R.id.map_information).setVisibility(View.GONE);
                    findViewById(R.id.notation).setVisibility(View.VISIBLE);
                    //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                    Log.e("AmapError","location Error, ErrCode:"
                            + amapLocation.getErrorCode() + ", errInfo:"
                            + amapLocation.getErrorInfo());
                }
            }
        }
    };

    private void requestLocation() {
        mLocationClient = new AMapLocationClient(getApplicationContext());     //声明LocationClient类
        mLocationClient.setLocationListener(myListener);    //注册监听函数
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        initLocation();
        //设置底部加载刷新
    }


    //    /**
//     * 监听函数，有新位置的时候，格式化成字符串，输出到屏幕中
//     */
    public class MyLocationListener implements AMapLocationListener {
        public void onReceiveLocation(AMapLocation location) {
            if (location == null) {
                return;
            }
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }

        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {

        }
    }

    private void initLocation() {
        Log.d("yellow", "initLocation");
        //获取一次定位结果：
        //该方法默认为false。
        mLocationOption.setOnceLocation(true);

        //获取最近3s内精度最高的一次定位结果：
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
        mLocationOption.setOnceLocationLatest(true);

        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
//        //关闭缓存机制
//        mLocationOption.setLocationCacheEnable(true);
//        mLocationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.SignIn);
        if(null != mLocationClient){
            mLocationClient.setLocationOption(mLocationOption);
            //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
//            mLocationClient.stopLocation();
            mLocationClient.startLocation();
        }

    }



}
