package moreberries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Predicate;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import moreberries.block.BlockBerryBush;
import moreberries.block.BlockBerryCake;
import moreberries.block.BlockCandleBerryCake;
import moreberries.config.MoreBerriesConfig;
import moreberries.item.ItemJuice;
import moreberries.item.ItemJuicer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.LandPathNodeTypesRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleCakeBlock;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.FoodComponents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;

public class MoreBerries implements ModInitializer {

	public static final String MOD_ID = "moreberries";

	public static Block blueBerryBush;
	public static Block yellowBerryBush;
	public static Block orangeBerryBush;
	public static Block purpleBerryBush;
	public static Block greenBerryBush;
	public static Block blackBerryBush;

	public ArrayList<ItemStack> itemStacks = new ArrayList<ItemStack>();

	public static MoreBerriesConfig config;

	// Candle = Candle Cake Block
	public static HashMap<Block, CandleCakeBlock> VANILLA_CANDLES_TO_CANDLE_CAKES = new HashMap<>();

	@Override
	public void onInitialize() {
		AutoConfig.register(MoreBerriesConfig.class, JanksonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(MoreBerriesConfig.class).getConfig();

		// Sweet berry stuff
		Item juicer = new ItemJuicer(new Item.Settings());
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "juicer"), juicer);
		itemStacks.add(new ItemStack(juicer));

