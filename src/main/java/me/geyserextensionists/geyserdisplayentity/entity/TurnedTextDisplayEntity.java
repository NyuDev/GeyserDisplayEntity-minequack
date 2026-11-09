package me.geyserextensionists.geyserdisplayentity.entity;

import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.TextDisplayEntity;

/**
 * MineQuack: Geyser's text display, with its translation turned by the display's yaw as Java does for a fixed
 * text. Geyser adds the translation as it is, so two columns side by side on a board facing east or west (the
 * leaderboards) land on top of each other.
 */
public class TurnedTextDisplayEntity extends TextDisplayEntity {

    public TurnedTextDisplayEntity(EntitySpawnContext entitySpawnContext) {
        super(entitySpawnContext);
    }

    @Override
    public Vector3f bedrockPosition() {
        Vector3f shown = super.bedrockPosition();
        Vector3f t = getTranslation();
        if (t == null || (t.getX() == 0 && t.getZ() == 0)) return shown;

        // Java draws a fixed text at orientation * translation, orientation = rotationY(-yaw)
        double radians = Math.toRadians(-getYaw());
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float x = t.getX() * cos + t.getZ() * sin;
        float z = -t.getX() * sin + t.getZ() * cos;
        return shown.add(x - t.getX(), 0, z - t.getZ());
    }
}
