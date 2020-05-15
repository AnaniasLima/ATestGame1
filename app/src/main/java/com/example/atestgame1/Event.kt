package com.example.atestgame1

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Event(
    var eventType: EventType = EventType.FW_STATUS_RQ,
    var action: String = QUESTION,
    var timestamp: Long = Date().time) {    // TODO: Acho que podemos sumir com este campo

    companion object {
        val ON = "on"
        val OFF = "off"
        val QUESTION = "question"
        val RESET = "reset"
        var pktNumber: Int = 0
        val SIMULA5REAIS  = "simula5"
        val SIMULA10REAIS = "simula10"
        val SIMULA20REAIS = "simula20"
        val SIMULA50REAIS = "simula50"

        fun getCommandData(event: Event): String {
            val commandData = JSONObject()
            ++pktNumber

            commandData.put("cmd", event.eventType.command)

            if (event.eventType == EventType.FW_PINPAD) { // Mateus porque FW_PINPAD ?
                if (event.action == ON) {
                    commandData.put("state", 1)
                } else {
                    commandData.put("state", 0)
                }
            } else {
                commandData.put("action", event.action)
            }

            commandData.put("packetNumber", pktNumber)
            commandData.put("hour", SimpleDateFormat( "HH:mm:SS", Locale.getDefault()).format(Date()))
//            commandData.put("timestamp", event.timestamp.toString())

            return commandData.toString()
        }
    }
}

data class EventResponse(
    var cmd: String = "",
    var action: String = "",
    var error_n: Int = 0,
    var value: Int = 0,
    var mifare: Long = 0,
    var mifare_pass: Int = 0,
    var premio_n: Int = 0,
    var button_1: String = "",
    var button_2: String = "",
    var ret: String = "",
    var status:String = "",
    var fsm_state: String = "",
    var success: String = "",
    var R: String = "",
    var G: String = "",
    var B: String = "",
    var tR: String = "",
    var tB: String = "",
    var tG: String = "",
    var packetNumber: String = "",
    var numPktResp: String = "",

    var cordinates: String = "",
    var eventType: EventType = EventType.FW_STATUS_RQ) {
    companion object {
        val OK = "ok"
        val ERROR = "error"
        val BUSY = "busy"
        var invalidJsonPacketsReceived = 0
    }
}

enum class EventType(val type: Int, val command: String) {
    FW_STATUS_RQ(0, "fw_status_rq"),
    FW_PINPAD(1, "fw_pinpad"),
    FW_TABLET_RESET(2, "fw_tablet_reset"),
    FW_PLAY(3, "fw_play"),
    FW_DEMO(4, "fw_demo"),
    FW_BILL_ACCEPTOR(5, "fw_noteiro"),
    FW_LED(6, "fw_led"),
    FW_NACK(7, "fw_nack"),
    FW_CALIBRATE(5, "fw_calibrate");

    companion object {
        fun getByCommand(command: String): EventType? {
            for (value in values()) {
                if (value.command == command) {
                    return value
                }
            }
            return null
        }
    }
}
