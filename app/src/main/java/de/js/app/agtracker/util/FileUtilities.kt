package de.js.app.agtracker.util

import android.util.Log
import java.io.*
import java.util.*


object FileUtilities {
    /**
     * Copy a file.
     *
     * @param fromFile source file.
     * @param toFile   dest file.
     * @throws IOException if something goes wrong.
     */
    @Throws(IOException::class)
    fun copyFile(fromFile: String?, toFile: String?) {
        val `in` = File(fromFile)
        val out = File(toFile)
        FileUtilities.copyFile(`in`, out)
    }

    /**
     * Copy a file.
     *
     * @param in  source file.
     * @param out dest file.
     * @throws IOException if something goes wrong.
     */
    @Throws(IOException::class)
    fun copyFile(`in`: File?, out: File?) {
        val fis = FileInputStream(`in`)
        val fos = FileOutputStream(out)
        FileUtilities.copyFile(fis, fos)
    }

    /**
     * Copy a file.
     *
     * @param fis source file.
     * @param fos dest file.
     * @throws IOException if something goes wrong.
     */
    @Throws(IOException::class)
    fun copyFile(fis: InputStream?, fos: OutputStream?) {
        try {
            val buf = ByteArray(1024)
            var i = 0
            while (fis!!.read(buf).also { i = it } != -1) {
                fos!!.write(buf, 0, i)
            }
        } finally {
            fis?.close()
            fos?.close()
        }
    }

    /**
     * Get the file name without extension.
     *
     * @param file the file.
     * @return the name.
     */
    fun getNameWithoutExtention(file: File): String {
        var name = file.name
        val lastDot = name.lastIndexOf(".") //$NON-NLS-1$
        name = name.substring(0, lastDot)
        return name
    }

