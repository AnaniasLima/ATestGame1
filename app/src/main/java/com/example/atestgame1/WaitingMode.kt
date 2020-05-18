package com.example.atestgame1

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.view.View
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

@SuppressLint("StaticFieldLeak")
object WaitingMode {
    var modoWaitingRunning=false

    var lastPlayedVideo: Int = -1

    lateinit private var videoView: VideoView
    private var myActivity: AppCompatActivity? = null

    var runDemoTimeoutHandler: Handler = Handler()
    var runDemoTimeoutRunnable: Runnable = Runnable {
        (myActivity as MainActivity).dealWithError(ErrorType.RUN_DEMO_TIMEOUT)
    }

    var runDemoHandler: Handler = Handler()
    var runDemoRunnable: Runnable = Runnable {
        ScreenLog.add(LogType.TO_HISTORY, "Enviando EventType.FW_DEMO")

        ArduinoDevice.requestToSend(EventType.FW_DEMO, Event.ON)
        // Esperamos receber resposta de um comando FW_DEMO em no máximo 10 segundos
        // TODO: Validar com Marcus se este tempo esta OK
        runDemoTimeoutHandler.postDelayed(runDemoTimeoutRunnable, 10000)
    }


    fun start(mainActivity: AppCompatActivity, view: VideoView) {
        myActivity = mainActivity
        videoView = view

        Timber.e("${BuildConfig.APPLICATION_ID}")

    }

    fun enterWaitingMode() {
        if (Config.videosDemo.isEmpty() ) {
            (myActivity as MainActivity).dealWithError(ErrorType.INVALID_WAITING_MODE_VIDEOS)
            return
        }

        // Tempo Minimo aceitável (em segundos) para executar uma demo
        // TODO: ajustar depois de testar para 120
        if ( Config.demoTime < 30 ) {
            (myActivity as MainActivity).dealWithError(ErrorType.INVALID_WAITING_MODE_VIDEOS)
            return
        }

        releasePlayer()
        videoView.visibility = View.VISIBLE


        modoWaitingRunning = true

        initPlayer()
        initRunDemoTimer()
    }

    fun leaveWaitingMode() {
        releasePlayer()

        cancelRunDemoTimeoutRunnable()
        cancelRunDemoRunnable()

        modoWaitingRunning = false
    }



    private fun playNextVideo() {
        if (++lastPlayedVideo == Config.videosDemo.size ) {
            lastPlayedVideo = 0
        }
        Timber.i("WWW PLAYING NEXT VIDEO $lastPlayedVideo  Video:${Config.videosDemo[lastPlayedVideo]} Max:${Config.videosDemo.size}")
        setVideoFilename(Config.videosDemo[lastPlayedVideo].filename)
        videoView.start()
    }


    private fun initPlayer() {
        videoView.setVisibility(View.VISIBLE)
        lastPlayedVideo = 0
        setVideoFilename(Config.videosDemo[lastPlayedVideo].filename)
        videoView.setOnCompletionListener {
            try {
                playNextVideo()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        videoView.start()
    }

    fun releasePlayer() {
        if ( videoView.isPlaying ) {
            videoView.stopPlayback()
        }
        videoView.visibility = View.GONE
        (myActivity as MainActivity).btnInvisivel.visibility = View.INVISIBLE
    }

    private fun setVideoFilename(filename:String) {
        if ( filename.contains('/')) {
            videoView.setVideoPath(filename)
        } else {
            videoView.setVideoURI(Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/raw/" + filename))
        }
    }


    private fun initRunDemoTimer() {
        cancelRunDemoRunnable()
        cancelRunDemoTimeoutRunnable()

        if ( modoWaitingRunning ) {
            runDemoHandler.postDelayed(runDemoRunnable,Config.demoTime * 1000L )
        }
    }


    fun onDemoEventReturn() {
        initRunDemoTimer()
    }

    fun cancelRunDemoRunnable() {
        try {
            runDemoHandler.removeCallbacks(runDemoRunnable)
        } catch (e: Exception) {}
    }

    fun cancelRunDemoTimeoutRunnable() {
        try {
            runDemoTimeoutHandler.removeCallbacks(runDemoTimeoutRunnable)
        } catch (e: Exception) {}
    }

}
