package io.github.alloffabric.beeproductive.mixin;

import io.github.alloffabric.beeproductive.BeeProductive;
import io.github.alloffabric.beeproductive.api.BeeComponent;
import io.github.alloffabric.beeproductive.api.trait.BeeTrait;
import io.github.alloffabric.beeproductive.block.BeeFeederBlock;
import io.github.alloffabric.beeproductive.hooks.BeeEntityAccessor;
import io.github.alloffabric.beeproductive.init.BeeProdTags;
import io.github.alloffabric.beeproductive.init.BeeProdTraits;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;
import java.util.function.Predicate;

@Mixin(BeeEntity.class)
public abstract class MixinBeeEntity extends LivingEntity implements BeeEntityAccessor {

    protected MixinBeeEntity(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Shadow
    protected abstract void setHasNectar(boolean hasNectar);

    @ModifyArg(method = "initGoals", at = @At(value = "INVOKE", target = "Lnet/minecraft/recipe/Ingredient;fromTag(Lnet/minecraft/tag/Tag;)Lnet/minecraft/recipe/Ingredient;"))
    private Tag<Item> modTemptTag(Tag<Item> original) {
        return BeeProdTags.BEE_TEMPTING;
    }

    @Override
    public void beeproductive$setNectar(boolean hasNectar) {
        this.setHasNectar(hasNectar);
    }

    @ModifyArg(method = "isFlowers", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;isIn(Lnet/minecraft/tag/Tag;)Z"))
    private Tag<Block> modFeedTag(Tag<Block> original) {
        return BeeProdTags.BEE_FEEDING;
    }

    @Inject(method = "getAngerTime", at = @At("HEAD"), cancellable = true)
    private void modAnger(CallbackInfoReturnable<Integer> info) {
        BeeComponent component = BeeProductive.BEE_COMPONENT.get(this);
        if (component.getTraitValue(BeeProdTraits.PACIFIST)) info.setReturnValue(0);
    }

    @Inject(method = "createChild(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/PassiveEntity;)Lnet/minecraft/entity/passive/BeeEntity;", at = @At("RETURN"))
    @SuppressWarnings("unchecked")
    private void spawnChildBee(ServerWorld serverWorld, PassiveEntity partner, CallbackInfoReturnable<BeeEntity> info) {
        if (partner instanceof BeeEntity) {
            Random random = new Random();
            BeeComponent myComp = BeeProductive.BEE_COMPONENT.get(this);
            BeeComponent partnerComp = BeeProductive.BEE_COMPONENT.get(partner);
            BeeComponent childComp = BeeProductive.BEE_COMPONENT.get(info.getReturnValue());
            for (Identifier id : BeeProductive.BEE_TRAITS.getIds()) {
                BeeTrait trait = BeeProductive.BEE_TRAITS.get(id);
                if (random.nextBoolean()) {
                    childComp.setTraitValue(trait, myComp.getTraitValue(trait));
                } else {
                    childComp.setTraitValue(trait, partnerComp.getTraitValue(trait));
                }
            }
        }
    }

    @Mixin(targets = "net.minecraft.entity.passive.BeeEntity$PollinateGoal")
    public static abstract class MixinPollinateGoal {

        @Shadow
        @Final
        BeeEntity field_20377;

        @Shadow
        @Final
        @Mutable
        private Predicate<BlockState> flowerPredicate = (blockState) -> {
            if (blockState.isIn(BlockTags.TALL_FLOWERS)) {
                if (blockState.isOf(Blocks.SUNFLOWER)) {
                    return blockState.get(TallPlantBlock.HALF) == DoubleBlockHalf.UPPER;
                } else {
                    return true;
                }
            } else {
                return blockState.isIn(BeeProdTags.BEE_FEEDING);
            }
        };

        @Shadow
        protected abstract boolean completedPollination();

        @Inject(method = "stop()V", at = @At("HEAD"))
        private void applyNectar(CallbackInfo ci) {
            World world = field_20377.getEntityWorld();
            BlockPos pos = field_20377.getFlowerPos();
            BeeComponent component = BeeProductive.BEE_COMPONENT.get(field_20377);
            if (this.completedPollination() && pos != null) {
                if (world.getBlockState(pos).getBlock() instanceof BeeFeederBlock) {
                    component.setNectar(((BeeFeederBlock) world.getBlockState(pos).getBlock()).consumeNectar(world, pos));
                }
            }
        }
    }
}