    /**
     * Read a file to string.
     *
     * @param file file to read.
     * @return the read string.
     * @throws IOException if something goes wrong.
     */
    @Throws(IOException::class)
    fun readfile(file: File?): String {
        val sb = StringBuilder()
        var br: BufferedReader? = null
        return try {
            br = BufferedReader(FileReader(file))
            var line: String? = null
            while (br.readLine().also { line = it } != null) {
                if (line!!.length == 0 || line!!.startsWith("#")) { //$NON-NLS-1$
                    continue
                }
                sb.append(line).append("\n") //$NON-NLS-1$
            }
            sb.toString()
        } finally {
            if (br != null) {
                try {
                    br.close()
                } catch (e: IOException) {
                    Log.e("FILEUTILS", e.localizedMessage, e) //$NON-NLS-1$
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Read a file to a list of line strings.
     *
     * @param file file to read.
     * @return the read lines list.
     * @throws IOException if something goes wrong.
     */
    @Throws(IOException::class)
    fun readfileToList(file: File?): List<String> {
        var br: BufferedReader? = null
        val linesList: MutableList<String> = ArrayList()
        return try {
            br = BufferedReader(FileReader(file))
            var line: String? = null
            while (br.readLine().also { line = it } != null) {
                if (line!!.length == 0 || line!!.startsWith("#")) { //$NON-NLS-1$
                    continue
                }
                linesList.add(line!!.trim { it <= ' ' })
            }
            linesList
        } finally {
            if (br != null) {
                try {
                    br.close()
                } catch (e: IOException) {
                    Log.e("FILEUTILS", e.localizedMessage, e) //$NON-NLS-1$
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Write a string to file.
     *
     * @param text the string.
     * @param file the file.
     * @throws IOException if something goes wrong.
     */
    @Throws(IOException::class)
    fun writefile(text: String?, file: File?) {
        var bw: BufferedWriter? = null
        try {
            bw = BufferedWriter(FileWriter(file))
            bw.write(text)
        } finally {
            if (bw != null) {
                try {
                    bw.close()
                } catch (e: IOException) {
                    Log.e("FILEUTILS", e.localizedMessage, e) //$NON-NLS-1$
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Write a byte[] to file.
     *
     * @param data     byte[].
     * @param fileName the fileName.
     * @throws IOException if something goes wrong.
     */
    @Throws(IOException::class)
    fun writefiledata(data: ByteArray?, fileName: String?) {
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(fileName)
            out.write(data)
        } finally {
            if (out != null) {
                try {
                    out.close()
                } catch (e: IOException) {
                    Log.e("FILEUTILS", e.localizedMessage, e) //$NON-NLS-1$
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Returns true if all deletions were successful. If a deletion fails, the method stops
     * attempting to delete and returns false.
     *
     * @param filehandle file to remove.
     * @return true if all deletions were successful
     */
    fun deleteFileOrDir(filehandle: File): Boolean {
        if (filehandle.isDirectory) {
            val children = filehandle.list()
            for (i in children.indices) {
                val success: Boolean = FileUtilities.deleteFileOrDir(
                    File(
                        filehandle,
                        children[i]
                    )
                )
                if (!success) {
                    return false
                }
            }
        }

        // The directory is now empty so delete it
        val isdel = filehandle.delete()
        if (!isdel) {
            // if it didn't work, which often happens on windows systems,
            // remove on exit
            filehandle.deleteOnExit()
        }
        return isdel
    }

    /**
     * Delete file or folder recursively on exit of the program
     *
     * @param filehandle file to remove.
     * @return true if all went well
     */
    fun deleteFileOrDirOnExit(filehandle: File): Boolean {
        if (filehandle.isDirectory) {
            val children = filehandle.list()
            for (i in children.indices) {
                val success: Boolean = FileUtilities.deleteFileOrDir(
                    File(
                        filehandle,
                        children[i]
                    )
                )
                if (!success) {
                    return false
                }
            }
        }
        filehandle.deleteOnExit()
        return true
    }

    /**
     * Checks if a given file exists in a supplied folder.
     *
     * @param fileName the name of the file to check.
     * @param folder   the folder.
     * @return `true`, if the file exists
     */
    fun fileExistsInFolder(fileName: String, folder: File): Boolean {
        val listFiles =
            folder.listFiles { arg0, tmpName -> fileName.trim { it <= ' ' } == tmpName.trim { it <= ' ' } }
        return listFiles.size > 0
    }

    /**
     * Read files to byte array.
     *
     * @param file the file to read.
     * @return the read byte array.
     * @throws IOException if something goes wrong.
     */
    @Throws(IOException::class)
    fun readFileToByte(file: File?): ByteArray {
        val f = RandomAccessFile(file, "r") //NON-NLS
        return try {
            val length = f.length()
            val data = ByteArray(length.toInt())
            f.readFully(data)
            data
        } finally {
            f.close()
        }
    }

    /**
     * Recursive search of files with a specific extension.
     *
     *
     * This can be called multiple times, adding to the same list
     *
     * @param searchDir        the directory to read.
     * @param searchExtentions the extensions of the files to search for.
     * @param returnFiles      the List<File> where the found files will be added to.
     * @return the number of files found.
    </File> */
    fun searchDirectoryRecursive(
        searchDir: File,
        searchExtentions: Array<String?>,
        returnFiles: MutableList<File?>
    ): Int {
        val listFiles = searchDir.listFiles()
        for (thisFile in listFiles) {
            // mj10777: collect desired extension
            if (thisFile.isDirectory) {
                // mj10777: read recursive directories inside the
                // sdcard/maps directory
                FileUtilities.searchDirectoryRecursive(thisFile, searchExtentions, returnFiles)
            } else {
                for (searchExtention in searchExtentions) {
                    if (thisFile.name.endsWith(searchExtention!!)) {
                        returnFiles.add(thisFile)
                    }
                }
            }
        }
        return returnFiles.size
    }

    /**
     * Method to read a properties file into a [LinkedHashMap].
     *
     *
     *
     *Empty lines are ignored, as well as lines
     * that do not contain the separator.
     *
     * @param file       the file to read.
     * @param separator  the separator or `null`. Defaults to '='.
     * @param valueFirst if `true`, the second part of the string is used as key.
     * @return the read map.
     */
    @Throws(IOException::class)
    fun readFileToHashMap(
        file: File?,
        separator: String?,
        valueFirst: Boolean
    ): LinkedHashMap<String, String> {
        var separator = separator
        if (separator == null) {
            separator = "="
        }
        var lines: List<String> = FileUtilities.readfileToList(file)
        val propertiesMap = LinkedHashMap<String, String>()
        for (l in lines) {
            val line = l.trim { it <= ' ' }
            if (line.length == 0) {
                continue
            }
            val firstSep = line.indexOf(separator)
            if (firstSep == -1) {
                continue
            }
            val first = line.substring(0, firstSep)
            val second = line.substring(firstSep + 1)
            if (!valueFirst) {
                propertiesMap[first] = second
            } else {
                propertiesMap[second] = first
            }
        }
        return propertiesMap
    }
}