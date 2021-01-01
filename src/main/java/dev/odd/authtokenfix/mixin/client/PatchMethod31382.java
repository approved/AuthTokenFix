package dev.odd.authtokenfix.mixin.client;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.OfflineSocialInteractions;
import com.mojang.authlib.minecraft.SocialInteractionsService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;

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
        if(accessToken == null || accessToken.isEmpty() || accessToken.equals("FabricMC"))
        {
            LOGGER.info("Setting Social Interactions Offline");
            cir.setReturnValue(new OfflineSocialInteractions());
            cir.cancel();
            return;
        }
        else
        {
            final SocialInteractionsService[] socialService = new SocialInteractionsService[] { new OfflineSocialInteractions() };
            Thread socialThread = new Thread() {
                public void run() {
                    try {
                        socialService[0] = yggdrasilAuthenticationService.createSocialInteractionsService(accessToken);
                        LOGGER.info("Verified Authentication Successfully");
                    } catch (AuthenticationException ex) {
                        LOGGER.error("Failed to verify authentication", (Throwable)ex);
                        socialService[0] = new OfflineSocialInteractions();
                    }
                }  
            };
            
            socialThread.start();

            cir.setReturnValue(socialService[0]);
            cir.cancel();
            return;
        }
    }
}