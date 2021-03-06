package com.example.atestgame1

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.lang.Exception
import java.util.*

private enum class DeviceState  {
    UNKNOW,
    OFF,
    ON;
}

private enum class DeviceCommand  {
    OFF,
    ON,
    RESET,
    QUESTION,
    SIMULA5REAIS,
    SIMULA10REAIS,
    SIMULA20REAIS,
    SIMULA50REAIS
}


@SuppressLint("StaticFieldLeak")
object BillAcceptor {
    private const val WAIT_WHEN_OFFLINE = 5000L
    private var DEFAULT_TIME_TO_QUESTION = 1000L
    private var SELECTED_TIME_TO_QUESTION = DEFAULT_TIME_TO_QUESTION
    private const val WAIT_TIME_TO_RESPONSE = 300L
    private const val BUSY_LIMIT_COUNTER = 10

    private var mainActivity: AppCompatActivity? = null
    private var appContext: Context? = null

    private var billAcceptorHandler = Handler()

    private var desiredState : DeviceState = DeviceState.OFF
    private var receivedState: DeviceState = DeviceState.UNKNOW

    private var inBusyStateCounter = 0
    private var inErrorStateCounter = 0         // TODO: Teoricamente nunca deve acontecer. Vamos criar uma forma de tratar caso fique > 0

    private var receivedValue = 0  // when receive values store here until

    private var turnOnTimeStamp: String = ""
    private var stateMachineRunning = false

    var questionDelayList = ArrayList<String>()

    lateinit var btnView : Button

    fun start(activity: AppCompatActivity, context: Context, btn: Button) {
        mainActivity = activity
        appContext = context
        btnView = btn

        mainActivity?.runOnUiThread {
            btnView.setBackgroundResource(R.drawable.bill_acceptor_yellow)
        }
        receivedState = DeviceState.UNKNOW
    }

    init {
        questionDelayList.add("Default ${DEFAULT_TIME_TO_QUESTION} ms")
        questionDelayList.add("Question 50 ms")
        questionDelayList.add("Question 100 ms")
        questionDelayList.add("Question 500 ms")
        questionDelayList.add("Question 1000 ms")
        questionDelayList.add("Question 5000 ms")
        questionDelayList.add("Question 10000 ms")
        questionDelayList.add("Question 60000 ms")
    }


    fun setDelayForQuestion(token: String) {
        val indStart = token.indexOfFirst {  it == ' '}
        val str2 = token.substring(indStart+1)
        val indEnd = str2.indexOfFirst {  it == ' '}
        val str3 = str2.substring(0, indEnd)

        try {
            val delay: Long = str3.toLong()
            SELECTED_TIME_TO_QUESTION = delay
            deviceChecking(0L) // Para iniciar novo ciclo
        } catch (e: Exception) {
            SELECTED_TIME_TO_QUESTION = DEFAULT_TIME_TO_QUESTION
        }
    }



    fun fakeBillAccept(value: Int) {
        when (value) {
            5 -> sendCommandToDevice(DeviceCommand.SIMULA5REAIS)
            10 -> sendCommandToDevice(DeviceCommand.SIMULA10REAIS)
            20 -> sendCommandToDevice(DeviceCommand.SIMULA20REAIS)
            50 -> sendCommandToDevice(DeviceCommand.SIMULA50REAIS)
        }
    }


    fun isEnabled() : Boolean {
        return receivedState == DeviceState.ON
    }


    fun startStateMachine()  {
        stateMachineRunning = true
        desiredState = DeviceState.ON
        deviceChecking(WAIT_TIME_TO_RESPONSE)
    }

    fun stopStateMachine()  {
        stateMachineRunning = false
    }

    fun SendTurnOn() {
        if ( stateMachineRunning ) {
            desiredState = DeviceState.ON
        } else {
            sendCommandToDevice(DeviceCommand.ON)
        }
    }

    fun SendTurnOff() {
        if ( stateMachineRunning ) {
            desiredState = DeviceState.OFF
        } else {
            sendCommandToDevice(DeviceCommand.OFF)
        }
    }

    fun SendQuestion() {
        sendCommandToDevice(DeviceCommand.QUESTION)
    }

    fun SendReset() {
        sendCommandToDevice(DeviceCommand.RESET)
    }


