package de.hysky.mixin;

import de.hysky.SimpleModpackUpdateChecker;

import java.lang.management.ManagementFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Mixin(MinecraftClient.class)
public class MainMenuMixin {
    @Unique
    final Logger LOGGER = LoggerFactory.getLogger("simple-modpack-update-checker");

    @Shadow
    @Final
    private ToastManager toastManager;

    @Inject(method = "collectLoadTimes", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        long timeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        LOGGER.info("[simple-modpack-update-checker | Startup Time] timeMillis = {}", timeMillis / 1000.0);
        if (SimpleModpackUpdateChecker.updateMessage != null) {
            SystemToast.show(this.toastManager, SystemToast.Type.PERIODIC_NOTIFICATION, Text.literal("Modpack Update Available").formatted(Formatting.DARK_AQUA, Formatting.BOLD), SimpleModpackUpdateChecker.updateMessage);
            LOGGER.warn("[simple-modpack-update-checker | Modpack Info] {}", SimpleModpackUpdateChecker.updateMessage.getString());
        }
    }
}
