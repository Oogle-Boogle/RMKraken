package kor.bots.kraken;

import com.runemate.game.api.client.*;
import com.runemate.game.api.hybrid.*;
import com.runemate.game.api.hybrid.entities.*;
import com.runemate.game.api.hybrid.input.*;
import com.runemate.game.api.hybrid.input.direct.*;
import com.runemate.game.api.hybrid.local.*;
import com.runemate.game.api.hybrid.local.hud.interfaces.*;
import com.runemate.game.api.hybrid.location.*;
import com.runemate.game.api.hybrid.location.navigation.*;
import com.runemate.game.api.hybrid.region.*;
import com.runemate.game.api.hybrid.util.calculations.*;
import com.runemate.game.api.hybrid.web.*;
import com.runemate.game.api.osrs.local.hud.interfaces.*;
import com.runemate.game.api.script.*;
import com.runemate.game.api.script.framework.listeners.*;
import com.runemate.game.api.script.framework.listeners.events.*;
import com.runemate.game.api.script.framework.listeners.events.ItemEvent;
import com.runemate.ui.*;
import com.runemate.ui.setting.annotation.open.*;

import java.awt.event.*;
import java.util.*;
import java.util.regex.*;

import kor.framework.prayerhandler.PrayerHandler;
import lombok.*;
import lombok.extern.log4j.*;
import kor.bots.kraken.config.*;
import kor.bots.kraken.enums.*;
import kor.framework.*;
import kor.framework.breaking.*;
import kor.framework.discordhandler.*;

@Log4j2(topic = "Kraken")
public class Kraken extends BreakingLoop implements SettingsListener, EngineListener, ChatboxListener, Breaking, InventoryListener, NpcListener {

    boolean started = false;
    State state;

    private static final String FISHING_EXPLOSIVE = "Fishing explosive";
    public static final int[] ALCHABLES = {1403, 1347, 1303, 4091, 20425, 4093, 20426};

    @Getter
    @SettingsProvider(updatable = true)
    private KrakenConfig config;

    @SettingsProvider(updatable = true)
    private BreakConfig breakConfig;

    private void loot() {

        DefaultUI.setStatus("Picking up loot...");

        if (Prayer.isQuickPraying()) {
            if (Prayer.toggleQuickPrayers()) {
                return;
            }
        }

        if (!Inventory.isFull()) {
            DefaultUI.setStatus("Looting...");

            var misc = GroundItems.newQuery()
                    .names(Pattern.compile("(Rune|rune|Mystic|Trident|Magic|Torstol|Kraken|Jar|Coins|Sanfew)"))
                    .actions("Take")
                    .results()
                    .first();
            if (misc != null && misc.interact("Take")) {
                Execution.delayWhile(misc::isValid, 1200);
                return;
            }

            GroundItem brimKey = GroundItems.newQuery().names("Brimstone key").results().first();
            if (brimKey != null) {
                if (brimKey.interact("Take")) {
                    Execution.delayUntil(() -> Inventory.contains("Brimstone key"), 2000);
                    return;
                }
            }

            GroundItem noted = GroundItems.newQuery().actions("Take").noted().reachable().results().nearest();
            if (noted != null && config.pickupNoted()) {
                if (noted.interact("Take")) {
                    Execution.delayUntil(() -> !noted.isValid(), 2800);
                }
            }

            if (Inventory.getQuantity(config.food().getGameName()) <= 5 && Health.getCurrentPercent() <= 45) {
                GroundItem food = GroundItems.newQuery()
                        .names("Shark", "Edible seaweed", "Cooked karambwan", "Lobster", "Anglerfish", "Manta ray", "Swordfish")
                        .results()
                        .nearest();
                //Food
                if (food != null && food.interact("Take")) {
                    Execution.delayUntil(() -> !food.isValid(), 1200);
                    return;
                }
            }
        }

        if (Inventory.isFull() && Health.getCurrentPercent() <= 74) {
            DefaultUI.setStatus("Eating food to carry more inventory space...");
            var food = Inventory.newQuery().actions("Eat").results().first();
            if (food != null && food.interact("Eat")) {
                Execution.delayWhile(food::isValid, 1200);
                return;
            }
        } else if (Inventory.isFull() && Health.getCurrentPercent() >= 75) {
            //drop food
            DefaultUI.setStatus("Dropping food, we can will pickup when needs be....");
            var food = Inventory.newQuery().actions("Eat").results().first();
            if (food != null && food.interact("Drop")) {
                Execution.delayWhile(food::isValid, 1200);
            }
        }
        state = State.THROW;
    }

