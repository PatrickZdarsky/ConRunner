package at.control.shiftcontrol.shift.service;

import java.time.Instant;
import java.util.Collection;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.platform.exception.ConflictException;
import at.control.shiftcontrol.platform.exception.NotFoundException;
import at.control.shiftcontrol.shift.lib.type.PositionSignupState;

import lombok.NonNull;
import org.jspecify.annotations.Nullable;

public interface EligibilityService {
    /**
     * Determines the signup state of a volunteer for a given position slot.
     *
     * <p>
     * <b>This does not check if the given user has privileges for sign-up</b>
     *
     * @param positionSlot The position slot to check
     * @param volunteer    The volunteer to check
     * @return The signup state of the volunteer for the position slot
     */
    @NonNull PositionSignupState getSignupStateForPositionSlot(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer);

    /**
     * Determines the signup state of a volunteer for a given position slot.
     * Excludes the other slot when checking for conflicts (used for trade)
     *
     * <p>
     * <b>This does not check if the given user has privileges for sign-up</b>
     *
     * @param positionSlot The position slot to check
     * @param volunteer    The volunteer to check
     * @param slotToExclude The position slot to exclude when checking for conflicts
     * @return The signup state of the volunteer for the position slot
     */
    @NonNull PositionSignupState getSignupStateForPositionSlotExcludingSlot(@NonNull PositionSlot positionSlot,
                                                                            @NonNull Volunteer volunteer, @Nullable PositionSlot slotToExclude);

    /**
     * Determines the signup state of a volunteer for a given position slot.
     *
     * <p>
     * <b>This does not check if the given user has privileges for sign-up</b>
     *
     * @param positionSlotId The position slot to check
     * @param userId         The volunteer to check
     * @return The signup state of the volunteer for the position slot
     * @throws NotFoundException if the position slot or volunteer could not be found
     */
    @NonNull PositionSignupState getSignupStateForPositionSlot(@NonNull Long positionSlotId, @NonNull String userId);

    /**
     * Determines the signup state of a volunteer for a given position slot.
     *
     * <p>
     * <b>This does not check if the given user has privileges for sign-up</b>
     *
     * @param positionSlot The position slot to check
     * @param userId       The volunteer to check
     * @return The signup state of the volunteer for the position slot
     */
    @NonNull PositionSignupState getSignupStateForPositionSlot(@NonNull PositionSlot positionSlot, @NonNull String userId);

    /**
     * Checks if the volunteer can join the position slot based on PositionSignUpState.
     *
     * @param positionSlot The position slot to check
     * @param volunteer    The volunteer to check
     * @throws ConflictException if it is not possible for the volunteer to sign up for this position slot
     */
    void validateSignUpStateForJoin(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer);

    /**
     * Checks if the volunteer can claim an auction for the position slot based on PositionSignUpState.
     *
     * @param positionSlot The requested position slot to check
     * @param volunteer    The volunteer to check
     * @throws ConflictException if it is not possible for the volunteer to claim an auction for this position slot
     */
    void validateSignUpStateForAuction(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer);

    /**
     * Checks if the volunteer is eligible and not signed up to the given position slot.
     *
     * @param positionSlot to check for
     * @param volunteer    to assign
     * @return true if the user is not signed up and eligible
     */
    boolean isEligibleAndNotSignedUp(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer);

    /**
     * Checks if the volunteer is eligible and not signed up to the given position slot.
     * Excludes the other slot when checking for conflicts (used for trade)
     *
     * @param positionSlot to check for
     * @param volunteer    to assign
     * @param slotToExclude The position slot to exclude when checking for conflicts
     * @return true if the user is not signed up and eligible
     */
    boolean isEligibleAndNotSignedUpExcludingSlot(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer, @NonNull PositionSlot slotToExclude);

    /**
     * Checks if the volunteer is eligible and not signed up to the given position slot.
     *
     * @param positionSlot to check for
     * @param volunteer    to assign
     * @throws ConflictException if the user is not assignable to the slot
     */
    void validateIsEligibleAndNotSignedUp(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer);

    /**
     * Checks if the volunteer is eligible and not signed up to the given position slot.
     * Excludes the other slot when checking for conflicts (used for trade)
     *
     * @param positionSlot to check for
     * @param volunteer    to assign
     * @param slotToExclude The position slot to exclude when checking for conflicts
     * @throws ConflictException if the user is not assignable to the slot
     */
    void validateIsEligibleAndNotSignedUpExcludingSlot(@NonNull PositionSlot positionSlot, @NonNull Volunteer volunteer, @NonNull PositionSlot slotToExclude);

    /**
     * returns all conflicting assignments within a given time for a specific user.
     *
     * @param volunteerId The volunteer to check
     * @param startTime   start of the timespan to check
     * @param endTime     end of the timespan to check
     * @return the overlapping assignments
     */
    @NonNull Collection<Assignment> getConflictingAssignments(@NonNull String volunteerId, @NonNull Instant startTime, @NonNull Instant endTime);

    /**
     * returns all conflicting assignments within a given time for a specific user.
     *
     * @param volunteerId  The volunteer to check
     * @param positionSlot slot of which the time is to check
     * @return the overlapping assignments
     */
    @NonNull Collection<Assignment> getConflictingAssignments(@NonNull String volunteerId, @NonNull PositionSlot positionSlot);

    /**
     * returns all conflicting assignments within a given time for a specific user, ignoring the provided position slot.
     *
     * @param volunteerId      The volunteer to check
     * @param startTime        start of the timespan to check
     * @param endTime          end of the timespan to check
     * @param shiftIdToExclude shift that is ignored for the check
     * @return the overlapping assignments
     */
    @NonNull Collection<Assignment> getConflictingAssignmentsExcludingShift(@NonNull String volunteerId, @NonNull Instant startTime, @NonNull Instant endTime, long shiftIdToExclude);

    /**
     * returns all conflicting assignments with a given position slot for a specific user, ignoring  another position slot.
     *
     * @param volunteerId      The volunteer to check
     * @param positionSlot     positionSlot to check
     * @param shiftIdToExclude shift that is ignored for the check
     * @return the overlapping assignments
     */
    @NonNull Collection<Assignment> getConflictingAssignmentsExcludingShift(@NonNull String volunteerId, @NonNull PositionSlot positionSlot, long shiftIdToExclude);

    /**
     * checks if the user has any conflicting assignments within a given time, ignoring the given position slot.
     *
     * @param volunteerId  The volunteer to check
     * @param startTime    start of the timespan to check
     * @param endTime      end of the timespan to check
     * @param positionSlot position slot that is ignored for the check
     * @throws ConflictException if overlapping assignments exist
     */
    void validateHasConflictingAssignmentsExcludingShift(@NonNull String volunteerId, @NonNull Instant startTime, @NonNull Instant endTime, long positionSlot);

    /**
     * checks if the user has any conflicting assignments with a given position slot, ignoring another position slot.
     *
     * @param volunteerId    The volunteer to check
     * @param positionSlot   positionSlot to check
     * @param shiftToExclude shift that is ignored for the check
     * @throws ConflictException if overlapping assignments exist
     */
    void validateHasConflictingAssignmentsExcludingShift(@NonNull String volunteerId, @NonNull PositionSlot positionSlot, long shiftToExclude);

    /**
     * checks if a position slot is full.
     * ignores signup-requests
     *
     * @param positionSlot to check
     * @return true if slot has capacity left
     */
    boolean hasCapacity(@NonNull PositionSlot positionSlot);
}
