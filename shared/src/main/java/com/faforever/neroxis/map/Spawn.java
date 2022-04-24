package com.faforever.neroxis.map;

import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public strictfp class Spawn extends Marker {

    private Vector2 noRushOffset;
    private int teamID;

    public Spawn(String id, Vector2 position, Vector2 noRushOffset, int teamID) {
        this(id, new Vector3(position), noRushOffset, teamID);
    }

    public Spawn(String id, Vector3 position, Vector2 noRushOffset, int teamID) {
        super(id, position);
        this.noRushOffset = noRushOffset;
        this.teamID = teamID;
    }
}
