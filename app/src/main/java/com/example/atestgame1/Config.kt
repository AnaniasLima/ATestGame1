package com.example.atestgame1

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.FileInputStream
import java.lang.Exception
import java.nio.channels.FileChannel
import java.nio.charset.Charset

enum class ConfigType(val type: Int, val token: String) {
    SERVER          ( 0, "SERVER"),
    CREDIT_VALUE    ( 1, "CREDIT_VALUE"),
    ALARM_TIME      ( 2, "ALARM_TIME"),
    DEMO_TIME       ( 3, "DEMO_TIME"),
    TRY_TIME        ( 4, "TRY_TIME"),
    DURING_TRY_AUDIO( 5, "DURING_TRY_AUDIO"),
    ON_LOSE_AUDIO   ( 6, "ON_LOSE_AUDIO"),
    ON_WIN_VIDEO    ( 7, "ON_WIN_VIDEO"),
    ON_DEMO_VIDEO   ( 8, "ON_DEMO_VIDEO"),
    MONEY_VIDEO     ( 9, "MONEY_VIDEO"),
    CARD_VIDEO      (10, "CARD_VIDEO");
}



object Config {

        var msgErro: String? = null
        var server: Server = Server("http://vm.sger.com.br/", 1234, "", "")
        var creditValue: Int = 500
        var alarmTime: Int = 30
        var demoTime: Int = 120
        var tryTime: Int = 40
        var audioTry: Media = Media("/storage/emulated/0/JMGames/A_Playing.mp3", 99)
        var audioLose: Media = Media("/storage/emulated/0/JMGames/A_Gameover.mp3", 99)
        var videoWin: Media = Media("/storage/emulated/0/JMGames/V_Success.mp4", 99)
        var videosDemo: ArrayList<Media> = arrayListOf(
            Media("/storage/emulated/0/JMGames/V_Demo1.mp3", 99),
            Media("/storage/emulated/0/JMGames/V_Demo2.mp3", 99)
        )
        var videoMoney: Media = Media("/storage/emulated/0/JMGames/V_Money.mp4", 99)
        var videoCard: Media = Media("/storage/emulated/0/JMGames/V_Card.mp4", 99)

    init {
        Timber.e("===== =====  ==== Init 2222")
        printConfig()
    }


    fun loadConfig( context : Context, file: String) : Boolean {
        val jsonObject : JSONObject?
        var curItem: String = ""

        if ( ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED ) {
            msgErro = "\n   Sem permissao para ler arquivo   \n   " + file +  "   \n"
            return false
        }

        try {
            jsonObject = JSONObject(readJsonFile(file))
        } catch (e: Exception) {
            msgErro = "Invalid file"
            Timber.e("%s: %s", msgErro, e.message.toString())
            return false
        }

        try {
            for (value in ConfigType.values()) {
                curItem = value.token
                when(value) {
                    ConfigType.SERVER            -> server      = getServer(jsonObject.getJSONObject(value.token))
                    ConfigType.CREDIT_VALUE      -> creditValue = jsonObject.getInt(value.token)
                    ConfigType.ALARM_TIME        -> alarmTime   = jsonObject.getInt(value.token)
                    ConfigType.DEMO_TIME         -> demoTime    = jsonObject.getInt(value.token)
                    ConfigType.TRY_TIME          -> tryTime     = jsonObject.getInt(value.token)
                    ConfigType.ON_DEMO_VIDEO     -> videosDemo  = getDemoVideos(jsonObject.getJSONArray(value.token))
                    ConfigType.DURING_TRY_AUDIO  -> audioTry    = getMedia(jsonObject.getJSONObject(value.token))
                    ConfigType.ON_LOSE_AUDIO     -> audioLose   = getMedia(jsonObject.getJSONObject(value.token))
                    ConfigType.ON_WIN_VIDEO      -> videoWin    = getMedia(jsonObject.getJSONObject(value.token))
                    ConfigType.MONEY_VIDEO       -> videoMoney  = getMedia(jsonObject.getJSONObject(value.token))
                    ConfigType.CARD_VIDEO        -> videoCard   = getMedia(jsonObject.getJSONObject(value.token))
                }
            }
        } catch (e: Exception) {
            msgErro = curItem
            Timber.e("Config item: %s: %s", msgErro, e.message.toString())
            return false
        }

        Timber.e("===== =====  ==== Init 3333")

        printConfig()
        return true
    }


    private fun printConfig() {
        for (value in ConfigType.values()) {
            when(value) {
                ConfigType.SERVER            -> Timber.i("%-20s = %s", value.token, server.toString())
                ConfigType.CREDIT_VALUE      -> Timber.i("%-20s = %d", value.token, creditValue)
                ConfigType.ALARM_TIME        -> Timber.i("%-20s = %d", value.token, alarmTime)
                ConfigType.DEMO_TIME         -> Timber.i("%-20s = %d", value.token, demoTime)
                ConfigType.TRY_TIME          -> Timber.i("%-20s = %d", value.token, tryTime)
                ConfigType.ON_DEMO_VIDEO     -> {
                    videosDemo.forEach {
                        Timber.i("%-20s Volume: %d File:[%s]", value.token, it.volume, it.filename)
                    }
                }
                ConfigType.DURING_TRY_AUDIO  -> Timber.i("%-20s Volume: %d File:[%s]", value.token, audioTry.volume, audioTry.filename)
                ConfigType.ON_LOSE_AUDIO     -> Timber.i("%-20s Volume: %d File:[%s]", value.token, audioLose.volume, audioLose.filename)
                ConfigType.ON_WIN_VIDEO      -> Timber.i("%-20s Volume: %d File:[%s]", value.token, videoWin.volume, videoWin.filename)
                ConfigType.MONEY_VIDEO       -> Timber.i("%-20s Volume: %d File:[%s]", value.token, videoMoney.volume, videoMoney.filename)
                ConfigType.CARD_VIDEO        -> Timber.i("%-20s Volume: %d File:[%s]", value.token, videoCard.volume, videoCard.filename)
            }
        }
    }

    private fun readJsonFile(file : String): String {
        val mtStream = FileInputStream(file)
        var jsonString = ""

        mtStream.use { stream ->
            val fileChannel = stream.channel
            val mappedByteBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()
            )
            jsonString = Charset.defaultCharset().decode(mappedByteBuffer).toString()
        }
        return jsonString
    }


    private fun getServer(jsonObject: JSONObject): Server {
        return Server(
            jsonObject.getString("host"),
            jsonObject.getInt("port"),
            jsonObject.getString("username"),
            jsonObject.getString("password")
        )
    }

    private fun getMedia(jsonObject: JSONObject): Media {
        return Media(
            jsonObject.getString("filename"),
            jsonObject.getInt("volume")
        )
    }

    private fun getDemoVideos(jsonArray: JSONArray): ArrayList<Media> {
        val medias = ArrayList<Media>()
        for ( x in 0 until jsonArray.length()) {
            medias.add( Media(
                jsonArray.getJSONObject(x).getString("filename"),
                jsonArray.getJSONObject(x).getInt("volume")))
        }
        return medias
    }
}

data class Server (val host: String, val port: Int, val username: String, val password:String ) {

}

data class Media(var filename: String="") {

    var volume: Int =0

    constructor (file:String, volume:Int) : this (file) {
        this.volume = volume
        if ( this.volume < 0) {
            this.volume = 0
        }
        if (this. volume > 99) {
            this.volume = 99
        }
    }
}