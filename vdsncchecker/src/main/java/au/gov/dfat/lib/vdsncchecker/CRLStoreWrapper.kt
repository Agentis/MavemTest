package au.gov.dfat.lib.vdsncchecker

import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import java.net.URL
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

@RequiresApi(Build.VERSION_CODES.O)
class CRLStoreWrapper(url: URL?, data: ByteArray?) {

    private val keychainKeyDatePrefix = "vdsncchecker.downloaded."
    private val keychainKeyCrlPrefix = "vdsncchecker.crldata."

    var url: URL? = null
    var data: ByteArray? = null
    var dateLastDownloaded: Date? = null

    init{
        this.url = url

        if(url == null && data != null)
        {
            // This option uses the given data and will never auto-update, given the lack of a URL
            this.data = data
        }
        else if(url != null && data != null){
            // In this option, the system will still use the value from the vault if it is present. Otherwise it will use the given data.
            val tempData = getCRLDataFromVault()
            if(tempData != null)
                this.data = tempData
            else
                this.data = data
        }
        else if(url != null && data == null){
            // no action necessary
        }
        else
        {
            // Can't do anything if there are no parameters
            throw IllegalArgumentException("Provide URL, data, or both")
        }

        if(data == null){
            // if no initial data is provided, check the Vault for it
            val temp = Base64.getDecoder().decode(CertificateVault.getInstance()!!.getEntry(keychainKeyCrlPrefix + url.toString()))
            if(temp != null && !temp.isEmpty())
                this.data = temp
        }
        else {
            this.data = data
        }

        this.saveCRLDataToVault()
        this.dateLastDownloaded = getUpdatedDateFromVault()
    }

    private fun saveCRLDataToVault(){
        if(url == null)
            return

        CertificateVault.getInstance()!!.setEntry(keychainKeyCrlPrefix + this.url.toString(), Base64.getEncoder().encodeToString(this.data))
    }

    private fun saveUpdatedDateToVault(){
        if(dateLastDownloaded == null)
            return

        CertificateVault.getInstance()!!.setEntry(keychainKeyDatePrefix + this.url.toString(), this.dateLastDownloaded!!.toString())
    }

    private fun getUpdatedDateFromVault() : Date? {
        val value = CertificateVault.getInstance()!!.getEntry(keychainKeyDatePrefix + this.url.toString())
        if(value == null || value == "")
            return null

        return Date(value)
    }

    private fun getCRLDataFromVault() : ByteArray? {
        if(url == null) // not in vault if theres no URL
        {
            if(data != null) // but if theres already data on the object, return that
                return data
            return null
        }

        val entryAsString = CertificateVault.getInstance()!!.getEntry(keychainKeyCrlPrefix + this.url.toString())

        if(entryAsString == null)
            return null

        return Base64.getDecoder().decode(entryAsString)
    }



    // It is expected that this method will be called from a coroutine
    // Returns false on any exception that would indicate a network failure
    fun download() : Boolean {

        try {
            if (url == null) {
                return true
            }

            var connection = if (url!!.protocol == "https") {
                url!!.openConnection() as HttpsURLConnection

                // TODO: ICAO is hosting certs at a location with an invalid certificate! What in the heck are we supposed to do?
                val allHostsValid = HostnameVerifier { hostname, session -> true }
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)

            } else {
                url!!.openConnection() as HttpURLConnection
            }

            val input = BufferedInputStream(url!!.openStream(), 8192)

            val output = input.readBytes()

            input.close()

            this.data = output
            this.dateLastDownloaded = Date()

            this.saveCRLDataToVault()
            this.saveUpdatedDateToVault()

            return true
        }
        catch(e: Exception)
        {
            // TODO: Handle this exception; for now just return false so that the parent can handle rescheduling
            return false
        }
    }


}