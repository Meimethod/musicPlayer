package cn.edu.sicnu.cs.lrj.musicplayerservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import java.util.List;

public class MusicPlayService extends Service {

    private MediaPlayer mediaPlayer;
    private List<Song> songList;
    private int currentIndex = 0;

    private final MyBinder binder = new MyBinder();
    public class MyBinder extends Binder{
        MusicPlayService getService(){
            return MusicPlayService.this;
        }
    }

    private static final String CHANNEL_ID = "music_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        createNotificationChannel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setSongList(List<Song> list){
        songList = list;
    }

    public void play(int index){
        if(songList == null || songList.size() ==0) return;
        currentIndex = index;
        try{
            mediaPlayer.reset();
            Song song = songList.get(currentIndex);
            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            showNotification(song.getTitle());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void pause(){
        if(mediaPlayer!=null && mediaPlayer.isPlaying()){
            mediaPlayer.pause();
        }
    }

    public void resumePlay(){
        if(mediaPlayer!=null && !mediaPlayer.isPlaying()){
            mediaPlayer.start();
        }
    }

    public void stopPlay(){
        if(mediaPlayer!=null){
            mediaPlayer.stop();
        }
    }

    public void preSong(){
        if(songList == null) return;
        currentIndex--;
        if(currentIndex < 0) currentIndex = songList.size()-1;
        play(currentIndex);
    }

    public void nextSong(){
        if(songList == null) return;
        currentIndex++;
        if(currentIndex >= songList.size()) currentIndex =0;
        play(currentIndex);
    }

    public String getCurrentSongName(){
        if(songList == null || songList.size() ==0) return "";
        return songList.get(currentIndex).getTitle();
    }

    public int getDuration(){
        if(mediaPlayer == null) return 0;
        return mediaPlayer.getDuration();
    }

    public int getCurrentPosition(){
        if(mediaPlayer == null) return 0;
        return mediaPlayer.getCurrentPosition();
    }

    public void seekTo(int pos){
        if(mediaPlayer!=null){
            mediaPlayer.seekTo(pos);
        }
    }

    //Android8.0以上必须创建通知渠道（API24是7.0，但兼容高版本手机）
    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,"音乐播放", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }

    private void showNotification(String songName){
        Notification.Builder builder;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            builder = new Notification.Builder(this,CHANNEL_ID);
        }else{
            builder = new Notification.Builder(this);
        }
        builder.setContentTitle("正在播放")
                .setContentText(songName)
                .setSmallIcon(android.R.drawable.ic_media_play);
        Notification notification = builder.build();
        startForeground(1001,notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mediaPlayer!=null){
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopForeground(true);
    }
}
