package at.control.shiftcontrol.identity.repo;

import at.control.shiftcontrol.identity.ApplicationRole;
import at.control.shiftcontrol.identity.entity.UserAccountEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {
    Optional<UserAccountEntity> findFirstByEmailIgnoreCase(String email);

    @Query(value = """
        select userAccount.id
        from UserAccountEntity userAccount
        where :pattern is null
            or lower(coalesce(userAccount.username, '')) like :pattern
            or lower(coalesce(userAccount.email, '')) like :pattern
            or exists (
                select 1
                from ExternalIdentityEntity externalIdentity
                where externalIdentity.userAccount = userAccount
                    and lower(coalesce(externalIdentity.subject, '')) like :pattern
            )
        order by lower(coalesce(userAccount.username, '')),
            userAccount.id
        """, countQuery = """
        select count(userAccount.id)
        from UserAccountEntity userAccount
        where :pattern is null
            or lower(coalesce(userAccount.username, '')) like :pattern
            or lower(coalesce(userAccount.email, '')) like :pattern
            or exists (
                select 1
                from ExternalIdentityEntity externalIdentity
                where externalIdentity.userAccount = userAccount
                    and lower(coalesce(externalIdentity.subject, '')) like :pattern
            )
        """)
    Page<UUID> searchUserIds(@Param("pattern") String pattern, Pageable pageable);

//    @Query("""
//        select distinct userAccount
//        from UserAccount userAccount
//        left join fetch userAccount.externalIdentities externalIdentity
//        where :platformAdminRole member of userAccount.applicationRoles
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
    List<UserAccountEntity> findAllWithExternalIdentitiesByIdIn(@Param("userAccountIds") Collection<UUID> userAccountIds);

    @Query("""
        select distinct userAccount
        from UserAccountEntity userAccount
        left join fetch userAccount.externalIdentities externalIdentity
        where :role member of userAccount.applicationRoles
        """)
    List<UserAccountEntity> findAllWithApplicationRoleAndExternalIdentities(@Param("role") ApplicationRole role);

    default List<UserAccountEntity> findAllPlatformAdminsWithExternalIdentities() {
        return findAllWithApplicationRoleAndExternalIdentities(ApplicationRole.PLATFORM_ADMIN);
    }
}
