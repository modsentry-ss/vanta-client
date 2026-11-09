package vanta.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

public class BlockUtil {
   private static final Minecraft mc = Minecraft.getInstance();

   public static boolean isReplaceable(BlockPos blockPos) {
      return isReplaceable(mc.level.getBlockState(blockPos));
   }

   public static boolean isReplaceable(BlockState state) {
      if (!state.canBeReplaced()) {
         return false;
      } else if (!(state.getBlock() instanceof SnowLayerBlock)) {
         return true;
      } else {
         return (Integer)state.getValue(SnowLayerBlock.LAYERS) <= 1;
      }
   }

   public static boolean isInteractable(BlockPos blockPos) {
      return isInteractable(mc.level.getBlockState(blockPos).getBlock());
   }

   public static boolean isInteractable(Block block) {
      if (block instanceof BaseEntityBlock) {
         return true;
      } else if (block instanceof CraftingTableBlock) {
         return true;
      } else if (block instanceof AnvilBlock) {
         return true;
      } else if (block instanceof BedBlock) {
         return true;
      } else {
         if (block instanceof DoorBlock) {
            DoorBlock door = (DoorBlock)block;
            if (door.type().canOpenByHand()) {
               return true;
            }
         }

         if (block instanceof TrapDoorBlock) {
            return true;
         } else if (block instanceof FenceGateBlock) {
            return true;
         } else if (block instanceof FenceBlock) {
            return true;
         } else if (block instanceof ButtonBlock) {
            return true;
         } else if (block instanceof LeverBlock) {
            return true;
         } else if (block instanceof NoteBlock) {
            return true;
         } else if (block instanceof DiodeBlock) {
            return true;
         } else if (block instanceof CartographyTableBlock) {
            return true;
         } else if (block instanceof GrindstoneBlock) {
            return true;
         } else if (block instanceof LoomBlock) {
            return true;
         } else if (block instanceof StonecutterBlock) {
            return true;
         } else if (block instanceof SmithingTableBlock) {
            return true;
         } else {
            return block instanceof CakeBlock ? true : block instanceof RespawnAnchorBlock;
         }
      }
   }

   public static boolean isSolid(Block block) {
      if (block instanceof StairBlock) {
         return false;
      } else if (block instanceof SlabBlock) {
         return false;
      } else if (block instanceof EndPortalFrameBlock) {
         return false;
      } else if (block instanceof EndPortalBlock) {
         return false;
      } else if (block instanceof VineBlock) {
         return false;
      } else if (block instanceof CarvedPumpkinBlock) {
         return false;
      } else if (block instanceof CactusBlock) {
         return false;
      } else if (block instanceof VegetationBlock) {
         return false;
      } else if (block instanceof FallingBlock) {
         return false;
      } else if (block instanceof WebBlock) {
         return false;
      } else if (block instanceof IronBarsBlock) {
         return false;
      } else if (block instanceof CarpetBlock) {
         return false;
      } else if (block instanceof SnowLayerBlock) {
         return false;
      } else if (block instanceof FenceBlock) {
         return false;
      } else if (block instanceof FenceGateBlock) {
         return false;
      } else if (block instanceof WallBlock) {
         return false;
      } else if (block instanceof LadderBlock) {
         return false;
      } else if (block instanceof TorchBlock) {
         return false;
      } else if (block instanceof RedStoneWireBlock) {
         return false;
      } else if (block instanceof DiodeBlock) {
         return false;
      } else if (block instanceof BasePressurePlateBlock) {
         return false;
      } else if (block instanceof TripWireBlock) {
         return false;
      } else if (block instanceof TripWireHookBlock) {
         return false;
      } else if (block instanceof BaseRailBlock) {
         return false;
      } else if (block instanceof SlimeBlock) {
         return false;
      } else if (block instanceof HoneyBlock) {
         return false;
      } else {
         return block instanceof TntBlock ? false : block.defaultBlockState().isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
      }
   }

   public static boolean canPlace(ItemStack stack, BlockHitResult hit) {
      Item var3 = stack.getItem();
      if (var3 instanceof BlockItem blockItem) {
         BlockPlaceContext context = new BlockPlaceContext(mc.player, InteractionHand.MAIN_HAND, stack, hit);
         if (!context.canPlace()) {
            return false;
         } else {
            BlockState state = blockItem.getBlock().getStateForPlacement(context);
            BlockPos pos = context.getClickedPos();
            return state != null && state.canSurvive(mc.level, pos) && mc.level.isUnobstructed(state, pos, CollisionContext.of(mc.player));
         }
      } else {
         return false;
      }
   }
}
