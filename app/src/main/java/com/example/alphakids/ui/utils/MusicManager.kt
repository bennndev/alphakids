package com.example.alphakids.ui.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.IOException

// 🌐 URLs de música desde Firebase Storage (Mantenemos las constantes aquí para la referencia)
private const val MUSICA_FONDO_APP_URL =
    "https://firebasestorage.googleapis.com/v0/b/alphakids-tecsup.firebasestorage.app/o/musica_fondo_app.mp3?alt=media&token=e199b012-8522-4d1c-8f82-86c49d6a8677"

private const val MUSICA_FONDO_JUEGO_URL =
    "https://firebasestorage.googleapis.com/v0/b/alphakids-tecsup.firebasestorage.app/o/musica_fondo_juego.mp3?alt=media&token=9ad53b6e-bc50-4b0a-a6cc-5c1913f2c889"

/**
 * Clase Singleton para gestionar y controlar todo el ciclo de vida del audio de la aplicación.
 */
object MusicManager {

    // 🎧 Reproductores privados
    private var musicaApp: MediaPlayer? = null
    private var musicaJuego: MediaPlayer? = null

    /**
     * 🎵 Reproduce la música global (de la app) en loop. Usa Context para iniciar.
     */
    fun startMusicaApp(context: Context) {
        if (musicaApp?.isPlaying == true) return

        stopMusicaApp() // Asegurar que no hay duplicados
        musicaApp = MediaPlayer().apply {
            try {
                // Usamos la URL
                setDataSource(MUSICA_FONDO_APP_URL)
                isLooping = true
                setOnPreparedListener {
                    it.start()
                    Log.d("MusicManager", "Música de APP iniciada/reanuda.")
                }
                setOnErrorListener { mp, what, _ ->
                    Log.e("MusicManager", "Error música app: $what")
                    mp.release()
                    musicaApp = null
                    true
                }
                prepareAsync()
            } catch (e: Exception) {
                Log.e("MusicManager", "Error al iniciar música app: ${e.message}")
            }
        }
    }

    /**
     * ▶️ Reanuda la música de la app si estaba pausada.
     */
    fun resumeMusicaApp() {
        if (musicaApp != null && !musicaApp!!.isPlaying) {
            musicaApp?.start()
            Log.d("MusicManager", "Música de APP reanudada.")
        }
    }

    /**
     * ⏸️ Pausa la música global sin destruirla.
     */
    fun pauseMusicaApp() {
        if (musicaApp?.isPlaying == true) {
            musicaApp?.pause()
            Log.d("MusicManager", "Música de APP pausada.")
        }
    }

    /**
     * 🚫 Detiene completamente la música de la app.
     */
    fun stopMusicaApp() {
        musicaApp?.stop()
        musicaApp?.release()
        musicaApp = null
    }

    /**
     * 🎮 Reproduce la música del juego (loop), pausando la global. Usa Context para iniciar.
     */
    fun startMusicaJuego(context: Context) {
        // 1. Siempre pausar la música de la APP antes de iniciar el juego.
        pauseMusicaApp()

        // 2. Asegurar que no hay duplicados del juego.
        // NO LLAMAMOS stopMusicaJuego() aquí, ya que stopMusicaJuego() llama a resumeMusicaApp()
        // lo que crearía el conflicto. Lo hacemos manual para evitar ese resume.
        musicaJuego?.stop()
        musicaJuego?.release()
        musicaJuego = null

        musicaJuego = MediaPlayer().apply {
            try {
                // Usamos la URL
                setDataSource(MUSICA_FONDO_JUEGO_URL)
                isLooping = true
                setOnPreparedListener {
                    it.start()
                    Log.d("MusicManager", "Música de Juego INICIADA. App está pausada.")
                }
                setOnErrorListener { mp, what, _ ->
                    Log.e("MusicManager", "Error música juego: $what")
                    mp.release()
                    musicaJuego = null
                    true
                }
                prepareAsync()
            } catch (e: IOException) {
                Log.e("MusicManager", "Error al configurar música juego: ${e.message}")
            } catch (e: Exception) {
                Log.e("MusicManager", "Error general música juego: ${e.message}")
            }
        }
    }

    /**
     * 🧹 Detiene la música del juego. NO REANUDA LA MÚSICA DE LA APP.
     */
    fun stopMusicaJuego() {
        musicaJuego?.stop()
        musicaJuego?.release()
        musicaJuego = null

        // 🛑 ESTE ERA EL PRINCIPAL PROBLEMA. Lo eliminamos para que el control
        // de reanudación sea EXCLUSIVO de la pantalla (CameraOCRScreen).
        // resumeMusicaApp()
        Log.d("MusicManager", "Música de Juego detenida.")
    }

    /**
     * 🔚 Limpieza general (por ejemplo, al cerrar la app - llamado desde MainActivity.onDestroy/onStop)
     */
    fun releaseAllMusic() {
        stopMusicaApp()
        stopMusicaJuego()
        // Aquí no hay problema, ya que stopMusicaJuego() ya no llama a resumeMusicaApp().
        Log.d("MusicManager", "Todos los recursos de audio liberados.")
    }
}