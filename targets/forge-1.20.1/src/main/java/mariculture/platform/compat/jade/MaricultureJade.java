package mariculture.platform.compat.jade;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import mariculture.domain.content.Ids;
import mariculture.domain.view.RecipeViews;
import mariculture.platform.Stacks;
import mariculture.platform.block.OysterBlock;
import mariculture.platform.block.OysterBlockEntity;
import mariculture.platform.machine.MachineBlock;
import mariculture.platform.machine.MachineBlockEntity;

/**
 * Jade tooltips: the server sends each machine's {@code describe} lines (tanks, progress, temperature, feeder fish
 * and problems) and each oyster's contents; the client translates them. Only Jade loads this class.
 */
@WailaPlugin(Ids.MOD_ID)
public final class MaricultureJade implements IWailaPlugin {
    private static final String KEY = "mariculture_lines";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Provider.MACHINE, MachineBlockEntity.class);
        registration.registerBlockDataProvider(Provider.OYSTER, OysterBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Provider.MACHINE, MachineBlock.class);
        registration.registerBlockComponent(Provider.OYSTER, OysterBlock.class);
    }

    /** Lines as text: one per line, fields separated by tabs, the key first; arguments carry a type prefix. */
    static String encode(List<RecipeViews.Line> lines) {
        StringBuilder sb = new StringBuilder();
        for (RecipeViews.Line l : lines) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(l.key());
            for (Object a : l.args()) {
                sb.append('\t');
                if (a instanceof RecipeViews.Name n) sb.append(n.fluid() ? "f:" : "i:").append(n.id());
                else if (a instanceof String s && s.startsWith("lang:")) sb.append(s);
                else sb.append("s:").append(a);
            }
        }
        return sb.toString();
    }

    static List<Component> decode(String text) {
        List<Component> out = new ArrayList<>();
        if (text.isEmpty()) return out;
        for (String line : text.split("\n")) {
            String[] f = line.split("\t");
            Object[] args = new Object[f.length - 1];
            for (int i = 1; i < f.length; i++) args[i - 1] = arg(f[i]);
            out.add(Component.translatable(f[0], args));
        }
        return out;
    }

    private static Object arg(String a) {
        String v = a.substring(a.indexOf(':') + 1);
        if (a.startsWith("lang:")) return Component.translatable(v);
        if (a.startsWith("f:")) {
            Fluid fluid = MachineBlockEntity.fluid(v);
            return fluid == null ? v : fluid.getFluidType().getDescription();
        }
        if (a.startsWith("i:")) {
            Item item = Stacks.item(v);
            return item == null ? v : new ItemStack(item).getHoverName();
        }
        return v;
    }

    private enum Provider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        MACHINE("machine"), OYSTER("oyster");

        private final ResourceLocation uid;

        Provider(String path) {
            this.uid = new ResourceLocation(Ids.MOD_ID, path);
        }

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            List<RecipeViews.Line> lines = new ArrayList<>();
            if (accessor.getBlockEntity() instanceof MachineBlockEntity be) {
                be.machine().describe(lines);
            } else if (accessor.getBlockEntity() instanceof OysterBlockEntity oyster && oyster.contentsId() != null) {
                lines.add(new RecipeViews.Line("jade.mariculture.oyster", RecipeViews.Name.item(oyster.contentsId())));
            }
            if (!lines.isEmpty()) data.putString(KEY, encode(lines));
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            for (Component c : decode(accessor.getServerData().getString(KEY))) tooltip.add(c);
        }
    }
}
