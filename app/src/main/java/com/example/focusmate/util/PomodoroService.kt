package com.example.focusmate.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.example.focusmate.R
import com.example.focusmate.data.repository.PomodoroRepository
import com.example.focusmate.ui.pomodoro.PomodoroActivity
import com.example.focusmate.ui.pomodoro.TimerState
import com.example.focusmate.util.PomodoroSoundPlayer
import com.example.focusmate.util.SoundEvent

class PomodoroService : Service() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var soundPlayer: PomodoroSoundPlayer
    private var wakeLock: PowerManager.WakeLock? = null

    private var currentState: TimerState = TimerState.IDLE
    private var currentTimeLeft: Int = 0
    private var lastSoundEvent: SoundEvent? = null

    companion object {
        const val CHANNEL_ID = "pomodoro_service_channel"
        const val NOTIFICATION_ID = 1
        const val PUSH_CHANNEL_ID = "pomodoro_push_channel"

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_START_BREAK = "ACTION_START_BREAK"
        const val ACTION_SKIP_BREAK = "ACTION_SKIP_BREAK"

        fun startService(context: Context) {
            val intent = Intent(context, PomodoroService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, PomodoroService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        soundPlayer = PomodoroSoundPlayer(this)

        createNotificationChannels()
        acquireWakeLock()
        observeRepository()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle actions from notification buttons
        when (intent?.action) {
            ACTION_START -> PomodoroRepository.startTimer()
            ACTION_PAUSE -> PomodoroRepository.pauseTimer()
            ACTION_RESUME -> PomodoroRepository.startTimer()
            ACTION_STOP -> {
                PomodoroRepository.resetTimer()
            }
            ACTION_START_BREAK -> PomodoroRepository.startBreak()
            ACTION_SKIP_BREAK -> PomodoroRepository.skipBreak()
        }

        // Start foreground with initial notification
        startForeground(NOTIFICATION_ID, buildNotification())

        return START_STICKY
    }

    private fun observeRepository() {
        // Observe time changes
        val timeObserver = Observer<Int> { seconds ->
            currentTimeLeft = seconds
            updateNotification()
        }
        PomodoroRepository.timeLeft.observeForever(timeObserver)

        // Observe state changes
        val stateObserver = Observer<TimerState> { state ->
            val previousState = currentState
            currentState = state
            updateNotification()

            // Check for state transitions that require push notifications
            checkAndShowPushNotification(previousState, state)
        }
        PomodoroRepository.state.observeForever(stateObserver)

        // Observe sound events
        val soundObserver = Observer<SoundEvent?> { event ->
            event?.let {
                if (it != lastSoundEvent) {
                    soundPlayer.playSound(it)
                    lastSoundEvent = it
                }
            }
        }
        PomodoroRepository.soundEvent.observeForever(soundObserver)
    }

    private fun checkAndShowPushNotification(previousState: TimerState, newState: TimerState) {
        // Kết thúc giải lao -> Bắt đầu Pomodoro mới
        if ((previousState == TimerState.BREAK_RUNNING || previousState == TimerState.BREAK_READY)
            && newState == TimerState.IDLE) {
            showPushNotification(
                "Sẵn sàng tập trung!",
                "Phiên giải lao đã kết thúc. Nhấn để bắt đầu Pomodoro mới."
            )
        }

        // Kết thúc Pomodoro -> Bắt đầu giải lao
        if (previousState == TimerState.RUNNING && newState == TimerState.BREAK_READY) {
            showPushNotification(
                "Giờ nghỉ ngơi!",
                "Phiên tập trung đã hoàn thành. Nhấn để bắt đầu giải lao."
            )
        }
    }

    private fun showPushNotification(title: String, message: String) {
        val intent = Intent(this, PomodoroActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, PUSH_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun buildNotification(): android.app.Notification {
        val intent = Intent(this, PomodoroActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Use custom layout based on state
        when (currentState) {
            TimerState.RUNNING -> {
                builder.setCustomContentView(createRunningLayout())
            }
            TimerState.PAUSED -> {
                builder.setCustomContentView(createPausedLayout())
            }
            TimerState.IDLE -> {
                builder.setCustomContentView(createIdleLayout())
            }
            TimerState.BREAK_READY -> {
                builder.setCustomContentView(createBreakReadyLayout())
            }
            TimerState.BREAK_RUNNING -> {
                builder.setCustomContentView(createBreakRunningLayout())
            }
            TimerState.BREAK_PAUSED -> {
                builder.setCustomContentView(createBreakRunningLayout()) // Same as running for break
            }
        }

        return builder.build()
    }

    private fun createRunningLayout(): RemoteViews {
        val remoteViews = RemoteViews(packageName, R.layout.notification_pomodoro_running)

        val minutes = currentTimeLeft / 60
        val seconds = currentTimeLeft % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        remoteViews.setTextViewText(R.id.tvNotificationTitle, "Phát Triển Ứng Dụng Di Động: ...")
        remoteViews.setTextViewText(R.id.tvNotificationTime, timeText)
        remoteViews.setTextColor(R.id.tvNotificationTime, resources.getColor(android.R.color.holo_orange_dark, null))

        // Set pause button action
        val pauseIntent = Intent(this, PomodoroService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this, ACTION_PAUSE.hashCode(), pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.btnNotificationPause, pausePendingIntent)

        return remoteViews
    }

    private fun createPausedLayout(): RemoteViews {
        val remoteViews = RemoteViews(packageName, R.layout.notification_pomodoro_paused)

        val minutes = currentTimeLeft / 60
        val seconds = currentTimeLeft % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        remoteViews.setTextViewText(R.id.tvNotificationTitle, "Phát Triển Ứng Dụng Di Động,...")
        remoteViews.setTextViewText(R.id.tvNotificationTime, timeText)
        remoteViews.setTextColor(R.id.tvNotificationTime, resources.getColor(android.R.color.holo_orange_dark, null))

        // Set resume button action
        val resumeIntent = Intent(this, PomodoroService::class.java).apply {
            action = ACTION_RESUME
        }
        val resumePendingIntent = PendingIntent.getService(
            this, ACTION_RESUME.hashCode(), resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.btnNotificationResume, resumePendingIntent)

        // Set stop button action
        val stopIntent = Intent(this, PomodoroService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, ACTION_STOP.hashCode(), stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.btnNotificationStop, stopPendingIntent)

        return remoteViews
    }

    private fun createIdleLayout(): RemoteViews {
        val remoteViews = RemoteViews(packageName, R.layout.notification_pomodoro_idle)

        remoteViews.setTextViewText(R.id.tvNotificationTitle, "Sau khi bắt đầu đếm thời gian, hãy kiê...")
        remoteViews.setTextViewText(R.id.tvNotificationTime, "25:00")
        remoteViews.setTextColor(R.id.tvNotificationTime, resources.getColor(android.R.color.holo_orange_dark, null))

        // Set start button action
        val startIntent = Intent(this, PomodoroService::class.java).apply {
            action = ACTION_START
        }
        val startPendingIntent = PendingIntent.getService(
            this, ACTION_START.hashCode(), startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.btnNotificationStart, startPendingIntent)

        return remoteViews
    }

    private fun createBreakReadyLayout(): RemoteViews {
        val remoteViews = RemoteViews(packageName, R.layout.notification_pomodoro_break_ready)

        val minutes = currentTimeLeft / 60
        val seconds = currentTimeLeft % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        remoteViews.setTextViewText(R.id.tvNotificationTitle, "Giờ nghỉ ngơi!")
        remoteViews.setTextViewText(R.id.tvNotificationTime, timeText)
        remoteViews.setTextColor(R.id.tvNotificationTime, resources.getColor(android.R.color.holo_blue_dark, null))

        // Set start break button action
        val startBreakIntent = Intent(this, PomodoroService::class.java).apply {
            action = ACTION_START_BREAK
        }
        val startBreakPendingIntent = PendingIntent.getService(
            this, ACTION_START_BREAK.hashCode(), startBreakIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.btnNotificationStartBreak, startBreakPendingIntent)

        return remoteViews
    }

    private fun createBreakRunningLayout(): RemoteViews {
        val remoteViews = RemoteViews(packageName, R.layout.notification_pomodoro_break_running)

        val minutes = currentTimeLeft / 60
        val seconds = currentTimeLeft % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        remoteViews.setTextViewText(R.id.tvNotificationTitle, "Đang giải lao...")
        remoteViews.setTextViewText(R.id.tvNotificationTime, timeText)
        remoteViews.setTextColor(R.id.tvNotificationTime, resources.getColor(android.R.color.holo_blue_dark, null))

        // Set skip break button action
        val skipBreakIntent = Intent(this, PomodoroService::class.java).apply {
            action = ACTION_SKIP_BREAK
        }
        val skipBreakPendingIntent = PendingIntent.getService(
            this, ACTION_SKIP_BREAK.hashCode(), skipBreakIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.btnNotificationSkipBreak, skipBreakPendingIntent)

        return remoteViews
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Foreground service channel (low priority, no sound)
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Pomodoro Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị trạng thái Pomodoro timer"
                setShowBadge(false)
                setSound(null, null)
            }

            // Push notification channel (high priority, with sound)
            val pushChannel = NotificationChannel(
                PUSH_CHANNEL_ID,
                "Thông báo Pomodoro",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo khi hoàn thành phiên tập trung hoặc giải lao"
                setShowBadge(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(pushChannel)
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FocusMate::PomodoroWakeLock"
        ).apply {
            acquire(60 * 60 * 1000L) // 1 hour max
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPlayer.release()
        wakeLock?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}