    fun deviceChecking(delay: Long) {
        var dropLog= false

        if ( stateMachineRunning ) {
            var delayToNext = delay

            if ( ! ConnectThread.isConnected ) {
                delayToNext = WAIT_WHEN_OFFLINE
            } else {
                if ( delayToNext == 0L ) {
                    delayToNext = SELECTED_TIME_TO_QUESTION
                    dropLog = true
                }
            }

            if ( ! dropLog) {
                val c = Calendar.getInstance()
                val strHora =  String.format("%02d:%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND))
                Timber.i("agendando deviceChecking ${strHora} + ${delayToNext}ms")
            }

            billAcceptorHandler.removeCallbacks(deviceCheckRunnable)
            billAcceptorHandler.postDelayed(deviceCheckRunnable, delayToNext)
        }
    }

    private var deviceCheckRunnable = Runnable {

        deviceChecking(0 ) // A principio agenda nova execução

        if ( receivedState == desiredState ) {
            sendCommandToDevice(DeviceCommand.QUESTION)
        } else {
            when (desiredState) {
                DeviceState.ON -> {
                    sendCommandToDevice(DeviceCommand.ON)
                }
                DeviceState.OFF -> {
                    sendCommandToDevice(DeviceCommand.OFF)
                }
                else -> {
                    desiredState = DeviceState.ON
                    sendCommandToDevice(DeviceCommand.ON)
                    println("ATENÇÃO: CCC Situação nao deveria ocorrer. Preciso reavaliara") // TODO: Verificar se vai ocorrer
                }
            }
        }
    }



    fun processReceivedResponse(response : EventResponse) {

        // Check if response is about BILL_ACCEPTOR
        if ( response.cmd != EventType.FW_BILL_ACCEPTOR.command ) {
            return
        }

        when ( response.ret ) {
            EventResponse.ERROR -> {
                if ( response.action == Event.SIMULA5REAIS) {
                    ScreenLog.add(LogType.TO_HISTORY, "REJEITOU NOTA 5")
                }
                else if ( response.action == Event.SIMULA10REAIS) {
                    ScreenLog.add(LogType.TO_HISTORY, "REJEITOU NOTA 10")
                }
                else if ( response.action == Event.SIMULA20REAIS) {
                    ScreenLog.add(LogType.TO_HISTORY, "REJEITOU NOTA 20")
                }
                else if ( response.action == Event.SIMULA50REAIS) {
                    ScreenLog.add(LogType.TO_HISTORY, "REJEITOU NOTA 50")
                } else {
                    inErrorStateCounter++
                    Timber.e("ERROR($inErrorStateCounter): ${response}")
                }
                return
            }
            EventResponse.BUSY -> {
                if ( ++inBusyStateCounter > BUSY_LIMIT_COUNTER ) {
                    // TODO: Avaliar o que fazer
                }
                return
            }
            EventResponse.OK -> {

                inBusyStateCounter = 0

                when (response.action ) {
                    Event.ON -> {
                        // Lets check if command ON turn on the Bill Acceptor
                        sendCommandToDevice(DeviceCommand.QUESTION)
                    }
                    Event.OFF -> {
                        // Lets check if command OFF turn off the Bill Acceptor
                        sendCommandToDevice(DeviceCommand.QUESTION)
                    }

                    Event.QUESTION -> {
                        when(response.status) {
                            Event.ON -> {
                                mainActivity?.runOnUiThread {
                                    (mainActivity as MainActivity).btn_bill_acceptor.setBackgroundResource(R.drawable.bill_acceptor_green)
                                }
                                receivedState = DeviceState.ON
                            }
                            Event.OFF -> {
                                mainActivity?.runOnUiThread {
                                    (mainActivity as MainActivity).btn_bill_acceptor.setBackgroundResource(R.drawable.bill_acceptor_red)
                                }
                                receivedState = DeviceState.OFF
                            }
                        }
                        // Quando receber uma resposta com um valor > 0
                        // vamos enviar um comando de reset. Quando recebermos a resposta
                        // do Reset com o valor ZERADO vamos contabilizar o valor armazenado em
                        // receivedValue
                        if ( response.value > 0 ) {
                            if ( receivedValue > 0) {
                                if ( receivedValue != response.value) {
                                    println("ATENÇÃO: AAA Situação nao deveria ocorrer. Preciso reavaliara") // TODO: Verificar se vai ocorrer
                                }
                            }
                            receivedValue = response.value
                            sendCommandToDevice(DeviceCommand.RESET)
                            desiredState = DeviceState.OFF
                        }

                        // Podemos reagendar o proximo question automatico
                        if ( stateMachineRunning ) {
                            deviceChecking(0L)
                        }
                    }
                    Event.RESET -> {
                        if ( response.value > 0 ) {
                            println("ATENÇÃO: BBB Situação nao deveria ocorrer. Preciso reavaliara") // TODO: Verificar se vai ocorrer
                        }

                        if ( receivedValue > 0 ) {
                            sendCreditToController(receivedValue)
                            receivedValue = 0
                        }

                        if ( Config.automaticBillAcceptor == 1) {
                            desiredState = DeviceState.ON
                            sendCommandToDevice(DeviceCommand.ON)
                        } else {
                            desiredState = DeviceState.OFF
                        }

                    }
                    Event.SIMULA5REAIS  -> {
                        sendCommandToDevice(DeviceCommand.QUESTION)
                    }
                    Event.SIMULA10REAIS  -> {
                        sendCommandToDevice(DeviceCommand.QUESTION)
                    }
                    Event.SIMULA20REAIS  -> {
                        sendCommandToDevice(DeviceCommand.QUESTION)
                    }
                    Event.SIMULA50REAIS  -> {
                        sendCommandToDevice(DeviceCommand.QUESTION)
                    }
                    else -> {
                        Timber.e("Invalid Response from BillAcceptor")
                    }
                }
            }
        }
    }


    private fun sendCommandToDevice(cmd : DeviceCommand) {

        when (cmd) {
            DeviceCommand.OFF -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.OFF)
            }
            DeviceCommand.ON -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.ON)
                turnOnTimeStamp = Date().time.toString()
            }
            DeviceCommand.QUESTION -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.QUESTION)
            }
            DeviceCommand.RESET -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.RESET)
            }
            DeviceCommand.SIMULA5REAIS -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.SIMULA5REAIS)
            }
            DeviceCommand.SIMULA10REAIS -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.SIMULA10REAIS)
            }
            DeviceCommand.SIMULA20REAIS -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.SIMULA20REAIS)
            }
            DeviceCommand.SIMULA50REAIS -> {
                ArduinoDevice.requestToSend(EventType.FW_BILL_ACCEPTOR, Event.SIMULA50REAIS)
            }
        }
    }


    private fun sendCreditToController(value : Int) {
        Timber.i("CREDITAR ${value}") // TODO: integrar com interface
        ScreenLog.add(LogType.TO_HISTORY, "CREDITAR ${value}")

        mainActivity?.runOnUiThread {
            (mainActivity as MainActivity).mostraEmResult(value)
        }
    }

}
