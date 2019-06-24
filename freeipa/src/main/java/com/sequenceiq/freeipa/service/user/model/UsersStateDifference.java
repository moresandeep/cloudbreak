package com.sequenceiq.freeipa.service.user.model;

import static java.util.Objects.requireNonNull;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.User;

public class UsersStateDifference {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsersStateDifference.class);

    private Set<Group> groupsToAdd;

    private Set<User> usersToAdd;

    private Set<Group> groupsToRemove;

    private Set<User> usersToRemove;

    private Multimap<String, String> groupMembershipToAdd;

    private Multimap<String, String> groupMembershipToRemove;

    public UsersStateDifference(Set<Group> groupsToAdd, Set<User> usersToAdd, Set<Group> groupsToRemove, Set<User> usersToRemove,
            Multimap<String, String> groupMembershipToAdd, Multimap<String, String> groupMembershipToRemove) {
        this.groupsToAdd = requireNonNull(groupsToAdd);
        this.usersToAdd = requireNonNull(usersToAdd);
        this.groupsToRemove = requireNonNull(groupsToRemove);
        this.usersToRemove = requireNonNull(usersToRemove);
        this.groupMembershipToAdd = requireNonNull(groupMembershipToAdd);
        this.groupMembershipToRemove = requireNonNull(groupMembershipToRemove);
    }

    public Set<Group> getGroupsToAdd() {
        return groupsToAdd;
    }

    public Set<User> getUsersToAdd() {
        return usersToAdd;
    }

    public Set<Group> getGroupsToRemove() {
        return groupsToRemove;
    }

    public Set<User> getUsersToRemove() {
        return usersToRemove;
    }

    public Multimap<String, String> getGroupMembershipToAdd() {
        return groupMembershipToAdd;
    }

    public Multimap<String, String> getGroupMembershipToRemove() {
        return groupMembershipToRemove;
    }

    @Override
    public String toString() {
        return "UsersStateDifference{"
                + "groupsToAdd=" + groupsToAdd
                + ", usersToAdd=" + usersToAdd
                + ", groupsToRemove=" + groupsToRemove
                + ", usersToRemove=" + usersToRemove
                + ", groupMembershipToAdd=" + groupMembershipToAdd
                + ", groupMembershipToRemove=" + groupMembershipToRemove
                + '}';
    }

    public static UsersStateDifference fromUmsAndIpaUsersStates(UsersState umsState, UsersState ipaState) {
        Multimap<String, String> umsGroupMembership = umsState.getGroupMembership();
        Multimap<String, String> ipaGroupMembership = ipaState.getGroupMembership();

        Multimap<String, String> groupMembershipToAdd = HashMultimap.create();
        umsGroupMembership.forEach((group, user) -> {
            LOGGER.info("Evaluation group = {} and user = {}", group, user);
            if (!ipaGroupMembership.containsEntry(group, user)) {
                LOGGER.info("adding");
                groupMembershipToAdd.put(group, user);
            }
        });

        Multimap<String, String> groupMembershipToRemove = HashMultimap.create();
        ipaGroupMembership.forEach((group, user) -> {
            LOGGER.info("Evaluation group = {} and user = {}", group, user);
            if (!umsGroupMembership.containsEntry(group, user)) {
                LOGGER.info("removing");
                groupMembershipToRemove.put(group, user);
            }
        });

        return new UsersStateDifference(
                Set.copyOf(Sets.difference(umsState.getGroups(), ipaState.getGroups())),
                Set.copyOf(Sets.difference(umsState.getUsers(), ipaState.getUsers())),
                Set.copyOf(Sets.difference(ipaState.getGroups(), umsState.getGroups())),
                Set.copyOf(Sets.difference(ipaState.getUsers(), umsState.getUsers())),
                groupMembershipToAdd,
                groupMembershipToRemove);
    }
}
