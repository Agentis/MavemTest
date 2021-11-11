package au.gov.dfat.lib.vdsncchecker

import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer

class CRLManager {

    companion object {

        private var updateTimer: Timer? = null

        private var reconnectTimer: Timer? = null

        private var connectionFailure = false

        private var isAutoUpdating = false

        var crls = ArrayList<CRLStoreWrapper>().toTypedArray()

        var secondsBetweenUpdates = 86400 // 1 day

        var maxSecondsBeforeOverdue = 864000 // 10 days

        private var appContext: Context? = null

        fun setContext(app: Context)
        {
            appContext = app

            CertificateVault.setContext(app);
        }

        /**
         * Inspects the CRL list and verifies if any entries are older than the maxSecondsBeforeOverdue age in seconds
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun isUpdateOverdue() : Boolean
        {
            val c = Calendar.getInstance()

            crls.forEach {
                if(it.dateLastDownloaded == null)
                    return true

                c.time = it.dateLastDownloaded!!
                c.add(Calendar.SECOND, maxSecondsBeforeOverdue)

                if(Date().after(c.time))
                    return true
            }

            return false
        }

        /**
         * Set up the certificate revocation list. Overrides any existing data.
         *
         * @param data An array of CRLStoreWrapper objects
         */
        fun setupCRLs(data: Array<CRLStoreWrapper>)
        {
            crls = data
        }

        /**
         * Begin a background process which updates the CRL list from the sources provided on a periodic basis.
         * Default update interval is 1 day/86400 seconds.
         *
         * @param p_secondsBetweenUpdates Overrides the default update interval
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun startAutoUpdatingCRLData(p_secondsBetweenUpdates : Int? = null) {
            if(p_secondsBetweenUpdates != null)
                this.secondsBetweenUpdates = p_secondsBetweenUpdates

            isAutoUpdating = true

            updateTimer?.cancel() // cancel the timer if it already exists

            val asMilliseconds = (this.secondsBetweenUpdates * 1000).toLong()

            updateTimer = fixedRateTimer("crlCheckTimer", true, asMilliseconds, asMilliseconds){
                updateCRLData()
            }
        }

        /**
         * Cancels the CRL auto update process
         */
        fun stopAutoUpdatingCRLData(){
            isAutoUpdating = false
            updateTimer?.cancel()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun waitForReconnection(){
            if(appContext == null)
                throw Exception("Application context must first be initialised via a call to CRLManager.setContext")

            // unlike iOS, we can't wait for an "internet is available again" message, so we have to periodically check again...
            val asMilliseconds = (600 * 1000).toLong() // every 10 minutes

            reconnectTimer?.cancel() // just in case

            reconnectTimer = fixedRateTimer("crlReconnectTimer", true, asMilliseconds, asMilliseconds) {

                val connectivityManager = ContextWrapper(appContext).getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

                // if any of these capabilities are detected, we are good to go
                if(capabilities!!.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                {
                    reconnectTimer?.cancel() // stop the reconnect timer

                    updateCRLData() // perform an immediate update no matter what

                    if(isAutoUpdating) // if the user had set up auto-updates, recommence them
                        startAutoUpdatingCRLData()
                }
            }
        }

        /**
         * Manually initiate a refresh of CRL data based on the URLs provided in the CRLStoreWrapper list
         *
         * @param complete A callback function called on successful update of the list
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun updateCRLData(complete: ((count: Int) -> Unit)? = null) {
            GlobalScope.launch {

                connectionFailure = false

                // Since this parent method is itself asynchronous, we will call .download synchronously and download each item individually in sequence
                crls.forEach {
                    connectionFailure = connectionFailure || !it.download()
                }

                if(connectionFailure) // If downloads/connection literally failed, we need to perform additional steps
                {
                    waitForReconnection()
                    updateTimer?.cancel()
                }
                else {
                    CRLManagerUpdateEvent.post(crls.size)
                    if(complete != null)
                        complete(crls.size)
                }
            }
        }

    }

}

/**
 * Provides an update event which can be subscribed to whenever the CRLManager CRL list is updated from its source URLs
 */
class CRLManagerUpdateEvent{

    // https://jayrambhia.com/notes/eventbus-rxkotlin

    companion object {
        val publisher: PublishSubject<Any> = PublishSubject.create()

        inline fun <reified T> subscribe(): Observable<T>? {
            return publisher.filter {
                it is T
            }.map {
                it as T
            }
        }

        fun post(recordsUpdated: Int) {
            publisher.onNext(recordsUpdated)
        }
    }
}