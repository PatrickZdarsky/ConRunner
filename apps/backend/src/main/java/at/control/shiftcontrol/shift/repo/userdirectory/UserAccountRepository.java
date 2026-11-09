package at.control.shiftcontrol.shift.repo.userdirectory;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import at.shiftcontrol.lib.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    @Query(value = """
        select userAccount.id
        from UserAccount userAccount
        where (
            :pattern is null
            or lower(coalesce(userAccount.preferredUsername, '')) like :pattern
            or lower(coalesce(userAccount.firstName, '')) like :pattern
            or lower(coalesce(userAccount.lastName, '')) like :pattern
            or lower(coalesce(userAccount.displayName, '')) like :pattern
            or lower(coalesce(userAccount.email, '')) like :pattern
            or exists (
                select 1
                from ExternalIdentity externalIdentity
                where externalIdentity.userAccount = userAccount
                    and lower(coalesce(externalIdentity.subject, '')) like :pattern
            )
        )
        order by lower(coalesce(userAccount.lastName, '')),
            lower(coalesce(userAccount.firstName, '')),
            lower(coalesce(userAccount.preferredUsername, '')),
            userAccount.id
        """, countQuery = """
        select count(userAccount.id)
        from UserAccount userAccount
        where (
            :pattern is null
            or lower(coalesce(userAccount.preferredUsername, '')) like :pattern
            or lower(coalesce(userAccount.firstName, '')) like :pattern
            or lower(coalesce(userAccount.lastName, '')) like :pattern
            or lower(coalesce(userAccount.displayName, '')) like :pattern
            or lower(coalesce(userAccount.email, '')) like :pattern
            or exists (
                select 1
                from ExternalIdentity externalIdentity
                where externalIdentity.userAccount = userAccount
                    and lower(coalesce(externalIdentity.subject, '')) like :pattern
            )
        )
        """)
    Page<Long> searchUserIds(@Param("pattern") String pattern, Pageable pageable);

    @Query("""
        select distinct userAccount
        from UserAccount userAccount
        left join fetch userAccount.externalIdentities externalIdentity
        """)
    List<UserAccount> findAllWithExternalIdentities();

    @Query("""
        select distinct userAccount
        from UserAccount userAccount
        left join fetch userAccount.externalIdentities externalIdentity
        where userAccount.id in :userAccountIds
        """)
    List<UserAccount> findAllWithExternalIdentitiesByIdIn(@Param("userAccountIds") Collection<Long> userAccountIds);

    @Query("""
        select distinct userAccount
        from UserAccount userAccount
        left join fetch userAccount.externalIdentities externalIdentity
        where userAccount.platformAdmin = true
        """)
    List<UserAccount> findAllPlatformAdminsWithExternalIdentities();
}
