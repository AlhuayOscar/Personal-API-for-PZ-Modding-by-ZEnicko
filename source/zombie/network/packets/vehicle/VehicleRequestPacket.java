// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class VehicleRequestPacket extends VehicleID implements INetworkPacket {
    @JSONField
    protected short flag;

    @Override
    public void setData(Object... values) {
        this.setID((Short)values[0]);
        this.flag = (Short)values[1];
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.flag = b.getShort();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putShort(this.flag);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.vehicle.connectionState[connection.getIndex()] == null) {
            this.vehicle.connectionState[connection.getIndex()] = new BaseVehicle.ServerVehicleState();
        }

        if (this.flag == 16384) {
            if (!connection.isRelevantTo(this.vehicle.getX(), this.vehicle.getY())) {
                INetworkPacket.send(connection, PacketTypes.PacketType.VehicleRemove, this.vehicle);
            }
        } else {
            BaseVehicle.ServerVehicleState var10000 = this.vehicle.connectionState[connection.getIndex()];
            var10000.flags = (short)(var10000.flags | this.flag);
        }
    }
}
