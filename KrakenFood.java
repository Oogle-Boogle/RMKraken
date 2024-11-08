package kor.bots.kraken.enums;

import lombok.*;
@Getter
@RequiredArgsConstructor
public enum KrakenFood {
    COOKED_KARAMBWAN("Cooked karambwan"),
    SHARK("Shark"),
    MANTA_RAY("Manta ray"),
    ANGLERFISH("Anglerfish"),
    SWORDFISH("Swordfish"),
    PINEAPPLE_PIZZA("Pineapple pizza");

    private final String gameName;
}
