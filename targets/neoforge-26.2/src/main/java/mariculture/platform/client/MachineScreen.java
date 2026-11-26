package mariculture.platform.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import mariculture.domain.fluid.FluidEffect;
import mariculture.domain.fluid.FluidSpec;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.Layout;
import mariculture.platform.machine.MachineMenu;

/**
 * The one screen for every machine: the panel, slots, tanks, bars and buttons are drawn from the machine
 * type's {@link Layout} with plain fills (no per-machine texture).
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    /** Face letters in Face order (down, up, north, south, west, east) and the legacy sorter colours for them. */
    private static final String FACES = "DUNSWE";
    private static final int[] FACE_COLORS = {0xFF555555, 0xFFFFFF55, 0xFF55FF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFFFFFF};
    private SlotHints hints;
    private static final int BG = 0xFFC6C6C6, DARK = 0xFF373737, LIGHT = 0xFFFFFFFF, SLOT = 0xFF8B8B8B;

    public MachineScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, menu.layout().width(), menu.layout().height());
        this.inventoryLabelY = menu.layout().inventoryY() - 11;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        List<Component> tip = widgetTooltip(mouseX - leftPos, mouseY - topPos);
        if (!tip.isEmpty()) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component c : tip) lines.add(c.getVisualOrderText());
            g.setTooltipForNextFrame(lines, mouseX, mouseY);
        }
        else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < menu.machineSlotCount() && menu.getCarried().isEmpty())
            g.setComponentTooltipForNextFrame(font, SlotHints.tooltip(menu.slotInfo(hoveredSlot.index)), mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = leftPos, y = topPos;
        panel(g, x, y, 176, imageHeight);
        if (imageWidth > 176) panel(g, x + 176, y, imageWidth - 176, Math.min(imageHeight, 104));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) slot(g, x + menu.layout().inventoryX() + c * 18, y + menu.layout().inventoryY() + r * 18);
        for (int c = 0; c < 9; c++) slot(g, x + menu.layout().inventoryX() + c * 18, y + menu.layout().inventoryY() + 58);
        for (Layout.Widget w : menu.layout().widgets()) {
            if (w instanceof Layout.SlotW s) slot(g, x + s.x(), y + s.y());
            else if (w instanceof Layout.TankW t) tank(g, t, x, y);
            else if (w instanceof Layout.ProgressW p) progress(g, p, x, y);
            else if (w instanceof Layout.ButtonW b) button(g, b, x, y);
        }
        slotHints(g, x, y);
    }

    /** Role borders, and the faint item an empty machine slot is meant for (covered so it reads as a hint). */
    private void slotHints(GuiGraphicsExtractor g, int x, int y) {
        int n = menu.machineSlotCount();
        if (hints == null) hints = new SlotHints(menu::slotInfo, n);
        boolean[] covered = new boolean[n];
        for (int i = 0; i < n; i++) {
            Slot s = menu.slots.get(i);
            if (s.hasItem()) continue;
            ItemStack ghost = hints.ghost(i);
            if (ghost.isEmpty()) continue;
            g.fakeItem(ghost, x + s.x, y + s.y);
            covered[i] = true;
        }
        g.nextStratum();
        for (int i = 0; i < n; i++) {
            Slot s = menu.slots.get(i);
            int sx = x + s.x, sy = y + s.y;
            if (covered[i]) g.fill(sx, sy, sx + 16, sy + 16, SlotHints.COVER);
            int colour = menu.slotInfo(i).role().colour;
            if (colour != 0) g.outline(sx - 1, sy - 1, 18, 18, colour);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        for (Layout.Widget w : menu.layout().widgets()) {
            if (w instanceof Layout.LabelW l) {
                Component text = l.gauge() < 0 ? Component.translatable(l.key()) : Component.translatable(l.key(), menu.gauge(l.gauge()));
                g.text(font, text, l.x(), l.y(), 0xFF404040, false);
            } else if (w instanceof Layout.ButtonW b) {
                g.text(font, String.valueOf(menu.gauge(b.stateGauge())), b.x() + b.w() / 2 - 2, b.y() + (b.h() - 8) / 2, 0xFFFFFFFF, false);
            }
            else if (w instanceof Layout.FaceW f) {
                int face = Math.floorMod(menu.gauge(f.gauge()), 6);
                g.text(font, FACES.substring(face, face + 1), f.x() + 11, f.y() + 9, FACE_COLORS[face], true);
            }
        }
    }

    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BG);
        g.fill(x, y, x + w - 1, y + 1, LIGHT);
        g.fill(x, y, x + 1, y + h - 1, LIGHT);
        g.fill(x + 1, y + h - 1, x + w, y + h, DARK);
        g.fill(x + w - 1, y + 1, x + w, y + h, DARK);
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT);
        g.fill(x - 1, y - 1, x + 16, y, DARK);
        g.fill(x - 1, y - 1, x, y + 16, DARK);
        g.fill(x, y + 16, x + 17, y + 17, LIGHT);
        g.fill(x + 16, y, x + 17, y + 17, LIGHT);
    }

    private void tank(GuiGraphicsExtractor g, Layout.TankW t, int ox, int oy) {
        int x = ox + t.x(), y = oy + t.y();
        g.fill(x - 1, y - 1, x + t.w() + 1, y + t.h() + 1, DARK);
        g.fill(x, y, x + t.w(), y + t.h(), 0xFF202020);
        int id = menu.tankFluid(t.tank());
        int cap = menu.tankCapacity(t.tank());
        if (id < 0 || cap <= 0) return;
        Fluid fluid = BuiltInRegistries.FLUID.byId(id);
        int filled = (int) Math.min(t.h(), (long) menu.tankAmount(t.tank()) * t.h() / cap);
        if (filled <= 0) return;
        String key = BuiltInRegistries.FLUID.getKey(fluid).toString();
        FluidSpec spec = Fluids.byId(key);
        // 26.1 fluid textures live in fluid models; Mariculture fluids use their own still texture, others fall back to water.
        Identifier still = Identifier.parse(spec != null ? spec.stillTexture() : "minecraft:block/water_still");
        int tint = spec == null || spec.effect() == FluidEffect.PRESSURISED ? 0xFF3F76E4 : 0xFFFFFFFF;
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(still);
        for (int drawn = 0; drawn < filled; drawn += 16) {
            int h = Math.min(16, filled - drawn);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y + t.h() - drawn - h, t.w(), h, tint);
        }
    }

    private void progress(GuiGraphicsExtractor g, Layout.ProgressW p, int ox, int oy) {
        int x = ox + p.x(), y = oy + p.y();
        g.fill(x, y, x + p.w(), y + p.h(), 0xFF555555);
        int max = p.max() < 0 ? p.fixedMax() : menu.gauge(p.max());
        if (max <= 0) return;
        double f = Math.max(0, Math.min(1, menu.gauge(p.value()) / (double) max));
        switch (p.direction()) {
            case "up" -> {
                int h = (int) (p.h() * f);
                g.fill(x, y + p.h() - h, x + p.w(), y + p.h(), p.color());
            }
            case "down" -> g.fill(x, y, x + p.w(), y + (int) (p.h() * f), p.color());
            default -> g.fill(x, y, x + (int) (p.w() * f), y + p.h(), p.color());
        }
    }

    private static void button(GuiGraphicsExtractor g, Layout.ButtonW b, int ox, int oy) {
        int x = ox + b.x(), y = oy + b.y();
        g.fill(x, y, x + b.w(), y + b.h(), DARK);
        g.fill(x + 1, y + 1, x + b.w() - 1, y + b.h() - 1, 0xFF6F6F6F);
    }

    private List<Component> widgetTooltip(int mx, int my) {
        List<Component> tip = new ArrayList<>();
        for (Layout.Widget w : menu.layout().widgets()) {
            if (w instanceof Layout.TankW t && in(mx, my, t.x(), t.y(), t.w(), t.h())) {
                int id = menu.tankFluid(t.tank());
                if (id < 0) tip.add(Component.translatable("gui.mariculture.tank.empty"));
                else tip.add(BuiltInRegistries.FLUID.byId(id).getFluidType().getDescription());
                tip.add(Component.translatable("gui.mariculture.tank.amount", Math.max(0, menu.tankAmount(t.tank())), menu.tankCapacity(t.tank())));
            } else if (w instanceof Layout.ButtonW b && in(mx, my, b.x(), b.y(), b.w(), b.h())) {
                tip.add(Component.translatable(b.key() + "." + menu.gauge(b.stateGauge())));
            }
        }
        return tip;
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x() - leftPos, my = (int) event.y() - topPos;
        for (Layout.Widget w : menu.layout().widgets()) {
            if (w instanceof Layout.ButtonW b && in(mx, my, b.x(), b.y(), b.w(), b.h())) {
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, b.id());
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
