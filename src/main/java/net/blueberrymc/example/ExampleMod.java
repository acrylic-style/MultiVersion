package net.blueberrymc.example;

import net.blueberrymc.common.bml.BlueberryMod;

public class ExampleMod extends BlueberryMod {
    @Override
    public void onLoad() {
        getLogger().info("Hello world!");
    }

    @Override
    public void onPostInit() {
        getLogger().info("Hello world again!");
    }
}