    private void bank() {

        if (Prayer.isQuickPraying()) {
            if (Prayer.toggleQuickPrayers()) {
                return;
            }
        }

        if (Bank.isOpen()) {
            DefaultUI.setStatus("Banking....");

            //TODO -
            if (config.magicEquipment() != null && !config.magicEquipment().isEmpty()) {
                mageSwitch();
            }

            if (Inventory.containsAnyOf("Coins",
                    "Mystic water staff",
                    "Rune warhammer",
                    "Rune longsword",
                    "Mystic robe top",
                    "Mystic robe bottom",
                    "Trident of the seas (full)",
                    "Torstol seed",
                    "Magic seed",
                    "Battlestaff",
                    "Diamond",
                    "Oak plank",
                    "Seaweed",
                    "Unpowered orb",
                    "Raw shark",
                    "Raw monkfish",
                    "Grimy snapdragon",
                    "Runite bar",
                    "Kraken tentacle",
                    "Crystal key",
                    "Dragonstone ring",
                    "Antidote++(4)",
                    "Jar of dirt",
                    "Pet kraken",
                    "Brimstone key",
                    "Clue scroll (elite)"
            )) {
                if (Bank.depositInventory()) {
                    Execution.delayUntil(Inventory::isEmpty, 1200);
                    return;
                }
            }

            if (Inventory.contains("Amulet of glory") && Bank.deposit("Amulet of glory", 1)) {
                Execution.delayUntil(() -> !Inventory.contains("Amulet of glory"), 800);
                return;
            }

            if (!Bank.contains(config.food().getGameName())
                    || !Bank.contains(config.prayerPotions().getGameName())
                    || !Bank.contains(config.traversalToBank().getGameName())) {
                stop("Ran out of supplies. Please make sure you have all the correct supplies + teleports.");
                return;
            }

            if (Inventory.getQuantity(config.prayerPotions().getGameName()) > config.prayerPotionQuantity()) {
                if (Bank.deposit(
                        config.prayerPotions().getGameName(),
                        Inventory.getQuantity(config.prayerPotions().getGameName()) - config.prayerPotionQuantity()
                )) {
                    Execution.delay(1000, 2400);
                    return;
                }
            }

            if (!Bank.contains(FISHING_EXPLOSIVE) && !Inventory.contains(FISHING_EXPLOSIVE)) {
                stop("Ran out of Fishing Explosives...");
                return;
            }

            if (!Inventory.contains(config.traversalToBank().getGameName())) {
                if (Bank.withdraw(config.traversalToBank().getGameName(), 1)) {
                    Execution.delayUntil(() -> Inventory.contains(config.traversalToBank().getGameName()), 1200);
                    return;
                }
            }

            if (!Inventory.contains(config.traversalToCave().getGameName())) {
                if (Bank.withdraw(config.traversalToCave().getGameName(), 1)) {
                    Execution.delayUntil(() -> Inventory.contains(config.traversalToCave().getGameName()), 1200);
                    return;
                }
            }

            if (config.enableHeart() && !Inventory.contains(config.heart().getGameName())) {
                if (Bank.withdraw(config.heart().getGameName(), 1)) {
                    Execution.delayUntil(() -> Inventory.contains(config.heart().getGameName()), 1200);
                    return;
                }
            }

            if (!Inventory.contains(FISHING_EXPLOSIVE) && Bank.withdraw(FISHING_EXPLOSIVE, 0)) {
                Execution.delayUntil(() -> Inventory.contains(FISHING_EXPLOSIVE), 800);
                return;
            }

            if (Inventory.getQuantity(config.prayerPotions().getGameName()) < config.prayerPotionQuantity()
                    && !config.prayerPotions().getGameName().equals(ItemPatterns.NONE)) {
                if (Bank.withdraw(
                        config.prayerPotions().getGameName(),
                        config.prayerPotionQuantity() - Inventory.getQuantity(config.prayerPotions().getGameName())
                )) {
                    log.debug("Withdrawing missing quantity of " + config.prayerPotions().getGameName() + ".");
                    Execution.delayUntil(() -> Inventory.getQuantity(config.prayerPotions().getGameName())
                            == config.prayerPotionQuantity(), 3000);
                    return;
                }
            }

            com.runemate.game.api.hybrid.EquipmentLoadout magicEquipmentMap = config.magicEquipment();
            if (magicEquipmentMap != null && !magicEquipmentMap.isEmpty()) {
                for (Pattern mageEquipment : magicEquipmentMap.values()) {
                    if (mageEquipment != null && (!Inventory.contains(mageEquipment) && !Equipment.contains(mageEquipment))) {
                        if (!Bank.containsAnyOf(mageEquipment)) {
                            stop("We are missing items from the ranged equipment...");
                            return;
                        }
                        log.debug("Withdrawing Magic equipment...");
                        if (Bank.withdraw(mageEquipment, 1)) {
                            Execution.delayUntil(() -> Inventory.contains(mageEquipment), 2000);
                            return;
                        }
                    }
                }
            }

            if (config.highAlch() && !Inventory.contains("Nature rune") && Bank.contains("Nature rune")) {
                if (Bank.withdraw("Nature rune", 0)) {
                    Execution.delayUntil(() -> Inventory.contains("Nature rune"), 1200);
                    return;
                }
            }

            if (config.highAlch() && !Inventory.contains("Fire rune") && Bank.contains("Fire rune")) {
                if (Bank.withdraw("Fire rune", 0)) {
                    Execution.delayUntil(() -> Inventory.contains("Fire rune"), 1200);
                    return;
                }
            }

            if (config.enableBloodSpells() && config.spell().equals(BloodSpells.BLOOD_BARRAGE)) {
                if (config.enableBloodSpells() && !Inventory.contains("Death rune") && Bank.contains("Death rune")) {
                    if (Bank.withdraw("Death rune", 0)) {
                        Execution.delayUntil(() -> Inventory.contains("Death rune"), 1200);
                        return;
                    }
                }

                if (config.enableBloodSpells() && !Inventory.contains("Blood rune") && Bank.contains("Blood rune")) {
                    if (Bank.withdraw("Blood rune", 0)) {
                        Execution.delayUntil(() -> Inventory.contains("Blood rune"), 1200);
                        return;
                    }
                }

                if (config.enableBloodSpells() && !Inventory.contains("Soul rune") && Bank.contains("Soul rune")) {
                    if (Bank.withdraw("Soul rune", 0)) {
                        Execution.delayUntil(() -> Inventory.contains("Soul rune"), 1200);
                        return;
                    }
                }
            }

            if (config.enableBloodSpells() && config.spell().equals(BloodSpells.BLOOD_BLITZ)) {
                if (config.enableBloodSpells() && !Inventory.contains("Death rune") && Bank.contains("Death rune")) {
                    if (Bank.withdraw("Death rune", 0)) {
                        Execution.delayUntil(() -> Inventory.contains("Death rune"), 1200);
                        return;
                    }
                }

                if (config.enableBloodSpells() && !Inventory.contains("Blood rune") && Bank.contains("Blood rune")) {
                    if (Bank.withdraw("Blood rune", 0)) {
                        Execution.delayUntil(() -> Inventory.contains("Blood rune"), 1200);
                        return;
                    }
                }
            }

            if (config.enableBloodSpells() && config.spell().equals(BloodSpells.BLOOD_BURST)) {
                if (config.enableBloodSpells() && !Inventory.contains("Death rune") && Bank.contains("Death rune")) {
                    if (Bank.withdraw("Death rune", 0)) {
                        Execution.delayUntil(() -> Inventory.contains("Death rune"), 1200);
                        return;
                    }
                }

                if (config.enableBloodSpells() && !Inventory.contains("Blood rune") && Bank.contains("Blood rune")) {
                    if (Bank.withdraw("Blood rune", 0)) {
                        Execution.delayUntil(() -> Inventory.contains("Blood rune"), 1200);
                        return;
                    }
                }

                if (config.enableBloodSpells() && !Inventory.contains("Chaos rune") && Bank.contains("Chaos rune")) {
                    if (Bank.withdraw("Chaos rune", 0)) {
                        Execution.delayUntil(() -> Inventory.contains("Chaos rune"), 1200);
                        return;
                    }
                }
            }

            if (Inventory.getQuantity(config.food().getGameName()) < config.foodQuantity()) {
                if (Bank.withdraw(
                        config.food().getGameName(),
                        config.foodQuantity() - Inventory.getQuantity(config.food().getGameName())
                )) {
                    log.debug("Withdrawing missing quantity of " + config.food().getGameName() + ".");
                    Execution.delayUntil(() -> Inventory.getQuantity(config.food().getGameName())
                            == config.foodQuantity(), 3000);
                    return;
                }
            }
        }
        Bank.close(true);
        if (config.traversalToCave().equals(TraversalToCave.FAIRY_RING)) {
            state = State.WALK_FAIRY;
        } else {
            state = State.WALK_CAVE;
        }
    }

