package kor.bots.kraken.config;

import com.runemate.game.api.hybrid.*;
import com.runemate.ui.setting.annotation.open.*;
import com.runemate.ui.setting.open.*;
import kor.bots.kraken.enums.*;
import kor.framework.enums.*;

@SettingsGroup(group = "Kraken")
public interface KrakenConfig extends Settings {

    @SettingsSection(title = "Combat Settings", description = "Combat settings", order = 1)
    String combatSettings = "combatSettings";


    @Setting(key = "enableHeart", title = "Enable imbue/saturated", description = "Want to use Imbued or Saturated heart?", order = 2, section = combatSettings)
    default boolean enableHeart() {
        return false;
    }

    @Setting(key = "heart", title = "Heart Settings", section = combatSettings, order = 3)
    default KrakenHeart heart() {
        return KrakenHeart.IMBUED_HEART;
    }

    @Setting(key = "bloodSpells", title = "Enable blood spells", description = "Would you like to use blood spells?", order = 4, section = combatSettings)
    default boolean enableBloodSpells() {
        return false;
    }

    @Setting(key = "bloodSpell", title = "Blood spell", section = combatSettings, order = 5)
    default BloodSpells spell() {
        return BloodSpells.BLOOD_BURST;
    }

    @Setting(key = "prayFlick", title = "Pray Flick", description = "Would you like to enable prayer flicking?", order = 6, section = combatSettings)
    default boolean prayFlicker() {
        return true;
    }

    @SettingsSection(title = "Loot", description = "Loot settings", order = 2)
    String lootSettings = "lootSettings";

    @Setting(key = "highAlch", title = "High Level Alchemy", description = "Whether to use High Level Alchemy on suitable items or not", order = 1, section = lootSettings)
    default boolean highAlch() {
        return false;
    }

    @Setting(key = "noted", title = "Take Noted Items", description = "Pickup noted items that are dropped", order = 2, section = lootSettings)
    default boolean pickupNoted() {
        return true;
    }

    @SettingsSection(title = "Consumables", description = "Consumable settings", order = 3)
    String consumableSettings = "consumableSettings";

    @Setting(key = "food", title = "Food", section = consumableSettings, order = 1)
    default KrakenFood food() {
        return KrakenFood.COOKED_KARAMBWAN;
    }

    @Suffix(" pieces")
    @Setting(key = "foodQuantity", title = "Food Quantity", description = "Pieces of food to bring", order = 2, section = consumableSettings)
    default int foodQuantity() {
        return 15;
    }

    @Setting(key = "prayerPotions", title = "Prayer Potions", section = consumableSettings, order = 3)
    default PrayPotions prayerPotions() {
        return PrayPotions.NONE;
    }

    @Suffix(" pieces")
    @Setting(key = "prayerPotionQuantity", title = "Prayer Potion Quantity", description = "Pieces of Prayer Potions to bring", order = 4, section = consumableSettings)
    default int prayerPotionQuantity() {
        return 1;
    }

    @SettingsSection(title = "Traversal", description = "Traversal settings", order = 4)
    String TraversalSettings = "TraversalSettings";

    @Setting(key = "traversalToBank", title = "Traversal To Bank", section = TraversalSettings, order = 1)
    default TraversalToBank traversalToBank() {
        return TraversalToBank.GLORY;
    }

    @Setting(key = "traversalToCave", title = "Traversal To Cave", section = TraversalSettings, order = 2)
    default TraversalToCave traversalToCave() {
        return TraversalToCave.FAIRY_RING;
    }

    @SettingsSection(title = "Equipment", description = "Equipment settings", order = 4)
    String equipmentSettings = "equipmentSettings";

    @Setting(key = "magicEquipment", title = "Mage Equipment", converter = EquipmentLoadout.SettingConverter.class, order = 1, section = equipmentSettings)
    default EquipmentLoadout magicEquipment() {
        return new EquipmentLoadout();
    }

}
