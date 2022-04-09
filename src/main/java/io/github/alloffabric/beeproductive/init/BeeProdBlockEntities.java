package io.github.alloffabric.beeproductive.init;

import io.github.alloffabric.beeproductive.BeeProductive;
import io.github.alloffabric.beeproductive.block.entity.BeeFeederBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.function.Supplier;

public class BeeProdBlockEntities {
    public static void init() {
    }

    public static <T extends BlockEntity> BlockEntityType<T> register(String name, Supplier<T> supplier, Block... blocks) {
        return Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(BeeProductive.MODID, name), BlockEntityType.Builder.create(supplier, blocks).build(null));
    }    public static final BlockEntityType<BeeFeederBlockEntity> FEEDER_ENTITY = register("bee_feeder", BeeFeederBlockEntity::new, BeeProdBlocks.BEE_FEEDER);




}