    private void kill() { //Completed
        Player local = Players.getLocal();
        if (local == null) return;

        DefaultUI.setStatus("Attacking Kraken...");

        Npc kraken = Npcs.newQuery().names("Kraken").actions("Attack").results().first();
        if (kraken == null || !kraken.isValid()) {
            state = State.LOOT;
            return;
        }

        if (config.enableBloodSpells() && Health.getCurrentPercent() <= 60 && Magic.Book.ANCIENT.isCurrent() && PlayerUtil.canBloodSpell()) {
            log.debug("Using " + config.spell().getSpell().name() + "!");
            DirectInput.sendSpellCastOn(config.spell().getSpell(), kraken);
            final int hitpoints = Health.getCurrent();
            Execution.delayWhile(() -> hitpoints == Health.getCurrent(), 1600);
            return;
        }

        if (config.enableHeart() && Inventory.contains(config.heart().getGameName()) && state == State.KILL || state == State.THROW) {
            if (Skill.MAGIC.getBaseLevel() == Skill.MAGIC.getCurrentLevel()) {
                SpriteItem heart = Inventory.newQuery().names(config.heart().getGameName()).results().first();
                if (heart != null && heart.interact("Invigorate")) {
                    Execution.delayUntil(() -> Skill.MAGIC.getBaseLevel() != Skill.MAGIC.getCurrentLevel(), 1800);
                }
            }
        }

        if (kraken.isValid() && PlayerUtil.isPlayerInCombat(local) && kraken.interact("Attack")) {
            log.debug("Attacked Kraken...");
            Execution.delayWhile(() -> PlayerUtil.isPlayerInCombat(local), 1200);
        }
    }

