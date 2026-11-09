package vanta.setting;

import java.util.List;
import java.util.Set;
import vanta.friends.FriendList;
import vanta.util.Entities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class TargetSettings implements SettingGroup {
   private static final String[] OPTIONS = new String[]{"Players", "Invisible", "Entities"};
   private final MultiSetting targets;
   private final EntitySetting entities;
   private final boolean frostAntiBot;

   public TargetSettings() {
      this(true);
   }

   public TargetSettings(boolean frostAntiBot) {
      this.targets = new MultiSetting("targets", OPTIONS, new int[]{0});
      this.entities = new EntitySetting("entities");
      this.frostAntiBot = frostAntiBot;
      this.entities.visibleWhen(() -> this.targets.has(2));
   }

   public List<Setting<?>> settings() {
      return List.of(this.targets, this.entities);
   }

   public boolean accepts(Player self, LivingEntity entity) {
      if (entity != self && entity.isAlive() && !entity.isSpectator() && !FriendList.protects(entity)) {
         if (this.frostAntiBot) {
         }

         if (entity.isInvisible() && !this.targets.has(1)) {
            return false;
         } else {
            return entity instanceof Player ? this.targets.has(0) : this.targets.has(2) && ((Set)this.entities.get()).contains(Entities.id(entity));
         }
      } else {
         return false;
      }
   }
}
