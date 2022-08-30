package app.simple.inure.viewmodels.dialogs

import android.app.Application
import android.content.pm.PackageInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.R
import app.simple.inure.apk.utils.PackageData
import app.simple.inure.extensions.viewmodels.WrappedViewModel
import app.simple.inure.util.NullSafety.isNotNull
import app.simple.inure.util.PermissionUtils.areStoragePermissionsGranted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.*

class ExtractViewModel(application: Application, val packageInfo: PackageInfo) : WrappedViewModel(application) {

    private val apk = File(packageInfo.applicationInfo.sourceDir).parentFile

    private val progress: MutableLiveData<Long> = MutableLiveData<Long>()
    private val status: MutableLiveData<String> = MutableLiveData<String>()
    private val success: MutableLiveData<Boolean> = MutableLiveData(false)

    init {
        extractAppFile()
    }

    fun getProgress(): LiveData<Long> {
        return progress
    }

    fun getStatus(): LiveData<String> {
        return status
    }

    fun getError(): LiveData<String> {
        return error
    }

    fun getSuccess(): LiveData<Boolean> {
        return success
    }

    private fun extractAppFile() {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                if (context.areStoragePermissionsGranted()) {
                    PackageData.makePackageFolder(applicationContext())
                } else {
                    throw SecurityException("Storage Permission not granted")
                }

                if (packageInfo.applicationInfo.splitSourceDirs.isNotNull()) { // For split packages
                    extractBundle()
                } else { // For APK files
                    extractApk()
                }
            }.onFailure {
                it.printStackTrace()
                error.postValue(it.stackTraceToString())
            }.onSuccess {
                success.postValue(true)
            }
        }
    }

    private suspend fun extractBundle() {
        status.postValue(getString(R.string.split_apk_detected))

        val list = arrayListOf<File>()

        list.add(File(packageInfo.applicationInfo.sourceDir))

        for (i in packageInfo.applicationInfo.splitSourceDirs.indices) {
            list.add(File(packageInfo.applicationInfo.splitSourceDirs[i]))
        }

        status.postValue(getString(R.string.creating_split_package))

        kotlin.runCatching {
            val zipFile = ZipFile(getBundlePathAndFileName())
            val progressMonitor = zipFile.progressMonitor

            zipFile.addFiles(list)

            while (!progressMonitor.state.equals(ProgressMonitor.State.READY)) {
                progress.postValue(progressMonitor.percentDone.toLong())
                delay(10)
            }

            if (progressMonitor.result.equals(ProgressMonitor.Result.SUCCESS)) {
                success.postValue(true)
            } else if (progressMonitor.result.equals(ProgressMonitor.Result.ERROR)) {
                error.postValue(progressMonitor.exception.stackTraceToString())
            } else if (progressMonitor.result.equals(ProgressMonitor.Result.CANCELLED)) {
                status.postValue(getString(R.string.cancelled))
            }
        }.onFailure {
            it.printStackTrace()
            error.postValue(it.stackTraceToString())
        }
    }

    @Throws(IOException::class)
    private fun extractApk() {
        status.postValue(getString(R.string.preparing_apk_file))

        val source = File(packageInfo.applicationInfo.sourceDir)
        val dest = File(PackageData.getPackageDir(applicationContext()), getApkPathAndFileName())
        val length = source.length()

        val inputStream = FileInputStream(source)
        val outputStream = FileOutputStream(dest)

        copyStream(inputStream, outputStream, length)

        inputStream.close()
        outputStream.close()
    }

    @Throws(IOException::class)
    fun copyStream(from: InputStream, to: OutputStream, length: Long) {
        val buf = ByteArray(1024 * 1024)
        var len: Int
        var total = 0L
        while (from.read(buf).also { len = it } > 0) {
            to.write(buf, 0, len)
            total += len
            progress.postValue(total * 100 / length)
        }
    }

    private fun getBundlePathAndFileName(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(PackageData.getPackageDir(applicationContext()))
        stringBuilder.append("/")
        stringBuilder.append(packageInfo.applicationInfo.name)
        stringBuilder.append("_(${packageInfo.versionName})")
        stringBuilder.append(".apkm")
        return stringBuilder.toString()
    }

    private fun getApkPathAndFileName(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(packageInfo.applicationInfo.name)
        stringBuilder.append("_(${packageInfo.versionName})")
        stringBuilder.append(".apk")
        return stringBuilder.toString()
    }
}