    private void throwExplosives() { //Completed
        Player local = Players.getLocal();
        if (local == null) return;

        DefaultUI.setStatus("Throwing Explosives at Whirlpool...");

        Npc kraken = Npcs.newQuery().names("Kraken").actions("Attack").results().first();
        if (kraken != null) {
            state = State.KILL;
            return;
        }

        if (ChatDialog.isOpen()) {
            log.debug("Spacebar key pressed.");
            Keyboard.pressKey(KeyEvent.VK_SPACE);
            return;
        }

        if (!Inventory.contains(config.food().getGameName())) {
            log.debug("No food left, going to bank...");
            state = State.WALK_BANK;
            return;
        }

        if (!Inventory.contains(config.prayerPotions().getGameName())) {
            log.debug("No prayer pots left, going to bank...");
            state = State.WALK_BANK;
            return;
        }

        if (!Prayer.getSelectedQuickPrayers().contains(Prayer.AUGURY) && !Prayer.getSelectedQuickPrayers()
                .contains(Prayer.PROTECT_FROM_MAGIC)) {
            if (Prayer.setQuickPrayers(Prayer.AUGURY, Prayer.PROTECT_FROM_MAGIC)) {
                Execution.delayUntil(Prayer.PROTECT_FROM_MAGIC::isSelectedAsQuickPrayer, 2000);
                return;
            }
            if (Prayer.confirmQuickPrayerSelection()) {
                Execution.delayWhile(Prayer::isQuickPrayerSetupOpen, 1200);
                return;
            }
        }

        if (Inventory.containsAnyOf(ALCHABLES) && config.highAlch() && Magic.Book.STANDARD.isCurrent()) {
            state = State.ALCH;
            return;
        }

        if (!Inventory.contains(FISHING_EXPLOSIVE)) {
            state = State.WALK_BANK;
            return;
        }

        if (AutoRetaliate.isActivated()) {
            if (AutoRetaliate.deactivate()) {
                Execution.delayWhile(AutoRetaliate::isActivated, 1400);
                return;
            }
        }

        Npc whirlpool = Npcs.newQuery().ids(496).results().first();
        if (whirlpool == null || !whirlpool.isValid()) {
            return;
        }

        if (whirlpool.isValid() && PlayerUtil.isPlayerAttacking(local, whirlpool)) {
            SpriteItem explosive = Inventory.newQuery().names(FISHING_EXPLOSIVE).results().first();
            if (explosive != null) {
                DefaultUI.setStatus("Throwing fishing explosives...");
                DirectInput.sendItemUseOn(explosive, whirlpool);
                Execution.delayUntil(() -> !whirlpool.isValid(), 12000);
            }
        }
    }

