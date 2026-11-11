package at.control.shiftcontrol.shift.lib.type;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum ExportFormat {
    CSV(null),
    XLSX(new String[] {"excel"}),
    ;

    private final String[] aliases;

    public static final ExportFormat[] VALUES = values();

    public static ExportFormat fromString(String format) {
        for (ExportFormat exportFormat : VALUES) {
            if (exportFormat.name().equalsIgnoreCase(format)) {
                return exportFormat;
            }
            if (exportFormat.aliases != null) {
                for (String alias : exportFormat.aliases) {
                    if (alias.equalsIgnoreCase(format)) {
                        return exportFormat;
                    }
                }
            }
        }
        return null;
    }
}
