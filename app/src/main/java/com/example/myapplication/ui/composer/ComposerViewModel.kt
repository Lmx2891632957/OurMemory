package com.example.myapplication.ui.composer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.PlaceSelection
import com.example.myapplication.data.PostRepository
import com.example.myapplication.data.location.LocationSource
import com.example.myapplication.data.location.PlaceLabeler
import kotlinx.coroutines.launch
import java.io.File

data class ComposerUiState(
    val photoPaths: List<String> = emptyList(),
    val caption: String = "",
    val place: PlaceSelection? = null,
    val locating: Boolean = false,
    val publishing: Boolean = false,
    val published: Boolean = false,
    val message: String? = null,
)

class ComposerViewModel(
    photoPath: String,
    private val repository: PostRepository,
    private val locationClient: LocationSource,
    private val placeResolver: PlaceLabeler,
) : ViewModel() {

    var uiState by mutableStateOf(
        ComposerUiState(photoPaths = listOf(photoPath).filter { it.isNotBlank() }),
    )
        private set

    /** 再拍一张：追加到草稿末尾，超过 9 张拒绝。 */
    fun addPhoto(path: String) {
        val state = uiState
        if (state.publishing || state.published) return
        if (path.isBlank() || path in state.photoPaths) return
        if (state.photoPaths.size >= MAX_PHOTOS) {
            uiState = state.copy(message = "最多只能放 $MAX_PHOTOS 张照片")
            return
        }
        uiState = state.copy(photoPaths = state.photoPaths + path)
    }

    /** 从草稿里移除一张，草稿文件同时删掉。 */
    fun removePhoto(path: String) {
        val state = uiState
        if (state.publishing || state.published) return
        if (path !in state.photoPaths) return
        File(path).delete()
        uiState = state.copy(photoPaths = state.photoPaths - path)
    }

    /** 调整顺序：把第 [index] 张往前（-1）或往后（+1）挪一位。 */
    fun movePhoto(index: Int, delta: Int) {
        val state = uiState
        if (state.publishing || state.published) return
        val target = index + delta
        if (index !in state.photoPaths.indices || target !in state.photoPaths.indices) return
        val reordered = state.photoPaths.toMutableList().apply {
            add(target, removeAt(index))
        }
        uiState = state.copy(photoPaths = reordered)
    }

    fun updateCaption(value: String) {
        uiState = uiState.copy(caption = value)
    }

    fun setPlace(place: PlaceSelection) {
        uiState = uiState.copy(place = place, locating = false)
    }

    fun renamePlace(label: String) {
        val current = uiState.place ?: return
        if (label.isBlank()) return
        uiState = uiState.copy(place = current.copy(label = label.trim()))
    }

    /** 权限已授予时调用：定位 + 逆地理编码，任何一步失败都只是"没有地点"。 */
    fun detectLocation() {
        if (uiState.locating) return
        uiState = uiState.copy(locating = true)
        viewModelScope.launch {
            val coordinates = runCatching { locationClient.currentCoordinates() }.getOrNull()
            if (coordinates == null) {
                uiState = uiState.copy(locating = false, message = "没拿到定位，可以手动选择地点")
                return@launch
            }

            // 先立刻把坐标显示出来，再慢慢解析地址，避免用户对着"正在定位…"等十几秒
            val provisional = PlaceSelection(
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
                label = "%.5f, %.5f".format(coordinates.latitude, coordinates.longitude),
            )
            uiState = uiState.copy(locating = false, place = provisional)

            // 高德定位通常已经带回地址/POI，能省掉一次逆地理调用
            val label = coordinates.displayLabel() ?: runCatching {
                placeResolver.labelFor(coordinates.latitude, coordinates.longitude)
            }.getOrNull()
            if (label != null && uiState.place?.label == provisional.label) {
                uiState = uiState.copy(place = provisional.copy(label = label))
            }
        }
    }

    fun publish() {
        val state = uiState
        if (state.publishing || state.published) return
        if (state.photoPaths.isEmpty()) {
            uiState = state.copy(message = "先拍一张照片吧")
            return
        }
        val drafts = state.photoPaths.map(::File)
        if (drafts.any { !it.exists() }) {
            uiState = state.copy(message = "有照片丢了，回去重新拍一张吧")
            return
        }
        uiState = state.copy(publishing = true)
        viewModelScope.launch {
            runCatching { repository.create(drafts, state.caption, state.place) }
                .onSuccess {
                    uiState = uiState.copy(publishing = false, published = true)
                }
                .onFailure {
                    uiState = uiState.copy(publishing = false, message = "发布失败：${it.message}")
                }
        }
    }

    fun consumeMessage() {
        uiState = uiState.copy(message = null)
    }

    fun discardDraft() {
        if (uiState.published) return
        uiState.photoPaths.forEach { File(it).delete() }
    }

    companion object {
        const val MAX_PHOTOS = 9

        fun factory(
            photoPath: String,
            repository: PostRepository,
            locationClient: LocationSource,
            placeResolver: PlaceLabeler,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ComposerViewModel(photoPath, repository, locationClient, placeResolver) as T
        }
    }
}
