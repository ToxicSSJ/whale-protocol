package co.whale.packet.response;

import co.whale.packet.Packet;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class ResponseStatusPacket extends Packet implements Serializable {

    private String id;
    private String type;

    private long maxSize;
    private long size;

    private String hostname;
    private int port;

}
