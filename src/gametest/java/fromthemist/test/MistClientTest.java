package fromthemist.test;

import fromthemist.MistDirector;
import fromthemist.MistState;
import fromthemist.WardManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Boots the real client in a fresh world and walks the mod's features:
 * creature models, the mist, a leviathan, and the talisman's sealed-room rule.
 * Screenshots land in build/run/clientGameTest/screenshots.
 */
public class MistClientTest implements FabricClientGameTest {
	@Override
	public void runTest(final ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamemode creative @a");
			server.runCommand("difficulty normal");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 5");
			context.waitTicks(40);

			// Creatures, held still so they pose for the camera.
			server.runCommand("execute as @a at @s run summon fromthemist:hollow ~-3 ~ ~5 {NoAI:1b,Rotation:[180f,0f]}");
			server.runCommand("execute as @a at @s run summon fromthemist:hollow ~-1.5 ~ ~5 {NoAI:1b,Rotation:[180f,0f]}");
			server.runCommand("execute as @a at @s run summon fromthemist:skitterer ~1 ~ ~4 {NoAI:1b,Rotation:[150f,0f]}");
			server.runCommand("execute as @a at @s run summon fromthemist:stinger ~3 ~1.5 ~5 {NoAI:1b,NoGravity:1b,Rotation:[200f,0f]}");
			context.waitTicks(30);
			context.takeScreenshot("1_creatures");
			server.runCommand("kill @e[type=!player]");
			server.runCommand("execute as @a at @s run summon fromthemist:stinger ~-0.6 ~1.1 ~2.2 {NoAI:1b,NoGravity:1b,Rotation:[215f,0f]}");
			server.runCommand("execute as @a at @s run summon fromthemist:stinger ~1.3 ~0.2 ~2.6 {NoAI:1b,NoGravity:1b,Rotation:[90f,0f]}");
			context.waitTicks(23);
			context.takeScreenshot("1a_stinger_close");
			server.runCommand("kill @e[type=!player]");
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 -22");
			server.runCommand("execute as @a at @s run summon fromthemist:leviathan ~6 ~ ~46 {Rotation:[140f,0f]}");
			context.waitTicks(50);
			context.takeScreenshot("1b_leviathan_clear");
			server.runCommand("kill @e[type=!player]");
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 -28");
			server.runCommand("execute as @a at @s run summon fromthemist:colossus ~8 ~ ~58 {Rotation:[140f,0f]}");
			context.waitTicks(50);
			context.takeScreenshot("1c_colossus_clear");
			// Killed, it should come straight down rather than tip over, and still be there as a carcass.
			server.runCommand("kill @e[type=fromthemist:colossus]");
			context.waitTicks(30);
			context.takeScreenshot("1d_colossus_falling");
			context.waitTicks(45);
			context.takeScreenshot("1e_colossus_down");
			int carcass = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(fromthemist.entity.ColossusEntity.class,
				s.getPlayerList().getPlayers().get(0).getBoundingBox().inflate(120.0)).size());
			if (carcass != 1) {
				throw new AssertionError("a dead colossus should lie where it fell for a while, found " + carcass);
			}

