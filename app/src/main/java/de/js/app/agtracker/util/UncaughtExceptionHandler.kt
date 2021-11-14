package de.js.app.agtracker.util

import android.os.Build
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream


class UncaughtExceptionHandler(private val baseDirectory: File) : Thread.UncaughtExceptionHandler {
    private val SINGLE_LINE_SEP = "\n"
    private val DOUBLE_LINE_SEP = "\n\n"

    override fun uncaughtException(t: Thread, e: Throwable) {

        val report = StringBuffer(e.toString())
        report.append(DOUBLE_LINE_SEP)
        report.append("--------- Stack trace ---------\n")
        var bas = ByteArrayOutputStream()
        var ps = PrintStream(bas)
        e.printStackTrace(ps)
        e.printStackTrace()
        report.append(bas.toString(Charsets.UTF_8.name()))


        // Getting the Device brand,model and sdk verion details.
        report.append(DOUBLE_LINE_SEP)
        report.append("--------- Device ---------\n")
        report.append("Brand: ")
        report.append(Build.BRAND)
        report.append(SINGLE_LINE_SEP)
        report.append("Device: ")
        report.append(Build.DEVICE)
        report.append(SINGLE_LINE_SEP)
        report.append("Model: ")
        report.append(Build.MODEL)
        report.append(SINGLE_LINE_SEP)
        report.append("Id: ")
        report.append(Build.ID)
        report.append(SINGLE_LINE_SEP)
        report.append("Product: ")
        report.append(Build.PRODUCT)
        report.append(SINGLE_LINE_SEP)
        report.append("--------- Firmware ---------\n")
        report.append("SDK: ")
        report.append(Build.VERSION.SDK)
        report.append(SINGLE_LINE_SEP)
        report.append("Release: ")
        report.append(Build.VERSION.RELEASE)
        report.append(SINGLE_LINE_SEP)
        report.append("Incremental: ")
        report.append(Build.VERSION.INCREMENTAL)
        report.append(SINGLE_LINE_SEP)

        Log.e("Report ::", report.toString())

        // wirte crashfile
        val crashReportFile: File = File(baseDirectory, "crashReport.txt")
        FileUtilities.writefile(report.toString(), crashReportFile)

        System.exit(0)
        // If you don't kill the VM here the app goes into limbo

    }

    init {

    }
}