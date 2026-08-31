package com.enginecore.client.gui;

import com.enginecore.config.EngineCoreConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class EngineCoreConfigScreen extends Screen {

    private final Screen parent;

    // Module switches
    private boolean aiThrottle;
    private boolean gcOptimizer;
    private boolean redstoneBatching;
    private boolean itemMergeOptimizer;
    private boolean animalOptimizer;
    private boolean lightDampener;

    // Distances
    private double nearDist;
    private double midDist;
    private double farDist;

    public EngineCoreConfigScreen(Screen parent) {
        super(Component.literal("EngineCore Configuration"));
        this.parent = parent;

        this.aiThrottle = EngineCoreConfig.ENABLE_AI_THROTTLE.get();
        this.gcOptimizer = EngineCoreConfig.ENABLE_GC_OPTIMIZER.get();
        this.redstoneBatching = EngineCoreConfig.ENABLE_REDSTONE_BATCHING.get();
        this.itemMergeOptimizer = EngineCoreConfig.ENABLE_ITEM_MERGE_OPTIMIZER.get();
        this.animalOptimizer = EngineCoreConfig.ENABLE_ANIMAL_OPTIMIZER.get();
        this.lightDampener = EngineCoreConfig.ENABLE_LIGHT_DAMPENER.get();

        this.nearDist = EngineCoreConfig.AI_NEAR_DISTANCE.get();
        this.midDist = EngineCoreConfig.AI_MID_DISTANCE.get();
        this.farDist = EngineCoreConfig.AI_FAR_DISTANCE.get();
    }

    @Override
    protected void init() {
        int midX = this.width / 2;
        int col1 = midX - 155;
        int col2 = midX + 5;
        int startY = 38;
        int btnW = 150;
        int btnH = 20;
        int spacing = 24;

        // --- Column 1: Core Switches ---
        this.addRenderableWidget(CycleButton.onOffBuilder(this.aiThrottle)
                .create(col1, startY, btnW, btnH, Component.literal("Mob AI Throttle"), (b, v) -> this.aiThrottle = v));

        this.addRenderableWidget(CycleButton.onOffBuilder(this.gcOptimizer)
                .create(col1, startY + spacing, btnW, btnH, Component.literal("GC Optimizer"), (b, v) -> this.gcOptimizer = v));

        this.addRenderableWidget(CycleButton.onOffBuilder(this.redstoneBatching)
                .create(col1, startY + (spacing * 2), btnW, btnH, Component.literal("Redstone Batcher"), (b, v) -> this.redstoneBatching = v));

        this.addRenderableWidget(CycleButton.onOffBuilder(this.itemMergeOptimizer)
                .create(col1, startY + (spacing * 3), btnW, btnH, Component.literal("Item Merger"), (b, v) -> this.itemMergeOptimizer = v));

        this.addRenderableWidget(CycleButton.onOffBuilder(this.animalOptimizer)
                .create(col1, startY + (spacing * 4), btnW, btnH, Component.literal("Animal Optimizer"), (b, v) -> this.animalOptimizer = v));

        this.addRenderableWidget(CycleButton.onOffBuilder(this.lightDampener)
                .create(col1, startY + (spacing * 5), btnW, btnH, Component.literal("Light Dampener"), (b, v) -> this.lightDampener = v));

        // --- Column 2: Distance Sliders ---
        this.addRenderableWidget(new DistanceSlider(col2, startY, btnW, btnH, "Near Dist", this.nearDist, 8.0, 64.0, val -> this.nearDist = val));
        this.addRenderableWidget(new DistanceSlider(col2, startY + spacing, btnW, btnH, "Mid Dist", this.midDist, 16.0, 96.0, val -> this.midDist = val));
        this.addRenderableWidget(new DistanceSlider(col2, startY + (spacing * 2), btnW, btnH, "Far Dist", this.farDist, 32.0, 160.0, val -> this.farDist = val));

        // --- Presets ---
        int presetBtnY = startY + (spacing * 4) + 2;
        int pBtnW = 46;
        this.addRenderableWidget(Button.builder(Component.literal("Safe"), b -> applyPreset(16, 32, 48, false)).bounds(col2, presetBtnY, pBtnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("Norm"), b -> applyPreset(16, 32, 48, true)).bounds(col2 + 52, presetBtnY, pBtnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("Max"), b -> applyPreset(12, 24, 36, true)).bounds(col2 + 104, presetBtnY, pBtnW, btnH).build());

        // --- Bottom Buttons: Save / Cancel ---
        int bottomY = this.height - 26;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            EngineCoreConfig.ENABLE_AI_THROTTLE.set(this.aiThrottle);
            EngineCoreConfig.ENABLE_GC_OPTIMIZER.set(this.gcOptimizer);
            EngineCoreConfig.ENABLE_REDSTONE_BATCHING.set(this.redstoneBatching);
            EngineCoreConfig.ENABLE_ITEM_MERGE_OPTIMIZER.set(this.itemMergeOptimizer);
            EngineCoreConfig.ENABLE_ANIMAL_OPTIMIZER.set(this.animalOptimizer);
            EngineCoreConfig.ENABLE_LIGHT_DAMPENER.set(this.lightDampener);

            EngineCoreConfig.AI_NEAR_DISTANCE.set(this.nearDist);
            EngineCoreConfig.AI_MID_DISTANCE.set(Math.max(this.nearDist, this.midDist));
            EngineCoreConfig.AI_FAR_DISTANCE.set(Math.max(this.midDist, this.farDist));
            EngineCoreConfig.SPEC.save();

            EngineCoreConfig.enableAiThrottle = this.aiThrottle;
            EngineCoreConfig.enableGcOptimizer = this.gcOptimizer;
            EngineCoreConfig.enableRedstoneBatching = this.redstoneBatching;
            EngineCoreConfig.enableItemMergeOptimizer = this.itemMergeOptimizer;
            EngineCoreConfig.enableAnimalOptimizer = this.animalOptimizer;
            EngineCoreConfig.enableLightDampener = this.lightDampener;
            EngineCoreConfig.aiNearDistance = this.nearDist;
            EngineCoreConfig.aiMidDistance = Math.max(this.nearDist, this.midDist);
            EngineCoreConfig.aiFarDistance = Math.max(this.midDist, this.farDist);

            this.minecraft.setScreen(this.parent);
        }).bounds(midX - 105, bottomY, 100, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
            this.minecraft.setScreen(this.parent);
        }).bounds(midX + 5, bottomY, 100, 20).build());
    }

    private void applyPreset(double near, double mid, double far, boolean heavyOptimization) {
        this.nearDist = near;
        this.midDist = mid;
        this.farDist = far;
        this.aiThrottle = true;
        this.gcOptimizer = true;
        this.itemMergeOptimizer = true;
        this.animalOptimizer = heavyOptimization;
        this.rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Korrekter renderBackground-Aufruf für 1.20.2
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        
        int textY = 38 + (24 * 3) + 12;
        guiGraphics.drawString(this.font, Component.literal("Presets:"), this.width / 2 + 6, textY, 0xAAAAAA);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private static class DistanceSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final java.util.function.Consumer<Double> responder;

        public DistanceSlider(int x, int y, int width, int height, String label, double current, double min, double max, java.util.function.Consumer<Double> responder) {
            super(x, y, width, height, Component.empty(), (current - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.responder = responder;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            int value = (int) Math.round(this.min + (this.value * (this.max - this.min)));
            this.setMessage(Component.literal(this.label + ": " + value + "m"));
        }

        @Override
        protected void applyValue() {
            double resolved = Math.round(this.min + (this.value * (this.max - this.min)));
            this.responder.accept(resolved);
        }
    }
}
