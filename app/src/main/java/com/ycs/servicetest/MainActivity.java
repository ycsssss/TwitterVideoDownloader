package com.ycs.servicetest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.downloader.PRDownloader;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.tencent.mmkv.MMKV;
import com.ycs.servicetest.utils.ClipBoardUtil;
import com.ycs.servicetest.utils.ImageDialog;
import com.ycs.servicetest.utils.IosAlertDialog;
import com.ycs.servicetest.utils.WebUtil;

import java.util.Calendar;

import cn.bmob.v3.Bmob;
import io.flutter.embedding.android.FlutterActivity;

import static com.ycs.servicetest.Constant.REQUEST_CODE;
import static com.ycs.servicetest.utils.WebUtil.analyzeList;
import static com.ycs.servicetest.utils.WebUtil.isAnalyse;
import static com.ycs.servicetest.utils.WebUtil.isDownloading;
import static com.ycs.servicetest.utils.WebUtil.isHttpUrl;

//import io.flutter.embedding.android.FlutterActivity;
//import io.flutter.embedding.android.FlutterView;

public class MainActivity extends AppCompatActivity {
    final static String TAG="yyy";
    final static int GOTO_DOWNLOAD=1;

    private RelativeLayout floatWindow;
    private Button btn;
    private Context context;
    private EditText etInput;
    private Boolean isFloatWindowsshow=false;
    private String[] permissions = {Manifest.permission.SYSTEM_ALERT_WINDOW,Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
    private ImageView iv;
    private Vibrator vibrator;
    //跳到下载
    private Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what==GOTO_DOWNLOAD){
                    //WebUtil.isAnalyse=true;
                    MainService.updateNotification(MainActivity.this,"链接正在解析中...");
                    Intent i=new Intent(MainActivity.this,WebService.class);
                    i.putExtra("url",msg.obj.toString());
                    startService(i);
                }

