package org.Cry_Legende.sHViaTickFix;

import com.viaversion.viabackwards.protocol.v1_21_4to1_21_2.Protocol1_21_4To1_21_2;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.ProtocolManager;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.exception.InformativeException;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 26.x servers disconnect a player whose client sends two movement packets carrying a position between two client tick
 * ends (ServerGamePacketListenerImpl#receivedPositionThisTick), and log nothing. 1.21.2-1.21.3 clients send their own tick
 * ends but sometimes put two positions in one tick, mostly while the world loads after joining. ViaBackwards only covers
 * older clients, whose tick ends it emulates. This sends the missing tick end right before the second position, in the
 * first protocol a 1.21.2-1.21.3 client goes through.
 */
final class TickEndHook {

    private final SHViaTickFix plugin;
    private final AtomicLong added = new AtomicLong();
    private final AtomicLong connections = new AtomicLong();
    private volatile boolean active = true;

    TickEndHook(SHViaTickFix plugin) {
        this.plugin = plugin;
    }

    void install() {
        ProtocolManager manager = Via.getManager().getProtocolManager();
        // Handlers appended before the protocol registers its own would clash with them
        manager.completeMappingDataLoading(Protocol1_21_4To1_21_2.class);
        Protocol1_21_4To1_21_2 protocol = manager.getProtocol(Protocol1_21_4To1_21_2.class);
        if (protocol == null) {
            throw new IllegalStateException("ViaBackwards has no 1.21.4 -> 1.21.2 protocol");
        }

        protocol.appendServerbound(ServerboundPackets1_21_2.ACCEPT_TELEPORTATION, wrapper -> {
            TickState state = state(wrapper);
            if (state != null) {
                state.teleportReply = true;
            }
        });
        protocol.appendServerbound(ServerboundPackets1_21_2.MOVE_PLAYER_POS_ROT, wrapper -> position(wrapper, true));
        protocol.appendServerbound(ServerboundPackets1_21_2.MOVE_PLAYER_POS, wrapper -> position(wrapper, false));
        protocol.appendServerbound(ServerboundPackets1_21_2.CLIENT_TICK_END, wrapper -> {
            TickState state = state(wrapper);
            if (state != null) {
                state.positionThisTick = false;
            }
        });
    }

    void deactivate() {
        active = false;
    }

    long added() {
        return added.get();
    }

    long connections() {
        return connections.get();
    }

    private void position(PacketWrapper wrapper, boolean withRotation) throws InformativeException {
        TickState state = state(wrapper);
        if (state == null) {
            return;
        }
        // The pos_rot answering a teleport becomes the 26.3 teleport acceptance in ViaBackwards: not a movement for the server
        if (withRotation && state.teleportReply) {
            state.teleportReply = false;
            return;
        }
        if (state.positionThisTick) {
            // Goes through this protocol as well, so the CLIENT_TICK_END handler resets the flag before it is set again below
            wrapper.create(ServerboundPackets1_21_2.CLIENT_TICK_END).sendToServer(Protocol1_21_4To1_21_2.class, false);
            added.incrementAndGet();
            if (!state.reported) {
                state.reported = true;
                connections.incrementAndGet();
                plugin.logFix(wrapper.user().getProtocolInfo().getUsername());
            }
        }
        state.positionThisTick = true;
    }

    private TickState state(PacketWrapper wrapper) {
        if (!active || !plugin.fixEnabled()) {
            return null;
        }
        UserConnection user = wrapper.user();
        if (!user.getProtocolInfo().protocolVersion().equalTo(ProtocolVersion.v1_21_2)) {
            return null;
        }
        TickState state = user.get(TickState.class);
        if (state == null) {
            state = new TickState();
            user.put(state);
        }
        return state;
    }

    /** Per connection, only touched from the connection's own netty thread. */
    private static final class TickState implements StorableObject {
        private boolean positionThisTick;
        private boolean teleportReply;
        private boolean reported;
    }
}
