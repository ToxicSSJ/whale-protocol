package co.whale.events;

import co.whale.packet.Packet;

public interface SocketClientEvent<T extends Packet> {

    void fire(T packet);

}
