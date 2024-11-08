package kor.bots.kraken.enums;

import lombok.*;

@Getter
@RequiredArgsConstructor
public enum KrakenHeart {
    IMBUED_HEART("Imbued heart"),
    SATURATED_HEART("Saturated heart");

    private final String gameName;
}
