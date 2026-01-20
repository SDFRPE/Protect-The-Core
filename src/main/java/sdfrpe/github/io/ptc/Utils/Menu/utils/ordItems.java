package sdfrpe.github.io.ptc.Utils.Menu.utils;

import java.util.List;

public class ordItems {
   private Icon icon;
   private List<String> materialList;
   private Integer slot;
   private String command;
   private String permission;

   public ordItems(Icon icon, List<String> materialList, Integer slot, String command, String permission) {
      this.icon = icon;
      this.slot = slot;
      this.command = command;
      this.permission = permission;
      this.materialList = materialList;
   }

   public Icon getIcon() {
      return this.icon;
   }

   public List<String> getMaterialList() {
      return this.materialList;
   }

   public Integer getSlot() {
      return this.slot;
   }

   public String getCommand() {
      return this.command;
   }

   public String getPermission() {
      return this.permission;
   }

   public void setIcon(Icon icon) {
      this.icon = icon;
   }

   public void setMaterialList(List<String> materialList) {
      this.materialList = materialList;
   }

   public void setSlot(Integer slot) {
      this.slot = slot;
   }

   public void setCommand(String command) {
      this.command = command;
   }

   public void setPermission(String permission) {
      this.permission = permission;
   }
}
