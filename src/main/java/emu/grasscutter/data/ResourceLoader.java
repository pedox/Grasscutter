package emu.grasscutter.data;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import emu.grasscutter.utils.Utils;
import org.reflections.Reflections;

import com.google.gson.reflect.TypeToken;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.custom.AbilityEmbryoEntry;
import emu.grasscutter.data.custom.OpenConfigEntry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public class ResourceLoader {

	public static List<Class<?>> getResourceDefClasses() {
		Reflections reflections = new Reflections(ResourceLoader.class.getPackage().getName());
		Set<?> classes = reflections.getSubTypesOf(GenshinResource.class);

		List<Class<?>> classList = new ArrayList<>(classes.size());
		classes.forEach(o -> {
			Class<?> c = (Class<?>) o;
			if (c.getAnnotation(ResourceType.class) != null) {
				classList.add(c);
			}
		});

		classList.sort((a, b) -> b.getAnnotation(ResourceType.class).loadPriority().value() - a.getAnnotation(ResourceType.class).loadPriority().value());

		return classList;
	}
	
	public static void loadAll() {
		// Load ability lists
		loadAbilityEmbryos();
		loadOpenConfig();
		// Load resources
		loadResources();
		// Process into depots
		GenshinDepot.load();
		// Custom - TODO move this somewhere else
		try {
			GenshinData.getAvatarSkillDepotDataMap().get(504).setAbilities(
				new AbilityEmbryoEntry(
					"", 
					new String[] {
						"Avatar_PlayerBoy_ExtraAttack_Wind",
						"Avatar_Player_UziExplode_Mix",
						"Avatar_Player_UziExplode",
						"Avatar_Player_UziExplode_Strike_01",
						"Avatar_Player_UziExplode_Strike_02",
						"Avatar_Player_WindBreathe",
						"Avatar_Player_WindBreathe_CameraController"
					}
			));
			GenshinData.getAvatarSkillDepotDataMap().get(704).setAbilities(
				new AbilityEmbryoEntry(
					"", 
					new String[] {
						"Avatar_PlayerGirl_ExtraAttack_Wind",
						"Avatar_Player_UziExplode_Mix",
						"Avatar_Player_UziExplode",
						"Avatar_Player_UziExplode_Strike_01",
						"Avatar_Player_UziExplode_Strike_02",
						"Avatar_Player_WindBreathe",
						"Avatar_Player_WindBreathe_CameraController"
					}
			));
		} catch (Exception e) {
			Grasscutter.getLogger().error("Error loading abilities", e);
		}
	}

	public static void loadResources() {
		for (Class<?> resourceDefinition : getResourceDefClasses()) {
			ResourceType type = resourceDefinition.getAnnotation(ResourceType.class);

			if (type == null) {
				continue;
			}

			@SuppressWarnings("rawtypes")
			Int2ObjectMap map = GenshinData.getMapByResourceDef(resourceDefinition);

			if (map == null) {
				continue;
			}

			try {
				loadFromResource(resourceDefinition, type, map);
			} catch (Exception e) {
				Grasscutter.getLogger().error("Error loading resource file: " + Arrays.toString(type.name()), e);
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	protected static void loadFromResource(Class<?> c, ResourceType type, Int2ObjectMap map) throws Exception {
		for (String name : type.name()) {
			loadFromResource(c, name, map);
		}
		Grasscutter.getLogger().info("Loaded " + map.size() + " " + c.getSimpleName() + "s.");
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected static void loadFromResource(Class<?> c, String fileName, Int2ObjectMap map) throws Exception {
		try (FileReader fileReader = new FileReader(Grasscutter.getConfig().RESOURCE_FOLDER + "ExcelBinOutput/" + fileName)) {
			List list = Grasscutter.getGsonFactory().fromJson(fileReader, TypeToken.getParameterized(Collection.class, c).getType());

			for (Object o : list) {
				GenshinResource res = (GenshinResource) o;
				res.onLoad();
				map.put(res.getId(), res);
			}
		}
	}

	private static void loadAbilityEmbryos() {
		// Read from cached file if exists
		File embryoCache = new File(Grasscutter.getConfig().DATA_FOLDER + "AbilityEmbryos.json");
		List<AbilityEmbryoEntry> embryoList = null;
		
		if (embryoCache.exists()) {
			// Load from cache
			try (FileReader fileReader = new FileReader(embryoCache)) {
				embryoList = Grasscutter.getGsonFactory().fromJson(fileReader, TypeToken.getParameterized(Collection.class, AbilityEmbryoEntry.class).getType());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// Load from BinOutput
			Pattern pattern = Pattern.compile("(?<=ConfigAvatar_)(.*?)(?=.json)");
			
			embryoList = new LinkedList<>();
			File folder = new File(Utils.toFilePath(Grasscutter.getConfig().RESOURCE_FOLDER + "BinOutput/Avatar/"));
			File[] files = folder.listFiles();
			if(files == null) {
				Grasscutter.getLogger().error("Error loading ability embryos: no files found in " + folder.getAbsolutePath());
				return;
			}
			
			for (File file : files) {
				AvatarConfig config;
				String avatarName;
				
				Matcher matcher = pattern.matcher(file.getName());
				if (matcher.find()) {
					avatarName = matcher.group(0);
				} else {
					continue;
				}
				
				try (FileReader fileReader = new FileReader(file)) {
					config = Grasscutter.getGsonFactory().fromJson(fileReader, AvatarConfig.class);
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				
				if (config.abilities == null) {
					continue;
				}
				
				int s = config.abilities.size();
				AbilityEmbryoEntry al = new AbilityEmbryoEntry(avatarName, config.abilities.stream().map(Object::toString).toArray(size -> new String[s]));
				embryoList.add(al);
			}
		}
		
		if (embryoList == null || embryoList.isEmpty()) {
			Grasscutter.getLogger().error("No embryos loaded!");
			return;
		}

		for (AbilityEmbryoEntry entry : embryoList) {
			GenshinData.getAbilityEmbryoInfo().put(entry.getName(), entry);
		}
	}
	
	private static void loadOpenConfig() {
		// Read from cached file if exists
		File openConfigCache = new File(Grasscutter.getConfig().DATA_FOLDER + "OpenConfig.json");
		List<OpenConfigEntry> list = null;
		
		if (openConfigCache.exists()) {
			try (FileReader fileReader = new FileReader(openConfigCache)) {
				list = Grasscutter.getGsonFactory().fromJson(fileReader, TypeToken.getParameterized(Collection.class, OpenConfigEntry.class).getType());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Map<String, OpenConfigEntry> map = new TreeMap<>();
			java.lang.reflect.Type type = new TypeToken<Map<String, OpenConfigData[]>>() {}.getType();
			String[] folderNames = {"BinOutput\\Talent\\EquipTalents\\", "BinOutput\\Talent\\AvatarTalents\\"};
			
			for (String name : folderNames) {
				File folder = new File(Utils.toFilePath(Grasscutter.getConfig().RESOURCE_FOLDER + name));
				File[] files = folder.listFiles();
				if(files == null) {
					Grasscutter.getLogger().error("Error loading open config: no files found in " + folder.getAbsolutePath()); return;
				}
				
				for (File file : files) {
					if (!file.getName().endsWith(".json")) {
						continue;
					}
					
					Map<String, OpenConfigData[]> config;
					
					try (FileReader fileReader = new FileReader(file)) {
						config = Grasscutter.getGsonFactory().fromJson(fileReader, type);
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
					
					for (Entry<String, OpenConfigData[]> e : config.entrySet()) {
						List<String> abilityList = new ArrayList<>();
						int extraTalentIndex = 0;
						
						for (OpenConfigData entry : e.getValue()) {
							if (entry.$type.contains("AddAbility")) {
								abilityList.add(entry.abilityName);
							} else if (entry.talentIndex > 0) {
								extraTalentIndex = entry.talentIndex;
							}
						}
						
						OpenConfigEntry entry = new OpenConfigEntry(e.getKey(), abilityList, extraTalentIndex);
						map.put(entry.getName(), entry);
					}
				}
			}
			
			list = new ArrayList<>(map.values());
		}
		
		if (list == null || list.isEmpty()) {
			Grasscutter.getLogger().error("No openconfig entries loaded!");
			return;
		}
		
		for (OpenConfigEntry entry : list) {
			GenshinData.getOpenConfigEntries().put(entry.getName(), entry);
		}
	}
	
	// BinOutput configs
	
	private static class AvatarConfig {
		public ArrayList<AvatarConfigAbility> abilities;
		
		private static class AvatarConfigAbility {
			public String abilityName;
			public String toString() {
				return abilityName;
			}
		}
	}
	
	private static class OpenConfig {
		public OpenConfigData[] data;
	}
	
	private static class OpenConfigData {
		public String $type;
		public String abilityName;
		public int talentIndex;
	}
}
