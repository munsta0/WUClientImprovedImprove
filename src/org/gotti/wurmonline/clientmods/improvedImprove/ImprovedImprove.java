package org.gotti.wurmonline.clientmods.improvedImprove;

import java.io.*;
import java.util.logging.Logger;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;

import javassist.bytecode.Descriptor;


public class ImprovedImprove implements WurmMod, /*Configurable, */Initable {

    @Override
    public void init() {

        this.createNeededMethods();

        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();
            CtClass ctHeadsUpDisplay = HookManager.getInstance().getClassPool().get("com.wurmonline.client.renderer.gui.HeadsUpDisplay");
//
            CtClass[] paramTypes = {
                    classPool.get("com.wurmonline.shared.constants.PlayerAction"),
                    classPool.get("long[]"),
            };

            CtMethod ctSendAction = ctHeadsUpDisplay.getMethod("sendAction", Descriptor.ofMethod(CtPrimitiveType.voidType, paramTypes));
//
            /*Quick recap of what this does :
            * It will open the class HeadsUpDisplay, find the method sendAction(PlayerAction, long[])
            * Then it will loop the code below for all the methods found
            *
            * If the class of the method is SimpleServerConnectionClass and the name of the method is sendAction, it will execute the following code :
            *
            * Check if the action is an improve action and a toolbelt exists
            * If that check is true, it will then look into 3 different location that has an inventory to try and match the itemId it has as a target.
            *
            * The reason we need to look in 3 different location ( and probably more as we discover stuff that can't be reached ) is because the event
            * got trigger by a mouseHover action, and thus we only have the ID to go from
            *
            * If we manage to find the targetItem, we will then loop all 10 toolbelt slot and try to match the toolBeltItem.getType with itemTarget.getImproveIcon
            *
            * If we get a match, bingo! If not just send the same old boring code as existed before                *
            * */
            ctSendAction.instrument(new ExprEditor() {
                public void edit(MethodCall methodCall) throws CannotCompileException {
                    if (methodCall.getClassName().equals("com.wurmonline.client.comm.SimpleServerConnectionClass") && methodCall.getMethodName().equals("sendAction")) {
                        String replaceString =
                            //Check if the action is 192 IMPROVE and the toolbelt exists
                            "if (action.getId() == 192 && this.toolbeltComponent != null) {\n" +
//                            "   this.chatManager.textMessage(\"test\", 1.00f, 1.00f, 1.00f, \"This is an improve action\", false);\n" +
                            "   com.wurmonline.client.game.inventory.InventoryMetaItem matchedItem = null;\n" +

                            //Loop all inventoryContainerWindows which should contain the backpack and quiver slot
                            "   java.util.Iterator entries = this.inventoryContainerWindows.entrySet().iterator();\n" +
                            "   while (matchedItem == null && entries.hasNext()) {\n" +
//                            "       this.chatManager.textMessage(\"test\", 1.00f, 1.00f, 1.00f, \"Trying a new window\", false);\n" +
                            "       java.util.Map.Entry thisEntry = (java.util.Map.Entry) entries.next();\n" +
                            "       matchedItem = ((com.wurmonline.client.renderer.gui.InventoryContainerWindow)thisEntry.getValue()).getItem(Long.valueOf(limitedTargetIds[0]));\n" +
                            "       if ( matchedItem != null ){\n" +
//                            "           this.chatManager.textMessage(\"test\", 1.00f, 1.00f, 1.00f, \"Item found in backpack or quiver, breaking the loop\", false);\n" +
                            "           break;\n" +
                            "       }\n" +
                            "   }\n" +

                            //Loop all inventoryWindows which should contain all containers, bsb, forge, oven cart etc
                            "   java.util.Iterator entries2 = this.inventoryWindows.entrySet().iterator();\n" +
                            "   while (matchedItem == null && entries2.hasNext()) {\n" +
//                            "       this.chatManager.textMessage(\"test\", 1.00f, 1.00f, 1.00f, \"Trying a new window\", false);\n" +
                            "       java.util.Map.Entry thisEntry = (java.util.Map.Entry) entries2.next();\n" +
                            "       matchedItem = ((com.wurmonline.client.renderer.gui.ItemListWindow)thisEntry.getValue()).getItem(Long.valueOf(limitedTargetIds[0]));\n" +
                            "       if ( matchedItem != null ){\n" +
//                            "           this.chatManager.textMessage(\"test\", 1.00f, 1.00f, 1.00f, \"Item found in containers, breaking the loop\", false);\n" +
                            "           break;\n" +
                            "       }\n" +
                            "   }\n" +

                            //Look inside the inventory
                            "   if ( matchedItem == null ){\n" +
                            "       matchedItem = this.inventoryWindow.getInventoryListComponent().getItem(Long.valueOf(limitedTargetIds[0]));\n" +
                            "       if ( matchedItem != null ){\n" +
//                            "           this.chatManager.textMessage(\"test\", 1.00f, 1.00f, 1.00f, \"Item found in inventory\", false);\n" +
                            "       }\n" +
                            "   }\n" +

                            //If an item was found in one of the loop above
                            "   if ( matchedItem != null ) {\n" +
//                            "       this.chatManager.textMessage(\"test\", 1.00f, 1.00f, 1.00f, \"Match found.\", false);\n" +

                                    //Look in all the toolbelt slot and try to match the item.getType to the target.getImproveIcon
                            "       com.wurmonline.client.game.inventory.InventoryMetaItem sourceItem = null;\n" +
                            "       for(int i=0;i<=9;i++){\n" +
                            "           sourceItem = this.toolbeltComponent.getItemInSlot(i);\n" +
                            "           if ( sourceItem != null && sourceItem.getType() == matchedItem.getImproveIconId() ){\n" +
//                            "               this.chatManager.textMessage(\"test\", 1.00f, 1.00f, 1.00f, \"Found the correct tool!\", false);\n" +
                            "               this.world.getServerConnection().sendAction(sourceItem.getId(), limitedTargetIds, action);\n" +
                            "               return null;" +
                            "           }" +
                            "       }\n" +
                            "   }" +
                            "}\n" +
                            //If function doesn't hit a return, send the default command
                            "this.world.getServerConnection().sendAction(sourceItemId, limitedTargetIds, action);\n";
                        methodCall.replace(replaceString);
                    }

                }
            });
        } catch (NotFoundException | CannotCompileException e) {
            appendToFile(e);
            throw new HookException(e);
        }
    }

    private void createNeededMethods(){
        this.createMethodForBackPack();
        this.createMethodForInventory();
    }

    private void createMethodForBackPack() throws HookException{
        try {

            //Inject needed method getItem into the class InventoryContainerWindow, which will allow use to browse it's list of items and find the one we want
            CtClass cc = HookManager.getInstance().getClassPool().get("com.wurmonline.client.renderer.gui.InventoryContainerWindow");

            CtMethod m = CtNewMethod.make("" +
                    "public com.wurmonline.client.game.inventory.InventoryMetaItem getItem(Long itemId) {\n" +
                    "   com.wurmonline.client.renderer.gui.InventoryContainerWindow.InventoryContainerItem foundInventoryContainerItem = (com.wurmonline.client.renderer.gui.InventoryContainerWindow.InventoryContainerItem)this.itemList.get(itemId);\n" +
                    "   if ( foundInventoryContainerItem != null ){\n" +
                    "       return foundInventoryContainerItem.getItem();\n" +
                    "   } else {\n" +
                    "       return null;\n" +
                    "   }\n" +
                    "}\n", cc);

            cc.addMethod(m);

        } catch (Exception e) {
            appendToFile(e);
            throw new HookException(e);
        }
    }

    private void createMethodForInventory() throws HookException{
        try {

            //Inject needed method getItem into the class InventoryListComponent, which will allow use to browse it's list of items and find the one we want
            CtClass cc = HookManager.getInstance().getClassPool().get("com.wurmonline.client.renderer.gui.InventoryListComponent");

            CtMethod m = CtNewMethod.make("" +
                    "public com.wurmonline.client.game.inventory.InventoryMetaItem getItem(Long itemId) {\n" +
                    "   com.wurmonline.client.renderer.gui.InventoryListComponent.SingleTreeListItem foundSingleTreeListItem = (com.wurmonline.client.renderer.gui.InventoryListComponent.SingleTreeListItem)this.inventoryTreeListItems.get(itemId);\n" +
                    "   if ( foundSingleTreeListItem != null ){\n" +
                    "       return foundSingleTreeListItem.item;\n" +
                    "   } else {\n" +
                    "       return null;\n" +
                    "   }" +
                    "}", cc);
            cc.addMethod(m);
//
            //Inject needed method getItem into the class ItemListWindow, which will allow use to browse it's list of items and find the one we want
            CtClass cc2 = HookManager.getInstance().getClassPool().get("com.wurmonline.client.renderer.gui.ItemListWindow");

            CtMethod m2 = CtNewMethod.make("" +
                    "public com.wurmonline.client.game.inventory.InventoryMetaItem getItem(Long itemId) {" +
                    "   com.wurmonline.client.game.inventory.InventoryMetaItem foundItem = (com.wurmonline.client.game.inventory.InventoryMetaItem)this.component.getItem(itemId);\n" +
                    "   if ( foundItem != null ){\n" +
                    "       return foundItem;\n" +
                    "   } else {\n" +
                    "       return null;\n" +
                    "   }" +
                    "}", cc2);
            cc2.addMethod(m2);

        } catch (Exception e) {
            appendToFile(e);
            throw new HookException(e);
        }
    }

    /**
     * For anyone modding the client, this is seriously useful. It will write in exception.txt if the code can't be
     * injected in the client before it launches. It's the only way I found to get debug info.
     */
    public static void appendToFile(Exception e) {
        try {
            FileWriter fstream = new FileWriter("exception.txt", true);
            BufferedWriter out = new BufferedWriter(fstream);
            PrintWriter pWriter = new PrintWriter(out, true);
            e.printStackTrace(pWriter);
        }
        catch (Exception ie) {
            throw new RuntimeException("Could not write Exception to file", ie);
        }
    }
}
