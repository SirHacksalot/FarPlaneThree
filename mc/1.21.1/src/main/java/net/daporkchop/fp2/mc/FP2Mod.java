package net.daporkchop.fp2.mc;

//? if neoforge {
import net.daporkchop.fp2.mc.network.FP2Network1_21;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
//?} else {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;*/
//?}

import net.daporkchop.fp2.api.FP2;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.log4j.util.log.Log4jAsPorkLibLogger;
import net.daporkchop.fp2.core.util.I18n;
import net.daporkchop.fp2.core.util.threading.futureexecutor.ImmediateFutureExecutor;
import net.daporkchop.fp2.mc.client.FP2Client1_21;
import net.daporkchop.fp2.mc.server.FP2Server1_21;
import net.daporkchop.fp2.mc.util.I18n1_21;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Path;

//? if neoforge {
@Mod(FP2.MODID)
public class FP2Mod extends FP2Core {
    public FP2Mod(IEventBus modEventBus) {
        this.log(new Log4jAsPorkLibLogger(LogManager.getLogger(FP2.MODID)));
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onRegisterPayloadHandlers);
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        FP2Network1_21.init(this, event);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LogManager.getLogger(FP2.MODID).info("fp2.flagcheck={}", System.getProperty("fp2.flagcheck", "<null>"));
        this.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            this.client(new FP2Client1_21(this));
        }
        this.server(new FP2Server1_21(this));
        this.server().init(ImmediateFutureExecutor.INSTANCE);
    }

    @Override
    protected Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
//?} else {
/*public class FP2Mod extends FP2Core implements ModInitializer {
    @Override
    public void onInitialize() {
        this.log(new Log4jAsPorkLibLogger(LogManager.getLogger(FP2.MODID)));
        this.init();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            this.client(new FP2Client1_21(this));
        }
        this.server(new FP2Server1_21(this));
        this.server().init(ImmediateFutureExecutor.INSTANCE);
    }

    @Override
    protected Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }*/
//?}

    @Override
    public I18n i18n() {
        return new I18n1_21();
    }
}
