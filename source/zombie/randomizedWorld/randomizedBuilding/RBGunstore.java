// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.core.properties.IsoPropertyType;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.WeaponPart;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.sprite.IsoSprite;
import zombie.network.GameServer;
import zombie.scripting.objects.FaceType;
import zombie.scripting.objects.ItemKey;
import zombie.util.StringUtils;
import zombie.util.list.WeightedList;

public final class RBGunstore extends RandomizedBuildingBase {
    private static final int PROBABILITY_RIFLE_HUNTING = 40;
    private static final int PROBABILITY_RIFLE_M14 = 10;
    private static final int PROBABILITY_RIFLE_VARMINT = 60;
    private static final int PROBABILITY_RIFLE_L94 = 60;
    private static final int PROBABILITY_CARBINE_L92 = 60;
    private static final int PROBABILITY_CARBINE_TRAPPER = 80;
    private static final int PROBABILITY_RIFLE_JS14 = 80;
    private static final int PROBABILITY_RIFLE_MSR7T = 10;
    private static final int PROBABILITY_SHOTGUN = 60;
    private static final int PROBABILITY_SHOTGUN_JS3T = 10;
    private static final int PROBABILITY_PISTOL1 = 80;
    private static final int PROBABILITY_PISTOL2 = 40;
    private static final int PROBABILITY_PISTOL3 = 10;
    private static final int PROBABILITY_REVOLVER = 40;
    private static final int PROBABILITY_REVOLVER_LONG = 10;
    private static final int PROBABILITY_REVOLVER_SHORT = 60;
    private static final int PROBABILITY_AMMO_3030 = 60;
    private static final int PROBABILITY_AMMO_308 = 60;
    private static final int PROBABILITY_AMMO_357 = 20;
    private static final int PROBABILITY_AMMO_38 = 60;
    private static final int PROBABILITY_AMMO_556 = 20;
    private static final int PROBABILITY_AMMO_9mm = 60;
    private static final int PROBABILITY_AMMO_44 = 20;
    private static final int PROBABILITY_AMMO_45 = 20;
    private static final int PROBABILITY_AMMO_SHOTGUN = 80;
    private static final int PROBABILITY_DISPLAY_SHELF_RIFLES = 40;
    private static final int PROBABILITY_DISPLAY_SHELF_PISTOLS = 10;
    private static final int PROBABILITY_DISPLAY_COUNTER_AMMO = 40;
    private static final int PROBABILITY_DISPLAY_COUNTER_GUN = 40;
    private static final int PROBABILITY_DISPLAY_COUNTER_PISTOL = 80;
    private static final int PROBABILITY_SPAWN_RIFLE = 20;
    private static final int PROBABILITY_SPAWN_PISTOL = 20;
    private static final int PROBABILITY_SPAWN_BODYARMOR = 40;
    private static final WeightedList<ItemKey> PISTOLS = new WeightedList<>();
    private static final WeightedList<ItemKey> RIFLES = new WeightedList<>();
    private static final WeightedList<ItemKey> AMMO_BOXES = new WeightedList<>();
    private static final WeightedList<ItemKey> AMMO_CANS = new WeightedList<>();
    private static final WeightedList<ItemKey> AMMO_CASES = new WeightedList<>();

    @Override
    public void randomizeBuilding(BuildingDef def) {
        for (IsoObject obj : this.getBuildingObjects(def)) {
            IsoGridSquare sq = obj.getSquare();
            IsoSprite sprite = obj.getSprite();
            if (sprite != null) {
                PropertyContainer props = obj.getSprite().getProperties();
                String facing = props.get(IsoPropertyType.FACING);
                boolean facingE = FaceType.E.toString().equals(facing);
                boolean facingW = FaceType.W.toString().equals(facing);
                boolean facingN = FaceType.N.toString().equals(facing);
                if (props.has(IsoPropertyType.CUSTOM_NAME) && props.get(IsoPropertyType.CUSTOM_NAME).contains("Gun")) {
                    doGunShelves(facingE, sq);
                } else {
                    ItemContainer container = obj.getContainer();
                    if (container != null) {
                        String type = obj.getContainer().getType();
                        switch (type) {
                            case "counter":
                                doCounter(sq, props, facingE, facingW, facingN);
                                break;
                            case "locker":
                                doAmmoCans(props, facingE, facingW, facingN, sq);
                                break;
                            case "displaycase":
                                doDisplayCounter(sq, props, facingE, facingW, facingN);
                        }
                    }
                }
            }
        }
    }

