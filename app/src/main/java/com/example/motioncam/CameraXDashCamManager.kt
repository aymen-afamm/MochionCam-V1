package com.example.motioncam

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX DashCam Manager - Handles all camera operations for dashcam recording
 *
 * Features:
 * - Video recording with CameraX VideoCapture
 * - Camera preview
 * - Front/Back camera switching
 * - Loop recording support
 * - Audio recording
 * - File management integration
 */
class CameraXDashCamManager private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "DashCamManager"
        private const val DEFAULT_VIDEO_DURATION_MIN = 5 // 5 minute segments
        private const val MAX_STORAGE_GB = 4 // Keep 4GB free space

        @Volatile
        private var instance: CameraXDashCamManager? = null

        fun getInstance(context: Context): CameraXDashCamManager {
            return instance ?: synchronized(this) {
                instance ?: CameraXDashCamManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    // Recording state
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // Recording error
    private val _recordingError = MutableStateFlow<String?>(null)
    val recordingError: StateFlow<String?> = _recordingError.asStateFlow()

    // Current recording info
    private val _currentFile = MutableStateFlow<File?>(null)
    val currentFile: StateFlow<File?> = _currentFile.asStateFlow()

    // Camera provider
    private var cameraProvider: ProcessCameraProvider? = null

    // Video capture use case
    private var videoCapture: VideoCapture<androidx.camera.video.Recorder>? = null

    // Preview use case
    private var preview: Preview? = null

    // Camera selector (front/back)
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // Active recording
    private var activeRecording: Recording? = null

    // Executor for camera operations
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Video file manager
    private val videoFileManager = VideoFileManager(context)

    // Callbacks
    var onRecordingStarted: (() -> Unit)? = null
    var onRecordingStopped: ((file: File) -> Unit)? = null
    var onRecordingError: ((error: String) -> Unit)? = null

    /**
     * Initialize the camera
     */
    suspend fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewSurface: androidx.camera.core.Preview.SurfaceProvider
    ): Boolean {
        return try {
            val provider = ProcessCameraProvider.getInstance(context).await()
            cameraProvider = provider

            // Build preview use case
            preview = Preview.Builder()
                .build()
                .apply {
                    setSurfaceProvider(previewSurface)
                }

            // Build video capture use case with high quality
            val recorder = androidx.camera.video.Recorder.Builder()
                .setExecutor(cameraExecutor)
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            // Try to bind camera
            bindCamera(lifecycleOwner)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed", e)
            _recordingError.value = "Camera initialization failed: ${e.message}"
            false
        }
    }

    /**
     * Bind camera use cases
     */
    private fun bindCamera(lifecycleOwner: LifecycleOwner) {
        val provider = cameraProvider ?: return
        val preview = preview ?: return
        val videoCapture = videoCapture ?: return

        try {
            // Unbind all use cases before rebinding
            provider.unbindAll()

            // Bind use cases to camera
            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
            _recordingError.value = "Camera binding failed: ${e.message}"
        }
    }

    /**
     * Start video recording
     */
    fun startRecording(): Boolean {
        if (_isRecording.value) {
            Log.w(TAG, "Already recording")
            return false
        }

        val videoCapture = videoCapture ?: run {
            _recordingError.value = "Camera not initialized"
            return false
        }

        // Clean up old files before recording (loop recording)
        videoFileManager.cleanOldFiles()

        // Create output file
        val outputFile = videoFileManager.createVideoFile()
        _currentFile.value = outputFile

        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        // Start recording
        activeRecording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled() // Enable audio recording
            .start(ContextCompat.getMainExecutor(context)) { event ->
                handleRecordingEvent(event, outputFile)
            }

        _isRecording.value = true
        _recordingError.value = null

        Log.d(TAG, "Recording started: ${outputFile.absolutePath}")
        onRecordingStarted?.invoke()

        return true
    }

    /**
     * Stop video recording
     */
    fun stopRecording(): Boolean {
        if (!_isRecording.value) {
            return false
        }

        activeRecording?.stop()
        activeRecording = null
        _isRecording.value = false

        return true
    }

    /**
     * Handle recording events
     */
    private fun handleRecordingEvent(event: VideoRecordEvent, outputFile: File) {
        when (event) {
            is VideoRecordEvent.Start -> {
                Log.d(TAG, "Recording event: Start")
            }
            is VideoRecordEvent.Pause -> {
                Log.d(TAG, "Recording event: Pause")
            }
            is VideoRecordEvent.Resume -> {
                Log.d(TAG, "Recording event: Resume")
            }
            is VideoRecordEvent.Status -> {
                // Recording in progress - can update duration here if needed
                val duration = event.recordingStats.recordedDurationNanos
            }
            is VideoRecordEvent.Finalize -> {
                if (event.error != VideoRecordEvent.Finalize.ERROR_NONE) {
                    Log.e(TAG, "Recording error: ${event.cause}")
                    _recordingError.value = "Recording failed: ${event.cause?.message}"
                    onRecordingError?.invoke("Recording failed: ${event.cause?.message}")
                } else {
                    Log.d(TAG, "Recording finalized: ${outputFile.absolutePath}")
                    _recordingError.value = null
                    onRecordingStopped?.invoke(outputFile)
                }
                _isRecording.value = false
                activeRecording = null
            }
        }
    }

    /**
     * Toggle between front and back camera
     */
    fun toggleCamera(lifecycleOwner: LifecycleOwner) {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        bindCamera(lifecycleOwner)
    }

    /**
     * Check if back camera is available
     */
    fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /**
     * Check if front camera is available
     */
    fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    /**
     * Release all camera resources
     */
    fun shutdown() {
        stopRecording()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }

    /**
     * Extension function to await camera provider future
     */
    private suspend fun com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider>.await(): ProcessCameraProvider {
        return kotlinx.coroutines.suspendCoroutine { continuation ->
            addListener({
                try {
                    continuation.resumeWith(Result.success(get()))
                } catch (e: Exception) {
                    continuation.resumeWith(Result.failure(e))
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
}
