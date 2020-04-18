package com.example.ftpclient.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ftpclient.model.FtpConfig
import com.example.ftpclient.repo.FtpRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPFile
import timber.log.Timber
import java.io.File
import java.lang.StringBuilder
import java.util.*
import kotlin.coroutines.CoroutineContext

class FtpFilesViewModel : ViewModel(), CoroutineScope {
    companion object {
        const val ROOT = "/"
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    val repo = FtpRepo()
    val ftpConfig = FtpConfig()

    val needShowProgress = MutableLiveData<Boolean>()
    var currentDir = MutableLiveData<String>().apply { postValue("") }
    var currentPath = MutableLiveData<String>().apply { postValue(ROOT) }
    var dirPath: Queue<String> = LinkedList<String>()
    var currentDirFiles = MutableLiveData<List<FTPFile>>().apply { postValue(emptyList()) }

    fun setHostname(hostname: String) {
        ftpConfig.hostname = hostname
    }

    fun getHostname(): String = ftpConfig.hostname

    fun setPort(port: String) {
        ftpConfig.port = port.toIntOrNull()
    }

    fun getPort(): String = ftpConfig.port?.toString() ?: ""

    fun setUsername(username: String) {
        ftpConfig.username = username
    }

    fun getUsername(): String = ftpConfig.username

    fun setPassword(password: String) {
        ftpConfig.password = password
    }

    fun getPassword(): String = ftpConfig.password

    fun setNeedAuth(needAuth: Boolean) {
        ftpConfig.needAuth = needAuth
    }

    fun getNeedAuth(): Boolean = ftpConfig.needAuth

    fun updatePath() {
        val path = repo.getCurrentPath()
        currentPath.postValue(path.toString())
    }

    fun connect(onFinished: (()->Unit)? = null) {
        launch {
            withContext(Dispatchers.Main) {
                needShowProgress.postValue(true)
            }

            Timber.d("Connect to FTP")
            val port = ftpConfig.port
            if (port == null || port == 0) {
                repo.connect(ftpConfig.hostname)
            } else {
                repo.connect(ftpConfig.hostname, port)
            }
            Timber.d("FTP is connected = ${repo.isConnected}")
            if (repo.isConnected && ftpConfig.needAuth && ftpConfig.username.isNotEmpty()) {
                Timber.d("Login to FTP")
                repo.login(ftpConfig.username, ftpConfig.password)
            }

            withContext(Dispatchers.Main) {
                onFinished?.invoke()
                needShowProgress.postValue(false)
            }
        }
    }

    fun disconnect(onFinished: (()->Unit)? = null) {
        launch {
            withContext(Dispatchers.Main) {
                needShowProgress.postValue(true)
            }

            Timber.d("Disconnecting from FTP")
            repo.logout()
            repo.disconnect()

            withContext(Dispatchers.Main) {
                onFinished?.invoke()
                needShowProgress.postValue(false)
            }
        }
    }

    fun initDirectory(onFinished: (()->Unit)? = null) {
        launch {
            withContext(Dispatchers.Main) {
                needShowProgress.postValue(true)
            }

            Timber.d("Init directory")
            val files = repo.getFiles()
            val filesList = mutableListOf<FTPFile>()
            filesList.addAll(files)

            withContext(Dispatchers.Main) {
                onFinished?.invoke()
                currentDirFiles.postValue(filesList)
                needShowProgress.postValue(false)
            }
        }
    }

    fun goToDirectory(path: String, onFinished: (()->Unit)? = null) {
        launch {
            withContext(Dispatchers.Main) {
                needShowProgress.postValue(true)
            }

            Timber.d("Go to path")
            dirPath.offer(path)
            currentDir.postValue(path)
            repo.changeDir(path)
            initDirectory()
            updatePath()

            withContext(Dispatchers.Main) {
                onFinished?.invoke()
                needShowProgress.postValue(false)
            }
        }
    }

    fun goToParentDirectory(onFinished: (()->Unit)? = null) {
        launch {
            withContext(Dispatchers.Main) {
                needShowProgress.postValue(true)
            }

            currentDir.postValue(dirPath.poll())
            repo.changeDir("", true)
            initDirectory()
            updatePath()

            withContext(Dispatchers.Main) {
                onFinished?.invoke()
                needShowProgress.postValue(false)
            }
        }
    }

    fun deleteFile(path: String, onFinished: (()->Unit)? = null) {
        launch {
            withContext(Dispatchers.Main) {
                needShowProgress.postValue(true)
            }

            repo.deleteFile(path)
            initDirectory()
            updatePath()

            withContext(Dispatchers.Main) {
                onFinished?.invoke()
                needShowProgress.postValue(false)
            }
        }
    }

    fun download(destinationPath: File, file: FTPFile, onFinished: (()->Unit)? = null) {
        launch {
            withContext(Dispatchers.Main) {
                needShowProgress.postValue(true)
            }

            val newFile = File(destinationPath, file.name)
            repo.downloadFile(file, newFile.outputStream())


            withContext(Dispatchers.Main) {
                needShowProgress.postValue(false)
            }
        }
    }
}