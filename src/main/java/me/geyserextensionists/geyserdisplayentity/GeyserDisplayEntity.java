package me.geyserextensionists.geyserdisplayentity;

import me.geyserextensionists.geyserdisplayentity.entity.BlockDisplayEntity;
import me.geyserextensionists.geyserdisplayentity.entity.FurnitureFrameEntity;
import me.geyserextensionists.geyserdisplayentity.entity.ItemDisplayEntity;
import me.geyserextensionists.geyserdisplayentity.entity.SlotDisplayEntity;
import me.geyserextensionists.geyserdisplayentity.managers.ConfigManager;
import me.geyserextensionists.geyserdisplayentity.util.BlockShapes;
import me.geyserextensionists.geyserdisplayentity.util.DisplayTransforms;
import me.geyserextensionists.geyserdisplayentity.util.EntityUtils;
import me.geyserextensionists.geyserdisplayentity.util.FileConfiguration;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.*;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;

public class GeyserDisplayEntity implements Extension {

    private static GeyserDisplayEntity extension;

    private ConfigManager configManager;

    private static BedrockEntityDefinition ITEM_DISPLAY_BEDROCK;
    private static BedrockEntityDefinition BLOCK_DISPLAY_BEDROCK;

    private static VanillaEntityType<ItemDisplayEntity> ITEM_DISPLAY;
    private static VanillaEntityType<BlockDisplayEntity> BLOCK_DISPLAY;

    public static final Integer MAX_VALUE = 1000000;
    public static final Integer MIN_VALUE = -1000000;

    private static final String PACK_FILE = "GeyserDisplayEntityPack.mcpack";

    @Subscribe
    public void onLoad(GeyserPreInitializeEvent event) {
        extension = this;
        loadManagers();

        try {
            BlockShapes.load(getClass().getClassLoader());
        } catch (IOException err) {
            logger().error("Couldn't load the block renderer's shapes; block displays fall back to falling blocks", err);
        }
        if (configManager.isDrawItemFrames()) loadDisplayTransforms();
    }

    // Written by the Bedrock converter next to the item mappings; copy it into the extension's folder
    private void loadDisplayTransforms() {
        try {
            Path file = dataFolder().resolve("display_transforms.json");
            DisplayTransforms.load(file);
            logger().info(Files.exists(file)
                    ? "Loaded " + DisplayTransforms.size() + " item display transforms for furniture in item frames"
                    : "No display_transforms.json in " + dataFolder() + "; furniture in item frames shows as Geyser frames");
        } catch (IOException err) {
            logger().error("Couldn't load display_transforms.json; furniture in item frames shows as Geyser frames", err);
        }
    }

    @Subscribe
    public void onDefineEntities(GeyserDefineEntitiesEvent event) {
        //TODO loop through entity type hashmap

        ITEM_DISPLAY_BEDROCK = EntityUtils.findOrRegisterCustomDefinition(this, event, Identifier.of("geyser:item_display"));
        BLOCK_DISPLAY_BEDROCK = EntityUtils.findOrRegisterCustomDefinition(this, event, Identifier.of("geyser:block_display"));
    }

