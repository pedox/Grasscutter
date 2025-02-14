package emu.grasscutter.server.packet.recv;

import emu.grasscutter.game.entity.EntityClientGadget;
import emu.grasscutter.net.packet.Opcodes;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.proto.EvtCreateGadgetNotifyOuterClass.EvtCreateGadgetNotify;
import emu.grasscutter.net.packet.PacketHandler;
import emu.grasscutter.server.game.GameSession;

@Opcodes(PacketOpcodes.EvtCreateGadgetNotify)
public class HandlerEvtCreateGadgetNotify extends PacketHandler {
	
	@Override
	public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
		EvtCreateGadgetNotify notify = EvtCreateGadgetNotify.parseFrom(payload);
		
		// Dont handle in singleplayer
		if (!session.getPlayer().getWorld().isMultiplayer()) {
			return;
		}
		
		// Sanity check - dont add duplicate entities
		if (session.getPlayer().getWorld().getEntityById(notify.getEntityId()) != null) {
			return;
		}
		
		// Create entity and summon in world
		EntityClientGadget gadget = new EntityClientGadget(session.getPlayer().getWorld(), session.getPlayer(), notify);
		session.getPlayer().getWorld().onPlayerCreateGadget(gadget);
	}

}
