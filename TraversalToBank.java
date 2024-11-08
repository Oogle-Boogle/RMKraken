package kor.bots.kraken.enums;

import java.util.regex.*;
import lombok.*;
import kor.framework.*;

@Getter
@RequiredArgsConstructor
public enum TraversalToBank {

    //HOUSE_TAB(items.houseTabPattern),
    GLORY(ItemPatterns.GLORY);
    //RING_OF_DUELING(ItemPatterns.RING_OF_DUELING);

    private final Pattern gameName;

}
