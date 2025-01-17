package ram.talia.hexal.client;

import ram.talia.hexal.client.entity.WispRenderer;
import ram.talia.hexal.common.entities.HexalEntities;
import ram.talia.hexal.xplat.IClientXplatAbstractions;

public class RegisterClientStuff {
	public static void init () {
		var x = IClientXplatAbstractions.INSTANCE;
		
		x.registerEntityRenderer(HexalEntities.PROJECTILE_WISP, WispRenderer::new);
		x.registerEntityRenderer(HexalEntities.TICKING_WISP, WispRenderer::new);
	}
}
