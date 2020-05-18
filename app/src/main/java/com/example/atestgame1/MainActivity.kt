package com.example.atestgame1

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber


enum class ErrorType(val type: Int, val message: String) {
    INVALID_WAITING_MODE_VIDEOS( 1, "ON_WAITING_VIDEO: Sem videos definidos para modo waiting"),
    INVALID_TIME_TO_DEMO( 1, "DEMO_TIME: Tempo configurado menor que tempo minimo (120 segundos)"),
    RUN_DEMO_TIMEOUT( 2, "DEMO_TIME: Tempo configurado menor que tempo minimo (120 segundos)")
    ;
}


class MainActivity : AppCompatActivity() {


//    fun getURI(videoname:String): Uri {
//        if (URLUtil.isValidUrl(videoname)) {
//            //  an external URL
//            return Uri.parse(videoname)
//        } else { //  a raw resource
//            return Uri.parse("android.resource://" + packageName + "/raw/" + videoname);
//        }
//    }


    fun dealWithError(errorType: ErrorType) {

        ScreenLog.add(LogType.TO_HISTORY, "dealWithError errorType = ${errorType}")

        when (errorType) {
            ErrorType.INVALID_WAITING_MODE_VIDEOS ->   erroFatal(errorType.message)
            ErrorType.INVALID_TIME_TO_DEMO ->   erroFatal(errorType.message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        setContentView(R.layout.activity_main)

        // TODO: Ajustar para edir permissao para usuário ao invez de habilitar permissao na mão
        if ( ! Config.loadConfig(this, "/storage/emulated/0/JMGames/configJMGames.json") ) {
            erroFatal(Config.msgErro)
        }

        ScreenLog.start(this, applicationContext, log_recycler_view, history_recycler_view)
        WaitingMode.start(this, video_view)

        btn5reais.setOnClickListener {
            ScreenLog.add(LogType.TO_LOG, "btn5reais")
        }

        btn20reais.setOnClickListener {
            Thread {
                for ( contaLinha in  1..20) {
                    ScreenLog.add(LogType.TO_HISTORY, "Linha History ${contaLinha}")
                    Thread.sleep(200)
                }
            }.start()
        }

        btn50reais.setOnClickListener {
            Thread {
                for ( contaLinha in  1..100) {
                    ScreenLog.add(LogType.TO_LOG, "Linha ${contaLinha}")
                    Thread.sleep(20)
                }
            }.start()
        }

        btnStartVideo.setOnClickListener  {
            log_recycler_view.setVisibility(View.INVISIBLE)
            log_recycler_view.setVisibility(View.GONE)
            btnInvisivel.setVisibility(View.VISIBLE)
            WaitingMode.enterWaitingMode()
        }

        btnStopVideo.setOnClickListener  {
            WaitingMode.releasePlayer()
            log_recycler_view.setVisibility(View.VISIBLE)
            btnInvisivel.setVisibility(View.INVISIBLE)
        }


        btnInvisivel.setOnClickListener  {
            WaitingMode.releasePlayer()
            WaitingMode.leaveWaitingMode()
            log_recycler_view.setVisibility(View.VISIBLE)
            btnInvisivel.setVisibility(View.INVISIBLE)
        }

    }


    fun erroFatal(str: String?)
    {
        if ( str != null && str.isNotEmpty() ) {
            buttonErro.setVisibility(View.VISIBLE)
            buttonErro.isClickable=true
            buttonErro.setText(str)
            buttonErro.setOnClickListener {
                Timber.e(str)
                finish()
                System.exit(0)
            }
        }
    }


}
