package cn.edu.sicnu.cs.lrj.musicplayerservice;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView lvSong;
    private SeekBar seekBar;
    private Button btnPlay,btnPause,btnStop,btnPrev,btnNext;

    private List<Song> songList = new ArrayList<>();
    private List<String> songNameList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private MusicPlayService musicService;
    private boolean isBind = false;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayService.MyBinder binder = (MusicPlayService.MyBinder) service;
            musicService = binder.getService();
            musicService.setSongList(songList);
            isBind = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBind = false;
        }
    };

    private final Handler handler = new Handler();
    private final Runnable updateSeekRunnable = new Runnable() {
        @Override
        public void run() {
            if(musicService != null && isBind){
                int total = musicService.getDuration();
                int current = musicService.getCurrentPosition();
                seekBar.setMax(total);
                seekBar.setProgress(current);
            }
            handler.postDelayed(this,200);
        }
    };

    private static final int REQ_PERM = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvSong = findViewById(R.id.lv_song);
        seekBar = findViewById(R.id.seekBar);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,songNameList);
        lvSong.setAdapter(adapter);

        checkPermission();

        Intent intent = new Intent(this,MusicPlayService.class);
        bindService(intent,conn,BIND_AUTO_CREATE);

        new Thread(()->{
            handler.post(updateSeekRunnable);
        }).start();

        lvSong.setOnItemClickListener((parent, view, position, id) -> {
            if(musicService != null) musicService.play(position);
        });

        btnPlay.setOnClickListener(v->{
            if(musicService!=null) musicService.resumePlay();
        });
        btnPause.setOnClickListener(v->{
            if(musicService!=null) musicService.pause();
        });
        btnStop.setOnClickListener(v->{
            if(musicService!=null) musicService.stopPlay();
        });
        btnPrev.setOnClickListener(v->{
            if(musicService!=null) musicService.preSong();
        });
        btnNext.setOnClickListener(v->{
            if(musicService!=null) musicService.nextSong();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if(musicService != null){
                    musicService.seekTo(progress);
                }
            }
        });

    }

    //API24 只申请 READ_EXTERNAL_STORAGE
    private void checkPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQ_PERM);
        }else{
            scanMusic();
        }
    }

    private void scanMusic(){
        songList.clear();
        songNameList.clear();
        ContentResolver resolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = resolver.query(uri,null,null,null,null);
        if(cursor != null){
            while(cursor.moveToNext()){
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                songList.add(new Song(title,path));
                songNameList.add(title);
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
        Toast.makeText(this,"扫描到"+songList.size()+"首音乐",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQ_PERM){
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                scanMusic();
            }else{
                Toast.makeText(this,"必须授予存储权限才能读取音乐！",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isBind){
            unbindService(conn);
            isBind = false;
            handler.removeCallbacks(updateSeekRunnable);
        }
    }
}
