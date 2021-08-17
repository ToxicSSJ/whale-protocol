package co.whale.packet.request;

import co.whale.packet.Packet;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@Builder
@ToString
public class RequestDownloadFilePacket extends Packet implements Serializable {

    private String clientRequester;
    private String originServer;

    private String sha1;
    private String localPath;

}
