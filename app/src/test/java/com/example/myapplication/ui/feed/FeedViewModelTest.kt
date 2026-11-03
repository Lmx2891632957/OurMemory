package com.example.myapplication.ui.feed

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dispatcher: TestDispatcher
    private val createdViewModels = mutableListOf<FeedViewModel>()

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

    private fun newViewModel(dao: FakePostDao): FeedViewModel {
        val repository = LocalPostRepository(
            dao = dao,
            photoStore = PhotoStore(temporaryFolder.newFolder(), temporaryFolder.newFolder()),
            ioDispatcher = dispatcher,
        )
        return FeedViewModel(repository).also { createdViewModels += it }
    }

    private fun item(post: Post): PostWithPhotos = PostWithPhotos(
        post = post,
        photos = listOf(PostPhoto(id = 1L, postId = post.id, fileName = "a.jpg", position = 0)),
    )

    @Test
    fun `点赞会取反并写回数据库`() = runTest(dispatcher) {
        val dao = FakePostDao()
        val viewModel = newViewModel(dao)
        val post = Post(id = 1L, photoPath = "a.jpg", createdAt = 1L)
        dao.posts.value = listOf(post)
        dao.photos.value = item(post).photos

        viewModel.toggleLike(item(post))
        advanceUntilIdle()

        assertTrue(dao.posts.value.single().liked)
    }

    @Test
    fun `删除会软删除记录并从信息流移除`() = runTest(dispatcher) {
        val dao = FakePostDao()
        val viewModel = newViewModel(dao)
        val post = Post(id = 7L, photoPath = "a.jpg", createdAt = 1L)
        dao.posts.value = listOf(post)
        dao.photos.value = item(post).photos

        viewModel.delete(item(post))
        advanceUntilIdle()

        assertNotNull(dao.posts.value.single().deletedAt)
        assertEquals(0, dao.observeAll().first().size)
    }
}