    private static void doCounter(IsoGridSquare sq, PropertyContainer props, boolean facingE, boolean facingW, boolean facingN) {
        boolean result = true;

        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoSprite sprite = sq.getObjects().get(i).getSprite();
            PropertyContainer props1 = sprite.getProperties();
            if (props1.has(IsoPropertyType.CUSTOM_NAME)
                    && (
                        props1.get(IsoPropertyType.CUSTOM_NAME).contains("Phone")
                            || props1.get(IsoPropertyType.CUSTOM_NAME).contains("Register")
                            || props1.get(IsoPropertyType.CUSTOM_NAME).contains("Radio")
                    )
                || sprite.getName() != null && sprite.getName().contains("location_shop_generic_01_12")) {
                result = false;
                break;
            }
        }

        if (result) {
            int roll = Rand.Next(100);
            if (roll < 40) {
                return;
            }

            if (props.has(IsoPropertyType.GROUP_NAME) && props.get(IsoPropertyType.GROUP_NAME).contains("Corner")) {
                doCornerAmmoCans(facingE, facingW, facingN, sq);
            } else {
                doCounterAmmoDisplay(facingE, facingW, facingN, sq);
            }
        }
    }

    private static void doDisplayCounter(IsoGridSquare sq, PropertyContainer props, boolean facingE, boolean facingW, boolean facingN) {
        if (!props.has(IsoPropertyType.GROUP_NAME) || !props.get(IsoPropertyType.GROUP_NAME).contains("Corner")) {
            int roll = Rand.Next(100);
            if (roll < 40) {
                return;
            }

            if (roll < 80) {
                doHandgunCounterDisplay(facingE, facingW, facingN, sq);
            } else {
                doRifleCounterDisplay(facingE, facingW, facingN, sq);
            }
        }
    }

    private static void doAmmoCans(PropertyContainer props, boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq) {
        if (!props.has(IsoPropertyType.GROUP_NAME) || props.get(IsoPropertyType.GROUP_NAME).contains("Green")) {
            int ammoSlots = Rand.NextInclusive(1, 6);
            float xOffset = facingE ? 0.34F : (facingW ? 0.82F : 0.2F);
            float yOffset = facingE ? 0.2F : (facingN ? 0.82F : (facingW ? 0.2F : 0.34F));
            float zOffset = 0.52F;
            float xRotation = 0.0F;
            float yRotation = 0.0F;
            float zRotation = facingE ? 90.0F : (facingN ? 0.0F : (facingW ? 270.0F : 180.0F));

            for (int j = 0; j < ammoSlots; j++) {
                InventoryContainer ammoCan = InventoryItemFactory.CreateItem(getAmmoCan());
                sq.AddWorldInventoryItem(ammoCan, xOffset, yOffset, 0.52F, false, true);
                ItemPickerJava.rollContainerItem(ammoCan, null, ItemPickerJava.getItemPickerContainers().get(ammoCan.getType()));
                setWorldRotation(ammoCan, 0.0F, 0.0F, zRotation);
                if (!facingE && !facingW) {
                    xOffset += 0.14F;
                } else {
                    yOffset += 0.14F;
                }
            }
        }
    }

    private static void doBodyArmor(boolean facingE, IsoGridSquare sq) {
        int slotNumber = 4;
        int currentSlot = 1;
        float xOffset = facingE ? 0.12F : 0.28F;
        float yOffset = facingE ? 0.78F : 0.12F;
        float zOffset = 0.5F;
        float xRotation = 270.0F;
        float yRotation = facingE ? 270.0F : 0.0F;
        float zRotation = 0.0F;

        for (int j = 0; j < 4; j++) {
            InventoryItem vest = InventoryItemFactory.CreateItem(ItemKey.Clothing.VEST_BULLET_CIVILIAN);
            if (currentSlot == 1) {
                spawnBodyArmor(vest, sq, xOffset, yOffset, zOffset);
                zOffset += 0.2F;
            } else if (currentSlot == 2) {
                spawnBodyArmor(vest, sq, xOffset, yOffset, zOffset);
                zOffset -= 0.2F;
                if (facingE) {
                    yOffset -= 0.44F;
                } else {
                    xOffset += 0.44F;
                }
            } else if (currentSlot == 3) {
                spawnBodyArmor(vest, sq, xOffset, yOffset, zOffset);
                zOffset += 0.2F;
            } else if (currentSlot == 4) {
                spawnBodyArmor(vest, sq, xOffset, yOffset, zOffset);
            }

            setWorldRotation(vest, 270.0F, yRotation, 0.0F);
            if (GameServer.server && vest.getWorldItem() != null) {
                vest.getWorldItem().transmitCompleteItemToClients();
            }

            currentSlot++;
        }
    }

    private static void doCornerAmmoCans(boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq) {
        int caseSlots = 2;
        float xOffset = facingW ? 0.44F : (facingN ? 0.44F : 0.38F);
        float yOffset = facingE ? 0.82F : (facingN ? 0.82F : 0.34F);
        float zOffset = 0.38F;
        float xRotation = 0.0F;
        float yRotation = 0.0F;
        float zRotation = 0.0F;

        for (int j = 0; j < 2; j++) {
            InventoryContainer ammoCase = InventoryItemFactory.CreateItem(getAmmoCase());
            sq.AddWorldInventoryItem(ammoCase, xOffset, yOffset, 0.38F, false, true);
            ItemPickerJava.rollContainerItem(ammoCase, null, ItemPickerJava.getItemPickerContainers().get(ammoCase.getType()));
            setWorldRotation(ammoCase, 0.0F, 0.0F, 0.0F);
            xOffset += 0.38F;
        }

        int ammoSlots = 2;
        float xOffset2 = facingW ? 0.82F : (facingN ? 0.82F : 0.38F);
        float yOffset2 = facingE ? 0.52F : (facingN ? 0.52F : 0.64F);
        float zOffset2 = 0.38F;
        float xRotation2 = 0.0F;
        float yRotation2 = 0.0F;
        float zRotation2 = facingW ? 270.0F : (facingN ? 270.0F : 90.0F);

        for (int j = 0; j < 2; j++) {
            InventoryContainer ammoCan = InventoryItemFactory.CreateItem(getAmmoCan());
            sq.AddWorldInventoryItem(ammoCan, xOffset2, yOffset2, 0.38F, false, true);
            ItemPickerJava.rollContainerItem(ammoCan, null, ItemPickerJava.getItemPickerContainers().get(ammoCan.getType()));
            setWorldRotation(ammoCan, 0.0F, 0.0F, zRotation2);
            if (!facingE && !facingN) {
                yOffset2 += 0.14F;
            } else {
                yOffset2 -= 0.14F;
            }
        }
    }

    private static void doCounterAmmoDisplay(boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq) {
        int columns = 3;
        float xOffset = facingE ? 0.2F : (facingW ? 0.94F : 0.24F);
        float yOffset = facingE ? 0.84F : (facingW ? 0.84F : (facingN ? 0.94F : 0.24F));
        float zOffset = 0.38F;
        float xRotation = 0.0F;
        float yRotation = 0.0F;
        float zRotation = facingE ? 270.0F : (facingW ? 90.0F : (facingN ? 180.0F : 0.0F));

        for (int j = 0; j < 3; j++) {
            int typeRoll = Rand.Next(100);
            float xOffset2 = 0.0F;
            float yOffset2 = 0.0F;

            for (int k = 0; k < Rand.NextInclusive(1, 4); k++) {
                if (xOffset2 == 0.0F || yOffset2 == 0.0F) {
                    xOffset2 = xOffset;
                    yOffset2 = yOffset;
                }

                InventoryItem ammoBox = InventoryItemFactory.CreateItem(getAmmoBox());
                sq.AddWorldInventoryItem(ammoBox, xOffset2, yOffset2, 0.38F, false, true);
                setWorldRotation(ammoBox, 0.0F, 0.0F, zRotation);
                xOffset2 += facingE ? 0.12F : 0.0F;
                xOffset2 -= facingW ? 0.12F : 0.0F;
                yOffset2 -= facingN ? 0.12F : 0.0F;
                yOffset2 += !facingE && !facingW && !facingN ? 0.12F : 0.0F;
            }

            if (!facingE && !facingW) {
                xOffset += 0.28F;
            } else {
                yOffset -= 0.28F;
            }
        }
    }

    private static void doGunShelves(boolean facingE, IsoGridSquare sq) {
        int roll = Rand.Next(100);
        if (roll >= 40) {
            doGunShelfRifles(facingE, sq);
        } else if (roll >= 10) {
            doGunShelfHandguns(facingE, sq);
        } else {
            doBodyArmor(facingE, sq);
        }
    }

    private static void doGunShelfHandguns(boolean facingE, IsoGridSquare sq) {
        int column1Slots = 4;
        float xOffset = facingE ? 0.12F : 0.2F;
        float yOffset = facingE ? 0.92F : 0.12F;
        float zOffset = 0.48F;
        float xRotation = facingE ? 90.0F : 270.0F;
        float yRotation = facingE ? 90.0F : 0.0F;
        float zRotation = facingE ? 180.0F : 0.0F;

        for (int n = 0; n < 4; n++) {
            if (Rand.Next(100) >= 20) {
                HandWeapon gun = spawnPistol(getPistol());
                sq.AddWorldInventoryItem(gun, xOffset, yOffset, zOffset, false, true);
                setWorldRotation(gun, xRotation, yRotation, zRotation);
                zOffset += 0.08F;
            }
        }

        int column2Slots = 4;
        float xOffset2 = facingE ? 0.12F : 0.46F;
        float yOffset2 = facingE ? 0.64F : 0.12F;
        float zOffset2 = 0.48F;

        for (int nx = 0; nx < 4; nx++) {
            if (Rand.Next(100) >= 20) {
                HandWeapon gun = spawnPistol(getPistol());
                sq.AddWorldInventoryItem(gun, xOffset2, yOffset2, zOffset2, false, true);
                setWorldRotation(gun, xRotation, yRotation, zRotation);
                zOffset2 += 0.08F;
            }
        }

        int rifleSlots = 2;
        float xOffset3 = facingE ? 0.12F : 0.7F;
        float yOffset3 = facingE ? 0.2F : 0.12F;
        float zOffset3 = 0.6F;
        float xRotation3 = 0.0F;
        float yRotation3 = 90.0F;
        float zRotation3 = facingE ? 180.0F : 270.0F;

        for (int nxx = 0; nxx < 2; nxx++) {
            if (Rand.Next(100) >= 20) {
                HandWeapon gun = spawnRifle(getRifle());
                sq.AddWorldInventoryItem(gun, xOffset3, yOffset3, 0.6F, false, true);
                setWorldRotation(gun, 0.0F, 90.0F, zRotation3);
                yOffset3 += facingE ? 0.18F : 0.0F;
                xOffset3 += !facingE ? 0.18F : 0.0F;
            }
        }
    }

    private static void doGunShelfRifles(boolean facingE, IsoGridSquare sq) {
        int slotNumber = 4;
        float xOffset = facingE ? 0.12F : 0.56F;
        float yOffset = facingE ? 0.56F : 0.12F;
        float zOffset = 0.48F;
        float xRotation = facingE ? 90.0F : 270.0F;
        float yRotation = facingE ? 90.0F : 0.0F;
        float zRotation = facingE ? 180.0F : 0.0F;

        for (int n = 0; n < 4; n++) {
            if (Rand.Next(100) >= 20) {
                HandWeapon gun = spawnRifle(getRifle());
                sq.AddWorldInventoryItem(gun, xOffset, yOffset, zOffset, false, true);
                setWorldRotation(gun, xRotation, yRotation, zRotation);
                zOffset += 0.08F;
            }
        }
    }

    private static void doHandgunCounterDisplay(boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq) {
        float xOffset = facingE ? 0.8F : (facingW ? 0.42F : (facingN ? 0.82F : 0.38F));
        float yOffset = facingE ? 0.8F : (facingW ? 0.4F : (facingN ? 0.44F : 0.8F));
        float zOffset = 0.38F;
        float xRotation = 0.0F;
        float yRotation = 0.0F;
        float zRotation = facingE ? 270.0F : (facingW ? 90.0F : (facingN ? 180.0F : 0.0F));
        HandWeapon gun = spawnPistol(getPistol());
        sq.AddWorldInventoryItem(gun, xOffset, yOffset, 0.38F, false, true);
        setWorldRotation(gun, 0.0F, 0.0F, zRotation);
        if (!StringUtils.isNullOrEmpty(gun.getMagazineType())) {
            float xOffset2 = facingE ? 0.88F : (facingW ? 0.32F : 0.58F);
            float yOffset2 = facingE ? 0.62F : (facingW ? 0.58F : (facingN ? 0.34F : 0.84F));
            float zRotation2 = facingE ? 180.0F : (facingW ? 0.0F : (facingN ? 90.0F : 270.0F));
            int clipSlots = Rand.NextInclusive(1, 2);

            for (int j = 0; j < clipSlots; j++) {
                InventoryItem clip = InventoryItemFactory.CreateItem(gun.getMagazineType());
                sq.AddWorldInventoryItem(clip, xOffset2, yOffset2, 0.38F, false, true);
                setWorldRotation(clip, 0.0F, 0.0F, zRotation2);
                yOffset2 -= facingE ? 0.06F : 0.0F;
                yOffset2 += facingW ? 0.06F : 0.0F;
                xOffset2 -= facingN ? 0.06F : 0.0F;
                xOffset2 += !facingE && !facingW && !facingN ? 0.06F : 0.0F;
            }
        }

        if (!StringUtils.isNullOrEmpty(gun.getAmmoBox())) {
            float xOffset2 = facingE ? 0.76F : (facingW ? 0.48F : (facingN ? 0.32F : 0.84F));
            float yOffset2 = facingE ? 0.38F : (facingW ? 0.84F : (facingN ? 0.48F : 0.78F));
            float zRotation2 = facingE ? 270.0F : 0.0F;
            int boxSlots = Rand.NextInclusive(1, 2);

            for (int j = 0; j < boxSlots; j++) {
                InventoryItem ammoBox = InventoryItemFactory.CreateItem(gun.getAmmoBox());
                sq.AddWorldInventoryItem(ammoBox, xOffset2, yOffset2, 0.38F, false, true);
                setWorldRotation(ammoBox, 0.0F, 0.0F, zRotation2);
                xOffset2 += facingE ? 0.12F : 0.0F;
                xOffset2 -= facingW ? 0.12F : 0.0F;
                yOffset2 -= facingN ? 0.12F : 0.0F;
                yOffset2 += !facingE && !facingW && !facingN ? 0.12F : 0.0F;
            }
        }
    }

    private static void doRifleCounterDisplay(boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq) {
        float xOffset = facingE ? 0.82F : (facingW ? 0.42F : 0.6F);
        float yOffset = facingE ? 0.58F : (facingW ? 0.62F : (facingN ? 0.42F : 0.86F));
        float zOffset = 0.38F;
        float xRotation = 0.0F;
        float yRotation = 0.0F;
        float standardZRotation = facingE ? 270.0F : (facingW ? 90.0F : (facingN ? 180.0F : 0.0F));
        HandWeapon gun = spawnRifle(getRifle());
        sq.AddWorldInventoryItem(gun, xOffset, yOffset, 0.38F, false, true);
        setWorldRotation(gun, 0.0F, 0.0F, standardZRotation);
        if (!StringUtils.isNullOrEmpty(gun.getAmmoBox())) {
            float xOffset2 = facingE ? 0.66F : (facingW ? 0.58F : (facingN ? 0.88F : 0.32F));
            float yOffset2 = facingE ? 0.88F : (facingW ? 0.34F : (facingN ? 0.6F : 0.68F));
            int boxSlots = Rand.NextInclusive(1, 2);

            for (int j = 0; j < boxSlots; j++) {
                InventoryItem ammoBox = InventoryItemFactory.CreateItem(gun.getAmmoBox());
                sq.AddWorldInventoryItem(ammoBox, xOffset2, yOffset2, 0.38F, false, true);
                setWorldRotation(ammoBox, 0.0F, 0.0F, standardZRotation);
                yOffset2 -= facingE ? 0.22F : 0.0F;
                yOffset2 += facingW ? 0.22F : 0.0F;
                xOffset2 -= facingN ? 0.22F : 0.0F;
                xOffset2 += !facingE && !facingW && !facingN ? 0.22F : 0.0F;
            }
        }
    }

    private static void spawnBodyArmor(InventoryItem vest, IsoGridSquare sq, float xOffset, float yOffset, float zOffset) {
        if (Rand.Next(100) >= 40) {
            sq.AddWorldInventoryItem(vest, xOffset, yOffset, zOffset, false, true);
        }
    }

    private static void setWorldRotation(InventoryItem item, float xRotation, float yRotation, float zRotation) {
        item.setWorldXRotation(xRotation);
        item.setWorldYRotation(yRotation);
        item.setWorldZRotation(zRotation);
        if (GameServer.server && item.getWorldItem() != null) {
            item.getWorldItem().transmitCompleteItemToClients();
        }
    }

    private static void addClip(HandWeapon gun) {
        if (gun.usesExternalMagazine() && !gun.isContainsClip()) {
            gun.setContainsClip(true);
        }
    }

    private static HandWeapon spawnPistol(ItemKey gunType) {
        HandWeapon gun = InventoryItemFactory.CreateItem(gunType);
        addClip(gun);
        return gun;
    }

    private static HandWeapon spawnRifle(ItemKey gunType) {
        HandWeapon gun = InventoryItemFactory.CreateItem(gunType);
        addClip(gun);
        if (gun.is(ItemKey.Weapon.VARMINT_RIFLE)) {
            WeaponPart scope = InventoryItemFactory.CreateItem(ItemKey.WeaponPart.X2_SCOPE);
            gun.attachWeaponPart(scope);
        }

        if (gun.is(ItemKey.Weapon.HUNTING_RIFLE)) {
            ItemKey scopeType = ItemKey.WeaponPart.X4_SCOPE;
            if (Rand.Next(100) >= 60) {
                scopeType = ItemKey.WeaponPart.X8_SCOPE;
            }

            WeaponPart scope = InventoryItemFactory.CreateItem(scopeType);
            gun.attachWeaponPart(scope);
        }

        return gun;
    }

    private static ItemKey getRifle() {
        if (RIFLES.isEmpty()) {
            RIFLES.add(ItemKey.Weapon.ASSAULT_RIFLE_2, 10);
            RIFLES.add(ItemKey.Weapon.HUNTING_RIFLE, 40);
            RIFLES.add(ItemKey.Weapon.SHOTGUN, 60);
            RIFLES.add(ItemKey.Weapon.JS3T_SHOTGUN, 10);
            RIFLES.add(ItemKey.Weapon.VARMINT_RIFLE, 60);
            RIFLES.add(ItemKey.Weapon.L94_RIFLE, 60);
            RIFLES.add(ItemKey.Weapon.L92_CARBINE, 60);
            RIFLES.add(ItemKey.Weapon.TRAPPER_CARBINE, 80);
            RIFLES.add(ItemKey.Weapon.MSR7T_RIFLE, 10);
            RIFLES.add(ItemKey.Weapon.JS14_RIFLE, 80);
        }

        return RIFLES.getRandom();
    }

    private static ItemKey getPistol() {
        if (PISTOLS.isEmpty()) {
            PISTOLS.add(ItemKey.Weapon.PISTOL_3, 10);
            PISTOLS.add(ItemKey.Weapon.REVOLVER_LONG, 10);
            PISTOLS.add(ItemKey.Weapon.REVOLVER, 40);
            PISTOLS.add(ItemKey.Weapon.PISTOL_2, 40);
            PISTOLS.add(ItemKey.Weapon.PISTOL, 80);
            PISTOLS.add(ItemKey.Weapon.REVOLVER_SHORT, 60);
        }

        return PISTOLS.getRandom();
    }

    private static ItemKey getAmmoBox() {
        if (AMMO_BOXES.isEmpty()) {
            AMMO_BOXES.add(ItemKey.Normal.BOX_556, 20);
            AMMO_BOXES.add(ItemKey.Normal.BULLETS_45_BOX, 20);
            AMMO_BOXES.add(ItemKey.Normal.BULLETS_44_BOX, 20);
            AMMO_BOXES.add(ItemKey.Normal.BOX_308, 60);
            AMMO_BOXES.add(ItemKey.Normal.BULLETS_9MM_BOX, 60);
            AMMO_BOXES.add(ItemKey.Normal.SHOTGUN_SHELLS_BOX, 80);
            AMMO_BOXES.add(ItemKey.Normal.BULLETS_38_BOX, 60);
            AMMO_BOXES.add(ItemKey.Normal.BULLETS_357_BOX, 20);
            AMMO_BOXES.add(ItemKey.Normal.BOX_3030, 60);
        }

        return AMMO_BOXES.getRandom();
    }

    private static ItemKey getAmmoCan() {
        if (AMMO_CANS.isEmpty()) {
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX, 20);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_45, 20);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_44, 20);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_308, 60);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_9MM, 60);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_SHOTGUN_SHELLS, 80);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_38, 60);
        }

        return AMMO_CANS.getRandom();
    }

    private static ItemKey getAmmoCase() {
        if (AMMO_CASES.isEmpty()) {
            AMMO_CASES.add(ItemKey.Container.BAG_PROTECTIVE_CASE_BULKY_AMMO, 20);
            AMMO_CASES.add(ItemKey.Container.BAG_PROTECTIVE_CASE_BULKY_AMMO_45, 20);
            AMMO_CASES.add(ItemKey.Container.BAG_PROTECTIVE_CASE_BULKY_AMMO_44, 20);
            AMMO_CASES.add(ItemKey.Container.BAG_PROTECTIVE_CASE_BULKY_AMMO_308, 60);
            AMMO_CASES.add(ItemKey.Container.BAG_PROTECTIVE_CASE_BULKY_AMMO_9MM, 60);
            AMMO_CASES.add(ItemKey.Container.BAG_PROTECTIVE_CASE_BULKY_AMMO_SHOTGUN_SHELLS, 80);
            AMMO_CASES.add(ItemKey.Container.BAG_PROTECTIVE_CASE_BULKY_AMMO_38, 60);
        }

        return AMMO_CASES.getRandom();
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("gunstore") != null;
    }

    public ArrayList<IsoObject> getBuildingObjects(BuildingDef def) {
        ArrayList<IsoObject> objList = new ArrayList<>();
        ArrayList<IsoGridSquare> buildingSquares = this.getBuildingSquares(def);

        for (int i = 0; i < buildingSquares.size(); i++) {
            IsoGridSquare sq = buildingSquares.get(i);

            for (int j = 0; j < sq.getObjects().size(); j++) {
                if (sq.getObjects().get(j) != null) {
                    objList.add(sq.getObjects().get(j));
                }
            }
        }

        return objList;
    }

    public ArrayList<IsoGridSquare> getBuildingSquares(BuildingDef def) {
        ArrayList<RoomDef> rooms = def.getRooms();
        ArrayList<IsoGridSquare> buildingSquares = new ArrayList<>();

        for (int i = 0; i < rooms.size(); i++) {
            RoomDef room = rooms.get(i);
            ArrayList<RoomDef.RoomRect> rects = room.getRects();

            for (int j = 0; j < rects.size(); j++) {
                RoomDef.RoomRect rect = rects.get(j);
                ArrayList<IsoGridSquare> rectSquares = this.getRectSquares(rect, room);
                buildingSquares.addAll(rectSquares);
            }
        }

        return buildingSquares;
    }

    public ArrayList<IsoGridSquare> getRectSquares(RoomDef.RoomRect rect, RoomDef room) {
        ArrayList<IsoGridSquare> squares = new ArrayList<>();

        for (int x = rect.getX(); x < rect.getX2(); x++) {
            for (int y = rect.getY(); y < rect.getY2(); y++) {
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, room.getZ());
                if (sq != null && sq.getRoom() != null && sq.getRoom().getName().contains("gunstore")) {
                    squares.add(sq);
                }
            }
        }

        return squares;
    }

    public RBGunstore() {
        this.name = "Gunstore";
        this.setAlwaysDo(true);
    }
}
