package group.aim.framework.basemodel;

/**
 * Created by narztiizzer on 11/17/2016 AD.
 */

public class ConfigurationBaseModel {
    public static String DEFAULT_NULL = "-";

    private String entriesFieldName = "entries";

    private static ConfigurationBaseModel ourInstance;

    public static ConfigurationBaseModel getInstance() {
        if(ourInstance == null)
            ourInstance = new ConfigurationBaseModel();
        return ourInstance;
    }

    public String getEntriesFieldName() {
        return entriesFieldName;
    }

    public void setEntriesFieldName(String entriesFieldName) {
        this.entriesFieldName = entriesFieldName;
    }
}
