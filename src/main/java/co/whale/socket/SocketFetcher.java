package co.whale.socket;

import co.whale.packet.Packet;
import co.whale.util.Base64;
import lombok.*;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class SocketFetcher {

    private SocketFetcher instance;
    private SocketServer server;

    private Socket socket;
    private Thread thread;

    private ObjectInputStream input;
    private ObjectOutputStream output;

    @SneakyThrows
    public SocketFetcher(SocketServer server, Socket socket, String purpose) {

        this.instance = this;

        this.socket = socket;
        this.server = server;

        this.input = new ObjectInputStream(socket.getInputStream());
        this.output = new ObjectOutputStream(socket.getOutputStream());

        SocketFetcher instance = this;

        thread = new Thread(new Runnable() {

            @Override
            public void run() {

                while(true) {

                    try {
                        if(input.available() <= -1) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            continue;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {

                        Packet packet = (Packet) input.readObject();
                        server.fire(instance, packet);

                    } catch (Exception e) {

                        thread.stop();

                    }

                }

            }

        });

        thread.start();

    }

    public void send(Packet packet) {

        try {

            // output.writeInt(-1);
            // output.writeInt(-1);

            // output.writeUTF(Base64.toString(packet));
            // output.flush();

            //System.out.println("PASS1");

            output.writeUnshared(packet);
            output.flush();
            output.reset();

            //System.out.println("PASS4");

        } catch(Exception e) {
            e.printStackTrace();
        }

    }

}
