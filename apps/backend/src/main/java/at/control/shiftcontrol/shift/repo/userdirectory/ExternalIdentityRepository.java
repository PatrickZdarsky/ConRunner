package at.control.shiftcontrol.shift.repo.userdirectory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import at.shiftcontrol.lib.entity.ExternalIdentity;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, Long> {
    Optional<ExternalIdentity> findByIssuerAndSubject(String issuer, String subject);

    Collection<ExternalIdentity> findAllByUserAccountId(long userAccountId);

    List<ExternalIdentity> findAllBySubjectIn(Collection<String> subjects);
}