    @Subscribe
    public void onEntityPropertiesEvent(GeyserDefineEntityPropertiesEvent event) {
        try {
            registerDisplayProperties(event, Identifier.of("geyser:item_display"));
            registerBlockRigProperties(event, Identifier.of("geyser:item_display"));
            registerDisplayProperties(event, Identifier.of("geyser:block_display"));
            registerBlockRigProperties(event, Identifier.of("geyser:block_display"));

            EntityTypeBase<Entity> entityBase = EntityTypeDefinition.baseBuilder(Entity.class)
                    .addTranslator(MetadataTypes.BYTE, Entity::setFlags)
                    .addTranslator(MetadataTypes.INT, Entity::setAir) // Air/bubbles
                    .addTranslator(MetadataTypes.OPTIONAL_COMPONENT, Entity::setCustomName)
                    .addTranslator(MetadataTypes.BOOLEAN, Entity::setCustomNameVisible)
                    .addTranslator(MetadataTypes.BOOLEAN, Entity::setSilent)
                    .addTranslator(MetadataTypes.BOOLEAN, Entity::setGravity)
                    .addTranslator(MetadataTypes.POSE, (entity, entityMetadata) -> entity.setPose(entityMetadata.getValue()))
                    .addTranslator(MetadataTypes.INT, Entity::setFreezing)
                    .build();

            EntityTypeBase<SlotDisplayEntity> slotDisplayBase = EntityTypeBase.baseInherited(SlotDisplayEntity.class, entityBase)
                    .addTranslator(null) // Interpolation start ticks
                    .addTranslator(null) // Interpolation duration ID
                    .addTranslator(null) // Position/Rotation interpolation duration
                    .addTranslator(MetadataTypes.VECTOR3, SlotDisplayEntity::setTranslation) // Translation
                    .addTranslator(MetadataTypes.VECTOR3, SlotDisplayEntity::setScale) // Scale
                    .addTranslator(MetadataTypes.QUATERNION, SlotDisplayEntity::setLeftRotation) // Left rotation
                    .addTranslator(MetadataTypes.QUATERNION, SlotDisplayEntity::setRightRotation) // Right rotation
                    .addTranslator(MetadataTypes.BYTE, SlotDisplayEntity::setBillboard) // Billboard render constraints
                    .addTranslator(null) // Brightness override
                    .addTranslator(null) // View range
                    .addTranslator(null) // Shadow radius
                    .addTranslator(null) // Shadow strength
                    .addTranslator(null) // Width
                    .addTranslator(null) // Height
                    .addTranslator(null) // Glow color override
                    .build();

            BLOCK_DISPLAY = VanillaEntityType.inherited(BlockDisplayEntity::new, slotDisplayBase)
                    .type(EntityType.BLOCK_DISPLAY)
                    .height(configManager.getConfig().getFloat("general.height")).width(0.001f)
                    .bedrockDefinition(BLOCK_DISPLAY_BEDROCK)
                    .addTranslator(MetadataTypes.BLOCK_STATE, BlockDisplayEntity::setDisplayedBlockState)
                    .build(configManager.isDrawBlockDisplays());

            ITEM_DISPLAY = VanillaEntityType.inherited(ItemDisplayEntity::new, slotDisplayBase)
                    .type(EntityType.ITEM_DISPLAY)
                    .height(configManager.getConfig().getFloat("general.height")).width(0.001f)
                    .bedrockDefinition(ITEM_DISPLAY_BEDROCK)
                    .addTranslator(MetadataTypes.ITEM_STACK, ItemDisplayEntity::setDisplayedItem)
                    .addTranslator(MetadataTypes.BYTE, ItemDisplayEntity::setDisplayType)
                    .build(configManager.isDrawItemDisplays());

            // Geyser 2.12 b1278+ has no JAVA_ENTITY_TYPES registry: build() registers the definition itself
            // (BuiltInJavaRegistries.ENTITY_TYPE), so a display that isn't drawn is built with build(false)
            logger().info("Drawing item displays: " + configManager.isDrawItemDisplays() + ", block displays: " + configManager.isDrawBlockDisplays());
            if (configManager.isDrawItemFrames()) {
                // MineQuack: the furniture frames replaced Geyser's item frame through the removed registry
                logger().warning("draw.item-frames is not supported on this Geyser build; item frames keep Geyser's behaviour");
            }
        } catch (Throwable err) {
            logger().error("Error in load", err);
        }

        logger().info("Done");
    }

    @Subscribe
    public void onDefineResourcePacks(GeyserDefineResourcePacksEvent event) {
        Path pack = dataFolder().resolve(PACK_FILE);
        try (InputStream bundled = getClass().getClassLoader().getResourceAsStream(PACK_FILE)) {
            if (bundled == null) {
                logger().error(PACK_FILE + " is missing from the jar; displays won't render on Bedrock");
                return;
            }
            Files.createDirectories(dataFolder());
            Files.copy(bundled, pack, StandardCopyOption.REPLACE_EXISTING);
            event.register(ResourcePack.create(PackCodec.path(pack)), new ResourcePackOption<?>[0]);
        } catch (IOException err) {
            logger().error("Couldn't register " + PACK_FILE, err);
        }
    }

