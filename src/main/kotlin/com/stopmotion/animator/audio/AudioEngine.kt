package com.stopmotion.animator.audio

import com.stopmotion.animator.model.AudioTrack
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.*

class AudioEngine {
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Cache of loaded raw PCM audio bytes per track id
    private val audioDataCache = mutableMapOf<String, PcmAudioData>()

    data class PcmAudioData(
        val format: AudioFormat,
        val samples: ByteArray,
        val totalFrames: Long,
        val sampleRate: Float
    )

    /**
     * Loads and parses an audio file (WAV/AIFF supported out-of-the-box by Java Sound).
     * Calculates waveform peaks for visualization in the timeline.
     */
    fun loadAudioTrack(file: File, fps: Int): AudioTrack? {
        if (!file.exists()) return null
        return try {
            val audioInputStream = AudioSystem.getAudioInputStream(file)
            val baseFormat = audioInputStream.format

            // Convert to 16-bit PCM if needed for consistent processing
            val targetFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.sampleRate,
                16,
                baseFormat.channels,
                baseFormat.channels * 2,
                baseFormat.sampleRate,
                false
            )

            val pcmStream = if (AudioSystem.isConversionSupported(targetFormat, baseFormat)) {
                AudioSystem.getAudioInputStream(targetFormat, audioInputStream)
            } else {
                audioInputStream
            }

            val rawBytes = pcmStream.readAllBytes()
            val format = pcmStream.format

            val bytesPerSample = format.sampleSizeInBits / 8
            val totalSamples = rawBytes.size / (bytesPerSample * format.channels)
            val durationSeconds = totalSamples.toDouble() / format.sampleRate
            val totalDurationFrames = (durationSeconds * fps).toInt().coerceAtLeast(1)

            // Compute waveform peaks (one peak per timeline frame)
            val peaks = calculateWaveformPeaks(rawBytes, format, totalDurationFrames)

            val track = AudioTrack(
                name = file.nameWithoutExtension,
                filePath = file.absolutePath,
                durationFrames = totalDurationFrames,
                waveformPeaks = peaks
            )

            audioDataCache[track.id] = PcmAudioData(
                format = format,
                samples = rawBytes,
                totalFrames = totalSamples.toLong(),
                sampleRate = format.sampleRate
            )

            track
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateWaveformPeaks(
        bytes: ByteArray,
        format: AudioFormat,
        framesCount: Int
    ): List<Float> {
        if (framesCount <= 0 || bytes.isEmpty()) return emptyList()
        val peaks = FloatArray(framesCount)
        val bytesPerFrameChunk = (bytes.size / framesCount).coerceAtLeast(4)

        for (i in 0 until framesCount) {
            val startByte = (i * bytesPerFrameChunk).coerceAtMost(bytes.size - 2)
            val endByte = ((i + 1) * bytesPerFrameChunk).coerceAtMost(bytes.size)
            var maxVal = 0

            var b = startByte
            while (b < endByte - 1) {
                // Read 16-bit signed sample (little endian)
                val sample = ((bytes[b + 1].toInt() shl 8) or (bytes[b].toInt() and 0xFF)).toShort()
                val abs = Math.abs(sample.toInt())
                if (abs > maxVal) maxVal = abs
                b += 2 * format.channels
            }
            peaks[i] = (maxVal / 32768.0f).coerceIn(0.05f, 1.0f)
        }
        return peaks.toList()
    }

    /**
     * Frame-accurate audio scrub: plays a short snippet (1 frame slice) for lip sync.
     */
    fun scrubFrame(track: AudioTrack, frameIndex: Int, fps: Int) {
        if (track.isMuted || track.volume <= 0f) return
        val pcm = audioDataCache[track.id] ?: return

        scope.launch(Dispatchers.IO) {
            try {
                val relativeFrame = frameIndex - track.startFrame
                if (relativeFrame < 0 || relativeFrame >= track.durationFrames) return@launch

                val secondsPerFrame = 1.0 / fps
                val startSecond = relativeFrame * secondsPerFrame
                val bytesPerSecond = pcm.format.frameRate * pcm.format.frameSize
                val startByte = (startSecond * bytesPerSecond).toLong().coerceIn(0, pcm.samples.size.toLong())
                val sliceLength = (secondsPerFrame * bytesPerSecond).toInt().coerceAtMost((pcm.samples.size - startByte).toInt())

                if (sliceLength > 0) {
                    val slice = ByteArray(sliceLength)
                    System.arraycopy(pcm.samples, startByte.toInt(), slice, 0, sliceLength)

                    // Apply volume
                    applyVolume(slice, pcm.format, track.volume)

                    val bais = ByteArrayInputStream(slice)
                    val ais = AudioInputStream(bais, pcm.format, (sliceLength / pcm.format.frameSize).toLong())
                    val clip = AudioSystem.getClip()
                    clip.open(ais)
                    clip.start()
                }
            } catch (e: Exception) {
                // Ignore audio scrub glitches
            }
        }
    }

    private fun applyVolume(bytes: ByteArray, format: AudioFormat, volume: Float) {
        if (volume >= 1.0f) return
        var i = 0
        while (i < bytes.size - 1) {
            val sample = ((bytes[i + 1].toInt() shl 8) or (bytes[i].toInt() and 0xFF)).toShort()
            val adjusted = (sample * volume).toInt().toShort()
            bytes[i] = (adjusted.toInt() and 0xFF).toByte()
            bytes[i + 1] = ((adjusted.toInt() shr 8) and 0xFF).toByte()
            i += 2
        }
    }

    fun release() {
        playbackJob?.cancel()
        audioDataCache.clear()
        scope.cancel()
    }
}
