package co.whale.socket;

import co.whale.events.SocketClientEvent;
import co.whale.packet.Packet;
import co.whale.util.Base64;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@Data
public class SocketClient {

    private String url;
    private int port;

    private Socket socket;
    private Thread thread;

    private Map<Class, SocketClientEvent> events;

    private ObjectInputStream input;
    private ObjectOutputStream output;

    private boolean connected = false;

    public SocketClient(String url, int port) {

        this.url = url;
        this.port = port;

        this.events = new HashMap<>();

    }

    @SneakyThrows
    public void connect() {

        this.socket = new Socket(url, port);

        this.output = new ObjectOutputStream(socket.getOutputStream());
        this.input = new ObjectInputStream(socket.getInputStream());

        connected = true;

        thread = new Thread(() -> {

            while(true) {

                try {

                    if(input.available() <= -1)
                        continue;

                    Packet packet = (Packet) input.readObject();
                    fire(packet);

                } catch (Exception e) { }

            }

        });

        thread.start();

    }

    @SneakyThrows
    public void send(Packet packet) {
        try {

            if(output == null)
                return;

            // output.writeInt(-1);
            // output.writeInt(-1);

            // output.writeUTF(Base64.toString(packet));
            output.writeUnshared(packet);
            output.flush();
            output.reset();

        } catch(Exception e) {

            if(!connected) {

                e.printStackTrace();

            }

            connected = false;
            socket.close();

            thread.stop();

            e.printStackTrace();

        }
    }

    public void fire(Packet packet) {

        SocketClientEvent trigger = getTrigger(packet);

        if(trigger == null)
            return;

        trigger.fire(packet);

    }

    public SocketClientEvent getTrigger(Packet packet) {
        if(events.containsKey(packet.getClass()))
            return events.get(packet.getClass());
        return null;
    }

    public <E extends Packet> void listen(Class<E> clazz, SocketClientEvent<E> event) {
        events.put(clazz, event);
    }

}
