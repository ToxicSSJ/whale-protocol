package co.whale.packet.request;

import co.whale.packet.Packet;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@Builder
@ToString
public class RequestStatusPacket extends Packet implements Serializable {

    private String originServer;
    private String serverHostname;
    private int serverPort;

}
