package vanta.util;

import java.util.function.Predicate;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class Placement {
   private static final float DUPLICATE_TOLERANCE = 1.0E-4F;
   private static boolean placed;
   private static boolean rotated;
   private static float lastRotationDelta;
   private static float lastPlacedDelta = -1.0F;

   public static boolean ready() {
      return !placed && (!rotated || !duplicates(lastRotationDelta));
   }

   public static boolean duplicates(float yawDelta) {
      return yawDelta > 2.0F && Math.abs(yawDelta - lastPlacedDelta) < 1.0E-4F;
   }

   public static void rotated(float yawDelta) {
      lastRotationDelta = yawDelta;
      rotated = true;
   }

   public static BlockHitResult placeInto(Level level, Vec3 eye, BlockPos target) {
      return placeInto(level, eye, target, (BlockPos)null);
   }

   public static BlockHitResult placeInto(Level level, Vec3 eye, BlockPos target, BlockPos avoid) {
      return placeIntoExcept(level, eye, target, (against) -> against.equals(avoid));
   }

   public static BlockHitResult placeIntoExcept(Level level, Vec3 eye, BlockPos target, Predicate<BlockPos> avoid) {
      BlockHitResult best = null;

      for(Direction face : Direction.values()) {
         BlockPos against = target.relative(face.getOpposite());
         if (!avoid.test(against) && level.getBlockState(against).isFaceSturdy(level, against, face)) {
            best = nearer(eye, best, candidate(level, eye, against, face));
         }
      }

      return best;
   }

   public static BlockHitResult clickOn(Level level, Vec3 eye, BlockPos block) {
      BlockHitResult best = null;

      for(Direction face : Direction.values()) {
         BlockPos neighbor = block.relative(face);
         if (level.getBlockState(neighbor).getCollisionShape(level, neighbor).isEmpty()) {
            best = nearer(eye, best, candidate(level, eye, block, face));
         }
      }

      return best;
   }

   public static boolean looksAt(Level level, BlockPos pos, Vec3 eye, Vec3 look, double range) {
      return shape(level, pos).clip(eye, eye.add(look.scale(range))).isPresent();
   }

   public static AABB shape(Level level, BlockPos pos) {
      VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
      return shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
   }

   private static BlockHitResult candidate(Level level, Vec3 eye, BlockPos block, Direction face) {
      return !canClick(eye, new AABB(block), face) ? null : new BlockHitResult(faceCenter(shape(level, block), face), face, block, false);
   }

   private static BlockHitResult nearer(Vec3 eye, BlockHitResult current, BlockHitResult candidate) {
      if (candidate == null) {
         return current;
      } else {
         return current != null && !(candidate.getLocation().distanceToSqr(eye) < current.getLocation().distanceToSqr(eye)) ? current : candidate;
      }
   }

   private static boolean canClick(Vec3 eye, AABB block, Direction face) {
      if (block.contains(eye)) {
         return true;
      } else {
         boolean var10000;
         switch (face) {
            case UP -> var10000 = eye.y >= block.maxY;
            case DOWN -> var10000 = eye.y <= block.minY;
            case NORTH -> var10000 = eye.z <= block.minZ;
            case SOUTH -> var10000 = eye.z >= block.maxZ;
            case WEST -> var10000 = eye.x <= block.minX;
            case EAST -> var10000 = eye.x >= block.maxX;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }
   }

   private static Vec3 faceCenter(AABB box, Direction face) {
      Vec3 center = box.getCenter();
      Vec3 var10000;
      switch (face) {
         case UP -> var10000 = new Vec3(center.x, box.maxY, center.z);
         case DOWN -> var10000 = new Vec3(center.x, box.minY, center.z);
         case NORTH -> var10000 = new Vec3(center.x, center.y, box.minZ);
         case SOUTH -> var10000 = new Vec3(center.x, center.y, box.maxZ);
         case WEST -> var10000 = new Vec3(box.minX, center.y, center.z);
         case EAST -> var10000 = new Vec3(box.maxX, center.y, center.z);
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public static void predict(LocalPlayer player, ItemStack stack, BlockHitResult hit) {
      Item var4 = stack.getItem();
      if (var4 instanceof BlockItem item) {
         BlockPlaceContext var6 = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
         BlockState state = item.getBlock().getStateForPlacement(var6);
         if (state != null) {
            Game.level().setBlock(var6.getClickedPos(), state, 11);
         }
      }

   }

   public static void sent(Packet<?> packet) {
      if (packet instanceof ServerboundClientTickEndPacket) {
         placed = false;
      } else if (packet instanceof ServerboundUseItemOnPacket) {
         placed = true;
         if (rotated) {
            lastPlacedDelta = lastRotationDelta;
         }

         rotated = false;
      }

   }

   private Placement() {
   }
}
