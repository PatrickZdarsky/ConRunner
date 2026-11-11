# Check bootstrap administrator role synchronization

`ApplicationUserSyncServiceImpl.constructUserForLogin` combines profile synchronization and bootstrap
role assignment with a short-circuiting `||`. When profile claims change, the role-grant call is skipped
for that login. Review whether bootstrap administrators must receive their role on the same login,
and add a regression test and fix if appropriate. This is separate from the audit coverage work.
