package gamerguy11.anarchyaddon.sound;

import gamerguy11.anarchyaddon.AnarchyAddon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.sound.OggAudioStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class SoundEngine {
    public static final SoundEngine INSTANCE = new SoundEngine();

    public enum Mode {
        Random,
        Sequential,
        Specific
    }

    private static final Set<String> EXTENSIONS = Set.of("ogg", "wav", "aif", "aiff", "au");
    private static final int MAX_VOICES = 16;
    private static final long MAX_FILE_BYTES = 16L * 1024 * 1024;
    private static final long MAX_DECODED_BYTES = 64L * 1024 * 1024;

    private final Path root = FabricLoader.getInstance().getConfigDir().resolve("anarchyaddon").resolve("sounds");
    private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AnarchyAddon-Sound");
        t.setDaemon(true);
        return t;
    });

    private final Map<SoundType, List<Path>> files = new ConcurrentHashMap<>();
    private final Map<SoundType, FileTime> stamps = new ConcurrentHashMap<>();
    private final Map<SoundType, Integer> sequence = new ConcurrentHashMap<>();
    private final Map<SoundType, Integer> lastIndex = new ConcurrentHashMap<>();
    private final Map<Path, PcmSound> cache = new ConcurrentHashMap<>();
    private final Set<Path> failed = ConcurrentHashMap.newKeySet();
    private final AtomicInteger voices = new AtomicInteger();

    private SoundEngine() {}

    private record PcmSound(byte[] data, float sampleRate, int channels) {
        AudioFormat format() {
            return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16, channels, channels * 2, sampleRate, false);
        }
    }

    public Path getRoot() {
        return root;
    }

    public Path getFolder(SoundType type) {
        return root.resolve(type.folder);
    }

    public void ensureFolders() {
        try {
            Files.createDirectories(root);
            for (SoundType type : SoundType.values()) Files.createDirectories(getFolder(type));

            Path readme = root.resolve("README.txt");
            if (!Files.exists(readme)) {
                Files.writeString(readme, String.join(System.lineSeparator(),
                    "AnarchyAddon custom sounds",
                    "",
                    "Put your own .ogg or .wav files in the folder that matches the event:",
                    "",
                    describeFolders(),
                    "You can put several files in one folder - use the 'Sound Editor' module in the click GUI to choose",
                    "Random / Sequential / Specific playback per event, and to preview or pick files.",
                    "Nothing plays for an event until its folder has at least one file in it.",
                    ""));
            }
        } catch (IOException e) {
            AnarchyAddon.LOG.warn("Could not create sound folders in {}", root, e);
        }
    }

    private static String describeFolders() {
        StringBuilder sb = new StringBuilder();
        for (SoundType type : SoundType.values()) {
            sb.append("  ").append(type.folder).append("/  -  ").append(type.description).append(System.lineSeparator());
        }
        return sb.toString();
    }

    public void rescan() {
        ensureFolders();
        for (SoundType type : SoundType.values()) scan(type);
    }

    public void reload() {
        cache.clear();
        failed.clear();
        sequence.clear();
        lastIndex.clear();
        stamps.clear();
        rescan();
    }

    private void scan(SoundType type) {
        Path dir = getFolder(type);
        List<Path> found = new ArrayList<>();

        if (Files.isDirectory(dir)) {
            try (Stream<Path> stream = Files.list(dir)) {
                stream.filter(Files::isRegularFile)
                    .filter(p -> EXTENSIONS.contains(extension(p)))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .forEach(found::add);
            } catch (IOException e) {
                AnarchyAddon.LOG.warn("Could not list {}", dir, e);
            }
            try {
                stamps.put(type, Files.getLastModifiedTime(dir));
            } catch (IOException ignored) {}
        }

        files.put(type, found);
    }

    private void refreshIfChanged(SoundType type) {
        try {
            Path dir = getFolder(type);
            if (!Files.isDirectory(dir)) {
                if (!files.getOrDefault(type, List.of()).isEmpty()) files.put(type, List.of());
                return;
            }
            FileTime now = Files.getLastModifiedTime(dir);
            if (!now.equals(stamps.get(type)) || !files.containsKey(type)) scan(type);
        } catch (IOException ignored) {}
    }

    private static String extension(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public List<String> getFileNames(SoundType type) {
        List<String> names = new ArrayList<>();
        for (Path p : files.getOrDefault(type, List.of())) names.add(p.getFileName().toString());
        return names;
    }

    public void play(SoundType type, Mode mode, String specific, float volume, float pitch) {
        exec.execute(() -> {
            try {
                refreshIfChanged(type);
                Path file = pick(type, mode, specific);
                if (file != null) playFile(file, volume, pitch);
            } catch (Throwable t) {
                AnarchyAddon.LOG.warn("Failed to play {} sound", type.folder, t);
            }
        });
    }

    public void playNamed(SoundType type, String fileName, float volume, float pitch) {
        exec.execute(() -> {
            try {
                for (Path p : files.getOrDefault(type, List.of())) {
                    if (p.getFileName().toString().equalsIgnoreCase(fileName)) {
                        playFile(p, volume, pitch);
                        return;
                    }
                }
            } catch (Throwable t) {
                AnarchyAddon.LOG.warn("Failed to preview {}", fileName, t);
            }
        });
    }

    private Path pick(SoundType type, Mode mode, String specific) {
        List<Path> list = files.getOrDefault(type, List.of());
        if (list.isEmpty()) return null;

        if (mode == Mode.Specific && specific != null && !specific.isBlank()) {
            String wanted = specific.trim();
            for (Path p : list) {
                String name = p.getFileName().toString();
                String noExt = name.substring(0, Math.max(0, name.lastIndexOf('.')));
                if (name.equalsIgnoreCase(wanted) || noExt.equalsIgnoreCase(wanted)) return p;
            }
        }

        if (mode == Mode.Sequential) {
            int idx = sequence.merge(type, 1, Integer::sum) - 1;
            return list.get(Math.floorMod(idx, list.size()));
        }

        int idx = ThreadLocalRandom.current().nextInt(list.size());
        if (list.size() > 1 && idx == lastIndex.getOrDefault(type, -1)) idx = (idx + 1) % list.size();
        lastIndex.put(type, idx);
        return list.get(idx);
    }

    private void playFile(Path file, float volume, float pitch) {
        if (volume <= 0.0001f) return;

        PcmSound sound = load(file);
        if (sound == null) return;

        if (voices.get() >= MAX_VOICES) return;

        float p = Math.max(0.25f, Math.min(4f, pitch));
        byte[] data = Math.abs(p - 1f) < 0.005f ? sound.data() : resample(sound, p);

        try {
            Clip clip = AudioSystem.getClip();
            voices.incrementAndGet();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) clip.close();
                else if (event.getType() == LineEvent.Type.CLOSE) voices.decrementAndGet();
            });

            try {
                clip.open(sound.format(), data, 0, data.length);
            } catch (Throwable t) {
                clip.close();
                throw t;
            }

            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float db = (float) (20.0 * Math.log10(Math.max(volume, 0.0001f)));
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
            }

            clip.start();
        } catch (Throwable t) {
            AnarchyAddon.LOG.warn("Could not play {}", file.getFileName(), t);
        }
    }

    private PcmSound load(Path file) {
        PcmSound cached = cache.get(file);
        if (cached != null) return cached;
        if (failed.contains(file)) return null;

        try {
            if (Files.size(file) > MAX_FILE_BYTES) throw new IOException("file is larger than 16 MB - use short clips");

            PcmSound decoded = extension(file).equals("ogg") ? decodeOgg(file) : decodeJavaSound(file);
            if (decoded.data().length > MAX_DECODED_BYTES) throw new IOException("sound is too long");

            cache.put(file, decoded);
            return decoded;
        } catch (Throwable t) {
            failed.add(file);
            AnarchyAddon.LOG.warn("Could not load sound {} - it will be skipped until you press Reload ({})", file.getFileName(), t.toString());
            return null;
        }
    }

    private static PcmSound decodeOgg(Path file) throws IOException {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file)); OggAudioStream ogg = new OggAudioStream(in)) {
            AudioFormat format = ogg.getFormat();
            int channels = Math.max(1, format.getChannels());

            FloatBuffer samples = new FloatBuffer();
            int guard = 0;
            while (ogg.read(samples::add)) {
                if (samples.size * 2L > MAX_DECODED_BYTES || ++guard > 1_000_000) throw new IOException("sound is too long");
            }

            int usable = samples.size - (samples.size % channels);
            if (usable <= 0) throw new IOException("no audio data");

            ByteBuffer out = ByteBuffer.allocate(usable * 2).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < usable; i++) {
                float v = Math.max(-1f, Math.min(1f, samples.data[i]));
                out.putShort((short) Math.round(v * 32767f));
            }

            return new PcmSound(out.array(), format.getSampleRate(), channels);
        }
    }

    private static PcmSound decodeJavaSound(Path file) throws Exception {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(file.toFile())) {
            AudioFormat src = source.getFormat();
            int channels = Math.max(1, src.getChannels());
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, src.getSampleRate(), 16, channels, channels * 2, src.getSampleRate(), false);

            try (AudioInputStream pcm = AudioSystem.getAudioInputStream(target, source)) {
                return new PcmSound(pcm.readAllBytes(), src.getSampleRate(), channels);
            }
        }
    }

    private static final class FloatBuffer {
        float[] data = new float[1 << 16];
        int size;

        void add(float v) {
            if (size == data.length) data = java.util.Arrays.copyOf(data, data.length * 2);
            data[size++] = v;
        }
    }

    private static byte[] resample(PcmSound sound, float pitch) {
        int ch = sound.channels();
        ShortBuffer in = ByteBuffer.wrap(sound.data()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        int frames = in.capacity() / ch;
        int outFrames = Math.max(1, (int) (frames / pitch));

        ByteBuffer outBytes = ByteBuffer.allocate(outFrames * ch * 2).order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer out = outBytes.asShortBuffer();

        for (int i = 0; i < outFrames; i++) {
            double pos = i * (double) pitch;
            int i0 = Math.min((int) pos, frames - 1);
            int i1 = Math.min(i0 + 1, frames - 1);
            double frac = pos - i0;

            for (int c = 0; c < ch; c++) {
                short a = in.get(i0 * ch + c);
                short b = in.get(i1 * ch + c);
                out.put(i * ch + c, (short) Math.round(a + (b - a) * frac));
            }
        }

        return outBytes.array();
    }
}