    private void walkBank() { //Completed
        DefaultUI.setStatus("Walking towards the bank...");

        Player local = Players.getLocal();
        if (Bank.isOpen()) {
            state = State.BANK;
            return;
        }

        if (Camera.getZoom() > 0) {
            Camera.setZoom(0, 0);
        }

        GameObject bank = GameObjects.newQuery().names(Constants.BANK_NAMES).results().nearest();
        if (Distance.between(local, bank) <= 15 && bank != null && local != null) {
            Camera.concurrentlyTurnTo(bank);
            if (bank.interact(Constants.BANK_ACTIONS)) {
                Execution.delayWhile(() -> !Bank.isOpen() || local.isMoving(), 8400);
                return;
            }
        }

        ChatDialog.Option option = ChatDialog.getOption(1);
        if (config.traversalToBank().equals(TraversalToBank.GLORY) && option != null) {
            log.info("ChatDialog is open");
            if (option.select(true)) {
                Execution.delayWhile(Animation::playerIsAnimating, 3600);
                return;
            }
        }

        if (config.traversalToBank().equals(TraversalToBank.GLORY) && Inventory.contains(ItemPatterns.GLORY)) {
            SpriteItem gloryInv = Inventory.newQuery().names(ItemPatterns.GLORY).results().first();
            if (gloryInv != null && gloryInv.interact("Rub")) {
                Execution.delayUntil(ChatDialog::isOpen, 1200);
                return;
            }
        }

        WebPath path = CachePathing.getPathLandmark(Landmark.BANK, false);
        if (path != null) {
            path.step(true);
        }
    }

    Coordinate creviceEntraceCoordinate = new Coordinate(2280, 10017, 0);

