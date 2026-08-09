package at.control.shiftcontrol.identity.repo;

import at.control.shiftcontrol.identity.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {
    Optional<UserAccountEntity> findFirstByEmailIgnoreCase(String email);

//    @Query(value = """
//        select userAccount.id
//        from UserAccountEntity userAccount
//        where (
//            userAccount.platformAdmin = true
//            or exists (
//                select 1
//                from ExternalIdentityEntity externalIdentity
//                where externalIdentity.userAccount = userAccount
//                    and exists (
//                        select 1
//                        from Volunteer volunteer
//                        where volunteer.id = externalIdentity.subject
//                    )
//            )
//        )
//        and (
//            :pattern is null
//            or lower(coalesce(userAccount.preferredUsername, '')) like :pattern
//            or lower(coalesce(userAccount.firstName, '')) like :pattern
//            or lower(coalesce(userAccount.lastName, '')) like :pattern
//            or lower(coalesce(userAccount.displayName, '')) like :pattern
//            or lower(coalesce(userAccount.email, '')) like :pattern
//            or exists (
//                select 1
//                from ExternalIdentityEntity externalIdentity
//                where externalIdentity.userAccount = userAccount
//                    and lower(coalesce(externalIdentity.subject, '')) like :pattern
//            )
//        )
//        order by lower(coalesce(userAccount.lastName, '')),
//            lower(coalesce(userAccount.firstName, '')),
//            lower(coalesce(userAccount.preferredUsername, '')),
//            userAccount.id
//        """, countQuery = """
//        select count(userAccount.id)
//        from UserAccountEntity userAccount
//        where (
//            userAccount.platformAdmin = true
//            or exists (
//                select 1
//                from ExternalIdentity externalIdentity
//                where externalIdentity.userAccount = userAccount
//                    and exists (
//                        select 1
//                        from Volunteer volunteer
//                        where volunteer.id = externalIdentity.subject
//                    )
//            )
//        )
//        and (
//            :pattern is null
//            or lower(coalesce(userAccount.preferredUsername, '')) like :pattern
//            or lower(coalesce(userAccount.firstName, '')) like :pattern
//            or lower(coalesce(userAccount.lastName, '')) like :pattern
//            or lower(coalesce(userAccount.displayName, '')) like :pattern
//            or lower(coalesce(userAccount.email, '')) like :pattern
//            or exists (
//                select 1
//                from ExternalIdentity externalIdentity
//                where externalIdentity.userAccount = userAccount
//                    and lower(coalesce(externalIdentity.subject, '')) like :pattern
//            )
//        )
//        """)
//    Page<Long> searchRelevantUserIds(@Param("pattern") String pattern, Pageable pageable);

//    @Query("""
//        select distinct userAccount
//        from UserAccount userAccount
//        left join fetch userAccount.externalIdentities externalIdentity
//        where userAccount.platformAdmin = true
//            or exists (
//                select 1
//                from ExternalIdentity relevantIdentity
//                where relevantIdentity.userAccount = userAccount
//                    and exists (
//                        select 1
//                        from Volunteer volunteer
//                        where volunteer.id = relevantIdentity.subject
//                    )
//            )
//        """)
//    List<UserAccountEntity> findAllRelevantWithExternalIdentities();

    @Query("""
        select distinct userAccount
        from UserAccountEntity userAccount
        left join fetch userAccount.externalIdentities externalIdentity
        """)
    List<UserAccountEntity> findAllWithExternalIdentities();

    @Query("""
        select distinct userAccount
        from UserAccountEntity userAccount
        left join fetch userAccount.externalIdentities externalIdentity
        where userAccount.id in :userAccountIds
        """)
    List<UserAccountEntity> findAllWithExternalIdentitiesByIdIn(@Param("userAccountIds") Collection<Long> userAccountIds);

    @Query("""
        select distinct userAccount
        from UserAccountEntity userAccount
        left join fetch userAccount.externalIdentities externalIdentity
        where userAccount.platformAdmin = true
        """)
    List<UserAccountEntity> findAllPlatformAdminsWithExternalIdentities();
}
