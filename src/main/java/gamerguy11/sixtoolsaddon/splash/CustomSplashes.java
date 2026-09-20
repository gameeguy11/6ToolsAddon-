package gamerguy11.sixtoolsaddon.splash;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class CustomSplashes {

    private static final List<String> SPLASHES;

    static {
        SPLASHES = new ArrayList<>();
        SPLASHES.add("FUCK MOJANG");
        SPLASHES.add("Based anarchy mod.");
    }

    private static final Random RANDOM = new Random();

    private CustomSplashes() {
    }

    public static String pick() {
        if (SPLASHES.isEmpty()) {
            return null;
        }
        return SPLASHES.get(RANDOM.nextInt(SPLASHES.size()));
    }
}
