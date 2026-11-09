package me.geyserextensionists.geyserdisplayentity.entity;

import me.geyserextensionists.geyserdisplayentity.GeyserDisplayEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.TextDisplayEntity;
import org.geysermc.geyser.entity.type.Tickable;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;

import java.util.Optional;

/**
 * MineQuack: Geyser's text display, with two changes.
 * <ul>
 * <li>Its translation is turned by the display's yaw, as Java does for a fixed text. Geyser adds the translation as it
 * is, so two columns side by side on a board facing east or west (the leaderboards) land on top of each other.</li>
 * <li>A Bedrock player doesn't see it from farther than {@code text-displays.max-distance} blocks. Geyser draws a text
 * as a name tag: always the same size, turned to the player and seen through walls, so far texts pile up.</li>
 * <li>A script can give it another look on Bedrock through its custom name, which Java doesn't show while
 * custom_name_visible is false: {@code bedrock:hide} hides it, {@code bedrock:<text>} (§ colours) shows that text
 * instead. Small texts laid on a table or a machine read well on Java and pile up on Bedrock.</li>
 * </ul>
 */
public class TurnedTextDisplayEntity extends TextDisplayEntity implements Tickable {

    // Re-checked every this many ticks; shown again this much closer than the limit (no flicker at the edge)
    private static final int CHECK_TICKS = 5;
    private static final float MARGIN = 0.5f;

    // The Bedrock name tag Geyser made of the Java text, and whether it is blanked out (too far)
    private String nameTag;
    private boolean far;
    // What a script asked Bedrock to show instead (custom name "bedrock:..."): null = the Java text, "" = nothing
    private static final String MARK = "bedrock:";
    private String bedrockText;
    private int ticks;

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

    @Override
    public void setText(EntityMetadata<Component, ?> entityMetadata) {
        super.setText(entityMetadata);
        // (Geyser has just put the converted text, still waiting to be sent)
        Object name = getMetadata().get(EntityDataTypes.NAME);
        if (name instanceof String text) nameTag = text;
        if (far || bedrockText != null) putName();
    }

    @Override
    public void setCustomName(EntityMetadata<Optional<Component>, ?> entityMetadata) {
        Component name = entityMetadata.getValue().orElse(null);
        String text = name == null ? "" : LegacyComponentSerializer.legacySection().serialize(name);
        if (text.startsWith(MARK)) {
            String rest = text.substring(MARK.length());
            String wanted = rest.equals("hide") ? "" : rest;
            if (!wanted.equals(bedrockText)) {
                bedrockText = wanted;
                putName();
            }
            return;
        }
        if (bedrockText != null) {
            bedrockText = null;
            putName();
        }
        super.setCustomName(entityMetadata);
    }

    // The name tag Bedrock draws: nothing when too far, else the script's Bedrock text or the Java one
    private void putName() {
        String shown = bedrockText != null ? bedrockText : nameTag;
        getMetadata().put(EntityDataTypes.NAME, far || shown == null ? "" : shown);
    }

    @Override
    public void tick() {
        if (++ticks % CHECK_TICKS != 0 || !valid) return;
        float max = (float) GeyserDisplayEntity.getExtension().getConfigManager().getTextMaxDistance();
        boolean nowFar = false;
        if (max > 0) {
            float limit = far ? max - MARGIN : max + MARGIN;
            nowFar = session.getPlayerEntity().getPosition().distanceSquared(where()) > limit * limit;
        }
        if (nowFar == far) return;
        far = nowFar;
        putName();
        updateBedrockMetadata();
    }

    // Where the text is: a rider's own position is where it was spawned (Java doesn't send it), so take its vehicle's
    private Vector3f where() {
        Entity root = this;
        for (int i = 0; i < 8 && root.getVehicle() != null; i++) root = root.getVehicle();
        return root == this ? bedrockPosition() : root.getPosition();
    }
}
