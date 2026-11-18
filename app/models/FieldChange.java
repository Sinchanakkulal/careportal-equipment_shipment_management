package models;

/** One field that an edit actually changed. */
public class FieldChange {

    private final String key;        // JSON name stored in old_value / new_value, e.g. "modelNumber"
    private final String label;      // readable name used in the description, e.g. "model"
    private final String oldValue;
    private final String newValue;

    public FieldChange(String key, String label, String oldValue, String newValue) {
        this.key = key;
        this.label = label;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
}