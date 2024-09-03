package app.simple.inure.apk.parsers

import android.content.Context
import android.content.pm.ApplicationInfo
import app.simple.inure.R
import app.simple.inure.constants.Extensions.isExtrasFile
import app.simple.inure.constants.Extensions.isImageFile
import app.simple.inure.exceptions.ApkParserException
import app.simple.inure.exceptions.DexClassesNotFoundException
import app.simple.inure.models.Extra
import app.simple.inure.models.Graphic
import net.dongliu.apk.parser.ApkFile
import net.dongliu.apk.parser.bean.ApkMeta
import net.dongliu.apk.parser.bean.DexClass
import java.io.File
import java.io.IOException
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@Suppress("ConstPropertyName")
object APKParser {

    private const val ARMEABI = "armeabi"
    private const val ARM64 = "arm64-v8a"
    private const val ARMv7 = "armeabi-v7a"
    private const val MIPS = "mips"
    private const val x86 = "x86"
    private const val x86_64 = "x86_64"

    /**
     * Fetch the install location of an APK file
     */
    fun File.getGlEsVersion(): String {
        kotlin.runCatching {
            ApkFile(this).use {
                return it.apkMeta.glEsVersion.toString()
            }
        }.getOrElse {
            throw ApkParserException("Couldn't fetch GLES version due to error : ${it.message}")
        }
    }

    fun File.getMinSDK(): String {
        kotlin.runCatching {
            ApkFile(this).use {
                return it.apkMeta.minSdkVersion.toString()
            }
        }.getOrElse {
            throw ApkParserException("Couldn't fetch min SDK version due to error : ${it.message}")
        }
    }

