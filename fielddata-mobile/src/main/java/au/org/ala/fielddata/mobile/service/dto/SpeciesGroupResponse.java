package au.org.ala.fielddata.mobile.service.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import au.org.ala.fielddata.mobile.model.SpeciesGroup;

/**
 * Overrides SpeciesGroup to match the id to the value returned via the service.
 */
public class SpeciesGroupResponse  {

    @SerializedName("id")
    public int groupId;

    public String name;

    public List<SpeciesGroupResponse> subgroups;

    public Integer getId() {
        return groupId;
    }

    public SpeciesGroup toSpeciesGroup() {

        SpeciesGroup mappedGroup = mapSingle(this, null);
        map(this.subgroups, mappedGroup);

        return mappedGroup;
    }


    private void map(List<SpeciesGroupResponse> groups, SpeciesGroup parent) {

        List<SpeciesGroup> mappedGroups = new ArrayList<SpeciesGroup>(groups.size());
        for (SpeciesGroupResponse group : groups) {

            SpeciesGroup mappedGroup = mapSingle(group, parent);

            map(group.subgroups, mappedGroup);
            mappedGroups.add(mappedGroup);

        }
        if (parent != null) {
            parent.subgroups = mappedGroups;
        }
    }

    private SpeciesGroup mapSingle(SpeciesGroupResponse group, SpeciesGroup parent) {
        SpeciesGroup mappedGroup = new SpeciesGroup();
        mappedGroup.server_id = group.getId();
        mappedGroup.name = group.name;
        mappedGroup.parentGroup = parent != null ? parent.getId() : -1;
        return mappedGroup;
    }
}
