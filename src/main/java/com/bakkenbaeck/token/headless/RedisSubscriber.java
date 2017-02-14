package com.bakkenbaeck.token.headless;

import com.bakkenbaeck.token.headless.rpc.HeadlessRPC;
import com.bakkenbaeck.token.headless.rpc.entities.HeadlessRPCRequest;
import com.bakkenbaeck.token.headless.signal.AttachmentInvalidException;
import com.bakkenbaeck.token.headless.signal.Manager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.whispersystems.signalservice.api.push.exceptions.EncapsulatedExceptions;
import redis.clients.jedis.JedisPubSub;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

class RedisSubscriber extends JedisPubSub {
   // private static Logger logger = Logger.getLogger(RedisSubscriber.class);
    private Manager manager;
    private ObjectMapper mapper;
    private HeadlessRPC rpc;

    public RedisSubscriber(HeadlessRPC rpc, Manager manager) {
        this.rpc = rpc;
        this.manager = manager;
        mapper = new ObjectMapper();
    }

    @Override
    public void onMessage(String channel, String message) {
        System.out.println("Redis message received on channel "+channel+": "+message);
        if (channel.equals(this.manager.getUsername())) {
            try {
                SignalWrappedSOFA wrapped = mapper.readValue(message, SignalWrappedSOFA.class);
                if (!wrapped.getSender().equals(manager.getUsername())) {
                    System.out.println("Ignoring: "+wrapped.getSender()+" is not "+manager.getUsername());
                    return;
                }
                System.out.println(wrapped.getSofa());
                List<String> attachments = Arrays.asList();
                try {
                    manager.sendMessage(wrapped.getSofa(), attachments, wrapped.getRecipient());
                } catch (EncapsulatedExceptions encapsulatedExceptions) {
                    encapsulatedExceptions.printStackTrace();
                } catch (AttachmentInvalidException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (channel.equals(this.manager.getUsername()+"_rpc_request")) {
            try {
                HeadlessRPCRequest request = mapper.readValue(message, HeadlessRPCRequest.class);
                rpc.handleRequest(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {

    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
          System.out.println("onSubcribe "+channel);
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        System.out.println("onUnSubcribe "+channel);
    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {

    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {

    }
}