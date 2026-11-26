package mariculture.platform.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import mariculture.platform.magic.MirrorMenu;

/** The mirror: three jewelry slots, the enchanting slot and, for magic mirrors, three level buttons (plain fills). */
public class MirrorScreen extends AbstractContainerScreen<MirrorMenu> {
    private static final int BG = 0xFFC6C6C6, DARK = 0xFF373737, LIGHT = 0xFFFFFFFF, SLOT = 0xFF8B8B8B;
    private static final int BX = 104, BY = 17, BW = 64, BH = 19;

    public MirrorScreen(MirrorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float a, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, BG);
        g.fill(x, y, x + imageWidth - 1, y + 1, LIGHT);
        g.fill(x, y, x + 1, y + imageHeight - 1, LIGHT);
        g.fill(x + 1, y + imageHeight - 1, x + imageWidth, y + imageHeight, DARK);
        g.fill(x + imageWidth - 1, y + 1, x + imageWidth, y + imageHeight, DARK);
        for (var s : menu.slots) slot(g, x + s.x, y + s.y);
        for (int i = 0; i < 3; i++) {
            if (menu.level(i) <= 0) continue;
            int bx = x + BX, by = y + BY + i * (BH + 3);
            g.fill(bx, by, bx + BW, by + BH, menu.canEnchant(i) ? 0xFF6E5A8C : 0xFF555555);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        for (int i = 0; i < 3; i++) {
            if (menu.level(i) <= 0) continue;
            g.drawString(font, String.valueOf(menu.level(i)), BX + BW / 2 - 4, BY + i * (BH + 3) + 6, menu.canEnchant(i) ? 0xFF80FF20 : 0xFFA0A0A0, true);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < 3; i++) {
            double xx = mx - (leftPos + BX), yy = my - (topPos + BY + i * (BH + 3));
            if (xx >= 0 && yy >= 0 && xx < BW && yy < BH && menu.canEnchant(i)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private static void slot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT);
        g.fill(x - 1, y - 1, x + 16, y, DARK);
        g.fill(x - 1, y - 1, x, y + 16, DARK);
        g.fill(x, y + 16, x + 17, y + 17, LIGHT);
        g.fill(x + 16, y, x + 17, y + 17, LIGHT);
    }
}
