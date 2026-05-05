package net.mcreator.climber.client;

import net.mcreator.climber.ClimberMod;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClimberSoundPreloader {

    private static final Map<String, Integer> BUFFER_CACHE = new HashMap<>();

    /** Pre-load a file in the background so the first keypress is instant. */
    public static void preload(String path) {
        if (path == null || path.isEmpty()) return;
        if (BUFFER_CACHE.containsKey(path)) return;
        if (!isFilePath(path)) return;

        new Thread(() -> {
            int buf = loadToOpenAL(path);
            if (buf != -1) {
                BUFFER_CACHE.put(path, buf);
                ClimberMod.LOGGER.info("[Climber] Preloaded sound: {}", path);
            }
        }, "ClimberSound-Preload").start();
    }

    /** Play a file-path sound at a world position. */
    public static void playAt(String path, float volume, float pitch, Vec3 pos) {
        if (path == null || path.isEmpty() || !isFilePath(path)) return;

        if (!BUFFER_CACHE.containsKey(path)) {
            int buf = loadToOpenAL(path);
            if (buf != -1) BUFFER_CACHE.put(path, buf);
        }

        Integer buf = BUFFER_CACHE.get(path);
        if (buf == null || buf == -1) return;

        int finalBuf = buf;
        new Thread(() -> {
            int source = AL10.alGenSources();
            AL10.alSourcei(source, AL10.AL_BUFFER, finalBuf);
            AL10.alSourcef(source, AL10.AL_GAIN, volume);
            AL10.alSourcef(source, AL10.AL_PITCH, pitch);
            AL10.alSource3f(source, AL10.AL_POSITION, (float) pos.x, (float) pos.y, (float) pos.z);
            AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_FALSE);
            AL10.alSourcePlay(source);

            while (AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }
            AL10.alDeleteSources(source);
        }, "ClimberSound-Play").start();
    }

    /** Play a file-path sound (non-positional / UI). */
    public static void play(String path, float volume, float pitch) {
        if (path == null || path.isEmpty() || !isFilePath(path)) return;

        if (!BUFFER_CACHE.containsKey(path)) {
            int buf = loadToOpenAL(path);
            if (buf != -1) BUFFER_CACHE.put(path, buf);
        }

        Integer buf = BUFFER_CACHE.get(path);
        if (buf == null || buf == -1) return;

        int finalBuf = buf;
        new Thread(() -> {
            int source = AL10.alGenSources();
            AL10.alSourcei(source, AL10.AL_BUFFER, finalBuf);
            AL10.alSourcef(source, AL10.AL_GAIN, volume);
            AL10.alSourcef(source, AL10.AL_PITCH, pitch);
            AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_FALSE);
            AL10.alSourcePlay(source);

            while (AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }
            AL10.alDeleteSources(source);
        }, "ClimberSound-Play").start();
    }

    public static void clear() {
        BUFFER_CACHE.values().forEach(AL10::alDeleteBuffers);
        BUFFER_CACHE.clear();
    }

    public static boolean isFilePath(String s) {
        return s.contains("\\")
            || (s.length() > 1 && s.charAt(1) == ':')
            || s.startsWith("/")
            || s.startsWith("./")
            || s.startsWith("../");
    }

    // -----------------------------------------------------------------
    // Loading
    // -----------------------------------------------------------------

    private static int loadToOpenAL(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                ClimberMod.LOGGER.error("[Climber] Sound file not found: {}", path);
                return -1;
            }
            String low = path.toLowerCase();
            if (low.endsWith(".ogg")) return loadOgg(file);
            if (low.endsWith(".wav")) return loadWav(file);
            if (low.endsWith(".mp3")) return loadMp3(file);
            ClimberMod.LOGGER.error("[Climber] Unsupported sound format: {}", path);
            return -1;
        } catch (Exception e) {
            ClimberMod.LOGGER.error("[Climber] Failed to load sound: {}", path, e);
            return -1;
        }
    }

    private static int loadOgg(File file) throws Exception {
        ByteBuffer raw = null;
        ShortBuffer pcm = null;
        try (FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            raw = MemoryUtil.memAlloc((int) fc.size());
            fc.read(raw);
            raw.flip();

            int channels, sampleRate;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer ch = stack.mallocInt(1);
                IntBuffer sr = stack.mallocInt(1);
                pcm = STBVorbis.stb_vorbis_decode_memory(raw, ch, sr);
                if (pcm == null) throw new IOException("OGG decode failed: " + file);
                channels = ch.get(0);
                sampleRate = sr.get(0);
            }

            int buf = AL10.alGenBuffers();
            AL10.alBufferData(buf,
                    channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16,
                    pcm, sampleRate);
            return buf;
        } finally {
            if (pcm != null) MemoryUtil.memFree(pcm);
            if (raw != null) MemoryUtil.memFree(raw);
        }
    }

    private static int loadWav(File file) throws Exception {
        AudioInputStream audio = AudioSystem.getAudioInputStream(
                new BufferedInputStream(new FileInputStream(file)));
        AudioFormat fmt = audio.getFormat();
        if (fmt.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || fmt.getSampleSizeInBits() != 16) {
            AudioFormat target = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    fmt.getSampleRate(), 16,
                    fmt.getChannels(), fmt.getChannels() * 2,
                    fmt.getSampleRate(), false);
            audio = AudioSystem.getAudioInputStream(target, audio);
            fmt = target;
        }
        byte[] bytes = audio.readAllBytes();
        audio.close();

        ByteBuffer pcm = MemoryUtil.memAlloc(bytes.length);
        try {
            pcm.put(bytes).flip();
            ShortBuffer shorts = pcm.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            int buf = AL10.alGenBuffers();
            AL10.alBufferData(buf,
                    fmt.getChannels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16,
                    shorts, (int) fmt.getSampleRate());
            return buf;
        } finally {
            MemoryUtil.memFree(pcm);
        }
    }

    private static int loadMp3(File file) throws Exception {
        AudioInputStream audio;
        try {
            audio = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(new FileInputStream(file)));
        } catch (UnsupportedAudioFileException e) {
            ClimberMod.LOGGER.error("[Climber] MP3 not supported by JVM. Convert to OGG/WAV: {}", file);
            return -1;
        }
        AudioFormat fmt = audio.getFormat();
        AudioFormat target = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                fmt.getSampleRate() > 0 ? fmt.getSampleRate() : 44100,
                16,
                fmt.getChannels() > 0 ? fmt.getChannels() : 2,
                fmt.getChannels() * 2,
                fmt.getSampleRate() > 0 ? fmt.getSampleRate() : 44100,
                false);
        audio = AudioSystem.getAudioInputStream(target, audio);
        fmt = target;

        byte[] bytes = audio.readAllBytes();
        audio.close();

        ByteBuffer pcm = MemoryUtil.memAlloc(bytes.length);
        try {
            pcm.put(bytes).flip();
            ShortBuffer shorts = pcm.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            int buf = AL10.alGenBuffers();
            AL10.alBufferData(buf,
                    fmt.getChannels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16,
                    shorts, (int) fmt.getSampleRate());
            return buf;
        } finally {
            MemoryUtil.memFree(pcm);
        }
    }
}