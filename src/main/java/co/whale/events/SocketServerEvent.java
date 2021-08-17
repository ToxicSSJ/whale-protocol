package co.whale.events;

import co.whale.packet.Packet;
import co.whale.socket.SocketFetcher;

public interface SocketServerEvent<T extends Packet> {

    void fire(SocketFetcher fetcher, T packet);

}