    private void walkCave() {
        Player local = Players.getLocal();
        if (local == null) return;

        Coordinate dungeonEntrance1 = new Coordinate(2278, 3610, 0);
        List<GameObject> gameObjects = GameObjects.getLoaded().asList();

        DefaultUI.setStatus("Walking to Crevice...");

        if (config.traversalToCave().equals(TraversalToCave.PISCATORIS_TELEPORT_SCROLL)) {
            if (Inventory.contains(config.traversalToCave().getGameName())) {
                SpriteItem piscTeleport = Inventory.newQuery().names(config.traversalToCave().getGameName()).results().first();
                if (piscTeleport != null && piscTeleport.interact("Teleport")) {
                    Execution.delayWhile(Animation::playerIsAnimating, 2400);
                    return;
                }
            }
        }

        if (Equipment.contains(ItemPatterns.DRAMENSTAFF)) {
            mageSwitch();
            return;
        }

        if (ChatDialog.isOpen()) {
            ChatDialog.Option option = ChatDialog.getOption(Pattern.compile("Yes, pay 25,000 x Coins."));
            if (option != null && option.select(true)) {
                state = State.THROW;
                return;
            }
        }
        Npc whirlPool = Npcs.newQuery().names("Whirlpool").results().first();
        if (whirlPool != null && whirlPool.isValid()) {
            GameObject crevice = GameObjects.newQuery().provider(() -> gameObjects).names("Crevice").actions("Enter").results().first();
            if (crevice == null) {
                return;
            }
            DirectInput.send(MenuAction.forGameObject(crevice, "Private"));
            Execution.delayUntil(() -> ChatDialog.isOpen() && local.isMoving(), 4200);
            return;
        }

        GameObject dungeon = GameObjects.newQuery().provider(() -> gameObjects).names("Cave Entrance").results().nearestTo(dungeonEntrance1);
        if (dungeon != null && !local.isMoving()) {
            DirectInput.send(MenuAction.forGameObject(dungeon, "Enter"));
            Npc whirlPools = Npcs.newQuery().names("Whirlpool").results().first();
            if (whirlPools == null) return;
            Execution.delayUntil(whirlPools::isValid, 1200);
        }
    }

    private void walkFairyRing() {
        Player local = Players.getLocal();
        if (local == null) return;
        DefaultUI.setStatus("Walking to fairy rings...");

        Npc kebbit = Npcs.newQuery().names("Prickly kebbit").results().nearest();
        if (kebbit != null) {
            state = State.WALK_CAVE;
            return;
        }

        if (FairyRing.isConfigurationOpen()) {
            if (!FairyRing.Dial.LEFT.isSelected('A') && FairyRing.Dial.LEFT.select('A')) {
                return;
            }
            if (!FairyRing.Dial.MIDDLE.isSelected('K') && FairyRing.Dial.MIDDLE.select('K')) {
                return;
            }
            if (!FairyRing.Dial.RIGHT.isSelected('Q') && FairyRing.Dial.RIGHT.select('Q')) {
                return;
            }
            if (FairyRing.Dial.LEFT.isSelected('A') && FairyRing.Dial.MIDDLE.isSelected('K') && FairyRing.Dial.RIGHT.isSelected('Q') && FairyRing.confirm()) {
                return;
            }
            return;
        }

        if (Inventory.contains(ItemPatterns.DRAMENSTAFF)) {
            SpriteItem dramenStaff = Inventory.newQuery().names(ItemPatterns.DRAMENSTAFF).results().first();
            if (dramenStaff == null) return;

            if (dramenStaff.interact("Wield")) {
                Execution.delayUntil(() -> Equipment.contains(ItemPatterns.DRAMENSTAFF), 1200);
                return;
            }
        }

        WebPath path = CachePathing.getPathToNearestFairyRing(local, false);
        GameObject fairyRing = GameObjects.newQuery().names("Fairy ring").results().first();
        if (fairyRing != null) {
            var definition = fairyRing.getDefinition();
            if (definition == null) return;
            var fairyState = definition.getLocalState();
            if (fairyState == null) return;
            var actions = fairyState.getActions();
            var lastAction = "Last-destination (AKQ)";
            if (actions.contains(lastAction) && !local.isMoving()) {
                DirectInput.send(MenuAction.forGameObject(fairyRing, lastAction));
                Execution.delayUntil(() -> !fairyRing.isValid(), 12000);
                return;
            } else {
                DirectInput.send(MenuAction.forGameObject(fairyRing, "Configure"));
                Execution.delayUntil(FairyRing::isConfigurationOpen, 12000);
                return;
            }
        }
        path.step(true);
    }

