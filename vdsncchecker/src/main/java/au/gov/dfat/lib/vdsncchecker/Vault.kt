package au.gov.dfat.lib.vdsncchecker

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.*
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.cert.Certificate
import java.util.*
import javax.crypto.spec.SecretKeySpec

@RequiresApi(Build.VERSION_CODES.O)
class CertificateVault private constructor(val homeDir: String) : AVault(
    Paths.get(homeDir, ".secrets", "key.store"),
    Paths.get(homeDir, ".secrets", "key.password")) {
    override fun entryReturn(entry: KeyStore.Entry): String {
        if(entry is KeyStore.TrustedCertificateEntry){
            return certificateReturn(entry)
        }
        return String((entry as KeyStore.SecretKeyEntry).secretKey.encoded)
    }

    fun certificateReturn(entry: KeyStore.Entry): String{
        return String((entry as KeyStore.TrustedCertificateEntry).trustedCertificate.encoded)
    }

    companion object{

        private var appContext: Context? = null

        fun setContext(app: Context)
        {
            appContext = app
        }

        fun getInstance(): CertificateVault?{

            if(appContext == null)
                throw Exception("Application context must first be initialised via a call to Vault.setContext")

            if(instance == null){
                val wrapper = ContextWrapper(appContext).baseContext.dataDir.absolutePath
                instance = CertificateVault(wrapper)
            }

            return instance


        }

        private var instance: CertificateVault? = null
    }


}

@RequiresApi(Build.VERSION_CODES.O)
abstract class AVault(
    private val keyStorePath: Path = Paths.get(System.getProperty("user.home"), ".secrets", "key.store"),
                     private val passwordPath: Path = Paths.get(System.getProperty("user.home"), ".secrets", "key.password")
              ) {

    private val keyStoreFile: File
        get() {
            try {
                val dirPath = keyStorePath.parent

                if (!dirPath.toFile().exists()) {
                    dirPath.toFile().mkdirs()
                }

                return keyStorePath.toFile()
            }
            catch(e: Exception)
            {
                throw VaultException(VaultError.GET_STORE_FILE)
            }
        }

    private val keyStore: KeyStore by lazy {
        try{
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())

            if (keyStoreFile.exists()) {
                val stream = FileInputStream(keyStoreFile)
                keyStore.load(stream, password)
            } else {
                keyStore.load(null, password)
            }

            keyStore
        }
        catch(exception: IOException){
            throw VaultException(VaultError.LOAD_KEYSTORE)
        }
    }

    private val password: CharArray by lazy {

        val dirPath = passwordPath.parent

        if (!dirPath.toFile().exists()) {
            dirPath.toFile().mkdirs()
        }

        val path = passwordPath.toFile()

        if (path.exists()) {
            path.readText().toCharArray()
        } else {
            val uuid = UUID.randomUUID().toString()
            path.writeText(uuid)
            uuid.toCharArray()
        }
    }

    fun getEntry(entry: String): String? {
        try{
            if (!keyStoreFile.exists()) {
                return null
            }

            val protection = KeyStore.PasswordProtection(password)
            val secret = keyStore.getEntry(entry, protection)

            return if (secret != null) {
                entryReturn(secret)
            } else {
                return ""
            }
        }
        catch(exception: Exception){
            throw VaultException(VaultError.GET_ENTRY)
        }
    }

    abstract fun entryReturn(entry: KeyStore.Entry): String

    fun setCertificateEntry(entry: String, value: Certificate){
        try {
            val protection = KeyStore.PasswordProtection(password)
            keyStore.setEntry(entry, KeyStore.TrustedCertificateEntry(value), protection)
            storeEntry()
        }
        catch(e: Exception)
        {
            throw VaultException(VaultError.SET_ENTRY)
        }
    }

    fun setEntry(entry: String, value: String) {
        if(value == "")
            return

        try {
            val protection = KeyStore.PasswordProtection(password)

            val encoded = SecretKeySpec(value.toByteArray(), "AES")

            keyStore.setEntry(entry, KeyStore.SecretKeyEntry(encoded), protection)

            storeEntry()
        }
        catch(e: Exception)
        {
            throw VaultException(VaultError.SET_ENTRY)
        }
    }

    private fun storeEntry(){
        try {
            val stream = FileOutputStream(keyStoreFile)
            stream.use {
                keyStore.store(it, password)
            }
        }
        catch (e: Exception)
        {
            throw VaultException(VaultError.STORE_ENTRY)
        }
    }

    fun destroy() {
        Files.deleteIfExists(keyStorePath)
        Files.deleteIfExists(passwordPath)
    }
}

