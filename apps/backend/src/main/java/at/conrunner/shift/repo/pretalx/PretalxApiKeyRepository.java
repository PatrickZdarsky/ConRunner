package at.conrunner.shift.repo.pretalx;

import org.springframework.data.jpa.repository.JpaRepository;

import at.conrunner.shift.entity.PretalxApiKey;

public interface PretalxApiKeyRepository extends JpaRepository<PretalxApiKey, String> {
}
