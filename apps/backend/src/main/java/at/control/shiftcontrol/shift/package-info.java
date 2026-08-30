/**
 * The shift application module; owns volunteer profiles, plan membership/access, shift plans,
 * shifts, roles, positions, eligibility, assignments, trades, and rewards.
 *
 * <p>Volunteer records reference identity's stable user ID. Dispatch may use that ID through
 * explicit APIs, but live responder state remains dispatch-owned.</p>
 *
 * <p>Types intentionally exposed to other modules belong in this root package. Implementation
 * details remain in its subpackages so Spring Modulith can enforce the boundary.</p>
 */
package at.control.shiftcontrol.shift;
