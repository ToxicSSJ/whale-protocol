package co.whale.context.collector;

import co.whale.context.TemporalCollector;
import co.whale.packet.request.RequestFilePacket;
import co.whale.packet.request.ResponseFilePacket;

import java.util.List;
import java.util.function.Consumer;

public class OceanFileCollector extends TemporalCollector<ResponseFilePacket> {

    private RequestFilePacket requestFilePacket;
    private Consumer<List<ResponseFilePacket>> consumer;

    public OceanFileCollector(RequestFilePacket request, Consumer<List<ResponseFilePacket>> consumer) {
        this.requestFilePacket = request;
        this.consumer = consumer;
    }

    @Override
    public void end(List<ResponseFilePacket> collected) {
        consumer.accept(collected);
    }

    @Override
    public boolean canCollect(ResponseFilePacket responseFilePacket) {
        return responseFilePacket.getSearchId().equals(requestFilePacket.getSearchId());
    }

    @Override
    public Class<ResponseFilePacket> getPacketClass() {
        return ResponseFilePacket.class;
    }

}
