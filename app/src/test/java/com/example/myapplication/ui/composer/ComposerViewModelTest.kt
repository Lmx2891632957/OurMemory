package com.example.myapplication.ui.composer

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.FakePostDao
import com.example.myapplication.data.LocalPostRepository
import com.example.myapplication.data.PhotoStore
import com.example.myapplication.data.PlaceSelection
import com.example.myapplication.data.location.Coordinates
import com.example.myapplication.data.location.LocationSource
import com.example.myapplication.data.location.PlaceLabeler
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ComposerViewModelTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dispatcher: TestDispatcher
    private lateinit var drafts: File
    private lateinit var photos: File
    private val createdViewModels = mutableListOf<ComposerViewModel>()

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        drafts = temporaryFolder.newFolder("drafts")
        photos = temporaryFolder.newFolder("photos")
    }

    @After
    fun tearDown() {
        createdViewModels.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private class FakeLocationSource(private val coordinates: Coordinates?) : LocationSource {
        override suspend fun currentCoordinates(timeoutMs: Long): Coordinates? = coordinates
    }

    private class FakeLabeler(private val label: String?) : PlaceLabeler {
        override suspend fun labelFor(latitude: Double, longitude: Double): String? = label
    }

    private fun draftPhoto(): File = File(drafts, "draft.jpg").apply { writeText("jpeg") }

    private fun newViewModel(
        draft: File = draftPhoto(),
        dao: FakePostDao = FakePostDao(),
        source: LocationSource = FakeLocationSource(Coordinates(39.9087, 116.3975)),
        labeler: PlaceLabeler = FakeLabeler("天安门"),
    ): ComposerViewModel {
        val viewModel = ComposerViewModel(
            photoPath = draft.absolutePath,
            repository = LocalPostRepository(dao, PhotoStore(drafts, photos), dispatcher),
            locationClient = source,
            placeResolver = labeler,
        )
        createdViewModels += viewModel
        return viewModel
    }

    @Test
    fun `定位成功时地点与地址都会填好`() = runTest(dispatcher) {
        val viewModel = newViewModel()

        viewModel.detectLocation()
        advanceUntilIdle()

        val place = viewModel.uiState.place
        assertEquals("天安门", place?.label)
        assertEquals(39.9087, place?.latitude ?: 0.0, 0.00001)
        assertFalse(viewModel.uiState.locating)
    }

    @Test
    fun `拿不到定位时给出提示且不阻塞发布`() = runTest(dispatcher) {
        val viewModel = newViewModel(source = FakeLocationSource(null))

        viewModel.detectLocation()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.place)
        assertEquals("没拿到定位，可以手动选择地点", viewModel.uiState.message)
    }

    @Test
    fun `逆地理编码失败时退化为坐标文本`() = runTest(dispatcher) {
        val viewModel = newViewModel(labeler = FakeLabeler(null))

        viewModel.detectLocation()
        advanceUntilIdle()

        assertEquals("39.90870, 116.39750", viewModel.uiState.place?.label)
    }

    @Test
    fun `发布成功后状态变为已发布`() = runTest(dispatcher) {
        val dao = FakePostDao()
        val viewModel = newViewModel(dao = dao)
        viewModel.updateCaption("  今天去公园  ")
        viewModel.setPlace(PlaceSelection(39.9, 116.4, "朝阳公园"))

        viewModel.publish()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.published)
        val stored = dao.posts.value.single()
        assertEquals("今天去公园", stored.caption)
        assertEquals("朝阳公园", stored.placeLabel)
        val photo = dao.photos.value.single()
        assertEquals(stored.id, photo.postId)
        assertTrue(File(photos, photo.fileName).exists())
    }

    @Test
    fun `照片文件丢失时发布失败并提示`() = runTest(dispatcher) {
        val viewModel = newViewModel(draft = File(drafts, "missing.jpg"))

        viewModel.publish()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.published)
        assertTrue(viewModel.uiState.message?.contains("照片") == true)
    }

    @Test
    fun `改名只改显示文本不改坐标`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        viewModel.detectLocation()
        advanceUntilIdle()

        viewModel.renamePlace("  秘密基地 ")

        assertEquals("秘密基地", viewModel.uiState.place?.label)
        assertEquals(39.9087, viewModel.uiState.place?.latitude ?: 0.0, 0.00001)
    }

    @Test
    fun `取消编辑会删掉草稿照片`() = runTest(dispatcher) {
        val draft = draftPhoto()
        val viewModel = newViewModel(draft = draft)

        viewModel.discardDraft()

        assertFalse(draft.exists())
    }
}
