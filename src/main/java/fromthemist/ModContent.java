package fromthemist;

import java.util.function.Function;

import fromthemist.entity.ColossusEntity;
import fromthemist.entity.HollowEntity;
import fromthemist.entity.LeviathanEntity;
import fromthemist.entity.SkittererEntity;
import fromthemist.entity.StingerEntity;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModContent {
	public static final Block TALISMAN = Blocks.register(
		ResourceKey.create(Registries.BLOCK, FromTheMist.id("talisman")),
		TalismanBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.8F, 6.0F).sound(SoundType.STONE).noCollision().noOcclusion().pushReaction(PushReaction.POPPED)
	);

	public static final Item TALISMAN_ITEM = item("talisman", p -> new BlockItem(TALISMAN, p), new Item.Properties().stacksTo(1).useBlockDescriptionPrefix());
	public static final Item MIST_ESSENCE = item("mist_essence", Item::new, new Item.Properties());

	public static final EntityType<HollowEntity> HOLLOW = entity("hollow",
		EntityType.Builder.of(HollowEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(10));
	public static final EntityType<SkittererEntity> SKITTERER = entity("skitterer",
		EntityType.Builder.of(SkittererEntity::new, MobCategory.MONSTER).sized(0.95F, 0.7F).eyeHeight(0.45F).clientTrackingRange(8));
	public static final EntityType<StingerEntity> STINGER = entity("stinger",
		EntityType.Builder.of(StingerEntity::new, MobCategory.MONSTER).sized(0.8F, 0.8F).eyeHeight(0.5F).clientTrackingRange(8));
	public static final EntityType<LeviathanEntity> LEVIATHAN = entity("leviathan",
		EntityType.Builder.of(LeviathanEntity::new, MobCategory.MONSTER).sized(9.0F, 20.0F).eyeHeight(16.0F).fireImmune().noLootTable().clientTrackingRange(14).updateInterval(2));

	public static final EntityType<ColossusEntity> COLOSSUS = entity("colossus",
		EntityType.Builder.of(ColossusEntity::new, MobCategory.MONSTER).sized(14.0F, 34.0F).eyeHeight(28.0F).fireImmune().noLootTable().clientTrackingRange(14).updateInterval(2));

	private ModContent() {
	}

	private static Item item(final String name, final Function<Item.Properties, Item> factory, final Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, FromTheMist.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	private static <T extends Entity> EntityType<T> entity(final String name, final EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, FromTheMist.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	/** A talisman item that will place with the given number of nights left. */
	public static ItemStack talisman(final int nights) {
		ItemStack stack = new ItemStack(TALISMAN_ITEM);
		stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(TalismanBlock.CHARGE, nights));
		return stack;
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(HOLLOW, HollowEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SKITTERER, SkittererEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(STINGER, StingerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(LEVIATHAN, LeviathanEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(COLOSSUS, LeviathanEntity.createAttributes());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(TALISMAN_ITEM);
			output.accept(MIST_ESSENCE);
		});
	}
}
