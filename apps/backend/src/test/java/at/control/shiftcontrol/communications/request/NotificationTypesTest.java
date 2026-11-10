package at.control.shiftcontrol.communications.request;

import at.control.shiftcontrol.communications.api.NotificationTypes;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationTypesTest {
    @Test
    void everyPublicTypeHasExactlyOneTemplate() throws IllegalAccessException {
        var fields = Arrays.stream(NotificationTypes.class.getDeclaredFields())
            .filter(field -> Modifier.isPublic(field.getModifiers()) && field.getType() == String.class).toList();
        var values = new java.util.ArrayList<String>();
        for (var field : fields) values.add((String) field.get(null));
        assertThat(values).doesNotHaveDuplicates();
        assertThat(NotificationTemplates.types()).containsExactlyInAnyOrderElementsOf(values);
    }
}
