package com.example.myapplication.ui.detail

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.FakePostDao
import com.example.myapplication.data.LocalPostRepository
import com.example.myapplication.data.PhotoStore
import com.example.myapplication.data.Post
import com.example.myapplication.data.PostPhoto
import com.example.myapplication.data.PostWithPhotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dispatcher: TestDispatcher
    private val createdViewModels = mutableListOf<DetailViewModel>()

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        createdViewModels.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun newViewModel(dao: FakePostDao, postId: Long = 1L): DetailViewModel {
        val repository = LocalPostRepository(
            dao = dao,
            photoStore = PhotoStore(temporaryFolder.newFolder(), temporaryFolder.newFolder()),
            ioDispatcher = dispatcher,
        )
        return DetailViewModel(postId, repository).also { createdViewModels += it }
    }

    private fun seedPost(dao: FakePostDao): PostWithPhotos {
        val post = Post(id = 1L, photoPath = "a.jpg", caption = "详情", createdAt = 1L)
        val photos = listOf(
            PostPhoto(id = 1L, postId = 1L, fileName = "a.jpg", position = 0),
            PostPhoto(id = 2L, postId = 1L, fileName = "b.jpg", position = 1),
        )
        dao.posts.value = listOf(post)
        dao.photos.value = photos
        return PostWithPhotos(post, photos)
    }

    @Test
    fun `加载记录后进入内容态`() = runTest(dispatcher) {
        val dao = FakePostDao()
        seedPost(dao)

        val viewModel = newViewModel(dao)
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(state is DetailUiState.Content)
        assertEquals("详情", (state as DetailUiState.Content).item.post.caption)
    }

    @Test
    fun `记录不存在时进入空态`() = runTest(dispatcher) {
        val viewModel = newViewModel(FakePostDao(), postId = 404L)
        advanceUntilIdle()

        assertEquals(DetailUiState.Missing, viewModel.uiState)
    }

    @Test
    fun `点赞会取反并写回`() = runTest(dispatcher) {
        val dao = FakePostDao()
        seedPost(dao)
        val viewModel = newViewModel(dao)
        advanceUntilIdle()

        viewModel.toggleLike()
        advanceUntilIdle()

        assertTrue(dao.posts.value.single().liked)
    }

    @Test
    fun `删除后回调触发且状态变为空`() = runTest(dispatcher) {
        val dao = FakePostDao()
        seedPost(dao)
        val viewModel = newViewModel(dao)
        advanceUntilIdle()

        var deleted = false
        viewModel.delete { deleted = true }
        advanceUntilIdle()

        assertTrue(deleted)
        assertEquals(DetailUiState.Missing, viewModel.uiState)
        assertTrue(dao.posts.value.single().deletedAt != null)
    }
}