		Item sweetBerryJuice = new ItemJuice(new Item.Settings()
				.food(new FoodComponent.Builder().hunger(3).saturationModifier(0.1f).build()));
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "sweet_berry_juice"), sweetBerryJuice);
		itemStacks.add(new ItemStack(sweetBerryJuice));

		Item sweetBerryPie = new Item(new Item.Settings().food(FoodComponents.PUMPKIN_PIE));
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "sweet_berry_pie"), sweetBerryPie);
		itemStacks.add(new ItemStack(sweetBerryPie));

		// Berry stuff
		blueBerryBush = registerBlock("blue");
		yellowBerryBush = registerBlock("yellow");
		orangeBerryBush = registerBlock("orange");
		purpleBerryBush = registerBlock("purple");
		greenBerryBush = registerBlock("green");
		blackBerryBush = registerBlock("black");

		// Path node types (mobs should avoid berry bushes)
		LandPathNodeTypesRegistry.register(blueBerryBush, PathNodeType.DAMAGE_OTHER, null);
		LandPathNodeTypesRegistry.register(yellowBerryBush, PathNodeType.DAMAGE_OTHER, null);
		LandPathNodeTypesRegistry.register(orangeBerryBush, PathNodeType.DAMAGE_OTHER, null);
		LandPathNodeTypesRegistry.register(purpleBerryBush, PathNodeType.DAMAGE_OTHER, null);
		LandPathNodeTypesRegistry.register(greenBerryBush, PathNodeType.DAMAGE_OTHER, null);
		LandPathNodeTypesRegistry.register(blackBerryBush, PathNodeType.DAMAGE_OTHER, null);

		// Generation
		registerBiomeGeneration(config.blackBerrySpawnBiomes, blackBerryBush,
				"black_berry");
		registerBiomeGeneration(config.greenBerrySpawnBiomes, greenBerryBush,
				"green_berry");
		registerBiomeGeneration(config.blueBerrySpawnBiomes, blueBerryBush,
				"blue_berry");
		registerBiomeGeneration(config.orangeBerrySpawnBiomes, orangeBerryBush,
				"orange_berry");
		registerBiomeGeneration(config.purpleBerrySpawnBiomes, purpleBerryBush,
				"purple_berry");
		registerBiomeGeneration(config.yellowBerrySpawnBiomes, yellowBerryBush,
				"yellow_berry");

		addVanillaCandlesToCakeMap();

		// Itemgroup
		Registry.register(Registries.ITEM_GROUP, new Identifier(MOD_ID, "berries"), FabricItemGroup.builder()
				.icon(() -> new ItemStack(blueBerryBush))
				.displayName(Text.translatable("itemGroup.moreberries.berries"))
				.entries((context, entries) -> {
					entries.add(new ItemStack(Items.SWEET_BERRIES));
					entries.addAll(itemStacks);
				})
				.build());

		// Optional resource packs
		if (config.replaceSweetBerryBushModel) {
			ResourceManagerHelper.registerBuiltinResourcePack(
					new Identifier(MOD_ID, "modifiedsweetberrybushmodel"),
					FabricLoader.getInstance().getModContainer(MOD_ID).get(),
					Text.of("Modified Sweet Berry Bush Model"),
					ResourcePackActivationType.ALWAYS_ENABLED);
		}

		if (config.craftableBerryBushes) {
			ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(MOD_ID, "berrybushrecipes"),
					FabricLoader.getInstance().getModContainer(MOD_ID).get(), Text.of("Berry Bush Recipes"),
					ResourcePackActivationType.ALWAYS_ENABLED);
		}
	}

	private void addVanillaCandlesToCakeMap() {
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.CANDLE, (CandleCakeBlock) Blocks.CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.BLACK_CANDLE, (CandleCakeBlock) Blocks.BLACK_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.BLUE_CANDLE, (CandleCakeBlock) Blocks.BLUE_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.CYAN_CANDLE, (CandleCakeBlock) Blocks.CYAN_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.BROWN_CANDLE, (CandleCakeBlock) Blocks.BROWN_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.GRAY_CANDLE, (CandleCakeBlock) Blocks.GRAY_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.GREEN_CANDLE, (CandleCakeBlock) Blocks.GREEN_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.LIGHT_BLUE_CANDLE, (CandleCakeBlock) Blocks.LIGHT_BLUE_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.LIME_CANDLE, (CandleCakeBlock) Blocks.LIME_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.PURPLE_CANDLE, (CandleCakeBlock) Blocks.PURPLE_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.LIGHT_GRAY_CANDLE, (CandleCakeBlock) Blocks.LIGHT_GRAY_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.YELLOW_CANDLE, (CandleCakeBlock) Blocks.YELLOW_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.ORANGE_CANDLE, (CandleCakeBlock) Blocks.ORANGE_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.RED_CANDLE, (CandleCakeBlock) Blocks.RED_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.WHITE_CANDLE, (CandleCakeBlock) Blocks.WHITE_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.PINK_CANDLE, (CandleCakeBlock) Blocks.PINK_CANDLE_CAKE);
		VANILLA_CANDLES_TO_CANDLE_CAKES.put(Blocks.MAGENTA_CANDLE, (CandleCakeBlock) Blocks.MAGENTA_CANDLE_CAKE);
	}

	// Adds berry bushes to vanilla biomes
	private void registerBiomeGeneration(String spawnBiomes, Block bushBlock, String name) {
		String[] biomes = spawnBiomes.replaceAll(" ", "").split(",");

		// Get list of spawn biomes
		ArrayList<RegistryKey<Biome>> biomeKeys = new ArrayList<>();
		ArrayList<TagKey<Biome>> biomeTags = new ArrayList<>();

		for (String biome : biomes) {
			// Category
			if (biome.charAt(0) == '#') {
				biomeTags.add(TagKey.of(RegistryKeys.BIOME, new Identifier(biome.substring(1))));
			} else {
				// Biome
				biomeKeys.add(RegistryKey.of(RegistryKeys.BIOME, new Identifier(biome)));
			}
		}

		Predicate<BiomeSelectionContext> biomeSelector = BiomeSelectors.includeByKey(biomeKeys);

		if (!biomeTags.isEmpty()) {
			for (TagKey<Biome> biomeTag : biomeTags) {
				biomeSelector = biomeSelector.or(BiomeSelectors.tag(biomeTag));
			}
		}

		// Add to biomes
		BiomeModifications.addFeature(biomeSelector,
				GenerationStep.Feature.VEGETAL_DECORATION,
				RegistryKey.of(RegistryKeys.PLACED_FEATURE,
						new Identifier(MOD_ID, String.format("%s_generation", name))));
	}

	private Block registerBlock(String name) {
		// Create items
		Item berryItem = new Item(new Item.Settings()
				.food(new FoodComponent.Builder().hunger(2).saturationModifier(0.1f).build()));
		Item juiceItem = null;
		juiceItem = new ItemJuice(new Item.Settings().maxCount(16)
				.food(new FoodComponent.Builder().hunger(3).saturationModifier(0.2F).build())
				.recipeRemainder(juiceItem));
		Item pieItem = new Item(new Item.Settings().food(FoodComponents.PUMPKIN_PIE));

		// Create blocks
		Block bush = new BlockBerryBush(berryItem);
		BlockItem bushItem = new BlockItem(bush, new Item.Settings());
		BlockBerryCake cake = new BlockBerryCake(Block.Settings.copy(Blocks.CAKE));
		BlockItem cakeItem = new BlockItem(cake, new Item.Settings());

		// Register items
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, String.format("%s_berries", name)), berryItem);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, String.format("%s_berry_juice", name)),
				juiceItem);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, String.format("%s_berry_pie", name)), pieItem);

		// Register blocks
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, String.format("%s_berry_bush", name)), bush);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, String.format("%s_berry_bush", name)),
				bushItem);
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, String.format("%s_berry_cake", name)), cake);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, String.format("%s_berry_cake", name)),
				cakeItem);

		itemStacks.add(new ItemStack(berryItem));
		itemStacks.add(new ItemStack(juiceItem));
		itemStacks.add(new ItemStack(pieItem));
		itemStacks.add(new ItemStack(bushItem));
		itemStacks.add(new ItemStack(cakeItem));

		// Candle cakes
		registerCandleCakes(name, cake);

		// Compost berries
		CompostingChanceRegistry.INSTANCE.add(berryItem, 0.3f);

		return bush;
	}

	// Register all 17 candle cakes for a specific berry
	private void registerCandleCakes(String berry, BlockBerryCake cakeBlock) {
		registerCandleCake(Blocks.CANDLE, cakeBlock, "", berry);
		registerCandleCake(Blocks.BLACK_CANDLE, cakeBlock, "black_", berry);
		registerCandleCake(Blocks.BLUE_CANDLE, cakeBlock, "blue_", berry);
		registerCandleCake(Blocks.BROWN_CANDLE, cakeBlock, "brown_", berry);
		registerCandleCake(Blocks.CYAN_CANDLE, cakeBlock, "cyan_", berry);
		registerCandleCake(Blocks.GRAY_CANDLE, cakeBlock, "gray_", berry);
		registerCandleCake(Blocks.GREEN_CANDLE, cakeBlock, "green_", berry);
		registerCandleCake(Blocks.LIME_CANDLE, cakeBlock, "lime_", berry);
		registerCandleCake(Blocks.MAGENTA_CANDLE, cakeBlock, "magenta_", berry);
		registerCandleCake(Blocks.ORANGE_CANDLE, cakeBlock, "orange_", berry);
		registerCandleCake(Blocks.PINK_CANDLE, cakeBlock, "pink_", berry);
		registerCandleCake(Blocks.PURPLE_CANDLE, cakeBlock, "purple_", berry);
		registerCandleCake(Blocks.RED_CANDLE, cakeBlock, "red_", berry);
		registerCandleCake(Blocks.WHITE_CANDLE, cakeBlock, "white_", berry);
		registerCandleCake(Blocks.YELLOW_CANDLE, cakeBlock, "yellow_", berry);
		registerCandleCake(Blocks.LIGHT_BLUE_CANDLE, cakeBlock, "light_blue_", berry);
		registerCandleCake(Blocks.LIGHT_GRAY_CANDLE, cakeBlock, "light_gray_", berry);
	}

	// Register a single candle cake
	private void registerCandleCake(Block candle, BlockBerryCake cake, String colour, String berry) {
		Block candleCake = new BlockCandleBerryCake(candle, cake, AbstractBlock.Settings.copy(Blocks.CANDLE_CAKE));
		Identifier identifier = new Identifier(MOD_ID, String.format("%scandle_%s_berry_cake", colour, berry));
		Registry.register(Registries.BLOCK, identifier, candleCake);
	}
}
