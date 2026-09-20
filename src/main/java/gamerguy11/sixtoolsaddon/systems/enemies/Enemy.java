package gamerguy11.sixtoolsaddon.systems.enemies;

import java.util.Objects;

public class Enemy implements Comparable<Enemy> {
    public volatile String name;

    public Enemy(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Enemy enemy = (Enemy) o;
        return name.equalsIgnoreCase(enemy.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }

    @Override
    public int compareTo(Enemy enemy) {
        return name.compareToIgnoreCase(enemy.name);
    }
}
