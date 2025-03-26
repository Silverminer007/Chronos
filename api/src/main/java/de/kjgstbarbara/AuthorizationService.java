package de.kjgstbarbara;

import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Group;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;

public class AuthorizationService {
    public static boolean hasAdminRights(Date date, Person principal) {
        return date.getGroup().getAdmins().contains(principal) || hasAdminRights(date.getGroup().getOrganisation(), principal);
    }

    public static boolean canSee(Date date, Person principal) {
        return canSee(date.getGroup(), principal);
    }

    public static boolean canSee(Organisation organisation, Person principal) {
        return organisation.getMembers().contains(principal);
    }

    public static boolean hasAdminRights(Organisation organisation, Person principal) {
        return organisation.getAdmin().equals(principal);
    }

    public static boolean canSee(Group group, Person principal) {
        return group.getMembers().contains(principal) || hasAdminRights(group.getOrganisation(), principal);
    }

    public static boolean hasAdminRights(Group group, Person principal) {
        return group.getAdmins().contains(principal);
    }
}
