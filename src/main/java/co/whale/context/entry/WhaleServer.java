package co.whale.context.entry;

import co.whale.Main;
import co.whale.context.ServerManager;
import co.whale.packet.request.RequestStatusPacket;
import co.whale.packet.response.ResponseStatusPacket;
import co.whale.socket.SocketClient;

import lombok.Data;
import lombok.SneakyThrows;

@Data
public class WhaleServer {

    private ServerManager serverManager;

    private String generatedId = "Loading...";
    private SocketClient socketClient;

    private String type;
    private String key = "none";

    private String hostname;
    private int port;

    private long maxSize;
    private long size;

    private boolean online;

    public WhaleServer(String hostname, int port, String key, ServerManager manager) {

        this.serverManager = manager;

        this.hostname = hostname;
        this.port = port;
        this.key = key;

    }

    public void update() {

        if(socketClient == null) {

            this.generatedId = "Loading...";
            this.online = false;

            create();

        }

        if(!socketClient.isConnected()) {

            socketClient.connect();
            return;

        }



        socketClient.send(RequestStatusPacket.builder()
                .originServer(Main.getGeneratedNodeId())
                .serverHostname(serverManager.getHostname())
                .serverPort(serverManager.getPort())
                .build());

    }

    @SneakyThrows
    private void create() {

        socketClient = new SocketClient(hostname, port);

        socketClient.listen(ResponseStatusPacket.class, packet -> {

            this.generatedId = packet.getId();
            this.type = packet.getType();

            this.hostname = packet.getHostname();
            this.port = packet.getPort();

            this.maxSize = packet.getMaxSize();
            this.size = packet.getSize();

            this.online = true;

        });

    }

}
