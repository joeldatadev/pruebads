package com.zenflow.app.game

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/game/HapticController.kt
 */
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticController(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /** Toque suave al iniciar un nodo. */
    fun onNodeTouch() = vibrateOneShot(durationMs = 10, amplitude = 60)

    /** Vibración media cuando un color se conecta (sincronizada con Snap + partículas). */
    fun onColorConnected() = vibrateOneShot(durationMs = 25, amplitude = 140)

    /** Patrón más largo/fuerte al completar el nivel entero. */
    fun onLevelComplete() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 30, 40, 30, 40, 60)
            val amplitudes = intArrayOf(0, 100, 0, 140, 0, 220)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }

    /** Vibración muy corta y seca para un movimiento inválido (feedback de "rechazo"). */
    fun onInvalidMove() = vibrateOneShot(durationMs = 8, amplitude = 40)

    private fun vibrateOneShot(durationMs: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }
}
