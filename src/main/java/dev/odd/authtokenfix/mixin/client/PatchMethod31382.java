package dev.odd.authtokenfix.mixin.client;

import java.lang.reflect.Field;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.OfflineSocialInteractions;
import com.mojang.authlib.minecraft.SocialInteractionsService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilSocialInteractionsService;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.util.Util;

@Mixin(MinecraftClient.class)
public class PatchMethod31382
{
    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(at = @At("HEAD"), method = "method_31382(Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;Lnet/minecraft/client/RunArgs;)Lcom/mojang/authlib/minecraft/SocialInteractionsService;", cancellable = true)
    public void onMethod31382(YggdrasilAuthenticationService yggdrasilAuthenticationService, RunArgs runArgs, CallbackInfoReturnable<SocialInteractionsService> cir)
    {
        String accessToken = runArgs.network.session.getAccessToken();
        
        if (!runArgs.autoConnect.serverAddress.isBlank())
        {
            try {
                cir.setReturnValue(yggdrasilAuthenticationService.createSocialInteractionsService(accessToken));
            } catch (AuthenticationException ex) {
                LOGGER.error("Failed to verify authentication", ex);
            }
        }
        else if(accessToken.isBlank() || accessToken.equals("FabricMC"))
        {
            LOGGER.info("Social Interactions: Offline");
        }
        else
        {
            LOGGER.info("Assigning Main Worker to Create Social Interactions Service");
            Util.getMainWorkerExecutor().execute(new Runnable() {
                public void run() {
                    try {
                        YggdrasilSocialInteractionsService socialService = yggdrasilAuthenticationService.createSocialInteractionsService(accessToken);

                        if (socialService != null)
                        {
                            MinecraftClient client = MinecraftClient.getInstance();
                            Field socialField = MinecraftClient.class.getDeclaredField("field_26902");
                            socialField.setAccessible(true);
                            socialField.set(client, socialService);

                            Field socialInteractionsField = MinecraftClient.class.getDeclaredField("field_26842");
                            socialInteractionsField.setAccessible(true);
                            socialInteractionsField.set(client, new SocialInteractionsManager(client, socialService));

                            LOGGER.info("Social Interactions: Online");
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Failed to verify authentication", ex);
                    }
                }
            });
        }

        cir.setReturnValue(new OfflineSocialInteractions());
        cir.cancel();
        return;
    }
}