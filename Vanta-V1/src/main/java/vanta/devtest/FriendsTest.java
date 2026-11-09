package vanta.devtest;

import vanta.friends.FriendList;
import vanta.mixin.accessor.MouseHandlerAccessor;
import vanta.ui.click.ClickGui;
import vanta.ui.click.FriendsDriver;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.UUIDUtil;

final class FriendsTest {
   private static RemotePlayer buddy;

   static void add() {
      UiTest.then(1, (mc) -> {
         FriendsDriver.clear();
         buddy = new RemotePlayer(mc.level, UUIDUtil.createOfflineProfile("Buddy"));
         LocalPlayer p = mc.player;
         p.setYRot(0.0F);
         p.setXRot(10.0F);
         buddy.setPos(p.getX(), p.getY(), p.getZ() + (double)2.5F);
         buddy.setYRot(180.0F);
         buddy.setId(-4242);
         mc.level.addEntity(buddy);
      });
      UiTest.then(20, (mc) -> {
         String var10000 = mc.crosshairPickEntity == null ? "nothing" : mc.crosshairPickEntity.getName().getString();
         UiTest.log("looking at " + var10000 + " screen=" + String.valueOf(mc.gui.screen()));
      });
      UiTest.then(1, (mc) -> ((MouseHandlerAccessor)mc.mouseHandler).client$onButton(mc.getWindow().handle(), new MouseButtonInfo(2, 0), 1));
      UiTest.then(3, (mc) -> ((MouseHandlerAccessor)mc.mouseHandler).client$onButton(mc.getWindow().handle(), new MouseButtonInfo(2, 0), 0));
      UiTest.then(10, (mc) -> {
         boolean var10001 = mc.crosshairPickEntity instanceof RemotePlayer && FriendList.all().isEmpty();
         String var10002 = String.valueOf(mc.crosshairPickEntity);
         FriendsDriver.check("middle click on a player adds no friend", var10001, "target=" + var10002 + " list=" + String.valueOf(FriendList.all()));
      });
      UiTest.shot("friends_middle_click");
      UiTest.then(1, (mc) -> {
         ClickGui.open();
         ClickGui.testResetLayout();
      });
      UiTest.then(25, (mc) -> FriendsDriver.clickTabButton());
      UiTest.then(25, (mc) -> UiTest.log("friends empty"));
      UiTest.shot("friends_empty");
      UiTest.then(1, (mc) -> FriendsDriver.typeAndEnter("Notch"));
      UiTest.then(5, (mc) -> FriendsDriver.typeAndEnter("jeb_"));
      UiTest.then(25, (mc) -> UiTest.log("friends two added " + String.valueOf(FriendList.all())));
      UiTest.shot("friends_two");
      UiTest.then(1, (mc) -> FriendsDriver.remove("Notch"));
      UiTest.then(25, (mc) -> UiTest.log("friends after remove " + String.valueOf(FriendList.all())));
      UiTest.shot("friends_removed");
      UiTest.then(1, (mc) -> FriendsDriver.pickOnline("Buddy"));
      UiTest.then(25, (mc) -> UiTest.log("friends after online pick " + String.valueOf(FriendList.all())));
      UiTest.shot("friends_online_pick");
      UiTest.then(1, (mc) -> {
         ClickGui.close();
         FriendsDriver.clear();
         if (buddy != null) {
            buddy.discard();
         }

      });
      UiTest.then(10, (mc) -> UiTest.log("friends done"));
   }

   private FriendsTest() {
   }
}
