package au.org.ala.fielddata.mobile.model;

import java.util.List;

/**
 * Allows species to be categorised.
 */
public class SpeciesGroup extends Persistent {

    public String name;
    public int parentGroup;
    public List<SpeciesGroup> subgroups;

    public Integer getId() {
        return server_id;
    }


}
