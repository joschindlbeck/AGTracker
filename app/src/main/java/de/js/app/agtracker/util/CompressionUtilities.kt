package de.js.app.agtracker.util

import android.util.Log
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


/**
 * Utilities class to zip and unzip folders.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
object CompressionUtilities {
    /**
     *
     */
    const val THE_BASE_FILE_IS_SUPPOSED_TO_BE_A_DIRECTORY =
        "The base file is supposed to be a directory."

    /**
     *
     */
    const val FILE_EXISTS = "FILE EXISTS"

    /**
     * Compress a folder and its contents.
     *
     * @param srcFolder    path to the folder to be compressed.
     * @param destZipFile  path to the final output zip file.
     * @param excludeNames names of files to exclude.
     * @throws IOException if something goes wrong.
     */
    @Throws(IOException::class)
    fun zipFolder(srcFolder: String, destZipFile: String?, vararg excludeNames: String) {
        if (File(srcFolder).isDirectory) {
            var zip: ZipOutputStream? = null
            var fileWriter: FileOutputStream? = null
            try {
                fileWriter = FileOutputStream(destZipFile)
                zip = ZipOutputStream(fileWriter)
                CompressionUtilities.addFolderToZip("", srcFolder, zip, *excludeNames) //$NON-NLS-1$
            } finally {
                if (zip != null) {
                    zip.flush()
                    zip.close()
                }
                fileWriter?.close()
            }
        } else {
            throw IOException(CompressionUtilities.THE_BASE_FILE_IS_SUPPOSED_TO_BE_A_DIRECTORY) //$NON-NLS-1$
        }
    }

    /**
     * Uncompress a compressed file to the contained structure.
     *
     * @param zipFile      the zip file that needs to be unzipped
     * @param destFolder   the folder into which unzip the zip file and create the folder structure
     * @param addTimeStamp if `true`, the timestamp is added if the base folder already exists.
     * @return the name of the internal base folder or `null`.
     * @throws IOException if something goes wrong.
     */
    @Throws(IOException::class)
    fun unzipFolder(zipFile: String?, destFolder: String, addTimeStamp: Boolean): String? {
        val dateTimeFormatter = SimpleDateFormat("yyyyMMddHHmmss") //$NON-NLS-1$
        val zf = ZipFile(zipFile)
        val zipEnum = zf.entries()
        var firstName: String? = null
        var newFirstName: String? = null
        while (zipEnum.hasMoreElements()) {
            val item = zipEnum.nextElement() as ZipEntry
            var itemName = item.name
            if (firstName == null) {
                val firstSlash = itemName.indexOf('/')
                if (firstSlash != -1) {
                    firstName = itemName.substring(0, firstSlash)
                    newFirstName = firstName
                    val baseFile = File(destFolder + File.separator + firstName)
                    if (baseFile.exists()) {
                        newFirstName = if (addTimeStamp) {
                            firstName + "_" + dateTimeFormatter.format(Date()) //$NON-NLS-1$
                        } else {
                            throw IOException(CompressionUtilities.FILE_EXISTS + baseFile) //$NON-NLS-1$
                        }
                    }
                }
            }
            itemName = itemName.replaceFirst(firstName!!.toRegex(), newFirstName!!)
            if (item.isDirectory) {
                val newdir = File(destFolder + File.separator + itemName)
                if (!newdir.mkdir()) throw IOException()
            } else {
                val newfilePath = destFolder + File.separator + itemName
                val newFile = File(newfilePath)
                val parentFile = newFile.parentFile
                if (!parentFile.exists()) {
                    if (!parentFile.mkdirs()) throw IOException()
                }
                val `is` = zf.getInputStream(item)
                val fos = FileOutputStream(newfilePath)
                val buffer = ByteArray(512)
                var readchars = 0
                while (`is`.read(buffer).also { readchars = it } != -1) {
                    fos.write(buffer, 0, readchars)
                }
                `is`.close()
                fos.close()
            }
        }
        zf.close()
        return newFirstName
    }

    @Throws(IOException::class)
    private fun addToZip(
        path: String,
        srcFile: String,
        zip: ZipOutputStream,
        vararg excludeNames: String
    ) {
        val file = File(srcFile)
        if (file.isDirectory) {
            CompressionUtilities.addFolderToZip(path, srcFile, zip, *excludeNames)
        } else {
            if (CompressionUtilities.isInArray(file.name, excludeNames as Array<String>)) {
                // jump if excluded
                return
            }
            val buf = ByteArray(1024)
            var len: Int
            var `in`: FileInputStream? = null
            try {
                `in` = FileInputStream(srcFile)
                zip.putNextEntry(ZipEntry(path + File.separator + file.name))
                while (`in`.read(buf).also { len = it } > 0) {
                    zip.write(buf, 0, len)
                }
            } finally {
                `in`?.close()
            }
        }
    }

    @Throws(IOException::class)
    private fun addFolderToZip(
        path: String,
        srcFolder: String,
        zip: ZipOutputStream,
        vararg excludeNames: String
    ) {
        if (CompressionUtilities.isInArray(srcFolder, excludeNames as Array<String>)) {
            // jump folder if excluded
            return
        }
        val folder = File(srcFolder)
        val listOfFiles = folder.list()
        for (i in listOfFiles.indices) {
            if (CompressionUtilities.isInArray(listOfFiles[i], excludeNames)) {
                // jump if excluded
                continue
            }
            var folderPath: String? = null
            folderPath = if (path.length < 1) {
                folder.name
            } else {
                path + File.separator + folder.name
            }
            val srcFile = srcFolder + File.separator + listOfFiles[i]
            CompressionUtilities.addToZip(folderPath!!, srcFile, zip, *excludeNames)
        }
    }

    private fun isInArray(checkString: String, array: Array<String>): Boolean {
        for (arrayString in array) {
            if (arrayString.trim { it <= ' ' } == checkString.trim { it <= ' ' }) {
                return true
            }
        }
        return false
    }

    /**
     * Create zip from files.
     *
     * @param destinationZip zip file.
     * @param files          array of files to add.
     * @throws IOException if something goes wrong.
     */
    @Throws(IOException::class)
    fun createZipFromFiles(destinationZip: File?, vararg files: File) {
        val fos = FileOutputStream(destinationZip)
        val zos = ZipOutputStream(fos)
        var bytesRead: Int
        val buffer = ByteArray(1024)
        val crc = CRC32()
        var i = 0
        val n = files.size
        while (i < n) {
            val name = files[i].name
            val file = files[i]
            if (!file.exists()) {
                Log.d("COMPRESSIONUTILITIES", "Skipping: $name")
                i++
                continue
            }
            var bis = BufferedInputStream(FileInputStream(file))
            crc.reset()
            while (bis.read(buffer).also { bytesRead = it } != -1) {
                crc.update(buffer, 0, bytesRead)
            }
            bis.close()
            // Reset to beginning of input stream
            bis = BufferedInputStream(FileInputStream(file))
            val entry = ZipEntry(name)
            entry.method = ZipEntry.STORED
            entry.compressedSize = file.length()
            entry.size = file.length()
            entry.crc = crc.value
            zos.putNextEntry(entry)
            while (bis.read(buffer).also { bytesRead = it } != -1) {
                zos.write(buffer, 0, bytesRead)
            }
            bis.close()
            i++
        }
        zos.close()
    }
}