package fromthemist.rpg;

import fromthemist.FromTheMist;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The three messages the progression system sends. */
public final class RpgPayloads {
	private RpgPayloads() {
	}

	/** Server to client: everything the character screen shows. Cooldowns and clear air are ticks remaining. */
	public record Sync(int level, int xp, int points, int[] talents, int[] skillLevels, int[] skillXp, int[] cooldowns, int clearAir) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<Sync> TYPE = new CustomPacketPayload.Type<>(FromTheMist.id("progress"));
		public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = CustomPacketPayload.codec(Sync::write, Sync::new);

		private Sync(final FriendlyByteBuf input) {
			this(input.readVarInt(), input.readVarInt(), input.readVarInt(), input.readVarIntArray(64), input.readVarIntArray(64), input.readVarIntArray(64), input.readVarIntArray(64), input.readVarInt());
		}

		private void write(final FriendlyByteBuf output) {
			output.writeVarInt(this.level);
			output.writeVarInt(this.xp);
			output.writeVarInt(this.points);
			output.writeVarIntArray(this.talents);
			output.writeVarIntArray(this.skillLevels);
			output.writeVarIntArray(this.skillXp);
			output.writeVarIntArray(this.cooldowns);
			output.writeVarInt(this.clearAir);
		}

		@Override
		public CustomPacketPayload.Type<Sync> type() {
			return TYPE;
		}
	}

	/** Client to server: put a point into this talent. */
	public record Spend(int talent) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<Spend> TYPE = new CustomPacketPayload.Type<>(FromTheMist.id("spend"));
		public static final StreamCodec<FriendlyByteBuf, Spend> STREAM_CODEC = CustomPacketPayload.codec((p, out) -> out.writeVarInt(p.talent), in -> new Spend(in.readVarInt()));

		@Override
		public CustomPacketPayload.Type<Spend> type() {
			return TYPE;
		}
	}

	/** Client to server: use the ability in this slot (0 Warden, 1 Hunter, 2 Survivor). */
	public record UseAbility(int slot) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<UseAbility> TYPE = new CustomPacketPayload.Type<>(FromTheMist.id("ability"));
		public static final StreamCodec<FriendlyByteBuf, UseAbility> STREAM_CODEC = CustomPacketPayload.codec((p, out) -> out.writeVarInt(p.slot), in -> new UseAbility(in.readVarInt()));

		@Override
		public CustomPacketPayload.Type<UseAbility> type() {
			return TYPE;
		}
	}
}
