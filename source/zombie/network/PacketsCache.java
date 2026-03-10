// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.util.HashMap;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.packets.INetworkPacket;

public abstract class PacketsCache {
    private final HashMap<PacketTypes.PacketType, INetworkPacket> packets = new HashMap<>();

    protected PacketsCache() {
        for (PacketTypes.PacketType packetType : PacketTypes.PacketType.values()) {
            if (packetType.handler == null) {
                DebugType.Packet.warn("No packet handler for type: \"%s\"", packetType.name());
            } else {
                try {
                    this.packets.put(packetType, packetType.handler.getDeclaredConstructor().newInstance());
                } catch (Exception var6) {
                    DebugType.Packet.printException(var6, LogSeverity.Warning, "Error creating packet type: \"%s\"", packetType.name());
                }
            }
        }
    }

    public INetworkPacket getPacket(PacketTypes.PacketType packetType) {
        return this.packets.get(packetType);
    }
}
