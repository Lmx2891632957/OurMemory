package com.example.myapplication.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.PostRepository
import com.example.myapplication.data.PostWithPhotos
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(private val repository: PostRepository) : ViewModel() {

    val posts: StateFlow<List<PostWithPhotos>> = repository.observePosts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun toggleLike(item: PostWithPhotos) {
        viewModelScope.launch { repository.setLiked(item.post.id, !item.post.liked) }
    }

    fun delete(item: PostWithPhotos) {
        viewModelScope.launch { repository.delete(item.post.id) }
    }

    companion object {
        fun factory(repository: PostRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    FeedViewModel(repository) as T
            }
    }
}
