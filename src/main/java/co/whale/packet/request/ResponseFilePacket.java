package co.whale.packet.request;

import co.whale.context.entry.OceanFile;
import co.whale.packet.OriginType;
import co.whale.packet.Packet;
import co.whale.packet.request.RequestFilePacket;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@ToString
public class ResponseFilePacket extends Packet implements Serializable {

    private String clientRequester;
    private String originServer;

    private String searchId;

    private String fileName;
    private OriginType originType;

    private Set<String> requested = new HashSet<>();
    private Set<OceanFile> files = new HashSet<>();

}
