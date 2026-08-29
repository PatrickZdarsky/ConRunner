package at.control.shiftcontrol.identity.repo;

import at.control.shiftcontrol.identity.entity.ExternalIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentityEntity, Long> {
    Optional<ExternalIdentityEntity> findByIssuerAndSubject(String issuer, String subject);

    Collection<ExternalIdentityEntity> findAllByUserAccountId(UUID userAccountId);

    List<ExternalIdentityEntity> findAllBySubject(String subject);

    List<ExternalIdentityEntity> findAllBySubjectIn(Collection<String> subjects);
}
