package at.control.shiftcontrol.shift.entity;

import java.util.StringJoiner;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class PretalxApiKey {
    @Id
    private String apiKey;

    @NotNull
    private String pretalxHost;

    @Override
    public String toString() {
        return new StringJoiner(", ", PretalxApiKey.class.getSimpleName() + "[", "]")
            .add("apiKey='" + apiKey + "'")
            .toString();
    }
}
