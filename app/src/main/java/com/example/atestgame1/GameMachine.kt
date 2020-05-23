package com.example.atestgame1

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber
import java.util.*

enum class MachineState  {
    UNKNOW,
    IDLE,
    RUNNING_DEMO,
    PLAYING;
}


enum class MachineFSMState {
//    STARTING_SPECIAL_FUNCTION,
    FSM_IDLE,
    HOMMING_START_Y,
    HOMMING_WAIT_END_Y,
    HOMMING_PARALLEL_START,
    HOMMING_PARALLEL_WAIT_END,
//    CHECK_MODE,
    RUNNING_DEMO,
    RUNNING_DEMO_WAIT_Y,
    RUNNING_DEMO_WAIT_XZ,
    RUNNING_WAIT_START_MOV_X,
    RUNNING_WAIT_STOP_MOV_X,
    RUNNING_WAIT_START_MOV_Z,
    RUNNING_WAIT_STOP_MOV_Z,
    RUNNING_WAIT_EJECT_PRIZE,
    RUNNING_FIM;
}

enum class MachineCommand  {
    STATUS_RQ,
    FW_PLAY,
    FW_DEMO;
}

@SuppressLint("StaticFieldLeak")
object GameMachine {
    private const val WAIT_WHEN_OFFLINE = 5000L
    private var DEFAULT_TIME_TO_QUESTION = 1000L
    private var SELECTED_TIME_TO_QUESTION = DEFAULT_TIME_TO_QUESTION
    private const val WAIT_TIME_TO_RESPONSE = 300L
    private const val MAX_RUN_DEMO_TIMEOUT = 30000L
    private const val BUSY_LIMIT_COUNTER = 10

    private var mainActivity: AppCompatActivity? = null
    private var appContext: Context? = null

    private var gameMachineHandler = Handler()

    private var desiredState: MachineState = MachineState.IDLE
    private var receivedState: MachineState = MachineState.UNKNOW
    private var countCommandsToDesiredState = 0

    private var inBusyStateCounter = 0
    private var inErrorStateCounter =
        0         // TODO: Teoricamente nunca deve acontecer. Vamos criar uma forma de tratar caso fique > 0


    private var stateMachineRunning = false

    private var runningDemo = false

    var questionDelayList = ArrayList<String>()

    fun start(activity: AppCompatActivity, context: Context) {
        mainActivity = activity
        appContext = context

        receivedState = MachineState.UNKNOW

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
        val indStart = token.indexOfFirst { it == ' ' }
        val str2 = token.substring(indStart + 1)
        val indEnd = str2.indexOfFirst { it == ' ' }
        val str3 = str2.substring(0, indEnd)

        try {
            val delay: Long = str3.toLong()
            SELECTED_TIME_TO_QUESTION = delay
            machineChecking(0L) // Para iniciar novo ciclo
        } catch (e: Exception) {
            SELECTED_TIME_TO_QUESTION = DEFAULT_TIME_TO_QUESTION
        }
    }


    fun startStateMachine() {
        stateMachineRunning = true
        desiredState = MachineState.IDLE
        machineChecking(WAIT_TIME_TO_RESPONSE)
    }

    fun stopStateMachine() {
        stateMachineRunning = false
    }


    fun startRunDemo(): Long {
        countCommandsToDesiredState = 0
        desiredState = MachineState.RUNNING_DEMO

        machineChecking(WAIT_TIME_TO_RESPONSE)

        return (MAX_RUN_DEMO_TIMEOUT)
    }

    fun isRunningDemo(): Boolean {
        return (receivedState == MachineState.RUNNING_DEMO)
    }

    fun stopRunDemo() {
        if (isRunningDemo()) {
            ArduinoDevice.requestToSend(EventType.FW_DEMO, Event.OFF)
        }
    }


    fun machineChecking(delay: Long) {
        var dropLog = false

        if (stateMachineRunning) {
            var delayToNext = delay

            if (!ConnectThread.isConnected) {
                delayToNext = WAIT_WHEN_OFFLINE
            } else {
                if (delayToNext == 0L) {
                    delayToNext = SELECTED_TIME_TO_QUESTION
                    dropLog = true
                }
            }

            if (!dropLog) {
                val c = Calendar.getInstance()
                val strHora = String.format(
                    "%02d:%02d:%02d",
                    c.get(Calendar.HOUR_OF_DAY),
                    c.get(Calendar.MINUTE),
                    c.get(Calendar.SECOND)
                )
                Timber.i("agendando deviceChecking ${strHora} + ${delayToNext}ms")
            }

            gameMachineHandler.removeCallbacks(machineCheckRunnable)
            gameMachineHandler.postDelayed(machineCheckRunnable, delayToNext)
        }
    }


    fun machineDemoTimeout(start: Boolean) {

        if ( start ) {
            val c = Calendar.getInstance()
            val strHora = String.format(
                "%02d:%02d:%02d",
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                c.get(Calendar.SECOND)
            )
            Timber.i("agendando demoTimeout: ${strHora} + ${MAX_RUN_DEMO_TIMEOUT}ms")

            gameMachineHandler.removeCallbacks(demoTimeoutResetMachine)
            gameMachineHandler.postDelayed(demoTimeoutResetMachine, MAX_RUN_DEMO_TIMEOUT)
        } else {
            Timber.i("Removendo demoTimeout")
            gameMachineHandler.removeCallbacks(demoTimeoutResetMachine)
        }

    }


    private var demoTimeoutResetMachine = Runnable {
        erroFatal("Nao finalizou demo em ${MAX_RUN_DEMO_TIMEOUT}ms")
    }


