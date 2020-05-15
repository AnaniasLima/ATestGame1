package com.example.atestgame1

import android.R
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.view.View
import android.webkit.URLUtil
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

@SuppressLint("StaticFieldLeak")
object WaitingMode {

    var lastPlayedVideo: Int = -1

    private var videoView: VideoView? = null
    private var myActivity: AppCompatActivity? = null
    private var myContext: Context? = null

    private fun setVideoFilename(filename:String) {
        if ( filename.contains('/')) {
            videoView!!.setVideoPath(filename)
        } else {
            var uri = (myActivity as MainActivity).getURI(filename)
            videoView!!.setVideoURI(uri)
        }
    }

    fun start(mainActivity: AppCompatActivity, context: Context, view: VideoView) {
        myActivity = mainActivity
        myContext = context
        videoView = view
    }

    fun playVideos() {
        if ( videoView != null) {
            videoView!!.visibility = View.VISIBLE
            initPlayer()
        }
    }




    private fun playNextVideo() {
        if (Config.videosDemo.isEmpty()) {
            return
        }

        lastPlayedVideo++

        if (lastPlayedVideo == Config.videosDemo.size ) {
            lastPlayedVideo = 0
        }

        Timber.i("WWW PLAYING NEXT VIDEO $lastPlayedVideo  Video:${Config.videosDemo[lastPlayedVideo]} Max:${Config.videosDemo.size}")

        setVideoFilename(Config.videosDemo[lastPlayedVideo].filename)
        videoView!!.start()
    }


    private fun initPlayer() {
        var poucaMemoria = ActivityManager.MemoryInfo().lowMemory

        if (poucaMemoria ) {
            Timber.w("== == == == == == == == POUCA MEMORIA == == == == == == == ==")
        }

        videoView!!.setVisibility(View.VISIBLE)

        lastPlayedVideo = 0

        setVideoFilename(Config.videosDemo[lastPlayedVideo].filename)

        videoView!!.setOnCompletionListener {
            try {
                playNextVideo()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        videoView!!.start()
    }

    fun releasePlayer(){
        if ( videoView != null) {
            videoView!!.stopPlayback()
            videoView!!.visibility = View.GONE
        }
    }



}