    @Subscribe
    public void onDefineCommand(GeyserDefineCommandsEvent event) {
        event.register(Command.builder(this)
                .name("reload")
                .source(CommandSource.class)
                .playerOnly(false)
                .description("GeyserDisplayEntity Reload Command")
                .permission("geyserdisplayentity.commands.reload")
                .executor((source, command, args) -> {
                    configManager.load();
                    if (configManager.isDrawItemFrames()) loadDisplayTransforms();
                    refreshDisplays();
                    source.sendMessage(configManager.getLang().getString("commands.geyserdisplayentity.reload.successfully-reloaded"));
                })
                .build());
    }

    private void registerDisplayProperties(GeyserDefineEntityPropertiesEvent event, Identifier entityIdentifier) {
        Collection<GeyserEntityProperty<?>> existing = event.properties(entityIdentifier);

        FileConfiguration entityConfig = configManager.getEntityTypesCache().get(entityIdentifier);
        for (Object entityKey : entityConfig.getConfigurationSection("properties").getRootNode().childrenMap().keySet()) {
            String entityString = entityKey.toString();
            FileConfiguration propertyConfig = entityConfig.getConfigurationSection("properties." + entityString);

            String propertyType = propertyConfig.getString("property-type");

            if (propertyType.equals("integer")) {
                EntityUtils.registerInteger(event, existing, entityIdentifier, propertyConfig.getString("id"), propertyConfig.getInt("min-value"), propertyConfig.getInt("max-value"), propertyConfig.getInt("default-value"));
            } else if (propertyType.equals("float")) {
                EntityUtils.registerFloat(event, existing, entityIdentifier, propertyConfig.getString("id"), propertyConfig.getInt("min-value"), propertyConfig.getInt("max-value"), propertyConfig.getFloat("default-value"));
            }
        }
    }

    // Properties of the block cube, which draws vanilla blocks with the full Java transform. Registered in code
    // rather than the Entities/*.yml files, which existing installs never re-copy. Bedrock allows 32 per entity:
    // these come to 29 on block displays and 30 on item displays with the yml ones
    private void registerBlockRigProperties(GeyserDefineEntityPropertiesEvent event, Identifier entityIdentifier) {
        Collection<GeyserEntityProperty<?>> existing = event.properties(entityIdentifier);

        // Only item displays switch between the item rig and the block cube
        if (entityIdentifier.equals(Identifier.of("geyser:item_display"))) {
            EntityUtils.registerInteger(event, existing, entityIdentifier, ItemDisplayEntity.BLOCK_MODE, 0, 1, 0);
        }
        EntityUtils.registerFloat(event, existing, entityIdentifier, ItemDisplayEntity.RIGHT_ROTATION_X, MIN_VALUE, MAX_VALUE, 0F);
        EntityUtils.registerFloat(event, existing, entityIdentifier, ItemDisplayEntity.RIGHT_ROTATION_Y, MIN_VALUE, MAX_VALUE, 0F);
        EntityUtils.registerFloat(event, existing, entityIdentifier, ItemDisplayEntity.RIGHT_ROTATION_Z, MIN_VALUE, MAX_VALUE, 0F);
        for (String box : ItemDisplayEntity.BOX) EntityUtils.registerFloat(event, existing, entityIdentifier, box, MIN_VALUE, MAX_VALUE, 0F);
        for (String face : ItemDisplayEntity.FACES) EntityUtils.registerInteger(event, existing, entityIdentifier, face, -1, MAX_VALUE, -1);
        EntityUtils.registerInteger(event, existing, entityIdentifier, ItemDisplayEntity.FLAGS, 0, 3, 0);
    }

    // Apply the reloaded settings to displays already in view
    private void refreshDisplays() {
        for (GeyserSession session : GeyserImpl.getInstance().getSessionManager().getAllSessions()) {
            session.executeInEventLoop(() -> {
                for (Entity entity : session.getEntityCache().getEntitiesUnsafe().values()) {
                    if (entity instanceof ItemDisplayEntity display) display.refresh();
                }
            });
        }
    }

    private void loadManagers() {
        this.configManager = new ConfigManager(this);
    }

    public static BedrockEntityDefinition itemDisplayDefinition() {
        return ITEM_DISPLAY_BEDROCK;
    }

    public static GeyserDisplayEntity getExtension() {
        return extension;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
