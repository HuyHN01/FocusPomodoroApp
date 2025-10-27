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
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.example.focusmate.R
import com.example.focusmate.data.repository.PomodoroRepository
import com.example.focusmate.ui.pomodoro.PomodoroActivity
import com.example.focusmate.ui.pomodoro.TimerState
import com.example.focusmate.util.PomodoroSoundPlayer
import com.example.focusmate.util.SoundEvent

class PomodoroService: Service() {
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
            ACTION_PAUSE -> PomodoroRepository.pauseTimer()
            ACTION_RESUME -> PomodoroRepository.startTimer()
            ACTION_STOP -> {
                PomodoroRepository.resetTimer()
                stopForeground(true)
                stopSelf()
            }
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
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Thay bằng icon của bạn
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

        val minutes = currentTimeLeft / 60
        val seconds = currentTimeLeft % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        val (title, contentText) = when (currentState) {
            TimerState.RUNNING -> "Đang tập trung" to "Còn lại: $timeText"
            TimerState.PAUSED -> "Đã tạm dừng" to "Thời gian: $timeText"
            TimerState.BREAK_READY -> "Sẵn sàng giải lao" to "Nhấn để bắt đầu nghỉ"
            TimerState.BREAK_RUNNING -> "Đang giải lao" to "Còn lại: $timeText"
            TimerState.BREAK_PAUSED -> "Giải lao tạm dừng" to "Thời gian: $timeText"
            else -> "Pomodoro Timer" to "Sẵn sàng bắt đầu"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Thay bằng icon của bạn
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Add action buttons based on state
        when (currentState) {
            TimerState.RUNNING -> {
                builder.addAction(
                    R.drawable.pause_circle_24px, // Thay bằng icon pause
                    "Tạm dừng",
                    createActionPendingIntent(ACTION_PAUSE)
                )
            }
            TimerState.PAUSED -> {
                builder.addAction(
                    R.drawable.play_circle_24px, // Thay bằng icon play
                    "Tiếp tục",
                    createActionPendingIntent(ACTION_RESUME)
                )
                builder.addAction(
                    R.drawable.ic_close, // Thay bằng icon stop
                    "Dừng",
                    createActionPendingIntent(ACTION_STOP)
                )
            }
            TimerState.BREAK_RUNNING, TimerState.BREAK_PAUSED -> {
                builder.addAction(
                    R.drawable.ic_close, // Thay bằng icon skip
                    "Bỏ qua",
                    createActionPendingIntent(ACTION_SKIP_BREAK)
                )
            }
            else -> {
                // IDLE or BREAK_READY - no actions or minimal actions
            }
        }

        return builder.build()
    }

    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, PomodoroService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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