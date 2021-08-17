package co.whale.socket;

import co.whale.events.SocketServerEvent;
import co.whale.packet.Packet;

import lombok.Data;
import lombok.SneakyThrows;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Data
public class SocketServer {

    private SocketServer instance;

    private int port;

    private ServerSocket socket;
    private Thread thread;

    private LinkedList<SocketFetcher> clients;
    private Map<Class, SocketServerEvent> events;

    private ObjectInputStream input;
    private ObjectOutputStream output;

    public SocketServer(int port) {

        this.instance = this;

        this.port = port;

        this.clients = new LinkedList<>();
        this.events = new HashMap<>();

    }

    @SneakyThrows
    public void connect() {

        this.socket = new ServerSocket(port);

        thread = new Thread(new Runnable() {

            @Override
            @SneakyThrows
            public void run() {

                while(true) {

                    Socket client = socket.accept();
                    clients.add(new SocketFetcher(instance, client, "client"));

                }

            }

        });

        thread.start();

    }

    public void fire(SocketFetcher fetcher, Packet packet) {

        // System.out.println("WTF " + packet);

        SocketServerEvent trigger = getTrigger(packet);

        // System.out.println("WTF1");

        if(trigger == null)
            return;

        // System.out.println("WTF2");

        trigger.fire(fetcher, packet);

        // System.out.println("WTF3");

    }

    public SocketServerEvent getTrigger(Packet packet) {
        if(events.containsKey(packet.getClass()))
            return events.get(packet.getClass());
        return null;
    }

    public <E extends Packet> void listen(Class<E> clazz, SocketServerEvent<E> event) {
        events.put(clazz, event);
    }

}