    private static final ArrayList<String> alchables = new ArrayList<>();

    static {
        alchables.add("Mystic water staff");
        alchables.add("Rune warhammer");
        alchables.add("Rune longsword");
        alchables.add("Mystic robe top");
        alchables.add("Mystic robe bottom");
        alchables.add("Rune spear");
        alchables.add("Dragon spear");
    }

    private void alch() {
        DefaultUI.setStatus("High Alching items...");

        String[] alchablesArray = alchables.toArray(new String[0]);

        com.runemate.game.api.hybrid.entities.Player local = Players.getLocal();
        if (Inventory.containsAnyOf(alchablesArray) && Magic.Book.STANDARD.isCurrent()) {
            SpriteItem alchableItem = Inventory.newQuery().names(alchablesArray).results().first();
            if (local != null && alchableItem != null) {
                log.debug("Attempting to high alch...");
                DirectInput.sendSpellCastOn(Magic.HIGH_LEVEL_ALCHEMY, alchableItem);
                Execution.delayUntil(() -> local.getAnimationId() != -1, 1800);
                return;
            }
        }
        state = State.THROW;
    }

    private void end() {
        if (ChatDialog.isOpen()) {
            ChatDialog.Option option = ChatDialog.getOption(Pattern.compile("(Edgeville|Castle Wars Arena)"));
            if (option != null && option.select(true)) {
                Execution.delayUntil(Animation::playerIsAnimating, 1200);
                return;
            }
        }

        if (Inventory.contains(config.traversalToBank().getGameName())) {
            SpriteItem bankTraversal = Inventory.newQuery().names(config.traversalToBank().getGameName()).results().first();
            if (bankTraversal != null) {
                DirectInput.send(MenuAction.forSpriteItem(bankTraversal, Pattern.compile("(Rub|Break)")));
                return;
            }
        }

        if (RuneScape.isLoggedIn() && RuneScape.logout()) {
            DefaultUI.setStatus("Stopping bot, we've completed our task.");
            stop("Stopping bot, we've completed our task.");
        }
    }

    @Override
    public void loop() {
        if (config == null) return;
        if (breakConfig == null) return;
        if (!started) return;
        final Player local = Players.getLocal();
        if (local == null) return;

        if (state == null) { //TODO
            if (Region.isInstanced()) {
                state = State.THROW;
                return;
            } else {
                log.debug("Lets path to the nearest bank...");
                state = State.WALK_BANK;
            }
        }

        int prayerPoints = Prayer.getPoints();
        if (prayerPoints <= 25) {
            SpriteItem prayerPot = Inventory.newQuery().names(config.prayerPotions().getInteractName()).results().sortByIndex().first();
            if (prayerPot != null && prayerPot.interact("Drink")) {
                Execution.delayUntil(() -> prayerPoints >= 25, 1200);
            }
        }

        int playerHealth = Health.getCurrentPercent();
        if (playerHealth <= 55) {
            SpriteItem edibleFood = Inventory.newQuery().actions("Eat").results().first();
            if (edibleFood != null && edibleFood.interact("Eat")) {
                Execution.delayWhile(edibleFood::isValid, 800);
            }
        }

        switch (state) {
            case LOOT -> loot();
            case BANK -> bank();
            case END -> end();
            case KILL -> kill();
            case THROW -> throwExplosives();
            case WALK_BANK -> walkBank();
            case WALK_CAVE -> walkCave();
            case WALK_FAIRY -> walkFairyRing();
            case ALCH -> alch();
        }
    }

