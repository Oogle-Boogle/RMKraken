package kor.bots.kraken.enums;

import com.runemate.game.api.hybrid.local.*;
import com.runemate.game.api.osrs.local.hud.interfaces.*;
import lombok.*;

@Getter
@RequiredArgsConstructor
public enum BloodSpells {

    BLOOD_BURST(Magic.Ancient.BLOOD_BURST),
    BLOOD_BLITZ(Magic.Ancient.BLOOD_BLITZ),
    BLOOD_BARRAGE(Magic.Ancient.BLOOD_BARRAGE);

    private final Spell spell;

}
