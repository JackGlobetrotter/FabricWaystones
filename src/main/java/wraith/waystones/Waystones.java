package wraith.waystones;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import wraith.waystones.registries.*;

import java.util.HashSet;

public class Waystones implements ModInitializer {

    public static final String MOD_ID = "waystones";
    public static WaystoneDatabase WAYSTONE_DATABASE;
    public static boolean GLOBAL_DISCOVER = false;

    @Override
    public void onInitialize() {
        GLOBAL_DISCOVER = Config.loadConfig();
        BlockRegistry.registerBlocks();
        BlockEntityRegistry.registerBlockEntities();
        ItemRegistry.registerItems();
        CustomScreenHandlerRegistry.registerScreenHandlers();
        registerEvents();
        registerPacketHandlers();
    }


    private void registerPacketHandlers() {
        ServerSidePacketRegistry.INSTANCE.register(new Identifier(MOD_ID, "rename_waystone"), (packetContext, attachedData) -> {
            CompoundTag tag = attachedData.readCompoundTag();
            String oldName = tag.getString("old_name");
            String newName = tag.getString("new_name");
            if (WAYSTONE_DATABASE.containsWaystone(oldName)) {
                WAYSTONE_DATABASE.renameWaystone(oldName, newName);
            }
        });
        ServerSidePacketRegistry.INSTANCE.register(new Identifier(MOD_ID, "teleport_player"), (packetContext, attachedData) -> {
            CompoundTag tag = attachedData.readCompoundTag();
            teleportPlayer(packetContext.getPlayer(), tag);
        });
    }

    public static void teleportPlayer(PlayerEntity player, CompoundTag tag) {
        String world = tag.getString("WorldName");
        String facing = tag.getString("Facing");
        if (WAYSTONE_DATABASE.getWorld(world) == null) {
            return;
        }
        float x = 0;
        float z = 0;
        float yaw = player.yaw;
        switch (facing) {
            case "north":
                x = 0.5f;
                z = -0.5f;
                yaw = 0;
                break;
            case "south":
                x = 0.5f;
                z = 1.5f;
                yaw = 180;
                break;
            case "east":
                x = 1.5f;
                z = 0.5f;
                yaw = 90;
                break;
            case "west":
                x = -0.5f;
                z = 0.5f;
                yaw = 270;
                break;
        }
        int[] coords = tag.getIntArray("Coordinates");
        ((ServerPlayerEntity)player).teleport(WAYSTONE_DATABASE.getWorld(world), coords[0] + x, coords[1], coords[2] + z, yaw, 0);
    }

    public void registerEvents() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            WAYSTONE_DATABASE = new WaystoneDatabase(server);
            WAYSTONE_DATABASE.loadOrSaveWaystones(false);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
                    dispatcher.register(CommandManager.literal("waystones")
                            .then(CommandManager.literal("reload"))
                            .requires(source -> source.hasPermissionLevel(1))
                            .executes(context -> {
                                GLOBAL_DISCOVER = Config.loadConfig();
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player != null) {
                                    player.sendMessage(new LiteralText("§6[§eWaystones§6] §3has successfully been reloaded!"), false);
                                }

                                return 1;
                            })
                    );
                }
        );
    }

}