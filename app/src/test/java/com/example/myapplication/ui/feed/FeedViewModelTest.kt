package com.example.myapplication.ui.feed

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.FakePostDao
import com.example.myapplication.data.PhotoStore
import com.example.myapplication.data.Post
import com.example.myapplication.data.PostRepository
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
        val repository = PostRepository(
            dao = dao,
            photoStore = PhotoStore(temporaryFolder.newFolder(), temporaryFolder.newFolder()),
            ioDispatcher = dispatcher,
        )
        return FeedViewModel(repository).also { createdViewModels += it }
    }

    @Test
    fun `点赞会取反并写回数据库`() = runTest(dispatcher) {
        val dao = FakePostDao()
        val viewModel = newViewModel(dao)
        val post = Post(photoPath = "a.jpg", createdAt = 1L)
        dao.items.value = listOf(post)

        viewModel.toggleLike(post)
        advanceUntilIdle()

        assertTrue(dao.items.value.single().liked)
    }

    @Test
    fun `删除会同时移除列表项`() = runTest(dispatcher) {
        val dao = FakePostDao()
        val viewModel = newViewModel(dao)
        val post = Post(id = 7L, photoPath = "a.jpg", createdAt = 1L)
        dao.items.value = listOf(post)

        viewModel.delete(post)
        advanceUntilIdle()

        assertEquals(0, dao.items.value.size)
        assertEquals(7L, dao.deleted.single().id)
    }
}
