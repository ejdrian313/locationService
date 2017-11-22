package pl.kotliners.locationservice.Service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import pl.kotliners.locationservice.Util.Utils


/**
 * Created by ejdrian on 20.11.17.
 */

class StartService : BroadcastReceiver() {

    var alarmManager:AlarmManager? = null
    lateinit var myIntent: Intent
    lateinit var pendingIntent: PendingIntent

    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent!!.action == "android.intent.action.BOOT_COMPLETED") {
           start(context!!)
        }
    }

    fun start(context: Context) {
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        myIntent = Intent(context, LocationService::class.java)
        pendingIntent = PendingIntent.getService(context, 0, myIntent, 0)
        val period = Utils.minutesToMiliSeconds(1)

        alarmManager!!.set(AlarmManager.ELAPSED_REALTIME,  SystemClock.elapsedRealtime() , pendingIntent)
        alarmManager!!.setRepeating(AlarmManager.ELAPSED_REALTIME,  SystemClock.elapsedRealtime() , period, pendingIntent)
    }

    fun stop(context: Context) {
        alarmManager?.cancel(pendingIntent)
    }
}
