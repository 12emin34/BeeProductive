package io.github.alloffabric.beeproductive.mixin;

import io.github.alloffabric.beeproductive.api.HoneyFlavor;
import io.github.alloffabric.beeproductive.api.hive.Beehive;
import io.github.alloffabric.beeproductive.api.hive.BeehiveProvider;
import io.github.alloffabric.beeproductive.api.hive.SimpleBeehive;
import io.github.alloffabric.beeproductive.init.BeeProdHoneys;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BeehiveBlock.class)
public abstract class MixinBeehiveBlock extends Block implements BeehiveProvider {
    @Final
    private ThreadLocal<BlockState> cachedState = new ThreadLocal<>();
    @Final
    private ThreadLocal<World> cachedWorld = new ThreadLocal<>();
    @Final
    private ThreadLocal<BlockPos> cachedPos = new ThreadLocal<>();

    public MixinBeehiveBlock(Settings settings) {
        super(settings);
    }

    @Inject(method = "dropHoneycomb", at = @At("HEAD"), cancellable = true)
    private static void dropFlavoredCombs(World world, BlockPos pos, CallbackInfo info) {
        BlockState state = world.getBlockState(pos);
        HoneyFlavor toDrop = getDroppedFlavor(world, pos, state);
        if (toDrop == BeeProdHoneys.VANILLA) return;
        ItemStack stack = toDrop.getSheared();
        for (int i = 0; i < stack.getCount(); i++) {
            dropStack(world, pos, new ItemStack(stack.getItem()));
        }
        if (FabricLoader.getInstance().isModLoaded("beebetter")) {
            dropStack(world, pos, new ItemStack(Registry.ITEM.get(new Identifier("beebetter", "beeswax_flake"))));
        }
        info.cancel();
    }

    private static HoneyFlavor getDroppedFlavor(World world, BlockPos pos, BlockState state) {
        Beehive hive = ((BeehiveProvider) state.getBlock()).getBeehive(world, pos, state);
        HoneyFlavor flavor = hive.getFlavorToHarvest();
        hive.harvestHoney(flavor);
        return flavor;
    }

    @Override
    public Beehive getBeehive(World world, BlockPos pos, BlockState state) {
        return new SimpleBeehive(world, pos, state);
    }

    @Inject(method = "onUse", at = @At("HEAD"))
    private void cacheThreadLocals(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> info) {
        this.cachedState.set(state);
        this.cachedWorld.set(world);
        this.cachedPos.set(pos);
    }

    @ModifyArg(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setStackInHand(Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;)V"))
    private ItemStack modSetStack(ItemStack original) {
        HoneyFlavor toDrop = getDroppedFlavor(cachedWorld.get(), cachedPos.get(), cachedState.get());
        if (toDrop == BeeProdHoneys.VANILLA) return original;
        return toDrop.getBottled();
    }

    @ModifyArg(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;insertStack(Lnet/minecraft/item/ItemStack;)Z"))
    private ItemStack modInsertStack(ItemStack original) {
        HoneyFlavor toDrop = getDroppedFlavor(cachedWorld.get(), cachedPos.get(), cachedState.get());
        if (toDrop == BeeProdHoneys.VANILLA) return original;
        return toDrop.getBottled();
    }

    @ModifyArg(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;dropItem(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/ItemEntity;"))
    private ItemStack modDropStack(ItemStack original) {
        HoneyFlavor toDrop = getDroppedFlavor(cachedWorld.get(), cachedPos.get(), cachedState.get());
        if (toDrop == BeeProdHoneys.VANILLA) return original;
        return toDrop.getBottled();
    }

}
