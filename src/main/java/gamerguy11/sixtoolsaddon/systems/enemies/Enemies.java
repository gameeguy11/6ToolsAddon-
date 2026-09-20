package gamerguy11.sixtoolsaddon.systems.enemies;

import meteordevelopment.meteorclient.MeteorClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Enemies implements Iterable<Enemy> {
    private static final Enemies INSTANCE = new Enemies();
    private static final File FILE = new File(MeteorClient.FOLDER, "sixtoolsaddon-enemies.txt");

    private final List<Enemy> enemies = new ArrayList<>();
    private boolean loaded;

    private Enemies() {
    }

    public static Enemies get() {
        INSTANCE.loadIfNeeded();
        return INSTANCE;
    }

    private void loadIfNeeded() {
        if (loaded) return;
        loaded = true;

        if (!FILE.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String name = line.trim();
                if (name.isEmpty() || get(name) != null) continue;

                enemies.add(new Enemy(name));
            }

            Collections.sort(enemies);
        } catch (IOException e) {
            MeteorClient.LOG.error("6ToolsAddon: failed to load enemies list", e);
        }
    }

    private void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            for (Enemy enemy : enemies) {
                writer.write(enemy.getName());
                writer.write(System.lineSeparator());
            }
        } catch (IOException e) {
            MeteorClient.LOG.error("6ToolsAddon: failed to save enemies list", e);
        }
    }

    public boolean add(Enemy enemy) {
        if (enemy.name.isEmpty() || enemy.name.contains(" ")) return false;
        if (get(enemy.name) != null) return false;

        enemies.add(enemy);
        Collections.sort(enemies);
        save();

        return true;
    }

    public boolean remove(Enemy enemy) {
        if (enemies.remove(enemy)) {
            save();
            return true;
        }

        return false;
    }

    public Enemy get(String name) {
        for (Enemy enemy : enemies) {
            if (enemy.name.equalsIgnoreCase(name)) {
                return enemy;
            }
        }

        return null;
    }

    public boolean isEnemy(String name) {
        return get(name) != null;
    }

    public int count() {
        return enemies.size();
    }

    public boolean isEmpty() {
        return enemies.isEmpty();
    }

    @Override
    public Iterator<Enemy> iterator() {
        return enemies.iterator();
    }
}
