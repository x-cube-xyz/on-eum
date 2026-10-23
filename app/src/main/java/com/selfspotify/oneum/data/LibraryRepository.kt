package com.selfspotify.oneum.data

import android.content.Context
import android.os.Environment
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

class LibraryRepository(context: Context) {
    val mediaDirectory = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir, "Oneum").apply { mkdirs() }
    private val index = AtomicFile(File(context.filesDir, "library.json"))
    private val mutex = Mutex()
    private val mutableTracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks = mutableTracks.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!index.baseFile.exists()) return@withLock
            val items = JSONArray(index.openRead().bufferedReader().use { it.readText() })
            mutableTracks.value = (0 until items.length()).map { Track.fromJson(items.getJSONObject(it)) }
                .filter { File(it.audioPath).isFile }.sortedByDescending { it.addedAt }
        }
    }

    suspend fun upsert(track: Track) = mutate { previous ->
        val existing = previous.find { it.id == track.id }
        (previous.filterNot { it.id == track.id } + track.copy(
            favorite = existing?.favorite ?: track.favorite,
            lastPlayedAt = existing?.lastPlayedAt ?: track.lastPlayedAt,
            addedAt = existing?.addedAt ?: track.addedAt,
        )).sortedByDescending { it.addedAt }
    }

    suspend fun toggleFavorite(id: String) = mutate { tracks -> tracks.map { if (it.id == id) it.copy(favorite = !it.favorite) else it } }
    suspend fun markPlayed(id: String) = mutate { tracks -> tracks.map { if (it.id == id) it.copy(lastPlayedAt = System.currentTimeMillis()) else it } }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        mutate { tracks -> tracks.filterNot { it.id == id } }
        val directory = File(mediaDirectory, id)
        if (directory.canonicalFile.parentFile == mediaDirectory.canonicalFile) directory.deleteRecursively()
    }

    private suspend fun mutate(transform: (List<Track>) -> List<Track>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val next = transform(mutableTracks.value)
            val json = JSONArray().apply { next.forEach { put(it.toJson()) } }.toString()
            val stream = index.startWrite()
            try {
                stream.write(json.toByteArray(Charsets.UTF_8))
                index.finishWrite(stream)
                mutableTracks.value = next
            } catch (error: Exception) {
                index.failWrite(stream)
                throw error
            }
        }
    }
}
