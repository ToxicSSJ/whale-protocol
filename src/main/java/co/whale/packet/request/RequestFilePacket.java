package co.whale.packet.request;

import co.whale.packet.OriginType;
import co.whale.packet.Packet;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@ToString
public class RequestFilePacket extends Packet implements Serializable {

    private String clientRequester;

    private String originServer;
    private String serverHostname;
    private int serverPort;

    private String searchId;

    private String fileName;
    private OriginType originType;

    private Set<String> requested = new HashSet<>();

}
