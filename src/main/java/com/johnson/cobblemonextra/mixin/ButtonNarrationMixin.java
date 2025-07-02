package com.johnson.cobblemonextra.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修复Button的narration null值问题，防止GUI崩溃
 */
@Mixin(Button.class)
public class ButtonNarrationMixin {
    
    @Inject(method = "createNarrationMessage", at = @At("HEAD"), cancellable = true)
    private void fixNullNarration(CallbackInfoReturnable<Component> cir) {
        Button button = (Button) (Object) this;
        
        // 检查按钮消息是否为null
        Component message = button.getMessage();
        if (message == null) {
            // 提供默认消息，防止崩溃
            cir.setReturnValue(Component.literal("按钮"));
        }
    }
} 