    fun File.getNativeLibraries(context: Context): StringBuilder {
        val stringBuilder = StringBuilder()
        var zipFile: ZipFile? = null
        try {
            zipFile = ZipFile(path)
            val entries: Enumeration<out ZipEntry?> = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry: ZipEntry? = entries.nextElement()
                val name: String = entry!!.name
                if (name.contains("lib") || name.contains("libs")) {
                    if (name.endsWith(".so")) {
                        if (stringBuilder.isNotEmpty()) {
                            stringBuilder.append("\n")
                        }

                        stringBuilder.append(name)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            if (stringBuilder.isBlank()) {
                stringBuilder.append(context.getString(R.string.error))
            }
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        if (stringBuilder.isBlank()) {
            stringBuilder.append(context.getString(R.string.none))
        }

        return stringBuilder
    }

    fun File.getApkArchitecture(context: Context): StringBuilder {
        var zipFile: ZipFile? = null
        val stringBuilder = StringBuilder()

        try {
            zipFile = ZipFile(path)
            val entries: Enumeration<out ZipEntry?> = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry: ZipEntry? = entries.nextElement()
                val name: String = entry!!.name

                if (name.contains("lib")) {
                    if (name.contains(ARMEABI)) {
                        if (!stringBuilder.contains(ARMEABI)) {
                            if (stringBuilder.isNotBlank()) {
                                stringBuilder.append(" | ")
                            }

                            stringBuilder.append(ARMEABI)
                            stringBuilder.append(" “generic” 32-bit ARM")
                        }
                    }

                    if (name.contains(ARM64)) {
                        if (!stringBuilder.contains(ARM64)) {
                            if (stringBuilder.isNotBlank()) {
                                stringBuilder.append(" | ")
                            }

                            stringBuilder.append(ARM64)
                        }
                    }

                    if (name.contains(ARMv7)) {
                        if (!stringBuilder.contains(ARMv7)) {
                            if (stringBuilder.isNotBlank()) {
                                stringBuilder.append(" | ")
                            }

                            stringBuilder.append(ARMv7)
                        }
                    }

                    if (name.contains(MIPS)) {
                        if (!stringBuilder.contains(MIPS)) {
                            if (stringBuilder.isNotBlank()) {
                                stringBuilder.append(" | ")
                            }

                            stringBuilder.append(MIPS)
                        }
                    }

                    if (name.contains(x86)) {
                        if (!stringBuilder.contains(x86)) {
                            if (stringBuilder.isNotBlank()) {
                                stringBuilder.append(" | ")
                            }

                            stringBuilder.append(x86)
                        }
                    }

                    if (name.contains(x86_64)) {
                        if (!stringBuilder.contains(x86_64)) {
                            if (stringBuilder.isNotBlank()) {
                                stringBuilder.append(" | ")
                            }

                            stringBuilder.append(x86_64)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            if (stringBuilder.isBlank()) {
                stringBuilder.append(context.getString(R.string.error))
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
            if (stringBuilder.isBlank()) {
                stringBuilder.append(context.getString(R.string.not_available))
            }
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        if (stringBuilder.isBlank()) {
            stringBuilder.append(context.getString(R.string.unspecified))
        }

        return stringBuilder
    }

    /**
     * Fetch the list of broadcast receivers from
     * an APK file
     */
    fun ApplicationInfo.getApkMeta(): ApkMeta {
        kotlin.runCatching {
            ApkFile(this.sourceDir).use {
                return it.apkMeta
            }
        }.getOrElse {
            throw ApkParserException("Couldn't parse app info due to error : ${it.message}")
        }
    }

    /**
     * Fetch APK's dex data
     */
    fun File.getDexData(): Array<DexClass> {
        kotlin.runCatching {
            ApkFile(this).use {
                return it.dexClasses
            }
        }.getOrElse {
            throw DexClassesNotFoundException("This apk does not contain any recognizable dex classes data.")
        }
    }

    /**
     * Get list of all xml files within an APK file
     */
    fun getXmlFiles(path: String?, keyword: String, ignoreCase: Boolean = true): MutableList<String> {
        val xmlFiles: MutableList<String> = ArrayList()
        var zipFile: ZipFile? = null
        try {
            zipFile = ZipFile(path)
            val entries: Enumeration<out ZipEntry?> = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry: ZipEntry? = entries.nextElement()
                val name: String = entry!!.name
                if (name.lowercase().endsWith(".xml") && name != "AndroidManifest.xml") {
                    if (name.contains(keyword, ignoreCase)) {
                        xmlFiles.add(name)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        xmlFiles.sortBy {
            it.lowercase()
        }
        return xmlFiles
    }

    /**
     * Get list of all raster image files within an APK file
     */
    fun getGraphicsFiles(path: String?, keyword: String): MutableList<Graphic> {

        val graphicsFiles: MutableList<Graphic> = ArrayList()
        var zipFile: ZipFile? = null
        try {
            zipFile = ZipFile(path)
            val entries: Enumeration<out ZipEntry?> = zipFile.entries()
            while (entries.hasMoreElements()) {

                val entry: ZipEntry? = entries.nextElement()
                val entryPath: String = entry!!.name // is a path
                val graphic = Graphic()

                when {
                    keyword.lowercase().startsWith("$") -> {
                        if (entryPath.lowercase().endsWith(keyword.lowercase().replace("$", ""))) {
                            if (entryPath.lowercase().isImageFile()) {
                                graphic.path = entryPath
                                graphic.name = entryPath.substringAfterLast("/")
                                graphic.size = entry.size
                                graphicsFiles.add(graphic)
                            }
                        }
                    }
                    keyword.lowercase().endsWith("$") -> {
                        if (entryPath.lowercase().startsWith(keyword.lowercase().replace("$", ""))) {
                            if (entryPath.lowercase().isImageFile()) {
                                graphic.path = entryPath
                                graphic.name = entryPath.substringAfterLast("/")
                                graphic.size = entry.size
                                graphicsFiles.add(graphic)
                            }
                        }
                    }
                    else -> {
                        if (entryPath.lowercase().contains(keyword.lowercase())) {
                            if (entryPath.lowercase().isImageFile()) {
                                graphic.path = entryPath
                                graphic.name = entryPath.substringAfterLast("/")
                                graphic.size = entry.size
                                graphicsFiles.add(graphic)
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close()
                } catch (ignored: IOException) {
                }
            }
        }

        return graphicsFiles
    }

    /**
     * Get list of all raster image files within an APK file
     */
    fun getExtraFiles(path: String?, keyword: String): MutableList<Extra> {

        val extraFiles: MutableList<Extra> = ArrayList()
        var zipFile: ZipFile? = null

        try {
            zipFile = ZipFile(path)
            val entries: Enumeration<out ZipEntry?> = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry: ZipEntry? = entries.nextElement()
                val name: String = entry!!.name // is a path
                val extra = Extra()

                if (keyword.lowercase().startsWith("$")) {
                    if (name.lowercase().endsWith(keyword.lowercase().replace("$", ""))) {
                        if (name.isExtrasFile()) {
                            extra.path = name
                            extra.name = name.substringAfterLast("/")
                            extra.size = entry.size
                            extraFiles.add(extra)
                        }
                    }
                } else if (keyword.lowercase().endsWith("$")) {
                    if (name.lowercase().startsWith(keyword.lowercase().replace("$", ""))) {
                        if (name.isExtrasFile()) {
                            extra.path = name
                            extra.name = name.substringAfterLast("/")
                            extra.size = entry.size
                            extraFiles.add(extra)
                        }
                    }
                } else {
                    if (name.lowercase().contains(keyword.lowercase())) {
                        if (name.isExtrasFile()) {
                            extra.path = name
                            extra.name = name.substringAfterLast("/")
                            extra.size = entry.size
                            extraFiles.add(extra)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            kotlin.runCatching {
                zipFile?.close()
            }
        }

        return extraFiles
    }
}
