package com.example.atestgame1

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

//    var screenLogHandler = Handler()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        setContentView(R.layout.activity_main)

        ScreenLog.start(this, applicationContext, log_recycler_view, history_recycler_view)

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


    }


}