    fun changeGameMachineCurrentState( newState : MachineState) {
        if ( receivedState != newState) {
            // Saindo do state
            when (receivedState ) {
                MachineState.UNKNOW -> {

                }
                MachineState.IDLE -> {

                }
                MachineState.RUNNING_DEMO -> {
                    machineDemoTimeout(false)
                    showRunningDemo(false)
                }
                MachineState.PLAYING -> {

                }
            }

            receivedState = newState
            // Entrando no state
            when (receivedState ) {
                MachineState.UNKNOW -> {

                }
                MachineState.IDLE -> {

                }
                MachineState.RUNNING_DEMO -> {
                    machineDemoTimeout(true)
                    showRunningDemo(true)
                }
                MachineState.PLAYING -> {

                }
            }
        }
    }


    private var machineCheckRunnable = Runnable {

        machineChecking(0) // A principio agenda nova execução

        if (receivedState == desiredState) {
            ArduinoDevice.requestToSend(EventType.FW_STATUS_RQ, Event.QUESTION)
        } else {
            when (desiredState) {
                MachineState.RUNNING_DEMO -> {
                    if (countCommandsToDesiredState++ < 2) {
                        ArduinoDevice.requestToSend(EventType.FW_DEMO, Event.ON)
                    } else {
                        // Não conseguimos entrar em modo demo, vamos desistir
                        countCommandsToDesiredState = 0
                        desiredState = MachineState.IDLE
                    }
                }
                MachineState.IDLE -> {
                    ArduinoDevice.requestToSend(EventType.FW_STATUS_RQ, Event.QUESTION)
                }
                else -> {
                    println("ATENÇÃO: CCC Situação nao deveria ocorrer. Preciso reavaliara") // TODO: Verificar se vai ocorrer
                }
            }
        }
    }

    fun erroFatal(str: String?) {
        mainActivity?.runOnUiThread {
            (mainActivity as MainActivity).erroFatal(str)
        }
    }

    fun processReceivedResponse(response: EventResponse) {

        when (response.eventType) {

            EventType.FW_STATUS_RQ -> {
                when (receivedState) {
                    MachineState.UNKNOW->{
                        if (response.fsm_state == "FSM_IDLE") {
                            changeGameMachineCurrentState(MachineState.IDLE)
                        } else {
                            ScreenLog.add(LogType.TO_HISTORY, "Tratar response.fsm_state = ${response.fsm_state} em MachineState.UNKNOW")
                            changeGameMachineCurrentState(MachineState.IDLE) // TODO: Verificar qual estado deveriamos setar
                        }
                    }

                    MachineState.IDLE -> {
                        if (response.fsm_state == "FSM_IDLE") {
                            changeGameMachineCurrentState(MachineState.IDLE)
                        } else {
                            ScreenLog.add(LogType.TO_HISTORY, "Tratar response.fsm_state = ${response.fsm_state} ")
                        }
                    }

                    MachineState.RUNNING_DEMO -> {
                        if (response.fsm_state == "FSM_IDLE") {
                            changeGameMachineCurrentState(MachineState.IDLE)
                        }
                    }

                    MachineState.PLAYING-> {
                        ScreenLog.add(LogType.TO_HISTORY, "Tratar PLAYING 001")
                        if (response.fsm_state == "FSM_IDLE") {
                            changeGameMachineCurrentState(MachineState.IDLE)
                        }
                    }
                }
            }


            EventType.FW_DEMO -> {

                if (response.ret == EventResponse.ERROR) {
                    // Só responde erro se comando for diferente de ON/OFF. Qua nunca será o caso
                    erroFatal("Resposta ERROR inesperada para comando FW_DEMO ")
                }

                else if (response.action == Event.ON) {
                    if (response.ret == EventResponse.OK) {
                        if (response.fsm_state == "RUNNING_DEMO") {
                            changeGameMachineCurrentState(MachineState.RUNNING_DEMO)
                            countCommandsToDesiredState = 0
                            desiredState = MachineState.IDLE
                            ArduinoDevice.requestToSend(EventType.FW_STATUS_RQ, Event.QUESTION)
                        } else {
                            // BUSY - Comando não será processado
                            // TODO: futuramente avaliar se precisamos fazer alguma coisa para não "pular" a Demo
                            ScreenLog.add(LogType.TO_HISTORY, "Era esperado RUNNING_DEMO")
                        }

                    } else {
                        // BUSY - Comando não será processado
                        // TODO: futuramente avaliar se precisamos fazer alguma coisa para não "pular" a Demo
                        ScreenLog.add(LogType.TO_HISTORY, "FW_DEMO(On) ignorado - BUSY")
                    }

                } else if (response.action == Event.OFF) {

                    if (response.ret == EventResponse.OK) {
                        ArduinoDevice.requestToSend(EventType.FW_STATUS_RQ, Event.QUESTION)
                        // TODO: aqui poderiamos reajustar o timeout para menor (Temo de voltar pra home da posição mais longe possivel)
                        // Vamos aguardar finalização - FSM_IDLE
                    } else {
                        // BUSY - Comando não será processado
                        // Mandando desligar a demo e respondendo BUSY deve estar terminando o HOMMING
                    }

                } else {
                    ScreenLog.add(LogType.TO_HISTORY, "Resposta Invalida ${response}")
                }
            }

            EventType.FW_PLAY -> {
                ScreenLog.add(LogType.TO_HISTORY, "tratar FW_PLAY em  processReceivedResponse(GameMachine)")
            }

            else -> {
                ScreenLog.add(LogType.TO_HISTORY, "EventType invalido chegando em processReceivedResponse(GameMachine)")

            }

        }
    }

    private fun showRunningDemo(flag : Boolean) {
        mainActivity?.runOnUiThread {
            (mainActivity as MainActivity).showRunningDemo(flag)
        }
    }

}

