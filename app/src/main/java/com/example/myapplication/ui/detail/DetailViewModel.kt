package com.example.myapplication.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.PostRepository
import com.example.myapplication.data.PostWithPhotos
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object Missing : DetailUiState
    data class Content(val item: PostWithPhotos) : DetailUiState
}

/**
 * 详情页只读：展示记录、点赞、删除；不提供编辑。
 * 记录被删除后 DAO 的 Flow 会发射 null，页面跟着变成 Missing。
 */
class DetailViewModel(
    private val postId: Long,
    private val repository: PostRepository,
) : ViewModel() {

    var uiState by mutableStateOf<DetailUiState>(DetailUiState.Loading)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            repository.observePost(postId).collect { item ->
                uiState = if (item == null) DetailUiState.Missing else DetailUiState.Content(item)
            }
        }
    }

    fun toggleLike() {
        val item = (uiState as? DetailUiState.Content)?.item ?: return
        viewModelScope.launch { repository.setLiked(item.post.id, !item.post.liked) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.delete(postId) }
                .onSuccess { onDeleted() }
                .onFailure { message = "删除失败：${it.message}" }
        }
    }

    fun consumeMessage() {
        message = null
    }

    companion object {
        fun factory(postId: Long, repository: PostRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DetailViewModel(postId, repository) as T
            }
    }
}
