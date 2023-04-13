package de.js.app.agtracker.location

import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets

private const val TAG = "MyNtripClient"

/**
 * NTRIP client
 */
class MyNtripClient(params: Map<String, Any>) {
    private lateinit var dataReceivedListener: OnDataReceivedListener
    private var username = params[NTRIP_USERNAME] as String
    private var password = params[NTRIP_PASSWORD] as String
    private var mountpoint = params[NTRIP_MOUNTPOINT] as String
    private var server = params[NTRIP_SERVER] as String
    private var port = params[NTRIP_PORT] as Int
    private var gga = ""

    companion object{
        const val NTRIP_USERNAME = "NTRIP_USERNAME"
        const val NTRIP_PASSWORD = "NTRIP_PASSWORD"
        const val NTRIP_MOUNTPOINT = "NTRIP_MOUNTPOINT"
        const val NTRIP_SERVER = "NTRIP_SERVER"
        const val NTRIP_PORT = "NTRIP_PORT"
        const val DEFAULT_TIMEOUT = 10000
    }

    /** Connect to NTRIP caster */
    private fun connect(mountpoint: String): HttpURLConnection {
        // set up connection
        val protocol = "http"
        val casterURL = URL(protocol, this.server, this.port, "/$mountpoint")
        val connection = casterURL.openConnection() as HttpURLConnection
        connection.connectTimeout = DEFAULT_TIMEOUT
        connection.readTimeout = DEFAULT_TIMEOUT

        // common headers
        connection.setRequestProperty("Host", this.server)
        connection.setRequestProperty("Ntrip-Version", "Ntrip/2.0")
        connection.setRequestProperty(
            "User-Agent",
            "NTRIP MyNtripClient/1.0"
        ) // must start with NTRIP!
        connection.setRequestProperty("Connection", "close")

        // authentication
        val auth = "$username:$password"
        val encodedAuth: String =
            android.util.Base64.encodeToString(
                auth.toByteArray(StandardCharsets.UTF_8),
                auth.length
            )
        connection.setRequestProperty("Authorization", "Basic $encodedAuth")

        // send GGA if available
        if (gga.isNotEmpty()) {
            connection.setRequestProperty("Ntrip-GGA", gga)
        }

        return connection
    }

    fun setOnDataReceivedListener(listener: OnDataReceivedListener) {
        this.dataReceivedListener = listener
    }

    /** Run */
    fun call() {

        Log.d(TAG, "Client GGA is $gga")
        // prepare request
        val connection = connect(mountpoint)
        // perform request
        val responseCode = connection.responseCode //implicitly connects
        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            Log.w(TAG, "Unauthorized")
        } else if (responseCode != HttpURLConnection.HTTP_OK) {
            Log.w(TAG, "Not OK: $responseCode")
        }

        // what do we get back?
        Log.d(TAG, "Content: ${connection.contentType}")
        Log.d(TAG, "Content Length: ${connection.contentLength}")
        Log.d(TAG, "Content Encoding: ${connection.contentEncoding}")
        Log.d(TAG, "Response Message: ${connection.responseMessage}")
        Log.d(TAG, "Response Code: ${connection.responseCode}")

        // Read data
        try {
            val bArr = ByteArray(4096)
            var read = connection.inputStream.read(bArr, 0, 4096)
            Log.d(TAG, "Read $read bytes from NTRIP server")
            while (read != -1) {
                // copy read bytes to byte array 1
                val bArr2 = ByteArray(read)
                System.arraycopy(bArr, 0, bArr2, 0, read)

                // send data to listener
                dataReceivedListener.onDataReceived(bArr2)

                // read again
                read = connection.inputStream.read(bArr, 0, 4096)
            }
        } catch (ste: SocketTimeoutException) {
            // ignore exception, it will be handled by next round
            Log.e(TAG, "SocketTimeoutException: " + ste.message, ste)
        } catch (ioe: IOException) {
            Log.e(TAG, "IOException: " + ioe.message, ioe)
        }
    }


    fun setGGA(newGga: String) {
        gga = newGga
    }

    interface OnDataReceivedListener {
        fun onDataReceived(data: ByteArray)
    }
}