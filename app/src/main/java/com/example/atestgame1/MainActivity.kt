package com.example.atestgame1

import android.content.Context
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.time.LocalDateTime
import java.util.*
import kotlin.time.*


enum class ErrorType(val type: Int, val message: String) {
    INVALID_WAITING_MODE_VIDEOS( 1, "ON_WAITING_VIDEO: Sem videos definidos para modo waiting"),
    INVALID_TIME_TO_DEMO( 1, "DEMO_TIME: Tempo configurado menor que tempo minimo (120 segundos)"),
    RUN_DEMO_TIMEOUT( 2, "Sem Resposta da finalização da DEMO")
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
            ErrorType.INVALID_WAITING_MODE_VIDEOS -> erroFatal(errorType.message)
            ErrorType.INVALID_TIME_TO_DEMO -> erroFatal(errorType.message)
            ErrorType.RUN_DEMO_TIMEOUT -> erroFatal(errorType.message)
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
        WaitingMode.start(this, video_view, btnInvisivel)
        BillAcceptor.start(this, applicationContext, btn_bill_acceptor)

        insertSpinnerBillAcceptor()



        //
        // ----- ArduinoDevice
        //
        ArduinoDevice.usbManager = applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
        ArduinoDevice.myContext = applicationContext
        ArduinoDevice.mainActivity = this
        ArduinoDevice.usbSetFilters()
        ArduinoDevice.usbSerialImediateChecking(200)


        setButtonListeners()

    }


    // ------------- textResult--------------
    private var stringTextResult: String = "R$ 0,00"
    private var valorAcumulado: Int = 0
    private var mostraEmResultHandler = Handler()
    private var updateEmResult = Runnable {
        textResult.setText(stringTextResult)
    }

    fun mostraEmResult(valor: Int) {
        valorAcumulado += valor
        stringTextResult = "R$ ${valorAcumulado},00 "
        Timber.i("Total Recebido: ${stringTextResult}")
        mostraEmResultHandler.removeCallbacks(updateEmResult)
        mostraEmResultHandler.postDelayed(updateEmResult, 10)
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



    fun insertSpinnerBillAcceptor() {
        spinnerDelayQuestion.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1 , BillAcceptor.questionDelayList)
        BillAcceptor.setDelayForQuestion(spinnerDelayQuestion.selectedItem.toString())

        spinnerDelayQuestion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Timber.i("Nada foi selecionado")
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                BillAcceptor.setDelayForQuestion(parent!!.getItemAtPosition(pos).toString())
            }
        }
    }

    fun setButtonListeners() {


        // Primeira Linha -----------------------------------

            btnBillAcceptorQuestion.setOnClickListener {
                BillAcceptor.SendQuestion()
            }

            btnBillAcceptorReset.setOnClickListener {
                BillAcceptor.SendReset()
            }

            btnStatusRequest.setOnClickListener {
                ArduinoDevice.requestToSend(EventType.FW_STATUS_RQ, Event.QUESTION)
            }

            btnDemoOn.setOnClickListener {
                ArduinoDevice.requestToSend(EventType.FW_DEMO, Event.ON)
                Thread {
                    // Vamos desligar temporariamente o log TX (depois retornamos ao status original)
                    val old = ArduinoDevice.getLogLevel(FunctionType.FX_TX)
                    Thread.sleep(1000)
                    ArduinoDevice.logTX(false)
                    for ( contaLinha in  1..10) {
                        ArduinoDevice.requestToSend(EventType.FW_STATUS_RQ, Event.QUESTION)
                        Thread.sleep(1000)
                    }
                    ArduinoDevice.logTX(old)
                }.start()
            }

            btnBillAcceptorStateMachine.setOnClickListener{
                if ( BillAcceptor.isStatMachineRunning() ) {
                    btnBillAcceptorStateMachine.text = getString(R.string.startStateMachine)
                    BillAcceptor.StopStateMachine()
                } else {
                    btnBillAcceptorStateMachine.text = getString(R.string.stopStateMachine)
                    BillAcceptor.StartStateMachine()
                }
            }

        // Segunda Linha -----------------------------------
            btnLogTag.setOnClickListener{
                ScreenLog.tag(LogType.TO_LOG)
            }

            btnLogClear.setOnClickListener{
                ScreenLog.clear(LogType.TO_LOG)
            }


        // Rodape -----------------------------------
        btn_bill_acceptor.setOnClickListener {
            if ( BillAcceptor.isEnabled() ) {
                BillAcceptor.SendTurnOff()
                ScreenLog.add(LogType.TO_LOG, "BillAcceptor.SendTurnOff")
            } else {
                BillAcceptor.SendTurnOn()
                ScreenLog.add(LogType.TO_LOG, "BillAcceptor.SendTurnOn")
            }
        }

        btnStartVideo.setOnClickListener  {
            log_recycler_view.setVisibility(View.INVISIBLE)
            log_recycler_view.setVisibility(View.GONE)
            WaitingMode.enterWaitingMode()
        }

        btnStopVideo.setOnClickListener  {
            WaitingMode.leaveWaitingMode()
            log_recycler_view.setVisibility(View.VISIBLE)
        }


        btnInvisivel.setOnClickListener  {
            WaitingMode.leaveWaitingMode()
            log_recycler_view.setVisibility(View.VISIBLE)
        }



        btn5reais.setOnClickListener {
            BillAcceptor.fakeBillAccept(5)
            ScreenLog.add(LogType.TO_HISTORY, "Nota 5")
        }
        btn10reais.setOnClickListener{
            BillAcceptor.fakeBillAccept(10)
            ScreenLog.add(LogType.TO_HISTORY, "Nota 10")
        }
        btn20reais.setOnClickListener{
            BillAcceptor.fakeBillAccept(20)
            ScreenLog.add(LogType.TO_HISTORY, "Nota 20")
        }
        btn50reais.setOnClickListener{
            BillAcceptor.fakeBillAccept(50)
            ScreenLog.add(LogType.TO_HISTORY, "Nota 50")
        }


    }

}
