package com.example.atestgame1

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.list_item.view.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Handler


enum class LogType  {
    TO_LOG,
    TO_HISTORY;
}

enum class LogStatus  {
    DISABLED,
    ENABLED;
}


@SuppressLint("StaticFieldLeak")
object  ScreenLog {

    var status = LogStatus.ENABLED
    private var linesToLog: MutableList<String> = mutableListOf()
    private var linesToHistory: MutableList<String> = mutableListOf()
    private var MAX_LOG_LINES=300

    private var logView: RecyclerView? = null
    private var myActivity: AppCompatActivity? = null
    private var myContext: Context? = null
    private var logAdapter : LogAdapter? = null
//    private var myHandler: Handler? = null


    var logMainList = ArrayList<String>()

    fun start(mainActivity: AppCompatActivity, context: Context, view: RecyclerView) {
        myActivity = mainActivity
        myContext = context
        logView = view
        logAdapter = LogAdapter(myContext!!, logMainList)


        view.layoutManager = LinearLayoutManager(mainActivity)
        view.adapter = logAdapter

//        myHandler = handler

        enable()
    }

    private var updateMostraNaTela = Runnable {
        updateMainView()
    }


    fun enable() {
        if ( logView != null ) {
            status = LogStatus.ENABLED
        }
        add(LogType.TO_LOG, "Start")
    }

    fun add(logType : LogType, message : String) {
        val strHora1 = SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().time)
        val newString = "$strHora1 - $message"

        Timber.i(message)

        if ( status == LogStatus.ENABLED ) {
            if ( logType == LogType.TO_HISTORY) {
                linesToHistory.add(newString)
            }
            linesToLog.add(newString)
            (myActivity as MainActivity).screenLogHandler.removeCallbacks(updateMostraNaTela)
            (myActivity as MainActivity).screenLogHandler.postDelayed(updateMostraNaTela, 10)
        }

    }

    fun setLogLines(size:Int) {
        MAX_LOG_LINES = size
    }

    fun updateMainView() {
        val linesToMove = linesToLog.size

        Thread.currentThread().priority = 1

        // Copy lines from myBackgroundList to myList
        for (line in 0 until linesToMove ) {
            if (logMainList.size >= MAX_LOG_LINES) {
                logMainList.removeAt(0)
            }
            logMainList.add(linesToLog[line])
        }

        logAdapter!!.notifyDataSetChanged()
        (myActivity as MainActivity).log_recycler_view.smoothScrollToPosition(logAdapter!!.getItemCount() - 1)

        // Remove lines from myBackgroundList to myList
        for (line in 0 until linesToMove) {
            linesToLog.removeAt(0)
        }
    }
}


class LogAdapter(private val context: Context, val list: ArrayList<String>): RecyclerView.Adapter<LogAdapter.ViewHolder>() {
    var contaId=0
    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false)
//        println("onCreateViewHolder position = $position")
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
//        println("getItemCount myList.count = ${list.count()}")
        return list.count()
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val str : String?  = list.get(position)
//        println("onBindViewHolder position = $position - ${list[position]}  id:${viewHolder.id}")
        if ( str != null) {
            viewHolder.bind(str)
        }
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var id=contaId++
        fun bind(myItem:String) {
//            println("bind myItem = $myItem")
            itemView.tv_title.text = myItem
        }

    }
}