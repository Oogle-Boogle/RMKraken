package kor.bots.kraken.enums;

import java.util.regex.*;
import lombok.*;
import kor.framework.*;

@Getter
@RequiredArgsConstructor
public enum TraversalToCave {

    FAIRY_RING(ItemPatterns.DRAMENSTAFF),
    PISCATORIS_TELEPORT_SCROLL(Pattern.compile("Piscatoris teleport"));

    private final Pattern gameName;

}
