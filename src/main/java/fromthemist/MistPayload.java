package fromthemist;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: how thick the mist is right now, 0 (clear) to 1 (full). */
public record MistPayload(float intensity) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MistPayload> TYPE = new CustomPacketPayload.Type<>(FromTheMist.id("mist"));
	public static final StreamCodec<FriendlyByteBuf, MistPayload> STREAM_CODEC = CustomPacketPayload.codec(MistPayload::write, MistPayload::new);

	private MistPayload(final FriendlyByteBuf input) {
		this(input.readFloat());
	}

	private void write(final FriendlyByteBuf output) {
		output.writeFloat(this.intensity);
	}

	@Override
	public CustomPacketPayload.Type<MistPayload> type() {
		return TYPE;
	}
}
