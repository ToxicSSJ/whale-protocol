package co.whale.context;

import co.whale.context.ServerManager;
import co.whale.packet.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class TemporalCollector<K extends Packet> {

    private Thread thread;

    private boolean collecting = false;

    private int expected;
    private long time;

    private ScheduledFuture future;
    private List<K> collected;

    public void start(ServerManager serverManager, int expected, long timeout) {

        this.time = 0;
        this.collecting = true;
        this.expected = expected;
        this.collected = new ArrayList<>();

        future = serverManager.getExecutorService().scheduleAtFixedRate(() -> {

            time += 100;

            if(collected.size() == expected || time >= timeout) {

                collecting = false;

                end(collected);
                future.cancel(true);

            }

        }, 100, 100, TimeUnit.MILLISECONDS);

    }

    public abstract void end(List<K> collected);

    public abstract boolean canCollect(K k);

    public boolean isInCollector(K k) {
        if(this.collected.contains(k))
            return true;
        return false;
    }

    public void save(K k) {
        if(collecting)
            this.collected.add(k);
    }

    public abstract Class<K> getPacketClass();

}