			context.waitTicks(60);
			carcass = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(fromthemist.entity.ColossusEntity.class,
				s.getPlayerList().getPlayers().get(0).getBoundingBox().inflate(120.0)).size());
			if (carcass != 0) {
				throw new AssertionError("the carcass should have dissolved");
			}

			server.runCommand("execute as @a at @s run summon fromthemist:leviathan ~6 ~ ~46 {Rotation:[140f,0f]}");
			context.waitTicks(20);
			server.runCommand("kill @e[type=fromthemist:leviathan]");
			context.waitTicks(72);
			context.takeScreenshot("1f_strider_down");
			server.runCommand("kill @e[type=!player]");
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 5");

			// The talisman: a sealed stone box wards, a hole in the wall breaks it.
			BlockPos base = server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).blockPosition().offset(12, 0, 0));
			String box = base.getX() + " " + base.getY() + " " + base.getZ() + " " + (base.getX() + 4) + " " + (base.getY() + 3) + " " + (base.getZ() + 4);
			server.runCommand("fill " + box + " minecraft:cobblestone hollow");
			BlockPos inside = base.offset(2, 1, 2);
			BlockPos talisman = base.offset(2, 2, 1);
			server.runCommand("setblock " + talisman.getX() + " " + talisman.getY() + " " + talisman.getZ() + " fromthemist:talisman[facing=south,charge=3]");
			context.waitTicks(45);
			check(server, inside, true, "sealed box should be warded");
			check(server, base.offset(8, 1, 2), false, "outside the box should not be warded");
			boolean shell = server.computeOnServer(s -> WardManager.isProtected(s.overworld(), base.offset(0, 1, 2)));
			if (!shell) {
				throw new AssertionError("the wall of a warded room should be protected");
			}

			server.runCommand("setblock " + base.getX() + " " + (base.getY() + 1) + " " + (base.getZ() + 2) + " minecraft:air");
			context.waitTicks(45);
			check(server, inside, false, "a hole in the wall should break the ward");
			server.runCommand("setblock " + base.getX() + " " + (base.getY() + 1) + " " + (base.getZ() + 2) + " minecraft:oak_door[half=lower,open=false]");
			server.runCommand("setblock " + base.getX() + " " + (base.getY() + 2) + " " + (base.getZ() + 2) + " minecraft:oak_door[half=upper,open=false]");
			context.waitTicks(45);
			check(server, inside, true, "a closed door should seal the room again");
			int tracked = server.computeOnServer(s -> MistState.get(s.overworld()).talismans.size());
			if (tracked != 1) {
				throw new AssertionError("expected 1 tracked talisman, found " + tracked);
			}

			server.runCommand("execute as @a at @s run tp @s " + (base.getX() + 2.5) + " " + (base.getY() + 1) + " " + (base.getZ() + 3.5) + " 180 -10");
			context.waitTicks(20);
			context.takeScreenshot("2_talisman");

			// A Hollow outside, at night, with the player behind the ward: it must not get in or break the wall.
			server.runCommand("gamemode survival @a");
			server.runCommand("time set midnight");
			server.runCommand("summon fromthemist:hollow " + (base.getX() - 6) + " " + base.getY() + " " + (base.getZ() + 2));
			context.waitTicks(400);
			boolean intact = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				ServerPlayer player = s.getPlayerList().getPlayers().get(0);
				return player.isAlive() && WardManager.isWarded(level, player.blockPosition())
					&& !level.getBlockState(base.offset(0, 1, 2)).isAir() && !level.getBlockState(base.offset(0, 1, 1)).isAir();
			});
			if (!intact) {
				throw new AssertionError("the Hollow got through a warded wall");
			}

			// Now with the ward broken, the same Hollow should come through the wall.
			server.runCommand("setblock " + talisman.getX() + " " + talisman.getY() + " " + talisman.getZ() + " minecraft:air");
			context.waitTicks(700);
			boolean breached = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				for (BlockPos pos : BlockPos.betweenClosed(base, base.offset(4, 3, 4))) {
					boolean edge = pos.getX() == base.getX() || pos.getX() == base.getX() + 4 || pos.getZ() == base.getZ() || pos.getZ() == base.getZ() + 4;
					if (edge && pos.getY() > base.getY() && pos.getY() < base.getY() + 3 && level.getBlockState(pos).isAir()) {
						return true;
					}
				}

				return !s.getPlayerList().getPlayers().get(0).isAlive() || s.getPlayerList().getPlayers().get(0).getHealth() < 20.0F;
			});
			context.takeScreenshot("3_breach");
			if (!breached) {
				throw new AssertionError("without a ward the Hollow should break in");
			}

			// The Hollow usually wins that one; get back up before going on.
			context.runOnClient(client -> {
				if (client.player != null && !client.player.isAlive()) {
					client.player.respawn();
				}
			});
			context.waitTicks(60);

			// Progression: experience makes levels, levels make points, points buy talents, talents change the player.
			server.runCommand("time set noon");
			server.runCommand("kill @e[type=!player]");
			server.runCommand("execute as @a run mist xp 2000");
			int level = server.computeOnServer(s -> fromthemist.rpg.Rpg.progress(s.getPlayerList().getPlayers().get(0)).level);
			if (level < 8) {
				throw new AssertionError("2000 xp should be at least level 8, was " + level);
			}

			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().get(0);
				fromthemist.rpg.Rpg.spend(player, fromthemist.rpg.Talent.FLEET.ordinal());        // refused: Vigor comes first
				fromthemist.rpg.Rpg.spend(player, fromthemist.rpg.Talent.VIGOR.ordinal());
				fromthemist.rpg.Rpg.spend(player, fromthemist.rpg.Talent.VIGOR.ordinal());
				fromthemist.rpg.Rpg.spend(player, fromthemist.rpg.Talent.ADRENALINE.ordinal());   // refused: needs six points in the tree
				fromthemist.rpg.Rpg.spend(player, fromthemist.rpg.Talent.MIST_EYES.ordinal());
			});
			float maxHealth = server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).getMaxHealth());
			int spent = server.computeOnServer(s -> {
				fromthemist.rpg.Progress p = fromthemist.rpg.Rpg.progress(s.getPlayerList().getPlayers().get(0));
				return p.rank(fromthemist.rpg.Talent.VIGOR) * 100 + p.rank(fromthemist.rpg.Talent.FLEET) * 10 + p.rank(fromthemist.rpg.Talent.ADRENALINE);
			});
			if (maxHealth != 24.0F || spent != 200) {
				throw new AssertionError("talents wrong: max health " + maxHealth + ", ranks " + spent);
			}

			// Skills rise by use: hit something until Combat levels.
			server.runCommand("gamemode survival @a");
			server.runCommand("execute as @a at @s run summon minecraft:zombie ~ ~ ~3 {NoAI:1b,Health:1000f,attributes:[{id:\"minecraft:max_health\",base:1000}]}");
			for (int i = 0; i < 12; i++) {
				server.runCommand("execute as @a at @s run damage @e[type=minecraft:zombie,limit=1,sort=nearest] 10 minecraft:player_attack by @s");
				context.waitTicks(12);
			}

			int combat = server.computeOnServer(s -> fromthemist.rpg.Rpg.progress(s.getPlayerList().getPlayers().get(0)).skillLevels[fromthemist.rpg.Skill.COMBAT.ordinal()]);
			if (combat < 1) {
				throw new AssertionError("dealing 120 damage should raise Combat");
			}

			server.runCommand("kill @e[type=minecraft:zombie]");
			context.waitTicks(25);
			context.runOnClient(client -> client.gui.setScreen(new fromthemist.client.CharacterScreen()));
			context.waitTicks(10);
			context.takeScreenshot("3b_character");
			context.runOnClient(client -> client.gui.setScreen(null));

			// The mist, and something large walking through it.
			server.runCommand("gamemode creative @a");
			server.runCommand("kill @e[type=!player]");
			server.runCommand("time set noon");
			server.runCommand("execute as @a at @s run tp @s ~30 ~3 ~ 0 -12");
			server.runCommand("execute as @a run mist start");
			double startX = server.computeOnServer(s -> MistState.get(s.overworld()).centerX);
			double startZ = server.computeOnServer(s -> MistState.get(s.overworld()).centerZ);
			context.waitTicks(330);
			context.takeScreenshot("4_mist");
			server.runCommand("execute as @a at @s run summon fromthemist:leviathan ~-10 ~-3 ~34");
			context.waitTicks(60);
			context.takeScreenshot("5_leviathan");
			server.runCommand("kill @e[type=fromthemist:leviathan]");
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 -40");
			server.runCommand("execute as @a at @s run summon fromthemist:colossus ~-6 ~-3 ~18");
			context.waitTicks(200);
			context.takeScreenshot("6_colossus_overhead");
			int riders = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(fromthemist.entity.StingerEntity.class,
				s.getPlayerList().getPlayers().get(0).getBoundingBox().inflate(60.0)).size());
			if (riders < 1) {
				throw new AssertionError("stingers should travel with a colossus");
			}

			server.runCommand("execute as @a run mist status");
			float mist = server.computeOnServer(s -> MistDirector.intensityAt(s.getPlayerList().getPlayers().get(0)));
			if (mist < 0.99F) {
				throw new AssertionError("the player should be deep in the bank, mist was " + mist);
			}

			// The bank is a place: it drifts, and a few hundred blocks away the air is clear.
			double drift = server.computeOnServer(s -> Math.hypot(MistState.get(s.overworld()).centerX - startX, MistState.get(s.overworld()).centerZ - startZ));
			if (drift < 2.0) {
				throw new AssertionError("the bank should have drifted, moved " + drift);
			}

			float far = server.computeOnServer(s -> MistState.get(s.overworld()).intensityAt(startX + 250.0, startZ));
			float edge = server.computeOnServer(s -> {
				MistState state = MistState.get(s.overworld());
				return state.intensityAt(state.centerX + MistState.RADIUS - MistState.EDGE / 2.0, state.centerZ);
			});
			if (far != 0.0F || edge < 0.4F || edge > 0.6F) {
				throw new AssertionError("bank edge is wrong: far=" + far + " edge=" + edge);
			}

			// A bank that forms normally starts well away from the player.
			server.runCommand("execute as @a run mist approach");
			double away = server.computeOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().get(0);
				return MistState.get(s.overworld()).edgeDistance(player.getX(), player.getZ());
			});
			if (away < 150.0) {
				throw new AssertionError("a new bank should form far off, edge was " + away + " blocks away");
			}

			// Nights survived drive the Hollow.
			server.runCommand("mist nights 7");
			int stage = server.computeOnServer(s -> MistDirector.stage(s.overworld()));
			if (stage != 2) {
				throw new AssertionError("7 nights should be stage 2, was " + stage);
			}

			// Mist kills pay out.
			server.runCommand("mist nights 0");
			server.runCommand("execute as @a run mist start");
			server.runCommand("gamemode survival @a");
			server.runCommand("kill @e[type=!player]");
			context.waitTicks(5);
			server.runCommand("kill @e[type=item]");
			server.runCommand("execute as @a at @s run summon fromthemist:skitterer ~ ~ ~3 {NoAI:1b}");
			server.runCommand("execute as @a at @s run damage @e[type=fromthemist:skitterer,limit=1,sort=nearest] 100 minecraft:player_attack by @s");
			context.waitTicks(10);
			int essence = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				s.getPlayerList().getPlayers().get(0).getBoundingBox().inflate(8.0), e -> e.getItem().is(fromthemist.ModContent.MIST_ESSENCE)).size());
			if (essence < 1) {
				throw new AssertionError("a killed skitterer should always drop mist essence");
			}

			// Dying in a standard world: what you wear and hold on your bar comes back with you, the pack stays behind.
			server.runCommand("kill @e[type=!player]");
			server.runCommand("item replace entity @a hotbar.0 with minecraft:iron_sword");
			server.runCommand("item replace entity @a armor.head with minecraft:iron_helmet");
			server.runCommand("item replace entity @a inventory.0 with minecraft:diamond 5");
			server.runCommand("kill @a");
			context.waitTicks(20);
			context.runOnClient(client -> client.player.respawn());
			context.waitTicks(60);
			String after = server.computeOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().get(0);
				int diamonds = 0;
				for (net.minecraft.world.entity.item.ItemEntity item : s.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(400.0), e -> e.getItem().is(net.minecraft.world.item.Items.DIAMOND))) {
					diamonds += item.getItem().getCount();
				}

				return player.getInventory().getItem(0).is(net.minecraft.world.item.Items.IRON_SWORD) + " "
					+ player.getInventory().getItem(39).is(net.minecraft.world.item.Items.IRON_HELMET) + " "
					+ player.getInventory().getItem(9).isEmpty() + " " + diamonds;
			});
			if (!after.equals("true true true 5")) {
				throw new AssertionError("death should keep sword and helmet, empty the pack and leave 5 diamonds on the ground; got " + after);
			}
		}
	}

	private static void check(final TestServerContext server, final BlockPos pos, final boolean expected, final String what) {
		boolean warded = server.computeOnServer(s -> WardManager.isWarded(s.overworld(), pos));
		if (warded != expected) {
			throw new AssertionError(what);
		}
	}
}
