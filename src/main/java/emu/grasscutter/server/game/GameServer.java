package emu.grasscutter.server.game;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import emu.grasscutter.GenshinConstants;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.commands.CommandMap;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.game.GenshinPlayer;
import emu.grasscutter.game.dungeons.DungeonManager;
import emu.grasscutter.game.gacha.GachaManager;
import emu.grasscutter.game.managers.ChatManager;
import emu.grasscutter.game.managers.InventoryManager;
import emu.grasscutter.game.managers.MultiplayerManager;
import emu.grasscutter.game.shop.ShopManager;
import emu.grasscutter.net.packet.PacketHandler;
import emu.grasscutter.net.proto.SocialDetailOuterClass.SocialDetail;
import emu.grasscutter.netty.MihoyoKcpServer;

public final class GameServer extends MihoyoKcpServer {
	private final InetSocketAddress address;
	private final GameServerPacketHandler packetHandler;

	private final Map<Integer, GenshinPlayer> players;
	
	private final ChatManager chatManager;
	private final InventoryManager inventoryManager;
	private final GachaManager gachaManager;
	private final ShopManager shopManager;
	private final MultiplayerManager multiplayerManager;
	private final DungeonManager dungeonManager;
	private final CommandMap commandMap;
	
	public GameServer(InetSocketAddress address) {
		super(address);
		
		this.setServerInitializer(new GameServerInitializer(this));
		this.address = address;
		this.packetHandler = new GameServerPacketHandler(PacketHandler.class);
		this.players = new ConcurrentHashMap<>();
		
		this.chatManager = new ChatManager(this);
		this.inventoryManager = new InventoryManager(this);
		this.gachaManager = new GachaManager(this);
		this.shopManager = new ShopManager(this);
		this.multiplayerManager = new MultiplayerManager(this);
		this.dungeonManager = new DungeonManager(this);
		this.commandMap = new CommandMap(true);
		
		// Schedule game loop.
		Timer gameLoop = new Timer();
		gameLoop.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					onTick();
				} catch (Exception e) {
					Grasscutter.getLogger().error("An error occurred during game update.", e);
				}
			}
		}, new Date(), 1000L);
		
		// Hook into shutdown event.
		Runtime.getRuntime().addShutdownHook(new Thread(this::onServerShutdown));
	}
	
	public GameServerPacketHandler getPacketHandler() {
		return packetHandler;
	}

	public Map<Integer, GenshinPlayer> getPlayers() {
		return players;
	}

	public ChatManager getChatManager() {
		return chatManager;
	}

	public InventoryManager getInventoryManager() {
		return inventoryManager;
	}

	public GachaManager getGachaManager() {
		return gachaManager;
	}
	
	public ShopManager getShopManager() {
		return shopManager;
	}

	public MultiplayerManager getMultiplayerManager() {
		return multiplayerManager;
	}
	
	public DungeonManager getDungeonManager() {
		return dungeonManager;
	}
	
	public CommandMap getCommandMap() {
		return this.commandMap;
	}
	
	public void registerPlayer(GenshinPlayer player) {
		getPlayers().put(player.getId(), player);
	}

	public GenshinPlayer getPlayerById(int id) {
		return this.getPlayers().get(id);
	}
	
	public GenshinPlayer forceGetPlayerById(int id) {
		// Console check
		if (id == GenshinConstants.SERVER_CONSOLE_UID) {
			return null;
		}
		
		// Get from online players
		GenshinPlayer player = this.getPlayerById(id);
		
		// Check database if character isnt here
		if (player == null) {
			player = DatabaseHelper.getPlayerById(id);
		}
		
		return player;
	}
	
	public SocialDetail.Builder getSocialDetailById(int id) {
		// Get from online players
		GenshinPlayer player = this.forceGetPlayerById(id);
	
		if (player == null) {
			return null;
		}
		
		return player.getSocialDetail();
	}
	
	public Account getAccountByName(String username) {
		Optional<GenshinPlayer> playerOpt = getPlayers().values().stream().filter(player -> player.getAccount().getUsername().equals(username)).findFirst();
		if (playerOpt.get() != null) {
			return playerOpt.get().getAccount();
		}
		return DatabaseHelper.getAccountByName(username);
	}
	
	public void onTick() throws Exception {
		for (GenshinPlayer player : this.getPlayers().values()) {
			player.onTick();
		}
	}

	@Override
	public void onStartFinish() {
		Grasscutter.getLogger().info("Game Server started on port " + address.getPort());
	}
	
	public void onServerShutdown() {
		// Kick and save all players
		List<GenshinPlayer> list = new ArrayList<>(this.getPlayers().size());
		list.addAll(this.getPlayers().values());
		
		for (GenshinPlayer player : list) {
			player.getSession().close();
		}
	}
}