                super.handleMessage(msg);
            }
        };
    //显示倒计时dialog
    void showDialog(){
        ImageDialog d=new ImageDialog(MainActivity.this).builder();
        d.show();
        handler.postDelayed(() -> runOnUiThread(() -> d.setAni(R.mipmap.two)),1000);
        handler.postDelayed(() -> runOnUiThread(() -> d.setAni(R.mipmap.one)),2000);
        handler.postDelayed(() -> startActivity(
                FlutterActivity.createDefaultIntent(context)
        ),3000);
        handler.postDelayed(() -> d.dismiss(),4000);

    }
    //检查时间
    void checkTime(){
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH)+1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        if(month==12&&day==10){
            handler.postDelayed(() -> showDialog(),2000);
        }
        if(month==2&&day==1){
            handler.postDelayed(() -> showDialog(),2000);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //CrashReport.initCrashReport(getApplicationContext(), "b0a053b5dd", true);
        context=getApplicationContext();
        Beta.upgradeDialogLayoutId= R.layout.layout_upgrade;
        Bugly.init(getApplicationContext(), "b0a053b5dd", false);
        Bmob.initialize(this, "2d24c857824e0609dd2e185bf5378acc");
        getSupportActionBar().hide();
        checkTime();
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.colorIosBlue));
        PRDownloader.initialize(getApplicationContext());
        WebUtil.init(getApplicationContext());
        String rootDir = MMKV.initialize(this);
        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        getPermission();
        showNotification();
        initView();
        showFloatWindows();
        checkPermission();
    }
    public void initView(){
        TextView tv=findViewById(R.id.btn);
        iv=findViewById(R.id.download);
        btn=findViewById(R.id.confirm);
        etInput=findViewById(R.id.input);
        floatWindow=findViewById(R.id.floatWindow);
        iv.setOnClickListener(v -> {
            Intent i=new Intent(MainActivity.this, VideoActivity.class);
            startActivity(i);
        });
        tv.setOnLongClickListener(v -> {
            startActivity(
                    FlutterActivity.createDefaultIntent(context)
            );
            return false;
        });
        btn.setOnClickListener(v -> {
            String text=etInput.getText().toString();
            if(text.isEmpty()){
                text=etInput.getHint().toString();
            }

            if(!WebUtil.isNetworkConnected(MainActivity.this)){
                Toast.makeText(MainActivity.this, "网络未打开", Toast.LENGTH_SHORT).show();
            }else if(isHttpUrl(text)&&text.contains("twitter")){
                if(isAnalyse||isDownloading){
                    if(!analyzeList.contains(text)){
                        WebUtil.analyzeList.add(text);
                        Log.e(TAG, "analyzeList的值："+analyzeList.toString() );
                        Toast.makeText(MainActivity.this, "已加入下载列表", Toast.LENGTH_SHORT).show();
                    }else{
                        Log.e(TAG, "analyzeList的值："+analyzeList.toString() );
                        Toast.makeText(MainActivity.this, "已在下载列表中", Toast.LENGTH_SHORT).show();
                    }

                }else{
                    Message ms=new Message();
                    ms.what=GOTO_DOWNLOAD;
                    ms.obj=text;
                    handler.sendMessage(ms);

                }
            } else {
                Toast.makeText(MainActivity.this, "您粘贴的不是网址噢", Toast.LENGTH_SHORT).show();
            }
        });
        btn.setOnLongClickListener(v -> {
            startActivity(new Intent(MainActivity.this,PubuActivity.class));
            return false;
        });
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(etInput.getHint()!="在这粘贴链接"){
                    btn.setText("下载");
                } else{
                    btn.setText("粘贴");

                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        iv.setOnLongClickListener(v -> {
            IosAlertDialog dialog=new IosAlertDialog(MainActivity.this).builder();
            SharedPreferences ssp=getSharedPreferences("url",Context.MODE_PRIVATE);
            String url=ssp.getString("url","");
            if(url.equals("")){
                url= Environment.getExternalStorageDirectory() +"/.savedPic/";
            }
            dialog.setEditText(url)
                    .setTitle("设置你的下载路径")
                    .setPositiveButton("确定", v1 -> {
                        if(!dialog.getEditText().isEmpty()){
                            SharedPreferences.Editor e=ssp.edit();
                            e.putString("url",Environment.getExternalStorageDirectory() +"/"+dialog.getEditText()+"/");
                            e.commit();
                        }

                    })
                    .setNegativeButton("取消", v12 -> {

                    })
                    .show();

            return true;
        });
    }
    public void showNotification(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(MainActivity.this, MainService.class));
        }else{
            startService(new Intent(MainActivity.this, MainService.class));
        }
    }
    public void checkPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if (Environment.isExternalStorageManager()) {
                //Toast.makeText(MainActivity.this, "已获得所有权限", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
            }
        }
    }
    public void showFloatWindows(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            if(!isFloatWindowsshow){
                Intent i=new Intent(MainActivity.this,DownLoadWindowService.class);
                try{
                    startService(i);
                    isFloatWindowsshow=true;
                }catch (Exception e){

                }

            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e(TAG, "onActivityResult: 一次" );
        if (requestCode == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                    //startService(new Intent(MainActivity.this, MainService.class));
                }
            }
        }else if(requestCode==REQUEST_CODE){
            if (Environment.isExternalStorageManager()) {
                Toast.makeText(this, "已获得所有权限", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "暂未取得读取文件权限，请前往获取", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissionss, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissionss, grantResults);
        if(requestCode==111){
            Log.e("yyy","获取权限返回");
            //Toast.makeText(this, "yyy", Toast.LENGTH_SHORT).show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(this, "未允许权限，同意后才可以使用本APP", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(this, permissions,111);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(new Runnable() {
            @Override
            public void run() {
                getWindow().getDecorView().post(() -> {
                    String paste=new ClipBoardUtil(MainActivity.this).paste();
                    etInput.setHint(paste);
                    if(etInput.getHint().toString().equals("在这粘贴链接")){
                        btn.setText("粘贴");
                    } else{
                        btn.setText("下载");
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(MainActivity.this,DownLoadWindowService.class));
        stopService(new Intent(MainActivity.this,MainService.class));
    }

    private void getPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (!Settings.canDrawOverlays(this)) {
//                Toast.makeText(this, "请允许悬浮窗权限", Toast.LENGTH_SHORT);
                new IosAlertDialog(this)
                        .builder()
                        .setTitle("前往获取悬浮窗权限")
                        .setPositiveButton("立即跳转", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:" + getPackageName())), 0);
                            }
                        })

                        .show();

            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, permissions,111);
            }
        }
    }
}