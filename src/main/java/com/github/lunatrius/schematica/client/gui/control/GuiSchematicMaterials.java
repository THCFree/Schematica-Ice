package com.github.lunatrius.schematica.client.gui.control;

import com.github.lunatrius.core.client.gui.GuiScreenBase;
import com.github.lunatrius.schematica.Schematica;
import com.github.lunatrius.schematica.client.util.BlockList;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.ItemStackSortType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.config.GuiUnicodeGlyphButton;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

public class GuiSchematicMaterials extends GuiScreenBase {
    private GuiSchematicMaterialsSlot guiSchematicMaterialsSlot;

    private ItemStackSortType sortType = ItemStackSortType.fromString(ConfigurationHandler.sortType);

    private GuiUnicodeGlyphButton btnSort = null;
    private GuiButton btnDump = null;
    private GuiButton btnDone = null;
    private GuiSlider slRadius = null;

    private final String strMaterialName = I18n.format(Names.Gui.Control.MATERIAL_NAME);
    private final String strMaterialAmount = I18n.format(Names.Gui.Control.MATERIAL_AMOUNT);
    protected static List<BlockList.WrappedItemStack> blockList = new ArrayList<>();

// TODO: Get these in the lang files.
    private final GuiSlider.FormatHelper sliderHelper = (id, name, value) -> {
        if (value < 5) {
            return "Radius: infinite";
        } else {
            return ("Radius: " + (int) (Math.floor((value)/5)*5));
        }
    };

    private final GuiPageButtonList.GuiResponder sliderResponder = new GuiPageButtonList.GuiResponder() {
        @Override
        public void setEntryValue(int id, boolean value) { }

        @Override
        public void setEntryValue(int id, float value) {
            if (value < 5) {
                ClientProxy.listRadius = 0;
            } else {
                ClientProxy.listRadius = (int) (Math.floor(value/ 5) * 5);
            }
        }

        @Override
        public void setEntryValue(int id, String value) { }
    };


    public GuiSchematicMaterials(final GuiScreen guiScreen) {
        super(guiScreen);
        final Minecraft minecraft = Minecraft.getMinecraft();
        final SchematicWorld schematic = ClientProxy.schematic;
        this.blockList = new BlockList().getList(minecraft.player, schematic, minecraft.world, ClientProxy.listRadius);
        this.sortType.sort(this.blockList);
    }

    @Override
    public void initGui() {
        int id = 0;

        this.btnSort = new GuiUnicodeGlyphButton(++id, this.width / 2 - 154, this.height - 50, 100, 20, " " + I18n.format(Names.Gui.Control.SORT_PREFIX + this.sortType.label), this.sortType.glyph, 2.0f);
        this.buttonList.add(this.btnSort);

        this.btnDump = new GuiButton(++id, this.width / 2 - 50, this.height - 50, 100, 20, I18n.format(Names.Gui.Control.DUMP));
        this.buttonList.add(this.btnDump);

        this.btnDone = new GuiButton(++id, this.width / 2 + 54, this.height - 50, 100, 20, I18n.format(Names.Gui.DONE));
        this.buttonList.add(this.btnDone);

        this.slRadius = new GuiSlider(sliderResponder, ++id,this.width / 2 - 75,this.height-25,"matRadius", 0, 100,ClientProxy.listRadius,sliderHelper) {
            @Override
            public void mouseReleased(int mouseX, int mouseY) {
                this.isMouseDown = false;
                updateMaterialList();
            }
        };
        this.buttonList.add(this.slRadius);

        this.guiSchematicMaterialsSlot = new GuiSchematicMaterialsSlot(this);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.guiSchematicMaterialsSlot.handleMouseInput();
    }

    @Override
    protected void actionPerformed(final GuiButton guiButton) {
        if (guiButton.enabled) {
            if (guiButton.id == this.btnSort.id) {
                this.sortType = this.sortType.next();
                this.sortType.sort(this.blockList);
                this.btnSort.displayString = " " + I18n.format(Names.Gui.Control.SORT_PREFIX + this.sortType.label);
                this.btnSort.glyph = this.sortType.glyph;

                ConfigurationHandler.propSortType.set(String.valueOf(this.sortType));
                ConfigurationHandler.loadConfiguration();
            } else if (guiButton.id == this.btnDump.id) {
                dumpMaterialList(this.blockList);
            } else if (guiButton.id == this.btnDone.id) {
                this.mc.displayGuiScreen(this.parentScreen);
            } else {
                this.guiSchematicMaterialsSlot.actionPerformed(guiButton);
            }
        }
    }

    @Override
    public void renderToolTip(final ItemStack stack, final int x, final int y) {
        super.renderToolTip(stack, x, y);
    }

    @Override
    public void drawScreen(final int x, final int y, final float partialTicks) {
        this.guiSchematicMaterialsSlot.drawScreen(x, y, partialTicks);

        drawString(this.fontRenderer, this.strMaterialName, this.width / 2 - 108, 4, 0x00FFFFFF);
        drawString(this.fontRenderer, this.strMaterialAmount, this.width / 2 + 108 - this.fontRenderer.getStringWidth(this.strMaterialAmount), 4, 0x00FFFFFF);
        super.drawScreen(x, y, partialTicks);
    }

    private void dumpMaterialList(final List<BlockList.WrappedItemStack> blockList) {
        if (blockList.size() <= 0) {
            return;
        }

        int maxLengthName = 0;
        int maxSize = 0;
        for (final BlockList.WrappedItemStack wrappedItemStack : blockList) {
            maxLengthName = Math.max(maxLengthName, wrappedItemStack.getItemStackDisplayName().length());
            maxSize = Math.max(maxSize, wrappedItemStack.total);
        }

        final int maxLengthSize = String.valueOf(maxSize).length();
        final String formatName = "%-" + maxLengthName + "s";
        final String formatSize = "%" + maxLengthSize + "d [%s Shulkers]";

        final StringBuilder stringBuilder = new StringBuilder((maxLengthName + 1 + maxLengthSize) * blockList.size());
        final Formatter formatter = new Formatter(stringBuilder);
        for (final BlockList.WrappedItemStack wrappedItemStack : blockList) {
            formatter.format(formatName, wrappedItemStack.getItemStackDisplayName());
            stringBuilder.append(" ");
            formatter.format(formatSize, wrappedItemStack.total, BlockList.WrappedItemStack.getFormattedStackAmountInShulkers(wrappedItemStack.itemStack, wrappedItemStack.total));
            stringBuilder.append(System.lineSeparator());
        }

        final File dumps = Schematica.proxy.getDirectory("dumps");
        try {
            try (FileOutputStream outputStream = new FileOutputStream(new File(dumps, Reference.MODID + "-materials.txt"))) {
                IOUtils.write(stringBuilder.toString(), outputStream, StandardCharsets.UTF_8);
            }
        } catch (final Exception e) {
            Reference.logger.error("Could not dump the material list!", e);
        }
    }

    public void updateMaterialList() {
        this.blockList = new ArrayList<>();
        final Minecraft minecraft = Minecraft.getMinecraft();
        final SchematicWorld schematic = ClientProxy.schematic;
        this.blockList = new BlockList().getList(minecraft.player, schematic, minecraft.world, ClientProxy.listRadius);
        this.sortType.sort(this.blockList);
    }

}
