package fromthemist;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: a leviathan's foot just landed nearby. Strength is 0 (barely felt) to 1 (right beside you). */
public record ShakePayload(float strength) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ShakePayload> TYPE = new CustomPacketPayload.Type<>(FromTheMist.id("shake"));
	public static final StreamCodec<FriendlyByteBuf, ShakePayload> STREAM_CODEC = CustomPacketPayload.codec(ShakePayload::write, ShakePayload::new);

	private ShakePayload(final FriendlyByteBuf input) {
		this(input.readFloat());
	}

	private void write(final FriendlyByteBuf output) {
		output.writeFloat(this.strength);
	}

	@Override
	public CustomPacketPayload.Type<ShakePayload> type() {
		return TYPE;
	}
}