    @Override
    public void onSettingChanged(final SettingChangedEvent settingChangedEvent) {

    }

    @Override
    public void onSettingsConfirmed() {
        started = true;
    }

    @Override
    public void start() {
        ClientUI.showAlert(
                ClientUI.AlertLevel.INFO,
                "You can join KOR Discord to find out more information - <a href=\"https://discord.gg/5NGdx3BSsb\">Click here</a> "
        );
        //DefaultUI.setItemEventListening(this, false);
        getEventDispatcher().addListener(this);
    }


    public void onEngineEvent(EngineEvent event) {
        if (event.getType() == EngineEvent.Type.SERVER_TICK) {
            if (isPaused()) return;

            InterfaceComponent quickPrayerComponent = PrayerHandler.getQuickPrayerToggle();
            MenuAction action = MenuAction.forInterfaceComponent(quickPrayerComponent, 0);

            if (Environment.isDirectInputEnabled() && config.prayFlicker()) {
                if (state == State.KILL) {
                    if (Prayer.isQuickPraying()) {
                        DirectInput.send(action);
                    }
                    DirectInput.send(action);
                }
            } else if (!config.prayFlicker() && state == State.KILL) {
                if (action != null && !Prayer.isQuickPraying()) {
                    DirectInput.send(action);
                }
            }
        }
    }

    @Override
    public void onMessageReceived(final MessageEvent event) {
        if (isPaused()) return;

        if (event.getMessage().contains(DiscordNotifier.PET_MESSAGE)) {
            DiscordNotifier.sendWebhookNotification("Pet Kraken");
        }
        if (event.getMessage().contains("You have completed your task!")) {
            state = State.END;
        }
        if (event.getMessage().contains("received a drop")) {
            state = State.LOOT;
        }
    }

    private void mageSwitch() {
        if (config.magicEquipment() != null) {
            for (Pattern mageSwitch : config.magicEquipment().values()) {
                if (Inventory.contains(mageSwitch) && !Equipment.contains(mageSwitch)) {
                    SpriteItem itemEquipmentBank = Inventory.newQuery().names(mageSwitch).results().first();
                    if (itemEquipmentBank != null) {
                        DirectInput.send(MenuAction.forSpriteItem(itemEquipmentBank, Pattern.compile("(Wear|Wield|Equip)")));
                        Execution.delayUntil(() -> Equipment.contains(mageSwitch), 2000);
                    }
                }
            }
        }
    }

    @Override
    public void onInventoryUpdated(ItemEvent event) {
        if (event.getItem().getDefinition() != null) {
            String itemName = event.getItem().getDefinition().getName();
            if (DiscordNotifier.RARE_ITEM.matcher(itemName).matches()) {
                DiscordNotifier.sendWebhookNotification(itemName);
            }
        }
    }

    @Override
    public void onNpcDeath(DeathEvent event) {
        Player local = Players.getLocal();
        if (local == null) return;
        if (event.getSource().equals(local.getTarget())) {
            log.info("Kraken death");
            state = State.LOOT;
        }
    }

    @Override
    public BreakConfig breakConfig() {
        return breakConfig;
    }

    @Override
    public Break.Behavior onBreakRequested(final Break requested) {
        if (Region.isInstanced()) {
            return Break.Behavior.DEFER;
        }
        return Breaking.defaultBehavior(requested);
    }

    @Override
    public Stop.Behavior onStopRequested(final Stop requested) {
        if (Region.isInstanced()) {
            return Stop.Behavior.DEFER;
        }
        return Stop.Behavior.STOP;
    }

    private enum State {
        WALK_CAVE,
        WALK_BANK,
        WALK_FAIRY,
        THROW,
        BANK,
        LOOT,
        KILL,
        ALCH,
        END
    }
}
