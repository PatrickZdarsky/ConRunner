package at.control.shiftcontrol.shift.repo.pretalx;

import org.springframework.data.jpa.repository.JpaRepository;

import at.control.shiftcontrol.shift.entity.PretalxApiKey;

public interface PretalxApiKeyRepository extends JpaRepository<PretalxApiKey, String> {
}
