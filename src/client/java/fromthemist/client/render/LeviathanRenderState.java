package fromthemist.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class LeviathanRenderState extends LivingEntityRenderState {
	/** Position in the stride, 0 to 1, taken from world time so it matches the server's footfalls. */
	public float gait;
	/** 0 while alive, rising to 1 as a dying leviathan comes down. */
	public float collapse;
}
