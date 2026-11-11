# Test coverage to revisit

The following cases were removed during the migration from the old backend because they depended on a shared, pre-seeded database. Reintroduce useful cases with data created by each test, including matching and nonmatching records. Confirm that each behavior still applies to the current implementation before adding coverage. The existing `*IT` tests are not run by the default Maven test pattern.

## Assignment repository

- [ ] Find signup requests for a shift plan, filtering by assignment status and excluding other plans. (`testFindSignupRequestsByShiftPlanId`)
- [ ] Find active auctions for a shift plan and exclude an identified volunteer when requested. (`testFindAuctionsByShiftPlanId`, `findAuctionsByShiftPlanIdExcludingUser`)
- [ ] Find assignments that conflict with a shift's time interval, including the former one-second overlap boundary. (`testGetConflictingAssignments`)
- [ ] Find assignments for a position slot and one volunteer or a collection of volunteers. (`testFindAssignmentForPositionSlotAndUser`, `testFindAssignmentForPositionSlotAndUsers`)
- [ ] Verify what happens to related trade requests when an assignment is deleted, according to the current persistence rules. (`testDeleteCascadeToTrades`)

## Trade repository and service

- [ ] Cancel trade requests involving an assignment, and cancel requests for an offered position and requested volunteer. Verify affected and unaffected requests. (`testCancelTradesForAssignment`, `testCancelTradesForOfferedPositionAndRequestedUser`)
- [ ] Find open trades by requested position and offering volunteer, and find trades for a plan and volunteer. (`testFindOpenTradesForRequestedPositionAndOfferingUser`, `testFindTradesForShiftPlanAndUser`)
- [ ] Find positions a volunteer may offer in a trade. (`testGetPositionSlotsToOffer`)
- [ ] Create a trade and verify its status and offered/requested assignments. (`testCreateTrade`)
- [ ] Accept a trade and verify assignment ownership, removal of the original assignments, and reward-point effects. (`testAcceptTrade`)
- [ ] Decline or cancel a trade and verify its resulting status and assignments. (`testDeclineTrade`, `testCancelTrade`)

## Events and volunteers

- [ ] Find planners for an event and volunteer, including plan membership and unrelated planners. (`testGetPlannersForEventAndUser`)
- [ ] Find open events available to a volunteer at a specified time, including closed or unrelated events as exclusions. (`testGetAllOpenEventsForUser`)
- [ ] Find volunteers by shift plan, both with and without a requested volunteer-ID filter. (`testFindAllByShiftPlan`, `testFindAllByShiftPlanAndVolunteerIds`)

## Position-slot service

- [ ] Cancel an auction and check the restored assignment status and volunteer. (`testCancelAuction`)
- [ ] Create a join request and verify its status and assignee. (`testJoinRequest`)
- [ ] Request to leave and verify the expected assignment status. (`testLeaveRequest`)
- [ ] Review service-level coverage for creating and claiming auctions and joining and leaving slots. (`testCreateAuction`, `testClaimAuction`, `testJoin`, `testLeave`) The retained `PositionSlotIT` covers these flows at the HTTP level, including repeated join/leave; add service-level cases only where they cover distinct behavior.

## Trust alerts

- [ ] Check trust-alert pagination with more records than one page. The old `testGetAllByEventId` name was misleading: it called the unfiltered `getAllPaginated` method.
- [ ] Check persistence of an alert linked to a volunteer. `TrustAlertIT` retains HTTP-level create, list, and delete coverage.

## Removed nonempty-table checks

The old `findAll()` smoke tests only asserted that shared seed data existed. They covered activities, assignments, trade requests, events, locations, position constraints, slot preferences, slots, roles, plans, shifts, time constraints, trust alerts, and volunteers. Do not recreate those checks verbatim; add targeted persistence or query tests when the behavior warrants it. The independent activity-save test remains.
