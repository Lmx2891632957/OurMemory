package com.example.myapplication

import android.app.Application
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.PhotoStore
import com.example.myapplication.data.PostRepository
import com.example.myapplication.data.location.AmapPrivacy
import com.example.myapplication.data.location.LocationClient
import com.example.myapplication.data.location.PlaceResolver
import java.io.File

/** 极简手写依赖容器：整个 App 只有一个实例，挂在 Application 上。 */
class AppContainer(application: Application) {

    private val database = AppDatabase.create(application)

    val photoStore = PhotoStore(
        draftDir = File(application.cacheDir, "drafts"),
        photoDir = File(application.filesDir, "photos"),
    )

    val postRepository = PostRepository(database.postDao(), photoStore)
    val locationClient = LocationClient(application)
    val placeResolver = PlaceResolver(application)

    init {
        // 上次异常退出残留的草稿照片，启动时清掉
        photoStore.cleanupStaleDrafts()
    }
}

class MyApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 已同意过隐私条款的用户：启动时补一次高德合规声明（幂等）
        if (AmapPrivacy.isAgreed(this)) {
            AmapPrivacy.applyCompliance(this)
        }
    }
}
