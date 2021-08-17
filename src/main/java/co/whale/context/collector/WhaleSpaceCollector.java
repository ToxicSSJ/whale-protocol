package co.whale.context.collector;

import co.whale.context.TemporalCollector;
import co.whale.packet.request.RequestFilePacket;
import co.whale.packet.request.RequestSpacePacket;
import co.whale.packet.request.ResponseFilePacket;
import co.whale.packet.response.ResponseSpacePacket;

import java.util.List;
import java.util.function.Consumer;

public class WhaleSpaceCollector extends TemporalCollector<ResponseSpacePacket> {

    private RequestSpacePacket requestFilePacket;
    private Consumer<List<ResponseSpacePacket>> consumer;

    public WhaleSpaceCollector(RequestSpacePacket request, Consumer<List<ResponseSpacePacket>> consumer) {
        this.requestFilePacket = request;
        this.consumer = consumer;
    }

    @Override
    public void end(List<ResponseSpacePacket> collected) {
        consumer.accept(collected);
    }

    @Override
    public boolean canCollect(ResponseSpacePacket responseFilePacket) {
        return responseFilePacket.getSearchId().equals(requestFilePacket.getSearchId());
    }

    @Override
    public Class<ResponseSpacePacket> getPacketClass() {
        return ResponseSpacePacket.class;
    }

}
