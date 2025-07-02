package com.johnson.cobblemonextra.mixin;

import com.johnson.cobblemonextra.showdown.ShowdownDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {"com/cobblemon/mod/common/battles/runner/graal/GraalShowdownUnbundler"}, priority = 1100)
public class ShowdownThreadMixin {
    
    @Inject(method = {"attemptUnbundle"}, at = {@At("TAIL")}, remap = false)
    private void injected(CallbackInfo ci) {
        if (!cobblemonextra$loaded) {
            ShowdownDataManager.injectShowdown();
            cobblemonextra$loaded = true;
        }
    }
    
    @Unique
    private static boolean cobblemonextra$loaded = false;
} 