package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.enums.Pose;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateHealth;
import org.bukkit.util.Vector;

import java.util.List;

public class PacketPlayerRespawn extends PacketListenerAbstract {

    public PacketPlayerRespawn() {
        super(PacketListenerPriority.HIGH);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.UPDATE_HEALTH) {
            WrapperPlayServerUpdateHealth health = new WrapperPlayServerUpdateHealth(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            List<Runnable> tasks = event.getPostTasks();
            tasks.add(player::sendTransaction);

            if (health.getFood() == 20) { // Split so transaction before packet
                player.latencyUtils.addRealTimeTask(player.lastTransactionReceived.get(), () -> player.food = 20);
            } else { // Split so transaction after packet
                player.latencyUtils.addRealTimeTask(player.lastTransactionReceived.get() + 1, () -> player.food = health.getFood());
            }

            if (health.getHealth() <= 0) {
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.isDead = true);
            } else {
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.isDead = false);
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayServerJoinGame joinGame = new WrapperPlayServerJoinGame(event);
            player.gamemode = joinGame.getGameMode();

            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_17)) return;
            player.compensatedWorld.setDimension(joinGame.getDimension().getType().getName(), event.getUser());
        }

        if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
            WrapperPlayServerRespawn respawn = new WrapperPlayServerRespawn(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            List<Runnable> tasks = event.getPostTasks();
            tasks.add(player::sendTransaction);

            // Force the player to accept a teleport before respawning
            player.getSetbackTeleportUtil().hasAcceptedSpawnTeleport = false;

            // TODO: What does keep all metadata do?
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
                // Client creates a new entity on respawn
                player.isDead = false;
                player.isSneaking = false;
                player.lastOnGround = false;
                player.packetStateData.packetPlayerOnGround = false; // If somewhere else pulls last ground to fix other issues
                player.lastSprintingForSpeed = false; // This is reverted even on 1.18 clients
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14)) { // 1.14+ players send a packet for this, listen for it instead
                    player.isSprinting = false;
                    // TODO: This isn't right, what does viaversion do with keep all metadata??
                    player.compensatedEntities.hasSprintingAttributeEnabled = false;
                }
                player.pose = Pose.STANDING;
                player.clientVelocity = new Vector();
                player.gamemode = respawn.getGameMode();
                player.compensatedWorld.setDimension(respawn.getDimension().getType().getName(), event.getUser());
            });
        }
    }
}
