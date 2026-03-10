// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import zombie.VirtualZombieManager;
import zombie.characters.Capability;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.ServerMap;
import zombie.network.packets.INetworkPacket;
import zombie.network.statistics.data.GameStatistic;
import zombie.popman.NetworkZombiePacker;
import zombie.popman.ZombieCountOptimiser;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class ZombieDeletePacket implements INetworkPacket {
    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        int zombieForDelete = b.getShort();

        for (int i = 0; i < zombieForDelete; i++) {
            short zombieID = b.getShort();
            IsoZombie z = ServerMap.instance.zombieMap.get(zombieID);
            if (z != null && (connection.getRole().hasCapability(Capability.ManipulateZombie) || z.getOwner() == connection)) {
                NetworkZombiePacker.getInstance().deleteZombie(z);
                VirtualZombieManager.instance.removeZombieFromWorld(z);
                GameStatistic.getInstance().zombiesCulled.increase();
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        synchronized (ZombieCountOptimiser.zombiesForDelete) {
            int zombiesForDeleteCount = ZombieCountOptimiser.zombiesForDelete.size();
            b.putShort(zombiesForDeleteCount);

            for (int k = 0; k < zombiesForDeleteCount; k++) {
                b.putShort(ZombieCountOptimiser.zombiesForDelete.get(k).onlineId);
            }

            ZombieCountOptimiser.zombiesForDelete.clear();
        }
    }
}
