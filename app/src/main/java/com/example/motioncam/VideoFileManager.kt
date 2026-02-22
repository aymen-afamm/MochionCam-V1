package com.example.motioncam

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Video File Manager - Handles video storage, organization, and loop recording
 *
 * Features:
 * - Creates organized video folders by date
 * - Manages storage space with automatic cleanup (loop recording)
 * - Generates video metadata
 * - Thumbnail generation
 * - Storage usage tracking
 */
class VideoFileManager(private val context: Context) {

    companion object {
        private const val TAG = "VideoFileManager"
        private const val MAX_STORAGE_GB = 4L // Maximum 4GB for video storage
        private const val MIN_FREE_SPACE_MB = 500 // Keep 500MB free on device
        private const val SEGMENT_DURATION_MIN = 5 // 5-minute video segments
    }

    // Base directory for dashcam videos
    private val baseDir: File by lazy {
        val dir = File(context.getExternalFilesDir(null), "DashCam/Videos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    // Locked (protected) videos directory
    private val lockedDir: File by lazy {
        val dir = File(baseDir, "Locked")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    // Storage usage state
    private val _storageUsage = MutableStateFlow(StorageInfo())
    val storageUsage: StateFlow<StorageInfo> = _storageUsage.asStateFlow()

    // Video list state
    private val _videoList = MutableStateFlow<List<DashCamVideo>>(emptyList())
    val videoList: StateFlow<List<DashCamVideo>> = _videoList.asStateFlow()

    init {
        refreshStorageInfo()
        refreshVideoList()
    }

    /**
     * Create a new video file with timestamp-based naming
     */
    fun createVideoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "DASH_${timestamp}.mp4"
        return File(getTodayDirectory(), fileName)
    }

    /**
     * Get today's directory for organizing videos by date
     */
    private fun getTodayDirectory(): File {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val dir = File(baseDir, dateStr)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Create a locked video file (protected from deletion)
     */
    fun createLockedVideoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "LOCKED_${timestamp}.mp4"
        return File(lockedDir, fileName)
    }

    /**
     * Lock a video file (move to protected folder)
     */
    fun lockVideo(video: DashCamVideo): Boolean {
        return try {
            val sourceFile = File(video.filePath)
            if (!sourceFile.exists()) return false

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val destFile = File(lockedDir, "LOCKED_${timestamp}.mp4")

            sourceFile.renameTo(destFile)
            refreshVideoList()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lock video", e)
            false
        }
    }

    /**
     * Delete a video file
     */
    fun deleteVideo(video: DashCamVideo): Boolean {
        return try {
            val file = File(video.filePath)
            if (file.exists()) {
                file.delete()
                refreshVideoList()
                refreshStorageInfo()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete video", e)
            false
        }
    }

    /**
     * Clean up old video files to maintain storage limits (Loop Recording)
     */
    fun cleanOldFiles() {
        try {
            // Check current storage usage
            val currentUsage = calculateStorageUsage()
            val maxBytes = MAX_STORAGE_GB * 1024 * 1024 * 1024

            if (currentUsage < maxBytes) return // Still have space

            // Get all regular (non-locked) videos sorted by date (oldest first)
            val regularVideos = getAllRegularVideos().sortedBy { it.lastModified() }

            var freedSpace = 0L
            val spaceToFree = currentUsage - (maxBytes * 0.8).toLong() // Free down to 80%

            for (video in regularVideos) {
                if (freedSpace >= spaceToFree) break

                val file = File(video.filePath)
                if (file.exists() && !video.isLocked) {
                    val fileSize = file.length()
                    if (file.delete()) {
                        freedSpace += fileSize
                        Log.d(TAG, "Deleted old video: ${video.filePath}")
                    }
                }
            }

            refreshStorageInfo()
            refreshVideoList()

            Log.d(TAG, "Loop recording cleanup freed ${freedSpace / 1024 / 1024}MB")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning old files", e)
        }
    }

    /**
     * Get all videos including regular and locked
     */
    fun getAllVideos(): List<DashCamVideo> {
        val videos = mutableListOf<DashCamVideo>()

        // Regular videos
        videos.addAll(getAllRegularVideos())

        // Locked videos
        videos.addAll(getAllLockedVideos())

        return videos.sortedByDescending { it.timestamp }
    }

    /**
     * Get all regular videos
     */
    private fun getAllRegularVideos(): List<DashCamVideo> {
        val videos = mutableListOf<DashCamVideo>()

        baseDir.listFiles()?.forEach { dateDir ->
            if (dateDir.isDirectory) {
                dateDir.listFiles()?.forEach { file ->
                    if (file.extension == "mp4") {
                        videos.add(createVideoFromFile(file, false))
                    }
                }
            }
        }

        return videos
    }

    /**
     * Get all locked videos
     */
    private fun getAllLockedVideos(): List<DashCamVideo> {
        return lockedDir.listFiles()?.filter { it.extension == "mp4" }?.map { file ->
            createVideoFromFile(file, true)
        } ?: emptyList()
    }

    /**
     * Create a DashCamVideo object from a file
     */
    private fun createVideoFromFile(file: File, isLocked: Boolean): DashCamVideo {
        val fileName = file.nameWithoutExtension
        val timestamp = parseTimestampFromFileName(fileName) ?: file.lastModified()

        return DashCamVideo(
            id = fileName,
            filePath = file.absolutePath,
            fileName = file.name,
            timestamp = timestamp,
            duration = 0, // Will be updated after recording
            fileSize = file.length(),
            isLocked = isLocked,
            thumbnailPath = null // Generated separately
        )
    }

    /**
     * Parse timestamp from video file name (DASH_yyyyMMdd_HHmmss format)
     */
    private fun parseTimestampFromFileName(fileName: String): Long? {
        return try {
            if (fileName.startsWith("DASH_")) {
                val dateStr = fileName.substring(5)
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                sdf.parse(dateStr)?.time
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculate total storage usage
     */
    private fun calculateStorageUsage(): Long {
        var totalSize = 0L

        baseDir.listFiles()?.forEach { dateDir ->
            if (dateDir.isDirectory) {
                dateDir.listFiles()?.forEach { file ->
                    if (file.extension == "mp4") {
                        totalSize += file.length()
                    }
                }
            }
        }

        lockedDir.listFiles()?.forEach { file ->
            if (file.extension == "mp4") {
                totalSize += file.length()
            }
        }

        return totalSize
    }

    /**
     * Refresh storage info
     */
    fun refreshStorageInfo() {
        val usedBytes = calculateStorageUsage()
        val maxBytes = MAX_STORAGE_GB * 1024 * 1024 * 1024
        val usedPercentage = (usedBytes.toFloat() / maxBytes * 100).toInt()

        _storageUsage.value = StorageInfo(
            usedBytes = usedBytes,
            maxBytes = maxBytes,
            usedPercentage = usedPercentage.coerceIn(0, 100),
            videoCount = getAllVideos().size
        )
    }

    /**
     * Refresh video list
     */
    fun refreshVideoList() {
        _videoList.value = getAllVideos()
    }

    /**
     * Get storage info as formatted strings
     */
    fun getStorageInfoFormatted(): StorageInfoFormatted {
        val info = _storageUsage.value
        return StorageInfoFormatted(
            usedSize = formatFileSize(info.usedBytes),
            maxSize = formatFileSize(info.maxBytes),
            freeSize = formatFileSize(info.maxBytes - info.usedBytes),
            usedPercentage = info.usedPercentage
        )
    }

    /**
     * Format file size to human readable
     */
    private fun formatFileSize(sizeBytes: Long): String {
        return when {
            sizeBytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0))
            sizeBytes >= 1024 * 1024 -> String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
            sizeBytes >= 1024 -> String.format("%.1f KB", sizeBytes / 1024.0)
            else -> "$sizeBytes B"
        }
    }

    /**
     * Check if we can record (enough storage space)
     */
    fun canRecord(): Boolean {
        val info = _storageUsage.value
        val freeSpace = info.maxBytes - info.usedBytes
        return freeSpace > MIN_FREE_SPACE_MB * 1024 * 1024
    }
}

/**
 * Data class representing a DashCam video
 */
data class DashCamVideo(
    val id: String,
    val filePath: String,
    val fileName: String,
    val timestamp: Long,
    val duration: Int, // seconds
    val fileSize: Long,
    val isLocked: Boolean,
    val thumbnailPath: String?
) {
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

    val formattedDuration: String
        get() {
            val minutes = duration / 60
            val seconds = duration % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val formattedSize: String
        get() = when {
            fileSize >= 1024 * 1024 * 1024 -> String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0))
            fileSize >= 1024 * 1024 -> String.format("%.1f MB", fileSize / (1024.0 * 1024.0))
            else -> String.format("%.1f KB", fileSize / 1024.0)
        }
}

/**
 * Storage information
 */
data class StorageInfo(
    val usedBytes: Long = 0,
    val maxBytes: Long = 4L * 1024 * 1024 * 1024, // 4GB default
    val usedPercentage: Int = 0,
    val videoCount: Int = 0
)

/**
 * Formatted storage information for UI display
 */
data class StorageInfoFormatted(
    val usedSize: String = "0 MB",
    val maxSize: String = "4 GB",
    val freeSize: String = "4 GB",
    val usedPercentage: Int = 0
)
