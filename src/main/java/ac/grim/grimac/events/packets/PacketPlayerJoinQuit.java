package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PacketPlayerJoinQuit extends PacketListenerAbstract {
    @Override
    public void onUserLogin(UserLoginEvent event) {
        Player player = (Player) event.getPlayer();
        LogUtil.info("Netty pipeline is: " + ChannelHelper.pipelineHandlerNamesAsString(event.getUser().getChannel()));
        if (player.hasPermission("grim.alerts") && GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("alerts.enable-on-join", true)) {
            GrimAPI.INSTANCE.getAlertManager().toggle(player);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        GrimAPI.INSTANCE.getPlayerDataManager().remove(event.getUser());

        Player player = Bukkit.getPlayer(event.getUser().getProfile().getUUID());
        if (player != null) {
            GrimAPI.INSTANCE.getAlertManager().handlePlayerQuit(player);
        }
    }
}
