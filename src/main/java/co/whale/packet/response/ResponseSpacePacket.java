package co.whale.packet.response;

import co.whale.context.entry.WhaleServer;
import co.whale.context.entry.WhaleSpace;
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
public class ResponseSpacePacket extends Packet implements Serializable {

    private String clientRequester;
    private String originServer;

    private String searchId;
    private OriginType originType;

    private Set<String> requested = new HashSet<>();
    private Set<WhaleSpace> spaces = new HashSet<>();

}
