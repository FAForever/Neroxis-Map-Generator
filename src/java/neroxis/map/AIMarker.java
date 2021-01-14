package neroxis.map;

import lombok.Data;
import lombok.EqualsAndHashCode;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

import java.util.Collection;
import java.util.LinkedHashSet;

@EqualsAndHashCode(callSuper = true)
@Data
public strictfp class AIMarker extends Marker {
    private LinkedHashSet<String> neighbors;

    public AIMarker(String id, Vector2f position, LinkedHashSet<String> neighbors) {
        this(id, new Vector3f(position), neighbors);
    }

    public AIMarker(String id, Vector3f position, LinkedHashSet<String> neighbors) {
        super(id, position);
        this.neighbors = neighbors;
    }

    public int getNeighborCount() {
        return neighbors.size();
    }

    public void addNeighbor(String id) {
        neighbors.add(id);
    }

    public void addNeighbors(Collection<? extends String> ids) {
        neighbors.addAll(ids);
    }

}

