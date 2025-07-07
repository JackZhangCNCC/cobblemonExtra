package com.johnson.cobblemonextra.mixin;

import com.johnson.cobblemonextra.CobblemonExtra;
import com.johnson.cobblemonextra.showdown.UniversalShowdownMerger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 通用Showdown拦截器 - 拦截 gravels_extended_battles
 * 使用通用合并器处理所有mod的Showdown注入
 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(targets = "drai.dev.gravelsextendedbattles.showdown.ShowdownFileManager", remap = false)
public class GravelsExtendedBattlesInterceptorMixin {

    /**
     * 拦截 injectShowdown 方法
     * 使用通用合并器处理
     */
    @Inject(method = "injectShowdown", at = @At("HEAD"), remap = false, cancellable = true)
    private static void beforeInjectShowdown(CallbackInfo ci) {
        try {
            String modId = "gravels_extended_battles";
            CobblemonExtra.LOGGER.info("========= 通用拦截器：检测到 {} 的 injectShowdown 调用 =========", modId);

            // 取消原始的注入操作
            ci.cancel();

            // 使用通用合并器处理
            UniversalShowdownMerger.handleShowdownInjection(modId);

            CobblemonExtra.LOGGER.info("✅ 通用拦截器：成功处理 {} 的注入", modId);

        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("❌ 通用拦截器：处理失败，允许原始操作继续", e);
            // 如果我们的处理失败，不取消原始操作
        }
    }
}
