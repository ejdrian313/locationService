package pl.kotliners.locationservice.Service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by ejdrian on 20.11.17.
 */

class  StartWithOS: BroadcastReceiver(){

    override fun onReceive(context: Context?, intent: Intent?) {

         if (intent!!.action == "android.intent.action.BOOT_COMPLETED"){
            val intent= Intent(context, LocationService::class.java)
            context!!.startService(intent)
        }
    }
}