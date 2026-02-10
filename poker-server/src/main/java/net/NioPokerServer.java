package net;

import common.*;
import game.GameEngine;
import players.Player;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import service.GameServiceHandler;

public class NioPokerServer implements Runnable {
    private final int port;
    private Selector selector;
    private final GameServiceHandler gameHandler = new GameServiceHandler();

    public NioPokerServer(int port) { this.port = port; }

    @Override
    public void run() {
        try {
            selector = Selector.open();
            ServerSocketChannel server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(port));
            server.configureBlocking(false);
            server.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (!key.isValid()) continue;
                    if (key.isAcceptable()) handleAccept(key);
                    else if (key.isReadable()) handleRead(key);
                    else if (key.isWritable()) handleWrite(key);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, new ClientState());
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ClientState state = (ClientState) key.attachment();
        state.readBf.clear();
        if (client.read(state.readBf) == -1) { key.cancel(); client.close(); return; }
        state.readBf.flip();
        String msg = StandardCharsets.UTF_8.decode(state.readBf).toString().trim();
        if (!msg.isEmpty()) processCommand(key, msg);
    }

    private void processCommand(SelectionKey key, String msg) {
        ClientState state = (ClientState) key.attachment();
        GameCommand cmd = CommandParser.parse(msg);
        String senderId = state.getPlayerId() != null ? state.getPlayerId() : String.valueOf(key.hashCode());

        GameServiceHandler.ServiceResult res = gameHandler.processCommand(senderId, state.getGameId(), cmd);

        if (res.targetGameId() != null && state.getGameId() == null) {
            state.setGameId(res.targetGameId());
            state.setPlayerId(senderId);
        }

        // Pdealing cards when START and RESTART
        if ((cmd.getType() == CommandType.START || cmd.getType() == CommandType.RESTART)
                && state.getGameId() != null && !res.message().startsWith("ERR")) {
            sendDealMessages(state.getGameId());
        }

        String response = res.message();
        if (response.contains("WELCOME") && response.contains("JOIN")) {
            String[] parts = response.split("\n");
            for (String p : parts) {
                if (p.startsWith("WELCOME")) sendMessage(key, p);
                else if (p.startsWith("JOIN")) broadcast(res.targetGameId(), p);
            }
        } else if (res.broadcast()) {
            broadcast(res.targetGameId(), response);
        } else {
            sendMessage(key, response);
        }

        if (state.getGameId() != null) broadcastTurn(state.getGameId());
    }

    // info to everyone
    private void broadcastTurn(String gameId) {
        GameEngine e = gameHandler.getEngine(gameId);
        if (e.getState() == game.GameState.LOBBY || e.getState() == game.GameState.END) return;
        Player active = e.getPlayers().get(e.getPlayersTurnIdx());
        broadcast(gameId, CommandParser.createMessage(CommandType.TURN, active.getId(), active.getName(), e.getState().name()));
    }

    // message to one player
    private void sendDealMessages(String gameId) {
        GameEngine engine = gameHandler.getEngine(gameId);
        for (SelectionKey k : selector.keys()) {
            if (k.isValid() && k.attachment() instanceof ClientState cs && gameId.equals(cs.getGameId())) {
                Player p = engine.getPlayers().stream().filter(pl -> pl.getId().equals(cs.getPlayerId())).findFirst().orElse(null);
                if (p != null) {
                    StringBuilder sb = new StringBuilder("DEAL");
                    p.getHand().getCards().forEach(c -> sb.append(" ").append(c.rank().name()).append("_").append(c.suit().name()));
                    sendMessage(k, sb.toString());
                }
            }
        }
    }

    private void broadcast(String gId, String m) {
        for (SelectionKey k : selector.keys()) {
            if (k.isValid() && k.attachment() instanceof ClientState cs && gId.equals(cs.getGameId())) sendMessage(k, m);
        }
    }

    private void sendMessage(SelectionKey k, String m) {
        ClientState s = (ClientState) k.attachment();
        s.writeQ.add(ByteBuffer.wrap((m.endsWith("\n") ? m : m + "\n").getBytes(StandardCharsets.UTF_8)));
        k.interestOps(k.interestOps() | SelectionKey.OP_WRITE);
    }

    private void handleWrite(SelectionKey k) throws IOException {
        SocketChannel c = (SocketChannel) k.channel();
        ClientState s = (ClientState) k.attachment();
        ByteBuffer b = s.writeQ.peek();
        if (b != null) { c.write(b); if (!b.hasRemaining()) s.writeQ.poll(); }
        if (s.writeQ.isEmpty()) k.interestOps(SelectionKey.OP_READ);
    }
}