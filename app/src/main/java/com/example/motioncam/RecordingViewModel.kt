package com.example.motioncam

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File

/**
 * DashCam Recording ViewModel - Manages all dashcam functionality
 *
 * Features:
 * - Real video recording with CameraX
 * - GPS speed tracking
 * - Auto-start on movement
 * - Loop recording management
 * - File storage and organization
 * - Recording duration tracking
 * - Battery monitoring
 */
class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "RecordingViewModel"
        private const val AUTO_START_ENABLED = true // Feature flag
        private const val AUTO_STOP_ENABLED = false // Feature flag
        private const val MIN_SPEED_FOR_RECORDING = 5 // Minimum speed to consider moving
        private const val STOP_DELAY_MS = 30000 // 30 seconds before auto-stop
    }

    // Context
    private val context: Context get() = getApplication()

    // Camera manager
    private val cameraManager: CameraXDashCamManager by lazy {
        CameraXDashCamManager.getInstance(context)
    }

    // GPS Speed tracker
    private val gpsTracker: GPSSpeedTracker by lazy {
        GPSSpeedTracker(context)
    }

    // Video file manager
    private val videoFileManager: VideoFileManager by lazy {
        VideoFileManager(context)
    }

    // Recording state
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // Recording duration in seconds
    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    // Recording duration formatted as String (HH:MM:SS)
    val recordingDurationFormatted: String
        get() {
            val hours = _recordingDuration.value / 3600
            val minutes = (_recordingDuration.value % 3600) / 60
            val seconds = _recordingDuration.value % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

    // Current speed from GPS
    private val _currentSpeed = MutableStateFlow(0)
    val currentSpeed: StateFlow<Int> = _currentSpeed.asStateFlow()

    // GPS Active state
    private val _isGpsActive = MutableStateFlow(false)
    val isGpsActive: StateFlow<Boolean> = _isGpsActive.asStateFlow()

    // Is car moving
    private val _isMoving = MutableStateFlow(false)
    val isMoving: StateFlow<Boolean> = _isMoving.asStateFlow()

    // Camera initialized state
    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    // Recording error
    private val _recordingError = MutableStateFlow<String?>(null)
    val recordingError: StateFlow<String?> = _recordingError.asStateFlow()

    // Recording lock state (protect video from deletion)
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // Current video file
    private val _currentVideoFile = MutableStateFlow<File?>(null)
    val currentVideoFile: StateFlow<File?> = _currentVideoFile.asStateFlow()

    // Trip distance in miles
    private val _tripDistance = MutableStateFlow(0.0)
    val tripDistance: StateFlow<Double> = _tripDistance.asStateFlow()

    // Max speed during recording
    private val _maxSpeed = MutableStateFlow(0)
    val maxSpeed: StateFlow<Int> = _maxSpeed.asStateFlow()

    // Battery level (placeholder - can be enhanced with BatteryManager)
    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    // Storage info
    val storageInfo: StateFlow<StorageInfo> = videoFileManager.storageUsage

    // Video list
    val videoList: StateFlow<List<DashCamVideo>> = videoFileManager.videoList

    // Recording timer job
    private var recordingTimerJob: Job? = null

    // Auto-stop job
    private var autoStopJob: Job? = null

    // Camera preview view
    var previewView: PreviewView? = null

    init {
        // Initialize callbacks
        setupCameraCallbacks()

        // Start GPS tracking
        if (gpsTracker.hasLocationPermission()) {
            gpsTracker.startTracking()
        }

        // Collect GPS updates
        viewModelScope.launch {
            combine(
                gpsTracker.currentSpeed,
                gpsTracker.isGpsActive,
                gpsTracker.isMoving,
                gpsTracker.tripDistance,
                gpsTracker.maxSpeed
            ) { speed, gpsActive, moving, distance, maxSpd ->
                _currentSpeed.value = speed
                _isGpsActive.value = gpsActive
                _isMoving.value = moving
                _tripDistance.value = distance
                _maxSpeed.value = maxSpd
            }.collect {}
        }

        // Auto-start recording when car starts moving
        viewModelScope.launch {
            gpsTracker.getMovementFlow().collect { isMoving ->
                if (AUTO_START_ENABLED && isMoving && !_isRecording.value && _isCameraReady.value) {
                    Log.d(TAG, "Auto-starting recording - car is moving")
                    startRecording()
                }

                if (AUTO_STOP_ENABLED && !isMoving && _isRecording.value) {
                    // Delay auto-stop
                    autoStopJob?.cancel()
                    autoStopJob = viewModelScope.launch {
                        delay(STOP_DELAY_MS.toLong())
                        if (!gpsTracker.isMoving.value) {
                            Log.d(TAG, "Auto-stopping recording - car stopped")
                            stopRecording()
                        }
                    }
                } else {
                    autoStopJob?.cancel()
                }
            }
        }

        // Refresh storage info
        videoFileManager.refreshStorageInfo()
    }

    /**
     * Setup camera callbacks
     */
    private fun setupCameraCallbacks() {
        cameraManager.onRecordingStarted = {
            Log.d(TAG, "Recording started callback")
            _isRecording.value = true
            startRecordingTimer()
        }

        cameraManager.onRecordingStopped = { file ->
            Log.d(TAG, "Recording stopped callback: ${file.absolutePath}")
            _isRecording.value = false
            _currentVideoFile.value = file
            stopRecordingTimer()

            // Refresh video list
            videoFileManager.refreshVideoList()

            // Lock file if needed
            if (_isLocked.value) {
                val video = videoFileManager.videoList.value.find { it.filePath == file.absolutePath }
                video?.let { videoFileManager.lockVideo(it) }
            }
        }

        cameraManager.onRecordingError = { error ->
            Log.e(TAG, "Recording error: $error")
            _recordingError.value = error
            _isRecording.value = false
            stopRecordingTimer()
        }
    }

    /**
     * Initialize camera
     */
    fun initializeCamera(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            try {
                val previewView = previewView ?: run {
                    Log.e(TAG, "Preview view not set")
                    return@launch
                }

                val success = cameraManager.initializeCamera(lifecycleOwner, previewView.surfaceProvider)
                _isCameraReady.value = success

                if (success) {
                    Log.d(TAG, "Camera initialized successfully")
                } else {
                    _recordingError.value = "Failed to initialize camera"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization error", e)
                _recordingError.value = "Camera error: ${e.message}"
                _isCameraReady.value = false
            }
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

        if (!_isCameraReady.value) {
            _recordingError.value = "Camera not ready"
            return false
        }

        // Reset trip stats
        gpsTracker.resetTrip()
        _maxSpeed.value = 0
        _recordingDuration.value = 0
        _recordingError.value = null

        val success = cameraManager.startRecording()
        return success
    }

    /**
     * Stop video recording
     */
    fun stopRecording(): Boolean {
        return cameraManager.stopRecording()
    }

    /**
     * Toggle recording state
     */
    fun toggleRecording(): Boolean {
        return if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    /**
     * Toggle lock state for current clip
     */
    fun toggleLock() {
        _isLocked.value = !_isLocked.value
        Log.d(TAG, "Lock state: ${_isLocked.value}")
    }

    /**
     * Toggle between front and back camera
     */
    fun toggleCamera(lifecycleOwner: LifecycleOwner) {
        cameraManager.toggleCamera(lifecycleOwner)
    }

    /**
     * Start recording duration timer
     */
    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                _recordingDuration.value += 1
            }
        }
    }

    /**
     * Stop recording duration timer
     */
    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
    }

    /**
     * Delete a video file
     */
    fun deleteVideo(video: DashCamVideo) {
        videoFileManager.deleteVideo(video)
    }

    /**
     * Lock a video file (protect from deletion)
     */
    fun lockVideo(video: DashCamVideo) {
        videoFileManager.lockVideo(video)
    }

    /**
     * Refresh video list
     */
    fun refreshVideos() {
        videoFileManager.refreshVideoList()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopRecording()
        cameraManager.shutdown()
        gpsTracker.cleanup()
        recordingTimerJob?.cancel()
        autoStopJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
